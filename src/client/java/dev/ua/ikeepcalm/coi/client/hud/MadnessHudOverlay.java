package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
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

    // Smoothed display value so the bar glides toward the target instead of snapping
    private static double shownMadness = -1;
    private static long lastFrameMs = 0;

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MADNESS_LAYER, MadnessHudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.gui.hud.isHidden() || !settings.enabled || !settings.showMadnessBar) {
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

        // 2. Render Screen-wide Effects based on Stage
        renderScreenEffects(ctx, w, h, stage);

        // 3. Render the Madness Bar
        renderMadnessBar(ctx, client, w, h, settings, madness, permMadness, freezeStacks, mentalPressure, tiredness, stage);
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

    private static void renderMadnessBar(GuiGraphicsExtractor ctx, Minecraft client, int w, int h, HudConfig.HudSettings settings,
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
        int barWidth = 182;
        int barHeight = 6;
        int barX;
        int barY;

        String anchor = settings.madnessAnchor != null ? settings.madnessAnchor.toUpperCase() : "TOP_LEFT";
        switch (anchor) {
            case "TOP_LEFT" -> {
                barX = 10;
                barY = 20;
            }
            case "TOP_CENTER" -> {
                barX = (w - barWidth) / 2;
                barY = 20;
            }
            case "BOTTOM_LEFT" -> {
                barX = 10;
                barY = h - settings.madnessYOffset;
            }
            default -> { // BOTTOM_CENTER
                barX = (w - barWidth) / 2;
                barY = h - settings.madnessYOffset;
            }
        }
        barX = Mth.clamp(barX + settings.madnessXOffset, 0, Math.max(0, w - barWidth));

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

        // Determine colors based on stage and animations
        int mainColorTop;
        int mainColorBottom;
        int textColor;
        int borderColor;
        String statusName;

        switch (stage) {
            case 1 -> {
                // Stage 1: Warning (Yellow/Orange) - gentle pulse
                float pulse = (float) Math.sin(time * 0.003) * 0.15f + 0.85f;
                int r = (int) (255 * pulse);
                int g = (int) (170 * pulse);
                mainColorTop = (255 << 24) | (r << 16) | (g << 8);
                mainColorBottom = (255 << 24) | ((int) (204 * pulse) << 16) | ((int) (119 * pulse) << 8);
                textColor = 0xFFFFAA00;
                borderColor = 0xCC000000;
                statusName = "Sane";
            }
            case 2 -> {
                // Stage 2: Partial Loss (Red) - static dark red
                mainColorTop = 0xFFDD2222;
                mainColorBottom = 0xFF991111;
                textColor = 0xFFDD2222;
                borderColor = 0xCC1A0000;
                statusName = "Unstable";
            }
            case 3 -> {
                // Stage 3: Critical (Deep Crimson) - fast pulse
                float pulse = (float) Math.sin(time * 0.01) * 0.2f + 0.8f;
                int r = (int) (255 * pulse);
                mainColorTop = (255 << 24) | (r << 16);
                mainColorBottom = (255 << 24) | ((int) (139 * pulse) << 16);
                textColor = 0xFFFF0055;
                borderColor = 0xCC330000;
                statusName = "Unhinged";
            }
            case 4 -> {
                // MAX: Rampager (Dark Purple/Black)
                float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
                int r = (int) (153 * pulse);
                int b = (int) (153 * pulse);
                mainColorTop = (255 << 24) | (r << 16) | (51 << 8) | b;
                mainColorBottom = (255 << 24) | ((int) (58 * pulse) << 16) | ((int) (58 * pulse));
                textColor = 0xFF993399;
                borderColor = 0xCC1A0520;
                statusName = "Gone Mad";
            }
            default -> {
                // Stage 0: Stable (Green/Cyan)
                mainColorTop = 0xFF00FFCC;
                mainColorBottom = 0xFF00AA88;
                textColor = 0xFF00FFCC;
                borderColor = 0xCC000000;
                statusName = "Stable";
            }
        }

        // 1. Border + background with a subtle depth gradient
        ctx.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, borderColor);
        ctx.fillGradient(barX, barY, barX + barWidth, barY + barHeight, 0xFF1B1B1E, 0xFF0F0F11);

        // 2. permanentMadness region (Min Floor)
        int permWidth = (int) (barWidth * (permMadness / 100.0));
        permWidth = Mth.clamp(permWidth, 0, barWidth);
        if (permWidth > 0) {
            // 50% opacity of top color
            int permColor = (mainColorTop & 0x00FFFFFF) | (0x60 << 24);
            ctx.fill(barX, barY, barX + permWidth, barY + barHeight, permColor);
        }

        // 3. Primary filled region for current (smoothed) madness
        int madnessWidth = (int) (barWidth * (shownMadness / 100.0));
        madnessWidth = Mth.clamp(madnessWidth, 0, barWidth);

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

        if (madnessWidth > 0) {
            ctx.fillGradient(barX, barY, barX + madnessWidth, barY + barHeight, mainColorTop, mainColorBottom);
            // Bevel: top highlight, bottom shade
            ctx.fill(barX, barY, barX + madnessWidth, barY + 1, 0x40FFFFFF);
            ctx.fill(barX, barY + barHeight - 1, barX + madnessWidth, barY + barHeight, 0x40000000);
        }

        // 4. Calm stages get a slow shimmer sweeping across the fill
        if (stage <= 1 && madnessWidth > 8) {
            float sweep = (time % 3000) / 3000f;
            int bandX = barX - 16 + (int) ((madnessWidth + 32) * sweep);
            int[] offs = {0, 4, 8};
            int[] alphas = {30, 70, 30};
            for (int i = 0; i < offs.length; i++) {
                int x0 = Math.max(barX, bandX + offs[i]);
                int x1 = Math.min(barX + madnessWidth, bandX + offs[i] + 4);
                if (x1 > x0) {
                    ctx.fill(x0, barY + 1, x1, barY + barHeight - 1, (alphas[i] << 24) | 0xFFFFFF);
                }
            }
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
        for (int q = 1; q <= 3; q++) {
            int nx = barX + barWidth * q / 4;
            ctx.fill(nx, barY, nx + 1, barY + barHeight, 0x50000000);
        }

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
        int textWidth = textRenderer.width(text);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY - 10;
        ctx.text(textRenderer, text, textX, textY, textColor, true);

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
