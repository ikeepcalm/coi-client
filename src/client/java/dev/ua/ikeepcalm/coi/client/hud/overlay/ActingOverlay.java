package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.state.ActingState;

import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Acting progress toward the next sequence, in the pathway's own colour.
 * <p>
 * Acting is the core progression loop and had no on-screen surface at all —
 * only item lore and {@code /coi acting}. The bar shows how far the current
 * sequence has come, the method cooldown when one is running, and a short
 * {@code +N} popup whenever the server reports a grant.
 */
public final class ActingOverlay {

    private static final Identifier ACTING_LAYER = Identifier.fromNamespaceAndPath("coi-client", "acting");

    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 4;

    private static final int BORDER_COLOR = 0xCC000000;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    private ActingOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ACTING_LAYER, ActingOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        // Outer pathways have no acting progression at all, so no bar for them;
        // the character plate carries the same number as its mask gauge, so the
        // bar stands down rather than drawing it twice
        if (HudGate.blocked(client, settings)
                || settings.showCharacterPlate
                || !settings.showActingBar || !ActingState.hasData() || ActingState.isOuter()) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.actingScale);
        drawBar(ctx, client, pos[0], pos[1]);
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the bar, shared with the tour and the layout editor.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.actingAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.actingScale),
                s.actingXOffset, s.actingYOffset, s.actingYOffset);
    }

    /**
     * Plain bar plus label at an explicit percentage and pathway colour — no
     * method cooldown, no grant popup, no state read. The layout editor's
     * preview draws through this.
     */
    public static void drawBarAt(GuiGraphicsExtractor ctx, int barX, int barY, double percent, int rgb, String label) {
        Minecraft client = Minecraft.getInstance();
        int fillWidth = CoiBar.lerpWidth(Math.clamp(percent, 0, 100), 100, BAR_WIDTH);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, 0xFF000000 | rgb, darken(rgb));
        CoiBar.label(ctx, client.font, label, barX, barY, BAR_WIDTH, LABEL_COLOR);
    }

    private static void drawBar(GuiGraphicsExtractor ctx, Minecraft client, int barX, int barY) {
        int rgb = Pathways.pathwayRgb(ActingState.getPathway());
        double percent = Math.clamp(ActingState.getPercent(), 0, 100);
        int fillWidth = CoiBar.lerpWidth(percent, 100, BAR_WIDTH);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, 0xFF000000 | rgb, darken(rgb));

        CoiBar.label(ctx, client.font, labelText(percent), barX, barY, BAR_WIDTH, LABEL_COLOR);

        int granted = ActingState.activeGrant();
        if (granted > 0) {
            drawGainPopup(ctx, client, barX, barY, rgb, granted);
        }
    }

    private static String labelText(double percent) {
        String text = I18n.get("hud.coi.acting_label", String.format(Locale.ROOT, "%.1f", percent));
        if (ActingState.cooldownRemainingNow() > 0) {
            text = text + " · " + I18n.get("hud.coi.acting_cooldown", ActingState.cooldownClock());
        }
        return text;
    }

    /**
     * {@code +N} drifting up out of the label and fading, so a grant is
     * noticeable without another action-bar line.
     */
    private static void drawGainPopup(GuiGraphicsExtractor ctx, Minecraft client, int barX, int barY, int rgb, int granted) {
        float progress = ActingState.grantProgress();
        String text = I18n.get("hud.coi.acting_gain", granted);
        int textX = barX + (BAR_WIDTH - client.font.width(text)) / 2;
        int textY = barY - 22 - (int) (4 * progress);
        ctx.text(client.font, text, textX, textY, EffectPaint.argb(rgb, (int) (255 * (1f - progress))), true);
    }

    /**
     * Bottom edge of the fill gradient: the same hue at ~55% brightness.
     */
    private static int darken(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.55f);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.55f);
        int b = (int) ((rgb & 0xFF) * 0.55f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
