package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.hud.render.MadnessCorruption;
import dev.ua.ikeepcalm.coi.client.hud.render.MadnessPalette;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;

import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The madness bar, plus the stage effects madness puts over the whole screen.
 * <p>
 * The two halves are deliberately independent. The vignette and VHS glitches
 * are the world reacting to the player, so they run whatever else is on the
 * HUD — including when the character plate has taken the bar over — and they
 * cover the whole window, so they stay <em>outside</em> the bar's scale push.
 * The bar itself is a readout and stands down for the plate's sanity gauge.
 * <p>
 * From stage 2 the bar stops being a clean rail: it trembles, tears, cracks
 * and eventually corrupts its own label. Each of those is one layer below,
 * drawn in the order they stack — on the cadence
 * {@link MadnessCorruption#bursting} hands out, which is also what the screen
 * effects run on, so the HUD comes apart as one thing.
 */
public final class MadnessOverlay {

    private static final Identifier MADNESS_LAYER = Identifier.fromNamespaceAndPath("coi-client", "madness");

    /**
     * Geometry shared with {@code TourScreen}'s spotlight and the layout
     * editor: the bar is always 182×6, and {@code madnessYOffset} places it
     * for both anchor families (from the top edge for TOP_*, from the bottom
     * for BOTTOM_*), so it can be dragged either way.
     */
    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 6;
    /**
     * Where a TOP-anchored bar sits out of the box.
     */
    public static final int DEFAULT_TOP_Y = 20;

    /**
     * How long the shimmer takes to sweep the fill at the calm stages.
     */
    private static final long SHIMMER_PERIOD_MS = 3000;

    // Smoothed display value so the bar glides toward the target instead of snapping
    private static double shownMadness = -1;
    private static long lastFrameMs = 0;

    private MadnessOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MADNESS_LAYER, MadnessOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showMadnessBar) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        double madness = BeyonderState.getMadness();
        double permMadness = BeyonderState.getPermanentMadness();
        int freezeStacks = BeyonderState.getFreezeStacks();
        int mentalPressure = BeyonderState.getMentalPressure();
        double tiredness = BeyonderState.getTiredness();

        // 1. Determine Madness Stage
        int stage = stageOf(madness);

        // 2. Screen-wide effects cover the whole window, so they stay
        // outside the bar's scale push
        MadnessCorruption.screenEffects(ctx, w, h, stage);

        // 3. The character plate carries madness as its sanity gauge, so the
        // bar stands down for it — but only the bar. The stage effects above
        // are the world reacting to the player, not a readout, and run either way
        if (settings.showCharacterPlate) return;

        // 4. Render the Madness Bar, scaled about its own fill origin
        int[] pos = anchor(w, h, settings);
        HudScale.push(ctx, pos[0], pos[1], settings.madnessScale);
        renderMadnessBar(ctx, client, pos, madness, permMadness, freezeStacks, mentalPressure, tiredness, stage);
        HudScale.pop(ctx);
    }

    private static void renderMadnessBar(GuiGraphicsExtractor ctx, Minecraft client, int[] pos,
                                         double madness, double permMadness, int freezeStacks, int mentalPressure, double tiredness, int stage) {
        Font textRenderer = client.font;
        long time = System.currentTimeMillis();

        smoothTowards(madness, time);

        // Position coordinates
        int barWidth = BAR_WIDTH;
        int barHeight = BAR_HEIGHT;

        float flash = BeyonderState.getFlashIntensity();
        int[] shaken = shake(pos[0], pos[1], stage, time, flash);
        int barX = shaken[0];
        int barY = shaken[1];

        MadnessPalette.Style style = MadnessPalette.of(stage, time);
        int mainColorTop = style.top();
        int mainColorBottom = style.bottom();
        int textColor = style.text();
        int borderColor = style.border();
        String statusName = style.status();

        // 1. Border + background with a subtle depth gradient
        CoiBar.frame(ctx, barX, barY, barWidth, barHeight, borderColor);

        // 2. permanentMadness region (Min Floor)
        int permWidth = CoiBar.lerpWidth(permMadness, 100.0, barWidth);
        if (permWidth > 0) {
            // 50% opacity of top color
            int permColor = (mainColorTop & 0x00FFFFFF) | (0x60 << 24);
            ctx.fill(barX, barY, barX + permWidth, barY + barHeight, permColor);
        }

        // 3. Primary filled region for current (smoothed) madness
        int madnessWidth = unsteadyWidth(barWidth, stage, time);

        boolean burst = MadnessCorruption.bursting(time); // shared cadence with the glitch overlay

        // Stage 4: RGB-split ghost copies bleeding out from under the fill
        if (stage == 4 && madnessWidth > 0 && burst) {
            ctx.fill(barX - 1, barY, barX - 1 + madnessWidth, barY + barHeight, 0x50FF2299);
            ctx.fill(barX + 1, barY, barX + 1 + Math.min(madnessWidth, barWidth - 1), barY + barHeight, 0x5033EEFF);
        }

        CoiBar.fill(ctx, barX, barY, barHeight, madnessWidth, mainColorTop, mainColorBottom);

        // 4. Calm stages get a slow shimmer sweeping across the fill
        if (stage <= 1) {
            CoiBar.shimmer(ctx, barX, barY, barHeight, madnessWidth, time, SHIMMER_PERIOD_MS);
        }

        // 5. Glitch slices tear slivers out of the bar at high madness
        if (stage >= 3 && madnessWidth > 0 && burst) {
            drawSlices(ctx, barX, barY, barWidth, barHeight, madnessWidth, stage, time, mainColorTop);
        }

        // 6. Hairline cracks spread across the damaged bar (deterministic pattern)
        if (stage >= 2) {
            drawCracks(ctx, barX, barY, barWidth, barHeight, stage);
        }

        // 7. Static noise specks over the bar when fully gone
        if (stage == 4 && burst) {
            drawStatic(ctx, barX, barY, barWidth, barHeight, time);
        }

        // 8. Threshold notches at 25 / 50 / 75
        CoiBar.notches(ctx, barX, barY, barWidth, barHeight, 4, 0x50000000);

        // 9. permanentMadness marker line — blinks once the mind starts slipping
        if (permWidth > 0 && permWidth <= barWidth) {
            int markerA = 0xDD;
            if (stage >= 3) {
                markerA = (int) (0x66 + 0x77 * (0.5f + 0.5f * Math.sin(time * 0.012)));
            }
            ctx.fill(barX + permWidth - 1, barY - 1, barX + permWidth + 1, barY + barHeight + 1, (markerA << 24) | 0xFFFFFF);
        }

        // 10. Brief Flash overlay on madness increase
        if (flash > 0 && madnessWidth > 0) {
            int flashColor = ((int) (flash * 160) << 24) | 0xFFFFFF;
            ctx.fill(barX, barY, barX + madnessWidth, barY + barHeight, flashColor);
        }

        // 11. Text Indicator: Madness: 42.5% / 100% (Status, Min: 5.0%)
        String text = label(madness, statusName, permMadness);
        if (stage == 4 && burst) {
            text = MadnessCorruption.corruptText(text, time);
        }
        CoiBar.label(ctx, textRenderer, text, barX, barY, barWidth, textColor);

        // 12. Render other conditions (Freeze, Mental Pressure, Tiredness) below the bar
        drawExtras(ctx, textRenderer, barX, barY, barWidth, barHeight, freezeStacks, mentalPressure, tiredness);
    }

    /**
     * Eases {@link #shownMadness} toward the real value, framerate-independently
     * and with a dead band so it settles instead of creeping.
     */
    private static void smoothTowards(double madness, long time) {
        if (shownMadness < 0) shownMadness = madness;
        float dt = lastFrameMs == 0 ? 0.016f : Math.min(0.1f, (time - lastFrameMs) / 1000f);
        lastFrameMs = time;
        shownMadness += (madness - shownMadness) * Math.min(1f, dt * 10f);
        if (Math.abs(shownMadness - madness) < 0.05) shownMadness = madness;
    }

    /**
     * The bar's drawing origin once the tremor of high madness and the shake of
     * a fresh madness gain have both been applied.
     */
    private static int[] shake(int barX, int barY, int stage, long time, float flash) {
        // The whole bar trembles at high madness
        if (stage >= 3) {
            Random tremor = new Random((time / 40) * MadnessCorruption.SEED_MIX);
            int amp = stage == 4 ? 2 : 1;
            barX += tremor.nextInt(amp * 2 + 1) - amp;
            barY += tremor.nextInt(amp * 2 + 1) - amp;
        }

        // Apply screen shake offset on madness increase
        if (flash > 0) {
            Random rand = new Random();
            barX += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
            barY += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
        }
        return new int[]{barX, barY};
    }

    /**
     * The fill width for the smoothed value — wavering by a pixel or so from
     * stage 2, because an unstable mind can't hold a steady edge.
     */
    private static int unsteadyWidth(int barWidth, int stage, long time) {
        int madnessWidth = CoiBar.lerpWidth(shownMadness, 100.0, barWidth);
        if (stage >= 2 && madnessWidth > 2 && madnessWidth < barWidth - 2) {
            madnessWidth += (int) Math.round(Math.sin(time * 0.02) * (stage >= 3 ? 1.5 : 1.0));
            madnessWidth = Mth.clamp(madnessWidth, 0, barWidth);
        }
        return madnessWidth;
    }

    /**
     * Horizontal tears: a black sliver punched out of the bar with the fill
     * redrawn beside it, as if the row had slipped sideways.
     */
    private static void drawSlices(GuiGraphicsExtractor ctx, int barX, int barY, int barWidth, int barHeight,
                                   int madnessWidth, int stage, long time, int fillColor) {
        Random rng = new Random((time / 70) * 31L);
        int slices = stage == 4 ? 2 : 1;
        for (int i = 0; i < slices; i++) {
            int sy = barY + rng.nextInt(barHeight - 1);
            int sh = 1 + rng.nextInt(2);
            int off = (rng.nextBoolean() ? -1 : 1) * (1 + rng.nextInt(stage == 4 ? 3 : 2));
            ctx.fill(barX, sy, barX + barWidth, sy + sh, 0xFF0F0F11);
            ctx.fill(barX + off, sy, barX + off + madnessWidth, sy + sh, fillColor);
        }
    }

    /**
     * Hairlines across the rail, 3 per stage. The seed is fixed so the damage
     * stays where it was rather than crawling every frame.
     */
    private static void drawCracks(GuiGraphicsExtractor ctx, int barX, int barY, int barWidth, int barHeight, int stage) {
        Random crackRng = new Random(9137L);
        int crackCount = (stage - 1) * 3; // 3 / 6 / 9
        for (int i = 0; i < crackCount; i++) {
            float cx = barX + crackRng.nextFloat() * barWidth;
            float cy = barY + crackRng.nextFloat() * barHeight;
            float ang = (crackRng.nextFloat() - 0.5f) * 2.4f;
            float len = 3f + crackRng.nextFloat() * 5f;
            EffectPaint.line(ctx, cx, cy,
                    cx + (float) Math.cos(ang) * len,
                    cy + (float) Math.sin(ang) * len,
                    0x99000000, 1);
        }
    }

    /**
     * Single-pixel black-and-white noise over the whole bar at stage 4.
     */
    private static void drawStatic(GuiGraphicsExtractor ctx, int barX, int barY, int barWidth, int barHeight, long time) {
        Random rng = new Random((time / 60) * MadnessCorruption.SEED_MIX + 7);
        for (int i = 0; i < 6; i++) {
            int sx = barX + rng.nextInt(barWidth);
            int sy = barY + rng.nextInt(barHeight);
            int col = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
            ctx.fill(sx, sy, sx + 1, sy + 1, (120 << 24) | col);
        }
    }

    /**
     * Freeze / mental pressure / tiredness, centred under the bar. Conditions
     * the server isn't reporting are left out entirely, so the line disappears
     * when there is nothing to say.
     */
    private static void drawExtras(GuiGraphicsExtractor ctx, Font textRenderer, int barX, int barY,
                                   int barWidth, int barHeight,
                                   int freezeStacks, int mentalPressure, double tiredness) {
        StringBuilder extraInfo = new StringBuilder();
        if (freezeStacks > 0) {
            extraInfo.append("❄ Frz: ").append(freezeStacks).append("%  ");
        }
        if (mentalPressure > 0) {
            extraInfo.append("🧠 Prs: ").append(mentalPressure).append("  ");
        }
        if (tiredness > 0.0) {
            extraInfo.append("💤 Trd: ").append(String.format("%.1f%%", tiredness)).append("  ");
        }
        String extraText = extraInfo.toString().trim();
        if (extraText.isEmpty()) return;

        int extraWidth = textRenderer.width(extraText);
        int extraX = barX + (barWidth - extraWidth) / 2;
        int extraY = barY + barHeight + 3;
        ctx.text(textRenderer, extraText, extraX, extraY, 0xFF77AADD, true);
    }

    private static String label(double madness, String statusName, double permMadness) {
        return String.format("Madness: %.1f%% / 100%% (%s, Min: %.1f%%)", madness, statusName, permMadness);
    }

    /**
     * Top-left corner of the bar. {@code madnessYOffset} feeds both anchor
     * families, so the layout editor can drag the bar in either one.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.madnessAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.madnessScale),
                s.madnessXOffset, s.madnessYOffset, s.madnessYOffset);
    }

    /**
     * Madness stage (0-4) for a value, on the 25/50/75/100 thresholds.
     */
    public static int stageOf(double madness) {
        if (madness >= 100.0) return 4;
        if (madness >= 75.0) return 3;
        if (madness >= 50.0) return 2;
        if (madness >= 25.0) return 1;
        return 0;
    }

    /**
     * The bar as it reads when nothing is wrong with it: frame, permanent
     * floor, fill, notches and label — no tremor, glitch slices, cracks,
     * static or gaslighting. Used by the layout editor's preview, which must
     * stay legible and must not touch the smoothing state.
     */
    public static void drawBarAt(GuiGraphicsExtractor ctx, int barX, int barY,
                                 double shownValue, double permanentValue, long time) {
        Font font = Minecraft.getInstance().font;
        int stage = stageOf(shownValue);
        MadnessPalette.Style style = MadnessPalette.of(stage, time);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, style.border());

        int permWidth = CoiBar.lerpWidth(permanentValue, 100.0, BAR_WIDTH);
        if (permWidth > 0) {
            ctx.fill(barX, barY, barX + permWidth, barY + BAR_HEIGHT, (style.top() & 0x00FFFFFF) | (0x60 << 24));
        }

        int fillWidth = CoiBar.lerpWidth(shownValue, 100.0, BAR_WIDTH);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, style.top(), style.bottom());
        CoiBar.shimmer(ctx, barX, barY, BAR_HEIGHT, fillWidth, time, SHIMMER_PERIOD_MS);
        CoiBar.notches(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, 4, 0x50000000);

        if (permWidth > 0 && permWidth <= BAR_WIDTH) {
            ctx.fill(barX + permWidth - 1, barY - 1, barX + permWidth + 1, barY + BAR_HEIGHT + 1, 0xDDFFFFFF);
        }

        CoiBar.label(ctx, font, label(shownValue, style.status(), permanentValue),
                barX, barY, BAR_WIDTH, style.text());
    }
}
