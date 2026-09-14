package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudElements;
import dev.ua.ikeepcalm.coi.util.CoiIcons;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
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
 * HUD settings in the mod's dark/gold card style, over a scrollable content
 * card so the screen stays usable at every gui scale.
 * <p>
 * Three tabs, split by what a setting <em>is</em> rather than by which overlay
 * happens to draw it:
 * <ul>
 *   <li><b>Ability HUD</b> — everything about the ability slots themselves.</li>
 *   <li><b>Elements</b> — one uniform block per bar/overlay: show it, scale it,
 *       align it, plus the handful of knobs that only that element has.</li>
 *   <li><b>General</b> — the master switch, accessibility, sound, integrations.</li>
 * </ul>
 * Placement lives <em>only</em> in {@link HudLayoutScreen}: every positional
 * control here is an <em>Align</em> button that opens the editor on this
 * screen's working copy, so Done still decides whether anything persists.
 */
public class HudSettingsScreen extends Screen {

    private static final int TAB_H = 22;
    private static final int ROW_STRIDE = 26;
    private static final int FIELD_WIDTH = 52;
    private static final int ALIGN_W = 60;
    /**
     * Left inset of a normal row, and of a row that belongs to the element
     * above it.
     */
    private static final int INDENT = 10;
    private static final int SUB_INDENT = 22;

    private enum Tab {HUD, ELEMENTS, GENERAL}

    /**
     * A widget inside the scrollable card with its Y offset from the viewport top.
     */
    private record ContentWidget(AbstractWidget widget, int baseY) {
    }

    private final Screen parent;
    private HudConfig.HudSettings settings;
    private Tab currentTab = Tab.HUD;
    private double scrollOffset = 0;

    private final List<ContentWidget> contentWidgets = new ArrayList<>();
    private int rowCursor;
    private int contentX, contentW, viewportTop, viewportBottom, tabContentH, buttonY;
    private Button resetButton;

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.hud_settings"));
        this.parent = parent;
        this.settings = new HudConfig.HudSettings();
        HudConfig.copySettings(HudConfig.getSettings(), this.settings);
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

