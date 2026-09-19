package dev.ua.ikeepcalm.coi.client.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A keyboard-focusable index tab with the same hit area at every GUI scale. */
public class ArchiveTab extends Button {
    private final BooleanSupplier selected;

    public ArchiveTab(Component label, Runnable action, BooleanSupplier selected) {
        super(0, 0, 100, 20, label, ignored -> action.run(), DEFAULT_NARRATION);
        this.selected = selected;
        setTooltip(Tooltip.create(label));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        boolean current = selected.getAsBoolean();
        int color = current || isHoveredOrFocused() ? ArchivePaint.LABEL : 0xFFA6A79B;
        if (isHoveredOrFocused()) g.fill(getX(), getY(), getRight(), getBottom(), 0x184F6251);
        g.fill(getX(), getBottom() - 2, getRight() - 3, getBottom(), current ? ArchivePaint.LABEL : ArchivePaint.RULE);
        String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, getWidth() - 12));
        g.text(font, label, getX() + 6, getY() + 6, color, false);
    }
}
