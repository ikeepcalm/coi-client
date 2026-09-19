package dev.ua.ikeepcalm.coi.client.screen.menu;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.FIELD_H;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.GAP;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.HOVER_MS;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.network.payload.MenuActionPayload;
import dev.ua.ikeepcalm.coi.client.state.MenuState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/**
 * Renders a server-authored {@link MenuDocument} — the native replacement for
 * the plugin's InvUI chest GUIs.
 * <p>
 * One card, scrolled in pixels, built from a flat {@link MenuPart} list so
 * layout, hit-testing and the scrollbar all walk the same structure even though
 * no two component types are the same height. Nothing here is a vanilla widget
 * except the text fields: buttons are drawn by {@link MenuTheme} and hit-tested
 * by hand, which is what lets them scroll inside a scissor and carry a second
 * line and a disabled reason.
 * <p>
 * This class owns the screen — its lifecycle, its scroll position, where a
 * click goes, and the guard that stops a send reaching nothing. What a document
 * is <em>made of</em> lives beside it: {@link MenuPartFactory} lays the
 * components out, the {@code Menu*Parts} classes draw them,
 * {@link MenuScrollbar} carries the scroll and {@link MenuConfirmModal} the
 * questions. Each of them reaches back through {@link MenuContext}, which is
 * the whole of what a collaborator may ask of the screen.
 * <p>
 * The screen owns no navigation. Every click goes back on
 * {@code coi-client:menu_action} with the newest session and version, and the
 * server answers with the next document; a document whose {@code screen} id is
 * unchanged is a refresh, so scroll position, typed text <em>and</em> whatever
 * the player has opened or collapsed survive it.
 */
public class MenuScreen extends Screen implements MenuContext {

    private static final int SCROLL_STEP = 20;

    private final Screen parent;
    private final MenuChrome chrome = new MenuChrome(this);
    private final MenuScrollbar scrollbar = new MenuScrollbar();
    private final MenuConfirmModal confirm = new MenuConfirmModal(this);
    private final MenuSpecimen specimen = new MenuSpecimen();
    private int specimenW;
    private int specimenH;

    private MenuDocument doc;
    private int docRevision = -1;
    private long adoptedAt;

    private final List<MenuPart> parts = new ArrayList<>();
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

    private int contentH;
    private int cardX, cardY, cardW, cardH;
    private int viewTop, viewBottom;

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

    private boolean closeSent;

    public MenuScreen(Screen parent) {
        super(Component.translatable("screen.coi.menu_title"));
        this.parent = parent;
    }

    @Override
    public boolean compact() {
        return this.height < 300;
    }

    @Override
    public int pad() {
        return compact() ? 8 : 12;
    }

    @Override
    public int buttonH() {
        return compact() ? 16 : 20;
    }

    @Override
    public int contentW() {
        return cardW - pad() * 2;
    }

    @Override
    public int accent() {
        return doc != null ? doc.accentArgb() : CoiStyle.ACCENT;
    }

    @Override
    public boolean archival() {
        return doc != null && doc.presentation().specimen();
    }

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    public int screenWidth() {
        return this.width;
    }

