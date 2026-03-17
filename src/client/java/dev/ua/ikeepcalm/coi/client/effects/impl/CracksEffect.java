package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CracksEffect implements VisualEffect {

    public static final String ID = "cracks";

    private static final int MAX_SEGMENTS = 30;

    private float intensity = 0.7f;
    private boolean pulse = false;
    private long duration = -1;
    private long startTime;

    private final List<int[]> segments = new ArrayList<>();

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
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        if (segments.isEmpty()) generateSegments(w, h);

        long elapsed = System.currentTimeMillis() - startTime;
        float growProgress = Math.min(1f, elapsed / 1200f);
        int visibleCount = (int) (segments.size() * growProgress);

        float pulseVal = pulse ? (float) (0.7 + 0.3 * Math.sin(elapsed * 0.004)) : 1f;
        int crackAlpha = (int) (230 * pulseVal);
        int crackColor = (crackAlpha << 24) | 0x080808;
        int redAlpha = pulse ? (int) (120 * pulseVal * intensity) : 0;
        int redColor = (redAlpha << 24) | 0xFF1100;

        for (int i = 0; i < Math.min(visibleCount, segments.size()); i++) {
            int[] seg = segments.get(i);
            drawSegment(ctx, seg[0], seg[1], seg[2], seg[3], crackColor, seg[4]);
            if (pulse && redAlpha > 0) {
                drawSegment(ctx, seg[0], seg[1], seg[2], seg[3], redColor, Math.max(1, seg[4] - 1));
            }
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
        ex = Math.max(-5, Math.min(w + 5, ex));
        ey = Math.max(-5, Math.min(h + 5, ey));

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

    /**
     * Draws a true straight line as a single rotated filled rectangle,
     * so diagonal cracks render as clean solid lines instead of dotted quads.
     */
    private static void drawSegment(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = x2 - x1, dy = y2 - y1;
        int length = (int) Math.sqrt((double) dx * dx + (double) dy * dy);
        if (length == 0) return;

        float angle = (float) Math.atan2(dy, dx);
        float mx = (x1 + x2) / 2f;
        float my = (y1 + y2) / 2f;
        int half = length / 2 + 1;
        int halfT = Math.max(1, thickness / 2);

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(mx, my);
        matrices.rotate(angle);
        ctx.fill(-half, -halfT, half, halfT, color);
        matrices.popMatrix();
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
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "pulse" -> pulse = Boolean.parseBoolean(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
