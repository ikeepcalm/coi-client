package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HudSettingsScreen extends Screen {

    private final Screen parent;
    private HudConfig.HudSettings settings;

    private CheckboxWidget enabledCheckbox;
    private SliderWidget hudXSlider;
    private SliderWidget hudYOffsetSlider;
    private SliderWidget slotSizeSlider;
    private SliderWidget slotSpacingSlider;
    private SliderWidget hudScaleSlider;
    private CheckboxWidget showKeybindsCheckbox;
    private CheckboxWidget showAbilityNamesCheckbox;
    private CheckboxWidget showGlowEffectCheckbox;

    private TextFieldWidget hudXField;
    private TextFieldWidget hudYOffsetField;
    private TextFieldWidget slotSizeField;
    private TextFieldWidget slotSpacingField;
    private TextFieldWidget hudScaleField;

    private ButtonWidget resetButton;
    private ButtonWidget presetButton;
    private int currentPreset = 0;
    private static final String[] PRESETS = {"Default", "Compact", "Large", "Minimal"};

    public HudSettingsScreen(Screen parent) {
        super(Text.translatable("screen.coi.hud_settings"));
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

        enabledCheckbox = CheckboxWidget.builder(Text.translatable("screen.coi.hud_enabled"), MinecraftClient.getInstance().textRenderer)
                .pos(leftColumn, currentY)
                .checked(settings.enabled)
                .maxWidth(200)
                .build();
        this.addDrawableChild(enabledCheckbox);
        currentY += spacing;

        int sliderWidth = Math.min(160, this.width / 4);
        int fieldWidth = Math.min(60, this.width / 12);
        
        hudXSlider = new SliderWidget(leftColumn, currentY, sliderWidth, 20,
                Text.literal("X: " + settings.hudX), settings.hudX / 500.0) {
            @Override
            protected void updateMessage() {
                settings.hudX = (int) (this.value * 500);
                this.setMessage(Text.literal("X: " + settings.hudX));
                hudXField.setText(String.valueOf(settings.hudX));
            }

            @Override
            protected void applyValue() {
                settings.hudX = (int) (this.value * 500);
                this.setMessage(Text.literal("X: " + settings.hudX));
                hudXField.setText(String.valueOf(settings.hudX));
            }
        };
        this.addDrawableChild(hudXSlider);

        hudXField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, fieldWidth, 20,
                Text.translatable("screen.coi.hud_x_field"));
        hudXField.setText(String.valueOf(settings.hudX));
        hudXField.setChangedListener(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.hudX = Math.max(0, Math.min(500, value));
                // hudXSlider.setValue(settings.hudX / 500.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(hudXField);
        currentY += spacing;

        hudYOffsetSlider = new SliderWidget(leftColumn, currentY, sliderWidth, 20,
                Text.literal("Y Offset: " + settings.hudYOffset), settings.hudYOffset / 200.0) {
            @Override
            protected void updateMessage() {
                settings.hudYOffset = (int) (this.value * 200);
                this.setMessage(Text.literal("Y Offset: " + settings.hudYOffset));
                hudYOffsetField.setText(String.valueOf(settings.hudYOffset));
            }

            @Override
            protected void applyValue() {
                settings.hudYOffset = (int) (this.value * 200);
                this.setMessage(Text.literal("Y Offset: " + settings.hudYOffset));
                hudYOffsetField.setText(String.valueOf(settings.hudYOffset));
            }
        };
        this.addDrawableChild(hudYOffsetSlider);

        hudYOffsetField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, fieldWidth, 20,
                Text.translatable("screen.coi.hud_y_offset_field"));
        hudYOffsetField.setText(String.valueOf(settings.hudYOffset));
        hudYOffsetField.setChangedListener(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.hudYOffset = Math.max(0, Math.min(200, value));
                // hudYOffsetSlider.setValue(settings.hudYOffset / 200.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(hudYOffsetField);
        currentY += spacing;

        slotSizeSlider = new SliderWidget(leftColumn, currentY, sliderWidth, 20,
                Text.literal("Slot Size: " + settings.slotSize), (settings.slotSize - 20) / 80.0) {
            @Override
            protected void updateMessage() {
                settings.slotSize = 20 + (int) (this.value * 80);
                this.setMessage(Text.literal("Slot Size: " + settings.slotSize));
                slotSizeField.setText(String.valueOf(settings.slotSize));
            }

            @Override
            protected void applyValue() {
                settings.slotSize = 20 + (int) (this.value * 80);
                this.setMessage(Text.literal("Slot Size: " + settings.slotSize));
                slotSizeField.setText(String.valueOf(settings.slotSize));
            }
        };
        this.addDrawableChild(slotSizeSlider);

        slotSizeField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, fieldWidth, 20,
                Text.translatable("screen.coi.slot_size_field"));
        slotSizeField.setText(String.valueOf(settings.slotSize));
        slotSizeField.setChangedListener(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.slotSize = Math.max(20, Math.min(100, value));
                // slotSizeSlider.setValue((settings.slotSize - 20) / 80.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(slotSizeField);
        currentY += spacing;

        slotSpacingSlider = new SliderWidget(leftColumn, currentY, sliderWidth, 20,
                Text.literal("Spacing: " + settings.slotSpacing), (settings.slotSpacing - 30) / 70.0) {
            @Override
            protected void updateMessage() {
                settings.slotSpacing = 30 + (int) (this.value * 70);
                this.setMessage(Text.literal("Spacing: " + settings.slotSpacing));
                slotSpacingField.setText(String.valueOf(settings.slotSpacing));
            }

            @Override
            protected void applyValue() {
                settings.slotSpacing = 30 + (int) (this.value * 70);
                this.setMessage(Text.literal("Spacing: " + settings.slotSpacing));
                slotSpacingField.setText(String.valueOf(settings.slotSpacing));
            }
        };
        this.addDrawableChild(slotSpacingSlider);

        slotSpacingField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, fieldWidth, 20,
                Text.translatable("screen.coi.slot_spacing_field"));
        slotSpacingField.setText(String.valueOf(settings.slotSpacing));
        slotSpacingField.setChangedListener(text -> {
            try {
                int value = Integer.parseInt(text);
                settings.slotSpacing = Math.max(30, Math.min(100, value));
                // slotSpacingSlider.setValue((settings.slotSpacing - 30) / 70.0);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(slotSpacingField);
        currentY += spacing;

        hudScaleSlider = new SliderWidget(leftColumn, currentY, sliderWidth, 20,
                Text.literal("Scale: " + String.format("%.1f", settings.hudScale)), (settings.hudScale - 0.5) / 1.5) {
            @Override
            protected void updateMessage() {
                settings.hudScale = 0.5f + (float) (this.value * 1.5);
                this.setMessage(Text.literal("Scale: " + String.format("%.1f", settings.hudScale)));
                hudScaleField.setText(String.format("%.1f", settings.hudScale));
            }

            @Override
            protected void applyValue() {
                settings.hudScale = 0.5f + (float) (this.value * 1.5);
                this.setMessage(Text.literal("Scale: " + String.format("%.1f", settings.hudScale)));
                hudScaleField.setText(String.format("%.1f", settings.hudScale));
            }
        };
        this.addDrawableChild(hudScaleSlider);

        hudScaleField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, fieldWidth, 20,
                Text.translatable("screen.coi.hud_scale_field"));
        hudScaleField.setText(String.format("%.1f", settings.hudScale));
        hudScaleField.setChangedListener(text -> {
            try {
                float value = Float.parseFloat(text);
                settings.hudScale = Math.max(0.5f, Math.min(2.0f, value));
                // hudScaleSlider.setValue((settings.hudScale - 0.5) / 1.5);
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(hudScaleField);
        currentY += spacing;

        showKeybindsCheckbox = CheckboxWidget.builder(Text.translatable("screen.coi.show_keybinds"), MinecraftClient.getInstance().textRenderer)
                .pos(leftColumn, currentY)
                .maxWidth(200)
                .callback((checkbox, checked) -> settings.showKeybinds = checked)
                .checked(settings.showKeybinds)
                .build();

        this.addDrawableChild(showKeybindsCheckbox);
        currentY += 25;

        showAbilityNamesCheckbox = CheckboxWidget.builder(Text.translatable("screen.coi.show_ability_names"), MinecraftClient.getInstance().textRenderer)
                .pos(leftColumn, currentY)
                .maxWidth(200)
                .callback((checkbox, checked) -> settings.showAbilityNames = checked)
                .checked(settings.showAbilityNames)
                .build();

        this.addDrawableChild(showAbilityNamesCheckbox);
        currentY += 25;

        showGlowEffectCheckbox = CheckboxWidget.builder(Text.translatable("screen.coi.show_glow_effect"), MinecraftClient.getInstance().textRenderer)
                .pos(leftColumn, currentY)
                .maxWidth(200)
                .callback((checkbox, checked) -> settings.showGlowEffect = checked)
                .checked(settings.showGlowEffect)
                .build();

        this.addDrawableChild(showGlowEffectCheckbox);

        int buttonY = this.height - Math.max(40, this.height / 10);
        int buttonWidth = Math.min(100, this.width / 8);
        int smallButtonWidth = Math.min(90, this.width / 9);

        presetButton = ButtonWidget.builder(
                Text.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]),
                button -> {
                    currentPreset = (currentPreset + 1) % PRESETS.length;
                    button.setMessage(Text.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
                    applyPreset(currentPreset);
                }
        ).dimensions(centerX - Math.min(205, this.width / 3), buttonY, buttonWidth, 20).build();
        this.addDrawableChild(presetButton);

        resetButton = ButtonWidget.builder(
                Text.translatable("screen.coi.reset_defaults"),
                button -> {
                    resetToDefaults();
                    this.init();
                }
        ).dimensions(centerX - buttonWidth, buttonY, smallButtonWidth, 20).build();
        this.addDrawableChild(resetButton);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> this.close()
        ).dimensions(centerX - 5, buttonY, smallButtonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> {
                    saveSettings();
                    this.close();
                }
        ).dimensions(centerX + Math.min(100, this.width / 10), buttonY, smallButtonWidth, 20).build());
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
        hudXField.setText(String.valueOf(settings.hudX));
        hudYOffsetField.setText(String.valueOf(settings.hudYOffset));
        slotSizeField.setText(String.valueOf(settings.slotSize));
        slotSpacingField.setText(String.valueOf(settings.slotSpacing));
        hudScaleField.setText(String.format("%.1f", settings.hudScale));

        presetButton.setMessage(Text.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
    }

    private void saveSettings() {
        settings.enabled = enabledCheckbox.isChecked();
        settings.showKeybinds = showKeybindsCheckbox.isChecked();
        settings.showAbilityNames = showAbilityNamesCheckbox.isChecked();
        settings.showGlowEffect = showGlowEffectCheckbox.isChecked();

        HudConfig.setSettings(settings);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        int columnSpacing = Math.min(160, this.width / 3);
        int leftColumn = this.width / 2 - columnSpacing;
        int startY = Math.max(80, this.height / 8);
        int spacing = Math.max(35, this.height / 15);
        int labelY = startY + 5;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.coi.hud_x"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.coi.hud_y_offset"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.coi.slot_size"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.coi.slot_spacing"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.coi.hud_scale"),
                leftColumn, labelY - 15, 0xA0A0A0);
        labelY += spacing;

        if (!settings.enabled) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("screen.coi.hud_disabled_warning").formatted(Formatting.RED),
                    this.width / 2, this.height - 80, 0xFF5555);
        }

        renderTooltips(context, mouseX, mouseY);
    }

    private void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        if (resetButton.isHovered()) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("screen.coi.reset_defaults.tooltip"), mouseX, mouseY);
        } else if (presetButton.isHovered()) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("screen.coi.preset.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}