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
    private AbilityDropdownWidget[] wheelDropdowns;
    private Button clearAllButton;
    private Button settingsButton;
    private Button modeToggleButton;
    private int contentHeight;
    private boolean showingWheel = false;

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 6;

    public AbilityBindingScreen(Screen parent) {
        super(Component.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        // Request abilities from server when screen opens
        CircleOfImaginationClient.requestAbilitiesFromServer();

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();

        // For testing purposes, add sample abilities if none are available
        if (abilities.isEmpty()) {
            System.out.println("COI Client: No abilities received from server, adding test abilities");
            CircleOfImaginationClient.addTestAbilities();
            abilities = CircleOfImaginationClient.getAvailableAbilities();
        }

        int maxAbilities = CircleOfImaginationClient.getMaxAbilities();
        int wheelSize = CircleOfImaginationClient.getWheelSize();
        int totalItems = showingWheel ? wheelSize : maxAbilities;
        int totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        abilityDropdowns = new AbilityDropdownWidget[maxAbilities];
        wheelDropdowns = new AbilityDropdownWidget[wheelSize];

        int centerX = this.width / 2;
        int topMargin = 60; 
        int spacing = 40;
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);
        int dropdownHeight = 20;

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

        // Create dropdowns for current page
        for (int i = startIdx; i < endIdx; i++) {
            final int slot = i;
            int y = topMargin + ((i - startIdx) * spacing);
            
            AbilityDropdownWidget dropdown = new AbilityDropdownWidget(
                    centerX - dropdownWidth / 2, y, dropdownWidth, dropdownHeight,
                    CircleOfImaginationClient::getAvailableAbilities,
                    showingWheel ? CircleOfImaginationClient.getWheelAbility(slot) : CircleOfImaginationClient.getBoundAbility(slot),
                    selected -> {
                        if (showingWheel) CircleOfImaginationClient.setWheelAbility(slot, selected);
                        else CircleOfImaginationClient.setBoundAbility(slot, selected);
                    }
            );
            
            if (showingWheel) wheelDropdowns[slot] = dropdown;
            else abilityDropdowns[slot] = dropdown;
            
            this.addRenderableWidget(dropdown);
        }

        int buttonY = this.height - 35;

        // Pagination buttons
        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                currentPage--;
                this.init();
            }).bounds(centerX - 180, buttonY - 25, 20, 20).build()).active = currentPage > 0;

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                currentPage++;
                this.init();
            }).bounds(centerX + 160, buttonY - 25, 20, 20).build()).active = currentPage < totalPages - 1;
            
            // Page indicator text handled in render
        }

        modeToggleButton = Button.builder(Component.translatable(showingWheel ? "screen.coi.show_keybinds_mode" : "screen.coi.show_wheel_mode"),
                button -> {
                    showingWheel = !showingWheel;
                    currentPage = 0;
                    this.init();
                }).bounds(centerX - 155, buttonY - 25, 310, 20).build();
        this.addRenderableWidget(modeToggleButton);

        clearAllButton = Button.builder(Component.translatable("screen.coi.clear_all"),
                button -> {
                    if (showingWheel) {
                        for (int i = 0; i < wheelSize; i++) {
                            CircleOfImaginationClient.setWheelAbility(i, null);
                        }
                    } else {
                        for (int i = 0; i < maxAbilities; i++) {
                            CircleOfImaginationClient.setBoundAbility(i, null);
                        }
                    }
                    this.init();
                }).bounds(centerX - 105, buttonY, 100, 20).build();
        this.addRenderableWidget(clearAllButton);

        settingsButton = Button.builder(Component.translatable("screen.coi.hud_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().setScreen(new HudSettingsScreen(null));
                }).bounds(this.width - 130, 10, 120, 20).build();

        this.addRenderableWidget(settingsButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(centerX + 5, buttonY, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalItems = showingWheel ? CircleOfImaginationClient.getWheelSize() : CircleOfImaginationClient.getMaxAbilities();
        int totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        
        if (verticalAmount > 0 && currentPage > 0) {
            currentPage--;
            this.init();
            return true;
        } else if (verticalAmount < 0 && currentPage < totalPages - 1) {
            currentPage++;
            this.init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Draw background and header info BEFORE super call (which renders widgets)
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.centeredText(this.font,
                this.title, this.width / 2, 10, 0xFFFFFF);
        
        graphics.centeredText(this.font, 
                Component.translatable(showingWheel ? "screen.coi.wheel_bindings" : "screen.coi.key_bindings"),
                this.width / 2, 25, 0xAAAAAA);

        int totalItems = showingWheel ? CircleOfImaginationClient.getWheelSize() : CircleOfImaginationClient.getMaxAbilities();
        int totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages > 1) {
            graphics.centeredText(this.font, Component.literal((currentPage + 1) + " / " + totalPages), this.width / 2, this.height - 55, 0xFFFFFF);
        }

        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();
        if (abilities.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.no_abilities").withStyle(ChatFormatting.RED),
                    this.width / 2, 40, 0xFF5555);
        }

        int centerX = this.width / 2;
        int topMargin = 60;
        int spacing = 40;
        int dropdownWidth = Math.clamp(this.width / 3, 200, this.width - 40);

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

        for (int i = startIdx; i < endIdx; i++) {
            int y = topMargin + ((i - startIdx) * spacing) - 15;
            if (showingWheel) {
                renderSlotInfo(graphics, i, centerX - dropdownWidth / 2, y, Component.literal(String.valueOf(i + 1)), true);
            } else {
                Component key = KeyMappingHelper.getBoundKeyOf(CircleOfImaginationClient.abilityKeys[i]).getDisplayName();
                renderSlotInfo(graphics, i, centerX - dropdownWidth / 2, y, key, false);
            }
        }

        // Render widgets (buttons and dropdowns)
        super.extractRenderState(graphics, mouseX, mouseY, a);

        renderTooltips(graphics, mouseX, mouseY);

        renderExpandedDropdowns(graphics, mouseX, mouseY, a);
    }

    private void renderExpandedDropdowns(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        AbilityDropdownWidget[] current = showingWheel ? wheelDropdowns : abilityDropdowns;
        if (current != null) {
            for (AbilityDropdownWidget dropdown : current) {
                if (dropdown != null && dropdown.isExpanded()) {
                    dropdown.renderExpanded(graphics, mouseX, mouseY, delta);
                }
            }
        }
    }

    private void renderSlotInfo(GuiGraphicsExtractor graphics, int slot, int x, int y, Component key, boolean isWheel) {
        Component label = Component.translatable(isWheel ? "screen.coi.wheel_slot" : "screen.coi.ability" + (slot + 1) + "_label");
        if (isWheel) label = label.copy().append(" " + (slot + 1));
        
        graphics.text(this.font, label, x, y, 0xA0A0A0);

        if (!isWheel) {
            graphics.text(this.font, "[" + key.tryCollapseToString() + "]", x + this.font.width(label) + 5, y, 0xFFFF55);
        }

        String bound = isWheel ? CircleOfImaginationClient.getWheelAbility(slot) : CircleOfImaginationClient.getBoundAbility(slot);
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