package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FrostEffect implements VisualEffect {

    public static final String ID = "frost";

    private static final int MAX_SEGMENTS = 110;
    private static final long GROW_MS = 2200;

    // Segment kinds — index into stroke style
    private static final int KIND_MAIN = 0;
    private static final int KIND_BRANCH = 1;
    private static final int KIND_NEEDLE = 2;
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

        float fadeAlpha = 1f;
        if (elapsed < 600) fadeAlpha = elapsed / 600f;
        else if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < 600) fadeAlpha = Math.max(0f, remaining / 600f);
        }

        // Icy edge glow: cold blue outer haze + near-white inner rim
        int hazeA = (int) (150 * intensity * fadeAlpha);
        EffectPaint.vignette(ctx, w, h, 0xAFD8FF, hazeA, 0.30f * intensity, 0.34f * intensity);
        EffectPaint.vignette(ctx, w, h, 0xEAF7FF, (int) (hazeA * 0.55f), 0.13f * intensity, 0.15f * intensity);

        // Crystal branches grow along their length, staggered from the edges inward
        int n = segments.size();
        float reach = growProgress * (n + 4);

        for (int i = 0; i < n; i++) {
            float segP = EffectPaint.clamp((reach - i) / 4f, 0f, 1f);
            if (segP <= 0f) break;
            float ease = EffectPaint.easeOutCubic(segP);

            int[] seg = segments.get(i);
            float ex = seg[0] + (seg[2] - seg[0]) * ease;
            float ey = seg[1] + (seg[3] - seg[1]) * ease;

            switch (seg[4]) {
                case KIND_MAIN -> {
                    EffectPaint.line(ctx, seg[0], seg[1], ex, ey,
                            EffectPaint.argb(0x9FCFF5, (int) (110 * fadeAlpha)), 5);
                    EffectPaint.line(ctx, seg[0], seg[1], ex, ey,
                            EffectPaint.argb(0xF2FAFF, (int) (225 * fadeAlpha)), 3);
                }
                case KIND_BRANCH -> EffectPaint.line(ctx, seg[0], seg[1], ex, ey,
                        EffectPaint.argb(0xD8EEFF, (int) (195 * fadeAlpha)), 2);
                default -> EffectPaint.line(ctx, seg[0], seg[1], ex, ey,
                        EffectPaint.argb(0xBFE2FF, (int) (150 * fadeAlpha)), 1);
            }
        }

        // Twinkling glints appear once their host segment has fully grown
        for (float[] sp : sparkles) {
            if (reach - sp[3] < 4f) continue;
            float tw = 0.25f + 0.75f * Math.max(0f, (float) Math.sin(elapsed * 0.005 + sp[2]));
            int a = (int) (235 * tw * fadeAlpha);
            if (a <= 8) continue;

            int x = (int) sp[0], y = (int) sp[1];
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

        // Pin twinkles to random points along the finished crystal web
        int sparkleCount = Math.min(18, segments.size() / 4 + 6);
        for (int i = 0; i < sparkleCount; i++) {
            int segIdx = rng.nextInt(segments.size());
            int[] seg = segments.get(segIdx);
            float t = 0.3f + rng.nextFloat() * 0.7f;
            sparkles.add(new float[]{
                    seg[0] + (seg[2] - seg[0]) * t,
                    seg[1] + (seg[3] - seg[1]) * t,
                    (float) (rng.nextDouble() * Math.PI * 2),
                    segIdx
            });
        }
    }

    private void addCrystal(Random rng, float x, float y, float angle, float length, int depth, int w, int h) {
        if (depth == 0 || length < 8 || segments.size() >= MAX_SEGMENTS) return;

        float ex = x + (float) Math.cos(angle) * length;
        float ey = y + (float) Math.sin(angle) * length;
        ex = Math.max(-4, Math.min(w + 4, ex));
        ey = Math.max(-4, Math.min(h + 4, ey));

        int kind = depth >= 3 ? KIND_MAIN : KIND_BRANCH;
        segments.add(new int[]{(int) x, (int) y, (int) ex, (int) ey, kind});

        // Fern-like needles sprouting sideways along the branch
        if (length > 18) {
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

        // Crystals branch at sharper angles than cracks — more geometric
        float branch1 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float branch2 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float newLen = length * (0.45f + rng.nextFloat() * 0.2f);

        addCrystal(rng, ex, ey, branch1, newLen, depth - 1, w, h);
        if (depth > 1 && rng.nextFloat() > 0.35f) {
            addCrystal(rng, ex, ey, branch2, newLen * 0.6f, depth - 1, w, h);
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
