package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * A click the player still has to agree to.
 * <p>
 * {@code confirm} is client-side: the modal is drawn locally and nothing leaves
 * the client until the player agrees, which is what replaces the plugin's
 * two-step chest confirms. While one is raised it owns the screen's input
 * outright — no click reaches the card behind it.
 */
final class MenuConfirmModal {

    private static final int MAX_W = 240;
    private static final int MIN_W = 160;

    /** The card's padding, and the gutter each side of the body text. */
    private static final int PAD = 12;

    private final MenuContext ctx;

    /**
     * The pending click. It carries the source button's style so a destructive
     * confirm reads as destructive, and the wrapped body so the modal is not
     * re-wrapped three times per frame.
     */
    private record Pending(MenuComponent.Confirm confirm, String action,
                           MenuComponent.ButtonStyle style, List<FormattedCharSequence> body) {
    }

    private Pending pending;

    MenuConfirmModal(MenuContext ctx) {
        this.ctx = ctx;
    }

    boolean active() {
        return pending != null;
    }

    void raise(MenuComponent.Button button) {
        pending = new Pending(button.confirm(), button.id(), button.style(),
                ctx.font().split(Component.literal(button.confirm().body()), width() - PAD * 2));
    }

    void dismiss() {
        pending = null;
    }

    private int width() {
        return Math.min(MAX_W, Math.max(MIN_W, ctx.screenWidth() - 20));
    }

    private int height() {
        return PAD + 9 + 6 + pending.body().size() * 9 + 10 + ctx.buttonH() + PAD;
    }

    private int x() {
        return (ctx.screenWidth() - width()) / 2;
    }

    private int y() {
        return (ctx.screenHeight() - height()) / 2;
    }

    private List<MenuComponent.Button> buttons() {
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

    private int buttonRowY() {
        return y() + height() - PAD - ctx.buttonH();
    }

    void draw(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(0, 0, ctx.screenWidth(), ctx.screenHeight(), 0xB0000000);
        int x = x();
        int y = y();
        int w = width();
        CoiStyle.drawCard(g, x, y, w, height());
        g.fill(x, y, x + w, y + 1, ctx.accent());

        String title = pending.confirm().title().isEmpty()
                ? Component.translatable("screen.coi.menu_confirm_title").getString()
                : pending.confirm().title();
        g.text(ctx.font(), ctx.font().plainSubstrByWidth(title, w - PAD * 2), x + PAD, y + PAD,
                ctx.accent(), true);

        int lineY = y + PAD + 15;
        for (FormattedCharSequence line : pending.body()) {
            g.text(ctx.font(), line, x + PAD, lineY, CoiStyle.TEXT_BODY, false);
            lineY += 9;
        }
        ctx.drawButtons(g, buttons(), x + PAD, buttonRowY(), w - PAD * 2, 2, ctx.buttonH(), mouseX, mouseY);
    }

    /**
     * Swallows every click while raised: the card behind is not reachable until
     * the player has answered.
     */
    boolean click(double mx, double my) {
        int index = ctx.hitButton(mx, my, 2, x() + PAD, buttonRowY(), width() - PAD * 2, 2, ctx.buttonH());
        if (index == 0) {
            pending = null;
        } else if (index == 1) {
            accept();
        }
        return true;
    }

    /**
     * Swallows every key while raised, for the same reason {@link #click} does.
     */
    boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            pending = null;
        } else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            accept();
        }
        return true;
    }

    private void accept() {
        String action = pending.action();
        pending = null;
        ctx.fire(action, null);
    }
}
