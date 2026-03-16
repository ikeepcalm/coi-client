package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

public class GlitchEffect implements VisualEffect {

    public static final String ID = "glitch";

    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "VHS Glitch";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=3000";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        // Determine burst/calm phase.
        // Pattern: 300ms burst, then calm for 400-900ms, repeating.
        // Use a deterministic cycle driven by elapsed time.
        long cycleLen = (long) (700 + 200 / intensity);  // shorter cycle at high intensity
        long burstLen = (long) (250 + 100 * intensity);
        long phasePos = elapsed % cycleLen;
        boolean inBurst = phasePos < burstLen;

        if (!inBurst) return;

        // Seed changes every 80ms so lines flicker ~12×/s, not 60×/s
        long tick = elapsed / 80;
        Random rng = new Random(tick * 0x9E3779B97F4A7C15L + startTime);

        int lineCount = (int) (3 + intensity * 5);   // 3-8 lines
        float alpha = 0.55f + 0.45f * intensity;

        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt((int) Math.max(1, 4 * intensity));  // 1-4px tall
            int type = rng.nextInt(4);

            switch (type) {
                case 0 -> {
                    // Dark scan band
                    int a = (int) (120 * alpha);
                    ctx.fill(0, y, w, y + bh, a << 24);
                }
                case 1 -> {
                    // White flash band
                    int a = (int) (60 * alpha);
                    ctx.fill(0, y, w, y + bh, (a << 24) | 0xFFFFFF);
                }
                case 2 -> {
                    // Red chromatic fringe above + cyan below (RGB split simulation)
                    int ra = (int) (50 * alpha);
                    int ca = (int) (40 * alpha);
                    ctx.fill(0, y, w, y + 1, (ra << 24) | 0xFF2200);
                    ctx.fill(0, y + bh, w, y + bh + 1, (ca << 24) | 0x00EEFF);
                }
                case 3 -> {
                    // Horizontal offset block — a lighter band on one half of the screen
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (45 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0xCCCCCC);
                }
            }
        }

        // Occasional full-width bright flash at very high intensity
        if (intensity > 0.85f && rng.nextFloat() < 0.15f) {
            ctx.fill(0, 0, w, h, 0x0CFFFFFF);
        }
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
