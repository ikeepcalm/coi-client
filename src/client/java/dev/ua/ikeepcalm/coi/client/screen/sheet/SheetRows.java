package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.BAR_H;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.SYMBOL;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.VITAL_ROW_H;

import dev.ua.ikeepcalm.coi.client.hud.render.PlateSymbols;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuGauges;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * One row of the sheet's stacked gauges, and the two brushes it is painted
 * with.
 * <p>
 * Health, spirituality, madness, tiredness and acting are the same row —
 * symbol, number, bar, a line of plain language — so there is one painter, and
 * each caller supplies only what differs: its mark and its bar.
 */
final class SheetRows {

    /**
     * One layer of a vitals row, so a row's bar can be the ordinary gauge or
     * madness's three stacked segments without two copies of the row painter.
     */
    @FunctionalInterface
    interface BarPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int w);
    }

    /**
     * A vitals row's mark — a drawn glyph for most, a {@link PlateSymbols}
     * sprite where the plate already owns the artwork.
     */
    @FunctionalInterface
    interface SymbolPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y);
    }

    private SheetRows() {
    }

    /**
     * Symbol, then a column carrying the number first and the bar second — the
     * reading a player actually wants is "how much", not "how full".
     */
    static int vital(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                            SymbolPainter symbol, String label, String value, int valueRgb,
                            BarPainter bar, Component sub, int subRgb, Component note) {
        // A null extractor measures the same wrapped row without submitting paint.
        if (graphics != null) symbol.draw(graphics, x, y);
        int textX = x + SYMBOL + 8;
        int textW = x + w - textX;

        int valueW = ctx.font().width(value);
        boolean stacked = ctx.font().width(label) + valueW + 8 > textW;
        int valueY = y + (stacked ? 13 : 1);
        if (graphics != null) {
            graphics.text(ctx.font(), ctx.trim(label, textW), textX, y + 1, CoiStyle.TEXT_BODY, false);
            graphics.text(ctx.font(), ctx.trim(value, textW), stacked ? textX : textX + textW - valueW,
                    valueY, valueRgb, false);
            bar.draw(graphics, textX, valueY + 12, textW);
        }
        int next = valueY + 22;
        for (var line : ctx.font().split(sub, Math.max(1, textW))) {
            if (graphics != null) graphics.text(ctx.font(), line, textX, next, subRgb, false);
            next += 10;
        }
        if (note != null) {
            next += 3;
            for (var line : ctx.font().split(note, Math.max(1, textW))) {
                if (graphics != null) graphics.text(ctx.font(), line, textX, next, CoiStyle.TEXT_MUTED, false);
                next += 10;
            }
        }
        return Math.max(y + VITAL_ROW_H, next + 3);
    }



    static BarPainter bar(double fraction, int argb) {
        return (graphics, x, y, w) -> MenuGauges.gauge(graphics, x, y, w, BAR_H, fraction, argb);
    }
}
