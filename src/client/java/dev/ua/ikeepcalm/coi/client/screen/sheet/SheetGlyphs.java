package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.PlateSymbols;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The character sheet's drawn symbols.
 * <p>
 * The mod ships bitmap art for exactly two gauge symbols — the brain and the
 * mask of {@code PlateSymbols} — and {@code CoiIcons} is explicit that 16px is
 * the floor for that kind of artwork. The sheet needs a mark for every vital
 * and every navigation card, at 16px and at 32px, so these are <em>drawn</em>
 * instead: an 8×8 grid blown up by whole pixels stays sharp at any size and
 * costs no PNG.
 * <p>
 * Every glyph is authored on the same 8×8 grid so they read as one set, and the
 * renderer centres the scaled block inside the box the caller asked for — a
 * 16px box gives 2px cells, a 32px box 4px ones, both exact.
 */
public final class SheetGlyphs {

    /**
     * The bundled 16px glyphs this screen names, one per section heading and one
     * per chip. They are the same {@code CoiIcons.GLYPHS} names a server-authored
     * document uses, so the sheet's headings are marked exactly as a menu's are.
     */
    public static final String GLYPH_HEALTH = "health";
    public static final String GLYPH_GROWTH = "growth";
    public static final String GLYPH_RESTORE = "restore";
    public static final String GLYPH_WARD = "ward";
    public static final String GLYPH_DIVINATION = "divination";
    public static final String GLYPH_DEFENSE = "defense";
    public static final String GLYPH_SOUL = "soul";
    public static final String GLYPH_POWER = "power";
    public static final String GLYPH_RESIST = "resist";
    public static final String GLYPH_AUTHORITY = "authority";
    public static final String GLYPH_COOLDOWN = "cooldown";

    private static final int GRID = 8;

    /**
     * The band {@link #drawFilled} is currently painting, in absolute screen
     * pixels. Render-thread only, in the same spirit as {@code HudScale}'s
     * static pose stack: it lets the glyph tables stay plain data instead of
     * every painter learning about fills.
     */
    private static int bandTop = Integer.MIN_VALUE;
    private static int bandBottom = Integer.MAX_VALUE;

    // --- Vitals (drawn at 32px, filled bottom-up) ---

    public static final String[] HEART = {
            ".XX..XX.",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "........"
    };

    public static final String[] FLASK = {
            "..XXXX..",
            "...XX...",
            "...XX...",
            "..XXXX..",
            ".XX..XX.",
            "XX....XX",
            "XX....XX",
            ".XXXXXX."
    };

    public static final String[] HOURGLASS = {
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX"
    };

    // --- Navigation cards and chips (drawn at 16px) ---

    public static final String[] CHURCH = {
            "...XX...",
            "...XX...",
            ".XXXXXX.",
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX",
            "XX.XX.XX"
    };

    public static final String[] SPARK = {
            "...XX...",
            "...XX...",
            "..XXXX..",
            "XXXXXXXX",
            "XXXXXXXX",
            "..XXXX..",
            "...XX...",
            "...XX..."
    };

    public static final String[] BEAST = {
            "XX....XX",
            "XX....XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "X.XXXX.X",
            "XXXXXXXX",
            ".XXXXXX.",
            "..XXXX.."
    };

    public static final String[] GEM = {
            ".XXXXXX.",
            "XXXXXXXX",
            "XXXXXXXX",
            ".XXXXXX.",
            ".XXXXXX.",
            "..XXXX..",
            "..XXXX..",
            "...XX..."
    };

    public static final String[] CROWN = {
            "........",
            "X..XX..X",
            "X..XX..X",
            "XX.XX.XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "........"
    };

    public static final String[] PIN = {
            "..XXXX..",
            ".XXXXXX.",
            "XX....XX",
            "XX....XX",
            ".XXXXXX.",
            "..XXXX..",
            "...XX...",
            "...XX..."
    };

    public static final String[] THRONE = {
            "X......X",
            "XX....XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX....XX",
            "XX....XX"
    };

    public static final String[] BLOCKS = {
            "........",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX.XX.XX",
            "XXXXXXXX",
            "XXXXXXXX",
            "XX.XX.XX",
            "........"
    };

    public static final String[] WARNING = {
            "...XX...",
            "..XXXX..",
            "..X..X..",
            ".XX..XX.",
            ".X.XX.X.",
            "XX.XX.XX",
            "XX....XX",
            "XXXXXXXX"
    };

    public static final String[] LOCK = {
            "..XXXX..",
            ".XX..XX.",
            ".XX..XX.",
            "XXXXXXXX",
            "XXX..XXX",
            "XXX..XXX",
            "XXXXXXXX",
            "........"
    };

    private SheetGlyphs() {
    }

    /**
     * Draws a glyph in one colour, centred in a {@code size}×{@code size} box.
     */
    public static void draw(GuiGraphicsExtractor ctx, String[] glyph, int x, int y, int size, int argb) {
        int cell = Math.max(1, size / GRID);
        int span = cell * GRID;
        int originX = x + (size - span) / 2;
        int originY = y + (size - span) / 2;
        for (int row = 0; row < GRID; row++) {
            String line = glyph[row];
            for (int col = 0; col < GRID; col++) {
                if (line.charAt(col) != 'X') continue;
                rect(ctx, originX + col * cell, originY + row * cell, cell, cell, argb);
            }
        }
    }

    /**
     * A vital's symbol: the whole glyph drained, then its bottom
     * {@code fill} of the box repainted in the live colour, so the mark itself
     * carries the reading and the bar beside it only confirms it.
     */
    public static void drawFilled(GuiGraphicsExtractor ctx, String[] glyph, int x, int y, int size,
                                  float fill, int emptyArgb, int fullArgb) {
        draw(ctx, glyph, x, y, size, emptyArgb);
        int filled = Math.round(size * Math.clamp(fill, 0f, 1f));
        if (filled <= 0) return;
        bandTop = y + size - filled;
        bandBottom = y + size;
        draw(ctx, glyph, x, y, size, fullArgb);
        bandTop = Integer.MIN_VALUE;
        bandBottom = Integer.MAX_VALUE;
    }

    /**
     * One cell, clipped to the fill band rather than scissored — a scissor
     * resolves in window pixels and would cut the wrong rows under a pose
     * transform, exactly as {@code PlateSymbols} notes for its sub-rect blits.
     */
    private static void rect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int argb) {
        int top = Math.max(y, bandTop);
        int bottom = Math.min(y + h, bandBottom);
        if (bottom > top) ctx.fill(x, top, x + w, bottom, argb);
    }

}
