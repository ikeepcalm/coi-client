package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.screen.ability.PickerModel.Row;
import dev.ua.ikeepcalm.coi.client.ui.AbilityIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Draws one row of the ability picker at a time, plus the tooltip and the
 * scrollbar. Stateless: the panel's geometry arrives as arguments, so the same
 * numbers the overlay hit-tests with are the ones that get painted.
 */
final class PickerPainter {

    private static final int UNBIND_HOVER = 0xFFFF6B6B;
    private static final int LOCKED_TEXT = 0xFFFF6B6B;
    private static final int TOOLTIP_WIDTH = 200;

    /** Just enough pathway colour to read as a block, not enough to fight the text. */
    private static final int ROW_TINT_ALPHA = 22;
    private static final int HEADER_RULE_ALPHA = 110;
    private static final int DIM_ICON_ALPHA = 80;

    private PickerPainter() {
    }

    static void unbindRow(GuiGraphicsExtractor graphics, Font font, int panelX, int panelW, int rowY, boolean hovered) {
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + PickerMetrics.UNBIND_H, CoiStyle.ROW_HOVER);
        }
        int color = hovered ? UNBIND_HOVER : CoiStyle.TEXT_MUTED;
        // Drawn ✕ glyph (unicode is unreliable in the MC font)
        float cx = panelX + PickerMetrics.PAD + 8;
        float cy = rowY + PickerMetrics.UNBIND_H / 2f;
        EffectPaint.line(graphics, cx - 3, cy - 3, cx + 3, cy + 3, color, 1);
        EffectPaint.line(graphics, cx - 3, cy + 3, cx + 3, cy - 3, color, 1);
        graphics.text(font, Component.translatable("screen.coi.picker_unbind"),
                panelX + PickerMetrics.TEXT_INSET, rowY + 5, color);
    }

    /**
     * Pathway caption plus a 1px rule to the panel edge — the rule is what makes
     * the abilities below it read as one block rather than a fresh list.
     */
    static void headerRow(GuiGraphicsExtractor graphics, Font font, int panelX, int panelW, Row row, int rowY) {
        int rgb = Pathways.pathwayRgb(row.pathway());
        String name = row.pathway().isEmpty()
                ? I18n.get("screen.coi.picker_group_other")
                : row.pathway().toUpperCase(Locale.ROOT);
        Component label = row.sequence() >= 0
                ? Component.translatable("screen.coi.picker_group_seq", name, row.sequence())
                : Component.literal(name);

        int textX = panelX + PickerMetrics.PAD;
        graphics.text(font, label, textX, rowY + 3, EffectPaint.argb(rgb, 255), false);

        int ruleX = textX + font.width(label) + 4;
        int ruleEnd = panelX + panelW - PickerMetrics.PAD;
        if (ruleEnd > ruleX) {
            graphics.fill(ruleX, rowY + 7, ruleEnd, rowY + 8, EffectPaint.argb(rgb, HEADER_RULE_ALPHA));
        }
    }

    static void messageRow(GuiGraphicsExtractor graphics, Font font, int panelX, int panelW, int rowY, String query) {
        graphics.centeredText(font, Component.translatable("screen.coi.picker_no_results", query),
                panelX + panelW / 2, rowY + 5, CoiStyle.TEXT_MUTED);
    }

    static void abilityRow(GuiGraphicsExtractor graphics, Font font, int panelX, int panelW,
                           Row row, int rowY, boolean hovered, boolean metaAvailable, boolean selected) {
        String option = row.option();
        AbilityInfo info = PickerModel.infoFor(option);
        boolean unavailable = info != null && info.isUnavailable();
        int rgb = Pathways.pathwayRgb(row.pathway());
        int rowH = PickerMetrics.ROW_H;

        // A wash of the pathway colour under every row of the group; the hover and
        // selection layers sit on top so they still read against it.
        graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + rowH, EffectPaint.argb(rgb, ROW_TINT_ALPHA));
        if (hovered) {
            graphics.fill(panelX + 1, rowY, panelX + panelW - 1, rowY + rowH, CoiStyle.ROW_HOVER);
        }
        graphics.fill(panelX + 1, rowY + 1, panelX + 3, rowY + rowH - 1, EffectPaint.argb(rgb, 255));

        AbilityIcons.draw(graphics, option, panelX + PickerMetrics.PAD, rowY + (rowH - PickerMetrics.ICON) / 2,
                PickerMetrics.ICON, unavailable ? DIM_ICON_ALPHA : 255);

        int textX = panelX + PickerMetrics.TEXT_INSET;
        int rightEdge = panelX + panelW - PickerMetrics.PAD;
        int lineOne = rowY + 4;

        Component tag = PickerLabels.kindTag(info);
        int tagX = rightEdge - font.width(tag);
        graphics.text(font, tag, tagX, lineOne, unavailable ? LOCKED_TEXT : CoiStyle.TEXT_MUTED, false);

        String name = font.plainSubstrByWidth(PickerModel.displayNameOf(option), Math.max(8, tagX - 5 - textX));
        graphics.text(font, name, textX, lineOne, unavailable ? LOCKED_TEXT : CoiStyle.TEXT_BODY, false);
        if (unavailable) {
            graphics.fill(textX, lineOne + 4, textX + font.width(name), lineOne + 5, LOCKED_TEXT);
        }

        metaLine(graphics, font, info, textX, rightEdge, rowY + 15, metaAvailable);

        if (selected) {
            graphics.outline(panelX + 1, rowY, panelW - 2, rowH, CoiStyle.ACCENT);
        }
    }

    /**
     * Cost, cooldown and category in fixed columns. The badges are drawn even
     * when the value is zero — a blank where a cost should be is exactly what
     * made the old list read as interchangeable — but a server that sends no
     * numbers at all gets the category alone instead of a row of em-dashes.
     */
    private static void metaLine(GuiGraphicsExtractor graphics, Font font, AbilityInfo info,
                                 int textX, int rightEdge, int lineY, boolean metaAvailable) {
        Component category = PickerLabels.category(info);
        if (!metaAvailable) {
            graphics.text(font, category, textX, lineY, CoiStyle.TEXT_MUTED, false);
            return;
        }

        int categoryX = rightEdge - font.width(category);
        graphics.text(font, category, categoryX, lineY, CoiStyle.TEXT_MUTED, false);

        Component cost = PickerLabels.cost(info);
        graphics.text(font, cost, textX, lineY, CoiStyle.TEXT_MUTED, false);

        // A long cost badge pushes the cooldown column rather than colliding with it
        int cooldownX = Math.max(textX + PickerMetrics.COOLDOWN_COL, textX + font.width(cost) + 6);
        Component cooldown = PickerLabels.cooldown(info);
        if (cooldownX + font.width(cooldown) < categoryX - 4) {
            graphics.text(font, cooldown, cooldownX, lineY, CoiStyle.TEXT_MUTED, false);
        }
    }

    /**
     * What the row cannot show: the prose, the sequence and why the ability is
     * unusable. Cost, cooldown and kind now live permanently on the row itself,
     * so repeating them here would only be noise.
     */
    static void tooltip(GuiGraphicsExtractor graphics, Font font, String option, int mouseX, int mouseY) {
        AbilityInfo info = PickerModel.infoFor(option);
        if (info == null) return;

        List<FormattedCharSequence> lines = new ArrayList<>();
        if (!info.description().isEmpty()) {
            lines.addAll(font.split(Component.literal(info.description()).withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        }
        if (info.sequence() >= 0) {
            addLine(lines, Component.translatable("screen.coi.ability_tooltip_sequence", info.sequence()).withStyle(ChatFormatting.DARK_GRAY));
        }
        Component reason = PickerLabels.blockReason(info);
        if (reason != null) {
            addLine(lines, reason.copy().withStyle(ChatFormatting.RED));
        }

        if (!lines.isEmpty()) {
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static void addLine(List<FormattedCharSequence> lines, Component component) {
        lines.add(component.getVisualOrderText());
    }

    /**
     * Handle size and position come from pixels, not row counts, since headers
     * and ability rows are different heights.
     */
    static void scrollbar(GuiGraphicsExtractor graphics, int panelX, int panelW,
                          int listTop, int listBottom, int listH, int above, int content) {
        int trackX = panelX + panelW - 5;
        int trackTop = listTop + 1;
        int trackH = listBottom - trackTop - 1;
        if (trackH <= 0) return;
        graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackH, CoiStyle.SCROLL_TRACK);

        int handleH = Math.clamp((long) listH * trackH / Math.max(1, content), Math.min(12, trackH), trackH);
        int handleY = trackTop + (trackH - handleH) * above / Math.max(1, content - listH);
        graphics.fill(trackX, handleY, trackX + 3, handleY + handleH, CoiStyle.BORDER);
    }
}
