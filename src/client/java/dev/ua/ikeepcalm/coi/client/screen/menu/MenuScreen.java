package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.menu.ClientMenuState;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.network.MenuActionPayload;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Renders a server-authored {@link MenuDocument} — the native replacement for
 * the plugin's InvUI chest GUIs.
 * <p>
 * One card, scrolled in pixels, built from a flat {@link Part} list so layout,
 * hit-testing and the scrollbar all walk the same structure even though no two
 * component types are the same height. Nothing here is a vanilla widget except
 * the text fields: buttons are drawn by {@link MenuTheme} and hit-tested by
 * hand, which is what lets them scroll inside a scissor and carry a second line
 * and a disabled reason.
 * <p>
 * The screen owns no navigation. Every click goes back on
 * {@code coi-client:menu_action} with the newest session and version, and the
 * server answers with the next document; a document whose {@code screen} id is
 * unchanged is a refresh, so scroll position, typed text <em>and</em> whatever
 * the player has opened or collapsed survive it.
 */
public class MenuScreen extends Screen {

    /** Card geometry is shared with the character sheet — see {@link CoiStyle#cardWidth}. */
    private static final int ICON = 16;
    private static final int SMALL_ICON = 12;
    private static final int SCROLL_STEP = 20;
    private static final int GAP = 4;
    private static final int CONFIRM_MAX_W = 240;
    private static final int MIN_TILE = 18;
    private static final int FIELD_H = 18;

    /**
     * A panel cell narrower than this cannot hold a title and a number, so a
     * document asking for three columns on a narrow card gets two.
     */
    private static final int MIN_PANEL_W = 72;

    private static final int HERO_ICON = 32;
    private static final int HERO_RING = 28;
    private static final int STAT_RING = 24;
    private static final int WATERMARK = 64;

    /**
     * Where a step rail's markers sit, and where its text starts.
     */
    private static final int RAIL_X = 5;
    private static final int RAIL_TEXT_X = 18;

    /**
     * How long a document's notice keeps its arrival highlight.
     */
    private static final long NOTICE_FLASH_MS = 900;

    /**
     * Hover tints ease over this; gauges tween toward their target over the
     * other. Both are suppressed outright under {@code epilepsyMode} — see
     * {@link #approach}.
     */
    private static final float HOVER_MS = 120;
    private static final float GAUGE_MS = 200;

    private final Screen parent;

    private MenuDocument doc;
    private int docRevision = -1;
    private long adoptedAt;

    private final List<Part> parts = new ArrayList<>();
    /**
     * Text fields survive a rebuild, keyed by component id. Recreating them per
     * layout would drop the caret mid-word — and the search box relays out on
     * every keystroke, so that happens constantly.
     */
    private final Map<String, EditBox> fields = new HashMap<>();
    /**
     * What the player has opened or collapsed, keyed {@code screen + "/" + id}.
     * Same bargain as {@link #fields}, and for a sharper reason: the server
     * re-pushes a document every 60 ticks, and a refresh that slammed an open
     * {@code details} shut would make the whole component unusable.
     */
    private final Map<String, Boolean> disclosure = new HashMap<>();
    /**
     * Chevron rotation per disclosure key. Kept beside the state rather than on
     * the part, because the part is thrown away and rebuilt by the very click
     * that starts the animation.
     */
    private final Map<String, Float> chevronT = new HashMap<>();
    private EditBox focusedField;

    private int scrollY;
    private int contentH;
    private int cardX, cardY, cardW, cardH;
    private int headerH, noticeH, footerH, viewTop, viewBottom;
    private List<FormattedCharSequence> noticeLines = List.of();

    private boolean draggingScroll;
    private int scrollGrab;

    /**
     * Milliseconds since the previous frame, the clock every tween runs on.
     */
    private float frameDelta;
    private long lastFrameAt;

    /**
     * Set while drawing, emitted after the scissor is popped — a tooltip raised
     * inside it would be clipped to the card.
     */
    private List<Component> tooltip;
    private String pressedAction;

    private Confirmation pending;
    private boolean closeSent;

    /**
     * A click the player still has to agree to. It carries the source button's
     * style so a destructive confirm reads as destructive, and the wrapped body
     * so the modal is not re-wrapped three times per frame.
     */
    private record Confirmation(MenuComponent.Confirm confirm, String action,
                                MenuComponent.ButtonStyle style, List<FormattedCharSequence> body) {
    }

    public MenuScreen(Screen parent) {
        super(Component.translatable("screen.coi.menu_title"));
        this.parent = parent;
    }

    private boolean compact() {
        return this.height < 300;
    }

    private int pad() {
        return compact() ? 8 : 12;
    }

    private int buttonH() {
        return compact() ? 16 : 20;
    }

    private int contentW() {
        return cardW - pad() * 2;
    }

    private int accent() {
        return doc != null ? doc.accentArgb() : CoiStyle.ACCENT;
    }

    // --- Lifecycle ---

    @Override
    protected void init() {
        rebuild();
    }

    @Override
    public boolean isPauseScreen() {
        // A chest GUI never paused the world, and this stands in for one
        return false;
    }

    @Override
    public void onClose() {
        sendClose();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        sendClose();
        super.removed();
    }

    /**
     * The server took its own screen away ({@code "closed":true}); it already
     * knows the menu is gone, so nothing goes back.
     */
    public void closeQuietly() {
        closeSent = true;
        this.minecraft.gui.setScreen(parent);
    }

    private void sendClose() {
        if (closeSent) return;
        closeSent = true;
        send(MenuActionPayload.CLOSE, null);
        ClientMenuState.clear();
    }

    /**
     * Never talk into the void: the debug screen opens this with no server
     * attached, and a server predating the menu protocol has nothing listening
     * on the channel.
     */
    private void send(String action, String value) {
        if (action == null || action.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || !ClientPlayNetworking.canSend(MenuActionPayload.ID)) return;
        ClientPlayNetworking.send(MenuActionPayload.of(
                ClientMenuState.session(), ClientMenuState.version(), action, value));
    }

    private void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void fire(String action, String value) {
        if (action == null || action.isEmpty()) return;
        click();
        send(action, value);
    }

    // --- Document adoption ---

    /**
     * Takes on whatever document the state holds. Called from {@code init} (so
     * a resize or a gui-scale change re-lays out) and from the render loop
     * whenever a push bumped the revision.
     */
    private void rebuild() {
        MenuDocument next = ClientMenuState.document();
        if (next == null) return;

        boolean sameScreen = doc != null && doc.screen().equals(next.screen());
        if (!sameScreen) {
            scrollY = 0;
            fields.clear();
            focusedField = null;
            // Disclosure belongs to the screen id, not to the document version:
            // this is the only place it may be forgotten
            disclosure.clear();
            chevronT.clear();
        }
        if (docRevision != ClientMenuState.revision()) {
            adoptedAt = System.currentTimeMillis();
        }

        doc = next;
        docRevision = ClientMenuState.revision();
        pending = null;
        pressedAction = null;

        layout();
    }

    private void layout() {
        cardW = CoiStyle.cardWidth(this.width, compact());
        cardX = (this.width - cardW) / 2;

        int titleBlock = doc.subtitle().isEmpty() ? 9 : 20;
        headerH = pad() + Math.max(ICON, titleBlock) + 6;

        noticeLines = doc.toast() == null ? List.of()
                : this.font.split(Component.literal(doc.toast().text()), contentW() - 12);
        noticeH = noticeLines.isEmpty() ? 0 : 6 + noticeLines.size() * 9 + 6;

        footerH = doc.footer().isEmpty() ? 0 : footerBlockH() + pad();

        buildParts();

        int margin = compact() ? 8 : 18;
        int availH = Math.max(80, this.height - margin * 2);
        cardH = Math.min(availH, headerH + noticeH + contentH + pad() + footerH);
        cardY = (this.height - cardH) / 2;
        viewTop = cardY + headerH + noticeH;
        viewBottom = Math.max(viewTop + 20, cardY + cardH - footerH - pad() / 2);

        scrollY = Mth.clamp(scrollY, 0, maxScroll());
    }

    private int viewH() {
        return viewBottom - viewTop;
    }

    private int maxScroll() {
        return Math.max(0, contentH - viewH());
    }

    // --- Animation ---

