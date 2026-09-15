package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Fern-like ice crystals growing inward from the screen edges — thick bright
 * trunks with side needles — over a layered icy blue-white haze, with
 * twinkling glints appearing along the crystals once they have grown.
 * <p>
 * Params: {@code intensity} (0-1, reach and brightness), {@code duration} (ms,
 * {@code -1} = until stopped).
 */
public class FrostEffect implements VisualEffect {

    public static final String ID = "frost";

    private static final int MAX_SEGMENTS = 110;
    private static final long GROW_MS = 2200;

    // Segment kinds — index into stroke style
    private static final int KIND_MAIN = 0;
    private static final int KIND_BRANCH = 1;
    private static final int KIND_NEEDLE = 2;

    /** Indices into a segment's {@code {x1, y1, x2, y2, kind}} array. */
    private static final int X1 = 0;
    private static final int Y1 = 1;
    private static final int X2 = 2;
    private static final int Y2 = 3;
    private static final int KIND = 4;

    /** Indices into a sparkle's {@code {x, y, phase, segIndex}} array. */
    private static final int SP_X = 0;
    private static final int SP_Y = 1;
    private static final int SP_PHASE = 2;
    private static final int SP_SEGMENT = 3;

    private static final float FADE_MS = 600f;
    /** How many segments' worth of growth windows overlap along the web. */
    private static final float SEGMENT_OVERLAP = 4f;

    /**
     * {x1, y1, x2, y2, kind}
     */
    private final List<int[]> segments = new ArrayList<>();
    /**
     * {x, y, phase, segIndex} — twinkling glints pinned to grown crystals
     */
    private final List<float[]> sparkles = new ArrayList<>();
    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Frost Creep";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        segments.clear();
        sparkles.clear();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (segments.isEmpty()) generateSegments(w, h);

        long elapsed = System.currentTimeMillis() - startTime;
        float growProgress = Math.min(1f, elapsed / (float) GROW_MS);
        float fadeAlpha = computeAlpha(elapsed);

        drawHaze(ctx, w, h, fadeAlpha);

