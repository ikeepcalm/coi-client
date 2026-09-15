package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The 3px track-and-thumb scrollbar the mod's card screens draw <em>beside</em>
 * their card, once their rows overflow the viewport.
 * <p>
 * Rows there are a uniform height, so the thumb is a plain proportion of the
 * viewport. The ability picker's bar is deliberately not this one: its rows are
 * unequal, so its handle is walked in pixels instead.
 */
public class ScrollbarPainter {

    private static final int WIDTH = 3;
    /**
     * Small enough to show a long list, big enough to still be grabbable.
     */
    private static final int MIN_THUMB_H = 16;

    private ScrollbarPainter() {
    }

    /**
     * Draws nothing when everything already fits.
     *
     * @param trackX left edge of the 3px track
     * @param top    top of the viewport the bar reports on
     * @param bottom bottom of that viewport
     */
    public static void draw(GuiGraphicsExtractor graphics, int trackX, int top, int bottom,
                            int contentHeight, double scrollOffset, double maxScroll) {
        if (maxScroll <= 0) return;

        int viewportH = bottom - top;
        graphics.fill(trackX, top, trackX + WIDTH, bottom, CoiStyle.SCROLL_TRACK);

        int thumbH = Math.max(MIN_THUMB_H, viewportH * viewportH / contentHeight);
        int thumbY = top + (int) ((viewportH - thumbH) * (scrollOffset / maxScroll));
        graphics.fill(trackX, thumbY, trackX + WIDTH, thumbY + thumbH, CoiStyle.BORDER);
    }
}
