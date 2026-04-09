package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class AbilityBindingScreen extends Screen {

    private final Screen parent;
    private AbilityDropdownWidget[] abilityDropdowns;
    private Button clearAllButton;
    private Button settingsButton;
    private int contentHeight;

    public AbilityBindingScreen(Screen parent) {
        super(Component.translatable("screen.coi.ability_binding"));
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

        int maxAbilities = CircleOfImaginationClient.getMaxAbilities();
        abilityDropdowns = new AbilityDropdownWidget[maxAbilities];

        int centerX = this.width / 2;
        int topMargin = Math.max(60, this.height / 8);
        int spacing = Math.max(50, this.height / 12);
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);
        int dropdownHeight = 20;

        contentHeight = topMargin;

        // Create dropdowns dynamically
        for (int i = 0; i < maxAbilities; i++) {
            final int slot = i;
            abilityDropdowns[i] = new AbilityDropdownWidget(
                    centerX - dropdownWidth / 2, contentHeight, dropdownWidth, dropdownHeight,
                    CircleOfImaginationClient::getAvailableAbilities,
                    CircleOfImaginationClient.getBoundAbility(slot),
                    selected -> CircleOfImaginationClient.setBoundAbility(slot, selected)
            );
            this.addRenderableWidget(abilityDropdowns[i]);
            contentHeight += spacing;
        }

        int buttonY = Math.max(contentHeight + 20, this.height - Math.max(40, this.height / 10));

        clearAllButton = Button.builder(Component.translatable("screen.coi.clear_all"),
                button -> {
                    for (int i = 0; i < maxAbilities; i++) {
                        CircleOfImaginationClient.setBoundAbility(i, null);
                    }
                    this.init();
                }).bounds(centerX - 105, buttonY, 100, 20).build();
        this.addRenderableWidget(clearAllButton);

        settingsButton = Button.builder(Component.translatable("screen.coi.hud_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().setScreen(new HudSettingsScreen(null));
                }).bounds(Math.max(10, this.width - Math.min(130, this.width / 4)), 10, Math.min(120, this.width / 5), 20).build();

        this.addRenderableWidget(settingsButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font,
                this.title, this.width / 2, 20, 0xFFFFFF);

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        if (abilities.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.no_abilities").withStyle(ChatFormatting.RED),
                    this.width / 2, 40, 0xFF5555);
        }

        int centerX = this.width / 2;
        int topMargin = Math.max(60, this.height / 8);
        int spacing = Math.max(50, this.height / 12);
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);

        // Render slot info for all abilities
        if (abilityDropdowns != null) {
            for (int i = 0; i < abilityDropdowns.length; i++) {
                int y = topMargin + (spacing * i) - 15;
                Component key = KeyMappingHelper.getBoundKeyOf(CircleOfImaginationClient.abilityKeys[i]).getDisplayName();
                renderSlotInfo(graphics, i, centerX - dropdownWidth / 2, y, key);
            }
        }

        renderTooltips(graphics, mouseX, mouseY);

        renderExpandedDropdowns(graphics, mouseX, mouseY, a);
    }

    private void renderExpandedDropdowns(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (abilityDropdowns != null) {
            for (AbilityDropdownWidget dropdown : abilityDropdowns) {
                if (dropdown != null && dropdown.isExpanded()) {
                    dropdown.renderExpanded(graphics, mouseX, mouseY, delta);
                }
            }
        }
    }

    private void renderSlotInfo(GuiGraphicsExtractor graphics, int slot, int x, int y, Component key) {
        Component label = Component.translatable("screen.coi.ability" + (slot + 1) + "_label");
        graphics.text(this.font, label, x, y, 0xA0A0A0);

        graphics.text(this.font, "[" + key.tryCollapseToString() + "]", x + this.font.width(label) + 5, y, 0xFFFF55);

        String bound = CircleOfImaginationClient.getBoundAbility(slot);
        if (bound != null && bound.contains(" - ")) {
            String abilityName = bound.split(" - ")[1];
            Component boundText = Component.literal("→ " + abilityName).withStyle(ChatFormatting.GREEN);
            int textOffset = Math.clamp(this.width / 6, 100, 150);
            graphics.text(this.font, boundText, x + textOffset, y, 0x55FF55);
        }
    }

    private void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (clearAllButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.clear_all.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}