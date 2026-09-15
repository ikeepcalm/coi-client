package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Branching fracture lines shattering inward from the screen corners and edge
 * midpoints, over an impact flash and a damage vignette. Each crack grows along
 * its own length and is stroked as glass — dark outline plus a bright core.
 * <p>
 * Params: {@code intensity} (0-1, how far the cracks reach), {@code pulse}
 * (boolean, add a rhythmic red glow), {@code duration} (ms, {@code -1} = until
 * stopped).
 * <p>
 * The pattern is generated once on first render and seeded by the trigger time,
 * so every trigger shatters differently.
 */
public class CracksEffect implements VisualEffect {

    public static final String ID = "cracks";

    private static final int MAX_SEGMENTS = 64;
    private static final long GROW_MS = 1400;

    private static final float IMPACT_FLASH_MS = 140f;
    private static final float DAMAGE_VIGNETTE_MS = 600f;
    /** How many segments' worth of growth windows overlap, so the shatter reads as continuous. */
    private static final float SEGMENT_OVERLAP = 3f;

    /** Indices into a segment's {@code {x1, y1, x2, y2, thickness}} array. */
    private static final int X1 = 0;
    private static final int Y1 = 1;
    private static final int X2 = 2;
    private static final int Y2 = 3;
    private static final int THICKNESS = 4;

    private final List<int[]> segments = new ArrayList<>();
    private float intensity = 0.7f;
    private boolean pulse = false;
    private long duration = -1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Screen Cracks";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,pulse=true,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        segments.clear();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (segments.isEmpty()) generateSegments(w, h);

        long elapsed = System.currentTimeMillis() - startTime;

        // Impact flash sells the initial shatter
        if (elapsed < IMPACT_FLASH_MS) {
            float f = 1f - elapsed / IMPACT_FLASH_MS;
            ctx.fill(0, 0, w, h, EffectPaint.argb(0xFFFFFF, (int) (85 * intensity * f * f)));
        }

        // Damage vignette rises in as the cracks spread
        float vigIn = Math.min(1f, elapsed / DAMAGE_VIGNETTE_MS);
        EffectPaint.vignette(ctx, w, h, 0x000000, (int) (60 * intensity * vigIn), 0.22f, 0.26f);

        drawSegments(ctx, elapsed);
    }

    private void drawSegments(GuiGraphicsExtractor ctx, long elapsed) {
        float growProgress = Math.min(1f, elapsed / (float) GROW_MS);
        float pulseVal = pulse ? (float) (0.7 + 0.3 * Math.sin(elapsed * 0.004)) : 1f;

        int n = segments.size();
        // Each segment grows along its own length; windows overlap so the
        // propagation reads as one continuous shatter, not a slideshow.
        float reach = growProgress * (n + 3);

        int glowColor = EffectPaint.argb(0xFF2A00, (int) (95 * pulseVal * intensity));
        int darkColor = EffectPaint.argb(0x05060A, 205);
        int coreColor = EffectPaint.argb(0xE9F1F8, (int) (185 * (0.8f + 0.2f * pulseVal)));

        for (int i = 0; i < n; i++) {
            float segP = EffectPaint.clamp((reach - i) / SEGMENT_OVERLAP, 0f, 1f);
            if (segP <= 0f) break;
            float ease = EffectPaint.easeOutCubic(segP);

            int[] seg = segments.get(i);
            float ex = seg[X1] + (seg[X2] - seg[X1]) * ease;
            float ey = seg[Y1] + (seg[Y2] - seg[Y1]) * ease;
            int thickness = seg[THICKNESS];

            if (pulse) {
                EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey, glowColor, thickness + 3);
            }
            EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey, darkColor, thickness + 1);
            EffectPaint.line(ctx, seg[X1], seg[Y1], ex, ey, coreColor, Math.max(1, thickness - 1));
        }
    }

    private void generateSegments(int w, int h) {
        Random rng = new Random(startTime);
        int cx = w / 2, cy = h / 2;
        float maxLen = (float) Math.sqrt(cx * cx + cy * cy) * 0.75f * intensity;

        // 4 corners + 4 edge midpoints = 8 origins
        int[][] origins = {{0, 0}, {w, 0}, {0, h}, {w, h}, {cx, 0}, {cx, h}, {0, cy}, {w, cy}};

        for (int[] o : origins) {
            if (segments.size() >= MAX_SEGMENTS) break;
            float angle = (float) Math.atan2(cy - o[1], cx - o[0]);
            angle += (rng.nextFloat() - 0.5f) * 0.6f;
            float len = maxLen * (0.45f + rng.nextFloat() * 0.55f);
            addCrack(rng, o[0], o[1], angle, len, 4, w, h, 5);
        }
    }

    private void addCrack(Random rng, float x, float y, float angle, float length, int depth, int w, int h, int thickness) {
        if (depth == 0 || length < 12 || segments.size() >= MAX_SEGMENTS) return;

        float ex = x + (float) Math.cos(angle) * length;
        float ey = y + (float) Math.sin(angle) * length;
        ex = Math.clamp(ex, -5, w + 5);
        ey = Math.clamp(ey, -5, h + 5);

        segments.add(new int[]{(int) x, (int) y, (int) ex, (int) ey, thickness});

        int nextThick = Math.max(1, thickness - 1);
        float newLen = length * (0.38f + rng.nextFloat() * 0.27f);

        // Primary branch — always spawns
        float b1 = angle + (rng.nextFloat() - 0.5f) * 0.75f;
        addCrack(rng, ex, ey, b1, newLen, depth - 1, w, h, nextThick);

        // Secondary branch — 75% chance
        if (rng.nextFloat() < 0.75f) {
            float b2 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
            addCrack(rng, ex, ey, b2, newLen * 0.65f, depth - 1, w, h, nextThick);
        }

        // Tertiary splinter off the main trunk — 30% chance at higher depths
        if (depth >= 3 && rng.nextFloat() < 0.30f) {
            float b3 = angle + (rng.nextFloat() - 0.5f) * 1.4f;
            addCrack(rng, ex, ey, b3, newLen * 0.4f, depth - 2, w, h, 1);
        }
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    @Override
    public void stop() {
        segments.clear();
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "pulse" -> pulse = Boolean.parseBoolean(value);
                case "duration" -> duration = Long.parseLong(value);
            }
        });
    }
}
