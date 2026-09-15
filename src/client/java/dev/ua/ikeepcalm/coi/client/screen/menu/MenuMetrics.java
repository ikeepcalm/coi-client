package dev.ua.ikeepcalm.coi.client.screen.menu;

/**
 * The geometry a menu document is drawn to, and the one hit test everything
 * uses.
 * <p>
 * These numbers are shared between {@link MenuScreen}, the card chrome and
 * every {@link MenuPart}, so they live in one place rather than being repeated
 * by whoever draws with them. Sizes that belong to a single drawing — a
 * component's own inner padding, a one-off easing coefficient — stay where they
 * are used.
 */
final class MenuMetrics {

    /** Card geometry is shared with the character sheet — see {@code CoiStyle.cardWidth}. */
    static final int ICON = 16;
    static final int SMALL_ICON = 12;
    static final int GAP = 4;
    static final int MIN_TILE = 18;
    static final int FIELD_H = 18;

    /**
     * A panel cell narrower than this cannot hold a title and a number, so a
     * document asking for three columns on a narrow card gets two.
     */
    static final int MIN_PANEL_W = 72;

    static final int HERO_ICON = 32;
    static final int HERO_RING = 28;
    static final int STAT_RING = 24;

    /**
     * Where a step rail's markers sit, and where its text starts.
     */
    static final int RAIL_X = 5;
    static final int RAIL_TEXT_X = 18;

    /**
     * Hover tints ease over this; gauges tween toward their target over the
     * other. Both are suppressed outright under {@code epilepsyMode} — see
     * {@link MenuContext#approach}.
     */
    static final float HOVER_MS = 120;
    static final float GAUGE_MS = 200;

    private MenuMetrics() {
    }

    static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
