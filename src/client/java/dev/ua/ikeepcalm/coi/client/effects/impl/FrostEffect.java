package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FrostEffect implements VisualEffect {

    public static final String ID = "frost";

    private static final int MAX_SEGMENTS = 14;
    private static final int MAX_QUADS_SEG = 12;

    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;

    private final List<int[]> segments = new ArrayList<>();

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
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (segments.isEmpty()) generateSegments(w, h);

        long elapsed = System.currentTimeMillis() - startTime;
        float growProgress = Math.min(1f, elapsed / 1500f);

        float fadeAlpha = 1f;
        if (elapsed < 600) fadeAlpha = elapsed / 600f;
        else if (duration > 0) {
            long remaining = duration - elapsed;
            if (remaining < 600) fadeAlpha = Math.max(0f, remaining / 600f);
        }

        // Blue-white icy edge gradient
        int vigW = (int) (w * 0.3f * intensity);
        int vigH = (int) (h * 0.35f * intensity);
        int maxA = (int) (160 * intensity * fadeAlpha);

        ctx.fillGradient(0, 0, w, vigH, (maxA << 24) | 0xAADDFF, 0x00AADDFF);
        ctx.fillGradient(0, h - vigH, w, h, 0x00AADDFF, (maxA << 24) | 0xAADDFF);

        int bands = 10;
        int stepW = Math.max(1, vigW / bands);
        for (int i = 0; i < bands; i++) {
            float t = (float) (bands - i) / bands;
            int a = (int) (maxA * t * t);
            ctx.fill(i * stepW, 0, i * stepW + stepW, h, (a << 24) | 0xAADDFF);
            ctx.fill(w - i * stepW - stepW, 0, w - i * stepW, h, (a << 24) | 0xAADDFF);
        }

        // Crystal line segments
        int visibleCount = (int) (segments.size() * growProgress);
        for (int i = 0; i < Math.min(visibleCount, segments.size()); i++) {
            int[] seg = segments.get(i);
            int crystalA = (int) (210 * fadeAlpha);
            drawLine(ctx, seg[0], seg[1], seg[2], seg[3], (crystalA << 24) | 0xEEF6FF, 3);
        }
    }

    private void generateSegments(int w, int h) {
        Random rng = new Random(startTime);
        int cx = w / 2, cy = h / 2;
        float maxLen = Math.min(w, h) * 0.35f * intensity;

        // Start from midpoints of all four edges, pointing inward
        float[][] origins = {
                {w * 0.25f, 0, (float) Math.PI / 2},    // top-left quarter
                {w * 0.75f, 0, (float) Math.PI / 2},    // top-right quarter
                {0, h * 0.4f, 0},                       // left
                {w, h * 0.4f, (float) Math.PI},          // right
                {w * 0.35f, h, -(float) Math.PI / 2},     // bottom
                {w * 0.65f, h, -(float) Math.PI / 2},     // bottom-right
        };

        for (float[] origin : origins) {
            if (segments.size() >= MAX_SEGMENTS) break;
            float angle = origin[2] + (rng.nextFloat() - 0.5f) * 0.6f;
            float len = maxLen * (0.5f + rng.nextFloat() * 0.5f);
            addCrystal(rng, origin[0], origin[1], angle, len, 3, w, h);
        }
    }

    private void addCrystal(Random rng, float x, float y, float angle, float length, int depth, int w, int h) {
        if (depth == 0 || length < 8 || segments.size() >= MAX_SEGMENTS) return;

        float ex = x + (float) Math.cos(angle) * length;
        float ey = y + (float) Math.sin(angle) * length;
        ex = Math.max(-4, Math.min(w + 4, ex));
        ey = Math.max(-4, Math.min(h + 4, ey));

        segments.add(new int[]{(int) x, (int) y, (int) ex, (int) ey});

        // Crystals branch at sharper angles than cracks — more geometric
        float branch1 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float branch2 = angle + (rng.nextFloat() - 0.5f) * 1.1f;
        float newLen = length * (0.4f + rng.nextFloat() * 0.2f);

        addCrystal(rng, ex, ey, branch1, newLen, depth - 1, w, h);
        if (depth > 1 && rng.nextFloat() > 0.4f) {
            addCrystal(rng, ex, ey, branch2, newLen * 0.55f, depth - 2, w, h);
        }
    }

    private static void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = x2 - x1, dy = y2 - y1;
        int length = (int) Math.sqrt((double) dx * dx + (double) dy * dy);
        if (length == 0) return;
        int stride = Math.max(thickness, length / MAX_QUADS_SEG);
        float sx = (float) dx / length, sy = (float) dy / length;
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
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
