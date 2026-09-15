package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.overlay.MadnessOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The madness bar. Absent from the editor entirely while the character plate
 * is on — the plate carries madness as its sanity gauge.
 */
public final class MadnessElement extends BarElement {

    /**
     * Sample values for the preview: a clearly-filled bar with a visible
     * permanent-madness floor, so both parts of the bar can be judged.
     */
    private static final double PREVIEW_MADNESS = 42;
    private static final double PREVIEW_PERMANENT = 10;

    public MadnessElement() {
        super(ElementIds.MADNESS, MadnessOverlay.BAR_HEIGHT);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showMadnessBar && !s.showCharacterPlate;
    }

    @Override
    protected int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
        return MadnessOverlay.anchor(w, h, s);
    }

    @Override
    protected void writeAnchor(HudConfig.HudSettings s, String anchor) {
        s.madnessAnchor = anchor;
    }

    @Override
    protected void writeOffsets(HudConfig.HudSettings s, int x, int y) {
        if (x != Integer.MIN_VALUE) s.madnessXOffset = x;
        if (y != Integer.MIN_VALUE) s.madnessYOffset = y;
    }

    @Override
    protected float scale(HudConfig.HudSettings s) {
        return s.madnessScale;
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = fillOrigin(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.madnessScale);
        MadnessOverlay.drawBarAt(ctx, pos[0], pos[1], PREVIEW_MADNESS, PREVIEW_PERMANENT, timeMs);
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.madnessAnchor = "TOP_LEFT";
        s.madnessXOffset = 0;
        s.madnessYOffset = MadnessOverlay.DEFAULT_TOP_Y;
    }
}
