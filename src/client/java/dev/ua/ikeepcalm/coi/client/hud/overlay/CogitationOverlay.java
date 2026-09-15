package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.state.CogitationState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * The cogitation prompt card, replacing the vanilla title the server used to
 * send: the action to perform, the current streak, and a bar draining over the
 * action's timeout.
 * <p>
 * The server only sweeps for timeouts every 40 ticks, so the bar can sit empty
 * for a moment before the {@code fail} packet lands — it just holds at 0.
 */
public final class CogitationOverlay {

    private static final Identifier COGITATION_LAYER = Identifier.fromNamespaceAndPath("coi-client", "cogitation");

    public static final int CARD_W = 220;
    public static final int CARD_H = 54;
    /**
     * Default distance above the crosshair; {@code cogitationYOffset} holds
     * the live (signed) value.
     */
    public static final int DEFAULT_Y_OFFSET = -70;
    private static final float LABEL_SCALE = 1.5f;

    private static final int TIME_BAR_W = 200;
    private static final int TIME_BAR_H = 3;
    private static final int TIME_BAR_BG = 0xFF2A2A32;
    private static final int FAIL_COLOR = 0xFFFF5555;

    private CogitationOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, COGITATION_LAYER, CogitationOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings)
                || !settings.showCogitationOverlay || !CogitationState.hasPrompt()) {
            return;
        }

        long now = System.currentTimeMillis();
        int[] pos = anchor(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), settings);

        HudScale.push(ctx, pos[0], pos[1], settings.cogitationScale);
        drawCardAt(ctx, pos[0], pos[1], CogitationState.getLabel(),
                CogitationState.getStreak(), CogitationState.timeFraction(now),
                CogitationState.isFailing(now));
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the card.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return new int[]{screenW / 2 - HudScale.size(CARD_W, s.cogitationScale) / 2 + s.cogitationXOffset,
                screenH / 2 + s.cogitationYOffset};
    }

    /**
     * The whole card from explicit values, so previews need no live session.
     */
    public static void drawCardAt(GuiGraphicsExtractor ctx, int cardX, int cardY,
                                  String label, int streak, double timeFraction, boolean failing) {
        Minecraft client = Minecraft.getInstance();
        CoiStyle.drawCard(ctx, cardX, cardY, CARD_W, CARD_H);
        drawLabel(ctx, client, cardX, cardY + 8, label);
        drawStreak(ctx, client, cardX, cardY + 30, streak);
        drawTimeBar(ctx, cardX, cardY + CARD_H - 10, timeFraction);

        if (failing) {
            String fail = I18n.get("hud.coi.cogitation_fail");
            int failX = cardX + (CARD_W - client.font.width(fail)) / 2;
            ctx.text(client.font, fail, failX, cardY + CARD_H + 4, FAIL_COLOR, true);
        }
    }

    /**
     * The action title, scaled up via the pose so it reads like the title it
     * replaced.
     */
    private static void drawLabel(GuiGraphicsExtractor ctx, Minecraft client, int cardX, int y, String label) {
        float width = client.font.width(label) * LABEL_SCALE;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(cardX + (CARD_W - width) / 2f, y);
        pose.scale(LABEL_SCALE, LABEL_SCALE);
        ctx.text(client.font, label, 0, 0, CoiStyle.ACCENT, true);
        pose.popMatrix();
    }

    private static void drawStreak(GuiGraphicsExtractor ctx, Minecraft client, int cardX, int y, int streakCount) {
        String streak = I18n.get("hud.coi.cogitation_streak", streakCount);
        int x = cardX + (CARD_W - client.font.width(streak)) / 2;
        ctx.text(client.font, streak, x, y, CoiStyle.TEXT_BODY, true);
    }

    private static void drawTimeBar(GuiGraphicsExtractor ctx, int cardX, int y, double fraction) {
        int barX = cardX + (CARD_W - TIME_BAR_W) / 2;
        ctx.fill(barX, y, barX + TIME_BAR_W, y + TIME_BAR_H, TIME_BAR_BG);
        int remaining = CoiBar.lerpWidth(fraction, 1.0, TIME_BAR_W);
        if (remaining > 0) {
            ctx.fill(barX, y, barX + remaining, y + TIME_BAR_H, CoiStyle.ACCENT);
        }
    }
}
