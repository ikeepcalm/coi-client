package dev.ua.ikeepcalm.coi.client.effects.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared 2D paint helpers for screen-space effects. Vanilla only exposes
 * vertical gradients, so horizontal gradients and soft-edged shapes are
 * composed here via pose rotation and layered fills.
 */
public final class EffectPaint {

    private EffectPaint() {
    }

    public static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float smoothstep(float t) {
        t = clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float easeOutCubic(float t) {
        float inv = 1f - clamp(t, 0f, 1f);
        return 1f - inv * inv * inv;
    }

    /**
     * Horizontal gradient (colorLeft → colorRight) drawn as a 90°-rotated
     * vertical gradient, since only vertical gradients exist natively.
     */
    public static void hGradient(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int colorLeft, int colorRight) {
        if (x2 <= x1 || y2 <= y1) return;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x2, y1);
        pose.rotate((float) (Math.PI / 2));
        ctx.fillGradient(0, 0, y2 - y1, x2 - x1, colorRight, colorLeft);
        pose.popMatrix();
    }

    /**
     * Four-sided soft vignette with a layered (pseudo-curved) falloff.
     * Corners darken naturally where the edge gradients overlap.
     */
    public static void vignette(GuiGraphicsExtractor ctx, int w, int h, int rgb, int alpha, float spreadX, float spreadY) {
        if (alpha <= 0) return;
        int vw = Math.max(2, (int) (w * clamp(spreadX, 0.02f, 0.5f)));
        int vh = Math.max(2, (int) (h * clamp(spreadY, 0.02f, 0.5f)));
        int aWide = Math.min(255, Math.round(alpha * 0.62f));
        int aTight = Math.min(255, Math.round(alpha * 0.55f));
        int clear = rgb & 0xFFFFFF;

        ctx.fillGradient(0, 0, w, vh, argb(rgb, aWide), clear);
        ctx.fillGradient(0, h - vh, w, h, clear, argb(rgb, aWide));
        ctx.fillGradient(0, 0, w, vh * 9 / 20, argb(rgb, aTight), clear);
        ctx.fillGradient(0, h - vh * 9 / 20, w, h, clear, argb(rgb, aTight));

        hGradient(ctx, 0, 0, vw, h, argb(rgb, aWide), clear);
        hGradient(ctx, w - vw, 0, w, h, clear, argb(rgb, aWide));
        hGradient(ctx, 0, 0, vw * 9 / 20, h, argb(rgb, aTight), clear);
        hGradient(ctx, w - vw * 9 / 20, 0, w, h, clear, argb(rgb, aTight));
    }

    /**
     * Solid straight line drawn as a single rotated rectangle, so diagonals
     * render as clean solid strokes instead of dotted quads.
     */
    public static void line(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5f) return;

        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((x1 + x2) / 2f, (y1 + y2) / 2f);
        pose.rotate((float) Math.atan2(dy, dx));
        int half = (int) (length / 2f) + 1;
        if (thickness <= 1) {
            ctx.fill(-half, 0, half, 1, color);
        } else {
            int halfT = Math.max(1, thickness / 2);
            ctx.fill(-half, -halfT, half, halfT, color);
        }
        pose.popMatrix();
    }
}
