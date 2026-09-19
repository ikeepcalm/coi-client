package dev.ua.ikeepcalm.coi.client.hud.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Stateless drawing primitives shared by the madness and spirituality bars.
 * <p>
 * Each helper is exactly one visual layer, in the order they stack: frame,
 * fill, shimmer, notches, label. Anything stage- or resource-specific
 * (glitch slices, cracks, markers) stays in the overlay that owns it.
 */
public class CoiBar {

    /**
     * Background gradient of an empty bar; the fill is drawn over it.
     */
    private static final int BG_TOP = 0xFF1B1B1E;
    private static final int BG_BOTTOM = 0xFF0F0F11;

    private CoiBar() {
    }

    /**
     * 1px border around the bar plus the recessed background gradient.
     */
    public static void frame(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int borderColor) {
        frame(ctx, x, y, w, h, borderColor, 1f);
    }

    /**
     * Same frame, with the whole chrome faded — used by bars that can ease
     * themselves out of view.
     */
    public static void frame(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int borderColor, float alpha) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(borderColor, alpha));
        ctx.fillGradient(x, y, x + w, y + h, withAlpha(BG_TOP, alpha), withAlpha(BG_BOTTOM, alpha));
    }

    /**
     * Multiplies a colour's alpha channel by {@code factor}.
     */
    public static int withAlpha(int color, float factor) {
        int a = (int) ((color >>> 24) * Mth.clamp(factor, 0f, 1f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /**
     * Filled region with a top highlight / bottom shade bevel.
     */
    public static void fill(GuiGraphicsExtractor ctx, int x, int y, int h, int fillW, int top, int bottom) {
        fill(ctx, x, y, h, fillW, top, bottom, 1f);
    }

    /**
     * Same fill, faded whole — the counterpart of the {@link #frame} overload
     * above. The bevel's two hairlines are the reason this exists: they are the
     * bar's own colours rather than the caller's, so a caller fading a bar
     * cannot reach them by dimming what it passes in.
     */
    public static void fill(GuiGraphicsExtractor ctx, int x, int y, int h, int fillW,
                            int top, int bottom, float alpha) {
        if (fillW <= 0) return;
        ctx.fillGradient(x, y, x + fillW, y + h, withAlpha(top, alpha), withAlpha(bottom, alpha));
        ctx.fill(x, y, x + fillW, y + 1, withAlpha(0x40FFFFFF, alpha));
        ctx.fill(x, y + h - 1, x + fillW, y + h, withAlpha(0x40000000, alpha));
    }

    /**
     * Soft highlight band sweeping left-to-right across the fill.
     */
    public static void shimmer(GuiGraphicsExtractor ctx, int x, int y, int h, int fillW, long time, long periodMs) {
        if (fillW <= 8 || periodMs <= 0) return;
        float sweep = (time % periodMs) / (float) periodMs;
        int bandX = x - 16 + (int) ((fillW + 32) * sweep);
        int[] offs = {0, 4, 8};
        int[] alphas = {30, 70, 30};
        for (int i = 0; i < offs.length; i++) {
            int x0 = Math.max(x, bandX + offs[i]);
            int x1 = Math.min(x + fillW, bandX + offs[i] + 4);
            if (x1 > x0) {
                ctx.fill(x0, y + 1, x1, y + h - 1, (alphas[i] << 24) | 0xFFFFFF);
            }
        }
    }

    /**
     * Divider ticks splitting the bar into {@code segments} equal parts
     * (three notches for four segments).
     */
    public static void notches(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int segments, int color) {
        for (int q = 1; q < segments; q++) {
            int nx = x + w * q / segments;
            ctx.fill(nx, y, nx + 1, y + h, color);
        }
    }

    /**
     * Shadowed caption centred on the bar, sitting 10px above it.
     */
    public static void label(GuiGraphicsExtractor ctx, Font font, String text, int x, int y, int w, int color) {
        int textX = x + (w - font.width(text)) / 2;
        ctx.text(font, text, textX, y - 10, color, true);
    }

    /**
     * Pixel width of the filled region for {@code shown} out of {@code max}.
     */
    public static int lerpWidth(double shown, double max, int w) {
        if (max <= 0) return 0;
        return Mth.clamp((int) (w * (shown / max)), 0, w);
    }
}
