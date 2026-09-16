package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class VignetteEffect implements VisualEffect {

    public static final String ID = "vignette";

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
        float breath = 0.93f + 0.07f * (float) Math.sin(elapsed * 0.0011);
        float spreadPulse = 0.97f + 0.03f * (float) Math.sin(elapsed * 0.0011);

        int maxA = (int) (215 * intensity * alpha * breath);
        EffectPaint.vignette(ctx, w, h, rgb, maxA,
                0.32f * intensity * spreadPulse,
                0.36f * intensity * spreadPulse);
    }

    /**
     * Fade in over the first 350ms; fade out in the last 500ms if duration is set.
     */
    private float computeAlpha(long elapsed) {
        float alpha = Math.min(1f, elapsed / 350f);
        if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining <= 0) return 0f;
            if (remaining < 500) alpha = Math.min(alpha, remaining / 500f);
        }
        return alpha;
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    private void parseParams(String params) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "color" -> rgb = Integer.parseInt(kv[1].trim(), 16);
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
