package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig.HudSettings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig.SlotPlacement;
import dev.ua.ikeepcalm.coi.client.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * {@code config/coi_hud.json} to a {@link HudSettings}.
 * <p>
 * Every key is read as "the value if the file names it, otherwise the
 * default", which is what lets a config written by an older build of the mod
 * open without losing anything: a key that did not exist yet simply falls back.
 * The key names here are the on-disk contract — renaming one silently resets
 * that setting for every existing player — so they are spelled out literally
 * rather than derived from the field names.
 * <p>
 * The reader does not migrate: {@link HudConfigMigrations} runs afterwards, on
 * the settings this produced.
 */
final class HudConfigReader {

    private HudConfigReader() {
    }

    /**
     * Reads {@code json} over {@code s}, leaving any key the file does not
     * name at whatever the fresh {@link HudSettings} had.
     */
    static void read(JsonObject json, HudSettings s) {
        readAbilityHud(json, s);
        readBeyonderHealth(json, s);
        readCharacterPlate(json, s);
        readBars(json, s);
        readOverlays(json, s);
        readGeneral(json, s);
        readSlotPlacements(json, s);
    }

    private static void readAbilityHud(JsonObject json, HudSettings s) {
        s.enabled = flag(json, "enabled", true);
        s.hudX = intOf(json, "hudX", 10);
        s.hudYOffset = intOf(json, "hudYOffset", 60);
        s.slotSize = Math.clamp(intOf(json, "slotSize", HudConfig.DEFAULT_SLOT_SIZE),
                HudConfig.MIN_SLOT_SIZE, HudConfig.MAX_SLOT_SIZE);
        s.showAbilityHud = flag(json, "showAbilityHud", true);
        s.showKeybinds = flag(json, "showKeybinds", true);
        s.showAbilityNames = flag(json, "showAbilityNames", true);
        s.showGlowEffect = flag(json, "showGlowEffect", true);
        s.wheelSlots = intOf(json, "wheelSlots", 8);
        s.activeAbilitySlots = Math.clamp(intOf(json, "activeAbilitySlots", 6), 1, AbilityBindings.MAX_ABILITIES);
    }

    /**
     * The style is read <em>first</em>, because it is what the vertical
     * placement defaults to: the three replacing styles sit on the hearts' own
     * row and {@link dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle#HEARTS}
     * has to clear it.
     */
    private static void readBeyonderHealth(JsonObject json, HudSettings s) {
        s.showBeyonderHealth = flag(json, "showBeyonderHealth", true);
        s.beyonderHealthStyle = string(json, "beyonderHealthStyle", HealthStyle.DEFAULT.name());
        HealthStyle style = HealthStyle.parse(s.beyonderHealthStyle);
        s.beyonderHealthAnchor = string(json, "beyonderHealthAnchor", BeyonderHealthOverlay.DEFAULT_ANCHOR);
        s.beyonderHealthXOffset = intOf(json, "beyonderHealthXOffset", BeyonderHealthOverlay.DEFAULT_X_OFFSET);
        s.beyonderHealthYOffset = intOf(json, "beyonderHealthYOffset", style.defaultYOffset());
        s.beyonderHealthScale = scale(json, "beyonderHealthScale");
    }

    private static void readCharacterPlate(JsonObject json, HudSettings s) {
        s.showCharacterPlate = flag(json, "showCharacterPlate", true);
        s.characterPlateAnchor = string(json, "characterPlateAnchor", "TOP_LEFT");
        s.characterPlateXOffset = intOf(json, "characterPlateXOffset", 0);
        s.characterPlateYOffset = intOf(json, "characterPlateYOffset", CharacterPlateOverlay.DEFAULT_TOP_Y);
        s.characterPlateScale = scale(json, "characterPlateScale");
        s.characterPlateOpacity = opacity(json, "characterPlateOpacity");
    }

    private static void readBars(JsonObject json, HudSettings s) {
        s.showMadnessBar = flag(json, "showMadnessBar", true);
        s.madnessXOffset = intOf(json, "madnessXOffset", 0);
        s.madnessYOffset = intOf(json, "madnessYOffset", HudConfig.DEFAULT_MADNESS_Y);
        s.madnessAnchor = string(json, "madnessAnchor", "TOP_LEFT");
        s.madnessScale = scale(json, "madnessScale");
        s.layoutVersion = intOf(json, "layoutVersion", 0);
        s.showSpiritualityBar = flag(json, "showSpiritualityBar", true);
        s.spiritualityAnchor = string(json, "spiritualityAnchor", "TOP_LEFT");
        s.spiritualityXOffset = intOf(json, "spiritualityXOffset", 0);
        s.spiritualityYOffset = intOf(json, "spiritualityYOffset", 50);
        s.spiritualityHideWhenFull = flag(json, "spiritualityHideWhenFull", true);
        s.spiritualityScale = scale(json, "spiritualityScale");
        s.showActingBar = flag(json, "showActingBar", true);
        s.actingAnchor = string(json, "actingAnchor", "TOP_LEFT");
        s.actingXOffset = intOf(json, "actingXOffset", 0);
        s.actingYOffset = intOf(json, "actingYOffset", 80);
        s.actingScale = scale(json, "actingScale");
        s.showResourceBars = flag(json, "showResourceBars", true);
        s.resourceAnchor = string(json, "resourceAnchor", "TOP_LEFT");
        s.resourceXOffset = intOf(json, "resourceXOffset", 0);
        s.resourceYOffset = intOf(json, "resourceYOffset", 100);
        s.resourceMaxBars = intOf(json, "resourceMaxBars", 4);
        s.resourceScale = scale(json, "resourceScale");
    }

