package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.SpiritualityOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The one element whose geometry isn't a plain 182×6 rectangle: its frame
 * sprite overhangs the fill on every side, so the offsets between the fill
 * origin and the drawn bounds come from the overlay itself.
 */
public class SpiritualityElement extends AbstractElement {

    /**
     * Where a TOP-anchored bar sits out of the box, between the madness and
     * acting bars.
     */
    private static final int DEFAULT_TOP_Y = 50;

    public SpiritualityElement() {
        super(ElementIds.SPIRITUALITY);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showSpiritualityBar;
    }

    private static int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
        return SpiritualityOverlay.anchor(w, h, s);
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        return SpiritualityOverlay.bounds(w, h, s);
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        // Whatever the frame's overhang is today, it is the gap between the
        // fill origin and the bounds origin
        int[] fill = fillOrigin(w, h, s);
        int[] box = bounds(w, h, s);
        LayoutGeometry.applyAnchoredMove(newX + (fill[0] - box[0]), newY + (fill[1] - box[1]),
                HudScale.size(SpiritualityOverlay.BAR_WIDTH, s.spiritualityScale),
                HudScale.size(SpiritualityOverlay.BAR_HEIGHT, s.spiritualityScale), w, h,
                anchor -> s.spiritualityAnchor = anchor,
                x -> s.spiritualityXOffset = x,
                y -> s.spiritualityYOffset = y);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        SpiritualityOverlay.renderPreview(ctx, w, h, s, timeMs);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.spiritualityAnchor = "TOP_LEFT";
        s.spiritualityXOffset = 0;
        s.spiritualityYOffset = DEFAULT_TOP_Y;
    }
}
