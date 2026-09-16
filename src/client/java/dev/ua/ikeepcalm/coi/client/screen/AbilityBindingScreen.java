package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.util.AbilityIcons;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private enum Tab {HOTKEYS, WHEEL, GESTURES}

    private final Screen parent;
    private final AbilityPickerOverlay picker = new AbilityPickerOverlay();
    private Tab currentTab = Tab.HOTKEYS;
    private double scrollOffset = 0;
    private Button hudSettingsButton;
    private Button appearanceSettingsButton;
    private Button clearAllButton;
    private Button doneButton;

    // Layout, recomputed every frame (description height varies per tab/locale)
    private int contentX, contentW, listTop, listBottom;

    public AbilityBindingScreen(Screen parent) {
        super(Component.translatable("screen.coi.ability_binding"));
        this.parent = parent;
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
        // Request abilities from server when screen opens
        CircleOfImaginationClient.requestAbilitiesFromServer();

        contentW = Math.clamp(this.width - 80, 300, 440);
        contentX = (this.width - contentW) / 2;

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

        // Bottom row: HUD Settings | Appearance | Clear All | Done. A single row instead of a
        // floating top-right button so nothing collides with the tabs at high
        // gui scales.
        int buttonW = Math.min(100, (contentW - 24) / 4);
        int rowW = buttonW * 4 + 24;
        int buttonX = (this.width - rowW) / 2;
        int buttonY = this.height - 30;

        hudSettingsButton = Button.builder(Component.translatable("screen.coi.hud_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().gui.setScreen(new HudSettingsScreen(null));
                }).bounds(buttonX, buttonY, buttonW, 20).build();
        this.addRenderableWidget(hudSettingsButton);

        appearanceSettingsButton = Button.builder(Component.translatable("screen.coi.appearance_settings"),
                button -> {
                    this.onClose();
                    Minecraft.getInstance().gui.setScreen(new AppearanceSettingsScreen(null));
                }).bounds(buttonX + buttonW + 8, buttonY, buttonW, 20).build();
        this.addRenderableWidget(appearanceSettingsButton);

        clearAllButton = Button.builder(Component.translatable("screen.coi.clear_all"),
                button -> {
                    switch (currentTab) {
                        case WHEEL -> {
                            for (int i = 0; i < CircleOfImaginationClient.getWheelSize(); i++) {
                                CircleOfImaginationClient.setWheelAbility(i, null);
                            }
                        }
                        case GESTURES -> {
                            for (int i = 0; i < GestureType.values().length; i++) {
                                CircleOfImaginationClient.setGestureAbility(i, null);
                            }
                        }
                        default -> {
                            for (int i = 0; i < CircleOfImaginationClient.getActiveAbilitySlots(); i++) {
                                CircleOfImaginationClient.setBoundAbility(i, null);
                            }
                        }
                    }
                }).bounds(buttonX + (buttonW + 8) * 2, buttonY, buttonW, 20).build();
        this.addRenderableWidget(clearAllButton);

        doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(buttonX + (buttonW + 8) * 3, buttonY, buttonW, 20).build();
        this.addRenderableWidget(doneButton);
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        scrollOffset = 0;
        if (picker.isOpen()) picker.close();
    }

    private int rowCount() {
        return switch (currentTab) {
            case WHEEL -> CircleOfImaginationClient.getWheelSize();
            case GESTURES -> GestureType.values().length;
            default -> CircleOfImaginationClient.getMaxAbilities();
        };
    }

    private static Component keyName(KeyMapping key) {
        return KeyMappingHelper.getBoundKeyOf(key).getDisplayName();
    }

    private Component tabDescription() {
        return switch (currentTab) {
            case WHEEL -> Component.translatable("screen.coi.tab_wheel_desc",
                    keyName(CircleOfImaginationClient.abilityWheel));
            case GESTURES -> Component.translatable("screen.coi.tab_gestures_desc",
                    keyName(CircleOfImaginationClient.gestureCast));
            default -> {
                String keys = IntStream.range(0, CircleOfImaginationClient.getActiveAbilitySlots())
                        .filter(i -> !CircleOfImaginationClient.abilityKeys[i].isUnbound())
                        .mapToObj(i -> keyName(CircleOfImaginationClient.abilityKeys[i]).getString())
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
        graphics.fill(0, 0, this.width, this.height, 0x90000000);
        graphics.centeredText(this.font, this.title, this.width / 2, compact() ? 5 : 12, CoiStyle.ACCENT);

        contentW = Math.clamp(this.width - 80, 300, 440);
        contentX = (this.width - contentW) / 2;

        // "How this works" banner with live keybind names
        Component desc = tabDescription();
        List<FormattedCharSequence> descLines = this.font.split(desc, contentW - 24);
        int descY = tabY() + TAB_H + 6;
        int descH = 8 + descLines.size() * 10 + 8;

        CoiStyle.drawCard(graphics, contentX, descY, contentW, descH);
        int lineY = descY + 8;
        for (FormattedCharSequence line : descLines) {
            graphics.text(this.font, line, contentX + 12, lineY, CoiStyle.TEXT_BODY);
            lineY += 10;
        }

        listTop = descY + descH + 8;
        if (CircleOfImaginationClient.getAvailableAbilities().isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.no_abilities"),
                    this.width / 2, listTop, 0xFFFF5555);
            listTop += 14;
        }
        // The viewport shrinks to fit the content so the bottom buttons follow
        // the list on tall screens instead of floating at the screen edge
        listBottom = Math.min(this.height - (compact() ? 32 : 40), listTop + contentHeight());
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());

        int buttonY = Math.min(this.height - (compact() ? 24 : 30), listBottom + 10);
        hudSettingsButton.setY(buttonY);
        appearanceSettingsButton.setY(buttonY);
        clearAllButton.setY(buttonY);
        doneButton.setY(buttonY);

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

    private void renderRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int count = rowCount();
        int activeSlots = CircleOfImaginationClient.getActiveAbilitySlots();

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
                case WHEEL -> renderWheelRow(graphics, i, rowY);
                case GESTURES -> renderGestureRow(graphics, i, rowY);
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

    private void renderHotkeyRow(GuiGraphicsExtractor graphics, int slot, int rowY, boolean inactive, int mouseX, int mouseY) {
        int labelColor = inactive ? CoiStyle.INACTIVE : CoiStyle.TEXT_MUTED;
        graphics.text(this.font, Component.translatable("screen.coi.slot_label", slot + 1),
                contentX + 8, rowY + 8, labelColor);

        // Keybind chip
        String key = keyName(CircleOfImaginationClient.abilityKeys[slot]).getString();
        key = this.font.plainSubstrByWidth(key, 52);
        int chipX = contentX + 62;
        int chipW = this.font.width(key) + 8;
        graphics.fill(chipX, rowY + 5, chipX + chipW, rowY + 19, 0x60000000);
        graphics.outline(chipX, rowY + 5, chipW, 14, inactive ? CoiStyle.INACTIVE : CoiStyle.BORDER);
        graphics.text(this.font, key, chipX + 4, rowY + 8, inactive ? CoiStyle.INACTIVE : CoiStyle.ACCENT, false);

        int rightX = contentX + Math.max(136, contentW * 2 / 5);
        if (inactive) {
            Component hint = Component.translatable("screen.coi.slot_inactive");
            String trimmed = this.font.plainSubstrByWidth(hint.getString(), contentX + contentW - rightX - 8);
            graphics.text(this.font, trimmed, rightX, rowY + 8, CoiStyle.INACTIVE, false);
            if (isRowHovered(mouseX, mouseY, rowY) && !picker.isOpen()) {
                graphics.setTooltipForNextFrame(this.font, hint, mouseX, mouseY);
            }
            return;
        }
        renderBoundValue(graphics, CircleOfImaginationClient.getBoundAbility(slot), rightX, rowY);
    }

    private void renderWheelRow(GuiGraphicsExtractor graphics, int slot, int rowY) {
        Component label = Component.translatable("screen.coi.wheel_slot").copy().append(" " + (slot + 1));
        graphics.text(this.font, label, contentX + 8, rowY + 8, CoiStyle.TEXT_MUTED);

        int rightX = contentX + Math.max(136, contentW * 2 / 5);
        renderBoundValue(graphics, CircleOfImaginationClient.getWheelAbility(slot), rightX, rowY);
    }

    private void renderGestureRow(GuiGraphicsExtractor graphics, int slot, int rowY) {
        GestureType type = GestureType.values()[slot];
        type.drawPreview(graphics, contentX + 8, rowY + 5, 14, CoiStyle.TEXT_BODY);
        graphics.text(this.font, type.displayName(), contentX + 30, rowY + 8, CoiStyle.TEXT_MUTED);

        int rightX = contentX + Math.max(136, contentW * 2 / 5);
        renderBoundValue(graphics, CircleOfImaginationClient.getGestureAbility(slot), rightX, rowY);
    }

    private void renderBoundValue(GuiGraphicsExtractor graphics, String bound, int rightX, int rowY) {
        if (bound != null) {
            AbilityIcons.draw(graphics, bound, rightX, rowY + 4, 16, 255);
            String name = AbilityInfo.extractDisplayName(bound);
            if (name == null) name = bound;
            String trimmed = this.font.plainSubstrByWidth(name, contentX + contentW - rightX - 28);
            graphics.text(this.font, trimmed, rightX + 20, rowY + 8, CoiStyle.TEXT_BODY, false);
        } else {
            graphics.text(this.font, Component.translatable("screen.coi.empty_slot"), rightX, rowY + 8, CoiStyle.TEXT_MUTED);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        double max = maxScroll();
        if (max <= 0) return;

        int viewportH = listBottom - listTop;
        int trackX = contentX + contentW + 4;
        graphics.fill(trackX, listTop, trackX + 3, listBottom, CoiStyle.SCROLL_TRACK);

        int thumbH = Math.max(16, viewportH * viewportH / contentHeight());
        int thumbY = listTop + (int) ((viewportH - thumbH) * (scrollOffset / max));
        graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, CoiStyle.BORDER);
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
        if (currentTab == Tab.HOTKEYS && idx >= CircleOfImaginationClient.getActiveAbilitySlots()) {
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
                    CircleOfImaginationClient.getWheelAbility(slot),
                    selected -> CircleOfImaginationClient.setWheelAbility(slot, selected));
            case GESTURES -> picker.open(
                    Component.translatable("screen.coi.picker_title",
                            GestureType.values()[slot].displayName()),
                    CircleOfImaginationClient.getGestureAbility(slot),
                    selected -> CircleOfImaginationClient.setGestureAbility(slot, selected));
            default -> {
                String key = keyName(CircleOfImaginationClient.abilityKeys[slot]).getString();
                picker.open(
                        Component.translatable("screen.coi.picker_title",
                                Component.translatable("screen.coi.slot_label", slot + 1).getString() + " [" + key + "]"),
                        CircleOfImaginationClient.getBoundAbility(slot),
                        selected -> CircleOfImaginationClient.setBoundAbility(slot, selected));
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
