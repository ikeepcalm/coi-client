package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Beyonder HP pool. It carries its readout <em>inside</em> the box rather
 * than above it, so unlike {@link BarElement} the only padding between the fill
 * origin and the drawn bounds is the 1px frame - and since the same scaled pad
 * is added here and taken back off in {@link #moveTo}, the two stay exact
 * inverses at any scale.
 * <p>
 * <b>The geometry is style-independent.</b> All four {@link HealthStyle}s
 * occupy the same {@code BAR_WIDTH x BAR_HEIGHT} box, so nothing here has to
 * ask which one is on; only {@link #resetPosition} does, because the styles
 * ship at different heights - see {@link HealthStyle#defaultYOffset()}.
 */
public final class BeyonderHealthElement extends AbstractElement {

    public BeyonderHealthElement() {
        super(ElementIds.BEYONDER_HEALTH);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showBeyonderHealth;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = BeyonderHealthOverlay.anchor(w, h, s);
        float scale = s.beyonderHealthScale;
        int pad = HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale);
        return new int[]{pos[0] - pad, pos[1] - pad,
                HudScale.size(BeyonderHealthOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale),
                HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD * 2, scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = s.beyonderHealthScale;
        int pad = HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale);
        LayoutGeometry.applyAnchoredMove(newX + pad, newY + pad,
                HudScale.size(BeyonderHealthOverlay.BAR_WIDTH, scale),
                HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT, scale), w, h,
                anchor -> s.beyonderHealthAnchor = anchor,
                x -> s.beyonderHealthXOffset = x,
                y -> s.beyonderHealthYOffset = y);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        BeyonderHealthOverlay.renderPreview(ctx, w, h, s);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        BeyonderHealthOverlay.resetPosition(s, HealthStyle.parse(s.beyonderHealthStyle));
    }
}
