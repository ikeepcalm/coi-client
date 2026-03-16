package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EyesEffect implements VisualEffect {

    public static final String ID = "eyes";

    private int count = 2;
    private long duration = 8000;
    private long startTime;

    private final List<EyeData> eyes = new ArrayList<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Creepy Eyes";
    }

    @Override
    public String getDefaultParams() {
        return "count=2,duration=8000";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        eyes.clear();
    }

    @Override
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        if (eyes.isEmpty()) spawnEyes(w, h);

        long elapsed = System.currentTimeMillis() - startTime;

        for (EyeData eye : eyes) {
            long eyeElapsed = elapsed - eye.offset;
            if (eyeElapsed < 0) continue;

            // Phase timings (ms)
            long fadeIn = 500;
            long opening = 1500;
            long staring = duration - 4500;
            long closing = 1500;
            long fadeOut = 500;

            float ambient, openProgress;

            if (eyeElapsed < fadeIn) {
                ambient = eyeElapsed / (float) fadeIn;
                openProgress = 0f;
            } else if (eyeElapsed < fadeIn + opening) {
                ambient = 1f;
                openProgress = (eyeElapsed - fadeIn) / (float) opening;
            } else if (eyeElapsed < fadeIn + opening + staring) {
                ambient = 1f;
                openProgress = 1f;
            } else if (eyeElapsed < fadeIn + opening + staring + closing) {
                ambient = 1f;
                openProgress = 1f - (eyeElapsed - fadeIn - opening - staring) / (float) closing;
            } else if (eyeElapsed < fadeIn + opening + staring + closing + fadeOut) {
                ambient = 1f - (eyeElapsed - fadeIn - opening - staring - closing) / (float) fadeOut;
                openProgress = 0f;
            } else {
                continue; // this eye is done
            }

            renderEye(ctx, eye, openProgress, ambient, elapsed);
        }
    }

    private void renderEye(DrawContext ctx, EyeData eye, float openProgress, float ambient, long elapsed) {
        int cx = eye.cx, cy = eye.cy;
        int rx = eye.rx, ry = eye.ry;

        // Slight pupil dilation pulsing while staring
        float dilate = (float) (1 + 0.08 * Math.sin(elapsed * 0.002));

        // Ambient dark glow around the eye
        if (ambient > 0) {
            int ga = (int) (100 * ambient);
            drawOval(ctx, cx, cy, rx + 14, ry + 7, (ga << 24));
            drawOval(ctx, cx, cy, rx + 8, ry + 4, (ga * 2 << 24));
        }

        if (openProgress <= 0) return;

        // Sclera (white)
        drawOval(ctx, cx, cy, rx, ry, 0xFFEEEEEE);

        // Iris (sickly yellowish-green with a hint of red for horror)
        int irisR = (int) (ry * 0.8f * dilate);
        drawOval(ctx, cx, cy, irisR, irisR, eye.irisColor);

        // Pupil (vertically elongated — cat-like)
        int pupilW = Math.max(1, (int) (irisR * 0.35f));
        int pupilH = (int) (irisR * 0.85f * dilate);
        drawOval(ctx, cx, cy, pupilW, pupilH, 0xFF000000);

        // Eyelids — cover top and bottom of eye based on open progress
        // openProgress=0: fully closed, 1: fully open
        int lidY = (int) ((1f - openProgress) * ry);
        // Upper lid (covers from top of eye down to cy - ry + lidY)
        ctx.fill(cx - rx - 4, cy - ry - 4, cx + rx + 4, cy - ry + lidY + 2, 0xFF000000);
        // Lower lid (covers from cy + ry - lidY up)
        ctx.fill(cx - rx - 4, cy + ry - lidY - 2, cx + rx + 4, cy + ry + 4, 0xFF000000);
    }

    /**
     * Rasterizes a filled ellipse using scanline approach.
     */
    private static void drawOval(DrawContext ctx, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0) return;
        for (int dy = -ry; dy <= ry; dy++) {
            float frac = (float) (dy * dy) / (float) (ry * ry);
            if (frac > 1f) continue;
            int hw = (int) (rx * Math.sqrt(1f - frac));
            ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    private void spawnEyes(int w, int h) {
        Random rng = new Random(startTime);
        int margin = 55;

        // Divide the screen into a grid so eyes are spread evenly.
        // Up to 3 columns; rows added as needed.
        int cols = Math.min(count, 3);
        int rows = (count + cols - 1) / cols;
        int cellW = (w - margin * 2) / cols;
        int cellH = (h - margin * 2) / rows;
        // Ensure eyes aren't placed so close to cell edges that they overlap neighbours
        int innerPad = margin / 2;

        int[] irisColors = {0xFF8B2500, 0xFF4B3200, 0xFF2D5A00, 0xFF1A1A4A};

        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;

            int cellOriginX = margin + col * cellW;
            int cellOriginY = margin + row * cellH;

            int ex = cellOriginX + innerPad + rng.nextInt(Math.max(1, cellW - innerPad * 2));
            int ey = cellOriginY + innerPad + rng.nextInt(Math.max(1, cellH - innerPad * 2));

            int eyeRx = 18 + rng.nextInt(12);
            int eyeRy = 9 + rng.nextInt(6);
            long offset = i * 400L;
            int irisColor = irisColors[rng.nextInt(irisColors.length)];

            eyes.add(new EyeData(ex, ey, eyeRx, eyeRy, irisColor, offset));
        }
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration + eyes.size() * 400L + 500L;
    }

    @Override
    public void stop() {
        eyes.clear();
    }

    private void parseParams(String params) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "count" -> count = Integer.parseInt(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }

    private record EyeData(int cx, int cy, int rx, int ry, int irisColor, long offset) {
    }
}
