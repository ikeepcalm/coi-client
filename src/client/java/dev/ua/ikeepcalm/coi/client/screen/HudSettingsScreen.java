package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class HudSettingsScreen extends Screen {

    private final Screen parent;
    private HudConfig.HudSettings settings;

    private Checkbox enabledCheckbox;

    private Checkbox showKeybindsCheckbox;
    private Checkbox showAbilityNamesCheckbox;
    private Checkbox showGlowEffectCheckbox;

    private EditBox hudXField;
    private EditBox hudYOffsetField;
    private EditBox slotSizeField;
    private EditBox slotSpacingField;
    private EditBox hudScaleField;

    private Button resetButton;
    private Button presetButton;
    private int currentPreset = 0;
    private static final String[] PRESETS = {"Default", "Compact", "Large", "Minimal"};

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.hud_settings"));
        this.parent = parent;
        this.settings = new HudConfig.HudSettings();
        copySettings(HudConfig.getSettings(), this.settings);
    }

    private void copySettings(HudConfig.HudSettings from, HudConfig.HudSettings to) {
        to.enabled = from.enabled;
        to.hudX = from.hudX;
        to.hudYOffset = from.hudYOffset;
        to.slotSize = from.slotSize;
        to.slotSpacing = from.slotSpacing;
        to.showKeybinds = from.showKeybinds;
        to.showAbilityNames = from.showAbilityNames;
        to.showGlowEffect = from.showGlowEffect;
        to.hudScale = from.hudScale;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int columnSpacing = Math.min(160, this.width / 3);
        int leftColumn = centerX - columnSpacing;
        int rightColumn = centerX + Math.min(30, this.width / 20);
        int startY = Math.max(40, this.height / 12);
        int spacing = Math.max(35, this.height / 15);
        int currentY = startY;

        enabledCheckbox = Checkbox.builder(Component.translatable("screen.coi.hud_enabled"), Minecraft.getInstance().font)
                .pos(leftColumn, currentY).selected(settings.enabled)
                .maxWidth(200)
                .build();
        this.addRenderableWidget(enabledCheckbox);
        currentY += spacing;

        int sliderWidth = Math.min(160, this.width / 4);
        int fieldWidth = Math.min(60, this.width / 12);

        AbstractSliderButton hudXSlider = new AbstractSliderButton(leftColumn, currentY, sliderWidth, 20, Component.literal("X: " + settings.hudX), settings.hudX / 500.0) {
            @Override
            protected void updateMessage() {
                settings.hudX = (int) (this.value * 500);
                this.setMessage(Component.literal("X: " + settings.hudX));
                hudXField.setValue(String.valueOf(settings.hudX));
            }

            @Override
            protected void applyValue() {
                settings.hudX = (int) (this.value * 500);
                this.setMessage(Component.literal("X: " + settings.hudX));
                hudXField.setValue(String.valueOf(settings.hudX));
            }
        };
        this.addRenderableWidget(hudXSlider);

        hudXField = new EditBox(this.font, rightColumn, currentY, fieldWidth, 20, Component.translatable("screen.coi.hud_x_field"));
        hudXField.setValue(String.valueOf(settings.hudX));
        hudXField.setResponder(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.hudX = Math.clamp(value, 0, 500);
                // hudXSlider.setValue(settings.hudX / 500.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(hudXField);
        currentY += spacing;

        AbstractSliderButton hudYOffsetSlider = new AbstractSliderButton(leftColumn, currentY, sliderWidth, 20, Component.literal("Y Offset: " + settings.hudYOffset), settings.hudYOffset / 200.0) {
            @Override
            protected void updateMessage() {
                settings.hudYOffset = (int) (this.value * 200);
                this.setMessage(Component.literal("Y Offset: " + settings.hudYOffset));
                hudYOffsetField.setValue(String.valueOf(settings.hudYOffset));
            }

            @Override
            protected void applyValue() {
                settings.hudYOffset = (int) (this.value * 200);
                this.setMessage(Component.literal("Y Offset: " + settings.hudYOffset));
                hudYOffsetField.setValue(String.valueOf(settings.hudYOffset));
            }
        };
        this.addRenderableWidget(hudYOffsetSlider);

        hudYOffsetField = new EditBox(this.font, rightColumn, currentY, fieldWidth, 20, Component.translatable("screen.coi.hud_y_offset_field"));
        hudYOffsetField.setValue(String.valueOf(settings.hudYOffset));
        hudYOffsetField.setResponder(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.hudYOffset = Math.clamp(value, 0, 200);
                // hudYOffsetSlider.setValue(settings.hudYOffset / 200.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(hudYOffsetField);
        currentY += spacing;

        AbstractSliderButton slotSizeSlider = new AbstractSliderButton(leftColumn, currentY, sliderWidth, 20, Component.literal("Slot Size: " + settings.slotSize), (settings.slotSize - 20) / 80.0) {
            @Override
            protected void updateMessage() {
                settings.slotSize = 20 + (int) (this.value * 80);
                this.setMessage(Component.literal("Slot Size: " + settings.slotSize));
                slotSizeField.setValue(String.valueOf(settings.slotSize));
            }

            @Override
            protected void applyValue() {
                settings.slotSize = 20 + (int) (this.value * 80);
                this.setMessage(Component.literal("Slot Size: " + settings.slotSize));
                slotSizeField.setValue(String.valueOf(settings.slotSize));
            }
        };
        this.addRenderableWidget(slotSizeSlider);

        slotSizeField = new EditBox(this.font, rightColumn, currentY, fieldWidth, 20, Component.translatable("screen.coi.slot_size_field"));
        slotSizeField.setValue(String.valueOf(settings.slotSize));
        slotSizeField.setResponder(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.slotSize = Math.clamp(value, 20, 100);
                // slotSizeSlider.setValue((settings.slotSize - 20) / 80.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(slotSizeField);
        currentY += spacing;

        AbstractSliderButton slotSpacingSlider = new AbstractSliderButton(leftColumn, currentY, sliderWidth, 20, Component.literal("Spacing: " + settings.slotSpacing), (settings.slotSpacing - 30) / 70.0) {
            @Override
            protected void updateMessage() {
                settings.slotSpacing = 30 + (int) (this.value * 70);
                this.setMessage(Component.literal("Spacing: " + settings.slotSpacing));
                slotSpacingField.setValue(String.valueOf(settings.slotSpacing));
            }

            @Override
            protected void applyValue() {
                settings.slotSpacing = 30 + (int) (this.value * 70);
                this.setMessage(Component.literal("Spacing: " + settings.slotSpacing));
                slotSpacingField.setValue(String.valueOf(settings.slotSpacing));
            }
        };
        this.addRenderableWidget(slotSpacingSlider);

        slotSpacingField = new EditBox(this.font, rightColumn, currentY, fieldWidth, 20, Component.translatable("screen.coi.slot_spacing_field"));
        slotSpacingField.setValue(String.valueOf(settings.slotSpacing));
        slotSpacingField.setResponder(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.slotSpacing = Math.clamp(value, 30, 100);
                // slotSpacingSlider.setValue((settings.slotSpacing - 30) / 70.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(slotSpacingField);
        currentY += spacing;

        AbstractSliderButton hudScaleSlider = new AbstractSliderButton(leftColumn, currentY, sliderWidth, 20, Component.literal("Scale: " + String.format("%.1f", settings.hudScale)), (settings.hudScale - 0.5) / 1.5) {
            @Override
            protected void updateMessage() {
                settings.hudScale = 0.5f + (float) (this.value * 1.5);
                this.setMessage(Component.literal("Scale: " + String.format("%.1f", settings.hudScale)));
                hudScaleField.setValue(String.format("%.1f", settings.hudScale));
            }

            @Override
            protected void applyValue() {
                settings.hudScale = 0.5f + (float) (this.value * 1.5);
                this.setMessage(Component.literal("Scale: " + String.format("%.1f", settings.hudScale)));
                hudScaleField.setValue(String.format("%.1f", settings.hudScale));
            }
        };
        this.addRenderableWidget(hudScaleSlider);

        hudScaleField = new EditBox(this.font, rightColumn, currentY, fieldWidth, 20, Component.translatable("screen.coi.hud_scale_field"));
        hudScaleField.setValue(String.format("%.1f", settings.hudScale));
        hudScaleField.setResponder(text -> {
            try {
                float value = Float.parseFloat(text);
                settings.hudScale = Math.clamp(value, 0.5f, 2.0f);
                // hudScaleSlider.setValue((settings.hudScale - 0.5) / 1.5);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(hudScaleField);
        currentY += spacing;

        showKeybindsCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_keybinds"), Minecraft.getInstance().font)
                .pos(leftColumn, currentY)
                .maxWidth(200).onValueChange((checkbox, checked) -> settings.showKeybinds = checked).selected(settings.showKeybinds)
                .build();

        this.addRenderableWidget(showKeybindsCheckbox);
        currentY += 25;

        showAbilityNamesCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_ability_names"), Minecraft.getInstance().font)
                .pos(leftColumn, currentY)
                .maxWidth(200).onValueChange((checkbox, checked) -> settings.showAbilityNames = checked).selected(settings.showAbilityNames)
                .build();

        this.addRenderableWidget(showAbilityNamesCheckbox);
        currentY += 25;

        showGlowEffectCheckbox = Checkbox.builder(Component.translatable("screen.coi.show_glow_effect"), Minecraft.getInstance().font)
                .pos(leftColumn, currentY)
                .maxWidth(200).onValueChange((checkbox, checked) -> settings.showGlowEffect = checked).selected(settings.showGlowEffect)
                .build();

        this.addRenderableWidget(showGlowEffectCheckbox);

        int buttonY = this.height - Math.max(40, this.height / 10);
        int buttonWidth = Math.min(100, this.width / 8);
        int smallButtonWidth = Math.min(90, this.width / 9);

        presetButton = Button.builder(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]),
                button -> {
                    currentPreset = (currentPreset + 1) % PRESETS.length;
                    button.setMessage(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
                    applyPreset(currentPreset);
                }).bounds(centerX - Math.min(205, this.width / 3), buttonY, buttonWidth, 20).build();
        this.addRenderableWidget(presetButton);

        resetButton = Button.builder(Component.translatable("screen.coi.reset_defaults"),
                button -> {
                    resetToDefaults();
                    this.init();
                }).bounds(centerX - buttonWidth, buttonY, smallButtonWidth, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose()).bounds(centerX - 5, buttonY, smallButtonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> {
                    saveSettings();
                    this.onClose();
                }).bounds(centerX + Math.min(100, this.width / 10), buttonY, smallButtonWidth, 20).build());
    }

    private void applyPreset(int preset) {
        switch (preset) {
            case 0: // Default - Safe values that work on all GUI scales
                settings.hudX = 10;
                settings.hudYOffset = 60;
                settings.slotSize = 40;
                settings.slotSpacing = 50;
                settings.hudScale = 1.0f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                break;
            case 1: // Compact - Small and minimal
                settings.hudX = 5;
                settings.hudYOffset = 40;
                settings.slotSize = 30;
                settings.slotSpacing = 35;
                settings.hudScale = 0.8f;
                settings.showKeybinds = true;
                settings.showAbilityNames = false;
                settings.showGlowEffect = false;
                break;
            case 2: // Large - Bigger but still safe
                settings.hudX = 15;
                settings.hudYOffset = 80;
                settings.slotSize = 55;
                settings.slotSpacing = 65;
                settings.hudScale = 1.2f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                break;
            case 3: // Minimal - Very small and clean
                settings.hudX = 3;
                settings.hudYOffset = 30;
                settings.slotSize = 25;
                settings.slotSpacing = 30;
                settings.hudScale = 0.7f;
                settings.showKeybinds = false;
                settings.showAbilityNames = false;
                settings.showGlowEffect = false;
                break;
        }
        refreshWidgets();
    }

    private void resetToDefaults() {
        settings = new HudConfig.HudSettings();
        currentPreset = 0;
        refreshWidgets();
    }

    private void refreshWidgets() {
        hudXField.setValue(String.valueOf(settings.hudX));
        hudYOffsetField.setValue(String.valueOf(settings.hudYOffset));
        slotSizeField.setValue(String.valueOf(settings.slotSize));
        slotSpacingField.setValue(String.valueOf(settings.slotSpacing));
        hudScaleField.setValue(String.format("%.1f", settings.hudScale));

        presetButton.setMessage(Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
    }

    private void saveSettings() {
        settings.enabled = enabledCheckbox.selected();
        settings.showKeybinds = showKeybindsCheckbox.selected();
        settings.showAbilityNames = showAbilityNamesCheckbox.selected();
        settings.showGlowEffect = showGlowEffectCheckbox.selected();

        HudConfig.setSettings(settings);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int columnSpacing = Math.min(160, this.width / 3);
        int leftColumn = this.width / 2 - columnSpacing;
        int startY = Math.max(80, this.height / 8);
        int spacing = Math.max(35, this.height / 15);
        int labelY = startY + 5;

        graphics.text(this.font, Component.translatable("screen.coi.hud_x"), leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        graphics.text(this.font, Component.translatable("screen.coi.hud_y_offset"), leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        graphics.text(this.font, Component.translatable("screen.coi.slot_size"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        graphics.text(this.font, Component.translatable("screen.coi.slot_spacing"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        graphics.text(this.font, Component.translatable("screen.coi.hud_scale"),
                leftColumn, labelY - 15, 0xA0A0A0);

        if (!settings.enabled) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.hud_disabled_warning").withStyle(ChatFormatting.RED),
                    this.width / 2, this.height - 80, 0xFF5555);
        }

        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (resetButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.reset_defaults.tooltip"), mouseX, mouseY);
        } else if (presetButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.preset.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}