package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CharacterPlateOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The one element whose bounds origin is also its fill origin: the card has
 * nothing hanging above it, so {@link #bounds} and {@link #moveTo} are
 * inverses with no padding to add and take back off, at any scale.
 * <p>
 * Its height is the <em>preview's</em>, not the live card's. The card grows
 * and shrinks with whatever the server is sending, but the editor draws the
 * preview, so the grab box has to match that instead.
 */
public final class CharacterPlateElement extends AbstractElement {

    public CharacterPlateElement() {
        super(ElementIds.CHARACTER_PLATE);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showCharacterPlate;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = CharacterPlateOverlay.anchor(w, h, s);
        float scale = s.characterPlateScale;
        return new int[]{pos[0], pos[1],
                HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                HudScale.size(CharacterPlateOverlay.previewHeight(), scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = s.characterPlateScale;
        LayoutGeometry.applyAnchoredMove(newX, newY,
                HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                HudScale.size(CharacterPlateOverlay.previewHeight(), scale), w, h,
                anchor -> s.characterPlateAnchor = anchor,
                x -> s.characterPlateXOffset = x,
                y -> s.characterPlateYOffset = y);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        CharacterPlateOverlay.renderPreview(ctx, w, h, s, timeMs);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.characterPlateAnchor = "TOP_LEFT";
        s.characterPlateXOffset = 0;
        s.characterPlateYOffset = CharacterPlateOverlay.DEFAULT_TOP_Y;
    }
}
