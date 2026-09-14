package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientSheetState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.PlateSymbols;
import dev.ua.ikeepcalm.coi.client.menu.ClientMenuState;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.network.ActionPayload;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.util.CoiIcons;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Beyonder character sheet — the client-rendered half of the plugin's
 * InvUI stats page, and the full-size sibling of the character plate on the
 * HUD.
 * <p>
 * Opening announces {@code sheet_open} so the server starts pushing
 * {@code coi-client:sheet} every 60 ticks; closing announces {@code sheet_close}
 * exactly once, whichever way the screen goes away. Everything drawn here is
 * re-read from {@link ClientSheetState} each frame, so those pushes land live.
 * <p>
 * <b>The sheet is drawn as a menu document, not as a screen of its own.</b> It
 * opens every server-authored menu the player will see, so the two have to be
 * the same object: one bounded card on {@link CoiStyle#BACKDROP}, a header
 * carrying the pathway emblem and a close cross, small-caps section headings on
 * the card's own surface, a draggable accent scrollbar inside the card, and a
 * footer of {@link MenuTheme} buttons under a hairline. Every primitive — chip,
 * gauge, panel, toggle, badge — comes from {@code MenuTheme}, so a change to the
 * menus' look reaches the sheet without anybody remembering to make it twice.
 * That is the whole reason this screen owns no paint of its own any more: the
 * old sheet floated a stack of separately bordered cards over a differently dim
 * backdrop with vanilla-ish buttons in the corner, and stepping from it into a
 * menu read as stepping into another program.
 * <p>
 * Two consequences of the layout shape the code:
 * <ul>
 *   <li><b>Geometry is recomputed every frame, during the draw.</b> Section
 *       heights follow the data (a source appears, a condition clears), so the
 *       draw is the layout, and mouse events reuse the last frame's hit boxes —
 *       the same bargain {@code AbilityPickerOverlay} makes.</li>
 *   <li><b>The card's height trails the content by one frame.</b> Measuring
 *       ahead would mean a second pass that can drift from the first, so
 *       {@link #contentHeight} is seeded full instead: the card opens at its
 *       full size and settles down to a short document, rather than opening as
 *       a sliver and snapping out.</li>
 * </ul>
 * The seven sub-menus stay server-side: their cards send
 * {@link ActionPayload#ofOpen(String)} and close the sheet, and the server
 * answers with either its own GUI or a {@code coi-client:menu} document.
 */
public class CharacterSheetScreen extends Screen {

    /**
     * One destination card: the wire target — which is also its lang stem —
     * and the mark that tells it apart at a glance.
     */
    private record Nav(String target, String[] glyph) {
    }

    private static final Nav[] NAV = {
            new Nav("church", SheetGlyphs.CHURCH),
            new Nav("abilities", SheetGlyphs.SPARK),
            new Nav("mythical", SheetGlyphs.BEAST),
            new Nav("uniqueness", SheetGlyphs.GEM),
            new Nav("honorific", SheetGlyphs.CROWN),
            new Nav("map", SheetGlyphs.PIN),
            new Nav("seat", SheetGlyphs.THRONE)
    };

    /**
     * A box the mouse can reach inside the scrolling column: a destination card
     * or the terrain toggle. Rebuilt every frame during the draw, in absolute
     * screen coordinates.
     */
    private record Hit(int x, int y, int w, int h, Runnable action, Component tip) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /**
     * A footer button and what it does. Modelled as a {@link MenuComponent.Button}
     * so {@link MenuTheme#button} draws it exactly as it draws a document's own.
     */
    private record Action(MenuComponent.Button model, Runnable run) {
    }

    private static final int GAP = 4;
    private static final int ICON = 16;
    private static final int SMALL_ICON = 12;
    private static final int WATERMARK = 64;
    private static final int HEADING_H = 15;
    private static final int LINE = 11;
    private static final int HEAD = 32;
    private static final int SYMBOL = 32;
    private static final int SEQ_BADGE_W = 34;
    private static final int SCROLL_STEP = 20;

    /**
     * Hover tints ease over this. Suppressed outright under {@code epilepsyMode}
     * — see {@link #approach}, the one chokepoint that honours the setting, as
     * on the menu screen.
     */
    private static final float HOVER_MS = 120;

    /**
     * How long the sheet waits for the server to answer a destination before giving up and
     * becoming live again. Generous: this is a round trip plus whatever the adapter has to look
     * up, and a sheet that gave up early would close under a player on a slow connection just as
     * their menu arrived.
     */
    private static final long AWAIT_TIMEOUT_MS = 5000;

    /**
     * A vitals row is exactly as tall as its symbol; a row carrying a second
     * muted line (madness with a floor, acting with a cooldown) grows by one.
     */
    private static final int VITAL_ROW_H = SYMBOL;
    private static final int VITAL_NOTE_H = 11;
    private static final int VITAL_GAP = 4;
    private static final int BAR_H = 6;

    private static final int LEDGER_ROW_H = 14;
    private static final int CHIP_GAP = 3;
    private static final int NAV_CARD_H = 30;
    private static final int PARA_LINE = 10;

    /**
     * Two destination columns are only worth it once each card can still hold a
     * readable description; below that one wide column beats two cramped ones.
     */
    private static final int TWO_COLUMN_MIN = 320;

    /**
     * And the width at which a third fits. Each destination card still needs room for its name and
     * a line of what is behind it, so this is 3x the two-column threshold's per-card share rather
     * than simply 1.5x the threshold itself.
     */
    private static final int THREE_COLUMN_MIN = 540;

    /**
     * The bundled 16px glyphs this screen names, one per section heading and one
     * per chip. They are the same {@code CoiIcons.GLYPHS} names a server-authored
     * document uses, so the sheet's headings are marked exactly as a menu's are.
     */
    private static final String GLYPH_HEALTH = "health";
    private static final String GLYPH_GROWTH = "growth";
    private static final String GLYPH_RESTORE = "restore";
    private static final String GLYPH_WARD = "ward";
    private static final String GLYPH_DIVINATION = "divination";
    private static final String GLYPH_DEFENSE = "defense";
    private static final String GLYPH_SOUL = "soul";
    private static final String GLYPH_POWER = "power";
    private static final String GLYPH_RESIST = "resist";
    private static final String GLYPH_AUTHORITY = "authority";
    private static final String GLYPH_COOLDOWN = "cooldown";

    private static final int SKIN_SHEET = 64;
    private static final int SYMBOL_EMPTY = 0xFF4A4A56;

    /**
     * {top, bottom, text} per madness stage, matching the madness HUD bar's
     * palette without its pulsing.
     */
    private static final int[][] STAGE_COLORS = {
            {0xFF00FFCC, 0xFF00AA88, 0xFF00FFCC},
            {0xFFFFAA00, 0xFFCC7700, 0xFFFFAA00},
            {0xFFDD2222, 0xFF991111, 0xFFDD2222},
            {0xFFFF0000, 0xFF8B0000, 0xFFFF0055},
            {0xFF993399, 0xFF3A3A3A, 0xFF993399}
    };

    /**
     * {top, bottom, text} per tiredness stage — grey drifting to amber.
     */
    private static final int[][] TIRED_COLORS = {
            {0xFF9A9AA2, 0xFF6E6E76, 0xFF9A9AA2},
            {0xFFC9B27A, 0xFF8E7C50, 0xFFC9B27A},
            {0xFFE0A83C, 0xFF9A6E1E, 0xFFE0A83C},
            {0xFFE07C1E, 0xFF9A4E0E, 0xFFE07C1E},
            {0xFFD64518, 0xFF8B2A0A, 0xFFD64518}
    };

    private static final int[] SPIRIT_COLORS = {0xFFB3CFEC, 0xFF5D8FC2, 0xFF9FC4E8};
    private static final int RED = MenuTheme.DANGER;
    private static final int VIOLET = 0xFFC9A0E8;
    private static final int AMBER = MenuTheme.WARN;

    /**
     * One layer of a vitals row, so a row's bar can be the ordinary gauge or
     * madness's three stacked segments without two copies of the row painter.
     */
    @FunctionalInterface
    private interface BarPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int w);
    }

    /**
     * A vitals row's mark — a drawn glyph for most, a {@link PlateSymbols}
     * sprite where the plate already owns the artwork.
     */
    @FunctionalInterface
    private interface SymbolPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y);
    }

    /**
     * A compact fact plus the plain-language line printed under the chip row.
     * Drawn by {@link MenuTheme#chip}, so the label is muted and the value
     * carries the colour — the same pair a menu's {@code chips} row makes.
     */
    private record Chip(String label, String value, int rgb, String glyph, Component explanation) {
        MenuComponent.Chip model() {
            return new MenuComponent.Chip(label, value, rgb & 0xFFFFFF,
                    new MenuIcon(MenuIcon.Kind.GLYPH, glyph), "");
        }
    }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Action> footer = new ArrayList<>();

    private boolean openSent;
    private boolean closeSent;

    private int cardX, cardY, cardW, cardH;
    private int headerH, footerH, viewTop, viewBottom;
    private double scroll;
    private boolean draggingScroll;
    private int scrollGrab;

    /**
     * Last frame's measured content height — what the scroll clamp works
     * against, one frame behind and self-correcting. Seeded full so the card
     * never opens as a sliver; see the class comment.
     */
    private int contentHeight = Integer.MAX_VALUE / 4;
    private Component hoveredTip;

    /** Milliseconds since the previous frame, the clock every tween runs on. */
    private float frameDelta;
    private long lastFrameAt;

    private final float[] navHover = new float[NAV.length];
    private float prefsHover;

    /**
     * When the player picked a destination and the server has not answered yet, or 0.
     * <p>
     * The sheet <b>stays on screen</b> for that window instead of closing immediately. Closing
     * first was the obvious thing to write and the wrong thing to do: between the click and the
     * server's document the player is dropped back into the world, which un-grabs the cursor and
     * hands the next mouse movement to the camera — so on any real latency a click on a
     * destination spun the view before the menu appeared. Holding the sheet costs nothing (the
     * document replaces this screen when it lands) and makes the transition silent.
     */
    private long awaitingSince;

    public CharacterSheetScreen(Screen parent) {
        super(Component.translatable("screen.coi.sheet_title"));
        this.parent = parent;
    }

    // --- Geometry, shared verbatim with the menu screen ---

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

    private int sectionGap() {
        return compact() ? 4 : 8;
    }

    /**
     * The card's own colour. A document names its accent; the sheet's subject is
     * the character, so the pathway names it — falling back to the mod's gold
     * before the first packet lands, exactly as a document with no accent does.
     */
    private static int accent() {
        return ClientSheetState.hasData() ? ClientSheetState.pathwayArgb() : CoiStyle.ACCENT;
    }

    @Override
    public boolean isPauseScreen() {
        // A chest GUI never paused the world, and this stands in for one
        return false;
    }

    // --- Lifecycle ---

    @Override
    protected void init() {
        // Cleared rather than kept: the labels are resolved through I18n once,
        // and a language change re-inits the screen
        footer.clear();
        layout();
        if (!openSent) {
            send(ActionPayload.of("sheet_open"));
            openSent = true;
        }
    }

    private void layout() {
        cardW = CoiStyle.cardWidth(this.width, compact());
        cardX = (this.width - cardW) / 2;
        headerH = pad() + ICON + 6;

        if (footer.isEmpty()) buildFooter();
        footerH = footerBlockH() + pad();

        int margin = compact() ? 8 : 18;
        int availH = Math.max(80, this.height - margin * 2);
        cardH = Math.min(availH, headerH + contentHeight + pad() + footerH);
        cardY = (this.height - cardH) / 2;
        viewTop = cardY + headerH;
        viewBottom = Math.max(viewTop + 20, cardY + cardH - footerH - pad() / 2);

        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    /**
     * The way out, and only the way out — the sheet's real actions are its
     * cards, so these two stay quiet and out of the scrolling column.
     */
    private void buildFooter() {
        footer.clear();
        footer.add(new Action(button("server_menu", I18n.get("screen.coi.sheet_btn_server_menu"),
                MenuComponent.ButtonStyle.SECONDARY), () -> {
            send(ActionPayload.of("open_menu"));
            awaitServer();
        }));
        footer.add(new Action(button("done", I18n.get("gui.done"),
                MenuComponent.ButtonStyle.PRIMARY), this::onClose));
    }

    private static MenuComponent.Button button(String id, String label, MenuComponent.ButtonStyle style) {
        return new MenuComponent.Button(id, label, "", style, true, "", null, MenuIcon.NONE);
    }

    private int footerBlockH() {
        return buttonH();
    }

    private int footerTop() {
        return cardY + cardH - pad() / 2 - footerBlockH();
    }

    /**
     * A destination card: hand the target to the server and step aside for
     * whatever it opens.
     * <p>
     * The sheet is the page the player came from, so the menu session is marked
     * as reached from here — a {@code back} at the root of the server's screen
     * then lands back on the sheet instead of dropping the player into the
     * world.
     */
    private void openTarget(String target) {
        ClientMenuState.markFromSheet();
        send(ActionPayload.ofOpen(target));
        awaitServer();
    }

    /**
     * Hands over to whatever the server is about to open: stop the sheet pushes, but keep the
     * screen up until the answer lands. See {@link #awaitingSince}.
     */
    private void awaitServer() {
        sendClose();
        awaitingSince = System.currentTimeMillis();
    }

    /**
     * Nothing came back in time — the target may be gated, or the packet lost. Rather than leave
     * the player looking at a frozen page, ask for the sheet again and carry on.
     */
    private void giveUpWaiting() {
        awaitingSince = 0;
        closeSent = false;
        send(ActionPayload.of("sheet_open"));
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

    private void sendClose() {
        if (closeSent) return;
        closeSent = true;
        send(ActionPayload.of("sheet_close"));
    }

    /**
     * Never talk into the void: the debug screen opens this sheet with no
     * server attached, and the plugin channel may not be registered at all.
     */
    private static void send(ActionPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || !ClientPlayNetworking.canSend(ActionPayload.ID)) return;
        ClientPlayNetworking.send(payload);
    }

    private void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        // Handing over to the server: the page is a picture until the answer lands, so a second
        // click cannot queue a second destination behind the first
        if (awaitingSince != 0) return true;

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

        if (my < viewTop || my >= viewBottom) return super.mouseClicked(event, doubleClick);
        for (Hit hit : hits) {
            if (hit.action() != null && hit.contains(mx, my)) {
                click();
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickHeader(double mx, double my) {
        int top = cardY + pad();
        if (parent != null && inBox(mx, my, cardX + pad(), top, 12, 11)) {
            click();
            this.onClose();
            return true;
        }
        if (inBox(mx, my, cardX + cardW - pad() - 10, top, 12, 11)) {
            click();
            this.onClose();
            return true;
        }
        return false;
    }

    private boolean clickFooter(double mx, double my) {
        int y = footerTop();
        int bw = (contentW() - (footer.size() - 1) * GAP) / footer.size();
        for (int i = 0; i < footer.size(); i++) {
            int bx = cardX + pad() + i * (bw + GAP);
            if (!inBox(mx, my, bx, y, bw, footerBlockH())) continue;
            click();
            footer.get(i).run().run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScroll) {
            scrollTo(event.y() - scrollGrab);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Mth.clamp(scroll - Math.signum(verticalAmount) * SCROLL_STEP, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        int page = viewBottom - viewTop - 20;
        switch (event.key()) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll = Mth.clamp(scroll + page, 0, maxScroll());
            case GLFW.GLFW_KEY_PAGE_UP -> scroll = Mth.clamp(scroll - page, 0, maxScroll());
            case GLFW.GLFW_KEY_HOME -> scroll = 0;
            case GLFW.GLFW_KEY_END -> scroll = maxScroll();
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - (viewBottom - viewTop));
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /**
     * One step of a tween toward {@code target}, frame-rate independent. Under
     * {@code epilepsyMode} it returns the target outright, which is what makes
     * the chrome snap rather than move — one place to honour the setting, as on
     * the menu screen.
     */
    private float approach(float current, float target, float durationMs) {
        if (HudConfig.getSettings().epilepsyMode || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    // --- Rendering ---

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        long now = System.currentTimeMillis();
        // A first frame, or one after a long stall, must not jump every tween
        frameDelta = lastFrameAt == 0 ? 0f : Math.min(100f, now - lastFrameAt);
        lastFrameAt = now;

        if (awaitingSince != 0 && now - awaitingSince > AWAIT_TIMEOUT_MS) giveUpWaiting();

        layout();
        graphics.fill(0, 0, this.width, this.height, CoiStyle.BACKDROP);

        int accent = accent();
        CoiStyle.drawCard(graphics, cardX, cardY, cardW, cardH);
        graphics.fill(cardX, cardY, cardX + cardW, cardY + 1, accent);
        drawWatermark(graphics);

        scroll = Mth.clamp(scroll, 0, maxScroll());
        hits.clear();
        hoveredTip = null;

        drawHeader(graphics, mouseX, mouseY, accent);

        graphics.enableScissor(cardX + 1, viewTop, cardX + cardW - 1, viewBottom);
        int x = cardX + pad();
        int w = contentW();
        int y = viewTop - (int) scroll;
        int start = y;
        y = drawHero(graphics, x, y + pad() / 2, w) + sectionGap();
        if (ClientSheetState.hasData()) {
            y = drawVitals(graphics, x, y, w) + sectionGap();
            y = drawActing(graphics, x, y, w) + sectionGap();
            y = drawConditions(graphics, x, y, w);
            y = drawNavigation(graphics, x, y, w, mouseX, mouseY) + sectionGap();
            y = drawPreferences(graphics, x, y, w, mouseX, mouseY);
        } else {
            y = drawWaiting(graphics, x, y, w);
        }
        graphics.disableScissor();
        contentHeight = y - start + pad();

        if (maxScroll() > 0) drawScrollbar(graphics, mouseX, mouseY, accent);
        drawFooter(graphics, mouseX, mouseY, accent);
        if (awaitingSince != 0) drawAwaiting(graphics, accent);
        super.extractRenderState(graphics, mouseX, mouseY, partial);

        if (hoveredTip != null && awaitingSince == 0) {
            // Titled, so a dimmed card reads as "locked, and here is why" rather
            // than as a stray sentence floating over the page
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.coi.sheet_lock_title")
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
            lines.addAll(this.font.split(hoveredTip.copy().withStyle(ChatFormatting.GRAY), 180));
            graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }

    /**
     * The hand-over veil: the card dims and says what it is doing, so the pause between the click
     * and the server's screen reads as "working" rather than as a click that did nothing.
     */
    private void drawAwaiting(GuiGraphicsExtractor graphics, int accent) {
        graphics.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1, 0xB0121216);
        String label = I18n.get("screen.coi.sheet_opening");
        graphics.centeredText(this.font, Component.literal(label),
                cardX + cardW / 2, cardY + cardH / 2 - 4, accent);
    }

    /**
     * The pathway emblem, huge and almost invisible behind the top right of the
     * card — the same mark a menu document puts there, so the sheet and the
     * screens it opens carry one watermark between them.
     */
    private void drawWatermark(GuiGraphicsExtractor graphics) {
        if (ClientSheetState.pathway().isEmpty()) return;
        graphics.enableScissor(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1);
        MenuTheme.watermark(graphics, this.font,
                new MenuIcon(MenuIcon.Kind.PATHWAY, ClientSheetState.pathway()),
                cardX + cardW - WATERMARK - 4, cardY + 4, WATERMARK);
        graphics.disableScissor();
    }

    private void drawHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        int left = cardX + pad();
        int right = cardX + cardW - pad();
        int top = cardY + pad();

        // The accent band: the header is the one block that belongs to the
        // subject's own colour rather than to the card
        graphics.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + headerH - 3,
                MenuTheme.withAlpha(accent, 0.06f));

        // Only the debug screen opens the sheet over something; from the world
        // it is the root, and a root has nowhere to go back to
        if (parent != null) {
            boolean hovered = inBox(mouseX, mouseY, left, top, 12, 11);
            MenuTheme.chevron(graphics, left + 1, top + 1, hovered ? accent : CoiStyle.TEXT_MUTED);
            left += 15;
        }
        int closeX = right - 8;
        boolean closeHover = inBox(mouseX, mouseY, closeX - 2, top, 12, 11);
        MenuTheme.cross(graphics, closeX, top + 1, closeHover ? MenuTheme.DANGER : CoiStyle.TEXT_MUTED);
        right -= 14;

        if (MenuTheme.drawIcon(graphics, this.font,
                new MenuIcon(MenuIcon.Kind.PATHWAY, ClientSheetState.pathway()), left, top, ICON, 1f)) {
            left += ICON + 6;
        }
        int textW = Math.max(20, right - left);
        graphics.text(this.font, this.font.plainSubstrByWidth(this.title.getString(), textW),
                left, top + (ICON - 8) / 2, accent, true);
        MenuTheme.hairline(graphics, cardX + pad(), cardY + headerH - 3, contentW());
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        int y = footerTop();
        MenuTheme.hairline(graphics, cardX + pad(), y - pad() / 2, contentW());
        int bw = (contentW() - (footer.size() - 1) * GAP) / footer.size();
        for (int i = 0; i < footer.size(); i++) {
            int bx = cardX + pad() + i * (bw + GAP);
            boolean hovered = inBox(mouseX, mouseY, bx, y, bw, footerBlockH());
            MenuTheme.button(graphics, this.font, bx, y, bw, footerBlockH(),
                    footer.get(i).model(), accent, hovered, false);
        }
    }

    // --- Scrollbar (the handle is grabbable, not decoration) ---

    private int trackX() {
        return cardX + cardW - 5;
    }

    private int trackTop() {
        return viewTop + 1;
    }

    private int trackH() {
        return viewBottom - viewTop - 2;
    }

    private int handleH() {
        int track = trackH();
        return Math.clamp((long) (viewBottom - viewTop) * track / Math.max(1, contentHeight),
                Math.min(14, track), track);
    }

    private int handleY() {
        double max = maxScroll();
        return trackTop() + (int) ((trackH() - handleH()) * (max <= 0 ? 0 : scroll / max));
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        if (trackH() <= 0) return;
        graphics.fill(trackX(), trackTop(), trackX() + 3, trackTop() + trackH(), CoiStyle.SCROLL_TRACK);
        boolean grabbed = draggingScroll || onHandle(mouseX, mouseY);
        graphics.fill(trackX(), handleY(), trackX() + 3, handleY() + handleH(),
                MenuTheme.withAlpha(accent, grabbed ? 0.85f : 0.55f));
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
            scroll = 0;
            return;
        }
        scroll = Mth.clamp((handleTop - trackTop()) * maxScroll() / span, 0, maxScroll());
    }

    // --- Chrome ---

    /**
     * A section caption: small-caps, the subject's colour, a rule out to the
     * card's edge — {@link MenuTheme#headingCaption}, so the sheet's sections
     * and a document's sections are the same object.
     */
    private int section(GuiGraphicsExtractor graphics, int x, int y, int w, String key, String glyph) {
        int accent = accent();
        int left = x;
        if (MenuTheme.drawIcon(graphics, this.font, new MenuIcon(MenuIcon.Kind.GLYPH, glyph),
                left, y + 1, SMALL_ICON, 1f)) {
            left += SMALL_ICON + 3;
        }
        int used = MenuTheme.headingCaption(graphics, this.font, I18n.get(key), left, y + 3,
                x + w - left, accent);
        MenuTheme.headingRule(graphics, left + used + 5, y + 6, x + w, accent);
        return y + HEADING_H;
    }

    // --- Hero ---

    /**
     * The subject of the page, in the same translucent accent panel a document's
     * {@code hero} gets: face, name at 1.5x, pathway and sequence beneath, and
     * the sequence again as a numeral in a badge — the one number other players
     * ask for.
     */
    private int drawHero(GuiGraphicsExtractor graphics, int x, int y, int w) {
        int accent = accent();
        boolean loaded = ClientSheetState.hasData();
        boolean badge = loaded && ClientSheetState.sequence() >= 0;
        int textBlock = 14 + 11 + (loaded ? 11 : 0);
        int core = Math.max(HEAD, textBlock);
        int h = 10 + core + 8;

        MenuTheme.panel(graphics, x, y, w, h, MenuTheme.withAlpha(accent, 0.08f),
                MenuTheme.withAlpha(accent, 0.30f));

        int coreTop = y + 10;
        drawHead(graphics, x + 8, coreTop + (core - HEAD) / 2);

        int right = x + w - 8;
        if (badge) {
            drawSequenceBadge(graphics, right - SEQ_BADGE_W, coreTop + (core - HEAD) / 2, accent);
            right -= SEQ_BADGE_W + 8;
        }

        int textX = x + 8 + HEAD + 8;
        int textW = Math.max(20, right - textX);
        int textY = coreTop + (core - textBlock) / 2;

        AbstractClientPlayer player = this.minecraft == null ? null : this.minecraft.player;
        String name = player == null ? "" : player.getName().getString();
        // plainSubstrByWidth measures at 1x, so the budget is divided by the
        // scale before clipping rather than after
        MenuTheme.scaledText(graphics, this.font,
                this.font.plainSubstrByWidth(name, (int) (textW / 1.5f)), textX, textY, 1.5f, accent, true);

        if (!loaded) {
            graphics.text(this.font, trim(I18n.get("screen.coi.sheet_loading"), textW),
                    textX, textY + 15, CoiStyle.TEXT_MUTED, false);
            return y + h;
        }

        int emblemW = CoiIcons.drawPathwayEmblem(graphics, this.font, ClientSheetState.pathway(),
                textX, textY + 15, accent);
        graphics.text(this.font, trim(pathwayCaption(), textW - emblemW - 4),
                textX + emblemW + 4, textY + 15, accent, false);
        graphics.text(this.font, trim(sequenceCaption(), textW), textX, textY + 26,
                CoiStyle.TEXT_MUTED, false);
        return y + h;
    }

    /**
     * The player's face and its hat layer, scaled up from the 8×8 patches of
     * the 64×64 skin sheet — the same two blits the character plate makes, four
     * times the size.
     */
    private void drawHead(GuiGraphicsExtractor graphics, int x, int y) {
        AbstractClientPlayer player = this.minecraft == null ? null : this.minecraft.player;
        graphics.outline(x - 1, y - 1, HEAD + 2, HEAD + 2, MenuTheme.withAlpha(accent(), 0.35f));
        if (player == null) {
            graphics.fill(x, y, x + HEAD, y + HEAD, 0xFF2A2A32);
            return;
        }
        Identifier skin = player.getSkin().body().texturePath();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
    }

    /**
     * The sequence as a numeral rather than a word in a sentence.
     */
    private void drawSequenceBadge(GuiGraphicsExtractor graphics, int x, int y, int accent) {
        MenuTheme.panel(graphics, x, y, SEQ_BADGE_W, HEAD, MenuTheme.withAlpha(accent, 0.14f),
                MenuTheme.withAlpha(accent, 0.45f));

        String tag = I18n.get("screen.coi.sheet_seq_badge");
        graphics.text(this.font, tag, x + (SEQ_BADGE_W - this.font.width(tag)) / 2, y + 4,
                CoiStyle.TEXT_MUTED, false);

        String number = String.valueOf(ClientSheetState.sequence());
        MenuTheme.scaledText(graphics, this.font, number,
                x + (SEQ_BADGE_W - this.font.width(number) * 2) / 2, y + 13, 2f, accent, true);
    }

    private static String pathwayCaption() {
        String name = ClientSheetState.pathwayName();
        return name.isEmpty() ? I18n.get("screen.coi.sheet_no_pathway") : name.toUpperCase(Locale.ROOT);
    }

    private static String sequenceCaption() {
        int sequence = ClientSheetState.sequence();
        if (sequence < 0) return "";
        return ClientSheetState.sequenceName().isEmpty()
                ? I18n.get("screen.coi.sheet_sequence_only", sequence)
                : I18n.get("screen.coi.sheet_sequence", sequence, ClientSheetState.sequenceName());
    }

    // --- Vitals ---

    private int drawVitals(GuiGraphicsExtractor graphics, int x, int y, int w) {
        boolean floors = ClientSheetState.permanentFloor() > 0 || ClientSheetState.godhoodFloor() > 0;
        int ry = section(graphics, x, y, w, "screen.coi.sheet_sec_vitals", GLYPH_HEALTH);

        ry = healthRow(graphics, x, ry, w) + VITAL_GAP;
        ry = spiritualityRow(graphics, x, ry, w) + VITAL_GAP;
        ry = madnessRow(graphics, x, ry, w, floors) + VITAL_GAP;
        return tirednessRow(graphics, x, ry, w);
    }

    private int healthRow(GuiGraphicsExtractor graphics, int x, int y, int w) {
        double max = Math.max(1.0, ClientSheetState.maxHealth());
        double fraction = Math.clamp(ClientSheetState.health() / max, 0, 1);
        int[] palette = healthColors(fraction);
        boolean low = fraction <= 0.25;
        Component sub = low
                ? Component.translatable("screen.coi.sheet_health_low")
                : Component.translatable("screen.coi.sheet_health_sub", round0(fraction * 100));
        return vitalRow(graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.HEART, sx, sy, SYMBOL,
                        (float) fraction, SYMBOL_EMPTY, palette[0]),
                I18n.get("screen.coi.sheet_health"),
                num(ClientSheetState.health()) + " / " + num(max), palette[2],
                bar(fraction, palette[0]),
                sub, low ? RED : CoiStyle.TEXT_MUTED, null);
    }

    private int spiritualityRow(GuiGraphicsExtractor graphics, int x, int y, int w) {
        double max = Math.max(1, ClientSheetState.maxSpirituality());
        double fraction = Math.clamp(ClientSheetState.spirituality() / max, 0, 1);
        boolean low = fraction < 0.30;
        Component sub = low
                ? Component.translatable("screen.coi.sheet_spirit_low")
                : Component.translatable("screen.coi.sheet_spirit_sub", round0(fraction * 100));
        return vitalRow(graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.FLASK, sx, sy, SYMBOL,
                        (float) fraction, SYMBOL_EMPTY, low ? RED : SPIRIT_COLORS[0]),
                I18n.get("screen.coi.sheet_spirituality"),
                ClientSheetState.spirituality() + " / " + ClientSheetState.maxSpirituality(),
                low ? RED : SPIRIT_COLORS[2],
                bar(fraction, low ? RED : SPIRIT_COLORS[0]),
                sub, low ? RED : CoiStyle.TEXT_MUTED, null);
    }

    /**
     * The one row the plate and the sheet deliberately disagree on: the plate's
     * brain fills with <em>sanity</em>, because a HUD gauge should read "how
     * much of you is left". Here the row is named Madness and the bar beside it
     * splits into permanent / godhood / temporary, so a brain filling the other
     * way would contradict the very bar it sits next to.
     */
    private int madnessRow(GuiGraphicsExtractor graphics, int x, int y, int w, boolean floors) {
        int stage = ClientSheetState.madnessStage();
        int[] palette = STAGE_COLORS[stage];
        double madness = ClientSheetState.madness();
        // Until stage 2 the artwork keeps its own colours and only the bar
        // carries the state, exactly as the plate's sanity gauge does
        int wash = stage < 2 ? PlateSymbols.NO_WASH : palette[0] & 0xFFFFFF;
        return vitalRow(graphics, x, y, w,
                (g, sx, sy) -> PlateSymbols.draw(g, PlateSymbols.BRAIN, sx, sy,
                        (float) (madness / 100.0), 1f, wash),
                I18n.get("screen.coi.sheet_madness"),
                pct(madness) + " · " + I18n.get("screen.coi.sheet_stage_" + stage), palette[2],
                madnessBar(palette),
                Component.translatable("screen.coi.sheet_stage_desc_" + stage), palette[2],
                floors ? floorNote() : null);
    }

    private Component floorNote() {
        double permanent = ClientSheetState.permanentFloor();
        double godhood = ClientSheetState.godhoodFloor();
        if (permanent > 0 && godhood > 0) {
            return Component.translatable("screen.coi.sheet_floor_both", round0(permanent), round0(godhood));
        }
        return permanent > 0
                ? Component.translatable("screen.coi.sheet_floor_perm", round0(permanent))
                : Component.translatable("screen.coi.sheet_floor_godhood", round0(godhood));
    }

    private int tirednessRow(GuiGraphicsExtractor graphics, int x, int y, int w) {
        int stage = ClientSheetState.tirednessStage();
        int[] palette = TIRED_COLORS[stage];
        double tiredness = Math.clamp(ClientSheetState.tiredness(), 0, 100);
        return vitalRow(graphics, x, y, w,
                (g, sx, sy) -> SheetGlyphs.drawFilled(g, SheetGlyphs.HOURGLASS, sx, sy, SYMBOL,
                        (float) (tiredness / 100.0), SYMBOL_EMPTY, palette[0]),
                I18n.get("screen.coi.sheet_tiredness"),
                pct(tiredness) + " · " + I18n.get("screen.coi.sheet_tired_" + stage), palette[2],
                bar(tiredness / 100.0, palette[0]),
                Component.translatable("screen.coi.sheet_tired_desc_" + stage), CoiStyle.TEXT_MUTED, null);
    }

    /**
     * Symbol, then a column carrying the number first and the bar second — the
     * reading a player actually wants is "how much", not "how full".
     */
    private int vitalRow(GuiGraphicsExtractor graphics, int x, int y, int w,
                         SymbolPainter symbol, String label, String value, int valueRgb,
                         BarPainter bar, Component sub, int subRgb, Component note) {
        symbol.draw(graphics, x, y);
        int textX = x + SYMBOL + 8;
        int textW = x + w - textX;

        int valueW = this.font.width(value);
        graphics.text(this.font, trim(label, textW - valueW - 6), textX, y + 1, CoiStyle.TEXT_BODY, true);
        graphics.text(this.font, value, textX + textW - valueW, y + 1, valueRgb, true);
        bar.draw(graphics, textX, y + 13, textW);
        graphics.text(this.font, trim(sub.getString(), textW), textX, y + 23, subRgb, false);
        if (note == null) return y + VITAL_ROW_H;
        graphics.text(this.font, trim(note.getString(), textW), textX, y + VITAL_ROW_H + 2,
                CoiStyle.TEXT_MUTED, false);
        return y + VITAL_ROW_H + VITAL_NOTE_H;
    }

    private static BarPainter bar(double fraction, int argb) {
        return (graphics, x, y, w) -> MenuTheme.gauge(graphics, x, y, w, BAR_H, fraction, argb);
    }

    /**
     * Three stacked segments left to right: the permanent floor solid, the
     * godhood floor over it at 60% alpha, then whatever madness is merely
     * temporary in the stage's own colour. Drawn over a {@link MenuTheme#gauge}
     * so the track and the highlight are the menus' own, not the HUD bar's.
     */
    private static BarPainter madnessBar(int[] palette) {
        double madness = ClientSheetState.madness();
        double permanent = ClientSheetState.permanentFloor();
        double godhood = ClientSheetState.godhoodFloor();
        return (graphics, x, y, w) -> {
            MenuTheme.gauge(graphics, x, y, w, BAR_H, madness / 100.0, palette[0]);
            int filled = (int) Math.round(Math.clamp(madness / 100.0, 0, 1) * w);
            int permEnd = Math.min((int) Math.round(Math.clamp(permanent / 100.0, 0, 1) * w), filled);
            int godEnd = Math.min(permEnd + (int) Math.round(Math.clamp(godhood / 100.0, 0, 1) * w), filled);
            // From y + 1, so the gauge's own top highlight survives the overdraw
            if (permEnd > 0) {
                graphics.fill(x, y + 1, x + permEnd, y + BAR_H, palette[1]);
            }
            if (godEnd > permEnd) {
                graphics.fill(x + permEnd, y + 1, x + godEnd, y + BAR_H,
                        MenuTheme.withAlpha(palette[0], 0.6f));
            }
        };
    }

    // --- Acting ---

    private int drawActing(GuiGraphicsExtractor graphics, int x, int y, int w) {
        ClientSheetState.Acting acting = ClientSheetState.acting();
        if (ClientSheetState.outer() || acting == null) return drawOuter(graphics, x, y, w);

        List<Chip> chips = actingChips(acting);
        List<List<Chip>> chipRows = layoutChips(chips, w);

        int ry = section(graphics, x, y, w, "screen.coi.sheet_acting", GLYPH_GROWTH);
        ry = actingRow(graphics, x, ry, w, acting);
        ry = drawChips(graphics, x, ry, chipRows);
        ry = drawExplanations(graphics, x, ry, w, chips);

        ry += 6;
        MenuTheme.hairline(graphics, x, ry, w);
        ry += 6;
        ry = section(graphics, x, ry, w, "screen.coi.sheet_sources", GLYPH_RESTORE);
        return drawLedger(graphics, x, ry, w, acting);
    }

    private int actingRow(GuiGraphicsExtractor graphics, int x, int y, int w, ClientSheetState.Acting acting) {
        int accent = accent();
        double percent = Math.clamp(acting.percent(), 0, 100);
        Component cooldown = ClientSheetState.actingCooldownNow() > 0
                ? Component.translatable("screen.coi.sheet_acting_cooldown", ClientSheetState.actingCooldownClock())
                : Component.translatable("screen.coi.sheet_acting_ready");
        return vitalRow(graphics, x, y, w,
                (g, sx, sy) -> PlateSymbols.draw(g, PlateSymbols.MASK, sx, sy,
                        (float) (percent / 100.0), 1f, PlateSymbols.NO_WASH),
                I18n.get("screen.coi.sheet_progress"), pct(percent), acting.limited() ? RED : accent,
                bar(acting.acting() / Math.max(1.0, acting.needed()), acting.limited() ? RED : accent),
                Component.translatable("screen.coi.sheet_acting_progress",
                        num(acting.acting()), num(acting.needed())), CoiStyle.TEXT_MUTED,
                cooldown);
    }

    /**
     * The ledger proper: every source is drawn, because a row silently missing
     * is the one thing a ledger must never do. A capped source turns red on
     * both sides so the reason a total stopped moving is visible without
     * arithmetic.
     */
    private int drawLedger(GuiGraphicsExtractor graphics, int x, int y, int w, ClientSheetState.Acting acting) {
        if (acting.sources().isEmpty()) {
            graphics.text(this.font, trim(I18n.get("screen.coi.sheet_sources_empty"), w),
                    x, y + 2, CoiStyle.TEXT_MUTED, false);
            return y + LEDGER_ROW_H;
        }
        for (ClientSheetState.Source source : acting.sources()) {
            String label = source.label().isEmpty() ? source.source() : source.label();
            String value = source.contributed() + " / " + (source.unlimited() ? "∞" : source.cap());
            boolean capped = source.capped();
            int color = capped ? RED : CoiStyle.TEXT_BODY;

            int labelW = Math.min(this.font.width(label), w * 2 / 5);
            int valueW = this.font.width(value);
            graphics.text(this.font, trim(label, labelW), x, y + 3, color, false);
            graphics.text(this.font, value, x + w - valueW, y + 3,
                    capped ? RED : CoiStyle.TEXT_MUTED, false);

            int barX = x + labelW + 6;
            int barW = x + w - valueW - 6 - barX;
            if (barW > 8 && !source.unlimited() && source.cap() > 0) {
                MenuTheme.gauge(graphics, barX, y + 4, barW, 4,
                        source.contributed() / (double) source.cap(), capped ? RED : accent());
            }
            y += LEDGER_ROW_H;
        }
        return y;
    }

    private static List<Chip> actingChips(ClientSheetState.Acting acting) {
        List<Chip> chips = new ArrayList<>();
        if (acting.limited()) {
            chips.add(new Chip(I18n.get("screen.coi.sheet_limited"), "", RED, GLYPH_RESIST,
                    Component.translatable("screen.coi.sheet_limited_desc")));
        }
        ClientSheetState.Overflow overflow = acting.overflow();
        if (overflow.eligible()) {
            String text = overflow.uncapped()
                    ? I18n.get("screen.coi.sheet_overflow_uncapped", overflow.banked())
                    : I18n.get("screen.coi.sheet_overflow", overflow.banked(), overflow.ceiling());
            chips.add(new Chip(text, "", CoiStyle.ACCENT, GLYPH_GROWTH,
                    Component.translatable("screen.coi.sheet_overflow_desc")));
        }
        if (overflow.foreignThrottlePercent() > 0) {
            chips.add(new Chip(I18n.get("screen.coi.sheet_foreign_throttle", overflow.foreignThrottlePercent()),
                    "", AMBER, GLYPH_AUTHORITY,
                    Component.translatable("screen.coi.sheet_foreign_desc")));
        }
        return chips;
    }

    /**
     * Outer pathways have no acting to show, so the section explains what they
     * do instead of leaving a bare line where the progress bar would have been.
     */
    private int drawOuter(GuiGraphicsExtractor graphics, int x, int y, int w) {
        int ry = section(graphics, x, y, w, "screen.coi.sheet_acting", GLYPH_GROWTH);
        int textX = x + SYMBOL + 8;
        int textW = x + w - textX;
        PlateSymbols.draw(graphics, PlateSymbols.MASK, x, ry, 0f, 1f, PlateSymbols.NO_WASH);
        graphics.text(this.font, trim(I18n.get("screen.coi.sheet_outer"), textW), textX, ry + 1,
                CoiStyle.TEXT_BODY, true);
        int lineY = ry + LINE + 2;
        for (FormattedCharSequence line : this.font.split(
                Component.translatable("screen.coi.sheet_outer_hint"), textW)) {
            graphics.text(this.font, line, textX, lineY, CoiStyle.TEXT_MUTED);
            lineY += PARA_LINE;
        }
        return Math.max(ry + SYMBOL, lineY);
    }

    // --- Conditions ---

    private int drawConditions(GuiGraphicsExtractor graphics, int x, int y, int w) {
        List<Chip> chips = conditionChips();
        if (chips.isEmpty()) return y;

        int ry = section(graphics, x, y, w, "screen.coi.sheet_sec_conditions", GLYPH_WARD);
        ry = drawChips(graphics, x, ry, layoutChips(chips, w));
        return drawExplanations(graphics, x, ry, w, chips) + sectionGap();
    }

    private static List<Chip> conditionChips() {
        List<Chip> chips = new ArrayList<>();
        ClientSheetState.Gauge lifeAndDeath = ClientSheetState.lifeAndDeath();
        if (lifeAndDeath.present()) {
            chips.add(new Chip(I18n.get("screen.coi.sheet_life_death"),
                    I18n.get("screen.coi.sheet_percent", round0(Math.clamp(lifeAndDeath.value(), 0, 1) * 100)),
                    VIOLET, GLYPH_SOUL,
                    Component.translatable("screen.coi.sheet_life_death_desc")));
        }
        ClientSheetState.Pressure pressure = ClientSheetState.pressure();
        if (pressure.present()) {
            chips.add(new Chip(I18n.get("screen.coi.sheet_pressure"),
                    pressure.stacks() + "/" + pressure.cap(), AMBER, GLYPH_POWER,
                    Component.translatable("screen.coi.sheet_pressure_desc")));
        }
        if (ClientSheetState.anomaly()) {
            chips.add(new Chip(I18n.get("screen.coi.sheet_anomaly"), "", RED, GLYPH_WARD,
                    Component.translatable("screen.coi.sheet_anomaly_desc")));
        }
        return chips;
    }

    // --- Chips ---

    /**
     * Chips wrapped into rows once, so measuring and drawing can never disagree
     * about how tall the block is.
     */
    private List<List<Chip>> layoutChips(List<Chip> chips, int w) {
        List<List<Chip>> rows = new ArrayList<>();
        List<Chip> row = new ArrayList<>();
        int used = 0;
        for (Chip chip : chips) {
            int width = MenuTheme.chipWidth(this.font, chip.model());
            if (!row.isEmpty() && used + CHIP_GAP + width > w) {
                rows.add(row);
                row = new ArrayList<>();
                used = 0;
            }
            used += (row.isEmpty() ? 0 : CHIP_GAP) + width;
            row.add(chip);
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }

    private int drawChips(GuiGraphicsExtractor graphics, int x, int y, List<List<Chip>> rows) {
        if (rows.isEmpty()) return y;
        y += 4;
        for (List<Chip> row : rows) {
            int cx = x;
            for (Chip chip : row) {
                MenuComponent.Chip model = chip.model();
                MenuTheme.chip(graphics, this.font, model, cx, y, accent(), 0f);
                cx += MenuTheme.chipWidth(this.font, model) + CHIP_GAP;
            }
            y += MenuTheme.CHIP_H + CHIP_GAP;
        }
        return y - CHIP_GAP;
    }

    /**
     * The chips say <em>that</em> something is happening; these lines say what
     * it does. They are printed rather than hidden behind a hover, because a
     * throttle the player cannot see is a throttle they will report as a bug.
     */
    private int drawExplanations(GuiGraphicsExtractor graphics, int x, int y, int w, List<Chip> chips) {
        if (chips.isEmpty()) return y;
        y += 4;
        for (Chip chip : chips) {
            for (FormattedCharSequence line : this.font.split(chip.explanation(), w)) {
                graphics.text(this.font, line, x, y, CoiStyle.TEXT_MUTED);
                y += PARA_LINE;
            }
        }
        return y;
    }

    // --- Navigation ---

    private int drawNavigation(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        int ry = section(graphics, x, y, w, "screen.coi.sheet_sec_nav", GLYPH_DIVINATION) + 4;
        // Seven destinations. At the sheet's old 440 these could only ever be two abreast, which
        // was four rows of card; with the shared width a third column fits and it becomes three.
        int cols = w >= THREE_COLUMN_MIN ? 3 : w >= TWO_COLUMN_MIN ? 2 : 1;
        int tileW = (w - GAP * (cols - 1)) / cols;
        boolean[] gates = gates();

        for (int i = 0; i < NAV.length; i++) {
            int cx = x + (i % cols) * (tileW + GAP);
            int cy = ry + (i / cols) * (NAV_CARD_H + GAP);
            drawNavCard(graphics, i, gates[i], cx, cy, tileW, mouseX, mouseY);
        }
        int rows = (NAV.length + cols - 1) / cols;
        return ry + rows * (NAV_CARD_H + GAP) - GAP;
    }

    private static boolean[] gates() {
        ClientSheetState.Actions actions = ClientSheetState.actions();
        return new boolean[]{actions.church(), actions.abilities(), actions.mythical(), actions.uniqueness(),
                actions.honorific(), actions.map(), actions.seat()};
    }

    /**
     * A destination, not a button: the name is only half of it, the line under
     * it says what is on the other side. A locked card stays legible and
     * explains itself on hover instead of going dead grey with no reason given.
     * <p>
     * Drawn as a {@link MenuTheme#panel} over {@link MenuTheme#surface}, which
     * is the surface a menu's list rows and grid tiles sit on — and the hover
     * eases in rather than snapping, as everything in a document does.
     */
    private void drawNavCard(GuiGraphicsExtractor graphics, int index, boolean unlocked,
                             int x, int y, int w, int mouseX, int mouseY) {
        Nav nav = NAV[index];
        boolean inside = mouseY >= viewTop && mouseY < viewBottom
                && inBox(mouseX, mouseY, x, y, w, NAV_CARD_H);
        navHover[index] = approach(navHover[index], inside && unlocked ? 1f : 0f, HOVER_MS);
        float hoverT = navHover[index];

        int accent = unlocked ? accent() : CoiStyle.INACTIVE;
        MenuTheme.panel(graphics, x, y, w, NAV_CARD_H, MenuTheme.surface(hoverT),
                unlocked ? MenuTheme.lerpArgb(MenuTheme.withAlpha(accent, 0.30f), accent, hoverT)
                        : MenuTheme.BORDER_OFF);
        graphics.fill(x, y + 1, x + 2, y + NAV_CARD_H - 1, accent);

        SheetGlyphs.draw(graphics, nav.glyph(), x + 7, y + (NAV_CARD_H - 16) / 2, 16, accent);

        int textX = x + 27;
        int textW = x + w - 8 - textX - (unlocked ? 0 : 12);
        graphics.text(this.font, trim(I18n.get("screen.coi.sheet_btn_" + nav.target()), textW),
                textX, y + 6, unlocked ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE, true);
        graphics.text(this.font, trim(I18n.get("screen.coi.sheet_nav_" + nav.target() + "_desc"), textW),
                textX, y + 17, unlocked ? CoiStyle.TEXT_MUTED : CoiStyle.INACTIVE, false);

        if (!unlocked) {
            SheetGlyphs.draw(graphics, SheetGlyphs.LOCK, x + w - 16, y + (NAV_CARD_H - 10) / 2, 10,
                    CoiStyle.INACTIVE);
        }
        Component tip = unlocked ? null : Component.translatable("screen.coi.sheet_lock_" + nav.target());
        hits.add(new Hit(x, y, w, NAV_CARD_H, unlocked ? () -> openTarget(nav.target()) : null, tip));
        if (inside && tip != null) hoveredTip = tip;
    }

    // --- Preferences ---

    /**
     * Terrain damage is a preference the player flips in place, so it gets a
     * switch and its own section rather than an eighth card that looks like a
     * doorway to somewhere.
     */
    private int drawPreferences(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        int ry = section(graphics, x, y, w, "screen.coi.sheet_sec_prefs", GLYPH_DEFENSE) + 4;
        boolean on = ClientSheetState.actions().terrainDamage();
        boolean hovered = mouseY >= viewTop && mouseY < viewBottom
                && inBox(mouseX, mouseY, x, ry, w, NAV_CARD_H);
        prefsHover = approach(prefsHover, hovered ? 1f : 0f, HOVER_MS);

        int accent = accent();
        int state = on ? MenuTheme.SUCCESS : CoiStyle.INACTIVE;
        MenuTheme.panel(graphics, x, ry, w, NAV_CARD_H, MenuTheme.surface(prefsHover),
                MenuTheme.lerpArgb(MenuTheme.withAlpha(accent, 0.30f), accent, prefsHover));

        SheetGlyphs.draw(graphics, SheetGlyphs.BLOCKS, x + 7, ry + (NAV_CARD_H - 16) / 2, 16, state);

        String label = I18n.get(on ? "screen.coi.sheet_on" : "screen.coi.sheet_off");
        int labelW = this.font.width(label);
        int switchX = x + w - 8 - 20;
        int textX = x + 27;
        int textW = switchX - 6 - labelW - 4 - textX;

        graphics.text(this.font, trim(I18n.get("screen.coi.sheet_terrain_title"), textW), textX, ry + 6,
                CoiStyle.TEXT_BODY, true);
        graphics.text(this.font, trim(I18n.get("screen.coi.sheet_terrain_desc"), textW), textX, ry + 17,
                CoiStyle.TEXT_MUTED, false);
        graphics.text(this.font, label, switchX - 4 - labelW, ry + (NAV_CARD_H - 8) / 2, state, false);
        MenuTheme.toggle(graphics, switchX, ry + (NAV_CARD_H - 10) / 2, on, true, MenuTheme.SUCCESS);

        hits.add(new Hit(x, ry, w, NAV_CARD_H, () -> send(ActionPayload.of("toggle_terrain")), null));
        return ry + NAV_CARD_H;
    }

    // --- Empty state ---

    /**
     * Before the first packet lands the sheet still knows who the player is, so
     * the hero panel stays and this explains what is missing — a bare centred
     * line could not tell "still loading" from "wrong server".
     */
    private int drawWaiting(GuiGraphicsExtractor graphics, int x, int y, int w) {
        int ry = section(graphics, x, y, w, "screen.coi.sheet_loading", GLYPH_COOLDOWN) + 4;
        for (FormattedCharSequence line : this.font.split(
                Component.translatable("screen.coi.sheet_loading_hint"), w)) {
            graphics.text(this.font, line, x, ry, CoiStyle.TEXT_MUTED);
            ry += PARA_LINE;
        }
        return ry;
    }

    // --- Formatting helpers ---

    private String trim(String text, int maxW) {
        if (text == null) return "";
        if (maxW <= 0) return "";
        if (this.font.width(text) <= maxW) return text;
        return this.font.plainSubstrByWidth(text, Math.max(0, maxW - this.font.width("…"))) + "…";
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    /**
     * A percentage as a bare whole number — the {@code %} sign belongs to the
     * lang string, which is the only place that knows where it goes.
     */
    private static String round0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    /**
     * Green / yellow / gold / red at 75 / 50 / 25 % of max.
     */
    private static int[] healthColors(double fraction) {
        if (fraction > 0.75) return new int[]{0xFF4CE05A, 0xFF2A8F34, 0xFF4CE05A};
        if (fraction > 0.50) return new int[]{0xFFE8E04C, 0xFF9A9424, 0xFFE8E04C};
        if (fraction > 0.25) return new int[]{0xFFE0A83C, 0xFF9A6E1E, 0xFFE0A83C};
        return new int[]{RED, 0xFF8F2A2A, RED};
    }
}
