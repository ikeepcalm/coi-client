package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.NotificationOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The toast stack. Three slots are always reserved — one drawn with sample
 * data, the other two ghosted — so the grab box covers the stack at its
 * fullest rather than growing under the cursor when a toast arrives.
 */
public class NotificationElement extends AbstractElement {

    private static final int SLOTS = 3;
    private static final int STRIDE = NotificationOverlay.CARD_H_1_LINE + NotificationOverlay.CARD_GAP;
    private static final int GHOST = 0x60FFFFFF;

    public NotificationElement() {
        super(ElementIds.NOTIFICATIONS);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showNotifications;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        int[] pos = NotificationOverlay.anchor(w, s);
        float scale = s.notificationScale;
        return new int[]{pos[0], pos[1], HudScale.size(NotificationOverlay.CARD_W, scale),
                SLOTS * HudScale.size(STRIDE, scale) - HudScale.size(NotificationOverlay.CARD_GAP, scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        // Stored as a margin from the right edge, so the stack stays in the
        // corner when the window widens
        s.notificationXOffset = w - (newX + HudScale.size(NotificationOverlay.CARD_W, s.notificationScale));
        s.notificationYOffset = newY;
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int[] pos = NotificationOverlay.anchor(w, s);
        HudScale.push(ctx, pos[0], pos[1], s.notificationScale);
        NotificationOverlay.drawToastAt(ctx, pos[0], pos[1],
                "Advancement", "Sequence 8 reached", 0xFFFFD870, 1f);
        for (int i = 1; i < SLOTS; i++) {
            LayoutGeometry.ghostOutline(ctx, pos[0], pos[1] + i * STRIDE,
                    NotificationOverlay.CARD_W, NotificationOverlay.CARD_H_1_LINE, GHOST);
        }
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.notificationXOffset = NotificationOverlay.DEFAULT_MARGIN;
        s.notificationYOffset = NotificationOverlay.DEFAULT_MARGIN;
    }
}
