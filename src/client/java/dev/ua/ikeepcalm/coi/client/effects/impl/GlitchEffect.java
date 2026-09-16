package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

public class GlitchEffect implements VisualEffect {

    public static final String ID = "glitch";

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
        long tick = elapsed / 80;
        Random rng = new Random(tick * 0x9E3779B97F4A7C15L + startTime);

        float alpha = 0.55f + 0.45f * intensity;

        // Faint scanlines over the whole frame during a burst
        int scanA = (int) (14 * alpha);
        for (int y = (int) (tick % 3); y < h; y += 3) {
            ctx.fill(0, y, w, y + 1, scanA << 24);
        }

        // Horizontal artifact bands
        int lineCount = (int) (3 + intensity * 5);
        for (int i = 0; i < lineCount; i++) {
            int y = rng.nextInt(h);
            int bh = 1 + rng.nextInt((int) Math.max(1, 4 * intensity));
            // Bands are randomly full-width or torn (partial with an offset start)
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
                    ctx.fill(x0, y, x1, y + 1, (ra << 24) | 0xFF2200);
                    ctx.fill(x0, y + bh, x1, y + bh + 1, (ca << 24) | 0x00EEFF);
                }
                case 3 -> {
                    // Horizontal offset block — a lighter band on one side of the screen
                    int splitX = w / 3 + rng.nextInt(w / 3);
                    int a = (int) (45 * alpha);
                    ctx.fill(splitX, y, w, y + bh, (a << 24) | 0xCCCCCC);
                }
            }
        }

        // Corrupted blocks with RGB split — the signature "digital damage" look
        int blockCount = 1 + rng.nextInt(2 + (int) (2 * intensity));
        for (int i = 0; i < blockCount; i++) {
            int bw = w / 8 + rng.nextInt(w / 5);
            int bhgt = 4 + rng.nextInt(12);
            int bx = rng.nextInt(Math.max(1, w - bw));
            int by = rng.nextInt(Math.max(1, h - bhgt));
            int a = (int) (55 * alpha);
            ctx.fill(bx - 2, by, bx + bw - 2, by + bhgt, (a << 24) | 0xFF2200);
            ctx.fill(bx + 2, by, bx + bw + 2, by + bhgt, (a << 24) | 0x00EEFF);
            ctx.fill(bx, by, bx + bw, by + bhgt, ((int) (65 * alpha) << 24) | 0xBBBBBB);
        }

        // Static noise specks
        int speckCount = (int) (35 * intensity);
        for (int i = 0; i < speckCount; i++) {
            int sx = rng.nextInt(w);
            int sy = rng.nextInt(h);
            int size = 1 + rng.nextInt(2);
            int col = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
            ctx.fill(sx, sy, sx + size, sy + size, ((int) (90 * alpha) << 24) | col);
        }

        // Occasional full-screen bright flash at very high intensity
        if (intensity > 0.85f && rng.nextFloat() < 0.15f) {
            ctx.fill(0, 0, w, h, 0x0CFFFFFF);
        }
    }

    private void renderTrackingBar(GuiGraphicsExtractor ctx, int w, int h, long elapsed, boolean inBurst) {
        float roll = (elapsed % 4200) / 4200f;
        int barH = Math.max(8, h / 14);
        int barY = (int) (roll * (h + barH * 2)) - barH;

        int barA = (int) ((inBurst ? 28 : 14) * (0.5f + 0.5f * intensity));
        int mid = barY + barH / 2;
        ctx.fillGradient(0, barY, w, mid,
                EffectPaint.argb(0xFFFFFF, 0), EffectPaint.argb(0xFFFFFF, barA));
        ctx.fillGradient(0, mid, w, barY + barH,
                EffectPaint.argb(0xFFFFFF, barA), EffectPaint.argb(0xFFFFFF, 0));
        // Chromatic tear on the bar's trailing edge
        ctx.fill(0, barY + barH, w, barY + barH + 1, EffectPaint.argb(0x00EEFF, barA + 25));
        ctx.fill(0, barY - 1, w, barY, EffectPaint.argb(0xFF2200, barA + 15));
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
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
