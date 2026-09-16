package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HeartbeatEffect implements VisualEffect {

    public static final String ID = "heartbeat";

    private float intensity = 0.85f;
    private float bpm = 75f;
    private int rgb = 0x8A0000;
    private long duration = -1;
    private long startTime;

    /**
     * Raised-cosine bump centered at {@code center} with half-width {@code width}.
     */
    private static float bump(float t, float center, float width) {
        float d = Math.abs(t - center);
        if (d >= width) return 0f;
        float c = (float) Math.cos(d / width * Math.PI / 2);
        return c * c;
    }

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
        return "intensity=0.85,bpm=75,color=8A0000,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        float pulse = computePulse();
        float fade = computeFade();
        if (fade <= 0f) return;

        // Constant dread between beats, surging dark edge on each pulse
        int darkA = (int) ((55 + 175 * pulse) * intensity * fade);
        EffectPaint.vignette(ctx, w, h, 0x000000, darkA,
                0.24f + 0.07f * pulse,
                0.28f + 0.08f * pulse);

        // Blood-colored inner glow riding the pulse, tighter than the dark edge
        int redA = (int) (95 * pulse * intensity * fade);
        EffectPaint.vignette(ctx, w, h, rgb, redA, 0.17f, 0.20f);

        // Faint whole-screen flush at the DUB peak
        if (pulse > 0.85f) {
            int flushA = (int) ((pulse - 0.85f) / 0.15f * 26 * intensity * fade);
            ctx.fill(0, 0, w, h, EffectPaint.argb(rgb, flushA));
        }
    }

    /**
     * Smooth lub-DUB double-pulse waveform: two raised-cosine bumps per beat.
     * Returns 0-1 representing current pulse strength.
     */
    private float computePulse() {
        long elapsed = System.currentTimeMillis() - startTime;
        long beatInterval = (long) (60000f / bpm);
        float t = (elapsed % beatInterval) / (float) beatInterval; // 0..1 within one beat

        float lub = 0.55f * bump(t, 0.08f, 0.07f);
        float dub = bump(t, 0.26f, 0.11f);
        return Math.min(1f, lub + dub);
    }

    private float computeFade() {
        long elapsed = System.currentTimeMillis() - startTime;
        float fade = Math.min(1f, elapsed / 400f);
        if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining <= 0) return 0f;
            if (remaining < 600) fade = Math.min(fade, remaining / 600f);
        }
        return fade;
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
                case "color" -> rgb = Integer.parseInt(kv[1].trim(), 16);
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
