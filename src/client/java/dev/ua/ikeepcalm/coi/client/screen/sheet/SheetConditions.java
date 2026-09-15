package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetContext.round0;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_POWER;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_SOUL;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_WARD;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.AMBER;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.RED;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.VIOLET;

import dev.ua.ikeepcalm.coi.client.state.SheetState;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * The conditions the character is under — and only those they are actually
 * under. A section with nothing to say is skipped outright rather than drawn
 * empty, which is what keeps the page's height a fact about the character.
 */
final class SheetConditions {

    private SheetConditions() {
    }

    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        List<SheetChips.Chip> chips = conditionChips();
        if (chips.isEmpty()) return y;

        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_conditions", GLYPH_WARD);
        ry = SheetChips.draw(ctx, graphics, x, ry, SheetChips.layout(ctx, chips, w));
        return SheetChips.explanations(ctx, graphics, x, ry, w, chips) + ctx.sectionGap();
    }

    private static List<SheetChips.Chip> conditionChips() {
        List<SheetChips.Chip> chips = new ArrayList<>();
        SheetState.Gauge lifeAndDeath = SheetState.lifeAndDeath();
        if (lifeAndDeath.present()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_life_death"),
                    I18n.get("screen.coi.sheet_percent", round0(Math.clamp(lifeAndDeath.value(), 0, 1) * 100)),
                    VIOLET, GLYPH_SOUL,
                    Component.translatable("screen.coi.sheet_life_death_desc")));
        }
        SheetState.Pressure pressure = SheetState.pressure();
        if (pressure.present()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_pressure"),
                    pressure.stacks() + "/" + pressure.cap(), AMBER, GLYPH_POWER,
                    Component.translatable("screen.coi.sheet_pressure_desc")));
        }
        if (SheetState.anomaly()) {
            chips.add(new SheetChips.Chip(I18n.get("screen.coi.sheet_anomaly"), "", RED, GLYPH_WARD,
                    Component.translatable("screen.coi.sheet_anomaly_desc")));
        }
        return chips;
    }
}
