package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.TargetHealthOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The hit target's HP bar under the crosshair. Positioned as a signed offset
 * from the screen's centre in both axes, since it belongs to the crosshair
 * rather than to an edge.
 */
public final class TargetHealthElement extends AbstractElement {

    /**
     * Name line above the bar, readout below it.
     */
    private static final int ABOVE = 11;
    private static final int BELOW = 12;

    public TargetHealthElement() {
        super(ElementIds.TARGET_HEALTH);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showTargetHealth;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = TargetHealthOverlay.anchor(w, h, s);
        float scale = s.targetHealthScale;
        return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale),
                pos[1] - HudScale.size(ABOVE, scale),
                HudScale.size(TargetHealthOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                HudScale.size(ABOVE + TargetHealthOverlay.BAR_HEIGHT + BELOW, scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = s.targetHealthScale;
        s.targetHealthXOffset = newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale)
                - (w / 2 - HudScale.size(TargetHealthOverlay.BAR_WIDTH, scale) / 2);
        s.targetHealthYOffset = newY + HudScale.size(ABOVE, scale) - h / 2;
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = TargetHealthOverlay.anchor(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.targetHealthScale);
        TargetHealthOverlay.drawBarAt(ctx, pos[0], pos[1], "Steve", 0.68f, 0.85f, 13.6, 20, true);
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.targetHealthXOffset = 0;
        s.targetHealthYOffset = TargetHealthOverlay.DEFAULT_Y_OFFSET;
    }
}
