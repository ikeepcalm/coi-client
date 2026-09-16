package dev.ua.ikeepcalm.coi.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared palette and card chrome for COI screens (originating from TourScreen's
 * dark/gold look). Every custom screen draws from here so the mod reads as one
 * interface.
 */
public final class CoiStyle {

    public static final int CARD_BG = 0xF0121216;
    public static final int TAB_BG_UNSELECTED = 0xC0121216;
    public static final int BORDER = 0xFF3A3A46;
    public static final int ACCENT = 0xFFFFD870;
    public static final int TEXT_BODY = 0xFFE0E0E0;
    public static final int TEXT_MUTED = 0xFF808088;
    public static final int ROW_HOVER = 0x28FFFFFF;
    public static final int BACKDROP = 0xA8000000;
    public static final int INACTIVE = 0xFF55555C;
    public static final int SCROLL_TRACK = 0x30FFFFFF;

    private CoiStyle() {
    }

    /**
     * Dark card with border and a 1px accent rule along the top edge.
     */
    public static void drawCard(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, CARD_BG);
        graphics.outline(x, y, w, h, BORDER);
        graphics.fill(x, y, x + w, y + 1, ACCENT);
    }
}
