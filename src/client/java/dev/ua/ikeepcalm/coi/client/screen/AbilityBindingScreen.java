package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AbilityBindingScreen extends Screen {

    private final Screen parent;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability1Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability2Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability3Dropdown;
    private ButtonWidget clearAllButton;
    private int contentHeight;

    public AbilityBindingScreen(Screen parent) {
        super(Text.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();

        int centerX = this.width / 2;
        int topMargin = 60;
        int spacing = 60;
        int dropdownWidth = Math.min(300, this.width - 40);
        int dropdownHeight = 20;

        contentHeight = topMargin;

        ability1Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                abilities,
                CircleOfImaginationClient.getBoundAbility(0),
                selected -> CircleOfImaginationClient.setBoundAbility(0, selected)
        );
        this.addDrawableChild(ability1Dropdown);
        contentHeight += spacing;

        ability2Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                abilities,
                CircleOfImaginationClient.getBoundAbility(1),
                selected -> CircleOfImaginationClient.setBoundAbility(1, selected)
        );
        this.addDrawableChild(ability2Dropdown);
        contentHeight += spacing;

        ability3Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                abilities,
                CircleOfImaginationClient.getBoundAbility(2),
                selected -> CircleOfImaginationClient.setBoundAbility(2, selected)
        );
        this.addDrawableChild(ability3Dropdown);
        contentHeight += spacing;

        int buttonY = Math.max(contentHeight + 20, this.height - 60);

        clearAllButton = ButtonWidget.builder(
                Text.translatable("screen.coi.clear_all"),
                button -> {
                    CircleOfImaginationClient.setBoundAbility(0, null);
                    CircleOfImaginationClient.setBoundAbility(1, null);
                    CircleOfImaginationClient.setBoundAbility(2, null);
                    this.init();
                }
        ).dimensions(centerX - 105, buttonY, 100, 20).build();
        this.addDrawableChild(clearAllButton);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> this.close()
        ).dimensions(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, this.width / 2, 20, 0xFFFFFF);

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        if (abilities.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("screen.coi.no_abilities").formatted(Formatting.RED),
                    this.width / 2, 40, 0xFF5555);
        }

        int centerX = this.width / 2;
        int topMargin = 60;
        int spacing = 60;
        int dropdownWidth = Math.min(300, this.width - 40);

        renderSlotInfo(context, 0, centerX - dropdownWidth / 2, topMargin - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability1Key).getLocalizedText());
        renderSlotInfo(context, 1, centerX - dropdownWidth / 2, topMargin + spacing - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability2Key).getLocalizedText());
        renderSlotInfo(context, 2, centerX - dropdownWidth / 2, topMargin + spacing * 2 - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability3Key).getLocalizedText());

        super.render(context, mouseX, mouseY, delta);

        renderTooltips(context, mouseX, mouseY);
    }

    private void renderSlotInfo(DrawContext context, int slot, int x, int y, Text key) {
        Text label = Text.translatable("screen.coi.ability" + (slot + 1) + "_label");
        context.drawTextWithShadow(this.textRenderer, label, x, y, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer, "[" + key.getLiteralString() + "]", x + this.textRenderer.getWidth(label) + 5, y, 0xFFFF55);

        String bound = CircleOfImaginationClient.getBoundAbility(slot);
        if (bound != null && bound.contains(" - ")) {
            String abilityName = bound.split(" - ")[1];
            Text boundText = Text.literal("→ " + abilityName).formatted(Formatting.GREEN);
            context.drawTextWithShadow(this.textRenderer, boundText, x + 150, y, 0x55FF55);
        }
    }

    private void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        if (clearAllButton.isHovered()) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("screen.coi.clear_all.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}