    /**
     * One step of a tween toward {@code target}, frame-rate independent.
     * <p>
     * Under {@code epilepsyMode} it returns the target outright, which is what
     * makes the whole chrome — hover tints, gauge fills, the disclosure
     * chevron — snap rather than move. Every animated value on this screen goes
     * through here, so there is exactly one place that has to honour the
     * setting.
     */
    private float approach(float current, float target, float durationMs) {
        if (HudConfig.getSettings().epilepsyMode || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    // --- Disclosure ---

    private String disclosureKey(String id) {
        return doc.screen() + "/" + id;
    }

    /**
     * @param initial the server's opening suggestion, used only until the
     *                player has touched this one
     */
    private boolean disclosed(String id, boolean initial) {
        return disclosure.getOrDefault(disclosureKey(id), initial);
    }

    private void toggleDisclosure(String id, boolean initial) {
        disclosure.put(disclosureKey(id), !disclosed(id, initial));
        click();
        // Heights changed, so the whole card has to be laid out again
        layout();
    }

    /**
     * The eased rotation of one disclosure chevron, seeded to its resting state
     * so the first frame after a rebuild does not replay the animation.
     */
    private float chevron(String id, boolean open) {
        String key = disclosureKey(id);
        float next = approach(chevronT.getOrDefault(key, open ? 1f : 0f), open ? 1f : 0f, HOVER_MS);
        chevronT.put(key, next);
        return next;
    }

    // --- Button rows (footer, buttons component, confirm modal all share this) ---

    private int rowHeightFor(List<MenuComponent.Button> buttons) {
        boolean twoLine = buttons.stream().anyMatch(b -> !b.desc().isEmpty());
        return buttonH() + (twoLine ? 10 : 0);
    }

    private int footerCols() {
        return Math.clamp(doc.footer().size(), 1, 3);
    }

    private int footerBlockH() {
        int cols = footerCols();
        int rows = (doc.footer().size() + cols - 1) / cols;
        return rows * (rowHeightFor(doc.footer()) + GAP) - GAP;
    }

    private int footerTop() {
        return cardY + cardH - pad() / 2 - footerBlockH();
    }

    private void drawButtons(GuiGraphicsExtractor g, List<MenuComponent.Button> buttons,
                             int x, int y, int width, int cols, int rowH, int mouseX, int mouseY) {
        int bw = (width - (cols - 1) * GAP) / cols;
        for (int i = 0; i < buttons.size(); i++) {
            MenuComponent.Button button = buttons.get(i);
            int bx = x + (i % cols) * (bw + GAP);
            int by = y + (i / cols) * (rowH + GAP);
            boolean hovered = inBox(mouseX, mouseY, bx, by, bw, rowH);
            MenuTheme.button(g, this.font, bx, by, bw, rowH, button, accent(),
                    hovered, hovered && button.id().equals(pressedAction));
            if (hovered && !button.enabled() && !button.disabledReason().isEmpty()) {
                tooltip = List.of(Component.literal(button.disabledReason()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private int hitButton(double mx, double my, int count, int x, int y, int width, int cols, int rowH) {
        int bw = (width - (cols - 1) * GAP) / cols;
        for (int i = 0; i < count; i++) {
            int bx = x + (i % cols) * (bw + GAP);
            int by = y + (i / cols) * (rowH + GAP);
            if (inBox(mx, my, bx, by, bw, rowH)) return i;
        }
        return -1;
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void hint(String text) {
        if (!text.isEmpty()) tooltip = List.of(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    private void reason(String text) {
        if (!text.isEmpty()) tooltip = List.of(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    /**
     * The one piece of chrome text the client owns on this screen. A chevron is
     * a small target and a silent one; naming the control is what keeps a
     * collapsed section from reading as an empty one.
     */
    private void disclosureHint(boolean open) {
        hint(Component.translatable(open ? "screen.coi.menu_collapse" : "screen.coi.menu_expand").getString());
    }

    private void lines(List<String> body, String lead) {
        List<Component> out = new ArrayList<>();
        if (!lead.isEmpty()) out.add(Component.literal(lead));
        body.forEach(line -> out.add(Component.literal(line).withStyle(ChatFormatting.GRAY)));
        if (!out.isEmpty()) tooltip = out;
    }

    // --- Parts ---

    /**
     * One laid-out piece of the document, positioned in content space: the
     * render loop adds {@code viewTop - scrollY}. Heights are decided once,
     * here, so the scrollbar and the hit test can never disagree with what was
     * drawn.
     */
    private abstract class Part {
        int y;
        int height;

        /**
         * Eased hover, and the eased gauge fill — one of each is all any part
         * has needed so far; the parts that hold several keep their own arrays.
         */
        float hoverT;
        float shownT;

        abstract void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY);

        boolean click(double mx, double my, int x, int top) {
            return false;
        }

        float hover(boolean hovered) {
            hoverT = approach(hoverT, hovered ? 1f : 0f, HOVER_MS);
            return hoverT;
        }

        double shown(double target) {
            shownT = approach(shownT, (float) target, GAUGE_MS);
            return shownT;
        }
    }

    private void add(Part part) {
        part.y = contentH;
        parts.add(part);
        contentH += part.height;
    }

    private void buildParts() {
        parts.clear();
        contentH = 0;

        for (MenuDocument.Section section : doc.sections()) {
            if (!section.title().isEmpty()) add(new HeadingPart(section.title()));
            for (MenuComponent component : section.components()) {
                if (component instanceof MenuComponent.Heading heading) {
                    add(new HeadingPart(heading));
                    // A collapsed section keeps its heading and drops its body;
                    // the chevron is the only way back to it
                    if (heading.collapsible() && !disclosed(heading.key(), !heading.collapsed())) break;
                    continue;
                }
                addComponent(component);
            }
            add(new SpacerPart(compact() ? 4 : 8));
        }
        if (parts.isEmpty()) {
            add(new MessagePart(Component.translatable("screen.coi.menu_empty").getString()));
        }
    }

    private void addComponent(MenuComponent component) {
        switch (component) {
            case MenuComponent.Text text -> add(new TextPart(text));
            case MenuComponent.Note note -> add(new NotePart(note));
            case MenuComponent.Stat stat -> add(new StatPart(stat));
            case MenuComponent.Kv kv -> add(new KvPart(kv));
            case MenuComponent.Checklist checklist -> add(new ChecklistPart(checklist));
            case MenuComponent.Button button -> add(new ButtonsPart(List.of(button), 1));
            case MenuComponent.Buttons buttons -> {
                if (!buttons.buttons().isEmpty()) add(new ButtonsPart(buttons.buttons(), buttons.columns()));
            }
            case MenuComponent.Toggle toggle -> add(new TogglePart(toggle));
            case MenuComponent.ListView list -> addList(list);
            case MenuComponent.Grid grid -> {
                if (!grid.cells().isEmpty()) add(new GridPart(grid));
            }
            case MenuComponent.Input input -> add(new InputPart(input));
            case MenuComponent.Divider divider -> add(new DividerPart(divider.label()));
            case MenuComponent.Spacer spacer -> add(new SpacerPart(spacer.size()));
            case MenuComponent.Hero hero -> add(new HeroPart(hero));
            case MenuComponent.Details details -> add(new DetailsPart(details));
            case MenuComponent.Steps steps -> {
                if (!steps.items().isEmpty()) add(new StepsPart(steps));
            }
            case MenuComponent.Chips chips -> {
                if (!chips.items().isEmpty()) add(new ChipsPart(chips));
            }
            case MenuComponent.Panels panels -> {
                if (!panels.cells().isEmpty()) add(new PanelsPart(panels));
            }
            // A heading nested anywhere but at the head of its section still
            // draws; only the section-level collapse is handled in buildParts
            case MenuComponent.Heading heading -> add(new HeadingPart(heading));
        }
    }

    /**
     * A list flattens into the outer scroll rather than growing a scrollbar of
     * its own: one card, one scrollbar. That is also why {@code maxVisible} is
     * advisory here — nothing is hidden behind a page control the player would
     * have to find first.
     */
    private void addList(MenuComponent.ListView list) {
        String query = "";
        if (list.searchable()) {
            SearchPart search = new SearchPart(list);
            add(search);
            query = search.box.getValue().trim().toLowerCase(Locale.ROOT);
        }
        int shown = 0;
        for (MenuComponent.Row row : list.rows()) {
            if (!matches(row, query)) continue;
            add(new RowPart(row));
            shown++;
        }
        if (shown == 0) {
            add(new MessagePart(emptyText(list, query)));
        }
    }

    private static String emptyText(MenuComponent.ListView list, String query) {
        if (!query.isEmpty()) {
            return Component.translatable("screen.coi.menu_no_results", query).getString();
        }
        return list.empty().isEmpty()
                ? Component.translatable("screen.coi.menu_list_empty").getString()
                : list.empty();
    }

    private static boolean matches(MenuComponent.Row row, String query) {
        if (query.isEmpty()) return true;
        return row.title().toLowerCase(Locale.ROOT).contains(query)
                || row.subtitle().toLowerCase(Locale.ROOT).contains(query)
                || row.badge().toLowerCase(Locale.ROOT).contains(query);
    }

    private EditBox field(String key, Supplier<EditBox> factory) {
        return fields.computeIfAbsent(key, ignored -> factory.get());
    }

    /**
     * Every text field on this card is drawn borderless over
     * {@link MenuTheme#field}: the vanilla chrome is a black box in a white
     * border, which reads as a hole punched through the card.
     */
    private void drawField(GuiGraphicsExtractor g, EditBox box, int x, int y, int w, int mouseX, int mouseY) {
        MenuTheme.field(g, x, y, w, FIELD_H, box == focusedField, accent());
        box.setX(x + 4);
        box.setY(y + (FIELD_H - 8) / 2);
        box.setWidth(Math.max(8, w - 8));
        box.extractRenderState(g, mouseX, mouseY, 0f);
    }

    // --- Rendering ---

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        if (docRevision != ClientMenuState.revision()) rebuild();
        if (doc == null) {
            super.extractRenderState(g, mouseX, mouseY, partial);
            return;
        }

        long now = System.currentTimeMillis();
        // A first frame, or one after a long stall, must not jump every tween
        frameDelta = lastFrameAt == 0 ? 0f : Math.min(100f, now - lastFrameAt);
        lastFrameAt = now;

        g.fill(0, 0, this.width, this.height, CoiStyle.BACKDROP);
        tooltip = null;

        CoiStyle.drawCard(g, cardX, cardY, cardW, cardH);
        // CoiStyle's top rule is the mod's gold; a document that named its own
        // accent owns that edge too
        g.fill(cardX, cardY, cardX + cardW, cardY + 1, accent());
        drawWatermark(g);

        drawHeader(g, mouseX, mouseY);
        drawNotice(g);
        drawContent(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);

        if (pending != null) {
            drawConfirm(g, mouseX, mouseY);
        } else if (tooltip != null) {
            g.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }

        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    /**
     * The document's pathway emblem, huge and almost invisible behind the top
     * right of the card. Clipped to the card, so a big emblem on a short
     * document does not bleed onto the backdrop.
     */
    private void drawWatermark(GuiGraphicsExtractor g) {
        if (doc.icon().kind() != MenuIcon.Kind.PATHWAY) return;
        g.enableScissor(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1);
        MenuTheme.watermark(g, this.font, doc.icon(), cardX + cardW - WATERMARK - 4, cardY + 4, WATERMARK);
        g.disableScissor();
    }

    private void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int left = cardX + pad();
        int right = cardX + cardW - pad();
        int top = cardY + pad();

        // The accent band: the header is the one block that belongs to the
        // document's own colour rather than to the card
        g.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + headerH - 3,
                MenuTheme.withAlpha(accent(), 0.06f));

        if (doc.back()) {
            boolean hovered = inBox(mouseX, mouseY, left, top, 12, 11);
            MenuTheme.chevron(g, left + 1, top + 1, hovered ? accent() : CoiStyle.TEXT_MUTED);
            left += 15;
        }
        if (doc.closable()) {
            int closeX = right - 8;
            boolean hovered = inBox(mouseX, mouseY, closeX - 2, top, 12, 11);
            MenuTheme.cross(g, closeX, top + 1, hovered ? MenuTheme.DANGER : CoiStyle.TEXT_MUTED);
            right -= 14;
        }

        if (MenuTheme.drawIcon(g, this.font, doc.icon(), left, top, ICON, 1f)) {
            left += ICON + 6;
        }

        int textW = Math.max(20, right - left);
        int textY = doc.subtitle().isEmpty() ? top + (ICON - 8) / 2 : top + 1;
        g.text(this.font, this.font.plainSubstrByWidth(doc.title(), textW), left, textY, accent(), true);
        if (!doc.subtitle().isEmpty()) {
            g.text(this.font, this.font.plainSubstrByWidth(doc.subtitle(), textW), left, textY + 11,
                    CoiStyle.TEXT_MUTED, false);
        }
        MenuTheme.hairline(g, cardX + pad(), cardY + headerH - 3, contentW());
    }

    /**
     * The document's own one-line answer to the click that produced it. It
     * stays for the document's life rather than timing out, so the card never
     * reflows under the cursor; only the arrival highlight is transient.
     */
    private void drawNotice(GuiGraphicsExtractor g) {
        if (noticeLines.isEmpty()) return;
        int color = MenuTheme.toastColor(doc.toast().style(), accent());
        int x = cardX + pad();
        int y = cardY + headerH;
        float flash = 0f;
        if (!HudConfig.getSettings().epilepsyMode) {
            long age = System.currentTimeMillis() - adoptedAt;
            if (age < NOTICE_FLASH_MS) flash = 1f - age / (float) NOTICE_FLASH_MS;
        }
        g.fill(x, y, x + contentW(), y + noticeH - 4, MenuTheme.withAlpha(color, 0.10f + 0.18f * flash));
        g.fill(x, y, x + 2, y + noticeH - 4, color);
        int lineY = y + 5;
        for (FormattedCharSequence line : noticeLines) {
            g.text(this.font, line, x + 9, lineY, color, false);
            lineY += 9;
        }
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean inView = mouseY >= viewTop && mouseY < viewBottom
                && mouseX >= cardX && mouseX < cardX + cardW;
        int hoverY = inView ? mouseY : Integer.MIN_VALUE;

        g.enableScissor(cardX + 1, viewTop, cardX + cardW - 1, viewBottom);
        int originX = cardX + pad();
        for (Part part : parts) {
            int top = viewTop + part.y - scrollY;
            if (top + part.height < viewTop || top > viewBottom) continue;
            part.render(g, originX, top, mouseX, hoverY);
        }
        g.disableScissor();

        if (maxScroll() > 0) drawScrollbar(g, mouseX, mouseY);
    }

    // --- Scrollbar (the handle is grabbable, not decoration) ---

    private int trackX() {
        return cardX + cardW - 5;
    }

    private int trackTop() {
        return viewTop + 1;
    }

    private int trackH() {
        return viewH() - 2;
    }

    private int handleH() {
        int track = trackH();
        return Math.clamp((long) viewH() * track / Math.max(1, contentH), Math.min(14, track), track);
    }

    private int handleY() {
        return trackTop() + (trackH() - handleH()) * scrollY / Math.max(1, maxScroll());
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (trackH() <= 0) return;
        g.fill(trackX(), trackTop(), trackX() + 3, trackTop() + trackH(), CoiStyle.SCROLL_TRACK);
        boolean grabbed = draggingScroll || onHandle(mouseX, mouseY);
        g.fill(trackX(), handleY(), trackX() + 3, handleY() + handleH(),
                MenuTheme.withAlpha(accent(), grabbed ? 0.85f : 0.55f));
    }

    private boolean onTrack(double mx, double my) {
        // Three pixels of bar is a two-pixel target once the gui scale is 1, so
        // the grab box is deliberately wider than the paint
        return maxScroll() > 0 && mx >= trackX() - 3 && mx < trackX() + 6
                && my >= trackTop() && my < trackTop() + trackH();
    }

    private boolean onHandle(double mx, double my) {
        return onTrack(mx, my) && my >= handleY() && my < handleY() + handleH();
    }

    private void scrollTo(double handleTop) {
        int span = trackH() - handleH();
        if (span <= 0) {
            scrollY = 0;
            return;
        }
        scrollY = Mth.clamp((int) Math.round((handleTop - trackTop()) * maxScroll() / span), 0, maxScroll());
    }

    private void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (doc.footer().isEmpty()) return;
        int y = footerTop();
        MenuTheme.hairline(g, cardX + pad(), y - pad() / 2, contentW());
        drawButtons(g, doc.footer(), cardX + pad(), y, contentW(), footerCols(),
                rowHeightFor(doc.footer()), mouseX, mouseY);
    }

    // --- Confirm modal ---

    private int confirmW() {
        return Math.min(CONFIRM_MAX_W, Math.max(160, this.width - 20));
    }

    private int confirmH() {
        return 12 + 9 + 6 + pending.body().size() * 9 + 10 + buttonH() + 12;
    }

    private int confirmX() {
        return (this.width - confirmW()) / 2;
    }

    private int confirmY() {
        return (this.height - confirmH()) / 2;
    }

    private List<MenuComponent.Button> confirmButtons() {
        String label = pending.confirm().confirmLabel().isEmpty()
                ? Component.translatable("screen.coi.menu_confirm_ok").getString()
                : pending.confirm().confirmLabel();
        MenuComponent.ButtonStyle style = pending.style() == MenuComponent.ButtonStyle.DANGER
                ? MenuComponent.ButtonStyle.DANGER : MenuComponent.ButtonStyle.PRIMARY;
        return List.of(
                new MenuComponent.Button("__cancel", Component.translatable("gui.cancel").getString(), "",
                        MenuComponent.ButtonStyle.SECONDARY, true, "", null, MenuIcon.NONE),
                new MenuComponent.Button("__confirm", label, "", style, true, "", null, MenuIcon.NONE));
    }

    private void drawConfirm(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int x = confirmX();
        int y = confirmY();
        int h = confirmH();
        CoiStyle.drawCard(g, x, y, confirmW(), h);
        g.fill(x, y, x + confirmW(), y + 1, accent());

        String title = pending.confirm().title().isEmpty()
                ? Component.translatable("screen.coi.menu_confirm_title").getString()
                : pending.confirm().title();
        g.text(this.font, this.font.plainSubstrByWidth(title, confirmW() - 24), x + 12, y + 12, accent(), true);

        int lineY = y + 27;
        for (FormattedCharSequence line : pending.body()) {
            g.text(this.font, line, x + 12, lineY, CoiStyle.TEXT_BODY, false);
            lineY += 9;
        }
        drawButtons(g, confirmButtons(), x + 12, y + h - 12 - buttonH(), confirmW() - 24, 2,
                buttonH(), mouseX, mouseY);
    }

    private boolean clickConfirm(double mx, double my) {
        int index = hitButton(mx, my, 2, confirmX() + 12, confirmY() + confirmH() - 12 - buttonH(),
                confirmW() - 24, 2, buttonH());
        if (index == 0) {
            pending = null;
        } else if (index == 1) {
            String action = pending.action();
            pending = null;
            fire(action, null);
        }
        return true;
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (doc == null) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();

        if (pending != null) return clickConfirm(mx, my);
        if (clickHeader(mx, my)) return true;
        if (clickFooter(mx, my)) return true;

        if (onTrack(mx, my)) {
            draggingScroll = true;
            // Grabbing the handle keeps the point under the cursor; clicking the
            // bare track centres the handle there instead of paging
            scrollGrab = onHandle(mx, my) ? (int) (my - handleY()) : handleH() / 2;
            scrollTo(my - scrollGrab);
            return true;
        }

        if (my >= viewTop && my < viewBottom && mx >= cardX && mx < cardX + cardW) {
            focusField(null);
            int originX = cardX + pad();
            for (Part part : parts) {
                int top = viewTop + part.y - scrollY;
                if (my < top || my >= top + part.height) continue;
                part.click(mx, my, originX, top);
                return true;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScroll) {
            scrollTo(event.y() - scrollGrab);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private boolean clickHeader(double mx, double my) {
        int top = cardY + pad();
        if (doc.back() && inBox(mx, my, cardX + pad(), top, 12, 11)) {
            fire(MenuActionPayload.BACK, null);
            return true;
        }
        if (doc.closable() && inBox(mx, my, cardX + cardW - pad() - 10, top, 12, 11)) {
            onClose();
            return true;
        }
        return false;
    }

    private boolean clickFooter(double mx, double my) {
        if (doc.footer().isEmpty()) return false;
        int index = hitButton(mx, my, doc.footer().size(), cardX + pad(), footerTop(),
                contentW(), footerCols(), rowHeightFor(doc.footer()));
        if (index < 0) return false;
        activate(doc.footer().get(index), null);
        return true;
    }

    /**
     * The one gate between a click and the wire: a button carrying a
     * {@code confirm} raises the modal and sends nothing until the player
     * agrees, which is what the plugin's two-step chest confirms become.
     */
    private void activate(MenuComponent.Button button, String value) {
        if (!button.enabled()) return;
        pressedAction = button.id();
        if (button.confirm() != null) {
            click();
            pending = new Confirmation(button.confirm(), button.id(), button.style(),
                    this.font.split(Component.literal(button.confirm().body()), confirmW() - 24));
            return;
        }
        fire(button.id(), value);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        pressedAction = null;
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (pending != null) return true;
        scrollY = Mth.clamp(scrollY - (int) Math.signum(vertical) * SCROLL_STEP, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (pending != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                pending = null;
            } else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                String action = pending.action();
                pending = null;
                fire(action, null);
            }
            return true;
        }
        if (focusedField != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                focusField(null);
                return true;
            }
            if (focusedField.keyPressed(event)) return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && doc != null && doc.back()) {
            fire(MenuActionPayload.BACK, null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (pending == null && focusedField != null && focusedField.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private void focusField(EditBox box) {
        if (focusedField != null && focusedField != box) focusedField.setFocused(false);
        focusedField = box;
        if (box != null) box.setFocused(true);
    }

    // --- Part implementations ---

    /**
     * A section caption. A heading the server decorated carries an icon, a count
     * badge and — when it named {@code collapsed} — a chevron that folds the
     * whole section away.
     */
    private final class HeadingPart extends Part {
        private final String title;
        private final MenuComponent.Heading heading;

        HeadingPart(String title) {
            this.title = title;
            this.heading = null;
            this.height = 15;
        }

        HeadingPart(MenuComponent.Heading heading) {
            this.title = heading.title();
            this.heading = heading;
            this.height = 15;
        }

        private boolean collapsible() {
            return heading != null && heading.collapsible();
        }

        private boolean open() {
            return !collapsible() || disclosed(heading.key(), !heading.collapsed());
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (heading == null) {
                MenuTheme.heading(g, MenuScreen.this.font, title, x, top + 3, contentW(), accent());
                return;
            }
            int y = top + 3;
            int left = x;
            int right = x + contentW();
            boolean hovered = collapsible() && inBox(mouseX, mouseY, x, top, contentW(), height);
            int color = MenuTheme.lerpArgb(accent(), MenuTheme.shade(accent(), 0.3f), hover(hovered));

            if (collapsible()) {
                MenuTheme.disclosure(g, left, y, chevron(heading.key(), open()), color);
                left += 10;
            }
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, heading.icon(), left, y - 2, SMALL_ICON, 1f)) {
                left += SMALL_ICON + 3;
            }
            int badgeW = heading.badge().isEmpty() ? 0
                    : MenuTheme.badge(g, MenuScreen.this.font, heading.badge(), right, top + 1,
                    heading.badgeRgb(), accent()) + 5;
            int capW = MenuTheme.headingCaption(g, MenuScreen.this.font, title, left, y,
                    Math.max(10, right - badgeW - left), color);
            MenuTheme.headingRule(g, left + capW + 5, y + 3, right - badgeW, accent());
            if (hovered) disclosureHint(open());
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (collapsible()) toggleDisclosure(heading.key(), !heading.collapsed());
            return true;
        }
    }

    private final class TextPart extends Part {
        private final List<FormattedCharSequence> lines;
        private final int color;
        private final MenuComponent.Align align;

        TextPart(MenuComponent.Text text) {
            this.lines = MenuScreen.this.font.split(Component.literal(text.text()), contentW());
            this.color = MenuTheme.textColor(text.style(), accent());
            this.align = text.align();
            this.height = lines.size() * 9 + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int y = top + 1;
            for (FormattedCharSequence line : lines) {
                int lineX = switch (align) {
                    case LEFT -> x;
                    case CENTER -> x + (contentW() - MenuScreen.this.font.width(line)) / 2;
                    case RIGHT -> x + contentW() - MenuScreen.this.font.width(line);
                };
                g.text(MenuScreen.this.font, line, lineX, y, color, false);
                y += 9;
            }
        }
    }

    private final class NotePart extends Part {
        private final MenuComponent.Note note;
        private final List<FormattedCharSequence> lines;
        private final int color;
        private final int indent;

        NotePart(MenuComponent.Note note) {
            this.note = note;
            this.color = MenuTheme.textColor(note.style(), accent());
            // An icon indents the whole block, not only the line beside it: a
            // paragraph that steps back under its own symbol reads as ragged
            this.indent = note.icon().present() ? 9 + SMALL_ICON + 3 : 9;
            this.lines = MenuScreen.this.font.split(Component.literal(note.text()),
                    Math.max(20, contentW() - indent - 9));
            this.height = 6 + (note.title().isEmpty() ? 0 : 11) + lines.size() * 9 + 6 + 4;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int h = height - 4;
            int textW = Math.max(20, contentW() - indent - 9);
            g.fill(x, top, x + contentW(), top + h, MenuTheme.withAlpha(color, 0.10f));
            g.fill(x, top, x + 2, top + h, color);
            int y = top + 5;
            MenuTheme.drawIcon(g, MenuScreen.this.font, note.icon(), x + 9, y - 2, SMALL_ICON, 1f);
            if (!note.title().isEmpty()) {
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(note.title(), textW), x + indent, y, color, true);
                y += 11;
            }
            for (FormattedCharSequence line : lines) {
                g.text(MenuScreen.this.font, line, x + indent, y, CoiStyle.TEXT_BODY, false);
                y += 9;
            }
        }
    }

    /**
     * Label, value and a gauge, where the gauge can be a bar, ten notches or a
     * 24px arc — the arc is for the one number on the screen that is the point
     * of the screen.
     */
    private final class StatPart extends Part {
        private final MenuComponent.Stat stat;
        private final int color;
        private final boolean ring;

        StatPart(MenuComponent.Stat stat) {
            this.stat = stat;
            this.color = MenuTheme.argb(stat.rgb(), accent());
            this.ring = stat.hasBar() && stat.style() == MenuComponent.GaugeStyle.RING;
            this.height = (ring ? STAT_RING : 11 + (stat.hasBar() ? 8 : 0)) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            // The arc lives at the right edge, so the text line stops short of it
            int right = x + w - (ring ? STAT_RING + 6 : 0);
            int labelX = x;
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, stat.icon(), x, top, SMALL_ICON, 1f)) {
                labelX += SMALL_ICON + 3;
            }

            int deltaW = 0;
            if (!stat.delta().isEmpty()) {
                int dw = MenuScreen.this.font.width(stat.delta());
                g.text(MenuScreen.this.font, stat.delta(), right - dw, top + 1,
                        MenuTheme.deltaColor(stat.delta()), false);
                deltaW = dw + 5;
            }
            int valueW = MenuScreen.this.font.width(stat.value());
            g.text(MenuScreen.this.font, stat.value(), right - deltaW - valueW, top + 1, color, false);
            g.text(MenuScreen.this.font, MenuScreen.this.font.plainSubstrByWidth(stat.label(),
                            Math.max(10, right - deltaW - valueW - 6 - labelX)),
                    labelX, top + 1, CoiStyle.TEXT_BODY, false);

            if (stat.hasBar()) {
                double fill = shown(stat.fraction());
                switch (stat.style()) {
                    case RING -> MenuTheme.ring(g, x + w - STAT_RING / 2, top + STAT_RING / 2,
                            STAT_RING / 2, 3, fill, color);
                    case SEGMENTS -> {
                        MenuTheme.segments(g, x, top + 12, w, 4,
                                stat.hasCap() ? Math.min(fill, stat.cap()) : fill, color, 10);
                        if (stat.hasCap()) MenuTheme.capBand(g, x, top + 12, w, 4, stat.cap(), color);
                    }
                    case BAR -> {
                        if (stat.hasCap()) MenuTheme.gauge(g, x, top + 12, w, 4, fill, color, stat.cap());
                        else MenuTheme.gauge(g, x, top + 12, w, 4, fill, color);
                    }
                }
            }
            if (!stat.hint().isEmpty() && inBox(mouseX, mouseY, x, top, w, height)) hint(stat.hint());
        }
    }

    private final class KvPart extends Part {
        private final List<MenuComponent.KvRow> rows;

        KvPart(MenuComponent.Kv kv) {
            this.rows = kv.rows();
            this.height = rows.size() * 11 + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            int y = top + 1;
            for (MenuComponent.KvRow row : rows) {
                int labelX = x;
                if (MenuTheme.drawIcon(g, MenuScreen.this.font, row.icon(), x, y - 2, SMALL_ICON, 1f)) {
                    labelX += SMALL_ICON + 3;
                }
                int valueW = MenuScreen.this.font.width(row.value());
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(row.label(),
                                Math.max(10, w - valueW - 6 - (labelX - x))),
                        labelX, y, CoiStyle.TEXT_MUTED, false);
                g.text(MenuScreen.this.font, row.value(), x + w - valueW, y,
                        MenuTheme.argb(row.rgb(), CoiStyle.TEXT_BODY), false);
                if (!row.hint().isEmpty() && inBox(mouseX, mouseY, x, y - 1, w, 11)) hint(row.hint());
                y += 11;
            }
        }
    }

    private final class ChecklistPart extends Part {
        private final List<MenuComponent.Check> items;

        ChecklistPart(MenuComponent.Checklist checklist) {
            this.items = checklist.items();
            int h = 3;
            for (MenuComponent.Check item : items) h += item.detail().isEmpty() ? 11 : 20;
            this.height = h;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int y = top + 1;
            for (MenuComponent.Check item : items) {
                // An icon stands in for the glyph entirely: a tick beside a
                // symbol says the same thing twice
                if (!MenuTheme.drawIcon(g, MenuScreen.this.font, item.icon(), x, y - 2, SMALL_ICON, 1f)) {
                    MenuTheme.check(g, x, y, item.state(), item.rgb());
                }
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(item.label(), contentW() - 16),
                        x + 16, y, MenuTheme.argb(item.rgb(), MenuTheme.checkLabelColor(item.state())), false);
                y += 11;
                if (!item.detail().isEmpty()) {
                    g.text(MenuScreen.this.font,
                            MenuScreen.this.font.plainSubstrByWidth(item.detail(), contentW() - 16),
                            x + 16, y, CoiStyle.TEXT_MUTED, false);
                    y += 9;
                }
            }
        }
    }

    private final class ButtonsPart extends Part {
        private final List<MenuComponent.Button> buttons;
        private final int columns;
        private final int rowH;

        ButtonsPart(List<MenuComponent.Button> buttons, int columns) {
            this.buttons = buttons;
            this.columns = Math.max(1, columns);
            this.rowH = rowHeightFor(buttons);
            int rows = (buttons.size() + this.columns - 1) / this.columns;
            this.height = rows * (rowH + GAP) + 2;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            drawButtons(g, buttons, x, top + 1, contentW(), columns, rowH, mouseX, mouseY);
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            int index = hitButton(mx, my, buttons.size(), x, top + 1, contentW(), columns, rowH);
            if (index >= 0) activate(buttons.get(index), null);
            return true;
        }
    }

    private final class TogglePart extends Part {
        private final MenuComponent.Toggle toggle;

        TogglePart(MenuComponent.Toggle toggle) {
            this.toggle = toggle;
            this.height = (toggle.desc().isEmpty() ? 18 : 28) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            int h = height - 3;
            boolean hovered = inBox(mouseX, mouseY, x, top, w, h);
            MenuTheme.panel(g, x, top, w, h, MenuTheme.surface(hover(hovered && toggle.enabled())), 0);

            int switchX = x + w - 26;
            MenuTheme.toggle(g, switchX, top + (h - 10) / 2, toggle.on(), toggle.enabled(), accent());

            String state = stateText();
            int stateW = MenuScreen.this.font.width(state);
            int stateColor = !toggle.enabled() ? CoiStyle.INACTIVE
                    : toggle.on() ? accent() : CoiStyle.TEXT_MUTED;
            g.text(MenuScreen.this.font, state, switchX - stateW - 6, top + (h - 8) / 2, stateColor, false);

            int labelX = x + 6;
            int labelY = top + (toggle.desc().isEmpty() ? (h - 8) / 2 : 4);
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, toggle.icon(), labelX, labelY - 2,
                    SMALL_ICON, toggle.enabled() ? 1f : 0.4f)) {
                labelX += SMALL_ICON + 3;
            }
            int labelW = Math.max(10, w - 38 - stateW - (labelX - x - 6));
            int labelColor = toggle.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE;
            g.text(MenuScreen.this.font, MenuScreen.this.font.plainSubstrByWidth(toggle.label(), labelW),
                    labelX, labelY, labelColor, false);
            if (!toggle.desc().isEmpty()) {
                g.text(MenuScreen.this.font, MenuScreen.this.font.plainSubstrByWidth(toggle.desc(), labelW),
                        labelX, top + 15, CoiStyle.TEXT_MUTED, false);
            }
            if (hovered && !toggle.enabled()) reason(toggle.disabledReason());
        }

        private String stateText() {
            if (toggle.on()) {
                return toggle.onText().isEmpty()
                        ? Component.translatable("screen.coi.menu_on").getString() : toggle.onText();
            }
            return toggle.offText().isEmpty()
                    ? Component.translatable("screen.coi.menu_off").getString() : toggle.offText();
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (toggle.enabled()) fire(toggle.id(), null);
            return true;
        }
    }

    private final class SearchPart extends Part {
        private final EditBox box;

        SearchPart(MenuComponent.ListView list) {
            this.box = field("search:" + list.id(), () -> {
                EditBox created = new EditBox(MenuScreen.this.font, 0, 0, 100, FIELD_H,
                        Component.translatable("screen.coi.menu_search"));
                created.setMaxLength(48);
                created.setBordered(false);
                created.setHint(Component.translatable("screen.coi.menu_search")
                        .withStyle(ChatFormatting.DARK_GRAY));
                // Relaying out on every keystroke is what makes the filter live.
                // The box itself outlives the layout, so the caret survives it.
                created.setResponder(ignored -> MenuScreen.this.layout());
                return created;
            });
            this.height = 22;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            drawField(g, box, x, top, contentW(), mouseX, mouseY);
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            focusField(box);
            return true;
        }
    }

    private final class RowPart extends Part {
        private final MenuComponent.Row row;

        RowPart(MenuComponent.Row row) {
            this.row = row;
            this.height = (row.subtitle().isEmpty() ? 20 : 28) + (row.hasFraction() ? 4 : 0) + 2;
        }

        private boolean clickable() {
            return row.enabled() && !row.action().isEmpty();
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            int h = height - 2;
            boolean hovered = inBox(mouseX, mouseY, x, top, w, h);
            int tint = MenuTheme.argb(row.rgb(), accent());

            MenuTheme.panel(g, x, top, w, h, MenuTheme.surface(hover(hovered && clickable())), 0);
            g.fill(x, top + 1, x + 2, top + h - 1, MenuTheme.withAlpha(tint, row.enabled() ? 0.9f : 0.3f));

            int textX = x + 8;
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, row.icon(), textX, top + (h - ICON) / 2, ICON,
                    row.enabled() ? 1f : 0.4f)) {
                textX += ICON + 6;
            }

            int rightEdge = x + w - 6;
            int badgeY = top + (row.meta().isEmpty() ? (h - 11) / 2 : 4);
            if (!row.badge().isEmpty()) {
                rightEdge -= MenuTheme.badge(g, MenuScreen.this.font, row.badge(), rightEdge,
                        badgeY, row.badgeRgb(), accent()) + 5;
            }
            if (!row.meta().isEmpty()) {
                int metaW = MenuScreen.this.font.width(row.meta());
                g.text(MenuScreen.this.font, row.meta(), x + w - 6 - metaW, badgeY + 12,
                        CoiStyle.TEXT_MUTED, false);
                rightEdge = Math.min(rightEdge, x + w - 6 - metaW - 5);
            }

            int textW = Math.max(10, rightEdge - textX);
            int titleColor = row.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE;
            int titleY = row.subtitle().isEmpty() ? top + (h - 8 - (row.hasFraction() ? 4 : 0)) / 2 : top + 4;
            g.text(MenuScreen.this.font, MenuScreen.this.font.plainSubstrByWidth(row.title(), textW),
                    textX, titleY, titleColor, false);
            if (!row.subtitle().isEmpty()) {
                g.text(MenuScreen.this.font, MenuScreen.this.font.plainSubstrByWidth(row.subtitle(), textW),
                        textX, titleY + 11, CoiStyle.TEXT_MUTED, false);
            }
            if (row.hasFraction()) {
                MenuTheme.gauge(g, textX, top + h - 6, Math.max(10, rightEdge - textX), 2,
                        shown(row.fraction()), tint);
            }
            if (hovered) {
                if (!row.enabled() && !row.disabledReason().isEmpty()) reason(row.disabledReason());
                else lines(row.tooltip(), "");
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (clickable()) fire(row.action(), null);
            return true;
        }
    }

    /**
     * Icon tiles. v2 made the cell's own fields visible at last: the title is a
     * caption under the tile, the badge a corner pill and the colour a bottom
     * edge — all three were parsed and thrown away before.
     */
    private final class GridPart extends Part {
        /**
         * How wide a captioned cell gets regardless of its tile. An ability name clipped to a 40px
         * tile is about seven characters, which is not a name — so when a grid carries captions the
         * cell is sized for the words and the tile is centred inside it.
         */
        private static final int CAPTION_MIN_W = 78;
        private static final int CAPTION_MAX_LINES = 2;

        private final List<MenuComponent.Row> cells;
        private final int columns;
        private final int tile;
        private final int cellW;
        private final int cellH;
        private final int captionLines;
        private final boolean captions;
        private final float[] hovers;

        GridPart(MenuComponent.Grid grid) {
            this.cells = grid.cells();
            this.hovers = new float[grid.cells().size()];
            // `columns` is a hint, not a instruction, in both directions.
            //
            // Downward it always was: a document asking for more columns than the
            // card can hold gets fewer, rather than a row running off the edge.
            // Upward is new — a grid that honoured "5" literally on a wide card
            // drew five tiles at their authored size and left the rest of the row
            // empty, turning width the player has into vertical scroll they did
            // not want. So the row fills at the authored tile size, never past
            // the number of cells there actually are.
            int wanted = Math.max(1, grid.columns());
            int preferred = maxTile(grid.size());
            this.captions = cells.stream().anyMatch(cell -> !cell.title().isEmpty());

            // A captioned cell is as wide as its words need; a bare one is just its tile.
            int wantW = captions ? Math.max(preferred, CAPTION_MIN_W) : preferred;
            int minW = captions ? Math.max(MIN_TILE, CAPTION_MIN_W / 2) : MIN_TILE;
            int fitsAtWanted = Math.max(1, (contentW() + GAP) / (wantW + GAP));
            int floor = Math.max(1, (contentW() + GAP) / (minW + GAP));
            this.columns = Math.clamp(Math.max(wanted, fitsAtWanted), 1,
                    Math.min(floor, Math.max(1, cells.size())));
            this.cellW = Math.max(minW, (contentW() - (columns - 1) * GAP) / columns);
            this.tile = Math.clamp(Math.min(cellW, preferred), MIN_TILE, preferred);

            // Every row is as tall as the wordiest caption in the grid, so the tiles stay on a
            // line even when one name wraps and its neighbours do not.
            int lines = 1;
            if (captions) {
                for (MenuComponent.Row cell : cells) {
                    if (cell.title().isEmpty()) continue;
                    lines = Math.max(lines, MenuScreen.this.font
                            .split(Component.literal(cell.title()), cellW).size());
                }
                lines = Math.min(lines, CAPTION_MAX_LINES);
            }
            this.captionLines = lines;
            this.cellH = tile + (captions ? captionLines * 9 + 3 : 0);
            int rows = (cells.size() + columns - 1) / columns;
            this.height = rows * (cellH + GAP) + 2;
        }

        private int maxTile(MenuComponent.TileSize size) {
            return switch (size) {
                case SMALL -> 18;
                case MEDIUM -> 28;
                case LARGE -> 40;
            };
        }

        private int cellX(int x, int index) {
            return x + (index % columns) * (cellW + GAP);
        }

        /** The tile sits centred in its cell, which may be wider to fit the caption. */
        private int tileX(int x, int index) {
            return cellX(x, index) + (cellW - tile) / 2;
        }

        private int cellY(int top, int index) {
            return top + 1 + (index / columns) * (cellH + GAP);
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.Row cell = cells.get(i);
                int cx = tileX(x, i);
                int cy = cellY(top, i);
                boolean hovered = inBox(mouseX, mouseY, cellX(x, i), cy, cellW, cellH);
                int border = cell.enabled() ? MenuTheme.argb(cell.rgb(), CoiStyle.BORDER) : MenuTheme.BORDER_OFF;
                hovers[i] = approach(hovers[i], hovered && cell.enabled() ? 1f : 0f, HOVER_MS);
                MenuTheme.panel(g, cx, cy, tile, tile, MenuTheme.surface(hovers[i]), border);
                // The art grows with the tile. Drawing a fixed 16 inside a 40px LARGE tile left
                // the icon floating in the middle of its own box, which read as the tile being
                // empty rather than as a large tile.
                int art = Math.max(ICON, tile - 12);
                int inset = (tile - art) / 2;
                MenuTheme.drawIcon(g, MenuScreen.this.font, cell.icon(), cx + inset, cy + inset, art,
                        cell.enabled() ? 1f : 0.4f);
                if (cell.rgb() != 0) {
                    g.fill(cx + 1, cy + tile - 2, cx + tile - 1, cy + tile,
                            MenuTheme.withAlpha(MenuTheme.argb(cell.rgb(), accent()),
                                    cell.enabled() ? 0.9f : 0.3f));
                }
                if (!cell.badge().isEmpty()) drawBadge(g, cell, cx, cy);
                if (captions && !cell.title().isEmpty()) {
                    int capX = cellX(x, i);
                    int capColor = cell.enabled() ? CoiStyle.TEXT_MUTED : CoiStyle.INACTIVE;
                    List<net.minecraft.util.FormattedCharSequence> wrapped =
                            MenuScreen.this.font.split(Component.literal(cell.title()), cellW);
                    for (int line = 0; line < Math.min(wrapped.size(), captionLines); line++) {
                        var seq = wrapped.get(line);
                        int lw = MenuScreen.this.font.width(seq);
                        g.text(MenuScreen.this.font, seq, capX + (cellW - lw) / 2,
                                cy + tile + 3 + line * 9, capColor, false);
                    }
                }
                if (hovered) {
                    if (!cell.enabled() && !cell.disabledReason().isEmpty()) reason(cell.disabledReason());
                    else lines(cell.tooltip(), cell.title());
                }
            }
        }

        /**
         * A pill needs room for its own padding; on a small tile it becomes a
         * corner dot, which still says "this one is different" without covering
         * the art the grid exists for.
         */
        private void drawBadge(GuiGraphicsExtractor g, MenuComponent.Row cell, int cx, int cy) {
            if (tile >= 28) {
                MenuTheme.badge(g, MenuScreen.this.font, cell.badge(), cx + tile - 1, cy + 1,
                        cell.badgeRgb(), accent());
            } else {
                g.fill(cx + tile - 4, cy + 1, cx + tile - 1, cy + 4,
                        MenuTheme.argb(cell.badgeRgb(), accent()));
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            for (int i = 0; i < cells.size(); i++) {
                if (inBox(mx, my, cellX(x, i), cellY(top, i), cellW, cellH)) {
                    MenuComponent.Row cell = cells.get(i);
                    if (cell.enabled() && !cell.action().isEmpty()) fire(cell.action(), null);
                    return true;
                }
            }
            return true;
        }
    }

    private final class InputPart extends Part {
        private final MenuComponent.Input input;
        private final EditBox box;
        private final MenuComponent.Button submit;
        private final int labelH;
        private final int submitW;

        InputPart(MenuComponent.Input input) {
            this.input = input;
            this.labelH = input.label().isEmpty() ? 0 : 11;
            String label = input.submitLabel().isEmpty()
                    ? Component.translatable("screen.coi.menu_submit").getString() : input.submitLabel();
            this.submit = new MenuComponent.Button(input.submit(), label, "",
                    MenuComponent.ButtonStyle.PRIMARY, !input.submit().isEmpty(), "", null, MenuIcon.NONE);
            this.submitW = Math.min(contentW() / 2, MenuScreen.this.font.width(label) + 16);

            this.box = field("input:" + input.id(), () -> {
                EditBox created = new EditBox(MenuScreen.this.font, 0, 0, 100, FIELD_H,
                        Component.literal(input.label()));
                created.setMaxLength(input.maxLength());
                created.setBordered(false);
                if (!input.placeholder().isEmpty()) {
                    created.setHint(Component.literal(input.placeholder()).withStyle(ChatFormatting.DARK_GRAY));
                }
                created.setValue(input.value());
                return created;
            });

            this.height = labelH + FIELD_H + (input.hint().isEmpty() ? 0 : 10) + 5;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (labelH > 0) {
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(input.label(), contentW()),
                        x, top + 1, CoiStyle.TEXT_BODY, false);
            }
            int fieldY = top + labelH;
            drawField(g, box, x, fieldY, contentW() - submitW - GAP, mouseX, mouseY);

            int bx = x + contentW() - submitW;
            boolean hovered = inBox(mouseX, mouseY, bx, fieldY, submitW, FIELD_H);
            MenuTheme.button(g, MenuScreen.this.font, bx, fieldY, submitW, FIELD_H, submit, accent(), hovered, false);

            if (!input.hint().isEmpty()) {
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(input.hint(), contentW()),
                        x, fieldY + FIELD_H + 2, CoiStyle.TEXT_MUTED, false);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            int fieldY = top + labelH;
            if (inBox(mx, my, x + contentW() - submitW, fieldY, submitW, FIELD_H)) {
                if (submit.enabled()) fire(input.submit(), box.getValue());
                return true;
            }
            if (inBox(mx, my, x, fieldY, contentW() - submitW - GAP, FIELD_H)) {
                focusField(box);
            }
            return true;
        }
    }

    private final class MessagePart extends Part {
        private final String text;

        MessagePart(String text) {
            this.text = text;
            this.height = 20;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            String line = MenuScreen.this.font.plainSubstrByWidth(text, contentW());
            g.text(MenuScreen.this.font, line, x + (contentW() - MenuScreen.this.font.width(line)) / 2,
                    top + 6, CoiStyle.TEXT_MUTED, false);
        }
    }

    private final class DividerPart extends Part {
        private final String label;

        DividerPart(String label) {
            this.label = label;
            this.height = label.isEmpty() ? 9 : 13;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (label.isEmpty()) {
                MenuTheme.hairline(g, x, top + 4, contentW());
            } else {
                MenuTheme.labelledRule(g, MenuScreen.this.font, label, x, top + 2, contentW());
            }
        }
    }

    private final class SpacerPart extends Part {
        SpacerPart(int size) {
            this.height = size;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
        }
    }

    // --- Vocabulary v2 ---

    /**
     * The screen's identity block. Everything else on the card is a band of the
     * same weight; this one is a panel in the document's own colour, with the
     * title at 1.5× and the one number that matters as a bar or an arc.
     */
    private final class HeroPart extends Part {
        private final MenuComponent.Hero hero;
        private final int color;
        private final boolean ringGauge;
        private final boolean barGauge;
        private final int core;
        private final float[] hovers;

        HeroPart(MenuComponent.Hero hero) {
            this.hero = hero;
            this.color = MenuTheme.argb(hero.rgb(), accent());
            this.ringGauge = hero.hasFraction() && hero.style() == MenuComponent.HeroStyle.RING;
            this.barGauge = hero.hasFraction() && hero.style() == MenuComponent.HeroStyle.PLAIN;
            this.hovers = new float[hero.chips().size()];
            int titleBlock = 12 + (hero.subtitle().isEmpty() ? 0 : 11);
            // The ring is drawn *beside* the text, so it widens the block's
            // minimum rather than stacking under it — the contract's additive
            // formula would leave a 34px empty band under a gauge that is not
            // there
            this.core = Math.max(Math.max(HERO_ICON, titleBlock), ringGauge ? HERO_RING + 6 : 0);
            this.height = 10 + core + (barGauge ? 10 : 0)
                    + (hero.chips().isEmpty() ? 0 : MenuTheme.CHIP_H + GAP) + 8 + 4;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            int h = height - 4;
            MenuTheme.panel(g, x, top, w, h, MenuTheme.withAlpha(accent(), 0.08f),
                    MenuTheme.withAlpha(accent(), 0.30f));

            int left = x + 8;
            int right = x + w - 8;
            int coreTop = top + 10;

            // The ring claims the right edge *before* the badge is placed. Both used
            // to anchor there independently, so a hero with a gauge and a state word
            // — the mythical form screen, every time it is recovering — drew the
            // badge straight through the ring.
            if (ringGauge) {
                int centreX = x + w - 8 - HERO_RING / 2;
                int centreY = coreTop + core / 2;
                MenuTheme.ring(g, centreX, centreY, HERO_RING / 2, 4, shown(hero.fraction()), color);
                if (!hero.fractionLabel().isEmpty()) {
                    int lw = MenuScreen.this.font.width(hero.fractionLabel());
                    g.text(MenuScreen.this.font, hero.fractionLabel(), centreX - lw / 2, centreY - 4,
                            CoiStyle.TEXT_BODY, false);
                }
                right = x + w - 8 - HERO_RING - 8;
            }
            if (!hero.badge().isEmpty()) {
                right -= MenuTheme.badge(g, MenuScreen.this.font, hero.badge(), right, top + 6,
                        hero.badgeRgb(), accent()) + 6;
            }
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, hero.icon(), left,
                    coreTop + (core - HERO_ICON) / 2, HERO_ICON, 1f)) {
                left += HERO_ICON + 8;
            }

            int textW = Math.max(20, right - left);
            int textY = coreTop + (core - (hero.subtitle().isEmpty() ? 12 : 23)) / 2;
            // plainSubstrByWidth measures at 1×, so the budget is divided by the
            // scale before clipping rather than after
            MenuTheme.scaledText(g, MenuScreen.this.font,
                    MenuScreen.this.font.plainSubstrByWidth(hero.title(), (int) (textW / 1.5f)),
                    left, textY, 1.5f, color, true);
            if (!hero.subtitle().isEmpty()) {
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(hero.subtitle(), textW),
                        left, textY + 14, CoiStyle.TEXT_MUTED, false);
            }

            int y = coreTop + core;
            if (barGauge) {
                if (!hero.fractionLabel().isEmpty()) {
                    int lw = MenuScreen.this.font.width(hero.fractionLabel());
                    g.text(MenuScreen.this.font, hero.fractionLabel(), x + w - 8 - lw, y - 10,
                            CoiStyle.TEXT_MUTED, false);
                }
                MenuTheme.gauge(g, x + 8, y, w - 16, 6, shown(hero.fraction()), color);
                y += 10;
            }
            if (!hero.chips().isEmpty()) drawChips(g, x + 8, y + GAP, w - 16, mouseX, mouseY);
        }

