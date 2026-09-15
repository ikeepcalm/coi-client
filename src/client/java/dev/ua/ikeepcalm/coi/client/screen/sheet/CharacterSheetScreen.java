package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_COOLDOWN;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.ICON;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.PARA_LINE;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.accent;

import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.network.payload.ActionPayload;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuIcons;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.state.MenuState;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/**
 * The Beyonder character sheet — the client-rendered half of the plugin's
 * InvUI stats page, and the full-size sibling of the character plate on the
 * HUD.
 * <p>
 * Opening announces {@code sheet_open} so the server starts pushing
 * {@code coi-client:sheet} every 60 ticks; closing announces {@code sheet_close}
 * exactly once, whichever way the screen goes away. Everything drawn here is
 * re-read from {@link SheetState} each frame, so those pushes land live.
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
 * The page itself is a column of sections, one class each — {@link SheetHero},
 * {@link SheetVitals}, {@link SheetActing}, {@link SheetConditions} and
 * {@link SheetDestinations} — all drawn through a {@link SheetContext} and each
 * returning the y it reached. This class owns what is around them: the card,
 * the scroll, the two payloads and the wait for the server's answer.
 * <p>
 * The seven sub-menus stay server-side: their cards send
 * {@link ActionPayload#ofOpen(String)} and close the sheet, and the server
 * answers with either its own GUI or a {@code coi-client:menu} document.
 */
public class CharacterSheetScreen extends Screen {

    private static final int WATERMARK = 64;
    private static final int SCROLL_STEP = 20;

    /** Three pixels of paint, and a grab box deliberately wider — see {@link #onTrack}. */
    private static final int TRACK_W = 3;

    /**
     * How long the sheet waits for the server to answer a destination before giving up and
     * becoming live again. Generous: this is a round trip plus whatever the adapter has to look
     * up, and a sheet that gave up early would close under a player on a slow connection just as
     * their menu arrived.
     */
    private static final long AWAIT_TIMEOUT_MS = 5000;

    private final Screen parent;
    private final SheetFooter footer = new SheetFooter();

    /**
     * Built on the first {@code init} and kept across later ones: a resize
     * re-lays the card out, but it must not restart a card's hover tween.
     */
    private SheetContext ctx;
    private SheetDestinations destinations;

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

    /** Milliseconds since the previous frame, the clock every tween runs on. */
    private float frameDelta;
    private long lastFrameAt;

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

    @Override
    public boolean isPauseScreen() {
        // A chest GUI never paused the world, and this stands in for one
        return false;
    }

    // --- Lifecycle ---

