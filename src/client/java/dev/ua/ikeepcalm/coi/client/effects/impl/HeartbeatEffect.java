package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HeartbeatEffect implements VisualEffect {

    public static final String ID = "heartbeat";

    private float intensity = 0.85f;
    private float bpm = 75f;
    private long duration = -1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Heartbeat";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.85,bpm=75,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        float pulse = computePulse();
        float alpha = computeFade();
        int maxA = (int) (220 * intensity * pulse * alpha);
        if (maxA <= 0) return;

        int vigW = (int) (w * 0.35f);
        int vigH = (int) (h * 0.4f);

        ctx.fillGradient(0, 0, w, vigH, maxA << 24, 0x00000000);
        ctx.fillGradient(0, h - vigH, w, h, 0x00000000, maxA << 24);

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
     * Generates a lub-DUB double-pulse waveform.
     * Returns a value 0-1 representing current vignette intensity.
     */
    private float computePulse() {
        long elapsed = System.currentTimeMillis() - startTime;
        long beatInterval = (long) (60000f / bpm);
        float t = (elapsed % beatInterval) / (float) beatInterval; // 0..1 within one beat

        // lub (small) at t=0.00..0.12, DUB (large) at t=0.15..0.30
        if (t < 0.06f) return t / 0.06f;                                 // lub rise
        else if (t < 0.12f) return 1f - (t - 0.06f) / 0.06f;                 // lub fall
        else if (t < 0.18f) return (t - 0.12f) / 0.06f * 1.4f;               // DUB rise
        else if (t < 0.28f) return Math.max(0f, 1.4f - (t - 0.18f) / 0.10f * 1.4f); // DUB fall
        else return 0f;                                         // silence
    }

    private float computeFade() {
        if (duration < 0) return 1f;
        long remaining = duration - (System.currentTimeMillis() - startTime);
        if (remaining <= 0) return 0f;
        if (remaining < 600) return remaining / 600f;
        return 1f;
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
                case "bpm" -> bpm = Float.parseFloat(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
