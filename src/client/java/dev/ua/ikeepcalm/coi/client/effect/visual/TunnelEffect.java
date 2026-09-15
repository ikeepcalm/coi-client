package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Circular vignette that closes inward until only a shrinking oval of
 * visibility is left, with a soft gradient edge. The closing is eased and the
 * hole breathes and drifts slightly off-center. Drawn by scanline fill, a few
 * hundred draw calls per frame while closed.
 * <p>
 * Params: {@code intensity} (0-1, how far it closes), {@code duration} (ms),
 * {@code closeDuration} (ms to reach full intensity, reused for the reopen).
 */
public class TunnelEffect implements VisualEffect {

    public static final String ID = "tunnel";

    /**
     * Radius multipliers of the two soft bands around the clear hole.
     */
    private static final float MID_BAND = 1.14f;
    private static final float OUTER_BAND = 1.30f;

    private static final int SOLID = 0xFF000000;
    private static final int BAND_70 = 0xB4000000;
    private static final int BAND_35 = 0x59000000;

    /**
     * Scanline height: the hole's edge is smooth enough at 3px rows.
     */
    private static final int ROW_STEP = 3;

    private float intensity = 0.7f;
    private long duration = 6000;
    private long closeDuration = 2000;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Tunnel Vision";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=6000,closeDuration=2000";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        float closed = closedFraction(elapsed) * intensity; // 0 = full screen visible, intensity = fully closed
        if (closed <= 0.001f) return;

        // The hole breathes and drifts slightly off-center — claustrophobic, organic
        float breathe = 1f + 0.025f * (float) Math.sin(elapsed * 0.0032);
        float rx = w * 0.5f * (1f - closed * 0.93f) * breathe;
        float ry = h * 0.5f * (1f - closed * 0.93f) * breathe;
        float cx = w / 2f + (float) Math.sin(elapsed * 0.0011) * w * 0.008f * closed;
        float cy = h / 2f + (float) Math.cos(elapsed * 0.0009) * h * 0.008f * closed;

        for (int y = 0; y < h; y += ROW_STEP) {
            drawRow(ctx, w, y, cx, cy, rx, ry);
        }
    }

    /**
     * Close phase, hold, open phase — eased so the walls glide, not march.
     * Returns 0 (wide open) to 1 (fully closed).
     */
    private float closedFraction(long elapsed) {
        float closedFrac;
        long remaining = duration - elapsed;
        if (elapsed < closeDuration) {
            closedFrac = (float) elapsed / closeDuration;
        } else if (remaining < closeDuration) {
            closedFrac = Math.max(0f, (float) remaining / closeDuration);
        } else {
            closedFrac = 1f;
        }
        return EffectPaint.smoothstep(closedFrac);
    }

    /**
     * One scanline of three nested ellipses: transparent hole → 35% → 70% →
     * solid black, giving the edge a soft falloff instead of a hard cutout.
     */
    private void drawRow(GuiGraphicsExtractor ctx, int w, int y, float cx, float cy, float rx, float ry) {
        float dy = (y + ROW_STEP / 2f) - cy;

        float fyOuter = ry > 0 ? dy / (ry * OUTER_BAND) : 2f;
        if (Math.abs(fyOuter) >= 1f) {
            // Row entirely outside the outer ellipse
            ctx.fill(0, y, w, y + ROW_STEP, SOLID);
            return;
        }
        float xw2 = rx * OUTER_BAND * (float) Math.sqrt(1.0 - fyOuter * fyOuter);
        int l2 = (int) (cx - xw2);
        int r2 = (int) (cx + xw2);
        if (l2 > 0) ctx.fill(0, y, l2, y + ROW_STEP, SOLID);
        if (r2 < w) ctx.fill(r2, y, w, y + ROW_STEP, SOLID);

        float fyMid = dy / (ry * MID_BAND);
        if (Math.abs(fyMid) >= 1f) {
            ctx.fill(l2, y, r2, y + ROW_STEP, BAND_70);
            return;
        }
        float xw1 = rx * MID_BAND * (float) Math.sqrt(1.0 - fyMid * fyMid);
        int l1 = (int) (cx - xw1);
        int r1 = (int) (cx + xw1);
        if (l1 > l2) ctx.fill(l2, y, l1, y + ROW_STEP, BAND_70);
        if (r2 > r1) ctx.fill(r1, y, r2, y + ROW_STEP, BAND_70);

        float fyInner = dy / ry;
        if (Math.abs(fyInner) >= 1f) {
            ctx.fill(l1, y, r1, y + ROW_STEP, BAND_35);
            return;
        }
        float xw0 = rx * (float) Math.sqrt(1.0 - fyInner * fyInner);
        int l0 = (int) (cx - xw0);
        int r0 = (int) (cx + xw0);
        if (l0 > l1) ctx.fill(l1, y, l0, y + ROW_STEP, BAND_35);
        if (r1 > r0) ctx.fill(r0, y, r1, y + ROW_STEP, BAND_35);
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration;
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "duration" -> duration = Long.parseLong(value);
                case "closeDuration" -> closeDuration = Long.parseLong(value);
            }
        });
    }
}
