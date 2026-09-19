package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ResourceOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The stack of server-pushed resource meters. Only the first two rows can be
 * drawn with sample data — the rest of the reserved height is ghosted, since
 * how many bars actually arrive is the server's business. Absent from the
 * editor entirely while the character plate is on, which lists the same meters
 * as its reserve rows.
 */
public class ResourceElement extends AbstractElement {

    private static final int GHOST = 0x60FFFFFF;

    /**
     * Where a TOP-anchored stack sits out of the box, below the acting bar.
     */
    private static final int DEFAULT_TOP_Y = 100;

    public ResourceElement() {
        super(ElementIds.RESOURCES);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showResourceBars && !s.showCharacterPlate;
    }

    private static int rows(HudConfig.HudSettings s) {
        return Math.max(1, s.resourceMaxBars);
    }

    /**
     * Gap between two stacked bars once the scale is applied.
     */
    private static int stride(HudConfig.HudSettings s) {
        return HudScale.size(ResourceOverlay.STRIDE, s.resourceScale);
    }

    private static int stackHeight(HudConfig.HudSettings s) {
        return (rows(s) - 1) * stride(s) + HudScale.size(
                LayoutGeometry.BAR_LABEL_PAD + ResourceOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD,
                s.resourceScale);
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = ResourceOverlay.anchor(w, h, s);
        float scale = s.resourceScale;
        int grow = (rows(s) - 1) * stride(s);
        // BOTTOM anchors stack upward, so the first bar is the stack's floor
        int top = pos[1] - HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale)
                - (HudAnchor.parse(s.resourceAnchor).isTop() ? 0 : grow);
        return new int[]{pos[0] - HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale), top,
                HudScale.size(ResourceOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2, scale), stackHeight(s)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = s.resourceScale;
        boolean top = newY + stackHeight(s) / 2 < h / 2;
        int grow = (rows(s) - 1) * stride(s);
        int fillY = newY + HudScale.size(LayoutGeometry.BAR_LABEL_PAD, scale) + (top ? 0 : grow);
        LayoutGeometry.applyAnchoredMove(newX + HudScale.size(LayoutGeometry.BAR_FRAME_PAD, scale), fillY,
                HudScale.size(ResourceOverlay.BAR_WIDTH, scale), w, h, top,
                anchor -> s.resourceAnchor = anchor,
                x -> s.resourceXOffset = x,
                y -> s.resourceYOffset = y);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = ResourceOverlay.anchor(w, h, s);
        // Inside the push everything keeps its unscaled geometry
        int step = HudAnchor.parse(s.resourceAnchor).isTop()
                ? ResourceOverlay.STRIDE : -ResourceOverlay.STRIDE;

        HudScale.push(ctx, pos[0], pos[1], s.resourceScale);
        ResourceOverlay.drawBarAt(ctx, pos[0], pos[1], "Rage Meter", 62.5, 100, 0xFF5555, true);
        if (rows(s) > 1) {
            ResourceOverlay.drawBarAt(ctx, pos[0], pos[1] + step, "Seeds", 3, 5, 0x55FF7F, false);
        }
        // The rest of the stack only appears when the server sends more
        for (int i = 2; i < rows(s); i++) {
            LayoutGeometry.ghostOutline(ctx, pos[0] - LayoutGeometry.BAR_FRAME_PAD,
                    pos[1] + step * i - LayoutGeometry.BAR_FRAME_PAD,
                    ResourceOverlay.BAR_WIDTH + LayoutGeometry.BAR_FRAME_PAD * 2,
                    ResourceOverlay.BAR_HEIGHT + LayoutGeometry.BAR_FRAME_PAD * 2, GHOST);
        }
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.resourceAnchor = "TOP_LEFT";
        s.resourceXOffset = 0;
        s.resourceYOffset = DEFAULT_TOP_Y;
    }
}
