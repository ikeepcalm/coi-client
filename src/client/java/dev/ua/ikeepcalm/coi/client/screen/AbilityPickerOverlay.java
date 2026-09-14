package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.ServerCapabilities;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import dev.ua.ikeepcalm.coi.util.AbilityIcons;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Modal ability chooser: a search box over a list bracketed by pathway <em>and
 * sequence</em>, each ability a two-line card carrying the numbers that tell one
 * spell from another (cost, cooldown, category, kind). Not a screen child — the owning
 * screen renders it last (on its own stratum) and routes all input here while
 * {@link #isOpen()}.
 * <p>
 * The list is a flat {@link Row} list mixing clickable entries with inert
 * headers and messages, so scrolling, hit-testing and keyboard arithmetic all
 * walk the same structure instead of guessing at index offsets.
 */
public class AbilityPickerOverlay {

    private static final int UNBIND_HOVER = 0xFFFF6B6B;
    private static final int LOCKED_TEXT = 0xFFFF6B6B;
    private static final int TOOLTIP_WIDTH = 200;

    /** Rows are no longer uniform: only ability rows carry two text lines. */
    private static final int ROW_H = 26;
    private static final int HEADER_H = 13;
    private static final int UNBIND_H = 18;
    private static final int MESSAGE_H = 18;

    private static final int PAD = 8;
    private static final int SEARCH_H = 20;
    private static final int ICON = 16;
    /** Shared text column for both lines of an ability row, clear of the icon. */
    private static final int TEXT_INSET = PAD + ICON + 4;
    /** Second column of the meta line; widened when the cost badge overruns it. */
    private static final int COOLDOWN_COL = 44;

    private static final int MIN_PANEL_W = 260;
    private static final int MAX_PANEL_W = 360;

    /** Just enough pathway colour to read as a block, not enough to fight the text. */
    private static final int ROW_TINT_ALPHA = 22;
    private static final int HEADER_RULE_ALPHA = 110;
    private static final int DIM_ICON_ALPHA = 80;

    private boolean open = false;
    private Component title = Component.empty();
    private String currentSelection;
    private Consumer<String> onSelect;
    private EditBox searchBox;
    /** Matching abilities in display order; Enter still picks the first of these. */
    private final List<String> filtered = new ArrayList<>();
    /** What actually gets drawn: {@link #filtered} plus Unbind, headers and messages. */
    private final List<Row> rows = new ArrayList<>();
    private int scrollOffset = 0;
    /** Whether this connection carries cost/cooldown numbers at all — see {@link #detectMeta()}. */
    private boolean metaAvailable = false;

    // Panel geometry, recomputed every render frame; mouse events reuse the last frame's values
    private int panelX, panelY, panelW, panelH, listTop, listH;

    // Row under the cursor this frame; the tooltip for it is emitted after the
    // list's scissor is popped, otherwise it would be clipped away
    private String hoveredOption;

    /** Pathway block, then sequence inside it, then name — the order the headers assume. */
    private static final Comparator<String> LIST_ORDER =
            Comparator.comparing(AbilityPickerOverlay::pathwayOf)
                    .thenComparingInt(AbilityPickerOverlay::sequenceOf)
                    .thenComparing(AbilityPickerOverlay::displayNameOf, String.CASE_INSENSITIVE_ORDER);

    private enum RowKind {UNBIND, HEADER, ABILITY, MESSAGE}

    /**
     * One visual line. Headers and the no-results message are inert: they take up
     * layout space and scroll with everything else, but never answer a click.
     */
    private record Row(RowKind kind, String option, String pathway, int sequence) {

        static Row unbind() {
            return new Row(RowKind.UNBIND, null, "", -1);
        }

        static Row message() {
            return new Row(RowKind.MESSAGE, null, "", -1);
        }

        static Row header(String pathway, int sequence) {
            return new Row(RowKind.HEADER, null, pathway, sequence);
        }

        static Row ability(String option, String pathway) {
            return new Row(RowKind.ABILITY, option, pathway, -1);
        }

        boolean clickable() {
            return kind == RowKind.UNBIND || kind == RowKind.ABILITY;
        }
    }

    public void open(Component contextTitle, String currentSelection, Consumer<String> onSelect) {
        this.open = true;
        this.title = contextTitle;
        this.currentSelection = currentSelection;
        this.onSelect = onSelect;
        this.scrollOffset = 0;
        this.metaAvailable = detectMeta();

        Font font = Minecraft.getInstance().font;
        this.searchBox = new EditBox(font, 0, 0, 100, SEARCH_H, Component.translatable("screen.coi.picker_search_hint"));
        searchBox.setMaxLength(60);
        searchBox.setHint(Component.translatable("screen.coi.picker_search_hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(s -> {
            refilter();
            scrollOffset = 0;
        });
        searchBox.setFocused(true);
        refilter();
    }

    public void close() {
        open = false;
        searchBox = null;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Does this server feed ability metadata? The capability reply is the
     * contract, but a list that carries any richer field counts too — that keeps
     * the badges alive in the dev environment, where nothing answers the hello.
     * A protocol-1 list has none of it and would otherwise render a column of
     * em-dashes, which says less than nothing.
     */
    private static boolean detectMeta() {
        if (ServerCapabilities.has("ability_meta")) return true;
        for (String option : CircleOfImaginationClient.getAvailableAbilities()) {
            AbilityInfo info = infoFor(option);
            if (info == null) continue;
            if (info.cost() > 0 || info.drainPerSecond() > 0 || info.cooldownSeconds() > 0
                    || !info.description().isEmpty() || info.isToggle()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search matches the display name, the pathway and the category, so "door",
     * "mobility" and "Traveler" all reach the same row.
     */
    private void refilter() {
        filtered.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (String option : CircleOfImaginationClient.getAvailableAbilities()) {
            if (matches(option, query)) filtered.add(option);
        }
        filtered.sort(LIST_ORDER);
        rebuildRows();
    }

    private static boolean matches(String option, String query) {
        if (query.isEmpty()) return true;
        if (displayNameOf(option).toLowerCase(Locale.ROOT).contains(query)) return true;
        if (pathwayOf(option).contains(query)) return true;
        AbilityInfo info = infoFor(option);
        return info != null && info.category() != null
                && info.category().toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * Flattens the sorted list into drawable rows, one header per
     * <em>pathway and sequence</em> — the sequence is the bracket a player
     * actually thinks in, so Fool 5 and Fool 3 are two blocks, not one. The
     * list is already sorted pathway → sequence → name, so a group break is
     * simply "this row's key differs from the last one's".
     */
    private void rebuildRows() {
        rows.clear();
        rows.add(Row.unbind());

        if (filtered.isEmpty()) {
            if (searchBox != null && !searchBox.getValue().isEmpty()) rows.add(Row.message());
            return;
        }

        String lastPathway = null;
        int lastSequence = Integer.MIN_VALUE;
        for (String option : filtered) {
            String pathway = pathwayOf(option);
            int sequence = sequenceOf(option);
            if (!pathway.equals(lastPathway) || sequence != lastSequence) {
                rows.add(Row.header(pathway, sequence));
                lastPathway = pathway;
                lastSequence = sequence;
            }
            rows.add(Row.ability(option, pathway));
        }
    }

    /** Protocol 2 names the pathway; otherwise it is the first id segment. */
    private static String pathwayOf(String option) {
        AbilityInfo info = infoFor(option);
        String pathway = info != null ? info.pathway() : null;
        return pathway == null || pathway.isEmpty()
                ? AbilityInfo.pathwayOf(AbilityInfo.extractId(option))
                : pathway;
    }

    private static int sequenceOf(String option) {
        AbilityInfo info = infoFor(option);
        if (info != null && info.sequence() >= 0) return info.sequence();
        return AbilityInfo.sequenceOf(AbilityInfo.extractId(option));
    }

    private static String displayNameOf(String option) {
        String name = AbilityInfo.extractDisplayName(option);
        return name == null ? option : name;
    }

    private int rowHeight(Row row) {
        return switch (row.kind()) {
            case UNBIND -> UNBIND_H;
            case HEADER -> HEADER_H;
            case MESSAGE -> MESSAGE_H;
            case ABILITY -> ROW_H;
        };
    }

    private int contentHeight() {
        int total = 0;
        for (Row row : rows) total += rowHeight(row);
        return total;
    }

    /**
     * First row index that still leaves the whole tail on screen. Rows have
     * different heights, so this is measured in pixels and answered as an index.
     */
    private int maxScroll() {
        int tail = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            tail += rowHeight(rows.get(i));
            if (tail > listH) return i + 1;
        }
        return 0;
    }

    private void updateGeometry(int screenW, int screenH) {
        panelW = Math.clamp(screenW * 3 / 5, MIN_PANEL_W, Math.max(MIN_PANEL_W, screenW - 20));
        panelW = Math.min(panelW, MAX_PANEL_W);

        int headerH = PAD + 9 + 6 + SEARCH_H + 6;
        // The floor keeps Unbind plus one real ability row reachable even at gui
        // scale 4 in a small window, where the honest fit would be zero rows.
        int floor = UNBIND_H + ROW_H;
        int room = screenH - 60 - headerH - PAD;
        listH = Math.min(contentHeight(), Math.max(room, floor));

        panelH = headerH + listH + PAD;
        panelX = Math.max(0, (screenW - panelW) / 2);
        panelY = Math.max(0, (screenH - panelH) / 2);
        listTop = panelY + headerH;

        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());

        if (searchBox != null) {
            searchBox.setX(panelX + PAD);
            searchBox.setY(panelY + PAD + 9 + 6);
            searchBox.setWidth(panelW - PAD * 2);
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int screenW, int screenH, int mouseX, int mouseY, float delta) {
        if (!open) return;
        updateGeometry(screenW, screenH);

        graphics.fill(0, 0, screenW, screenH, CoiStyle.BACKDROP);
        CoiStyle.drawCard(graphics, panelX, panelY, panelW, panelH);

        Component trimmedTitle = Component.literal(font.plainSubstrByWidth(title.getString(), panelW - PAD * 2));
        graphics.text(font, trimmedTitle, panelX + PAD, panelY + PAD, CoiStyle.ACCENT);

        if (searchBox != null) {
            searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        int listBottom = listTop + listH;
        hoveredOption = null;
        graphics.enableScissor(panelX + 1, listTop, panelX + panelW - 1, listBottom);
        int y = listTop;
        for (int i = scrollOffset; i < rows.size() && y < listBottom; i++) {
            Row row = rows.get(i);
            int height = rowHeight(row);
            boolean hovered = row.clickable()
                    && mouseX >= panelX + 1 && mouseX < panelX + panelW - 1
                    && mouseY >= y && mouseY < y + height
                    && mouseY >= listTop && mouseY < listBottom;
            switch (row.kind()) {
                case UNBIND -> renderUnbindRow(graphics, font, y, hovered);
                case HEADER -> renderHeaderRow(graphics, font, row, y);
                case MESSAGE -> graphics.centeredText(font,
                        Component.translatable("screen.coi.picker_no_results",
                                searchBox == null ? "" : searchBox.getValue()),
                        panelX + panelW / 2, y + 5, CoiStyle.TEXT_MUTED);
                case ABILITY -> {
                    if (hovered) hoveredOption = row.option();
                    renderAbilityRow(graphics, font, row, y, hovered);
                }
            }
            y += height;
        }
        graphics.disableScissor();

        if (hoveredOption != null) {
            renderAbilityTooltip(graphics, font, hoveredOption, mouseX, mouseY);
        }

        if (contentHeight() > listH) {
            renderScrollbar(graphics, listBottom);
        }
    }

    private void renderUnbindRow(GuiGraphicsExtractor graphics, Font font, int rowY, boolean hovered) {
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + UNBIND_H, CoiStyle.ROW_HOVER);
        }
        int color = hovered ? UNBIND_HOVER : CoiStyle.TEXT_MUTED;
        // Drawn ✕ glyph (unicode is unreliable in the MC font)
        float cx = panelX + PAD + 8;
        float cy = rowY + UNBIND_H / 2f;
        EffectPaint.line(graphics, cx - 3, cy - 3, cx + 3, cy + 3, color, 1);
        EffectPaint.line(graphics, cx - 3, cy + 3, cx + 3, cy - 3, color, 1);
        graphics.text(font, Component.translatable("screen.coi.picker_unbind"),
                panelX + TEXT_INSET, rowY + 5, color);
    }

    /**
     * Pathway caption plus a 1px rule to the panel edge — the rule is what makes
     * the abilities below it read as one block rather than a fresh list.
     */
    private void renderHeaderRow(GuiGraphicsExtractor graphics, Font font, Row row, int rowY) {
        int rgb = AbilityInfo.pathwayRgb(row.pathway());
        String name = row.pathway().isEmpty()
                ? I18n.get("screen.coi.picker_group_other")
                : row.pathway().toUpperCase(Locale.ROOT);
        Component label = row.sequence() >= 0
                ? Component.translatable("screen.coi.picker_group_seq", name, row.sequence())
                : Component.literal(name);

        int textX = panelX + PAD;
        graphics.text(font, label, textX, rowY + 3, EffectPaint.argb(rgb, 255), false);

        int ruleX = textX + font.width(label) + 4;
        int ruleEnd = panelX + panelW - PAD;
        if (ruleEnd > ruleX) {
            graphics.fill(ruleX, rowY + 7, ruleEnd, rowY + 8, EffectPaint.argb(rgb, HEADER_RULE_ALPHA));
        }
    }

    private void renderAbilityRow(GuiGraphicsExtractor graphics, Font font, Row row, int rowY, boolean hovered) {
        String option = row.option();
        AbilityInfo info = infoFor(option);
        boolean unavailable = info != null && info.isUnavailable();
        int rgb = AbilityInfo.pathwayRgb(row.pathway());

        // A wash of the pathway colour under every row of the group; the hover and
        // selection layers sit on top so they still read against it.
        graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + ROW_H, EffectPaint.argb(rgb, ROW_TINT_ALPHA));
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + ROW_H, CoiStyle.ROW_HOVER);
        }
        graphics.fill(panelX + 1, rowY + 1, panelX + 3, rowY + ROW_H - 1, EffectPaint.argb(rgb, 255));

        AbilityIcons.draw(graphics, option, panelX + PAD, rowY + (ROW_H - ICON) / 2, ICON,
                unavailable ? DIM_ICON_ALPHA : 255);

        int textX = panelX + TEXT_INSET;
        int rightEdge = panelX + panelW - PAD;
        int lineOne = rowY + 4;

        Component tag = kindTag(info);
        int tagX = rightEdge - font.width(tag);
        graphics.text(font, tag, tagX, lineOne, unavailable ? LOCKED_TEXT : CoiStyle.TEXT_MUTED, false);

        String name = font.plainSubstrByWidth(displayNameOf(option), Math.max(8, tagX - 5 - textX));
        graphics.text(font, name, textX, lineOne, unavailable ? LOCKED_TEXT : CoiStyle.TEXT_BODY, false);
        if (unavailable) {
            graphics.fill(textX, lineOne + 4, textX + font.width(name), lineOne + 5, LOCKED_TEXT);
        }

        renderMetaLine(graphics, font, info, textX, rightEdge, rowY + 15);

        if (isCurrentSelection(option)) {
            graphics.outline(panelX + 1, rowY, panelW - 2, ROW_H, CoiStyle.ACCENT);
        }
    }

    /**
     * Cost, cooldown and category in fixed columns. The badges are drawn even
     * when the value is zero — a blank where a cost should be is exactly what
     * made the old list read as interchangeable — but a server that sends no
     * numbers at all gets the category alone instead of a row of em-dashes.
     */
    private void renderMetaLine(GuiGraphicsExtractor graphics, Font font, AbilityInfo info,
                                int textX, int rightEdge, int lineY) {
        Component category = categoryLabel(info);
        if (!metaAvailable) {
            graphics.text(font, category, textX, lineY, CoiStyle.TEXT_MUTED, false);
            return;
        }

        int categoryX = rightEdge - font.width(category);
        graphics.text(font, category, categoryX, lineY, CoiStyle.TEXT_MUTED, false);

        Component cost = costLabel(info);
        graphics.text(font, cost, textX, lineY, CoiStyle.TEXT_MUTED, false);

        // A long cost badge pushes the cooldown column rather than colliding with it
        int cooldownX = Math.max(textX + COOLDOWN_COL, textX + font.width(cost) + 6);
        Component cooldown = cooldownLabel(info);
        if (cooldownX + font.width(cooldown) < categoryX - 4) {
            graphics.text(font, cooldown, cooldownX, lineY, CoiStyle.TEXT_MUTED, false);
        }
    }

    private static Component kindTag(AbilityInfo info) {
        if (info == null) return Component.empty();
        if (info.locked()) return Component.translatable("screen.coi.picker_kind_locked");
        if (info.blocked()) return Component.translatable("screen.coi.picker_kind_blocked");
        // Whitelisted, so a kind the server invents never leaks a raw lang key
        String kind = switch (info.kind()) {
            case AbilityInfo.KIND_ACTIVATED -> AbilityInfo.KIND_ACTIVATED;
            case AbilityInfo.KIND_PASSIVE -> AbilityInfo.KIND_PASSIVE;
            default -> AbilityInfo.KIND_ACTIVE;
        };
        return Component.translatable("screen.coi.picker_kind_" + kind);
    }

    private static Component costLabel(AbilityInfo info) {
        if (info == null) return Component.translatable("screen.coi.picker_meta_cost_none");
        if (info.drainPerSecond() > 0) {
            return Component.translatable("screen.coi.picker_meta_drain",
                    String.format(Locale.ROOT, "%.1f", info.drainPerSecond()));
        }
        if (info.cost() > 0) {
            return Component.translatable("screen.coi.picker_meta_cost", info.cost());
        }
        return Component.translatable("screen.coi.picker_meta_cost_none");
    }

    private static Component cooldownLabel(AbilityInfo info) {
        if (info == null || info.cooldownSeconds() <= 0) {
            return Component.translatable("screen.coi.picker_meta_cooldown_none");
        }
        return Component.translatable("screen.coi.picker_meta_cooldown", info.cooldownSeconds());
    }

    /**
     * The category as a translated word. The ability list carries the raw key
     * (the localized {@code categoryName} only ever rides on state updates), so
     * anything we have no string for falls back to the key itself — still better
     * on screen than a raw lang path.
     */
    private static Component categoryLabel(AbilityInfo info) {
        String category = info == null || info.category() == null ? "" : info.category().toLowerCase(Locale.ROOT);
        if (category.isEmpty()) category = "uncategorized";
        String key = "screen.coi.ability_cat_" + category;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(category);
    }

    private static AbilityInfo infoFor(String option) {
        return CircleOfImaginationClient.getAbilityInfo(AbilityInfo.extractId(option));
    }

    /**
     * What the row cannot show: the prose, the sequence and why the ability is
     * unusable. Cost, cooldown and kind now live permanently on the row itself,
     * so repeating them here would only be noise.
     */
    private void renderAbilityTooltip(GuiGraphicsExtractor graphics, Font font, String option, int mouseX, int mouseY) {
        AbilityInfo info = infoFor(option);
        if (info == null) return;

        List<FormattedCharSequence> lines = new ArrayList<>();
        if (!info.description().isEmpty()) {
            lines.addAll(font.split(Component.literal(info.description()).withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        }
        if (info.sequence() >= 0) {
            addLine(lines, Component.translatable("screen.coi.ability_tooltip_sequence", info.sequence()).withStyle(ChatFormatting.DARK_GRAY));
        }
        Component reason = blockReason(info);
        if (reason != null) {
            addLine(lines, reason.copy().withStyle(ChatFormatting.RED));
        }

        if (!lines.isEmpty()) {
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static void addLine(List<FormattedCharSequence> lines, Component component) {
        lines.add(component.getVisualOrderText());
    }

    private static Component blockReason(AbilityInfo info) {
        if (info.locked()) return Component.translatable("screen.coi.ability_tooltip_locked");
        if (!info.blocked()) return null;
        return switch (info.blockedBy()) {
            case "hanged" -> Component.translatable("screen.coi.ability_tooltip_blocked_hanged");
            case "devouring" -> Component.translatable("screen.coi.ability_tooltip_blocked_devouring");
            default -> Component.translatable("screen.coi.ability_tooltip_blocked_contract");
        };
    }

    private boolean isCurrentSelection(String option) {
        if (currentSelection == null) return false;
        return AbilityInfo.extractId(option).equals(AbilityInfo.extractId(currentSelection))
                && AbilityInfo.extractAction(option).equals(AbilityInfo.extractAction(currentSelection));
    }

    /**
     * Handle size and position come from pixels, not row counts, since headers
     * and ability rows are different heights.
     */
    private void renderScrollbar(GuiGraphicsExtractor graphics, int listBottom) {
        int trackX = panelX + panelW - 5;
        int trackTop = listTop + 1;
        int trackH = listBottom - trackTop - 1;
        if (trackH <= 0) return;
        graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackH, CoiStyle.SCROLL_TRACK);

        int content = contentHeight();
        int above = 0;
        for (int i = 0; i < scrollOffset && i < rows.size(); i++) {
            above += rowHeight(rows.get(i));
        }
        int handleH = Math.clamp((long) listH * trackH / Math.max(1, content), Math.min(12, trackH), trackH);
        int handleY = trackTop + (trackH - handleH) * above / Math.max(1, content - listH);
        graphics.fill(trackX, handleY, trackX + 3, handleY + handleH, CoiStyle.BORDER);
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!open) return false;
        double mx = event.x();
        double my = event.y();

        if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
            close();
            return true;
        }

        if (searchBox != null && searchBox.isMouseOver(mx, my)) {
            searchBox.setFocused(true);
            searchBox.mouseClicked(event, false);
            return true;
        }

        int listBottom = listTop + listH;
        if (my >= listTop && my < listBottom) {
            int y = listTop;
            for (int i = scrollOffset; i < rows.size() && y < listBottom; i++) {
                Row row = rows.get(i);
                int height = rowHeight(row);
                if (my >= y && my < y + height) {
                    // Headers and messages simply swallow the click
                    if (row.kind() == RowKind.UNBIND) pick(null);
                    else if (row.kind() == RowKind.ABILITY) pick(row.option());
                    break;
                }
                y += height;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return false;
        scrollOffset = Mth.clamp(scrollOffset + (verticalAmount > 0 ? -1 : 1), 0, maxScroll());
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!open) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (!filtered.isEmpty()) {
                pick(filtered.getFirst());
            }
            return true;
        }
        if (searchBox != null) {
            searchBox.keyPressed(event);
        }
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) return false;
        if (searchBox != null) {
            searchBox.charTyped(event);
        }
        return true;
    }

    private void pick(String option) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (onSelect != null) {
            onSelect.accept(option);
        }
        close();
    }
}
