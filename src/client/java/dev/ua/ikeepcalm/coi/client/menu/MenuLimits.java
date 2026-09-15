package dev.ua.ikeepcalm.coi.client.menu;

/**
 * How much of a menu document this client will read.
 * <p>
 * These are not a security boundary — the payload is already capped at 1 MiB
 * by {@code MenuPayload} — they keep a runaway server-side loop from laying out
 * a card a hundred thousand pixels tall, and they keep one absurd string from
 * pushing everything else off the screen. Exceeding a cap always truncates; it
 * never fails the document.
 */
final class MenuLimits {

    private MenuLimits() {
    }

    static final int MAX_SECTIONS = 32;
    static final int MAX_COMPONENTS = 96;
    static final int MAX_ROWS = 400;
    static final int MAX_KV_ROWS = 64;
    static final int MAX_CHECKS = 64;
    static final int MAX_BUTTONS = 24;
    static final int MAX_TOOLTIP_LINES = 10;

    /**
     * Vocabulary v2 caps. A chip row and a step rail are read top to bottom, so
     * the ceilings are low on purpose: past a dozen the component is the wrong
     * one and the server should have sent a list.
     */
    static final int MAX_CHIPS = 12;
    static final int MAX_STEPS = 12;
    static final int MAX_PANEL_CELLS = 12;
    static final int MAX_DETAILS_BLOCKS = 8;
    static final int MAX_HERO_CHIPS = 4;

    static final int MAX_TITLE = 128;
    static final int MAX_LABEL = 160;
    static final int MAX_TEXT = 2000;
    static final int MAX_ID = 96;

    /**
     * Short free-text fields: a badge, a delta, an on/off word.
     */
    static final int MAX_BADGE = 32;

    /**
     * An enum word on the wire ({@code "primary"}, {@code "timeline"}, …).
     */
    static final int MAX_WORD = 16;

    /**
     * A component {@code type} name.
     */
    static final int MAX_TYPE = 24;

    /**
     * {@code "#RRGGBB"} at its longest.
     */
    static final int MAX_COLOR = 8;
}
