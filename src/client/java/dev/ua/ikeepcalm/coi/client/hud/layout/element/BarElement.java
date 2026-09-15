package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.MadnessOverlay;

/**
 * A 182-wide bar with its label 10px above it: madness, acting, resources.
 * Bounds cover the frame and that label line.
 * <p>
 * Everything the bar occupies is measured through
 * {@link HudScale#size}, so {@link #bounds} and {@link #moveTo} stay exact
 * inverses at any {@link #scale}: the padding one adds is the padding the
 * other takes back off.
 */
public abstract class BarElement extends AbstractElement {

    private final int barHeight;

    protected BarElement(String id, int barHeight) {
        super(id);
        this.barHeight = barHeight;
    }

    /**
     * Fill origin, straight from the overlay's own anchor math. Already
     * resolved against the scaled bar width.
     */
    protected abstract int[] fillOrigin(int w, int h, HudConfig.HudSettings s);

    protected abstract void writeAnchor(HudConfig.HudSettings s, String anchor);

    protected abstract void writeOffsets(HudConfig.HudSettings s, int x, int y);

    /**
     * This bar's own per-element scale setting.
     */
    protected abstract float scale(HudConfig.HudSettings s);

    protected final int barWidth() {
        return MadnessOverlay.BAR_WIDTH;
    }

    protected final int boundsHeight() {
        return LayoutGeometry.BAR_LABEL_PAD + barHeight + LayoutGeometry.BAR_FRAME_PAD;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = fillOrigin(w, h, s);
        float scale = scale(s);
        return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                pos[1] - HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale),
                HudScale.size(barWidth() + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                HudScale.size(boundsHeight(), scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = scale(s);
        LayoutGeometry.applyAnchoredMove(newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                newY + HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale),
                HudScale.size(barWidth(), scale), HudScale.size(barHeight, scale), w, h,
                anchor -> writeAnchor(s, anchor),
                x -> writeOffsets(s, x, Integer.MIN_VALUE),
                y -> writeOffsets(s, Integer.MIN_VALUE, y));
    }
}
