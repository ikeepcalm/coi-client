package dev.ua.ikeepcalm.coi.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AbilityDropdownWidget extends ClickableWidget {

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
        super(x, y, width, height, Text.empty());
        this.optionsSupplier = optionsSupplier;
        this.selected = currentSelection;
        this.onSelect = onSelect;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // Render the main dropdown button
        int borderColor = this.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF2C2C2C);
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, borderColor);

        List<String> options = optionsSupplier.get();
        
        String displayText;
        if (selected != null) {
            if (selected.contains(" - ")) {
                String[] parts = selected.split(" - ");
                displayText = parts.length > 1 ? parts[1] : selected;
            } else {
                displayText = selected;
            }
        } else {
            if (options.isEmpty()) {
                displayText = Text.translatable("screen.coi.no_abilities_available").getString();
            } else {
                displayText = Text.translatable("screen.coi.ability_choose").getString();
            }
        }
        
        // Ensure text fits in the dropdown button
        String trimmedText = textRenderer.trimToWidth(displayText, this.width - 20);
        
        
        // Use light yellow color for better visibility against dark backgrounds
        context.drawText(textRenderer, trimmedText,
                this.getX() + 4, this.getY() + (this.height - 8) / 2, 0xFFFFFF55, false);

        String arrow = expanded ? "▲" : "▼";
        // Use same light yellow color as text for consistency
        context.drawText(textRenderer, arrow,
                this.getX() + this.width - 12, this.getY() + (this.height - 8) / 2, 0xFFFFFF55, false);
    }
    

    private void renderDropdown(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        List<String> options = optionsSupplier.get();
        
        int dropdownY = this.getY() + this.height;
        int visibleItems = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int dropdownHeight = visibleItems * ITEM_HEIGHT;

        // Draw dropdown background with a slightly lighter color than pure black
        context.fill(this.getX(), dropdownY,
                this.getX() + this.width, dropdownY + dropdownHeight, 0xFF2C2C2C);
        context.drawBorder(this.getX(), dropdownY, this.width, dropdownHeight, 0xFFFFFFFF);

        // Note: Scissor clipping disabled as it was preventing text from rendering properly
        // context.enableScissor(this.getX() + 1, dropdownY + 1,
        //         this.getX() + this.width - 1, dropdownY + dropdownHeight - 1);

        int itemY = dropdownY;
        hoveredIndex = -1;

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleItems, options.size()); i++) {
            boolean isHovered = mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                    mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

            if (isHovered) {
                context.fill(this.getX() + 1, itemY,
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
                String trimmedText = textRenderer.trimToWidth(displayText, this.width - 8);
                // Use light yellow color for better visibility
                context.drawText(textRenderer, trimmedText,
                        this.getX() + 4, itemY + 6, 0xFFFFFF55, false);
            }

            itemY += ITEM_HEIGHT;
        }

        // context.disableScissor(); // Not needed since scissor is disabled

        if (options.size() > MAX_VISIBLE_ITEMS) {
            renderScrollbar(context, dropdownY, dropdownHeight);
        }
    }

    private void renderScrollbar(DrawContext context, int dropdownY, int dropdownHeight) {
        List<String> options = optionsSupplier.get();
        int scrollbarX = this.getX() + this.width - 6;
        int scrollbarWidth = 4;

        context.fill(scrollbarX, dropdownY + 1,
                scrollbarX + scrollbarWidth, dropdownY + dropdownHeight - 1, 0xFF404040);

        int totalItems = options.size();
        int maxScroll = totalItems - MAX_VISIBLE_ITEMS;
        float scrollPercentage = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;

        int handleHeight = Math.max(20, (int) ((float) MAX_VISIBLE_ITEMS / totalItems * dropdownHeight));
        int handleY = dropdownY + 1 + (int) ((dropdownHeight - handleHeight - 2) * scrollPercentage);

        context.fill(scrollbarX, handleY,
                scrollbarX + scrollbarWidth, handleY + handleHeight, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMouseOver(mouseX, mouseY)) {
                if (!expanded) {
                    expanded = true;
                    return true;
                }
            }

            List<String> options = optionsSupplier.get();
            if (expanded && hoveredIndex >= 0 && hoveredIndex < options.size()) {
                selected = options.get(hoveredIndex);
                onSelect.accept(selected);
                expanded = false;
                return true;
            }

            expanded = false;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (expanded && isMouseOver(mouseX, mouseY)) {
            List<String> options = optionsSupplier.get();
            int maxScroll = Math.max(0, options.size() - MAX_VISIBLE_ITEMS);
            
            // Use proper scroll direction and magnitude
            int scrollDirection = verticalAmount > 0 ? -1 : 1; // Reverse direction for natural scrolling
            scrollOffset = MathHelper.clamp(scrollOffset + scrollDirection, 0, maxScroll);
            
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
    
    public void renderExpanded(DrawContext context, int mouseX, int mouseY, float delta) {
        List<String> options = optionsSupplier.get();
        if (expanded && !options.isEmpty()) {
            renderDropdown(context, MinecraftClient.getInstance().textRenderer, mouseX, mouseY);
        }
    }
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Ability dropdown"));
    }
}