        int arrangeW = Math.min(150, contentW / 2);
        int arrangeH = compact() ? 14 : 16;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.layout_open"),
                        b -> openLayout(null))
                .bounds(contentX + contentW - arrangeW, compact() ? 2 : 8, arrangeW, arrangeH).build());

        int tabW = (contentW - 8) / 3;
        addTab(contentX, tabW, Tab.HUD, "screen.coi.settings_tab_hud", (g, x, y, size, color) -> {
            // Ability slot glyph: outlined square with a dot inside
            g.outline(x, y, size, size, color);
            g.fill(x + 3, y + 3, x + size - 3, y + size - 3, color);
        });
        addTab(contentX + tabW + 4, tabW, Tab.ELEMENTS, "screen.coi.settings_tab_elements", (g, x, y, size, color) -> {
            // Elements glyph: a bar above a smaller card
            g.outline(x, y, size, size / 2, color);
            g.fill(x, y + size / 2 + 2, x + size - 2, y + size, color);
        });
        addTab(contentX + (tabW + 4) * 2, tabW, Tab.GENERAL, "screen.coi.settings_tab_general", (g, x, y, size, color) -> {
            // Sliders glyph: two tracks with handles at different positions.
            // Deliberately drawn rather than blitted — the cog artwork is
            // detailed 64px art and turns to mush in a 9px tab slot
            g.fill(x, y + 1, x + size, y + 2, color);
            g.fill(x + size - 4, y, x + size - 2, y + 3, color);
            g.fill(x, y + 6, x + size, y + 7, color);
            g.fill(x + 2, y + 5, x + 4, y + 8, color);
        });

        switch (currentTab) {
            case ELEMENTS -> buildElementsTab();
            case GENERAL -> buildGeneralTab();
            default -> buildHudTab();
        }
        tabContentH = Math.max(0, rowCursor - (ROW_STRIDE - 20));

        // The card shrinks to fit its content on tall screens; on short ones the
        // rows scroll inside it
        viewportBottom = Math.min(this.height - (compact() ? 34 : 44), viewportTop + tabContentH);
        buttonY = Math.min(this.height - (compact() ? 24 : 30), viewportBottom + 14);

        int buttonW = (contentW - 8) / 3;
        resetButton = Button.builder(Component.translatable("screen.coi.reset_defaults"),
                button -> {
                    settings = new HudConfig.HudSettings();
                    this.init();
                }).bounds(contentX, buttonY, buttonW, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(contentX + buttonW + 4, buttonY, buttonW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> {
                    HudConfig.setSettings(settings);
                    this.onClose();
                }).bounds(contentX + (buttonW + 4) * 2, buttonY, buttonW, 20).build());

        applyScroll();
    }

    /**
     * Hands the working copy to the layout editor, so arranging and the rows
     * on this screen edit the same settings and Done still decides.
     */
    private void openLayout(String elementId) {
        this.minecraft.gui.setScreen(new HudLayoutScreen(this, elementId, settings));
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
        addHeaderRow(Component.translatable("screen.coi.layout_el_ability_slots"), HudElements.ABILITY_SLOTS);
        // The single size knob: it scales the whole slot, and the row's spacing
        // follows it, so there is nothing left for a separate scale or spacing
        // slider to disagree about
        addIntRow(INDENT, Component.translatable("screen.coi.slot_size"), Component.translatable("screen.coi.slot_size_field"),
                HudConfig.MIN_SLOT_SIZE, HudConfig.MAX_SLOT_SIZE, settings.slotSize, value -> settings.slotSize = value);
        addIntRow(INDENT, Component.translatable("screen.coi.key_slots"), Component.translatable("screen.coi.key_slots_field"),
                1, CircleOfImaginationClient.MAX_ABILITIES, settings.activeAbilitySlots, value -> settings.activeAbilitySlots = value);
        addIntRow(INDENT, Component.translatable("screen.coi.wheel_slots"), Component.translatable("screen.coi.wheel_slots_field"),
                2, 16, settings.wheelSlots, value -> settings.wheelSlots = value);

        addHeaderRow(Component.translatable("screen.coi.slot_decoration_section"));
        addCheckboxRow(INDENT, Component.translatable("screen.coi.show_keybinds"), settings.showKeybinds,
                checked -> settings.showKeybinds = checked);
        addCheckboxRow(INDENT, Component.translatable("screen.coi.show_ability_names"), settings.showAbilityNames,
                checked -> settings.showAbilityNames = checked);
        addCheckboxRow(INDENT, Component.translatable("screen.coi.show_glow_effect"), settings.showGlowEffect,
                checked -> settings.showGlowEffect = checked);
    }

    /**
     * Every bar and overlay in the same shape — a show/hide checkbox carrying
     * the element's own name, an Align button, and, <em>only while it is
     * switched on</em>, its scale slider and whatever knobs it alone has.
     * <p>
     * Collapsing the switched-off ones keeps the tab readable: a player who
     * hides half the HUD should see a short list of what is left, not eight
     * blocks of controls that do nothing.
     */
    private void buildElementsTab() {
        addHintRow(Component.translatable("screen.coi.elements_hint"));

        // The plate absorbs sanity, acting and the reserve meters, so while it
        // is on those three get no rows at all — showing controls for a bar the
        // plate has taken over is exactly the clutter this was meant to end.
        // Spirituality is deliberately not part of that trade.
        if (addElementRow(HudElements.CHARACTER_PLATE, settings.showCharacterPlate,
                checked -> settings.showCharacterPlate = checked)) {
            addScaleRow(settings.characterPlateScale, value -> settings.characterPlateScale = value);
            addHintRow(Component.translatable("screen.coi.plate_supersedes"));
        }

        // Not part of the plate's either/or: this one replaces the vanilla
        // hearts rather than another COI bar, so it stands on its own
        if (addElementRow(HudElements.BEYONDER_HEALTH, settings.showBeyonderHealth,
                checked -> settings.showBeyonderHealth = checked)) {
            addScaleRow(settings.beyonderHealthScale, value -> settings.beyonderHealthScale = value);
            addHintRow(Component.translatable("screen.coi.health_supersedes"));
        }

        if (!settings.showCharacterPlate
                && addElementRow(HudElements.MADNESS, settings.showMadnessBar, checked -> settings.showMadnessBar = checked)) {
            addScaleRow(settings.madnessScale, value -> settings.madnessScale = value);
        }

        if (addElementRow(HudElements.SPIRITUALITY, settings.showSpiritualityBar, checked -> settings.showSpiritualityBar = checked)) {
            addScaleRow(settings.spiritualityScale, value -> settings.spiritualityScale = value);
            addCheckboxRow(SUB_INDENT, Component.translatable("screen.coi.spirituality_hide_when_full"),
                    settings.spiritualityHideWhenFull, checked -> settings.spiritualityHideWhenFull = checked);
        }

        if (!settings.showCharacterPlate
                && addElementRow(HudElements.ACTING, settings.showActingBar, checked -> settings.showActingBar = checked)) {
            addScaleRow(settings.actingScale, value -> settings.actingScale = value);
        }

        if (settings.showCharacterPlate) {
            // The plate still draws the reserve rows, so their cap stays reachable
            addIntRow(SUB_INDENT, Component.translatable("screen.coi.resource_max_bars"),
                    Component.translatable("screen.coi.resource_max_bars_field"),
                    1, 8, settings.resourceMaxBars, value -> settings.resourceMaxBars = value);
        } else if (addElementRow(HudElements.RESOURCES, settings.showResourceBars, checked -> settings.showResourceBars = checked)) {
            addScaleRow(settings.resourceScale, value -> settings.resourceScale = value);
            addIntRow(SUB_INDENT, Component.translatable("screen.coi.resource_max_bars"),
                    Component.translatable("screen.coi.resource_max_bars_field"),
                    1, 8, settings.resourceMaxBars, value -> settings.resourceMaxBars = value);
        }

        if (addElementRow(HudElements.ACTION_BAR, settings.showActionBar, checked -> settings.showActionBar = checked)) {
            addScaleRow(settings.actionBarScale, value -> settings.actionBarScale = value);
            addIntRow(SUB_INDENT, Component.translatable("screen.coi.action_bar_lines"),
                    Component.translatable("screen.coi.action_bar_lines_field"),
                    0, 4, settings.actionBarLines, value -> settings.actionBarLines = value);
        }

        if (addElementRow(HudElements.TARGET_HEALTH, settings.showTargetHealth, checked -> settings.showTargetHealth = checked)) {
            addScaleRow(settings.targetHealthScale, value -> settings.targetHealthScale = value);
        }

        if (addElementRow(HudElements.COGITATION, settings.showCogitationOverlay, checked -> settings.showCogitationOverlay = checked)) {
            addScaleRow(settings.cogitationScale, value -> settings.cogitationScale = value);
        }

        if (addElementRow(HudElements.NOTIFICATIONS, settings.showNotifications, checked -> settings.showNotifications = checked)) {
            addScaleRow(settings.notificationScale, value -> settings.notificationScale = value);
        }
    }

    private void buildGeneralTab() {
        addCheckboxRow(INDENT, Component.translatable("screen.coi.hud_enabled"), settings.enabled,
                checked -> settings.enabled = checked);

        addCheckboxRow(INDENT, Component.translatable("screen.coi.menu_use_server"), settings.useServerMenus,
                checked -> settings.useServerMenus = checked);
        addHintRow(Component.translatable("screen.coi.menu_use_server_hint"));

        addHeaderRow(Component.translatable("screen.coi.accessibility_section"));
        addCheckboxRow(INDENT, Component.translatable("screen.coi.epilepsy_mode"), settings.epilepsyMode,
                checked -> settings.epilepsyMode = checked);
        addCheckboxRow(INDENT, Component.translatable("screen.coi.enable_hallucinations"), settings.enableHallucinations,
                checked -> settings.enableHallucinations = checked);

        int initialVolume = Math.round(Math.clamp(settings.effectSoundVolume, 0f, 1f) * 100);
        AbstractSliderButton volumeSlider = new AbstractSliderButton(contentX + INDENT, 0, contentW - INDENT * 2, 20,
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

        addHeaderRow(Component.translatable("screen.coi.integrations_section"));
        addCheckboxRow(INDENT, Component.translatable("screen.coi.enable_discord_presence"), settings.enableDiscordPresence,
                checked -> settings.enableDiscordPresence = checked);
        addCheckboxRow(INDENT, Component.translatable("screen.coi.presence_show_madness"), settings.presenceShowMadness,
                checked -> settings.presenceShowMadness = checked);

        addHeaderRow(Component.translatable("screen.coi.help_section"));
        addContentRow(Button.builder(Component.translatable("screen.coi.show_tour"),
                _ -> {
                    ClientStateStore.setTourCompleted(false);
                    if (this.minecraft.player != null) {
                        this.minecraft.gui.setScreen(new TourScreen());
                    }
                }).bounds(contentX + INDENT, 0, contentW - INDENT * 2, 20).build());
    }

    // --- Row builders ---

    private void addContentRow(AbstractWidget widget) {
        contentWidgets.add(new ContentWidget(widget, rowCursor));
        this.addRenderableWidget(widget);
        rowCursor += ROW_STRIDE;
    }

    private void addHeaderRow(Component label) {
        addContentRow(new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 20, label, this.font));
    }

    /**
     * Section header with an Align button that opens the layout editor on this
     * element — the ability slots' only positional control.
     */
    private void addHeaderRow(Component label, String elementId) {
        StringWidget title = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2 - ALIGN_W - 4, 20, label, this.font);
        contentWidgets.add(new ContentWidget(title, rowCursor));
        this.addRenderableWidget(title);
        addContentRow(alignButton(elementId));
    }

    /**
     * One line of explanatory text above a tab's rows, on a tighter stride than
     * a real row so it reads as a caption rather than a setting.
     */
    private void addHintRow(Component label) {
        StringWidget hint = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 14,
                label.copy().withStyle(ChatFormatting.GRAY), this.font);
        contentWidgets.add(new ContentWidget(hint, rowCursor));
        this.addRenderableWidget(hint);
        rowCursor += 18;
    }

    /**
     * A bar/overlay's header line: the show/hide checkbox is the element's
     * name, so one row carries both what it is and whether it draws.
     * <p>
     * Toggling rebuilds the tab, since the rows below this one appear and
     * disappear with it.
     *
     * @return whether the element is on, i.e. whether its own rows follow
     */
    private boolean addElementRow(String elementId, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(Component.translatable("screen.coi.layout_el_" + elementId), this.font)
                .pos(contentX + INDENT, 0)
                .maxWidth(contentW - INDENT * 2 - ALIGN_W - 4)
                .onValueChange((box, checked) -> {
                    setter.accept(checked);
                    this.init();
                })
                .selected(selected)
                .build();
        contentWidgets.add(new ContentWidget(checkbox, rowCursor));
        this.addRenderableWidget(checkbox);
        addContentRow(alignButton(elementId));
        return selected;
    }

    private Button alignButton(String elementId) {
        return Button.builder(Component.translatable("screen.coi.layout_align"), b -> openLayout(elementId))
                .bounds(contentX + contentW - INDENT - ALIGN_W, 0, ALIGN_W, 20).build();
    }

    private void addScaleRow(float current, Consumer<Float> setter) {
        addDecimalRow(SUB_INDENT, Component.translatable("screen.coi.element_scale"),
                Component.translatable("screen.coi.element_scale_field"),
                HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE, current,
                value -> setter.accept((float) value));
    }

    private void addCheckboxRow(int indent, Component label, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(label, Minecraft.getInstance().font)
                .pos(contentX + indent, 0)
                .maxWidth(contentW - indent - INDENT)
                .onValueChange((box, checked) -> setter.accept(checked))
                .selected(selected)
                .build();
        addContentRow(checkbox);
    }

    private void addIntRow(int indent, Component label, Component fieldLabel, int min, int max, int initialValue, IntConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        int clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (double) (max - min);
        int sliderW = contentW - indent - INDENT - FIELD_WIDTH - 6;

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
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

        EditBox field = new EditBox(this.font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
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

    private void addDecimalRow(int indent, Component label, Component fieldLabel, double min, double max,
                               double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);
        int sliderW = contentW - indent - INDENT - FIELD_WIDTH - 6;

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
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

        EditBox field = new EditBox(this.font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
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

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, 0x90000000);
        int titleY = compact() ? 5 : 12;
        graphics.centeredText(this.font, this.title, this.width / 2, titleY, CoiStyle.ACCENT);
        // Sits in the gap the centred title leaves, so it never crowds the text
        CoiIcons.draw(graphics, CoiIcons.COG,
                this.width / 2 - this.font.width(this.title) / 2 - CoiIcons.COG_SIZE - 5,
                titleY - (CoiIcons.COG_SIZE - this.font.lineHeight) / 2 - 1, CoiIcons.COG_SIZE);

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
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
