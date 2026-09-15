package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.client.network.CoiNetworking;
import dev.ua.ikeepcalm.coi.client.screen.ScreenInput;
import dev.ua.ikeepcalm.coi.client.screen.ScrollbarPainter;
import dev.ua.ikeepcalm.coi.client.screen.settings.HudSettingsScreen;
import dev.ua.ikeepcalm.coi.client.screen.widget.CoiTabButton;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

/**
 * Binding screen with one tab per casting method — hotkeys, ability wheel,
 * gesture casting — so all three are discoverable at a glance. Each tab shows
 * a short "how this works" banner (with live keybind names) above a scrollable
 * slot list; clicking a slot opens the {@link AbilityPickerOverlay}.
 * <p>
 * Layout adapts to the gui-scaled screen size: vertical spacing tightens on
 * short screens (high gui scale) and the bottom buttons follow the content
 * instead of hugging the screen edge on tall ones (gui scale 1).
 */
public class AbilityBindingScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int ROW_STRIDE = 28;
    private static final int TAB_H = 22;

    /** Below this the tabs, the banner and the buttons stop fitting comfortably. */
    private static final int COMPACT_HEIGHT = 300;

    private enum Tab {HOTKEYS, WHEEL, GESTURES}

    private final Screen parent;
    private final AbilityPickerOverlay picker = new AbilityPickerOverlay();
    private Tab currentTab = Tab.HOTKEYS;
    private double scrollOffset = 0;
    private Button hudSettingsButton;
    private Button clearAllButton;
    private Button doneButton;

    // Layout, recomputed every frame (description height varies per tab/locale)
    private int contentX, contentW, listTop, listBottom;

    public AbilityBindingScreen(Screen parent) {
        super(Component.translatable("screen.coi.ability_binding"));
        this.parent = parent;
    }

    private boolean compact() {
        return this.height < COMPACT_HEIGHT;
    }

    private int tabY() {
        return compact() ? 18 : 30;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        // Request abilities from server when screen opens
        CoiNetworking.requestAbilitiesFromServer();

        // For testing purposes, add sample abilities if none are available
        if (AbilityRegistry.getAvailableAbilities().isEmpty()) {
            AbilityRegistry.addTestAbilities();
        }

        contentW = CoiStyle.formWidth(this.width);
        contentX = (this.width - contentW) / 2;

        addTabs();
        addBottomRow();
    }

    private void addTabs() {
        int tabW = (contentW - 8) / 3;
        this.addRenderableWidget(new CoiTabButton(contentX, tabY(), tabW, TAB_H,
                Component.translatable("screen.coi.tab_hotkeys"),
                (g, x, y, size, color) -> {
                    // Keycap glyph: square outline with a shading line near the bottom
                    g.outline(x, y, size, size, color);
                    g.fill(x + 2, y + size - 3, x + size - 2, y + size - 2, color);
                },
                () -> currentTab == Tab.HOTKEYS, () -> switchTab(Tab.HOTKEYS)));
        this.addRenderableWidget(new CoiTabButton(contentX + tabW + 4, tabY(), tabW, TAB_H,
                Component.translatable("screen.coi.tab_wheel"),
                GestureType.CIRCLE::drawPreview,
                () -> currentTab == Tab.WHEEL, () -> switchTab(Tab.WHEEL)));
        this.addRenderableWidget(new CoiTabButton(contentX + (tabW + 4) * 2, tabY(), tabW, TAB_H,
                Component.translatable("screen.coi.tab_gestures"),
                GestureType.Z::drawPreview,
                () -> currentTab == Tab.GESTURES, () -> switchTab(Tab.GESTURES)));
    }

    /**
     * HUD Settings | Clear All | Done. A single row instead of a floating
     * top-right button so nothing collides with the tabs at high gui scales.
     */
    private void addBottomRow() {
        int buttonW = Math.min(100, (contentW - 16) / 3);
        int rowW = buttonW * 3 + 16;
        int buttonX = (this.width - rowW) / 2;
        int buttonY = this.height - 30;

        hudSettingsButton = Button.builder(Component.translatable("screen.coi.hud_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().gui.setScreen(new HudSettingsScreen(null));
                }).bounds(buttonX, buttonY, buttonW, 20).build();
        this.addRenderableWidget(hudSettingsButton);

        clearAllButton = Button.builder(Component.translatable("screen.coi.clear_all"),
                button -> clearCurrentTab()).bounds(buttonX + buttonW + 8, buttonY, buttonW, 20).build();
        this.addRenderableWidget(clearAllButton);

        doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(buttonX + (buttonW + 8) * 2, buttonY, buttonW, 20).build();
        this.addRenderableWidget(doneButton);
    }

    /** Unbinds every slot of whichever tab is showing — never the other two. */
    private void clearCurrentTab() {
        switch (currentTab) {
            case WHEEL -> {
                for (int i = 0; i < AbilityBindings.getWheelSize(); i++) {
                    AbilityBindings.setWheelAbility(i, null);
                }
            }
            case GESTURES -> {
                for (int i = 0; i < GestureType.values().length; i++) {
                    AbilityBindings.setGestureAbility(i, null);
                }
            }
            default -> {
                for (int i = 0; i < AbilityBindings.getActiveAbilitySlots(); i++) {
                    AbilityBindings.setBoundAbility(i, null);
                }
            }
        }
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        scrollOffset = 0;
        if (picker.isOpen()) picker.close();
    }

    private int rowCount() {
        return switch (currentTab) {
            case WHEEL -> AbilityBindings.getWheelSize();
            case GESTURES -> GestureType.values().length;
            default -> AbilityBindings.getMaxAbilities();
        };
    }

    /** The "how this works" banner for the current tab, naming live keybinds. */
    private Component tabDescription() {
        return switch (currentTab) {
            case WHEEL -> Component.translatable("screen.coi.tab_wheel_desc",
                    ScreenInput.keyName(CoiKeyBindings.abilityWheel));
            case GESTURES -> Component.translatable("screen.coi.tab_gestures_desc",
                    ScreenInput.keyName(CoiKeyBindings.gestureCast));
            default -> {
                String keys = IntStream.range(0, AbilityBindings.getActiveAbilitySlots())
                        .filter(i -> !CoiKeyBindings.abilityKey(i).isUnbound())
                        .mapToObj(i -> ScreenInput.keyName(CoiKeyBindings.abilityKey(i)).getString())
                        .collect(Collectors.joining(" "));
                yield Component.translatable("screen.coi.tab_hotkeys_desc", keys);
            }
        };
    }

    private int contentHeight() {
        return rowCount() * ROW_STRIDE - (ROW_STRIDE - ROW_H);
    }

    private double maxScroll() {
        return Math.max(0, contentHeight() - (listBottom - listTop));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, CoiStyle.SCRIM);
        graphics.centeredText(this.font, this.title, this.width / 2, compact() ? 5 : 12, CoiStyle.ACCENT);

        contentW = CoiStyle.formWidth(this.width);
        contentX = (this.width - contentW) / 2;

        listTop = renderBanner(graphics) + 8;
        if (AbilityRegistry.getAvailableAbilities().isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.no_abilities"),
                    this.width / 2, listTop, 0xFFFF5555);
            listTop += 14;
        }
        placeViewportAndButtons();
        renderRows(graphics, mouseX, mouseY);

        // Widgets (tabs and buttons) — outside the scissored region
        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (clearAllButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.clear_all.tooltip"), mouseX, mouseY);
        }

        if (picker.isOpen()) {
            graphics.nextStratum();
            picker.render(graphics, this.font, this.width, this.height, mouseX, mouseY, a);
        }
    }

    /**
     * The viewport shrinks to fit the content so the bottom buttons follow the
     * list on tall screens instead of floating at the screen edge. The banner's
     * height varies per tab and locale, so this is redone every frame rather
     * than in {@code init()}.
     */
    private void placeViewportAndButtons() {
        listBottom = Math.min(this.height - (compact() ? 32 : 40), listTop + contentHeight());
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());

        int buttonY = Math.min(this.height - (compact() ? 24 : 30), listBottom + 10);
        hudSettingsButton.setY(buttonY);
        clearAllButton.setY(buttonY);
        doneButton.setY(buttonY);
    }

    /**
     * The "how this works" banner with live keybind names. Its height follows
     * the wrapped text, so it answers with the y it reached.
     */
    private int renderBanner(GuiGraphicsExtractor graphics) {
        List<FormattedCharSequence> descLines = this.font.split(tabDescription(), contentW - 24);
        int descY = tabY() + TAB_H + 6;
        int descH = 8 + descLines.size() * 10 + 8;

        CoiStyle.drawCard(graphics, contentX, descY, contentW, descH);
        int lineY = descY + 8;
        for (FormattedCharSequence line : descLines) {
            graphics.text(this.font, line, contentX + 12, lineY, CoiStyle.TEXT_BODY);
            lineY += 10;
        }
        return descY + descH;
    }

    private void renderRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int count = rowCount();
        int activeSlots = AbilityBindings.getActiveAbilitySlots();

        graphics.enableScissor(contentX, listTop, contentX + contentW, listBottom);
        for (int i = 0; i < count; i++) {
            int rowY = listTop + i * ROW_STRIDE - (int) scrollOffset;
            if (rowY + ROW_H < listTop || rowY > listBottom) continue;

            boolean inactive = currentTab == Tab.HOTKEYS && i >= activeSlots;
            boolean hovered = !picker.isOpen() && !inactive && isRowHovered(mouseX, mouseY, rowY);

            graphics.fill(contentX, rowY, contentX + contentW, rowY + ROW_H, CoiStyle.CARD_BG);
            graphics.outline(contentX, rowY, contentW, ROW_H, CoiStyle.BORDER);
            if (hovered) {
                graphics.fill(contentX, rowY, contentX + contentW, rowY + ROW_H, CoiStyle.ROW_HOVER);
            }

            switch (currentTab) {
                case WHEEL -> BindingRowPainter.wheelRow(graphics, this.font, contentX, contentW, i, rowY);
                case GESTURES -> BindingRowPainter.gestureRow(graphics, this.font, contentX, contentW, i, rowY);
                default -> renderHotkeyRow(graphics, i, rowY, inactive, mouseX, mouseY);
            }
        }
        graphics.disableScissor();

        renderScrollbar(graphics);
    }

    private boolean isRowHovered(int mouseX, int mouseY, int rowY) {
        return mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= Math.max(rowY, listTop) && mouseY < Math.min(rowY + ROW_H, listBottom);
    }

    /**
     * The inactive hint's tooltip stays here rather than with the painter: only
     * the screen knows whether the picker is over the row it would point at.
     */
    private void renderHotkeyRow(GuiGraphicsExtractor graphics, int slot, int rowY, boolean inactive, int mouseX, int mouseY) {
        BindingRowPainter.hotkeyRow(graphics, this.font, contentX, contentW, slot, rowY, inactive);
        if (inactive && isRowHovered(mouseX, mouseY, rowY) && !picker.isOpen()) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.translatable("screen.coi.slot_inactive"), mouseX, mouseY);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        ScrollbarPainter.draw(graphics, contentX + contentW + 4, listTop, listBottom,
                contentHeight(), scrollOffset, maxScroll());
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (picker.isOpen()) {
            return picker.mouseClicked(event);
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        double mx = event.x();
        double my = event.y();
        if (mx < contentX || mx >= contentX + contentW || my < listTop || my >= listBottom) {
            return false;
        }
        double listY = my - listTop + scrollOffset;
        int idx = (int) (listY / ROW_STRIDE);
        if (idx < 0 || idx >= rowCount() || listY - idx * ROW_STRIDE >= ROW_H) {
            return false;
        }
        if (currentTab == Tab.HOTKEYS && idx >= AbilityBindings.getActiveAbilitySlots()) {
            return false;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        openPickerFor(idx);
        return true;
    }

    private void openPickerFor(int slot) {
        switch (currentTab) {
            case WHEEL -> picker.open(
                    Component.translatable("screen.coi.picker_title",
                            Component.translatable("screen.coi.wheel_slot").getString() + " " + (slot + 1)),
                    AbilityBindings.getWheelAbility(slot),
                    selected -> AbilityBindings.setWheelAbility(slot, selected));
            case GESTURES -> picker.open(
                    Component.translatable("screen.coi.picker_title",
                            GestureType.values()[slot].displayName()),
                    AbilityBindings.getGestureAbility(slot),
                    selected -> AbilityBindings.setGestureAbility(slot, selected));
            default -> {
                String key = ScreenInput.keyName(CoiKeyBindings.abilityKey(slot)).getString();
                picker.open(
                        Component.translatable("screen.coi.picker_title",
                                Component.translatable("screen.coi.slot_label", slot + 1).getString() + " [" + key + "]"),
                        AbilityBindings.getBoundAbility(slot),
                        selected -> AbilityBindings.setBoundAbility(slot, selected));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (picker.isOpen()) {
            return picker.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        scrollOffset = Mth.clamp(scrollOffset - verticalAmount * ROW_STRIDE, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (picker.isOpen()) {
            return picker.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (picker.isOpen()) {
            return picker.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
