package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig.HudSettings;

/**
 * The field-by-field copy behind the settings screens' working copies and the
 * layout editor's Cancel snapshot.
 * <p>
 * Deliberately explicit rather than reflective or clone-based: a working copy
 * that silently shares an array with the live settings would make Cancel a
 * no-op, and that is exactly the bug this is written out longhand to avoid.
 */
final class HudSettingsCopy {

    private HudSettingsCopy() {
    }

    static void copy(HudSettings from, HudSettings to) {
        copyAbilityHud(from, to);
        copyBeyonderHealth(from, to);
        copyCharacterPlate(from, to);
        copyBars(from, to);
        copyOverlays(from, to);
        copyGeneral(from, to);
        copySlotPlacements(from, to);
    }

    private static void copyAbilityHud(HudSettings from, HudSettings to) {
        to.enabled = from.enabled;
        to.hudX = from.hudX;
        to.hudYOffset = from.hudYOffset;
        to.slotSize = from.slotSize;
        to.showKeybinds = from.showKeybinds;
        to.showAbilityNames = from.showAbilityNames;
        to.showGlowEffect = from.showGlowEffect;
        to.wheelSlots = from.wheelSlots;
        to.activeAbilitySlots = from.activeAbilitySlots;
    }

    private static void copyBeyonderHealth(HudSettings from, HudSettings to) {
        to.showBeyonderHealth = from.showBeyonderHealth;
        to.beyonderHealthAnchor = from.beyonderHealthAnchor;
        to.beyonderHealthXOffset = from.beyonderHealthXOffset;
        to.beyonderHealthYOffset = from.beyonderHealthYOffset;
        to.beyonderHealthScale = from.beyonderHealthScale;
    }

    private static void copyCharacterPlate(HudSettings from, HudSettings to) {
        to.showCharacterPlate = from.showCharacterPlate;
        to.characterPlateAnchor = from.characterPlateAnchor;
        to.characterPlateXOffset = from.characterPlateXOffset;
        to.characterPlateYOffset = from.characterPlateYOffset;
        to.characterPlateScale = from.characterPlateScale;
    }

    private static void copyBars(HudSettings from, HudSettings to) {
        to.showMadnessBar = from.showMadnessBar;
        to.madnessXOffset = from.madnessXOffset;
        to.madnessYOffset = from.madnessYOffset;
        to.madnessAnchor = from.madnessAnchor;
        to.madnessScale = from.madnessScale;
        to.showSpiritualityBar = from.showSpiritualityBar;
        to.spiritualityAnchor = from.spiritualityAnchor;
        to.spiritualityXOffset = from.spiritualityXOffset;
        to.spiritualityYOffset = from.spiritualityYOffset;
        to.spiritualityHideWhenFull = from.spiritualityHideWhenFull;
        to.spiritualityScale = from.spiritualityScale;
        to.showActingBar = from.showActingBar;
        to.actingAnchor = from.actingAnchor;
        to.actingXOffset = from.actingXOffset;
        to.actingYOffset = from.actingYOffset;
        to.actingScale = from.actingScale;
        to.showResourceBars = from.showResourceBars;
        to.resourceAnchor = from.resourceAnchor;
        to.resourceXOffset = from.resourceXOffset;
        to.resourceYOffset = from.resourceYOffset;
        to.resourceMaxBars = from.resourceMaxBars;
        to.resourceScale = from.resourceScale;
    }

    private static void copyOverlays(HudSettings from, HudSettings to) {
        to.showActionBar = from.showActionBar;
        to.actionBarXOffset = from.actionBarXOffset;
        to.actionBarYOffset = from.actionBarYOffset;
        to.actionBarLines = from.actionBarLines;
        to.actionBarScale = from.actionBarScale;
        to.showTargetHealth = from.showTargetHealth;
        to.targetHealthXOffset = from.targetHealthXOffset;
        to.targetHealthYOffset = from.targetHealthYOffset;
        to.targetHealthScale = from.targetHealthScale;
        to.showCogitationOverlay = from.showCogitationOverlay;
        to.cogitationXOffset = from.cogitationXOffset;
        to.cogitationYOffset = from.cogitationYOffset;
        to.cogitationScale = from.cogitationScale;
        to.showNotifications = from.showNotifications;
        to.notificationXOffset = from.notificationXOffset;
        to.notificationYOffset = from.notificationYOffset;
        to.notificationScale = from.notificationScale;
    }

    private static void copyGeneral(HudSettings from, HudSettings to) {
        to.epilepsyMode = from.epilepsyMode;
        to.effectSoundVolume = from.effectSoundVolume;
        to.enableHallucinations = from.enableHallucinations;
        to.enableDiscordPresence = from.enableDiscordPresence;
        to.presenceShowMadness = from.presenceShowMadness;
        to.useServerMenus = from.useServerMenus;
        to.coiTitleScreen = from.coiTitleScreen;
        to.layoutVersion = from.layoutVersion;
    }

    /**
     * {@link HudConfig.SlotPlacement} is immutable, so sharing the records is
     * safe — only the array itself has to be fresh.
     */
    private static void copySlotPlacements(HudSettings from, HudSettings to) {
        to.slotPlacements = new HudConfig.SlotPlacement[AbilityBindings.MAX_ABILITIES];
        if (from.slotPlacements == null) return;
        int n = Math.min(to.slotPlacements.length, from.slotPlacements.length);
        System.arraycopy(from.slotPlacements, 0, to.slotPlacements, 0, n);
    }
}
