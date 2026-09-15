package dev.ua.ikeepcalm.coi.client.screen.settings;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudElement;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * What {@link HudLayoutScreen} paints over the world: one sample-data preview
 * per element, the scrim that marks a hidden one, and the outline plus name
 * chip that mark the hovered and selected ones.
 */
final class LayoutPainter {

    static final int HOVER_OUTLINE = 0x80FFD870;
    private static final int GHOST_SCRIM = 0x99101014;
    private static final int CHIP_BG = 0xD0000000;

    private LayoutPainter() {
    }

    static void previews(GuiGraphicsExtractor graphics, Font font, List<HudElement> elements,
                         int screenW, int screenH, HudConfig.HudSettings settings, long time) {
        for (HudElement element : elements) {
            int[] box = element.bounds(screenW, screenH, settings);
            element.renderPreview(graphics, screenW, screenH, settings, time);
            if (!element.visible(settings)) {
                graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], GHOST_SCRIM);
                graphics.text(font, Component.translatable("screen.coi.layout_hidden"),
                        box[0] + 2, box[1] + 2, CoiStyle.TEXT_MUTED, true);
            }
        }
    }

    /**
     * Rings the element and names it. The selection takes a second ring, so it
     * still reads as the selection while something else is hovered.
     */
    static void highlight(GuiGraphicsExtractor graphics, Font font, HudElement element,
                          int screenW, int screenH, HudConfig.HudSettings settings,
                          int color, boolean doubleRing) {
        int[] box = element.bounds(screenW, screenH, settings);
        graphics.outline(box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, color);
        if (doubleRing) {
            graphics.outline(box[0] - 2, box[1] - 2, box[2] + 4, box[3] + 4, color);
        }
        nameChip(graphics, font, element, box, screenW);
    }

    /**
     * The element's name on a dark chip, flipped below the box when there is
     * no room above it.
     */
    private static void nameChip(GuiGraphicsExtractor graphics, Font font, HudElement element, int[] box, int screenW) {
        Component label = element.label();
        int textW = font.width(label);
        int chipX = Mth.clamp(box[0], 0, Math.max(0, screenW - textW - 6));
        int chipY = box[1] - 12 >= 0 ? box[1] - 12 : box[1] + box[3] + 2;
        graphics.fill(chipX, chipY, chipX + textW + 6, chipY + 11, CHIP_BG);
        graphics.text(font, label, chipX + 3, chipY + 2, CoiStyle.ACCENT, false);
    }
}
