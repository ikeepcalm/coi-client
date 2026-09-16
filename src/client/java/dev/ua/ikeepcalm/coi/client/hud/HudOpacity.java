package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Per-element HUD transparency — {@link HudScale}'s other half.
 * <p>
 * The two are the same kind of thing and are meant to be read together: both
 * are <em>ambient render state</em> pushed around one element's draw and popped
 * after it, both are render-thread only, and both exist so the draw code inside
 * the push needs no new argument. They differ in what they decide —
 * {@code HudScale} transforms <em>where</em> a pixel lands, this one decides
 * <em>how opaque</em> it is.
 * <p>
 * The asymmetry is forced by the graphics API rather than chosen: {@code
 * ctx.pose()} is a 2D matrix stack with no colour channel, so there is no
 * pose-level alpha to push. Opacity therefore has to be applied per colour, and
 * a caller inside the push obtains the colour it should actually draw with from
 * {@link #apply(int)}. That is the whole contract, and it is also the whole
 * hazard: a single colour that skips {@code apply} is a piece of the element
 * that stays solid while the rest fades, which reads as a bug rather than as a
 * setting.
 * <p>
 * The floor is {@link HudConfig#MIN_ELEMENT_OPACITY} and not zero on purpose.
 * Below roughly {@code alpha < 4/255} the font renderer stops honouring the
 * alpha channel at all, so a slider that reached zero would make an element's
 * numbers behave differently from the card they sit on — and an element the
 * player can fade to nothing is indistinguishable from one they switched off,
 * which is what the show/hide checkbox is for.
 */
public class HudOpacity {

    /**
     * Factors this close to 1 are treated as "fully opaque" and skip the work
     * entirely.
     */
    private static final float EPSILON = 1e-4f;

    /**
     * Which of the currently open {@link #push} levels actually changed the
     * factor, so {@link #pop} only restores the ones that did.
     */
    private static final BitSet PUSHED = new BitSet();
    /**
     * The factor each real push replaced, indexed by its depth.
     */
    private static float[] previous = new float[8];
    private static int depth = 0;

    /**
     * The factor every {@link #apply} multiplies by; 1 outside any push.
     */
    private static float factor = 1f;

    private HudOpacity() {
    }

    /**
     * Clamps a raw setting into the supported range.
     */
    public static float clamp(float opacity) {
        return Mth.clamp(opacity, HudConfig.MIN_ELEMENT_OPACITY, HudConfig.MAX_ELEMENT_OPACITY);
    }

    /**
     * Makes {@code opacity} ambient until the matching {@link #pop}. Nested
     * pushes <em>multiply</em>: an element drawn at 0.5 inside a container at
     * 0.5 is a quarter opaque, which is the only reading that lets a nested
     * piece never come out more solid than what encloses it.
     */
    public static void push(float opacity) {
        float o = clamp(opacity);
        boolean real = Math.abs(o - 1f) >= EPSILON;
        PUSHED.set(depth, real);
        if (real) {
            if (depth >= previous.length) previous = Arrays.copyOf(previous, depth * 2);
            previous[depth] = factor;
            factor *= o;
        }
        depth++;
    }

    /**
     * Undoes the matching {@link #push}, if it changed anything.
     */
    public static void pop() {
        if (depth <= 0) return;
        depth--;
        if (PUSHED.get(depth)) factor = previous[depth];
    }

    /**
     * The raw ambient factor, for the few callers that hand an alpha to a
     * helper of their own rather than a colour — {@code CoiBar}'s faded
     * {@code frame} and {@code fill} overloads take one.
     */
    public static float current() {
        return factor;
    }

    /**
     * The colour a draw call inside a push should actually use: the argb's own
     * alpha multiplied by the ambient factor, so a colour that was already
     * translucent stays proportionally so instead of being flattened to one
     * level.
     * <p>
     * The arithmetic is spelled out here rather than delegated to
     * {@code CoiBar.withAlpha}, which does the identical multiply, because
     * {@code CoiBar} lives in {@code hud.render} and that package already
     * depends on this one ({@code PlateSymbols} on {@link HudScale}). Reusing
     * it would close a package cycle for four lines of arithmetic.
     */
    public static int apply(int argb) {
        if (factor >= 1f - EPSILON) return argb;
        return CoiBar.withAlpha(argb, factor);
    }
}
