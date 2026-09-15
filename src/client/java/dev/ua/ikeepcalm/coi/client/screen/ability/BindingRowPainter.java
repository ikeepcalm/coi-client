package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.client.screen.ScreenInput;
import dev.ua.ikeepcalm.coi.client.ui.AbilityIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Paints one row of {@link AbilityBindingScreen}'s slot list — a hotkey slot, a
 * wheel slot or a gesture — inside the card the screen has already drawn.
 * <p>
 * All three end in the same right-hand column: the bound ability's icon and
 * name, or the empty-slot line. The row's own frame, hover wash and any tooltip
 * belong to the screen, which is what knows whether the picker is over it.
 */
final class BindingRowPainter {

    /** Widest a keybind name is allowed to be before it is clipped. */
    private static final int KEY_CHIP_TEXT_W = 52;
    private static final int KEY_CHIP_X = 62;
    /** Left edge of the bound-ability column, never tighter than this. */
    private static final int VALUE_COLUMN_MIN = 136;

    private BindingRowPainter() {
    }

    static void hotkeyRow(GuiGraphicsExtractor graphics, Font font, int contentX, int contentW,
                          int slot, int rowY, boolean inactive) {
        int labelColor = inactive ? CoiStyle.INACTIVE : CoiStyle.TEXT_MUTED;
        graphics.text(font, Component.translatable("screen.coi.slot_label", slot + 1),
                contentX + 8, rowY + 8, labelColor);

        // Keybind chip
        String key = ScreenInput.keyName(CoiKeyBindings.abilityKey(slot)).getString();
        key = font.plainSubstrByWidth(key, KEY_CHIP_TEXT_W);
        int chipX = contentX + KEY_CHIP_X;
        int chipW = font.width(key) + 8;
        graphics.fill(chipX, rowY + 5, chipX + chipW, rowY + 19, 0x60000000);
        graphics.outline(chipX, rowY + 5, chipW, 14, inactive ? CoiStyle.INACTIVE : CoiStyle.BORDER);
        graphics.text(font, key, chipX + 4, rowY + 8, inactive ? CoiStyle.INACTIVE : CoiStyle.ACCENT, false);

        int rightX = valueColumn(contentX, contentW);
        if (inactive) {
            Component hint = Component.translatable("screen.coi.slot_inactive");
            String trimmed = font.plainSubstrByWidth(hint.getString(), contentX + contentW - rightX - 8);
            graphics.text(font, trimmed, rightX, rowY + 8, CoiStyle.INACTIVE, false);
            return;
        }
        boundValue(graphics, font, contentX, contentW, AbilityBindings.getBoundAbility(slot), rightX, rowY);
    }

    static void wheelRow(GuiGraphicsExtractor graphics, Font font, int contentX, int contentW, int slot, int rowY) {
        Component label = Component.translatable("screen.coi.wheel_slot").copy().append(" " + (slot + 1));
        graphics.text(font, label, contentX + 8, rowY + 8, CoiStyle.TEXT_MUTED);

        int rightX = valueColumn(contentX, contentW);
        boundValue(graphics, font, contentX, contentW, AbilityBindings.getWheelAbility(slot), rightX, rowY);
    }

    static void gestureRow(GuiGraphicsExtractor graphics, Font font, int contentX, int contentW, int slot, int rowY) {
        GestureType type = GestureType.values()[slot];
        type.drawPreview(graphics, contentX + 8, rowY + 5, 14, CoiStyle.TEXT_BODY);
        graphics.text(font, type.displayName(), contentX + 30, rowY + 8, CoiStyle.TEXT_MUTED);

        int rightX = valueColumn(contentX, contentW);
        boundValue(graphics, font, contentX, contentW, AbilityBindings.getGestureAbility(slot), rightX, rowY);
    }

    private static int valueColumn(int contentX, int contentW) {
        return contentX + Math.max(VALUE_COLUMN_MIN, contentW * 2 / 5);
    }

    private static void boundValue(GuiGraphicsExtractor graphics, Font font, int contentX, int contentW,
                                   String bound, int rightX, int rowY) {
        if (bound != null) {
            AbilityIcons.draw(graphics, bound, rightX, rowY + 4, 16, 255);
            String name = AbilityInfo.extractDisplayName(bound);
            if (name == null) name = bound;
            String trimmed = font.plainSubstrByWidth(name, contentX + contentW - rightX - 28);
            graphics.text(font, trimmed, rightX + 20, rowY + 8, CoiStyle.TEXT_BODY, false);
        } else {
            graphics.text(font, Component.translatable("screen.coi.empty_slot"), rightX, rowY + 8, CoiStyle.TEXT_MUTED);
        }
    }
}
