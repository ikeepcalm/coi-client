package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Random;

public class MadnessHudOverlay {

    private static final Identifier MADNESS_LAYER = Identifier.fromNamespaceAndPath("coi-client", "madness");

    private static final String GLITCH_GLYPHS = "#%&@!?/\\";

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

    // Smoothed display value so the bar glides toward the target instead of snapping
    private static double shownMadness = -1;
    private static long lastFrameMs = 0;

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MADNESS_LAYER, MadnessHudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.gui.hud.isHidden() || HudLayout.editing()
                || !settings.enabled || !settings.showMadnessBar) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        double madness = ClientBeyonderState.getMadness();
        double permMadness = ClientBeyonderState.getPermanentMadness();
        int freezeStacks = ClientBeyonderState.getFreezeStacks();
        int mentalPressure = ClientBeyonderState.getMentalPressure();
        double tiredness = ClientBeyonderState.getTiredness();

        // 1. Determine Madness Stage
        int stage = 0;
        if (madness >= 100.0) stage = 4;
        else if (madness >= 75.0) stage = 3;
        else if (madness >= 50.0) stage = 2;
        else if (madness >= 25.0) stage = 1;

        // 2. Screen-wide effects cover the whole window, so they stay
        // outside the bar's scale push
        renderScreenEffects(ctx, w, h, stage);

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

    private static void renderScreenEffects(GuiGraphicsExtractor ctx, int w, int h, int stage) {
        if (stage < 2) return; // No screen effects for stages 0 and 1

        long time = System.currentTimeMillis();

        if (stage == 2) {
            // Stage 2: Faint red-black vignette
            EffectPaint.vignette(ctx, w, h, 0x1A0000, 70, 0.13f, 0.15f);
        } else if (stage == 3) {
            // Stage 3: Blood-red vignette, pulsating
            float pulse = (float) Math.sin(time * 0.008) * 0.25f + 0.75f;
            EffectPaint.vignette(ctx, w, h, 0x660000, (int) (185 * pulse), 0.22f, 0.25f);
        } else {
            // MAX (Stage 4 / Rampager): heavy dark purple vignette + VHS glitches
            float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
            EffectPaint.vignette(ctx, w, h, 0x2A082E, (int) (235 * pulse), 0.28f, 0.31f);

            renderGlitches(ctx, w, h, 0.85f * pulse);
        }
    }

    private static void renderGlitches(GuiGraphicsExtractor ctx, int w, int h, float intensity) {
        long elapsed = System.currentTimeMillis();
        // Pattern: 250ms burst, 400ms calm
        long cycleLen = 650;
        long burstLen = 250;
        long phasePos = elapsed % cycleLen;
        if (phasePos >= burstLen) return;

        Random rng = new Random((elapsed / 60) * 0x9E3779B97F4A7C15L);
        int lineCount = 3 + (int) (intensity * 4);
        float alpha = 0.6f + 0.4f * intensity;

        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt(3);
            int type = rng.nextInt(4);

            switch (type) {
                case 0 -> {
                    int a = (int) (90 * alpha);
                    ctx.fill(0, y, w, y + bh, a << 24);
                }
                case 1 -> {
                    int a = (int) (40 * alpha);
                    ctx.fill(0, y, w, y + bh, (a << 24) | 0xFFFFFF);
                }
                case 2 -> {
                    int ra = (int) (50 * alpha);
                    ctx.fill(0, y, w, y + 1, (ra << 24) | 0x8A0E8E);
                }
                case 3 -> {
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (35 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0x222222);
                }
            }
        }
    }

    private static void renderMadnessBar(GuiGraphicsExtractor ctx, Minecraft client, int[] pos,
                                         double madness, double permMadness, int freezeStacks, int mentalPressure, double tiredness, int stage) {
        Font textRenderer = client.font;
        long time = System.currentTimeMillis();

        // Smooth the displayed value toward the real one
        if (shownMadness < 0) shownMadness = madness;
        float dt = lastFrameMs == 0 ? 0.016f : Math.min(0.1f, (time - lastFrameMs) / 1000f);
        lastFrameMs = time;
        shownMadness += (madness - shownMadness) * Math.min(1f, dt * 10f);
        if (Math.abs(shownMadness - madness) < 0.05) shownMadness = madness;

        // Position coordinates
        int barWidth = BAR_WIDTH;
        int barHeight = BAR_HEIGHT;

        int barX = pos[0];
        int barY = pos[1];

        // The whole bar trembles at high madness
        if (stage >= 3) {
            Random tremor = new Random((time / 40) * 0x9E3779B97F4A7C15L);
            int amp = stage == 4 ? 2 : 1;
            barX += tremor.nextInt(amp * 2 + 1) - amp;
            barY += tremor.nextInt(amp * 2 + 1) - amp;
        }

        // Apply screen shake offset on madness increase
        float flash = ClientBeyonderState.getFlashIntensity();
        if (flash > 0) {
            Random rand = new Random();
            barX += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
            barY += (int) ((rand.nextFloat() * 2 - 1) * 3 * flash);
        }

        Style style = style(stage, time);
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
        int madnessWidth = CoiBar.lerpWidth(shownMadness, 100.0, barWidth);

        // Unstable minds can't hold a steady edge
        if (stage >= 2 && madnessWidth > 2 && madnessWidth < barWidth - 2) {
            madnessWidth += (int) Math.round(Math.sin(time * 0.02) * (stage >= 3 ? 1.5 : 1.0));
            madnessWidth = Mth.clamp(madnessWidth, 0, barWidth);
        }

        boolean burst = (time % 650) < 250; // shared cadence with the glitch overlay

        // Stage 4: RGB-split ghost copies bleeding out from under the fill
        if (stage == 4 && madnessWidth > 0 && burst) {
            ctx.fill(barX - 1, barY, barX - 1 + madnessWidth, barY + barHeight, 0x50FF2299);
            ctx.fill(barX + 1, barY, barX + 1 + Math.min(madnessWidth, barWidth - 1), barY + barHeight, 0x5033EEFF);
        }

        CoiBar.fill(ctx, barX, barY, barHeight, madnessWidth, mainColorTop, mainColorBottom);

        // 4. Calm stages get a slow shimmer sweeping across the fill
        if (stage <= 1) {
            CoiBar.shimmer(ctx, barX, barY, barHeight, madnessWidth, time, 3000);
        }

        // 5. Glitch slices tear slivers out of the bar at high madness
        if (stage >= 3 && madnessWidth > 0 && burst) {
            Random rng = new Random((time / 70) * 31L);
            int slices = stage == 4 ? 2 : 1;
            for (int i = 0; i < slices; i++) {
                int sy = barY + rng.nextInt(barHeight - 1);
                int sh = 1 + rng.nextInt(2);
                int off = (rng.nextBoolean() ? -1 : 1) * (1 + rng.nextInt(stage == 4 ? 3 : 2));
                ctx.fill(barX, sy, barX + barWidth, sy + sh, 0xFF0F0F11);
                ctx.fill(barX + off, sy, barX + off + madnessWidth, sy + sh, mainColorTop);
            }
        }

        // 6. Hairline cracks spread across the damaged bar (deterministic pattern)
        if (stage >= 2) {
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

        // 7. Static noise specks over the bar when fully gone
        if (stage == 4 && burst) {
            Random rng = new Random((time / 60) * 0x9E3779B97F4A7C15L + 7);
            for (int i = 0; i < 6; i++) {
                int sx = barX + rng.nextInt(barWidth);
                int sy = barY + rng.nextInt(barHeight);
                int col = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
                ctx.fill(sx, sy, sx + 1, sy + 1, (120 << 24) | col);
            }
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
        String text = String.format("Madness: %.1f%% / 100%% (%s, Min: %.1f%%)", madness, statusName, permMadness);
        if (stage == 4 && burst) {
            text = corruptText(text, time);
        }
        CoiBar.label(ctx, textRenderer, text, barX, barY, barWidth, textColor);

        // 12. Render other conditions (Freeze, Mental Pressure, Tiredness) below the bar
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
        if (!extraText.isEmpty()) {
            int extraWidth = textRenderer.width(extraText);
            int extraX = barX + (barWidth - extraWidth) / 2;
            int extraY = barY + barHeight + 3;
            ctx.text(textRenderer, extraText, extraX, extraY, 0xFF77AADD, true);
        }
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
     * The bar's palette for a stage, with the pulses that animate it.
     */
    private record Style(int top, int bottom, int text, int border, String status) {
    }

    private static Style style(int stage, long time) {
        switch (stage) {
            case 1 -> {
                // Stage 1: Warning (Yellow/Orange) - gentle pulse
                float pulse = (float) Math.sin(time * 0.003) * 0.15f + 0.85f;
                int r = (int) (255 * pulse);
                int g = (int) (170 * pulse);
                return new Style((255 << 24) | (r << 16) | (g << 8),
                        (255 << 24) | ((int) (204 * pulse) << 16) | ((int) (119 * pulse) << 8),
                        0xFFFFAA00, 0xCC000000, "Sane");
            }
            case 2 -> {
                // Stage 2: Partial Loss (Red) - static dark red
                return new Style(0xFFDD2222, 0xFF991111, 0xFFDD2222, 0xCC1A0000, "Unstable");
            }
            case 3 -> {
                // Stage 3: Critical (Deep Crimson) - fast pulse
                float pulse = (float) Math.sin(time * 0.01) * 0.2f + 0.8f;
                return new Style((255 << 24) | ((int) (255 * pulse) << 16),
                        (255 << 24) | ((int) (139 * pulse) << 16),
                        0xFFFF0055, 0xCC330000, "Unhinged");
            }
            case 4 -> {
                // MAX: Rampager (Dark Purple/Black)
                float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
                int r = (int) (153 * pulse);
                int b = (int) (153 * pulse);
                return new Style((255 << 24) | (r << 16) | (51 << 8) | b,
                        (255 << 24) | ((int) (58 * pulse) << 16) | ((int) (58 * pulse)),
                        0xFF993399, 0xCC1A0520, "Gone Mad");
            }
            default -> {
                // Stage 0: Stable (Green/Cyan)
                return new Style(0xFF00FFCC, 0xFF00AA88, 0xFF00FFCC, 0xCC000000, "Stable");
            }
        }
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
        Style style = style(stage, time);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, style.border());

        int permWidth = CoiBar.lerpWidth(permanentValue, 100.0, BAR_WIDTH);
        if (permWidth > 0) {
            ctx.fill(barX, barY, barX + permWidth, barY + BAR_HEIGHT, (style.top() & 0x00FFFFFF) | (0x60 << 24));
        }

        int fillWidth = CoiBar.lerpWidth(shownValue, 100.0, BAR_WIDTH);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, style.top(), style.bottom());
        CoiBar.shimmer(ctx, barX, barY, BAR_HEIGHT, fillWidth, time, 3000);
        CoiBar.notches(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, 4, 0x50000000);

        if (permWidth > 0 && permWidth <= BAR_WIDTH) {
            ctx.fill(barX + permWidth - 1, barY - 1, barX + permWidth + 1, barY + BAR_HEIGHT + 1, 0xDDFFFFFF);
        }

        String text = String.format("Madness: %.1f%% / 100%% (%s, Min: %.1f%%)",
                shownValue, style.status(), permanentValue);
        CoiBar.label(ctx, font, text, barX, barY, BAR_WIDTH, style.text());
    }

    /**
     * Replaces a few characters with glitch glyphs, re-rolled every 90ms so
     * the corruption crawls across the label instead of strobing per frame.
     */
    private static String corruptText(String text, long time) {
        Random rng = new Random((time / 90) * 0x9E3779B97F4A7C15L);
        char[] chars = text.toCharArray();
        int hits = 1 + rng.nextInt(3);
        for (int i = 0; i < hits; i++) {
            int idx = rng.nextInt(chars.length);
            if (chars[idx] != ' ') {
                chars[idx] = GLITCH_GLYPHS.charAt(rng.nextInt(GLITCH_GLYPHS.length()));
            }
        }
        return new String(chars);
    }
}
