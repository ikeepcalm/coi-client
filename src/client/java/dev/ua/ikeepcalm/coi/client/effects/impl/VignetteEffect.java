package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class VignetteEffect implements VisualEffect {

    public static final String ID = "vignette";

    private float intensity = 0.7f;
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
        return "intensity=0.7,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        float alpha = computeAlpha();
        int maxA = (int) (200 * intensity * alpha);

        int vigW = (int) (w * 0.28f * intensity);
        int vigH = (int) (h * 0.32f * intensity);

        // Top and bottom gradients
        ctx.fillGradient(0, 0, w, vigH, maxA << 24, 0x00000000);
        ctx.fillGradient(0, h - vigH, w, h, 0x00000000, maxA << 24);

        // Left / right — drawn as banded strips (DrawContext has no horizontal gradient)
        int bands = 12;
        int stepW = Math.max(1, vigW / bands);
        for (int i = 0; i < bands; i++) {
            float t = (float) (bands - i) / bands;
            int a = (int) (maxA * t * t);
            int col = a << 24;
            ctx.fill(i * stepW, 0, i * stepW + stepW, h, col);
            ctx.fill(w - i * stepW - stepW, 0, w - i * stepW, h, col);
        }
    }

    /**
     * Fade out in last 500ms if duration is set.
     */
    private float computeAlpha() {
        if (duration < 0) return 1.0f;
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = duration - elapsed;
        if (remaining <= 0) return 0f;
        if (remaining < 500) return remaining / 500f;
        return 1.0f;
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
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
