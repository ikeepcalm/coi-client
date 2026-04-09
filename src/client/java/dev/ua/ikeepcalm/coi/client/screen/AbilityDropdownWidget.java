package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AbilityDropdownWidget extends AbstractWidget {

    private final Supplier<List<String>> optionsSupplier;
    private final Consumer<String> onSelect;
    private String selected;
    private boolean expanded = false;
    private int hoveredIndex = -1;
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int MAX_VISIBLE_ITEMS = 5;

    public AbilityDropdownWidget(int x, int y, int width, int height,
                                 Supplier<List<String>> optionsSupplier, String currentSelection,
                                 Consumer<String> onSelect) {
        super(x, y, width, height, Component.empty());
        this.optionsSupplier = optionsSupplier;
        this.selected = currentSelection;
        this.onSelect = onSelect;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;

        // Render the main dropdown button
        int borderColor = this.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF2C2C2C);
        // Draw border manually
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor); // Top
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor); // Bottom
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor); // Left
        graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor); // Right

        List<String> options = optionsSupplier.get();

        String displayText;
        if (selected != null) {
            String selectedId = AbilityInfo.extractId(selected);
            displayText = options.stream()
                    .filter(o -> o.startsWith(selectedId + " - "))
                    .findFirst()
                    .map(AbilityInfo::extractDisplayName)
                    .orElseGet(() -> AbilityInfo.extractDisplayName(selected));
            if (displayText == null) displayText = selected;
        } else {
            if (options.isEmpty()) {
                displayText = Component.translatable("screen.coi.no_abilities_available").getString();
            } else {
                displayText = Component.translatable("screen.coi.ability_choose").getString();
            }
        }

        // Ensure text fits in the dropdown button
        String trimmedText = textRenderer.plainSubstrByWidth(displayText, this.width - 20);


        // Use light yellow color for better visibility against dark backgrounds
        graphics.text(textRenderer, trimmedText,
                this.getX() + 4, this.getY() + (this.height - 8) / 2, 0xFFFFFF55, false);

        String arrow = expanded ? "▲" : "▼";
        // Use same light yellow color as text for consistency
        graphics.text(textRenderer, arrow,
                this.getX() + this.width - 12, this.getY() + (this.height - 8) / 2, 0xFFFFFF55, false);
    }


    private void renderDropdown(GuiGraphicsExtractor graphics, Font textRenderer, int mouseX, int mouseY) {
        List<String> options = optionsSupplier.get();

        int dropdownY = this.getY() + this.height;
        int visibleItems = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int dropdownHeight = visibleItems * ITEM_HEIGHT;

        // Draw dropdown background with a slightly lighter color than pure black
        graphics.fill(this.getX(), dropdownY,
                this.getX() + this.width, dropdownY + dropdownHeight, 0xFF2C2C2C);
        // Draw border manually
        graphics.fill(this.getX(), dropdownY, this.getX() + this.width, dropdownY + 1, 0xFFFFFFFF); // Top
        graphics.fill(this.getX(), dropdownY + dropdownHeight - 1, this.getX() + this.width, dropdownY + dropdownHeight, 0xFFFFFFFF); // Bottom
        graphics.fill(this.getX(), dropdownY, this.getX() + 1, dropdownY + dropdownHeight, 0xFFFFFFFF); // Left
        graphics.fill(this.getX() + this.width - 1, dropdownY, this.getX() + this.width, dropdownY + dropdownHeight, 0xFFFFFFFF); // Right

        // Note: Scissor clipping disabled as it was preventing text from rendering properly
        // graphics.enableScissor(this.getX() + 1, dropdownY + 1,
        //         this.getX() + this.width - 1, dropdownY + dropdownHeight - 1);

        int itemY = dropdownY;
        hoveredIndex = -1;

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleItems, options.size()); i++) {
            boolean isHovered = mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                    mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

            if (isHovered) {
                graphics.fill(this.getX() + 1, itemY,
                        this.getX() + this.width - 1, itemY + ITEM_HEIGHT, 0xFF505050);
                hoveredIndex = i;
            }

            String displayText = options.get(i);

            if (displayText != null) {
                String[] parts = displayText.split(" - ");
                if (parts.length > 1) {
                    displayText = parts[1];
                }


                // Ensure text fits within the dropdown item
                String trimmedText = textRenderer.plainSubstrByWidth(displayText, this.width - 8);
                // Use light yellow color for better visibility
                graphics.text(textRenderer, trimmedText,
                        this.getX() + 4, itemY + 6, 0xFFFFFF55, false);
            }

            itemY += ITEM_HEIGHT;
        }

        // graphics.disableScissor(); // Not needed since scissor is disabled

        if (options.size() > MAX_VISIBLE_ITEMS) {
            renderScrollbar(graphics, dropdownY, dropdownHeight);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int dropdownY, int dropdownHeight) {
        List<String> options = optionsSupplier.get();
        int scrollbarX = this.getX() + this.width - 6;
        int scrollbarWidth = 4;

        graphics.fill(scrollbarX, dropdownY + 1,
                scrollbarX + scrollbarWidth, dropdownY + dropdownHeight - 1, 0xFF404040);

        int totalItems = options.size();
        int maxScroll = totalItems - MAX_VISIBLE_ITEMS;
        float scrollPercentage = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;

        int handleHeight = Math.max(20, (int) ((float) MAX_VISIBLE_ITEMS / totalItems * dropdownHeight));
        int handleY = dropdownY + 1 + (int) ((dropdownHeight - handleHeight - 2) * scrollPercentage);

        graphics.fill(scrollbarX, handleY,
                scrollbarX + scrollbarWidth, handleY + handleHeight, 0xFF808080);
    }


    @Override
    public void onClick(@NonNull MouseButtonEvent click, boolean doubled) {
        Minecraft client = Minecraft.getInstance();
        double mouseX = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) client.getWindow().getScreenHeight();

        handleClick(mouseX, mouseY);
    }

    protected void handleClick(double mouseX, double mouseY) {
        if (!expanded) {
            expanded = true;
        } else {
            List<String> options = optionsSupplier.get();
            if (hoveredIndex >= 0 && hoveredIndex < options.size()) {
                selected = options.get(hoveredIndex);
                onSelect.accept(selected);
                expanded = false;
            } else {
                expanded = false;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (expanded && isMouseOver(mouseX, mouseY)) {
            List<String> options = optionsSupplier.get();
            int maxScroll = Math.max(0, options.size() - MAX_VISIBLE_ITEMS);

            // Use proper scroll direction and magnitude
            int scrollDirection = verticalAmount > 0 ? -1 : 1; // Reverse direction for natural scrolling
            scrollOffset = Mth.clamp(scrollOffset + scrollDirection, 0, maxScroll);

            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (expanded) {
            List<String> options = optionsSupplier.get();
            int dropdownHeight = Math.min(options.size(), MAX_VISIBLE_ITEMS) * ITEM_HEIGHT;
            return mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                    mouseY >= this.getY() && mouseY < this.getY() + this.height + dropdownHeight;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void renderExpanded(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        List<String> options = optionsSupplier.get();
        if (expanded && !options.isEmpty()) {
            renderDropdown(graphics, Minecraft.getInstance().font, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, Component.literal("Ability dropdown"));
    }
}