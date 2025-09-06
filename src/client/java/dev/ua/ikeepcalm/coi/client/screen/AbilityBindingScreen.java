package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AbilityBindingScreen extends Screen {

    private final Screen parent;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability1Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability2Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability3Dropdown;
    private ButtonWidget clearAllButton;
    private ButtonWidget settingsButton;
    private int contentHeight;

    public AbilityBindingScreen(Screen parent) {
        super(Text.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Request abilities from server when screen opens
        CircleOfImaginationClient.requestAbilitiesFromServer();
        
        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        
        // For testing purposes, add sample abilities if none are available
        if (abilities.isEmpty()) {
            System.out.println("COI Client: No abilities received from server, adding test abilities");
            CircleOfImaginationClient.addTestAbilities();
            abilities = CircleOfImaginationClient.getAvailableAbilities();
        }
        
        System.out.println("COI Client: Screen init with " + abilities.size() + " abilities available");

        int centerX = this.width / 2;
        int topMargin = Math.max(60, this.height / 8);
        int spacing = Math.max(50, this.height / 12);
        int dropdownWidth = Math.min(Math.max(200, this.width / 3), this.width - 40);
        int dropdownHeight = 20;

        contentHeight = topMargin;

        ability1Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                CircleOfImaginationClient::getAvailableAbilities,
                CircleOfImaginationClient.getBoundAbility(0),
                selected -> CircleOfImaginationClient.setBoundAbility(0, selected)
        );
        this.addDrawableChild(ability1Dropdown);
        contentHeight += spacing;

        ability2Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                CircleOfImaginationClient::getAvailableAbilities,
                CircleOfImaginationClient.getBoundAbility(1),
                selected -> CircleOfImaginationClient.setBoundAbility(1, selected)
        );
        this.addDrawableChild(ability2Dropdown);
        contentHeight += spacing;

        ability3Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                () -> CircleOfImaginationClient.getAvailableAbilities(),
                CircleOfImaginationClient.getBoundAbility(2),
                selected -> CircleOfImaginationClient.setBoundAbility(2, selected)
        );
        this.addDrawableChild(ability3Dropdown);
        contentHeight += spacing;

        int buttonY = Math.max(contentHeight + 20, this.height - Math.max(40, this.height / 10));

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

        settingsButton = ButtonWidget.builder(
                Text.translatable("screen.coi.hud_settings"),
                button -> {
                    this.close();
                    MinecraftClient.getInstance().setScreen(new HudSettingsScreen(null));
                }
        ).dimensions(Math.max(10, this.width - Math.min(130, this.width / 4)), 10, Math.min(120, this.width / 5), 20).build();

        this.addDrawableChild(settingsButton);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> this.close()
        ).dimensions(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, this.width / 2, 20, 0xFFFFFF);

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        if (abilities.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("screen.coi.no_abilities").formatted(Formatting.RED),
                    this.width / 2, 40, 0xFF5555);
        }

        int centerX = this.width / 2;
        int topMargin = Math.max(60, this.height / 8);
        int spacing = Math.max(50, this.height / 12);
        int dropdownWidth = Math.min(Math.max(200, this.width / 3), this.width - 40);

        renderSlotInfo(context, 0, centerX - dropdownWidth / 2, topMargin - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability1Key).getLocalizedText());
        renderSlotInfo(context, 1, centerX - dropdownWidth / 2, topMargin + spacing - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability2Key).getLocalizedText());
        renderSlotInfo(context, 2, centerX - dropdownWidth / 2, topMargin + spacing * 2 - 15, KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.ability3Key).getLocalizedText());

        renderTooltips(context, mouseX, mouseY);
        
        renderExpandedDropdowns(context, mouseX, mouseY, delta);
    }
    
    private void renderExpandedDropdowns(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ability1Dropdown != null && ability1Dropdown.isExpanded()) {
            ability1Dropdown.renderExpanded(context, mouseX, mouseY, delta);
        }
        if (ability2Dropdown != null && ability2Dropdown.isExpanded()) {
            ability2Dropdown.renderExpanded(context, mouseX, mouseY, delta);
        }
        if (ability3Dropdown != null && ability3Dropdown.isExpanded()) {
            ability3Dropdown.renderExpanded(context, mouseX, mouseY, delta);
        }
    }

    private void renderSlotInfo(DrawContext context, int slot, int x, int y, Text key) {
        Text label = Text.translatable("screen.coi.ability" + (slot + 1) + "_label");
        context.drawTextWithShadow(this.textRenderer, label, x, y, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer, "[" + key.getLiteralString() + "]", x + this.textRenderer.getWidth(label) + 5, y, 0xFFFF55);

        String bound = CircleOfImaginationClient.getBoundAbility(slot);
        if (bound != null && bound.contains(" - ")) {
            String abilityName = bound.split(" - ")[1];
            Text boundText = Text.literal("→ " + abilityName).formatted(Formatting.GREEN);
            int textOffset = Math.min(Math.max(100, this.width / 6), 150);
            context.drawTextWithShadow(this.textRenderer, boundText, x + textOffset, y, 0x55FF55);
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
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}