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
        int leftColumn = centerX - 160;
        int rightColumn = centerX + 30;
        int startY = 50;
        int spacing = 50;
        int currentY = startY;

        enabledCheckbox = CheckboxWidget.builder(Text.translatable("screen.coi.hud_enabled"), MinecraftClient.getInstance().textRenderer)
                .pos(leftColumn, currentY)
                .checked(settings.enabled)
                .maxWidth(200)
                .build();
        this.addDrawableChild(enabledCheckbox);
        currentY += spacing;

        hudXSlider = new SliderWidget(leftColumn, currentY, 160, 20,
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

        hudXField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, 60, 20,
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

        hudYOffsetSlider = new SliderWidget(leftColumn, currentY, 160, 20,
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

        hudYOffsetField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, 60, 20,
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

        slotSizeSlider = new SliderWidget(leftColumn, currentY, 160, 20,
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

        slotSizeField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, 60, 20,
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

        slotSpacingSlider = new SliderWidget(leftColumn, currentY, 160, 20,
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

        slotSpacingField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, 60, 20,
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

        hudScaleSlider = new SliderWidget(leftColumn, currentY, 160, 20,
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

        hudScaleField = new TextFieldWidget(this.textRenderer, rightColumn, currentY, 60, 20,
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

        int buttonY = this.height - 60;

        presetButton = ButtonWidget.builder(
                Text.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]),
                button -> {
                    currentPreset = (currentPreset + 1) % PRESETS.length;
                    button.setMessage(Text.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]));
                    applyPreset(currentPreset);
                }
        ).dimensions(centerX - 205, buttonY, 100, 20).build();
        this.addDrawableChild(presetButton);

        resetButton = ButtonWidget.builder(
                Text.translatable("screen.coi.reset_defaults"),
                button -> {
                    resetToDefaults();
                    this.init();
                }
        ).dimensions(centerX - 100, buttonY, 90, 20).build();
        this.addDrawableChild(resetButton);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> this.close()
        ).dimensions(centerX - 5, buttonY, 90, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> {
                    saveSettings();
                    this.close();
                }
        ).dimensions(centerX + 100, buttonY, 90, 20).build());
    }

    private void applyPreset(int preset) {
        switch (preset) {
            case 0: // Default
                settings.hudX = 20;
                settings.hudYOffset = 80;
                settings.slotSize = 50;
                settings.slotSpacing = 60;
                settings.hudScale = 1.0f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                break;
            case 1: // Compact
                settings.hudX = 10;
                settings.hudYOffset = 60;
                settings.slotSize = 35;
                settings.slotSpacing = 40;
                settings.hudScale = 0.8f;
                settings.showKeybinds = true;
                settings.showAbilityNames = false;
                settings.showGlowEffect = false;
                break;
            case 2: // Large
                settings.hudX = 30;
                settings.hudYOffset = 100;
                settings.slotSize = 70;
                settings.slotSpacing = 80;
                settings.hudScale = 1.3f;
                settings.showKeybinds = true;
                settings.showAbilityNames = true;
                settings.showGlowEffect = true;
                break;
            case 3: // Minimal
                settings.hudX = 5;
                settings.hudYOffset = 50;
                settings.slotSize = 30;
                settings.slotSpacing = 35;
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
        super.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        int leftColumn = this.width / 2 - 160;
        int startY = 100;
        int spacing = 50;
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

        super.render(context, mouseX, mouseY, delta);

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