    @Override
    protected void init() {
        if (ctx == null) {
            ctx = new SheetContext(this.font, this::openTarget);
            destinations = new SheetDestinations(ctx);
        }
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

        if (footer.isEmpty()) footer.build(this::openServerMenu, this::onClose);
        footerH = buttonH() + pad();

        int margin = compact() ? 8 : 18;
        int availH = Math.max(80, this.height - margin * 2);
        cardH = Math.min(availH, headerH + contentHeight + pad() + footerH);
        cardY = (this.height - cardH) / 2;
        viewTop = cardY + headerH;
        viewBottom = Math.max(viewTop + 20, cardY + cardH - footerH - pad() / 2);

        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    /** The plugin's own chest menu, for a player who asked for that instead. */
    private void openServerMenu() {
        send(ActionPayload.of("open_menu"));
        awaitServer();
    }

    private int footerTop() {
        return cardY + cardH - pad() / 2 - buttonH();
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
        MenuState.markFromSheet();
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
    static void send(ActionPayload payload) {
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
        for (SheetContext.Hit hit : ctx.hits()) {
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
        if (parent != null && SheetMetrics.inBox(mx, my, cardX + pad(), top, 12, 11)) {
            click();
            this.onClose();
            return true;
        }
        if (SheetMetrics.inBox(mx, my, cardX + cardW - pad() - 10, top, 12, 11)) {
            click();
            this.onClose();
            return true;
        }
        return false;
    }

    private boolean clickFooter(double mx, double my) {
        Runnable action = footer.hit(mx, my, cardX + pad(), footerTop(), contentW(), buttonH());
        if (action == null) return false;
        click();
        action.run();
        return true;
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
        ctx.beginFrame(frameDelta, viewTop, viewBottom, compact());

        drawHeader(graphics, mouseX, mouseY, accent);

        graphics.enableScissor(cardX + 1, viewTop, cardX + cardW - 1, viewBottom);
        int x = cardX + pad();
        int w = contentW();
        int y = viewTop - (int) scroll;
        int start = y;
        y = SheetHero.draw(ctx, graphics, x, y + pad() / 2, w) + ctx.sectionGap();
        if (SheetState.hasData()) {
            y = SheetVitals.draw(ctx, graphics, x, y, w) + ctx.sectionGap();
            y = SheetActing.draw(ctx, graphics, x, y, w) + ctx.sectionGap();
            y = SheetConditions.draw(ctx, graphics, x, y, w);
            y = destinations.draw(graphics, x, y, w, mouseX, mouseY) + ctx.sectionGap();
            y = destinations.preferences(graphics, x, y, w, mouseX, mouseY);
        } else {
            y = drawWaiting(graphics, x, y, w);
        }
        graphics.disableScissor();
        contentHeight = y - start + pad();

        if (maxScroll() > 0) drawScrollbar(graphics, mouseX, mouseY, accent);
        drawFooter(graphics, mouseX, mouseY, accent);
        if (awaitingSince != 0) drawAwaiting(graphics, accent);
        super.extractRenderState(graphics, mouseX, mouseY, partial);

        Component hoveredTip = ctx.hoveredTip();
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
        if (SheetState.pathway().isEmpty()) return;
        graphics.enableScissor(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1);
        MenuIcons.watermark(graphics, this.font,
                new MenuIcon(MenuIcon.Kind.PATHWAY, SheetState.pathway()),
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
            boolean hovered = SheetMetrics.inBox(mouseX, mouseY, left, top, 12, 11);
            MenuTheme.chevron(graphics, left + 1, top + 1, hovered ? accent : CoiStyle.TEXT_MUTED);
            left += 15;
        }
        int closeX = right - 8;
        boolean closeHover = SheetMetrics.inBox(mouseX, mouseY, closeX - 2, top, 12, 11);
        MenuTheme.cross(graphics, closeX, top + 1, closeHover ? MenuTheme.DANGER : CoiStyle.TEXT_MUTED);
        right -= 14;

        if (MenuIcons.draw(graphics, this.font,
                new MenuIcon(MenuIcon.Kind.PATHWAY, SheetState.pathway()), left, top, ICON, 1f)) {
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
        footer.draw(this.font, graphics, cardX + pad(), y, contentW(), buttonH(), accent, mouseX, mouseY);
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
        boolean grabbed = draggingScroll || onHandle(mouseX, mouseY);
        MenuTheme.scrollbar(graphics, trackX(), trackTop(), trackH(), handleY(), handleH(),
                TRACK_W, accent, grabbed);
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

    // --- Empty state ---

    /**
     * Before the first packet lands the sheet still knows who the player is, so
     * the hero panel stays and this explains what is missing — a bare centred
     * line could not tell "still loading" from "wrong server".
     */
    private int drawWaiting(GuiGraphicsExtractor graphics, int x, int y, int w) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_loading", GLYPH_COOLDOWN) + 4;
        for (FormattedCharSequence line : this.font.split(
                Component.translatable("screen.coi.sheet_loading_hint"), w)) {
            graphics.text(this.font, line, x, ry, CoiStyle.TEXT_MUTED);
            ry += PARA_LINE;
        }
        return ry;
    }

}
