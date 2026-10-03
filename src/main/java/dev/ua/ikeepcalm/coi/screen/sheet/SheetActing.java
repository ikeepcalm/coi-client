package dev.ua.ikeepcalm.coi.screen.sheet;

import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.ActingPercent;
import dev.ua.ikeepcalm.coi.hud.render.PlateSymbols;
import dev.ua.ikeepcalm.coi.screen.menu.MenuGauges;
import dev.ua.ikeepcalm.coi.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static dev.ua.ikeepcalm.coi.screen.sheet.SheetContext.num;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetContext.pct;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetGlyphs.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetMetrics.*;
import static dev.ua.ikeepcalm.coi.screen.sheet.SheetPalette.*;

final class SheetActing {

    private SheetActing() {
    }

    static int overview(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        SheetState.Acting acting = SheetState.acting();
        if (SheetState.outer() || acting == null) return y;
        return actingRow(ctx, graphics, x,
                ctx.section(graphics, x, y, w, "screen.coi.sheet_acting", GLYPH_GROWTH), w, acting);
    }

    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w, int bottom) {
        SheetState.Acting acting = SheetState.acting();
        if (SheetState.outer() || acting == null) return drawOuter(ctx, graphics, x, y, w);

        List<SheetChips.Chip> chips = actingChips(acting);
        List<List<SheetChips.Chip>> chipRows = SheetChips.layout(ctx, chips, w);

        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_acting", GLYPH_GROWTH);
        ry = actingRow(ctx, graphics, x, ry, w, acting);
        ry = SheetChips.draw(ctx, graphics, x, ry, chipRows);
        ry = SheetChips.explanations(ctx, graphics, x, ry, w, chips);

        ry += 6;
        MenuTheme.hairline(graphics, x, ry, w);
        ry += 6;
        ry = ctx.section(graphics, x, ry, w, "screen.coi.sheet_sources", GLYPH_RESTORE);
        return drawLedger(ctx, graphics, x, ry, w, acting, bottom);
    }

    private static int actingRow(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                                 SheetState.Acting acting) {
        int accent = accent();
        double percent = Math.clamp(acting.percent(), 0, 100);
        Component cooldown = SheetState.actingCooldownNow() > 0
                ? Component.translatable("screen.coi.sheet_acting_cooldown", SheetState.actingCooldownClock())
                : Component.translatable("screen.coi.sheet_acting_ready");
        return SheetRows.vital(ctx, graphics, x, y, w,
                (g, sx, sy) -> PlateSymbols.draw(g, PlateSymbols.MASK, sx, sy,
                        (float) (percent / 100.0), 1f, PlateSymbols.NO_WASH),
                I18n.get("screen.coi.sheet_progress"), pct(ActingPercent.floor(percent, 1)), acting.limited() ? RED : accent,
                SheetRows.bar(acting.acting() / Math.max(1.0, acting.needed()), acting.limited() ? RED : accent),
                Component.translatable("screen.coi.sheet_acting_progress",
                        num(acting.acting()), num(acting.needed())), CoiStyle.TEXT_MUTED,
                cooldown);
    }

    private static int drawLedger(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w,
                                  SheetState.Acting acting, int bottom) {
        if (acting.sources().isEmpty()) {
            graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_sources_empty"), w),
                    x, y + 2, CoiStyle.TEXT_MUTED, false);
            return y + LEDGER_ROW_H;
        }
        int count = acting.sources().size();
        int minimumHeight = LEDGER_ROW_H + 8;
        int remainingHeight = Math.max(count * minimumHeight, bottom - y);
        int index = 0;
        for (SheetState.Source source : acting.sources()) {
            int rowHeight = remainingHeight / (count - index);
            remainingHeight -= rowHeight;
            int surfaceBottom = y + rowHeight - 3;
            graphics.fill(x, y, x + w, surfaceBottom, index % 2 == 0 ? 0x0EFFFFFF : 0x07FFFFFF);
            graphics.fill(x, surfaceBottom - 1, x + w, surfaceBottom, 0xFF494C43);
            int rowY = y + (rowHeight - 3 - 8) / 2;
            int left = x + 6;
            int right = x + w - 6;
            String label = source.label().isEmpty() ? source.source() : source.label();
            String value = source.contributed() + " / " + (source.unlimited() ? "∞" : source.cap());
            boolean capped = source.capped();
            int color = capped ? RED : CoiStyle.TEXT_BODY;

            int labelW = Math.min(ctx.font().width(label), w * 2 / 5);
            int valueW = ctx.font().width(value);
            graphics.text(ctx.font(), ctx.trim(label, labelW), left, rowY, color, false);
            graphics.text(ctx.font(), value, right - valueW, rowY,
                    capped ? RED : CoiStyle.TEXT_MUTED, false);

            int barX = left + labelW + 6;
            int barW = right - valueW - 6 - barX;
            if (barW > 8 && !source.unlimited() && source.cap() > 0) {
                MenuGauges.gauge(graphics, barX, rowY + 2, barW, 4,
                        source.contributed() / (double) source.cap(), capped ? RED : accent());
            }
            y += rowHeight;
            index++;
        }
        return y;
    }

    private static List<SheetChips.Chip> actingChips(SheetState.Acting acting) {
        List<SheetChips.Chip> chips = new ArrayList<>();
        if (acting.limited()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_limited"), "", RED, GLYPH_RESIST,
                    Component.translatable("screen.coi.sheet_limited_desc")));
        }
        SheetState.Overflow overflow = acting.overflow();
        if (overflow.eligible()) {
            String text = overflow.uncapped()
                    ? I18n.get("screen.coi.sheet_overflow_uncapped", overflow.banked())
                    : I18n.get("screen.coi.sheet_overflow", overflow.banked(), overflow.ceiling());
            chips.add(new SheetChips.Chip(text, "", CoiStyle.ACCENT, GLYPH_GROWTH,
                    Component.translatable("screen.coi.sheet_overflow_desc")));
        }
        if (overflow.foreignThrottlePercent() > 0) {
            chips.add(new SheetChips.Chip(
                    I18n.get("screen.coi.sheet_foreign_throttle", overflow.foreignThrottlePercent()),
                    "", AMBER, GLYPH_AUTHORITY,
                    Component.translatable("screen.coi.sheet_foreign_desc")));
        }
        return chips;
    }

    private static int drawOuter(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_acting", GLYPH_GROWTH);
        int textX = x + SYMBOL + 8;
        int textW = x + w - textX;
        PlateSymbols.draw(graphics, PlateSymbols.MASK, x, ry, 0f, 1f, PlateSymbols.NO_WASH);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_outer"), textW), textX, ry + 1,
                CoiStyle.TEXT_BODY, true);
        int lineY = ry + LINE + 2;
        for (FormattedCharSequence line : ctx.font().split(
                Component.translatable("screen.coi.sheet_outer_hint"), textW)) {
            graphics.text(ctx.font(), line, textX, lineY, CoiStyle.TEXT_MUTED);
            lineY += PARA_LINE;
        }
        return Math.max(ry + SYMBOL, lineY);
    }
}
