package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig.HudSettings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig.SlotPlacement;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * A {@link HudSettings} to the {@code config/coi_hud.json} object.
 * <p>
 * The mirror of {@link HudConfigReader}, key for key: a setting the writer
 * forgets is a setting that silently resets on the next launch, so the two
 * classes are meant to be read side by side.
 * <p>
 * {@code layoutVersion} is the one field that is not copied out of the
 * settings: a file this build writes is by definition current, so it is always
 * stamped {@link HudConfig#LAYOUT_VERSION}.
 */
final class HudConfigWriter {

    private HudConfigWriter() {
    }

    static JsonObject write(HudSettings s) {
        JsonObject json = new JsonObject();
        writeAbilityHud(json, s);
        writeBeyonderHealth(json, s);
        writeCharacterPlate(json, s);
        writeBars(json, s);
        writeOverlays(json, s);
        writeGeneral(json, s);
        json.add("slotPlacements", slotPlacements(s));
        return json;
    }

    private static void writeAbilityHud(JsonObject json, HudSettings s) {
        json.addProperty("enabled", s.enabled);
        json.addProperty("hudX", s.hudX);
        json.addProperty("hudYOffset", s.hudYOffset);
        json.addProperty("slotSize", s.slotSize);
        json.addProperty("showKeybinds", s.showKeybinds);
        json.addProperty("showAbilityNames", s.showAbilityNames);
        json.addProperty("showGlowEffect", s.showGlowEffect);
        json.addProperty("wheelSlots", s.wheelSlots);
        json.addProperty("activeAbilitySlots", s.activeAbilitySlots);
    }

    private static void writeBeyonderHealth(JsonObject json, HudSettings s) {
        json.addProperty("showBeyonderHealth", s.showBeyonderHealth);
        json.addProperty("beyonderHealthAnchor", s.beyonderHealthAnchor);
        json.addProperty("beyonderHealthXOffset", s.beyonderHealthXOffset);
        json.addProperty("beyonderHealthYOffset", s.beyonderHealthYOffset);
        json.addProperty("beyonderHealthScale", s.beyonderHealthScale);
    }

    private static void writeCharacterPlate(JsonObject json, HudSettings s) {
        json.addProperty("showCharacterPlate", s.showCharacterPlate);
        json.addProperty("characterPlateAnchor", s.characterPlateAnchor);
        json.addProperty("characterPlateXOffset", s.characterPlateXOffset);
        json.addProperty("characterPlateYOffset", s.characterPlateYOffset);
        json.addProperty("characterPlateScale", s.characterPlateScale);
    }

    private static void writeBars(JsonObject json, HudSettings s) {
        json.addProperty("showMadnessBar", s.showMadnessBar);
        json.addProperty("madnessXOffset", s.madnessXOffset);
        json.addProperty("madnessYOffset", s.madnessYOffset);
        json.addProperty("madnessAnchor", s.madnessAnchor);
        json.addProperty("madnessScale", s.madnessScale);
        json.addProperty("layoutVersion", HudConfig.LAYOUT_VERSION);
        json.addProperty("showSpiritualityBar", s.showSpiritualityBar);
        json.addProperty("spiritualityAnchor", s.spiritualityAnchor);
        json.addProperty("spiritualityXOffset", s.spiritualityXOffset);
        json.addProperty("spiritualityYOffset", s.spiritualityYOffset);
        json.addProperty("spiritualityHideWhenFull", s.spiritualityHideWhenFull);
        json.addProperty("spiritualityScale", s.spiritualityScale);
        json.addProperty("showActingBar", s.showActingBar);
        json.addProperty("actingAnchor", s.actingAnchor);
        json.addProperty("actingXOffset", s.actingXOffset);
        json.addProperty("actingYOffset", s.actingYOffset);
        json.addProperty("actingScale", s.actingScale);
        json.addProperty("showResourceBars", s.showResourceBars);
        json.addProperty("resourceAnchor", s.resourceAnchor);
        json.addProperty("resourceXOffset", s.resourceXOffset);
        json.addProperty("resourceYOffset", s.resourceYOffset);
        json.addProperty("resourceMaxBars", s.resourceMaxBars);
        json.addProperty("resourceScale", s.resourceScale);
    }

    private static void writeOverlays(JsonObject json, HudSettings s) {
        json.addProperty("showActionBar", s.showActionBar);
        json.addProperty("actionBarXOffset", s.actionBarXOffset);
        json.addProperty("actionBarYOffset", s.actionBarYOffset);
        json.addProperty("actionBarLines", s.actionBarLines);
        json.addProperty("actionBarScale", s.actionBarScale);
        json.addProperty("showTargetHealth", s.showTargetHealth);
        json.addProperty("targetHealthXOffset", s.targetHealthXOffset);
        json.addProperty("targetHealthYOffset", s.targetHealthYOffset);
        json.addProperty("targetHealthScale", s.targetHealthScale);
        json.addProperty("showCogitationOverlay", s.showCogitationOverlay);
        json.addProperty("cogitationXOffset", s.cogitationXOffset);
        json.addProperty("cogitationYOffset", s.cogitationYOffset);
        json.addProperty("cogitationScale", s.cogitationScale);
        json.addProperty("showNotifications", s.showNotifications);
        json.addProperty("notificationXOffset", s.notificationXOffset);
        json.addProperty("notificationYOffset", s.notificationYOffset);
        json.addProperty("notificationScale", s.notificationScale);
    }

    private static void writeGeneral(JsonObject json, HudSettings s) {
        json.addProperty("epilepsyMode", s.epilepsyMode);
        json.addProperty("effectSoundVolume", s.effectSoundVolume);
        json.addProperty("enableHallucinations", s.enableHallucinations);
        json.addProperty("enableDiscordPresence", s.enableDiscordPresence);
        json.addProperty("presenceShowMadness", s.presenceShowMadness);
        json.addProperty("useServerMenus", s.useServerMenus);
    }

    /**
     * Always {@link AbilityBindings#MAX_ABILITIES} entries, with an explicit
     * null for every slot still riding the shared row — a short array would
     * read as "the rest have no placement" anyway, but a fixed length keeps the
     * file legible when somebody opens it to check one slot.
     */
    private static JsonArray slotPlacements(HudSettings s) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            SlotPlacement placement = s.slotPlacements == null || i >= s.slotPlacements.length
                    ? null : s.slotPlacements[i];
            if (placement == null) {
                array.add(JsonNull.INSTANCE);
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("anchor", placement.anchor());
            entry.addProperty("x", placement.x());
            entry.addProperty("y", placement.y());
            array.add(entry);
        }
        return array;
    }
}
