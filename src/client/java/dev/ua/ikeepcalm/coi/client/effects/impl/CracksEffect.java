package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CracksEffect implements VisualEffect {

    public static final String ID = "cracks";

    private static final int MAX_SEGMENTS = 14;  // hard cap — keeps fill calls under ~280/frame
    private static final int MAX_QUADS_LINE = 20;  // max fill() calls per segment

    private float intensity = 0.7f;
    private boolean pulse = false;
    private long duration = -1;
    private long startTime;

    /**
     * Each segment: {x1, y1, x2, y2}
     */
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

        int crackColor = ((int) (230 * pulseVal) << 24) | 0x0A0A0A;
        int redAlpha = pulse ? (int) (110 * pulseVal * intensity) : 0;
        int redColor = (redAlpha << 24) | 0xFF0000;

        for (int i = 0; i < Math.min(visibleCount, segments.size()); i++) {
            int[] seg = segments.get(i);
            drawLine(ctx, seg[0], seg[1], seg[2], seg[3], crackColor, 3);
            if (pulse) {
                drawLine(ctx, seg[0], seg[1], seg[2], seg[3], redColor, 2);
            }
        }
    }

    private void generateSegments(int w, int h) {
        Random rng = new Random(startTime);
        int cx = w / 2, cy = h / 2;
        float maxLen = (float) Math.sqrt(cx * cx + cy * cy) * 0.7f * intensity;

        int[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (int[] corner : corners) {
            if (segments.size() >= MAX_SEGMENTS) break;
            float angle = (float) Math.atan2(cy - corner[1], cx - corner[0]);
            angle += (rng.nextFloat() - 0.5f) * 0.5f;
            addCrack(rng, corner[0], corner[1], angle, maxLen * (0.5f + rng.nextFloat() * 0.5f), 3, w, h);
        }
    }

    private void addCrack(Random rng, float x, float y, float angle, float length, int depth, int w, int h) {
        if (depth == 0 || length < 10 || segments.size() >= MAX_SEGMENTS) return;

        float ex = x + (float) Math.cos(angle) * length;
        float ey = y + (float) Math.sin(angle) * length;
        ex = Math.max(-5, Math.min(w + 5, ex));
        ey = Math.max(-5, Math.min(h + 5, ey));

        segments.add(new int[]{(int) x, (int) y, (int) ex, (int) ey});

        float branch1 = angle + (rng.nextFloat() - 0.5f) * 0.7f;
        float branch2 = angle + (rng.nextFloat() - 0.5f) * 0.9f;
        float newLen = length * (0.35f + rng.nextFloat() * 0.25f);

        addCrack(rng, ex, ey, branch1, newLen, depth - 1, w, h);
        if (depth > 1 && rng.nextFloat() > 0.45f) {
            addCrack(rng, ex, ey, branch2, newLen * 0.6f, depth - 2, w, h);
        }
    }

    /**
     * Draws a line as a series of quads, capped at MAX_QUADS_LINE fill calls.
     * Uses Euclidean length for stride so diagonal segments aren't over-sampled.
     */
    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = x2 - x1, dy = y2 - y1;
        int length = (int) Math.sqrt((double) dx * dx + (double) dy * dy);
        if (length == 0) return;
        int stride = Math.max(thickness, length / MAX_QUADS_LINE);
        float sx = (float) dx / length;
        float sy = (float) dy / length;
        for (int i = 0; i <= length; i += stride) {
            int px = (int) (x1 + sx * i);
            int py = (int) (y1 + sy * i);
            ctx.fill(px, py, px + thickness, py + thickness, color);
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