        /**
         * One row, clipped rather than wrapped: the hero is a fixed-height block
         * and the contract caps it at four chips, so a fifth would only make the
         * card jump.
         */
        private void drawChips(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
            int chipX = x;
            for (int i = 0; i < hero.chips().size(); i++) {
                MenuComponent.Chip chip = hero.chips().get(i);
                int cw = MenuTheme.chipWidth(MenuScreen.this.font, chip);
                if (chipX + cw > x + w && chipX > x) break;
                boolean hovered = inBox(mouseX, mouseY, chipX, y, cw, MenuTheme.CHIP_H);
                hovers[i] = approach(hovers[i], hovered ? 1f : 0f, HOVER_MS);
                MenuTheme.chip(g, MenuScreen.this.font, chip, chipX, y, accent(), hovers[i]);
                if (hovered) hint(chip.hint());
                chipX += cw + GAP;
            }
        }
    }

    /**
     * A disclosure row: one line of summary, and the paragraph behind it only
     * when the player asks. This is the component every wall of prose in the
     * system is supposed to move into.
     */
    private final class DetailsPart extends Part {
        private static final int BODY_X = 10;

        private final MenuComponent.Details details;
        private final boolean open;
        private final int bodyColor;
        private final List<List<FormattedCharSequence>> blocks = new ArrayList<>();

        DetailsPart(MenuComponent.Details details) {
            this.details = details;
            this.open = disclosed(details.id(), details.open());
            this.bodyColor = MenuTheme.textColor(details.style(), accent());
            int h = 16;
            if (open) {
                int bodyW = Math.max(20, contentW() - BODY_X);
                for (String block : details.text()) {
                    List<FormattedCharSequence> lines =
                            MenuScreen.this.font.split(Component.literal(block), bodyW);
                    blocks.add(lines);
                    h += lines.size() * 9 + 4;
                }
            }
            this.height = h + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = contentW();
            boolean hovered = inBox(mouseX, mouseY, x, top, w, 16);
            int color = MenuTheme.lerpArgb(CoiStyle.TEXT_BODY, accent(), hover(hovered));

            MenuTheme.disclosure(g, x, top + 4, chevron(details.id(), open), color);
            int left = x + BODY_X;
            if (MenuTheme.drawIcon(g, MenuScreen.this.font, details.icon(), left, top + 1, SMALL_ICON, 1f)) {
                left += SMALL_ICON + 3;
            }
            String summary = MenuScreen.this.font.plainSubstrByWidth(details.summary(),
                    Math.max(10, x + w - left - 10));
            g.text(MenuScreen.this.font, summary, left, top + 3, color, false);
            // The hairline runs from the end of the summary to the far edge, so
            // a collapsed row still reads as a full-width control
            MenuTheme.hairline(g, left + MenuScreen.this.font.width(summary) + 5, top + 7,
                    Math.max(0, x + w - left - MenuScreen.this.font.width(summary) - 5));

            if (hovered) disclosureHint(open);

            int y = top + 16;
            for (List<FormattedCharSequence> block : blocks) {
                for (FormattedCharSequence line : block) {
                    g.text(MenuScreen.this.font, line, x + BODY_X, y, bodyColor, false);
                    y += 9;
                }
                y += 4;
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            // Only the summary row toggles; a click in the open body is not a
            // request to close what the player is reading
            if (my < top + 16) toggleDisclosure(details.id(), details.open());
            return true;
        }
    }

    /**
     * A numbered rail. Six consecutive {@code text} components explaining a
     * sequence become one of these.
     */
    private final class StepsPart extends Part {
        private final MenuComponent.Steps steps;
        private final List<List<FormattedCharSequence>> texts = new ArrayList<>();
        private final int[] itemH;

        StepsPart(MenuComponent.Steps steps) {
            this.steps = steps;
            this.itemH = new int[steps.items().size()];
            int h = 2;
            for (int i = 0; i < steps.items().size(); i++) {
                MenuComponent.Step item = steps.items().get(i);
                List<FormattedCharSequence> lines = item.text().isEmpty() ? List.of()
                        : MenuScreen.this.font.split(Component.literal(item.text()),
                        Math.max(20, contentW() - RAIL_TEXT_X));
                texts.add(lines);
                itemH[i] = 13 + lines.size() * 9 + 4;
                h += itemH[i];
            }
            this.height = h;
        }

        private int itemTop(int top, int index) {
            int y = top + 1;
            for (int i = 0; i < index; i++) y += itemH[i];
            return y;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int count = steps.items().size();
            if (count > 1) {
                MenuTheme.rail(g, x + RAIL_X, itemTop(top, 0) + 6,
                        itemTop(top, count - 1) + 6, accent());
            }
            for (int i = 0; i < count; i++) {
                MenuComponent.Step item = steps.items().get(i);
                int y = itemTop(top, i);
                // An icon replaces the marker outright — a numbered disc beside a
                // symbol is two answers to the same question
                if (!MenuTheme.drawIcon(g, MenuScreen.this.font, item.icon(), x, y, SMALL_ICON, 1f)) {
                    String number = steps.style() == MenuComponent.StepStyle.NUMBERED
                            ? String.valueOf(i + 1) : "";
                    MenuTheme.stepMarker(g, MenuScreen.this.font, x + RAIL_X, y + 6, number,
                            item.done(), accent());
                }
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(item.title(), contentW() - RAIL_TEXT_X),
                        x + RAIL_TEXT_X, y + 2, CoiStyle.TEXT_BODY, false);
                int lineY = y + 13;
                for (FormattedCharSequence line : texts.get(i)) {
                    g.text(MenuScreen.this.font, line, x + RAIL_TEXT_X, lineY, CoiStyle.TEXT_MUTED, false);
                    lineY += 9;
                }
            }
        }
    }

    /**
     * A wrapping row of pills. The layout is decided once, in the constructor,
     * because a wrapping row's height is not knowable from its item count.
     */
    private final class ChipsPart extends Part {
        private final List<MenuComponent.Chip> items;
        private final int[][] spots;
        private final float[] hovers;

        ChipsPart(MenuComponent.Chips chips) {
            this.items = chips.items();
            this.spots = new int[items.size()][3];
            this.hovers = new float[items.size()];
            int w = contentW();
            int x = 0;
            int y = 0;
            for (int i = 0; i < items.size(); i++) {
                int cw = Math.min(w, MenuTheme.chipWidth(MenuScreen.this.font, items.get(i)));
                if (x > 0 && x + cw > w) {
                    x = 0;
                    y += MenuTheme.CHIP_H + GAP;
                }
                spots[i] = new int[]{x, y, cw};
                x += cw + GAP;
            }
            this.height = (items.isEmpty() ? 0 : y + MenuTheme.CHIP_H) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < items.size(); i++) {
                int cx = x + spots[i][0];
                int cy = top + 1 + spots[i][1];
                boolean hovered = inBox(mouseX, mouseY, cx, cy, spots[i][2], MenuTheme.CHIP_H);
                // Per chip, not per part: one shared tween would fade the whole
                // row whenever any one of them is pointed at
                hovers[i] = approach(hovers[i], hovered ? 1f : 0f, HOVER_MS);
                MenuTheme.chip(g, MenuScreen.this.font, items.get(i), cx, cy, accent(), hovers[i]);
                if (hovered) hint(items.get(i).hint());
            }
        }
    }

    /**
     * Side-by-side mini-cards — the one shape the renderer had no answer for,
     * since every other component is a full-width band. A cell with an
     * {@code action} is a button; one without is a read-only stat card.
     */
    private final class PanelsPart extends Part {
        private final List<MenuComponent.PanelCell> cells;
        private final int columns;
        private final int cellW;
        private final int[] rowH;
        private final List<List<FormattedCharSequence>> subs = new ArrayList<>();
        private final float[] hovers;
        private final float[] fills;

        PanelsPart(MenuComponent.Panels panels) {
            this.cells = panels.cells();
            // Columns yield to a cell floor, the same bargain the grid's tile
            // makes: fewer columns beats a cell too narrow to read
            int wanted = Math.clamp(panels.columns(), 1, 3);
            int fits = Math.max(1, (contentW() + GAP) / (MIN_PANEL_W + GAP));
            this.columns = Math.min(wanted, fits);
            this.cellW = (contentW() - (columns - 1) * GAP) / columns;
            this.hovers = new float[cells.size()];
            this.fills = new float[cells.size()];

            int rows = (cells.size() + columns - 1) / columns;
            this.rowH = new int[rows];
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.PanelCell cell = cells.get(i);
                List<FormattedCharSequence> sub = cell.subtitle().isEmpty() ? List.of()
                        : MenuScreen.this.font.split(Component.literal(cell.subtitle()), cellW - 12);
                // Two lines is the whole budget: a mini-card that grows a
                // paragraph is a note wearing the wrong component
                subs.add(sub.size() > 2 ? sub.subList(0, 2) : sub);
                rowH[i / columns] = Math.max(rowH[i / columns], cellHeight(cell, subs.get(i).size()));
            }
            int h = 2;
            for (int r : rowH) h += r + GAP;
            this.height = h;
        }

        private int cellHeight(MenuComponent.PanelCell cell, int subLines) {
            boolean chrome = cell.icon().present() || !cell.badge().isEmpty();
            int h = 6 + (chrome ? ICON + 3 : 0) + 11;
            if (!cell.value().isEmpty()) h += 13;
            h += subLines * 9;
            if (cell.hasFraction()) h += 6;
            return h + 6;
        }

        private int cellX(int x, int index) {
            return x + (index % columns) * (cellW + GAP);
        }

        private int cellY(int top, int index) {
            int y = top + 1;
            for (int r = 0; r < index / columns; r++) y += rowH[r] + GAP;
            return y;
        }

        private boolean clickable(MenuComponent.PanelCell cell) {
            return cell.enabled() && !cell.action().isEmpty();
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.PanelCell cell = cells.get(i);
                int cx = cellX(x, i);
                int cy = cellY(top, i);
                int ch = rowH[i / columns];
                boolean hovered = inBox(mouseX, mouseY, cx, cy, cellW, ch);
                hovers[i] = approach(hovers[i], hovered && clickable(cell) ? 1f : 0f, HOVER_MS);

                int color = MenuTheme.argb(cell.rgb(), accent());
                MenuTheme.panel(g, cx, cy, cellW, ch, MenuTheme.surface(hovers[i]),
                        cell.enabled() ? CoiStyle.BORDER : MenuTheme.BORDER_OFF);
                if (cell.rgb() != 0) {
                    g.fill(cx + 1, cy, cx + cellW - 1, cy + 2,
                            MenuTheme.withAlpha(color, cell.enabled() ? 0.9f : 0.3f));
                }

                int left = cx + 6;
                int right = cx + cellW - 6;
                int y = cy + 6;
                boolean chrome = cell.icon().present() || !cell.badge().isEmpty();
                if (chrome) {
                    MenuTheme.drawIcon(g, MenuScreen.this.font, cell.icon(), left, y, ICON,
                            cell.enabled() ? 1f : 0.4f);
                    if (!cell.badge().isEmpty()) {
                        MenuTheme.badge(g, MenuScreen.this.font, cell.badge(), right, y + 2,
                                cell.badgeRgb(), accent());
                    }
                    y += ICON + 3;
                }

                int textW = cellW - 12;
                g.text(MenuScreen.this.font,
                        MenuScreen.this.font.plainSubstrByWidth(cell.title(), textW), left, y,
                        cell.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE, false);
                y += 11;
                if (!cell.value().isEmpty()) {
                    MenuTheme.scaledText(g, MenuScreen.this.font,
                            MenuScreen.this.font.plainSubstrByWidth(cell.value(), (int) (textW / 1.25f)),
                            left, y, 1.25f, cell.enabled() ? color : CoiStyle.INACTIVE, false);
                    y += 13;
                }
                for (FormattedCharSequence line : subs.get(i)) {
                    g.text(MenuScreen.this.font, line, left, y, CoiStyle.TEXT_MUTED, false);
                    y += 9;
                }
                if (cell.hasFraction()) {
                    fills[i] = approach(fills[i], (float) cell.fraction(), GAUGE_MS);
                    MenuTheme.gauge(g, left, cy + ch - 7, textW, 3, fills[i], color);
                }

                if (!hovered) continue;
                if (!cell.enabled() && !cell.disabledReason().isEmpty()) reason(cell.disabledReason());
                else lines(cell.tooltip(), "");
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            for (int i = 0; i < cells.size(); i++) {
                if (!inBox(mx, my, cellX(x, i), cellY(top, i), cellW, rowH[i / columns])) continue;
                MenuComponent.PanelCell cell = cells.get(i);
                if (clickable(cell)) fire(cell.action(), null);
                return true;
            }
            return true;
        }
    }
}
