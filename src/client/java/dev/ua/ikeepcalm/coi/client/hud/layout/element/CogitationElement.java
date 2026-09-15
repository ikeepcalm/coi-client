package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CogitationOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The cogitation prompt card, offset from the screen's centre in both axes —
 * it stands in for a vanilla title, so it belongs to the middle of the screen
 * rather than to an edge.
 */
public final class CogitationElement extends AbstractElement {

    public CogitationElement() {
        super(ElementIds.COGITATION);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showCogitationOverlay;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = CogitationOverlay.anchor(w, h, s);
        float scale = s.cogitationScale;
        return new int[]{pos[0], pos[1],
                HudScale.size(CogitationOverlay.CARD_W, scale),
                HudScale.size(CogitationOverlay.CARD_H, scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        s.cogitationXOffset = newX - (w / 2 - HudScale.size(CogitationOverlay.CARD_W, s.cogitationScale) / 2);
        s.cogitationYOffset = newY - h / 2;
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = CogitationOverlay.anchor(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.cogitationScale);
        CogitationOverlay.drawCardAt(ctx, pos[0], pos[1], "Turn around", 3, 0.6, false);
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.cogitationXOffset = 0;
        s.cogitationYOffset = CogitationOverlay.DEFAULT_Y_OFFSET;
    }
}