    private static void readOverlays(JsonObject json, HudSettings s) {
        s.showActionBar = flag(json, "showActionBar", true);
        s.actionBarXOffset = intOf(json, "actionBarXOffset", 0);
        s.actionBarYOffset = intOf(json, "actionBarYOffset", 72);
        s.actionBarLines = intOf(json, "actionBarLines", 0);
        s.actionBarScale = scale(json, "actionBarScale");
        s.showTargetHealth = flag(json, "showTargetHealth", true);
        s.targetHealthXOffset = intOf(json, "targetHealthXOffset", 0);
        s.targetHealthYOffset = intOf(json, "targetHealthYOffset", 18);
        s.targetHealthScale = scale(json, "targetHealthScale");
        s.showCogitationOverlay = flag(json, "showCogitationOverlay", true);
        s.cogitationXOffset = intOf(json, "cogitationXOffset", 0);
        s.cogitationYOffset = intOf(json, "cogitationYOffset", -70);
        s.cogitationScale = scale(json, "cogitationScale");
        s.showNotifications = flag(json, "showNotifications", true);
        s.notificationXOffset = intOf(json, "notificationXOffset", 12);
        s.notificationYOffset = intOf(json, "notificationYOffset", 12);
        s.notificationScale = scale(json, "notificationScale");
    }

    private static void readGeneral(JsonObject json, HudSettings s) {
        s.epilepsyMode = flag(json, "epilepsyMode", false);
        s.effectSoundVolume = json.has("effectSoundVolume") ? json.get("effectSoundVolume").getAsFloat() : 1.0f;
        s.enableHallucinations = flag(json, "enableHallucinations", true);
        s.enableDiscordPresence = flag(json, "enableDiscordPresence", true);
        s.presenceShowMadness = flag(json, "presenceShowMadness", true);
        s.useServerMenus = flag(json, "useServerMenus", false);
        s.coiTitleScreen = flag(json, "coiTitleScreen", true);
    }

    /**
     * Reads the {@code slotPlacements} array defensively: a missing, short or
     * malformed entry just means "leave that slot in the row".
     */
    private static void readSlotPlacements(JsonObject json, HudSettings s) {
        HudConfig.clearSlotPlacements(s);
        if (!json.has("slotPlacements") || !json.get("slotPlacements").isJsonArray()) return;
        JsonArray array = json.getAsJsonArray("slotPlacements");
        int n = Math.min(array.size(), s.slotPlacements.length);
        for (int i = 0; i < n; i++) {
            s.slotPlacements[i] = placement(array.get(i));
        }
    }

    private static SlotPlacement placement(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject entry = element.getAsJsonObject();
        if (!entry.has("anchor")) return null;
        try {
            return new SlotPlacement(entry.get("anchor").getAsString(),
                    entry.has("x") ? entry.get("x").getAsInt() : 0,
                    entry.has("y") ? entry.get("y").getAsInt() : 0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * One per-element scale, clamped into
     * {@link HudConfig#MIN_ELEMENT_SCALE}..{@link HudConfig#MAX_ELEMENT_SCALE};
     * a missing or malformed entry reads as 1.0.
     */
    private static float scale(JsonObject json, String key) {
        if (!json.has(key)) return 1.0f;
        try {
            return Math.clamp(json.get(key).getAsFloat(), HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    /**
     * One per-element opacity, clamped into
     * {@link HudConfig#MIN_ELEMENT_OPACITY}..{@link HudConfig#MAX_ELEMENT_OPACITY};
     * a missing or malformed entry reads as 1.0.
     * <p>
     * Deliberately not {@link #scale}: the two share a shape but not their
     * bounds, and folding them together would let a later change to the scale
     * band quietly move the opacity floor that keeps text legible.
     */
    private static float opacity(JsonObject json, String key) {
        if (!json.has(key)) return 1.0f;
        try {
            return Math.clamp(json.get(key).getAsFloat(),
                    HudConfig.MIN_ELEMENT_OPACITY, HudConfig.MAX_ELEMENT_OPACITY);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    private static boolean flag(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int intOf(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }
}
