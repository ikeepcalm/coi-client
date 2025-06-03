package dev.ua.ikeepcalm.coi.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class AbilityDropdownWidget extends ClickableWidget {

    private final List<String> options;
    private final Consumer<String> onSelect;
    private String selected;
    private boolean expanded = false;
    private int hoveredIndex = -1;

    public AbilityDropdownWidget(int x, int y, int width, int height,
                                 List<String> options, String currentSelection,
                                 Consumer<String> onSelect) {
        super(x, y, width, height, Text.empty());
        this.options = options;
        this.selected = currentSelection;
        this.onSelect = onSelect;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int borderColor = this.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, borderColor);

        String displayText = selected != null ? selected : "Select Ability";
        context.drawText(textRenderer, displayText,
                this.getX() + 4, this.getY() + (this.height - 8) / 2, 0xFFFFFF, false);

        String arrow = expanded ? "▲" : "▼";
        context.drawText(textRenderer, arrow,
                this.getX() + this.width - 12, this.getY() + (this.height - 8) / 2, 0xFFFFFF, false);

        if (expanded) {
            int dropdownY = this.getY() + this.height;
            int dropdownHeight = Math.min(options.size() * 20, 100);

            context.fill(this.getX(), dropdownY,
                    this.getX() + this.width, dropdownY + dropdownHeight, 0xFF000000);
            context.drawBorder(this.getX(), dropdownY, this.width, dropdownHeight, 0xFFFFFFFF);

            int itemY = dropdownY;
            hoveredIndex = -1;

            for (int i = 0; i < options.size(); i++) {
                if (itemY + 20 > dropdownY + dropdownHeight) break;

                boolean isHovered = mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                        mouseY >= itemY && mouseY < itemY + 20;

                if (isHovered) {
                    context.fill(this.getX() + 1, itemY,
                            this.getX() + this.width - 1, itemY + 20, 0xFF404040);
                    hoveredIndex = i;
                }

                context.drawText(textRenderer, options.get(i),
                        this.getX() + 4, itemY + 6, 0xFFFFFF, false);

                itemY += 20;
            }
        }
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
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (expanded) {
            int dropdownHeight = Math.min(options.size() * 20, 100);
            return mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                    mouseY >= this.getY() && mouseY < this.getY() + this.height + dropdownHeight;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Ability dropdown"));
    }
}