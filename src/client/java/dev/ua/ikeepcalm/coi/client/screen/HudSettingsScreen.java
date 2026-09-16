package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * HUD settings in the mod's dark/gold card style: three tabs (Ability HUD,
 * Madness, Display) over a scrollable content card, so the screen stays usable
 * at every gui scale — rows that don't fit simply scroll. Settings edit a
 * working copy; Done persists it, Cancel discards.
 */
public class HudSettingsScreen extends Screen {

    private static final String[] PRESETS = {"Default", "Compact", "Large", "Minimal"};
    private static final String[] ANCHORS = {"TOP_LEFT", "TOP_CENTER", "BOTTOM_LEFT", "BOTTOM_CENTER"};
    private static final int TAB_H = 22;
    private static final int ROW_STRIDE = 26;
    private static final int FIELD_WIDTH = 52;

    private enum Tab {HUD, MADNESS, DISPLAY}

    /**
     * A widget inside the scrollable card with its Y offset from the viewport top.
     */
    private record ContentWidget(AbstractWidget widget, int baseY) {
    }

    private final Screen parent;
    private HudConfig.HudSettings settings;
    private Tab currentTab = Tab.HUD;
    private double scrollOffset = 0;
    private int currentPreset = 0;

    private final List<ContentWidget> contentWidgets = new ArrayList<>();
    private int rowCursor;
    private int contentX, contentW, viewportTop, viewportBottom, tabContentH, buttonY;
    private Button presetButton;
    private Button resetButton;

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
        to.wheelSlots = from.wheelSlots;
        to.activeAbilitySlots = from.activeAbilitySlots;
        to.epilepsyMode = from.epilepsyMode;
        to.showMadnessBar = from.showMadnessBar;
        to.madnessXOffset = from.madnessXOffset;
        to.madnessYOffset = from.madnessYOffset;
        to.madnessAnchor = from.madnessAnchor;
        to.effectSoundVolume = from.effectSoundVolume;
        to.enableHallucinations = from.enableHallucinations;
        to.enableDiscordPresence = from.enableDiscordPresence;
        to.presenceShowMadness = from.presenceShowMadness;
    }

    private boolean compact() {
        return this.height < 300;
    }

    private int tabY() {
        return compact() ? 18 : 30;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        contentWidgets.clear();
        rowCursor = 0;

        contentW = Math.clamp(this.width - 80, 300, 440);
        contentX = (this.width - contentW) / 2;
        viewportTop = tabY() + TAB_H + 6 + 8;

        int tabW = (contentW - 8) / 3;
        addTab(contentX, tabW, Tab.HUD, "screen.coi.settings_tab_hud", (g, x, y, size, color) -> {
            // Ability slot glyph: outlined square with a dot inside
            g.outline(x, y, size, size, color);
            g.fill(x + 3, y + 3, x + size - 3, y + size - 3, color);
        });
        addTab(contentX + tabW + 4, tabW, Tab.MADNESS, "screen.coi.settings_tab_madness", (g, x, y, size, color) -> {
            // Madness bar glyph: outlined bar, partially filled
            g.outline(x, y + 2, size, size - 4, color);
            g.fill(x + 2, y + 4, x + size - 4, y + size - 4, color);
        });
        addTab(contentX + (tabW + 4) * 2, tabW, Tab.DISPLAY, "screen.coi.settings_tab_display", (g, x, y, size, color) -> {
            // Display glyph: three stacked lines
            g.fill(x, y + 1, x + size, y + 2, color);
            g.fill(x, y + 4, x + size, y + 5, color);
            g.fill(x, y + 7, x + size - 3, y + 8, color);
        });

        switch (currentTab) {
            case MADNESS -> buildMadnessTab();
            case DISPLAY -> buildDisplayTab();
            default -> buildHudTab();
        }
        tabContentH = Math.max(0, rowCursor - (ROW_STRIDE - 20));

        // The card shrinks to fit its content on tall screens; on short ones the
        // rows scroll inside it
        viewportBottom = Math.min(this.height - (compact() ? 34 : 44), viewportTop + tabContentH);
        buttonY = Math.min(this.height - (compact() ? 24 : 30), viewportBottom + 14);

        int buttonW = (contentW - 12) / 4;
        presetButton = Button.builder(presetLabel(),
                button -> {
                    currentPreset = (currentPreset + 1) % PRESETS.length;
                    applyPreset(currentPreset);
                    this.init();
                }).bounds(contentX, buttonY, buttonW, 20).build();
        this.addRenderableWidget(presetButton);

        resetButton = Button.builder(Component.translatable("screen.coi.reset_defaults"),
                button -> {
                    settings = new HudConfig.HudSettings();
                    currentPreset = 0;
                    this.init();
                }).bounds(contentX + buttonW + 4, buttonY, buttonW, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(contentX + (buttonW + 4) * 2, buttonY, buttonW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> {
                    HudConfig.setSettings(settings);
                    this.onClose();
                }).bounds(contentX + (buttonW + 4) * 3, buttonY, buttonW, 20).build());

        applyScroll();
    }

    private Component presetLabel() {
        return Component.translatable("screen.coi.preset").append(": " + PRESETS[currentPreset]);
    }

    private void addTab(int x, int tabW, Tab tab, String labelKey, CoiTabButton.IconPainter icon) {
        this.addRenderableWidget(new CoiTabButton(x, tabY(), tabW, TAB_H,
                Component.translatable(labelKey), icon,
                () -> currentTab == tab,
                () -> {
                    currentTab = tab;
                    scrollOffset = 0;
                    this.init();
                }));
    }

    // --- Tab content ---

    private void buildHudTab() {
        addCheckboxRow(Component.translatable("screen.coi.hud_enabled"), settings.enabled,
                checked -> settings.enabled = checked);
        addIntRow(Component.translatable("screen.coi.hud_x"), Component.translatable("screen.coi.hud_x_field"),
                0, 500, settings.hudX, value -> settings.hudX = value);
        addIntRow(Component.translatable("screen.coi.hud_y_offset"), Component.translatable("screen.coi.hud_y_offset_field"),
                0, 200, settings.hudYOffset, value -> settings.hudYOffset = value);
        addIntRow(Component.translatable("screen.coi.slot_size"), Component.translatable("screen.coi.slot_size_field"),
                20, 100, settings.slotSize, value -> settings.slotSize = value);
        addIntRow(Component.translatable("screen.coi.slot_spacing"), Component.translatable("screen.coi.slot_spacing_field"),
                30, 100, settings.slotSpacing, value -> settings.slotSpacing = value);
        addDecimalRow(Component.translatable("screen.coi.hud_scale"), Component.translatable("screen.coi.hud_scale_field"),
                0.5, 2.0, settings.hudScale, value -> settings.hudScale = (float) value);
        addIntRow(Component.translatable("screen.coi.key_slots"), Component.translatable("screen.coi.key_slots_field"),
                1, CircleOfImaginationClient.MAX_ABILITIES, settings.activeAbilitySlots, value -> settings.activeAbilitySlots = value);
        addIntRow(Component.translatable("screen.coi.wheel_slots"), Component.translatable("screen.coi.wheel_slots_field"),
                2, 16, settings.wheelSlots, value -> settings.wheelSlots = value);
    }

    private void buildMadnessTab() {
        addCheckboxRow(Component.translatable("screen.coi.show_madness_bar"), settings.showMadnessBar,
                checked -> settings.showMadnessBar = checked);

        Button anchorButton = Button.builder(anchorLabel(),
                button -> {
                    int idx = 0;
                    for (int i = 0; i < ANCHORS.length; i++) {
                        if (ANCHORS[i].equalsIgnoreCase(settings.madnessAnchor)) {
                            idx = i;
                            break;
                        }
                    }
                    settings.madnessAnchor = ANCHORS[(idx + 1) % ANCHORS.length];
                    button.setMessage(anchorLabel());
                }).bounds(contentX + 10, 0, contentW - 20, 20).build();
        addContentRow(anchorButton);

        addIntRow(Component.translatable("screen.coi.madness_x_offset"), Component.translatable("screen.coi.madness_x_offset_field"),
                -500, 500, settings.madnessXOffset, value -> settings.madnessXOffset = value);
        addIntRow(Component.translatable("screen.coi.madness_y_offset"), Component.translatable("screen.coi.madness_y_offset_field"),
                0, 200, settings.madnessYOffset, value -> settings.madnessYOffset = value);
    }

    private Component anchorLabel() {
        return Component.translatable("screen.coi.madness_anchor").copy().append(": " + settings.madnessAnchor);
    }

    private void buildDisplayTab() {
        addCheckboxRow(Component.translatable("screen.coi.show_keybinds"), settings.showKeybinds,
                checked -> settings.showKeybinds = checked);
        addCheckboxRow(Component.translatable("screen.coi.show_ability_names"), settings.showAbilityNames,
                checked -> settings.showAbilityNames = checked);
        addCheckboxRow(Component.translatable("screen.coi.show_glow_effect"), settings.showGlowEffect,
                checked -> settings.showGlowEffect = checked);
        addCheckboxRow(Component.translatable("screen.coi.epilepsy_mode"), settings.epilepsyMode,
                checked -> settings.epilepsyMode = checked);
        addCheckboxRow(Component.translatable("screen.coi.enable_hallucinations"), settings.enableHallucinations,
                checked -> settings.enableHallucinations = checked);

        int initialVolume = Math.round(Math.clamp(settings.effectSoundVolume, 0f, 1f) * 100);
        AbstractSliderButton volumeSlider = new AbstractSliderButton(contentX + 10, 0, contentW - 20, 20,
                Component.translatable("screen.coi.effect_sound_volume").append(": " + initialVolume + "%"), initialVolume / 100.0) {
            @Override
            protected void updateMessage() {
                int value = (int) Math.round(this.value * 100);
                settings.effectSoundVolume = value / 100f;
                this.setMessage(Component.translatable("screen.coi.effect_sound_volume").append(": " + value + "%"));
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };
        addContentRow(volumeSlider);

        addCheckboxRow(Component.translatable("screen.coi.enable_discord_presence"), settings.enableDiscordPresence,
                checked -> settings.enableDiscordPresence = checked);
        addCheckboxRow(Component.translatable("screen.coi.presence_show_madness"), settings.presenceShowMadness,
                checked -> settings.presenceShowMadness = checked);

        addContentRow(Button.builder(Component.translatable("screen.coi.show_tour"),
                _ -> {
                    ClientStateStore.setTourCompleted(false);
                    if (this.minecraft.player != null) {
                        this.minecraft.gui.setScreen(new TourScreen());
                    }
                }).bounds(contentX + 10, 0, contentW - 20, 20).build());
    }

    // --- Row builders ---

    private void addContentRow(AbstractWidget widget) {
        contentWidgets.add(new ContentWidget(widget, rowCursor));
        this.addRenderableWidget(widget);
        rowCursor += ROW_STRIDE;
    }

    private void addCheckboxRow(Component label, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(label, Minecraft.getInstance().font)
                .pos(contentX + 10, 0)
                .maxWidth(contentW - 20)
                .onValueChange((box, checked) -> setter.accept(checked))
                .selected(selected)
                .build();
        addContentRow(checkbox);
    }

    private void addIntRow(Component label, Component fieldLabel, int min, int max, int initialValue, IntConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        int clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (double) (max - min);
        int sliderW = contentW - 20 - FIELD_WIDTH - 6;

        AbstractSliderButton slider = new AbstractSliderButton(contentX + 10, 0, sliderW, 20,
                label.copy().append(": " + clampedInitial), sliderValue) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                this.setMessage(label.copy().append(": " + value));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.valueOf(value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(this.font, contentX + 10 + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.valueOf(clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Integer.parseInt(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        contentWidgets.add(new ContentWidget(slider, rowCursor));
        this.addRenderableWidget(slider);
        addContentRow(field); // advances rowCursor for the pair
    }

    private void addDecimalRow(Component label, Component fieldLabel, double min, double max, double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);
        int sliderW = contentW - 20 - FIELD_WIDTH - 6;

        AbstractSliderButton slider = new AbstractSliderButton(contentX + 10, 0, sliderW, 20,
                label.copy().append(": " + String.format("%.1f", clampedInitial)), sliderValue) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                value = Math.round(value * 10.0) / 10.0;
                setter.accept(value);
                this.setMessage(label.copy().append(": " + String.format("%.1f", value)));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.format("%.1f", value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(this.font, contentX + 10 + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.format("%.1f", clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Double.parseDouble(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        contentWidgets.add(new ContentWidget(slider, rowCursor));
        this.addRenderableWidget(slider);
        addContentRow(field);
    }

    // --- Scrolling ---

    private double maxScroll() {
        return Math.max(0, tabContentH - (viewportBottom - viewportTop));
    }

    /**
     * Positions content widgets by scroll offset and hides the ones that don't
     * fully fit the viewport — hidden widgets neither render nor take clicks,
     * which stands in for scissor clipping of live widgets.
     */
    private void applyScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
        for (ContentWidget content : contentWidgets) {
            int y = viewportTop + content.baseY - (int) scrollOffset;
            content.widget.setY(y);
            content.widget.visible = y >= viewportTop - 2 && y + content.widget.getHeight() <= viewportBottom + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        scrollOffset = Mth.clamp(scrollOffset - verticalAmount * ROW_STRIDE, 0, maxScroll());
        applyScroll();
        return true;
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
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 55;
                settings.madnessAnchor = "TOP_LEFT";
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
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 35;
                settings.madnessAnchor = "TOP_LEFT";
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
                settings.showMadnessBar = true;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 70;
                settings.madnessAnchor = "TOP_LEFT";
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
                settings.showMadnessBar = false;
                settings.madnessXOffset = 0;
                settings.madnessYOffset = 25;
                settings.madnessAnchor = "TOP_LEFT";
                break;
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, 0x90000000);
        graphics.centeredText(this.font, this.title, this.width / 2, compact() ? 5 : 12, CoiStyle.ACCENT);

        int cardTop = tabY() + TAB_H + 6;
        CoiStyle.drawCard(graphics, contentX, cardTop, contentW, viewportBottom + 8 - cardTop);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (maxScroll() > 0) {
            int viewportH = viewportBottom - viewportTop;
            int trackX = contentX + contentW + 4;
            graphics.fill(trackX, viewportTop, trackX + 3, viewportBottom, CoiStyle.SCROLL_TRACK);
            int thumbH = Math.max(16, viewportH * viewportH / tabContentH);
            int thumbY = viewportTop + (int) ((viewportH - thumbH) * (scrollOffset / maxScroll()));
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, CoiStyle.BORDER);
        }

        if (!settings.enabled) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.hud_disabled_warning").withStyle(ChatFormatting.RED),
                    this.width / 2, buttonY - 11, 0xFFFF5555);
        }

        if (resetButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.reset_defaults.tooltip"), mouseX, mouseY);
        } else if (presetButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.preset.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
