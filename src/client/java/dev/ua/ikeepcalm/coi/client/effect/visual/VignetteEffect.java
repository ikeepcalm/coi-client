package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Darkened screen edges with a smooth falloff and a slow breathing pulse, so a
 * long-lived vignette never sits perfectly still.
 * <p>
 * Params: {@code intensity} (0-1, darkness and width), {@code color} (hex RGB,
 * no {@code #}), {@code duration} (ms, {@code -1} = until stopped).
 */
public class VignetteEffect implements VisualEffect {

    public static final String ID = "vignette";

    private static final float FADE_IN_MS = 350f;
    private static final float FADE_OUT_MS = 500f;
    /**
     * Angular speed of the breathing pulse, in radians per millisecond.
     */
    private static final double BREATH_RATE = 0.0011;
    private static final int MAX_ALPHA = 215;

    private float intensity = 0.7f;
    private int rgb = 0x000000;
    private long duration = -1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Vignette";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,color=000000,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;
        float alpha = computeAlpha(elapsed);
        if (alpha <= 0f) return;

        // Slow breathing so a long-lived vignette feels alive instead of static
        float breath = 0.93f + 0.07f * (float) Math.sin(elapsed * BREATH_RATE);
        float spreadPulse = 0.97f + 0.03f * (float) Math.sin(elapsed * BREATH_RATE);

        int maxA = (int) (MAX_ALPHA * intensity * alpha * breath);
        EffectPaint.vignette(ctx, w, h, rgb, maxA,
                0.32f * intensity * spreadPulse,
                0.36f * intensity * spreadPulse);
    }

    /**
     * Fade in over the first 350ms; fade out in the last 500ms if duration is set.
     */
    private float computeAlpha(long elapsed) {
        float alpha = Math.min(1f, elapsed / FADE_IN_MS);
        if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining <= 0) return 0f;
            if (remaining < FADE_OUT_MS) alpha = Math.min(alpha, remaining / FADE_OUT_MS);
        }
        return alpha;
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "color" -> rgb = Integer.parseInt(value, 16);
                case "duration" -> duration = Long.parseLong(value);
            }
        });
    }
}
