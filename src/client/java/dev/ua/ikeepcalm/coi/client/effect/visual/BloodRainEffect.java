package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tapered red streaks falling in two depth layers — dim slow background, bold
 * fast foreground — while slow smears run down the "glass" with hanging
 * droplets, all inside a red edge tint.
 * <p>
 * Params: {@code intensity} (0-1, number and density of drops),
 * {@code duration} (ms, {@code -1} = until stopped).
 */
public class BloodRainEffect implements VisualEffect {

    public static final String ID = "bloodrain";

    private static final int MAX_DROPS = 70;
    private static final int SMEAR_COUNT = 6;
    private static final float FADE_MS = 800f;
    private static final int SMEAR_RGB = 0x4A0000;
    private static final int DROPLET_RGB = 0x9D0F0F;
    private static final int DROP_HEAD_RGB = 0xC01414;

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
        float alpha = computeAlpha(elapsed);

        // Ominous red edge tint
        EffectPaint.vignette(ctx, w, h, 0x3F0000, (int) (70 * intensity * alpha), 0.20f, 0.24f);

        drawSmears(ctx, h, elapsed, alpha);
        drawDrops(ctx, h, elapsed, alpha);
    }

    /**
     * Fade in over 800ms, fade out in last 800ms.
     */
    private float computeAlpha(long elapsed) {
        if (elapsed < FADE_MS) return elapsed / FADE_MS;
        if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < FADE_MS) return Math.max(0f, remaining / FADE_MS);
        }
        return 1f;
    }

    /**
     * Slow smears running down the "glass", each with a hanging droplet at the tip.
     */
    private void drawSmears(GuiGraphicsExtractor ctx, int h, long elapsed, float alpha) {
        for (Smear s : smears) {
            long t = elapsed - s.delay;
            if (t <= 0) continue;
            int len = (int) Math.min(h * s.maxFrac, t * s.growth / 1000f);
            if (len < 4) continue;

            int a = (int) (110 * alpha * intensity);
            ctx.fillGradient(s.x, 0, s.x + s.width, len,
                    EffectPaint.argb(SMEAR_RGB, a),
                    EffectPaint.argb(SMEAR_RGB, a * 2 / 3));
            // Hanging droplet at the tip, slightly wider than the track
            ctx.fill(s.x - 1, len - 3, s.x + s.width + 1, len,
                    EffectPaint.argb(DROPLET_RGB, Math.min(255, a * 2)));
        }
    }

    /**
     * Tapered streaks: a transparent tail fading into a saturated falling head,
     * wrapping around from the bottom of the screen back to the top.
     */
    private void drawDrops(GuiGraphicsExtractor ctx, int h, long elapsed, float alpha) {
        int dropCount = Math.min(drops.length, (int) (MAX_DROPS * intensity));

        for (int i = 0; i < dropCount; i++) {
            Drop d = drops[i];
            // Wrap-around position: y increases over time, resets at bottom
            long totalH = h + d.length;
            int y = (int) ((d.startY + elapsed * d.speed / 1000.0) % totalH) - d.length;

            int a = (int) (210 * d.alpha * alpha);
            if (a <= 0) continue;

            ctx.fillGradient(d.x, y, d.x + d.width, y + d.length,
                    EffectPaint.argb(d.rgb, a / 6),
                    EffectPaint.argb(d.rgb, a));
            ctx.fill(d.x, y + d.length - 3, d.x + d.width, y + d.length,
                    EffectPaint.argb(DROP_HEAD_RGB, Math.min(255, a * 3 / 2)));
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
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "duration" -> duration = Long.parseLong(value);
            }
        });
    }

    private record Drop(int x, int startY, int length, int width, float speed, float alpha, int rgb) {
    }

    private record Smear(int x, int width, long delay, float growth, float maxFrac) {
    }
}
