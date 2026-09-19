package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.CHIP_GAP;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.PARA_LINE;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.accent;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The sheet's pills, and the lines printed under them.
 * <p>
 * Acting and conditions both say what is true of the character in the same
 * shape — a row of chips, then a paragraph each — so the wrapping, the drawing
 * and the explanations live here once rather than in both sections.
 */
final class SheetChips {

    /**
     * A compact fact plus the plain-language line printed under the chip row.
     * Drawn by {@link MenuTheme#chip}, so the label is muted and the value
     * carries the colour — the same pair a menu's {@code chips} row makes.
     */
    record Chip(String label, String value, int rgb, String glyph, Component explanation) {
        MenuComponent.Chip model() {
            return new MenuComponent.Chip(label, value, rgb & 0xFFFFFF,
                    new MenuIcon(MenuIcon.Kind.GLYPH, glyph), "");
        }
    }

    private SheetChips() {
    }

    /**
     * Chips wrapped into rows once, so measuring and drawing can never disagree
     * about how tall the block is.
     */
    static List<List<Chip>> layout(SheetContext ctx, List<Chip> chips, int w) {
        List<List<Chip>> rows = new ArrayList<>();
        List<Chip> row = new ArrayList<>();
        int used = 0;
        for (Chip chip : chips) {
            int width = MenuTheme.chipWidth(ctx.font(), chip.model());
            if (!row.isEmpty() && used + CHIP_GAP + width > w) {
                rows.add(row);
                row = new ArrayList<>();
                used = 0;
            }
            used += (row.isEmpty() ? 0 : CHIP_GAP) + width;
            row.add(chip);
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }

    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y,
                    List<List<Chip>> rows) {
        if (rows.isEmpty()) return y;
        y += 4;
        for (List<Chip> row : rows) {
            int cx = x;
            for (Chip chip : row) {
                MenuComponent.Chip model = chip.model();
                if (graphics != null) MenuTheme.chip(graphics, ctx.font(), model, cx, y, accent(), 0f);
                cx += MenuTheme.chipWidth(ctx.font(), model) + CHIP_GAP;
            }
            y += MenuTheme.CHIP_H + CHIP_GAP;
        }
        return y - CHIP_GAP;
    }

    /**
     * The chips say <em>that</em> something is happening; these lines say what
     * it does. They are printed rather than hidden behind a hover, because a
     * throttle the player cannot see is a throttle they will report as a bug.
     */
    static int explanations(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                            List<Chip> chips) {
        if (chips.isEmpty()) return y;
        y += 4;
        for (Chip chip : chips) {
            for (FormattedCharSequence line : ctx.font().split(chip.explanation(), w)) {
                if (graphics != null) graphics.text(ctx.font(), line, x, y, CoiStyle.TEXT_MUTED);
                y += PARA_LINE;
            }
        }
        return y;
    }
}
