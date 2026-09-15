package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ActingOverlay;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

/**
 * The acting bar. Absent from the editor entirely while the character plate is
 * on — the plate carries the same number as its mask gauge.
 */
public final class ActingElement extends BarElement {

    /**
     * Sample progress for the preview, drawn in the Fool's colour.
     */
    private static final double PREVIEW_PERCENT = 63.0;
    private static final String PREVIEW_PATHWAY = "fool";

    /**
     * Where a TOP-anchored bar sits out of the box, below the madness bar.
     */
    private static final int DEFAULT_TOP_Y = 80;

    public ActingElement() {
        super(ElementIds.ACTING, ActingOverlay.BAR_HEIGHT);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showActingBar && !s.showCharacterPlate;
    }

    @Override
    protected int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
        return ActingOverlay.anchor(w, h, s);
    }

    @Override
    protected void writeAnchor(HudConfig.HudSettings s, String anchor) {
        s.actingAnchor = anchor;
    }

    @Override
    protected void writeOffsets(HudConfig.HudSettings s, int x, int y) {
        if (x != Integer.MIN_VALUE) s.actingXOffset = x;
        if (y != Integer.MIN_VALUE) s.actingYOffset = y;
    }

    @Override
    protected float scale(HudConfig.HudSettings s) {
        return s.actingScale;
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = fillOrigin(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.actingScale);
        ActingOverlay.drawBarAt(ctx, pos[0], pos[1], PREVIEW_PERCENT,
                Pathways.pathwayRgb(PREVIEW_PATHWAY),
                I18n.get("hud.coi.acting_label", String.format(Locale.ROOT, "%.1f", PREVIEW_PERCENT)));
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.actingAnchor = "TOP_LEFT";
        s.actingXOffset = 0;
        s.actingYOffset = DEFAULT_TOP_Y;
    }
}
