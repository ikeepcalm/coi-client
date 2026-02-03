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
    private AbilityDropdownWidget[] abilityDropdowns;
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

        int maxAbilities = CircleOfImaginationClient.getMaxAbilities();
        abilityDropdowns = new AbilityDropdownWidget[maxAbilities];

        int centerX = this.width / 2;
        int topMargin = Math.max(60, this.height / 8);
        int spacing = Math.max(50, this.height / 12);
        int dropdownWidth = Math.min(Math.max(200, this.width / 3), this.width - 40);
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
            this.addDrawableChild(abilityDropdowns[i]);
            contentHeight += spacing;
        }

        int buttonY = Math.max(contentHeight + 20, this.height - Math.max(40, this.height / 10));

        clearAllButton = ButtonWidget.builder(
                Text.translatable("screen.coi.clear_all"),
                button -> {
                    for (int i = 0; i < maxAbilities; i++) {
                        CircleOfImaginationClient.setBoundAbility(i, null);
                    }
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

        // Render slot info for all abilities
        if (abilityDropdowns != null) {
            for (int i = 0; i < abilityDropdowns.length; i++) {
                int y = topMargin + (spacing * i) - 15;
                Text key = KeyBindingHelper.getBoundKeyOf(CircleOfImaginationClient.abilityKeys[i]).getLocalizedText();
                renderSlotInfo(context, i, centerX - dropdownWidth / 2, y, key);
            }
        }

        renderTooltips(context, mouseX, mouseY);

        renderExpandedDropdowns(context, mouseX, mouseY, delta);
    }

    private void renderExpandedDropdowns(DrawContext context, int mouseX, int mouseY, float delta) {
        if (abilityDropdowns != null) {
            for (AbilityDropdownWidget dropdown : abilityDropdowns) {
                if (dropdown != null && dropdown.isExpanded()) {
                    dropdown.renderExpanded(context, mouseX, mouseY, delta);
                }
            }
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