package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.state.TargetState;

import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Health of the Beyonder you just hit, under the crosshair.
 * <p>
 * Replaces the server's {@code ♥}-text bar, which cost 60 action-bar publishes
 * per hit. The chunk between {@code before} and {@code after} flashes white
 * briefly so the damage itself is legible, then settles to dark red.
 */
public final class TargetHealthOverlay {

    private static final Identifier TARGET_LAYER = Identifier.fromNamespaceAndPath("coi-client", "target");

    public static final int BAR_WIDTH = 100;
    public static final int BAR_HEIGHT = 5;
    /**
     * Default distance below the crosshair; {@code targetHealthYOffset} holds
     * the live value.
     */
    public static final int DEFAULT_Y_OFFSET = 18;

    private static final int BORDER_COLOR = 0xCC000000;
    private static final int LOST_FLASH = 0xFFFFFFFF;
    private static final int LOST_SETTLED = 0xFF6E1414;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    private TargetHealthOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, TARGET_LAYER, TargetHealthOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();
        long now = System.currentTimeMillis();

        if (HudGate.blocked(client, settings)
                || !settings.showTargetHealth || !TargetState.isVisible(now)) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.targetHealthScale);
        drawBar(ctx, pos[0], pos[1], now);
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the bar itself (the name sits 10px above it).
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return new int[]{screenW / 2 - HudScale.size(BAR_WIDTH, s.targetHealthScale) / 2 + s.targetHealthXOffset,
                screenH / 2 + s.targetHealthYOffset};
    }

    /**
     * The bar from explicit values, for previews.
     */
    public static void drawBarAt(GuiGraphicsExtractor ctx, int barX, int barY, String name,
                                 float after, float before, double health, double max, boolean flashing) {
        Minecraft client = Minecraft.getInstance();
        int afterW = CoiBar.lerpWidth(after, 1.0, BAR_WIDTH);
        int beforeW = CoiBar.lerpWidth(Math.max(after, before), 1.0, BAR_WIDTH);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);
        if (beforeW > afterW) {
            ctx.fill(barX + afterW, barY, barX + beforeW, barY + BAR_HEIGHT, flashing ? LOST_FLASH : LOST_SETTLED);
        }
        int fill = healthColor(after);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, afterW, fill, darken(fill));

        if (!name.isEmpty()) {
            CoiBar.label(ctx, client.font, name, barX, barY, BAR_WIDTH, LABEL_COLOR);
        }
        String readout = I18n.get("hud.coi.target_health", format(health), format(max),
                String.format(Locale.ROOT, "%.0f", after * 100f));
        int textX = barX + (BAR_WIDTH - client.font.width(readout)) / 2;
        ctx.text(client.font, readout, textX, barY + BAR_HEIGHT + 3, LABEL_COLOR, true);
    }

    private static void drawBar(GuiGraphicsExtractor ctx, int barX, int barY, long now) {
        drawBarAt(ctx, barX, barY, TargetState.getName(),
                TargetState.getAfter(), TargetState.getBefore(),
                TargetState.getHealth(), TargetState.getMax(),
                TargetState.isFlashing(now));
    }

    private static String format(double value) {
        return value == Math.floor(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Bottom edge of the fill gradient: the same hue at ~60% brightness.
     */
    private static int darken(int argb) {
        int r = (int) (((argb >> 16) & 0xFF) * 0.6f);
        int g = (int) (((argb >> 8) & 0xFF) * 0.6f);
        int b = (int) ((argb & 0xFF) * 0.6f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Same thresholds the server's text bar used: 75/50/25 percent.
     */
    private static int healthColor(float fraction) {
        if (fraction > 0.75f) return 0xFF55FF55;
        if (fraction > 0.5f) return 0xFFFFFF55;
        if (fraction > 0.25f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }
}
