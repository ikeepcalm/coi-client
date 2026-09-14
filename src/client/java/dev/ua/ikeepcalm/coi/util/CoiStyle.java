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

    /**
     * How wide a scrolling content card may grow, and the gutter either side of it.
     * <p>
     * These live here rather than on a screen because the character sheet and the server-authored
     * menus are the same object to a player — one opens the other — and they had drifted badly
     * apart: the menus were widened to cut their scrolling and the sheet stayed on its own 440,
     * so pressing M gave a column half the width of everything it leads to.
     * <p>
     * The cap only binds at gui scale 2 and below; at scale 3 on a 1080p monitor the scaled
     * viewport is 640 wide, so {@link #CARD_SIDE_MARGIN} is what decides the card. That is why the
     * gutter is thin rather than generous.
     */
    public static final int CARD_MAX_W = 720;
    public static final int CARD_MIN_W = 220;
    public static final int CARD_SIDE_MARGIN = 28;

    private CoiStyle() {
    }

    /**
     * The width a centred content card should take on a screen this wide.
     *
     * @param compact a short window, where the gutter is halved to buy back rows
     */
    public static int cardWidth(int screenW, boolean compact) {
        int margin = compact ? 16 : CARD_SIDE_MARGIN;
        int w = Math.clamp(screenW - margin, CARD_MIN_W, CARD_MAX_W);
        // Never wider than the window itself, however small the window gets.
        return Math.clamp(screenW - 8, 120, w);
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
