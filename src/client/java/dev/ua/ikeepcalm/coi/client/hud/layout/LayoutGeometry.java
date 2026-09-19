package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * The arithmetic every layout element shares: turning a dragged position back
 * into an anchor plus offsets, the snap distances that decide which anchor, and
 * the padding a bar's label and frame add around its fill.
 * <p>
 * It is separate from {@link HudElements} because it is the one part of the
 * editor with a rule rather than a list: the descriptors merely say which
 * settings fields to write, while everything about <em>where</em> a dropped
 * element lands is decided here, once, for all of them.
 */
public class LayoutGeometry {

    /**
     * How close to the screen's centre line an element has to land before it
     * snaps to a CENTER anchor / a zero offset.
     */
    public static final int CENTER_SNAP = 12;

    /**
     * The margin a LEFT- or RIGHT-anchored bar sits at with a zero offset -
     * the constant baked into {@link HudAnchor#resolve}.
     */
    public static final int EDGE_MARGIN = HudAnchor.MARGIN;

    /**
     * The label line every bar hangs above itself, plus the 1px frame.
     */
    public static final int BAR_LABEL_PAD = 11;
    public static final int BAR_FRAME_PAD = 1;

    private LayoutGeometry() {
    }

    /**
     * Turns a dragged bar position back into anchor + offsets.
     * <p>
     * {@code (pointX, pointY)} is the bar's new fill origin. The anchor follows
     * the half of the screen the bar ended up in, so a bar dragged to the
     * bottom keeps its distance from the bottom edge when the window is
     * resized — which is the whole point of having anchors.
     */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int barH, int w, int h,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        applyAnchoredMove(pointX, pointY, barW, w, h, pointY + barH / 2 < h / 2, anchor, xOffset, yOffset);
    }

    /**
     * Same, for stacks whose vertical extent is bigger than one bar and which
     * therefore decide their own top/bottom half.
     */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int w, int h, boolean top,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        int px = Mth.clamp(pointX, 0, Math.max(0, w - barW));
        int py = Mth.clamp(pointY, 0, Math.max(0, h));
        boolean center = Math.abs(px + barW / 2 - w / 2) <= CENTER_SNAP;
        // Off centre, the offset is measured from whichever edge is nearer, so
        // the element keeps its margin when the window is resized
        boolean right = !center && px + barW / 2 > w / 2;

        anchor.accept((top ? "TOP_" : "BOTTOM_") + (center ? "CENTER" : right ? "RIGHT" : "LEFT"));

        int xo;
        if (center) {
            xo = px - (w - barW) / 2;
            if (Math.abs(xo) <= CENTER_SNAP) xo = 0;
        } else if (right) {
            xo = px - (w - barW - EDGE_MARGIN);
        } else {
            xo = px - EDGE_MARGIN;
        }
        xOffset.accept(xo);
        yOffset.accept(top ? py : h - py);
    }

    /**
     * Faint dashed rectangle marking a slot that only fills up once the server
     * sends something — the remaining resource rows, the other two toasts.
     */
    public static void ghostOutline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            int x1 = Math.min(x + w, x + i + 2);
            ctx.fill(x + i, y, x1, y + 1, color);
            ctx.fill(x + i, y + h - 1, x1, y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            int y1 = Math.min(y + h, y + i + 2);
            ctx.fill(x, y + i, x + 1, y1, color);
            ctx.fill(x + w - 1, y + i, x + w, y1, color);
        }
    }
}
