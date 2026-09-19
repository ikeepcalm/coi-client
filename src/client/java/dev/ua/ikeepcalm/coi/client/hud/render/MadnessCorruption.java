package dev.ua.ikeepcalm.coi.client.hud.render;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * How madness corrupts what the player sees: the stage vignettes, the VHS
 * tearing over the whole window, and the glyphs that eat into a label.
 * <p>
 * All of it runs off one {@linkplain #bursting cadence} — a short burst, then a
 * calm stretch — which is why the screen tearing, the bar's own slices and its
 * static all come and go together instead of each flickering on its own timer.
 * <p>
 * The screen effects cover the whole window, so a caller draws them
 * <em>outside</em> any per-element scale push.
 */
public class MadnessCorruption {

    private static final String GLITCH_GLYPHS = "#%&@!?/\\";

    /**
     * The corruption cadence: {@value #BURST_LEN_MS}ms of glitching out of
     * every {@value #BURST_CYCLE_MS}.
     */
    private static final long BURST_CYCLE_MS = 650;
    private static final long BURST_LEN_MS = 250;

    /**
     * 64-bit golden ratio, used to spread a coarse timestamp over the whole
     * seed space so consecutive frames get unrelated patterns.
     */
    public static final long SEED_MIX = 0x9E3779B97F4A7C15L;

    private MadnessCorruption() {
    }

    /**
     * Whether this instant falls inside a corruption burst rather than the
     * calm stretch after it.
     */
    public static boolean bursting(long time) {
        return time % BURST_CYCLE_MS < BURST_LEN_MS;
    }

    /**
     * The stage's vignette, and at stage 4 the VHS tearing over it. Nothing at
     * all below stage 2.
     */
    public static void screenEffects(GuiGraphicsExtractor ctx, int w, int h, int stage) {
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

            glitchLines(ctx, w, h, 0.85f * pulse);
        }
    }

    private static void glitchLines(GuiGraphicsExtractor ctx, int w, int h, float intensity) {
        long elapsed = System.currentTimeMillis();
        // Pattern: 250ms burst, 400ms calm
        if (!bursting(elapsed)) return;

        Random rng = new Random((elapsed / 60) * SEED_MIX);
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

    /**
     * Replaces a few characters with glitch glyphs, re-rolled every 90ms so
     * the corruption crawls across the label instead of strobing per frame.
     */
    public static String corruptText(String text, long time) {
        Random rng = new Random((time / 90) * SEED_MIX);
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