        // Crystal branches grow along their length, staggered from the edges inward
        float reach = growProgress * (segments.size() + 4);
        drawCrystals(ctx, reach, fadeAlpha);
        drawSparkles(ctx, elapsed, reach, fadeAlpha);
    }

    private float computeAlpha(long elapsed) {
        if (elapsed < FADE_MS) return elapsed / FADE_MS;
        if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < FADE_MS) return Math.max(0f, remaining / FADE_MS);
        }
        return 1f;
    }

    /**
     * Icy edge glow: cold blue outer haze + near-white inner rim.
     */
    private void drawHaze(GuiGraphicsExtractor ctx, int w, int h, float fadeAlpha) {
        int hazeA = (int) (150 * intensity * fadeAlpha);
        EffectPaint.vignette(ctx, w, h, 0xAFD8FF, hazeA, 0.30f * intensity, 0.34f * intensity);
        EffectPaint.vignette(ctx, w, h, 0xEAF7FF, (int) (hazeA * 0.55f), 0.13f * intensity, 0.15f * intensity);
    }

    private void drawCrystals(GuiGraphicsExtractor ctx, float reach, float fadeAlpha) {
        int n = segments.size();
        for (int i = 0; i < n; i++) {
            float segP = EffectPaint.clamp((reach - i) / SEGMENT_OVERLAP, 0f, 1f);
            if (segP <= 0f) break;
            float ease = EffectPaint.easeOutCubic(segP);

            int[] seg = segments.get(i);
            float ex = seg[X1] + (seg[X2] - seg[X1]) * ease;
            float ey = seg[Y1] + (seg[Y2] - seg[Y1]) * ease;

            switch (seg[KIND]) {
                case KIND_MAIN -> {
                    EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey,
                            EffectPaint.argb(0x9FCFF5, (int) (110 * fadeAlpha)), 5);
                    EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey,
                            EffectPaint.argb(0xF2FAFF, (int) (225 * fadeAlpha)), 3);
                }
                case KIND_BRANCH -> EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey,
                        EffectPaint.argb(0xD8EEFF, (int) (195 * fadeAlpha)), 2);
                default -> EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey,
                        EffectPaint.argb(0xBFE2FF, (int) (150 * fadeAlpha)), 1);
            }
        }
    }

    /**
     * Twinkling glints appear once their host segment has fully grown.
     */
    private void drawSparkles(GuiGraphicsExtractor ctx, long elapsed, float reach, float fadeAlpha) {
        for (float[] sp : sparkles) {
            if (reach - sp[SP_SEGMENT] < 4f) continue;
            float tw = 0.25f + 0.75f * Math.max(0f, (float) Math.sin(elapsed * 0.005 + sp[SP_PHASE]));
            int a = (int) (235 * tw * fadeAlpha);
            if (a <= 8) continue;

            int x = (int) sp[SP_X], y = (int) sp[SP_Y];
            int c = EffectPaint.argb(0xFFFFFF, a);
            ctx.fill(x - 2, y, x + 3, y + 1, c);
            ctx.fill(x, y - 2, x + 1, y + 3, c);
            ctx.fill(x, y, x + 1, y + 1, EffectPaint.argb(0xFFFFFF, Math.min(255, a + 40)));
        }
    }

    private void generateSegments(int w, int h) {
        Random rng = new Random(startTime);
        float maxLen = Math.min(w, h) * 0.38f * intensity;

        // Start from all four edges, pointing inward
        float[][] origins = {
                {w * 0.22f, 0, (float) Math.PI / 2},
                {w * 0.55f, 0, (float) Math.PI / 2},
                {w * 0.85f, 0, (float) Math.PI / 2},
                {0, h * 0.35f, 0},
                {0, h * 0.75f, 0},
                {w, h * 0.3f, (float) Math.PI},
                {w, h * 0.7f, (float) Math.PI},
                {w * 0.3f, h, -(float) Math.PI / 2},
                {w * 0.7f, h, -(float) Math.PI / 2},
        };

        for (float[] origin : origins) {
            if (segments.size() >= MAX_SEGMENTS) break;
            float angle = origin[2] + (rng.nextFloat() - 0.5f) * 0.6f;
            float len = maxLen * (0.5f + rng.nextFloat() * 0.5f);
            addCrystal(rng, origin[0], origin[1], angle, len, 3, w, h);
        }

        pinSparkles(rng);
    }

    /**
     * Pin twinkles to random points along the finished crystal web.
     */
    private void pinSparkles(Random rng) {
        int sparkleCount = Math.min(18, segments.size() / 4 + 6);
        for (int i = 0; i < sparkleCount; i++) {
            int segIdx = rng.nextInt(segments.size());
            int[] seg = segments.get(segIdx);
            float t = 0.3f + rng.nextFloat() * 0.7f;
            sparkles.add(new float[]{
                    seg[X1] + (seg[X2] - seg[X1]) * t,
                    seg[Y1] + (seg[Y2] - seg[Y1]) * t,
                    (float) (rng.nextDouble() * Math.PI * 2),
                    segIdx
            });
        }
    }

    private void addCrystal(Random rng, float x, float y, float angle, float length, int depth, int w, int h) {
        if (depth == 0 || length < 8 || segments.size() >= MAX_SEGMENTS) return;

        float ex = x + (float) Math.cos(angle) * length;
        float ey = y + (float) Math.sin(angle) * length;
        ex = Math.clamp(ex, -4, w + 4);
        ey = Math.clamp(ey, -4, h + 4);

        int kind = depth >= 3 ? KIND_MAIN : KIND_BRANCH;
        segments.add(new int[]{(int) x, (int) y, (int) ex, (int) ey, kind});

        addNeedles(rng, x, y, ex, ey, angle, length);

        // Crystals branch at sharper angles than cracks — more geometric
        float branch1 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float branch2 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float newLen = length * (0.45f + rng.nextFloat() * 0.2f);

        addCrystal(rng, ex, ey, branch1, newLen, depth - 1, w, h);
        if (depth > 1 && rng.nextFloat() > 0.35f) {
            addCrystal(rng, ex, ey, branch2, newLen * 0.6f, depth - 1, w, h);
        }
    }

    /**
     * Fern-like needles sprouting sideways along the branch.
     */
    private void addNeedles(Random rng, float x, float y, float ex, float ey, float angle, float length) {
        if (length <= 18) return;

        int needles = 2 + rng.nextInt(2);
        for (int i = 0; i < needles && segments.size() < MAX_SEGMENTS; i++) {
            float t = 0.25f + rng.nextFloat() * 0.55f;
            float nx = x + (ex - x) * t;
            float ny = y + (ey - y) * t;
            float side = rng.nextBoolean() ? 1f : -1f;
            float needleAngle = angle + side * (0.6f + rng.nextFloat() * 0.4f);
            float needleLen = Math.min(14f, length * 0.3f) * (0.5f + rng.nextFloat() * 0.7f);
            segments.add(new int[]{
                    (int) nx, (int) ny,
                    (int) (nx + Math.cos(needleAngle) * needleLen),
                    (int) (ny + Math.sin(needleAngle) * needleLen),
                    KIND_NEEDLE
            });
        }
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    @Override
    public void stop() {
        segments.clear();
        sparkles.clear();
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
