package dev.ua.ikeepcalm.coi.client.hud.render;

import java.util.Locale;

/**
 * Which shape the Beyonder HP pool takes on the HUD.
 * <p>
 * The element exists because ten hearts cannot say 1,750; the styles exist
 * because the first answer to that — replacing the hearts outright — took the
 * vanilla absorption hearts with it and was not to everyone's taste.
 * {@link #HEARTS} is therefore the default: it is the <em>least</em> intrusive
 * of the four, leaving vanilla's own row exactly where it was and adding only
 * the numbers the hearts cannot carry.
 * <p>
 * <b>All four occupy the same box.</b> {@code BAR_WIDTH x BAR_HEIGHT} is the
 * footprint whichever style is picked, so switching style never moves the
 * element and {@code BeyonderHealthElement}'s {@code bounds} / {@code moveTo}
 * stay exact inverses without ever asking which style is on.
 */
public enum HealthStyle {

    /**
     * The vanilla hearts draw normally — absorption hearts included, which is
     * the whole point — and the mod adds only the pool readout beside them.
     */
    HEARTS(59),
    /**
     * The filled bar that replaced the hearts in 1.2.0, unchanged.
     */
    BAR(39),
    /**
     * The same bar under carved chrome in the mod's dark/gold language.
     */
    ORNATE(39),
    /**
     * The pool as ten notched segments, draining the way a heart row does.
     */
    PIPS(39);

    /**
     * What an unparseable {@code beyonderHealthStyle} reads as, and what a
     * fresh config is written with.
     */
    public static final HealthStyle DEFAULT = HEARTS;

    private final int defaultYOffset;

    HealthStyle(int defaultYOffset) {
        this.defaultYOffset = defaultYOffset;
    }

    /**
     * Parses a config string; anything unrecognised (or null) reads as
     * {@link #DEFAULT}, mirroring
     * {@link dev.ua.ikeepcalm.coi.client.hud.HudAnchor#parse}. A style this
     * build does not know about is a config from a newer build, and falling
     * back to the least intrusive style is the one answer that cannot look
     * broken.
     */
    public static HealthStyle parse(String value) {
        if (value == null) return DEFAULT;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }

    /**
     * The next style in declaration order, wrapping — what the settings
     * screen's cycle button steps through.
     */
    public HealthStyle next() {
        HealthStyle[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /**
     * The lang key naming this style on the settings row.
     */
    public String labelKey() {
        return "screen.coi.health_style_" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * Distance up from the screen bottom this style's box sits at out of the
     * box.
     * <p>
     * The three replacing styles take the hearts' own row (39), which is free
     * precisely because they took the hearts off it. {@link #HEARTS} cannot:
     * the hearts are still there, and so is vanilla's armour bar one row above
     * them at {@code h - 49}, so the first free row on that side of the hotbar
     * is {@code h - 59} - the obvious "one line up" would have dropped the
     * readout onto the armour of every armoured player. 59 is only claimed when
     * absorption is active, which pushes vanilla's own left-hand stack up a row
     * (see the known gap in CLAUDE.md), and the layout editor moves it in one
     * drag.
     */
    public int defaultYOffset() {
        return defaultYOffset;
    }

    /**
     * Whether the vanilla hearts still draw underneath this style — true only
     * for {@link #HEARTS}, and the reason the health element's replacement hook
     * needs three answers rather than a boolean.
     */
    public boolean drawsOverVanillaHearts() {
        return this == HEARTS;
    }
}
