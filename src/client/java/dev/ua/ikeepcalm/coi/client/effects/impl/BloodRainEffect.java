package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

public class BloodRainEffect implements VisualEffect {

    public static final String ID = "bloodrain";

    private static final int MAX_DROPS = 70;
    private static final int SMEAR_COUNT = 6;

    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;

    private Drop[] drops;
    private Smear[] smears;

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
        smears = null;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (drops == null) spawn(w, h);

        long elapsed = System.currentTimeMillis() - startTime;

        // Fade in over 800ms, fade out in last 800ms
        float alpha = 1f;
        if (elapsed < 800) alpha = elapsed / 800f;
        else if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < 800) alpha = Math.max(0f, remaining / 800f);
        }

        // Ominous red edge tint
        EffectPaint.vignette(ctx, w, h, 0x3F0000, (int) (70 * intensity * alpha), 0.20f, 0.24f);

        // Slow smears running down the "glass"
        for (Smear s : smears) {
            long t = elapsed - s.delay;
            if (t <= 0) continue;
            int len = (int) Math.min(h * s.maxFrac, t * s.growth / 1000f);
            if (len < 4) continue;

            int a = (int) (110 * alpha * intensity);
            ctx.fillGradient(s.x, 0, s.x + s.width, len,
                    EffectPaint.argb(0x4A0000, a),
                    EffectPaint.argb(0x4A0000, a * 2 / 3));
            // Hanging droplet at the tip, slightly wider than the track
            ctx.fill(s.x - 1, len - 3, s.x + s.width + 1, len,
                    EffectPaint.argb(0x9D0F0F, Math.min(255, a * 2)));
        }

        int dropCount = Math.min(drops.length, (int) (MAX_DROPS * intensity));

        for (int i = 0; i < dropCount; i++) {
            Drop d = drops[i];
            // Wrap-around position: y increases over time, resets at bottom
            long totalH = h + d.length;
            int y = (int) ((d.startY + elapsed * d.speed / 1000.0) % totalH) - d.length;

            int a = (int) (210 * d.alpha * alpha);
            if (a <= 0) continue;

            // Tapered streak: transparent tail fading into a saturated falling head
            ctx.fillGradient(d.x, y, d.x + d.width, y + d.length,
                    EffectPaint.argb(d.rgb, a / 6),
                    EffectPaint.argb(d.rgb, a));
            ctx.fill(d.x, y + d.length - 3, d.x + d.width, y + d.length,
                    EffectPaint.argb(0xC01414, Math.min(255, a * 3 / 2)));
        }
    }

    private void spawn(int w, int h) {
        Random rng = new Random(startTime);
        drops = new Drop[MAX_DROPS];
        for (int i = 0; i < MAX_DROPS; i++) {
            // Two depth layers: dim slow background drops, bold fast foreground drops
            boolean background = i % 5 < 2;
            if (background) {
                drops[i] = new Drop(
                        2 + rng.nextInt(w - 4),
                        rng.nextInt(h),
                        12 + rng.nextInt(16),
                        1,
                        45f + rng.nextFloat() * 50f,
                        0.25f + rng.nextFloat() * 0.25f,
                        0x5A0000
                );
            } else {
                drops[i] = new Drop(
                        2 + rng.nextInt(w - 4),
                        rng.nextInt(h),
                        24 + rng.nextInt(32),
                        rng.nextFloat() < 0.12f ? 3 : 2,
                        110f + rng.nextFloat() * 120f,
                        0.55f + rng.nextFloat() * 0.4f,
                        0x8A0000
                );
            }
        }

        smears = new Smear[SMEAR_COUNT];
        for (int i = 0; i < SMEAR_COUNT; i++) {
            smears[i] = new Smear(
                    10 + rng.nextInt(Math.max(1, w - 20)),
                    2 + rng.nextInt(2),
                    i * 900L + rng.nextInt(1200),
                    5f + rng.nextFloat() * 9f,
                    0.30f + rng.nextFloat() * 0.30f
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
        smears = null;
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

    private record Drop(int x, int startY, int length, int width, float speed, float alpha, int rgb) {
    }

    private record Smear(int x, int width, long delay, float growth, float maxFrac) {
    }
}
