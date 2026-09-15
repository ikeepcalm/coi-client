package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.state.NotificationState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * Toast stack in the top-right corner for {@code coi-client:notify}: sequence
 * advancements, acting successes, bounty completions, madness stage crossings.
 * <p>
 * Each toast slides in, holds for its own duration and fades out; under
 * {@code epilepsyMode} the slide is dropped and it simply fades.
 */
public final class NotificationOverlay {

    private static final Identifier NOTIFY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "notify");

    public static final int CARD_W = 180;
    private static final int BODY_W = 164;
    /**
     * Default margin from the top-right corner; {@code notificationXOffset} /
     * {@code notificationYOffset} hold the live values.
     */
    public static final int DEFAULT_MARGIN = 12;
    public static final int CARD_GAP = 6;
    /**
     * Height of a one-line toast - what the layout editor reserves per slot.
     */
    public static final int CARD_H_1_LINE = 32;
    private static final int ACCENT_W = 2;
    private static final int LINE_H = 10;

    private NotificationOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, NOTIFY_LAYER, NotificationOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showNotifications) {
            return;
        }

        long now = System.currentTimeMillis();
        List<NotificationState.Toast> toasts = NotificationState.visibleToasts(now);
        if (toasts.isEmpty()) return;

        // The card's left edge is the fill origin, so the stack grows right
        // and down from it instead of drifting off the corner
        int[] pos = anchor(client.getWindow().getGuiScaledWidth(), settings);
        int right = pos[0] + CARD_W;
        int y = pos[1];

        HudScale.push(ctx, pos[0], pos[1], settings.notificationScale);
        for (NotificationState.Toast toast : toasts) {
            y += drawToast(ctx, client, toast, right, y, now, settings.epilepsyMode) + CARD_GAP;
        }
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the first card — what the whole stack scales about.
     */
    public static int[] anchor(int screenW, HudConfig.HudSettings s) {
        return new int[]{screenW - s.notificationXOffset - HudScale.size(CARD_W, s.notificationScale),
                s.notificationYOffset};
    }

    /**
     * @return the card's height, so the caller can stack the next one below it
     */
    private static int drawToast(GuiGraphicsExtractor ctx, Minecraft client, NotificationState.Toast toast,
                                 int right, int y, long now, boolean epilepsyMode) {
        List<FormattedCharSequence> body = toast.body().isEmpty()
                ? List.of()
                : client.font.split(Component.literal(toast.body()), BODY_W);
        int cardH = 14 + LINE_H * body.size() + 8;

        float alpha = toast.alpha(now, epilepsyMode);
        if (alpha < 0.02f) return cardH;

        int x = right - CARD_W + toast.slideOffset(now, CARD_W + DEFAULT_MARGIN, epilepsyMode);
        drawCard(ctx, x, y, cardH, toast.title(), body, toast.argb(), alpha);
        return cardH;
    }

    /**
     * A card from explicit values, for previews that must not push real toasts.
     */
    public static void drawToastAt(GuiGraphicsExtractor ctx, int x, int y, String title, String body, int argb, float alpha) {
        Minecraft client = Minecraft.getInstance();
        List<FormattedCharSequence> lines = body.isEmpty()
                ? List.of()
                : client.font.split(Component.literal(body), BODY_W);
        drawCard(ctx, x, y, 14 + LINE_H * lines.size() + 8, title, lines, argb, alpha);
    }

    private static void drawCard(GuiGraphicsExtractor ctx, int x, int y, int cardH, String title,
                                 List<FormattedCharSequence> body, int argb, float alpha) {
        Minecraft client = Minecraft.getInstance();
        CoiStyle.drawCard(ctx, x, y, CARD_W, cardH);
        ctx.fill(x, y, x + ACCENT_W, y + cardH, CoiBar.withAlpha(argb, alpha));

        ctx.text(client.font, title, x + ACCENT_W + 6, y + 5, CoiBar.withAlpha(argb, alpha), true);
        for (int i = 0; i < body.size(); i++) {
            ctx.text(client.font, body.get(i), x + ACCENT_W + 6, y + 16 + i * LINE_H,
                    CoiBar.withAlpha(CoiStyle.TEXT_BODY, alpha), true);
        }
    }
}
