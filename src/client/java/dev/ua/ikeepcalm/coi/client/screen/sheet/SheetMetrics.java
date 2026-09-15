package dev.ua.ikeepcalm.coi.client.screen.sheet;

/**
 * The sheet's measurements, and the one hit test its cards use.
 * <p>
 * The card geometry at the top is shared verbatim with the menu screen, which
 * is the point: the sheet is drawn as a menu document, so a number here that
 * disagrees with one there is a number that has drifted.
 */
final class SheetMetrics {

    static final int GAP = 4;
    static final int ICON = 16;
    static final int SMALL_ICON = 12;
    static final int HEADING_H = 15;
    static final int LINE = 11;
    static final int HEAD = 32;
    static final int SYMBOL = 32;
    static final int SEQ_BADGE_W = 34;

    /**
     * Hover tints ease over this. Suppressed outright under {@code epilepsyMode}
     * — see {@link #approach}, the one chokepoint that honours the setting, as
     * on the menu screen.
     */
    static final float HOVER_MS = 120;

    /**
     * A vitals row is exactly as tall as its symbol; a row carrying a second
     * muted line (madness with a floor, acting with a cooldown) grows by one.
     */
    static final int VITAL_ROW_H = SYMBOL;
    static final int VITAL_NOTE_H = 11;
    static final int VITAL_GAP = 4;
    static final int BAR_H = 6;

    static final int LEDGER_ROW_H = 14;
    static final int CHIP_GAP = 3;
    static final int NAV_CARD_H = 30;
    static final int PARA_LINE = 10;

    /**
     * Two destination columns are only worth it once each card can still hold a
     * readable description; below that one wide column beats two cramped ones.
     */
    static final int TWO_COLUMN_MIN = 320;

    /**
     * And the width at which a third fits. Each destination card still needs room for its name and
     * a line of what is behind it, so this is 3x the two-column threshold's per-card share rather
     * than simply 1.5x the threshold itself.
     */
    static final int THREE_COLUMN_MIN = 540;

    static final int SKIN_SHEET = 64;

    private SheetMetrics() {
    }

    static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
