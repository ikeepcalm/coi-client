package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

public class BloodRainEffect implements VisualEffect {

    public static final String ID = "bloodrain";

    private static final int MAX_DROPS = 45;

    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;

    private Drop[] drops;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Blood Rain";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        drops = null;
    }

    @Override
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        if (drops == null) spawnDrops(w, h);

        long elapsed = System.currentTimeMillis() - startTime;

        // Fade in over 800ms, fade out in last 800ms
        float alpha = 1f;
        if (elapsed < 800) alpha = elapsed / 800f;
        else if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < 800) alpha = Math.max(0f, remaining / 800f);
        }

        int dropCount = Math.min(drops.length, (int) (MAX_DROPS * intensity));

        for (int i = 0; i < dropCount; i++) {
            Drop d = drops[i];
            // Wrap-around position: y increases over time, resets at bottom
            long totalH = h + d.length;
            int y = (int) ((d.startY + elapsed * d.speed / 1000.0) % totalH) - d.length;

            int a = (int) (200 * d.alpha * alpha);
            if (a <= 0) continue;

            // Main streak
            ctx.fill(d.x, y, d.x + d.width, y + d.length, (a << 24) | 0x7A0000);
            // Brighter head
            ctx.fill(d.x, y, d.x + d.width, y + 3, ((a * 3 / 2) << 24) | 0xBB0000);
        }
    }

    private void spawnDrops(int w, int h) {
        Random rng = new Random(startTime);
        drops = new Drop[MAX_DROPS];
        for (int i = 0; i < MAX_DROPS; i++) {
            drops[i] = new Drop(
                    2 + rng.nextInt(w - 4),                 // x
                    rng.nextInt(h),                          // startY (random initial phase)
                    15 + rng.nextInt(30),                    // length px
                    1 + rng.nextInt(2),                      // width px
                    50f + rng.nextFloat() * 120f,            // speed px/s
                    0.35f + rng.nextFloat() * 0.65f          // alpha
            );
        }
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    @Override
    public void stop() {
        drops = null;
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

    private record Drop(int x, int startY, int length, int width, float speed, float alpha) {
    }
}