    @Override
    public int screenHeight() {
        return this.height;
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
        MenuState.clear();
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
                MenuState.session(), MenuState.version(), action, value));
    }

    private void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void fire(String action, String value) {
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
        MenuDocument next = MenuState.document();
        if (next == null) return;

        boolean sameScreen = doc != null && doc.screen().equals(next.screen());
        if (!sameScreen) {
            specimen.reset();
            scrollbar.toTop();
            fields.clear();
            focusedField = null;
            // Disclosure belongs to the screen id, not to the document version:
            // this is the only place it may be forgotten
            disclosure.clear();
            chevronT.clear();
        }
        if (docRevision != MenuState.revision()) {
            adoptedAt = System.currentTimeMillis();
        }

        doc = next;
        docRevision = MenuState.revision();
        confirm.dismiss();
        pressedAction = null;

        layout();
    }

    private void layout() {
        cardW = CoiStyle.cardWidth(this.width, compact());
        cardX = (this.width - cardW) / 2;
        specimenW = 0;
        specimenH = 0;
        if (doc.presentation().specimen()) {
            int totalW = Math.min(960, this.width - 32);
            if (totalW >= 620) {
                specimenW = Math.min(340, totalW / 3);
                cardW = totalW - specimenW - 12;
                cardX = (this.width - totalW) / 2 + specimenW + 12;
            } else {
                specimenH = Math.clamp((this.height - 90) / 2, 100, 160);
            }
        } else {
            specimen.clear();
        }

        chrome.measure(doc);

        contentH = MenuPartFactory.build(this, doc, parts);
        if (specimenH > 0) {
            for (MenuPart part : parts) part.y += specimenH + 10;
            contentH += specimenH + 10;
        }

        int margin = compact() ? 8 : 18;
        int availH = Math.max(80, this.height - margin * 2);
        cardH = Math.min(availH, chrome.headerH() + chrome.noticeH() + contentH + pad() + chrome.footerH());
        if (specimenW > 0) cardH = availH;
        cardY = (this.height - cardH) / 2;
        viewTop = cardY + chrome.headerH() + chrome.noticeH();
        viewBottom = Math.max(viewTop + 20, cardY + cardH - chrome.footerH() - pad() / 2);

        chrome.place(cardX, cardY, cardW, cardH);
        scrollbar.place(cardX, cardW, viewTop, viewBottom, contentH);
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
    @Override
    public float approach(float current, float target, float durationMs) {
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
    @Override
    public boolean disclosed(String id, boolean initial) {
        return disclosure.getOrDefault(disclosureKey(id), initial);
    }

    @Override
    public void toggleDisclosure(String id, boolean initial) {
        disclosure.put(disclosureKey(id), !disclosed(id, initial));
        click();
        // Heights changed, so the whole card has to be laid out again
        layout();
    }

    /**
     * The eased rotation of one disclosure chevron, seeded to its resting state
     * so the first frame after a rebuild does not replay the animation.
     */
    @Override
    public float chevron(String id, boolean open) {
        String key = disclosureKey(id);
        float next = approach(chevronT.getOrDefault(key, open ? 1f : 0f), open ? 1f : 0f, HOVER_MS);
        chevronT.put(key, next);
        return next;
    }

    @Override
    public void relayout() {
        layout();
    }

    // --- Button rows (footer, buttons component, confirm modal all share this) ---

    @Override
    public int rowHeightFor(List<MenuComponent.Button> buttons) {
        boolean twoLine = buttons.stream().anyMatch(b -> !b.desc().isEmpty());
        return buttonH() + (twoLine ? 10 : 0);
    }

    @Override
    public void drawButtons(GuiGraphicsExtractor g, List<MenuComponent.Button> buttons,
                             int x, int y, int width, int cols, int rowH, int mouseX, int mouseY) {
        int bw = (width - (cols - 1) * GAP) / cols;
        for (int i = 0; i < buttons.size(); i++) {
            MenuComponent.Button button = buttons.get(i);
            int bx = x + (i % cols) * (bw + GAP);
            int by = y + (i / cols) * (rowH + GAP);
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, bx, by, bw, rowH);
            MenuTheme.button(g, this.font, bx, by, bw, rowH, button, accent(),
                    hovered, hovered && button.id().equals(pressedAction));
            if (hovered && !button.enabled() && !button.disabledReason().isEmpty()) {
                tooltip = List.of(Component.literal(button.disabledReason()).withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public int hitButton(double mx, double my, int count, int x, int y, int width, int cols, int rowH) {
        int bw = (width - (cols - 1) * GAP) / cols;
        for (int i = 0; i < count; i++) {
            int bx = x + (i % cols) * (bw + GAP);
            int by = y + (i / cols) * (rowH + GAP);
            if (MenuMetrics.inBox(mx, my, bx, by, bw, rowH)) return i;
        }
        return -1;
    }

    @Override
    public void hint(String text) {
        if (!text.isEmpty()) tooltip = List.of(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void reason(String text) {
        if (!text.isEmpty()) tooltip = List.of(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    /**
     * The one piece of chrome text the client owns on this screen. A chevron is
     * a small target and a silent one; naming the control is what keeps a
     * collapsed section from reading as an empty one.
     */
    @Override
    public void disclosureHint(boolean open) {
        hint(Component.translatable(open ? "screen.coi.menu_collapse" : "screen.coi.menu_expand").getString());
    }

    @Override
    public void lines(List<String> body, String lead) {
        List<Component> out = new ArrayList<>();
        if (!lead.isEmpty()) out.add(Component.literal(lead));
        body.forEach(line -> out.add(Component.literal(line).withStyle(ChatFormatting.GRAY)));
        if (!out.isEmpty()) tooltip = out;
    }

    @Override
    public EditBox field(String key, Supplier<EditBox> factory) {
        return fields.computeIfAbsent(key, ignored -> factory.get());
    }

    /**
     * Every text field on this card is drawn borderless over
     * {@link MenuTheme#field}: the vanilla chrome is a black box in a white
     * border, which reads as a hole punched through the card.
     */
    @Override
    public void drawField(GuiGraphicsExtractor g, EditBox box, int x, int y, int w, int mouseX, int mouseY) {
        MenuTheme.field(g, x, y, w, FIELD_H, box == focusedField, accent());
        box.setX(x + 4);
        box.setY(y + (FIELD_H - 8) / 2);
        box.setWidth(Math.max(8, w - 8));
        box.extractRenderState(g, mouseX, mouseY, 0f);
    }

    // --- Rendering ---

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        if (docRevision != MenuState.revision()) rebuild();
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

        if (doc.presentation().specimen()) ArchivePaint.folio(g, cardX, cardY, cardW, cardH);
        else CoiStyle.drawCard(g, cardX, cardY, cardW, cardH);
        // CoiStyle's top rule is the mod's gold; a document that named its own
        // accent owns that edge too
        if (!doc.presentation().specimen()) {
            g.fill(cardX, cardY, cardX + cardW, cardY + 1, accent());
            chrome.drawWatermark(g);
        }
        if (specimenW > 0) specimen.draw(g, font, doc, cardX - specimenW - 12, cardY,
                specimenW, cardH, cardY, cardY + cardH);

        chrome.drawHeader(g, mouseX, mouseY);
        chrome.drawNotice(g, adoptedAt);
        drawContent(g, mouseX, mouseY);
        chrome.drawFooter(g, mouseX, mouseY);

        if (confirm.active()) {
            confirm.draw(g, mouseX, mouseY);
        } else if (tooltip != null) {
            g.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }

        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean inView = mouseY >= viewTop && mouseY < viewBottom
                && mouseX >= cardX && mouseX < cardX + cardW;
        int hoverY = inView ? mouseY : Integer.MIN_VALUE;

        g.enableScissor(cardX + 1, viewTop, cardX + cardW - 1, viewBottom);
        int originX = cardX + pad();
        if (specimenH > 0) specimen.draw(g, font, doc, originX, viewTop - scrollbar.scrollY(),
                contentW(), specimenH, viewTop, viewBottom);
        for (MenuPart part : parts) {
            int top = viewTop + part.y - scrollbar.scrollY();
            if (top + part.height < viewTop || top > viewBottom) continue;
            part.render(g, originX, top, mouseX, hoverY);
        }
        g.disableScissor();

        if (scrollbar.maxScroll() > 0) scrollbar.draw(g, mouseX, mouseY, accent());
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (doc == null) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();

        if (confirm.active()) return confirm.click(mx, my);
        if (doc.presentation().specimen() && specimen.press(mx, my)) return true;
        if (clickHeader(mx, my)) return true;
        if (chrome.clickFooter(mx, my)) return true;

        if (scrollbar.press(mx, my)) return true;

        if (my >= viewTop && my < viewBottom && mx >= cardX && mx < cardX + cardW) {
            focusField(null);
            int originX = cardX + pad();
            for (MenuPart part : parts) {
                int top = viewTop + part.y - scrollbar.scrollY();
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
        if (confirm.active()) return true;
        if (specimen.drag(dragX)) return true;
        if (scrollbar.drag(event.y())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    /**
     * The header's two controls are the chrome's to place, so they are the
     * chrome's to hit-test; what they mean is the screen's.
     */
    private boolean clickHeader(double mx, double my) {
        switch (chrome.hitHeader(mx, my)) {
            case BACK -> fire(MenuActionPayload.BACK, null);
            case CLOSE -> onClose();
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    /**
     * The one gate between a click and the wire: a button carrying a
     * {@code confirm} raises the modal and sends nothing until the player
     * agrees, which is what the plugin's two-step chest confirms become.
     */
    @Override
    public void activate(MenuComponent.Button button, String value) {
        if (!button.enabled()) return;
        pressedAction = button.id();
        if (button.confirm() != null) {
            click();
            confirm.raise(button);
            return;
        }
        fire(button.id(), value);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        specimen.release();
        pressedAction = null;
        scrollbar.release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (confirm.active()) return true;
        if (doc != null && doc.presentation().specimen() && specimen.zoom(mouseX, mouseY, vertical)) return true;
        scrollbar.scrollBy(-(int) Math.signum(vertical) * SCROLL_STEP);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (confirm.active()) return confirm.keyPressed(event);
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
        if (!confirm.active() && focusedField != null && focusedField.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    public void focusField(EditBox box) {
        if (focusedField != null && focusedField != box) focusedField.setFocused(false);
        focusedField = box;
        if (box != null) box.setFocused(true);
    }
}
