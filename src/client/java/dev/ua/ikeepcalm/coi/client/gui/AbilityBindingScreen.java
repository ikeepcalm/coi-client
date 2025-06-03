package dev.ua.ikeepcalm.coi.client.gui;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class AbilityBindingScreen extends Screen {

    private final Screen parent;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability1Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability2Dropdown;
    private dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget ability3Dropdown;

    public AbilityBindingScreen(Screen parent) {
        super(Text.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<String> abilities = CircleOfImaginationClient.getAvailableAbilities();

        int centerX = this.width / 2;
        int startY = this.height / 4;

        ability1Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - 100, startY, 200, 20,
                abilities,
                CircleOfImaginationClient.getBoundAbility(0),
                selected -> CircleOfImaginationClient.setBoundAbility(0, selected)
        );
        this.addDrawableChild(ability1Dropdown);

        ability2Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - 100, startY + 40, 200, 20,
                abilities,
                CircleOfImaginationClient.getBoundAbility(1),
                selected -> CircleOfImaginationClient.setBoundAbility(1, selected)
        );
        this.addDrawableChild(ability2Dropdown);

        ability3Dropdown = new dev.ua.ikeepcalm.coi.client.screen.AbilityDropdownWidget(
                centerX - 100, startY + 80, 200, 20,
                abilities,
                CircleOfImaginationClient.getBoundAbility(2),
                selected -> CircleOfImaginationClient.setBoundAbility(2, selected)
        );
        this.addDrawableChild(ability3Dropdown);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 40, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = this.height / 4;

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability1_label"),
                centerX - 100, startY - 12, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability2_label"),
                centerX - 100, startY + 28, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability3_label"),
                centerX - 100, startY + 68, 0xA0A0A0);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}