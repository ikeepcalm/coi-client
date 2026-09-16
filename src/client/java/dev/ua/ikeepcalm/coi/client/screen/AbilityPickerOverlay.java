package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Modal ability chooser: search box, pathway-colored rows with icons, and an
 * always-present Unbind row. Not a screen child — the owning screen renders it
 * last (on its own stratum) and routes all input here while {@link #isOpen()}.
 */
public class AbilityPickerOverlay {

    private static final int UNBIND_HOVER = 0xFFFF6B6B;

    private static final int ROW_H = 22;
    private static final int PAD = 8;
    private static final int SEARCH_H = 20;

    private boolean open = false;
    private Component title = Component.empty();
    private String currentSelection;
    private Consumer<String> onSelect;
    private EditBox searchBox;
    private final List<String> filtered = new ArrayList<>();
    private int scrollOffset = 0;

    // Panel geometry, recomputed every render frame; mouse events reuse the last frame's values
    private int panelX, panelY, panelW, panelH, listTop, visibleRows;

    public void open(Component contextTitle, String currentSelection, Consumer<String> onSelect) {
        this.open = true;
        this.title = contextTitle;
        this.currentSelection = currentSelection;
        this.onSelect = onSelect;
        this.scrollOffset = 0;

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

    private void refilter() {
        filtered.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (String option : CircleOfImaginationClient.getAvailableAbilities()) {
            String name = AbilityInfo.extractDisplayName(option);
            if (name == null) name = option;
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(option);
            }
        }
    }

    /**
     * Total visual rows: the Unbind row plus every filtered ability.
     */
    private int totalRows() {
        return filtered.size() + 1;
    }

    /**
     * An extra row is reserved for the "no results" message when a search matches nothing.
     */
    private boolean showNoResults() {
        return filtered.isEmpty() && searchBox != null && !searchBox.getValue().isEmpty();
    }

    private void updateGeometry(int screenW, int screenH) {
        panelW = Math.clamp(screenW / 2, 240, Math.max(240, screenW - 20));
        panelW = Math.min(panelW, 320);

        int headerH = PAD + 9 + 6 + SEARCH_H + 6;
        int maxListH = screenH - 60 - headerH - PAD;
        int capacity = Math.max(3, maxListH / ROW_H);
        visibleRows = Math.min(totalRows() + (showNoResults() ? 1 : 0), capacity);
        panelH = headerH + visibleRows * ROW_H + PAD;

        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;
        listTop = panelY + headerH;

        int maxScroll = Math.max(0, totalRows() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

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

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, CoiStyle.CARD_BG);
        graphics.outline(panelX, panelY, panelW, panelH, CoiStyle.BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, CoiStyle.ACCENT);

        Component trimmedTitle = Component.literal(font.plainSubstrByWidth(title.getString(), panelW - PAD * 2));
        graphics.text(font, trimmedTitle, panelX + PAD, panelY + PAD, CoiStyle.ACCENT);

        if (searchBox != null) {
            searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        int listBottom = listTop + visibleRows * ROW_H;
        graphics.enableScissor(panelX + 1, listTop, panelX + panelW - 1, listBottom);
        for (int row = scrollOffset; row < Math.min(scrollOffset + visibleRows, totalRows()); row++) {
            int rowY = listTop + (row - scrollOffset) * ROW_H;
            boolean hovered = mouseX >= panelX + 1 && mouseX < panelX + panelW - 1
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (row == 0) {
                renderUnbindRow(graphics, font, rowY, hovered);
            } else {
                renderAbilityRow(graphics, font, filtered.get(row - 1), rowY, hovered);
            }
        }
        graphics.disableScissor();

        if (showNoResults()) {
            graphics.centeredText(font,
                    Component.translatable("screen.coi.picker_no_results", searchBox.getValue()),
                    panelX + panelW / 2, listTop + ROW_H + 7, CoiStyle.TEXT_MUTED);
        }

        if (totalRows() > visibleRows) {
            renderScrollbar(graphics, listBottom);
        }
    }

    private void renderUnbindRow(GuiGraphicsExtractor graphics, Font font, int rowY, boolean hovered) {
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + ROW_H, CoiStyle.ROW_HOVER);
        }
        int color = hovered ? UNBIND_HOVER : CoiStyle.TEXT_MUTED;
        // Drawn ✕ glyph (unicode is unreliable in the MC font)
        float cx = panelX + PAD + 8;
        float cy = rowY + ROW_H / 2f;
        EffectPaint.line(graphics, cx - 3, cy - 3, cx + 3, cy + 3, color, 1);
        EffectPaint.line(graphics, cx - 3, cy + 3, cx + 3, cy - 3, color, 1);
        graphics.text(font, Component.translatable("screen.coi.picker_unbind"), panelX + PAD + 20, rowY + 7, color);
    }

    private void renderAbilityRow(GuiGraphicsExtractor graphics, Font font, String option, int rowY, boolean hovered) {
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + ROW_H, CoiStyle.ROW_HOVER);
        }

        int pathway = AbilityInfo.pathwayColor(AbilityInfo.extractId(option));
        graphics.fill(panelX + 1, rowY + 1, panelX + 3, rowY + ROW_H - 1, pathway);

        AbilityIcons.draw(graphics, option, panelX + PAD, rowY + (ROW_H - 16) / 2, 16, 255);

        String name = AbilityInfo.extractDisplayName(option);
        if (name == null) name = option;
        int textX = panelX + PAD + 20;
        graphics.text(font, font.plainSubstrByWidth(name, panelX + panelW - PAD - textX), textX, rowY + 7, CoiStyle.TEXT_BODY, false);

        if (isCurrentSelection(option)) {
            graphics.outline(panelX + 1, rowY, panelW - 2, ROW_H, CoiStyle.ACCENT);
        }
    }

    private boolean isCurrentSelection(String option) {
        if (currentSelection == null) return false;
        return AbilityInfo.extractId(option).equals(AbilityInfo.extractId(currentSelection))
                && AbilityInfo.extractAction(option).equals(AbilityInfo.extractAction(currentSelection));
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int listBottom) {
        int trackX = panelX + panelW - 5;
        int trackTop = listTop + 1;
        int trackH = listBottom - trackTop - 1;
        graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackH, CoiStyle.SCROLL_TRACK);

        int maxScroll = totalRows() - visibleRows;
        int handleH = Math.max(12, visibleRows * trackH / totalRows());
        int handleY = trackTop + (trackH - handleH) * scrollOffset / Math.max(1, maxScroll);
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

        int listBottom = listTop + visibleRows * ROW_H;
        if (my >= listTop && my < listBottom) {
            int row = scrollOffset + (int) ((my - listTop) / ROW_H);
            if (row == 0) {
                pick(null);
            } else if (row - 1 < filtered.size()) {
                pick(filtered.get(row - 1));
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return false;
        int maxScroll = Math.max(0, totalRows() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset + (verticalAmount > 0 ? -1 : 1), 0, maxScroll);
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
