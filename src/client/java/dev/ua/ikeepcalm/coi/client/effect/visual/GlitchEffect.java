package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * VHS-style corruption: short violent bursts of torn bands, chromatic fringes,
 * corrupted blocks and static specks, separated by uneasy calm, with a tracking
 * bar rolling down the screen throughout.
 * <p>
 * Params: {@code intensity} (0-1, artifact count, alpha and burst frequency),
 * {@code duration} (ms, {@code -1} = until stopped).
 */
public class GlitchEffect implements VisualEffect {

    public static final String ID = "glitch";

    /**
     * Artifacts are reseeded on this interval, so they flicker ~12x/s instead of every frame.
     */
    private static final long ARTIFACT_TICK_MS = 80;
    /**
     * Golden-ratio odd constant, mixed into the seed so consecutive ticks look unrelated.
     */
    private static final long SEED_MIX = 0x9E3779B97F4A7C15L;
    /**
     * Period of one full top-to-bottom sweep of the tracking bar.
     */
    private static final long TRACKING_ROLL_MS = 4200;

    private static final int RED_FRINGE_RGB = 0xFF2200;
    private static final int CYAN_FRINGE_RGB = 0x00EEFF;

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
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        // Burst/calm phase: short violent bursts separated by uneasy calm
        long cycleLen = (long) (700 + 200 / intensity);
        long burstLen = (long) (250 + 100 * intensity);
        long phasePos = elapsed % cycleLen;
        boolean inBurst = phasePos < burstLen;

        // VHS tracking bar rolls down continuously, brighter during bursts
        renderTrackingBar(ctx, w, h, elapsed, inBurst);

        if (!inBurst) return;

        // Seed changes every 80ms so artifacts flicker ~12×/s, not 60×/s
        long tick = elapsed / ARTIFACT_TICK_MS;
        Random rng = new Random(tick * SEED_MIX + startTime);

        float alpha = 0.55f + 0.45f * intensity;

        renderScanlines(ctx, w, h, tick, alpha);
        renderBands(ctx, w, h, rng, alpha);
        renderCorruptedBlocks(ctx, w, h, rng, alpha);
        renderStaticSpecks(ctx, w, h, rng, alpha);

        // Occasional full-screen bright flash at very high intensity
        if (intensity > 0.85f && rng.nextFloat() < 0.15f) {
            ctx.fill(0, 0, w, h, 0x0CFFFFFF);
        }
    }

    /**
     * Faint scanlines over the whole frame during a burst.
     */
    private void renderScanlines(GuiGraphicsExtractor ctx, int w, int h, long tick, float alpha) {
        int scanA = (int) (14 * alpha);
        for (int y = (int) (tick % 3); y < h; y += 3) {
            ctx.fill(0, y, w, y + 1, scanA << 24);
        }
    }

    /**
     * Horizontal artifact bands, randomly full-width or torn (partial with an
     * offset start), in one of four flavours.
     */
    private void renderBands(GuiGraphicsExtractor ctx, int w, int h, Random rng, float alpha) {
        int lineCount = (int) (3 + intensity * 5);
        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt((int) Math.max(1, 4 * intensity));
            int x0 = rng.nextFloat() < 0.4f ? rng.nextInt(w / 2) : 0;
            int x1 = x0 == 0 ? w : Math.min(w, x0 + w / 3 + rng.nextInt(w / 2));
            int type = rng.nextInt(4);

            switch (type) {
                case 0 -> {
                    // Dark scan band
                    int a = (int) (120 * alpha);
                    ctx.fill(x0, y, x1, y + bh, a << 24);
                }
                case 1 -> {
                    // White flash band
                    int a = (int) (60 * alpha);
                    ctx.fill(x0, y, x1, y + bh, (a << 24) | 0xFFFFFF);
                }
                case 2 -> {
                    // Red chromatic fringe above + cyan below (RGB split simulation)
                    int ra = (int) (55 * alpha);
                    int ca = (int) (45 * alpha);
                    ctx.fill(x0, y, x1, y + 1, (ra << 24) | RED_FRINGE_RGB);
                    ctx.fill(x0, y + bh, x1, y + bh + 1, (ca << 24) | CYAN_FRINGE_RGB);
                }
                case 3 -> {
                    // Horizontal offset block — a lighter band on one side of the screen
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (45 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0xCCCCCC);
                }
            }
        }
    }

    /**
     * Corrupted blocks with RGB split — the signature "digital damage" look.
     */
    private void renderCorruptedBlocks(GuiGraphicsExtractor ctx, int w, int h, Random rng, float alpha) {
        int blockCount = 1 + rng.nextInt(2 + (int) (2 * intensity));
        for (int i = 0; i < blockCount; i++) {
            int bw = w / 8 + rng.nextInt(w / 5);
            int bhgt = 4 + rng.nextInt(12);
            int bx = rng.nextInt(Math.max(1, w - bw));
            int by = rng.nextInt(Math.max(1, h - bhgt));
            int a = (int) (55 * alpha);
            ctx.fill(bx - 2, by, bx + bw - 2, by + bhgt, (a << 24) | RED_FRINGE_RGB);
            ctx.fill(bx + 2, by, bx + bw + 2, by + bhgt, (a << 24) | CYAN_FRINGE_RGB);
            ctx.fill(bx, by, bx + bw, by + bhgt, ((int) (65 * alpha) << 24) | 0xBBBBBB);
        }
    }

    private void renderStaticSpecks(GuiGraphicsExtractor ctx, int w, int h, Random rng, float alpha) {
        int speckCount = (int) (35 * intensity);
        for (int i = 0; i < speckCount; i++) {
            int sx = rng.nextInt(w);
            int sy = rng.nextInt(h);
            int size = 1 + rng.nextInt(2);
            int col = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
            ctx.fill(sx, sy, sx + size, sy + size, ((int) (90 * alpha) << 24) | col);
        }
    }

    private void renderTrackingBar(GuiGraphicsExtractor ctx, int w, int h, long elapsed, boolean inBurst) {
        float roll = (elapsed % TRACKING_ROLL_MS) / (float) TRACKING_ROLL_MS;
        int barH = Math.max(8, h / 14);
        int barY = (int) (roll * (h + barH * 2)) - barH;

        int barA = (int) ((inBurst ? 28 : 14) * (0.5f + 0.5f * intensity));
        int mid = barY + barH / 2;
        ctx.fillGradient(0, barY, w, mid,
                EffectPaint.argb(0xFFFFFF, 0), EffectPaint.argb(0xFFFFFF, barA));
        ctx.fillGradient(0, mid, w, barY + barH,
                EffectPaint.argb(0xFFFFFF, barA), EffectPaint.argb(0xFFFFFF, 0));
        // Chromatic tear on the bar's trailing edge
        ctx.fill(0, barY + barH, w, barY + barH + 1, EffectPaint.argb(CYAN_FRINGE_RGB, barA + 25));
        ctx.fill(0, barY - 1, w, barY, EffectPaint.argb(RED_FRINGE_RGB, barA + 15));
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "duration" -> duration = Long.parseLong(value);
            }
        });
    }
}
