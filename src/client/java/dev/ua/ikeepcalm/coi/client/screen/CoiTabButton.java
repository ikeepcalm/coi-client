package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.BooleanSupplier;

/**
 * Hand-drawn tab in the mod's dark/gold card style. The selected tab carries a
 * 2px accent rule along its bottom edge. An optional 9px icon is drawn left of
 * the centered label.
 */
public class CoiTabButton extends AbstractWidget {

    @FunctionalInterface
    public interface IconPainter {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int size, int color);
    }

    private final BooleanSupplier selected;
    private final Runnable onPress;
    private final IconPainter icon;

    public CoiTabButton(int x, int y, int width, int height, Component label,
                        IconPainter icon, BooleanSupplier selected, Runnable onPress) {
        super(x, y, width, height, label);
        this.icon = icon;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean sel = selected.getAsBoolean();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, sel ? CoiStyle.CARD_BG : CoiStyle.TAB_BG_UNSELECTED);
        graphics.outline(x, y, w, h, CoiStyle.BORDER);
        if (sel) {
            graphics.fill(x, y + h - 2, x + w, y + h, CoiStyle.ACCENT);
        }

        int color = sel ? CoiStyle.ACCENT : (isHovered() ? CoiStyle.TEXT_BODY : CoiStyle.TEXT_MUTED);
        Font font = Minecraft.getInstance().font;
        int iconSpace = icon != null ? 13 : 0;
        String label = font.plainSubstrByWidth(getMessage().getString(), w - iconSpace - 8);
        int labelW = font.width(label);
        int startX = x + (w - (iconSpace + labelW)) / 2;

        if (icon != null) {
            icon.draw(graphics, startX, y + (h - 9) / 2, 9, color);
        }
        graphics.text(font, label, startX + iconSpace, y + (h - 8) / 2, color, false);
    }

    @Override
    public void onClick(@NonNull MouseButtonEvent click, boolean doubled) {
        playDownSound(Minecraft.getInstance().getSoundManager());
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, getMessage());
    }
}
