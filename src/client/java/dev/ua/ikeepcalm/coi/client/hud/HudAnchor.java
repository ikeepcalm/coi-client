package dev.ua.ikeepcalm.coi.client.hud;

import net.minecraft.util.Mth;

/**
 * Where a HUD bar sits on screen. One place for the corner math so the
 * overlays and the tour's spotlight rectangles can never drift apart.
 */
public enum HudAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /**
     * The margin a LEFT/RIGHT anchored element sits at with a zero offset.
     */
    public static final int MARGIN = 10;

    /**
     * Parses a config string; anything unrecognised (or null) reads as
     * {@link #TOP_LEFT}.
     */
    public static HudAnchor parse(String value) {
        if (value == null) return TOP_LEFT;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOP_LEFT;
        }
    }

    /**
     * Whether {@link #resolve} measures y down from the top of the screen.
     * Stacked bars use this to decide which way to grow.
     */
    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }

    /**
     * Whether x is measured from the screen's centre line.
     */
    public boolean isCenter() {
        return this == TOP_CENTER || this == BOTTOM_CENTER;
    }

    /**
     * Whether x is measured in from the right edge — the mirror of the LEFT
     * anchors' 10px margin.
     */
    public boolean isRight() {
        return this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    /**
     * Resolves the bar's top-left corner.
     *
     * @param topY          y for the TOP_* anchors (a bar-specific constant or offset)
     * @param bottomYOffset distance from the screen bottom for the BOTTOM_* anchors
     * @return {@code {x, y}}, with x clamped so the bar stays on screen
     */
    public int[] resolve(int screenW, int screenH, int barW, int xOffset, int topY, int bottomYOffset) {
        int x;
        if (isCenter()) {
            x = (screenW - barW) / 2;
        } else if (isRight()) {
            x = screenW - barW - MARGIN;
        } else {
            x = MARGIN;
        }
        int y = isTop() ? topY : screenH - bottomYOffset;
        x = Mth.clamp(x + xOffset, 0, Math.max(0, screenW - barW));
        return new int[]{x, y};
    }
}
