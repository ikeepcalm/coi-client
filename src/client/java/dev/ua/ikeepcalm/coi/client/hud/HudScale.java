package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import java.util.BitSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Per-element HUD scaling.
 * <p>
 * <b>The one rule:</b> every bar and overlay scales about its own <em>fill
 * origin</em> — the point its {@code anchor()} / placement math already
 * returns. Growing an element therefore pushes it right and down from where
 * the player dropped it in the layout editor and never teleports it, which is
 * what keeps the stored anchor + offsets meaningful at any scale.
 * <p>
 * The practical consequence for callers: {@link #push} makes the origin a
 * fixed point of the transform, so every draw call inside the push/pop keeps
 * using its ordinary, unscaled screen coordinates — a 182px bar drawn at the
 * origin simply comes out {@code 182 * scale} wide. Geometry that has to be
 * reported <em>outside</em> the push (anchor resolution, the layout editor's
 * bounds) goes through {@link #size} instead.
 * <p>
 * Render-thread only: the push/pop pairing is tracked in a static stack.
 */
public class HudScale {

    /**
     * Scales this close to 1 are treated as "no scaling at all" and skip the
     * matrix work entirely.
     */
    private static final float EPSILON = 1e-4f;

    /**
     * Which of the currently open {@link #push} levels actually touched the
     * pose, so {@link #pop} only unwinds the ones that did.
     */
    private static final BitSet PUSHED = new BitSet();
    private static int depth = 0;

    private HudScale() {
    }

    /**
     * Clamps a raw setting into the supported range.
     */
    public static float clamp(float scale) {
        return Mth.clamp(scale, HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE);
    }

    /**
     * A pixel measurement at this scale — the scaled counterpart of any
     * constant that leaves the push, such as a bar width fed to
     * {@link HudAnchor#resolve} or a layout-editor bound.
     */
    public static int size(int base, float scale) {
        return Math.round(base * clamp(scale));
    }

    /**
     * Scales about {@code (originX, originY)} in screen space: translate to the
     * origin, scale, translate back. Every drawing call until the matching
     * {@link #pop} keeps its absolute screen coordinates.
     */
    public static void push(GuiGraphicsExtractor ctx, int originX, int originY, float scale) {
        float s = clamp(scale);
        boolean real = Math.abs(s - 1f) >= EPSILON;
        PUSHED.set(depth, real);
        depth++;
        if (!real) return;

        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(originX, originY);
        pose.scale(s, s);
        pose.translate(-originX, -originY);
    }

    /**
     * Undoes the matching {@link #push}, if it pushed anything.
     */
    public static void pop(GuiGraphicsExtractor ctx) {
        if (depth <= 0) return;
        depth--;
        if (PUSHED.get(depth)) ctx.pose().popMatrix();
    }
}
