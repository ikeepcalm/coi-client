package dev.ua.ikeepcalm.coi.client.screen.settings;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudElements;
import dev.ua.ikeepcalm.coi.client.screen.TourScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The contents of {@link HudSettingsScreen}'s three tabs, one method each.
 * <p>
 * The tabs are split by what a setting <em>is</em> rather than by which overlay
 * draws it, so each method here is a flat reading of one tab's rows against the
 * working copy of the settings it edits.
 */
final class SettingsTabs {

    /** Upper bound of the wheel slot count, and of the resource meter cap. */
    private static final int MAX_WHEEL_SLOTS = 16;
    private static final int MAX_RESOURCE_BARS = 8;
    private static final int MAX_ACTION_BAR_LINES = 4;

    private SettingsTabs() {
    }

    static void hud(SettingsRows rows, HudConfig.HudSettings settings) {
        rows.headerRow(Component.translatable("screen.coi.layout_el_ability_slots"), HudElements.ABILITY_SLOTS);
        // The single size knob: it scales the whole slot, and the row's spacing
        // follows it, so there is nothing left for a separate scale or spacing
        // slider to disagree about
        rows.intRow(SettingsRows.INDENT, Component.translatable("screen.coi.slot_size"), Component.translatable("screen.coi.slot_size_field"),
                HudConfig.MIN_SLOT_SIZE, HudConfig.MAX_SLOT_SIZE, settings.slotSize, value -> settings.slotSize = value);
        rows.intRow(SettingsRows.INDENT, Component.translatable("screen.coi.key_slots"), Component.translatable("screen.coi.key_slots_field"),
                1, AbilityBindings.MAX_ABILITIES, settings.activeAbilitySlots, value -> settings.activeAbilitySlots = value);
        rows.intRow(SettingsRows.INDENT, Component.translatable("screen.coi.wheel_slots"), Component.translatable("screen.coi.wheel_slots_field"),
                2, MAX_WHEEL_SLOTS, settings.wheelSlots, value -> settings.wheelSlots = value);

        rows.headerRow(Component.translatable("screen.coi.slot_decoration_section"));
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.show_keybinds"), settings.showKeybinds,
                checked -> settings.showKeybinds = checked);
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.show_ability_names"), settings.showAbilityNames,
                checked -> settings.showAbilityNames = checked);
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.show_glow_effect"), settings.showGlowEffect,
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
    static void elements(SettingsRows rows, HudConfig.HudSettings settings) {
        rows.hintRow(Component.translatable("screen.coi.elements_hint"));

        // The plate absorbs sanity, acting and the reserve meters, so while it
        // is on those three get no rows at all — showing controls for a bar the
        // plate has taken over is exactly the clutter this was meant to end.
        // Spirituality is deliberately not part of that trade.
        if (rows.elementRow(HudElements.CHARACTER_PLATE, settings.showCharacterPlate,
                checked -> settings.showCharacterPlate = checked)) {
            rows.scaleRow(settings.characterPlateScale, value -> settings.characterPlateScale = value);
            rows.hintRow(Component.translatable("screen.coi.plate_supersedes"));
        }

        // Not part of the plate's either/or: this one replaces the vanilla
        // hearts rather than another COI bar, so it stands on its own
        if (rows.elementRow(HudElements.BEYONDER_HEALTH, settings.showBeyonderHealth,
                checked -> settings.showBeyonderHealth = checked)) {
            rows.scaleRow(settings.beyonderHealthScale, value -> settings.beyonderHealthScale = value);
            rows.hintRow(Component.translatable("screen.coi.health_supersedes"));
        }

        if (!settings.showCharacterPlate
                && rows.elementRow(HudElements.MADNESS, settings.showMadnessBar, checked -> settings.showMadnessBar = checked)) {
            rows.scaleRow(settings.madnessScale, value -> settings.madnessScale = value);
        }

        if (rows.elementRow(HudElements.SPIRITUALITY, settings.showSpiritualityBar, checked -> settings.showSpiritualityBar = checked)) {
            rows.scaleRow(settings.spiritualityScale, value -> settings.spiritualityScale = value);
            rows.checkboxRow(SettingsRows.SUB_INDENT, Component.translatable("screen.coi.spirituality_hide_when_full"),
                    settings.spiritualityHideWhenFull, checked -> settings.spiritualityHideWhenFull = checked);
        }

        if (!settings.showCharacterPlate
                && rows.elementRow(HudElements.ACTING, settings.showActingBar, checked -> settings.showActingBar = checked)) {
            rows.scaleRow(settings.actingScale, value -> settings.actingScale = value);
        }

        if (settings.showCharacterPlate) {
            // The plate still draws the reserve rows, so their cap stays reachable
            resourceMaxBarsRow(rows, settings);
        } else if (rows.elementRow(HudElements.RESOURCES, settings.showResourceBars, checked -> settings.showResourceBars = checked)) {
            rows.scaleRow(settings.resourceScale, value -> settings.resourceScale = value);
            resourceMaxBarsRow(rows, settings);
        }

        if (rows.elementRow(HudElements.ACTION_BAR, settings.showActionBar, checked -> settings.showActionBar = checked)) {
            rows.scaleRow(settings.actionBarScale, value -> settings.actionBarScale = value);
            rows.intRow(SettingsRows.SUB_INDENT, Component.translatable("screen.coi.action_bar_lines"),
                    Component.translatable("screen.coi.action_bar_lines_field"),
                    0, MAX_ACTION_BAR_LINES, settings.actionBarLines, value -> settings.actionBarLines = value);
        }

        if (rows.elementRow(HudElements.TARGET_HEALTH, settings.showTargetHealth, checked -> settings.showTargetHealth = checked)) {
            rows.scaleRow(settings.targetHealthScale, value -> settings.targetHealthScale = value);
        }

        if (rows.elementRow(HudElements.COGITATION, settings.showCogitationOverlay, checked -> settings.showCogitationOverlay = checked)) {
            rows.scaleRow(settings.cogitationScale, value -> settings.cogitationScale = value);
        }

        if (rows.elementRow(HudElements.NOTIFICATIONS, settings.showNotifications, checked -> settings.showNotifications = checked)) {
            rows.scaleRow(settings.notificationScale, value -> settings.notificationScale = value);
        }
    }

    private static void resourceMaxBarsRow(SettingsRows rows, HudConfig.HudSettings settings) {
        rows.intRow(SettingsRows.SUB_INDENT, Component.translatable("screen.coi.resource_max_bars"),
                Component.translatable("screen.coi.resource_max_bars_field"),
                1, MAX_RESOURCE_BARS, settings.resourceMaxBars, value -> settings.resourceMaxBars = value);
    }

    static void general(SettingsRows rows, HudConfig.HudSettings settings) {
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.hud_enabled"), settings.enabled,
                checked -> settings.enabled = checked);

        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.menu_use_server"), settings.useServerMenus,
                checked -> settings.useServerMenus = checked);
        rows.hintRow(Component.translatable("screen.coi.menu_use_server_hint"));

        rows.headerRow(Component.translatable("screen.coi.accessibility_section"));
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.epilepsy_mode"), settings.epilepsyMode,
                checked -> settings.epilepsyMode = checked);
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.enable_hallucinations"), settings.enableHallucinations,
                checked -> settings.enableHallucinations = checked);
        rows.contentRow(volumeSlider(rows, settings));

        rows.headerRow(Component.translatable("screen.coi.integrations_section"));
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.enable_discord_presence"), settings.enableDiscordPresence,
                checked -> settings.enableDiscordPresence = checked);
        rows.checkboxRow(SettingsRows.INDENT, Component.translatable("screen.coi.presence_show_madness"), settings.presenceShowMadness,
                checked -> settings.presenceShowMadness = checked);

        rows.headerRow(Component.translatable("screen.coi.help_section"));
        rows.contentRow(Button.builder(Component.translatable("screen.coi.show_tour"),
                _ -> {
                    ClientStateStore.setTourCompleted(false);
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null) {
                        client.gui.setScreen(new TourScreen());
                    }
                }).bounds(rows.contentX() + SettingsRows.INDENT, 0,
                rows.contentW() - SettingsRows.INDENT * 2, 20).build());
    }

    /** Stored 0..1, shown as a whole percentage. */
    private static AbstractSliderButton volumeSlider(SettingsRows rows, HudConfig.HudSettings settings) {
        int initialVolume = Math.round(Math.clamp(settings.effectSoundVolume, 0f, 1f) * 100);
        return new AbstractSliderButton(rows.contentX() + SettingsRows.INDENT, 0,
                rows.contentW() - SettingsRows.INDENT * 2, 20,
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
    }
}
