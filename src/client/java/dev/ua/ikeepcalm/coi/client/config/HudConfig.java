package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.*;
import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.hud.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_hud.json");

    /**
     * Schema version of the positional settings. Bumped to 2 when the madness
     * bar started honouring {@code madnessYOffset} for TOP anchors too, to
     * 3 when {@code hudScale} and {@code slotSpacing} were retired in favour of
     * {@code slotSize} alone, and to 4 when the Beyonder health bar was widened
     * — see {@link #migrate}.
     */
    public static final int LAYOUT_VERSION = 4;

    /**
     * Where the madness bar sat before it was movable; still the default.
     */
    public static final int DEFAULT_MADNESS_Y = 20;

    /**
     * Range of the per-element scale settings ({@code madnessScale} and
     * friends). Every bar and overlay grows about its own fill origin, so the
     * spot the player dragged it to stays put - see
     * {@link dev.ua.ikeepcalm.coi.client.hud.HudScale}.
     */
    public static final float MIN_ELEMENT_SCALE = 0.5f;
    public static final float MAX_ELEMENT_SCALE = 2.0f;

    /**
     * The slot size every hand-tuned constant in {@code AbilitySlotWidget} was
     * drawn against; {@code slotSize} is expressed as a scale of it.
     */
    public static final int BASE_SLOT_SIZE = 40;
    public static final int DEFAULT_SLOT_SIZE = BASE_SLOT_SIZE;

    /**
     * Slot size bounds. The range is exactly {@link #MIN_ELEMENT_SCALE}..{@link
     * #MAX_ELEMENT_SCALE} of {@link #BASE_SLOT_SIZE}, so the derived pose scale
     * never has to be clamped away from what the slider promised.
     */
    public static final int MIN_SLOT_SIZE = Math.round(BASE_SLOT_SIZE * MIN_ELEMENT_SCALE);
    public static final int MAX_SLOT_SIZE = Math.round(BASE_SLOT_SIZE * MAX_ELEMENT_SCALE);

    private static HudSettings settings = new HudSettings();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);

                settings.enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
                settings.hudX = json.has("hudX") ? json.get("hudX").getAsInt() : 10;
                settings.hudYOffset = json.has("hudYOffset") ? json.get("hudYOffset").getAsInt() : 60;
                settings.slotSize = json.has("slotSize")
                        ? Math.clamp(json.get("slotSize").getAsInt(), MIN_SLOT_SIZE, MAX_SLOT_SIZE)
                        : DEFAULT_SLOT_SIZE;
                settings.showKeybinds = !json.has("showKeybinds") || json.get("showKeybinds").getAsBoolean();
                settings.showAbilityNames = !json.has("showAbilityNames") || json.get("showAbilityNames").getAsBoolean();
                settings.showGlowEffect = !json.has("showGlowEffect") || json.get("showGlowEffect").getAsBoolean();
                settings.wheelSlots = json.has("wheelSlots") ? json.get("wheelSlots").getAsInt() : 8;
                settings.activeAbilitySlots = json.has("activeAbilitySlots")
                        ? Math.clamp(json.get("activeAbilitySlots").getAsInt(), 1, CircleOfImaginationClient.MAX_ABILITIES) : 6;
                settings.epilepsyMode = json.has("epilepsyMode") && json.get("epilepsyMode").getAsBoolean();
                settings.showBeyonderHealth = !json.has("showBeyonderHealth") || json.get("showBeyonderHealth").getAsBoolean();
                settings.beyonderHealthAnchor = json.has("beyonderHealthAnchor") ? json.get("beyonderHealthAnchor").getAsString() : BeyonderHealthOverlay.DEFAULT_ANCHOR;
                settings.beyonderHealthXOffset = json.has("beyonderHealthXOffset") ? json.get("beyonderHealthXOffset").getAsInt() : BeyonderHealthOverlay.DEFAULT_X_OFFSET;
                settings.beyonderHealthYOffset = json.has("beyonderHealthYOffset") ? json.get("beyonderHealthYOffset").getAsInt() : BeyonderHealthOverlay.DEFAULT_Y_OFFSET;
                settings.beyonderHealthScale = readScale(json, "beyonderHealthScale");
                settings.showCharacterPlate = !json.has("showCharacterPlate") || json.get("showCharacterPlate").getAsBoolean();
                settings.characterPlateAnchor = json.has("characterPlateAnchor") ? json.get("characterPlateAnchor").getAsString() : "TOP_LEFT";
                settings.characterPlateXOffset = json.has("characterPlateXOffset") ? json.get("characterPlateXOffset").getAsInt() : 0;
                settings.characterPlateYOffset = json.has("characterPlateYOffset") ? json.get("characterPlateYOffset").getAsInt() : CharacterPlateOverlay.DEFAULT_TOP_Y;
                settings.characterPlateScale = readScale(json, "characterPlateScale");
                settings.showMadnessBar = !json.has("showMadnessBar") || json.get("showMadnessBar").getAsBoolean();
                settings.madnessXOffset = json.has("madnessXOffset") ? json.get("madnessXOffset").getAsInt() : 0;
                settings.madnessYOffset = json.has("madnessYOffset") ? json.get("madnessYOffset").getAsInt() : DEFAULT_MADNESS_Y;
                settings.madnessAnchor = json.has("madnessAnchor") ? json.get("madnessAnchor").getAsString() : "TOP_LEFT";
                settings.madnessScale = readScale(json, "madnessScale");
                settings.layoutVersion = json.has("layoutVersion") ? json.get("layoutVersion").getAsInt() : 0;
                settings.showSpiritualityBar = !json.has("showSpiritualityBar") || json.get("showSpiritualityBar").getAsBoolean();
                settings.spiritualityAnchor = json.has("spiritualityAnchor") ? json.get("spiritualityAnchor").getAsString() : "TOP_LEFT";
                settings.spiritualityXOffset = json.has("spiritualityXOffset") ? json.get("spiritualityXOffset").getAsInt() : 0;
                settings.spiritualityYOffset = json.has("spiritualityYOffset") ? json.get("spiritualityYOffset").getAsInt() : 50;
                settings.spiritualityHideWhenFull = !json.has("spiritualityHideWhenFull") || json.get("spiritualityHideWhenFull").getAsBoolean();
                settings.spiritualityScale = readScale(json, "spiritualityScale");
                settings.showActingBar = !json.has("showActingBar") || json.get("showActingBar").getAsBoolean();
                settings.actingAnchor = json.has("actingAnchor") ? json.get("actingAnchor").getAsString() : "TOP_LEFT";
                settings.actingXOffset = json.has("actingXOffset") ? json.get("actingXOffset").getAsInt() : 0;
                settings.actingYOffset = json.has("actingYOffset") ? json.get("actingYOffset").getAsInt() : 80;
                settings.actingScale = readScale(json, "actingScale");
                settings.showResourceBars = !json.has("showResourceBars") || json.get("showResourceBars").getAsBoolean();
                settings.resourceAnchor = json.has("resourceAnchor") ? json.get("resourceAnchor").getAsString() : "TOP_LEFT";
                settings.resourceXOffset = json.has("resourceXOffset") ? json.get("resourceXOffset").getAsInt() : 0;
                settings.resourceYOffset = json.has("resourceYOffset") ? json.get("resourceYOffset").getAsInt() : 100;
                settings.resourceMaxBars = json.has("resourceMaxBars") ? json.get("resourceMaxBars").getAsInt() : 4;
                settings.resourceScale = readScale(json, "resourceScale");
                settings.showActionBar = !json.has("showActionBar") || json.get("showActionBar").getAsBoolean();
                settings.actionBarXOffset = json.has("actionBarXOffset") ? json.get("actionBarXOffset").getAsInt() : 0;
                settings.actionBarYOffset = json.has("actionBarYOffset") ? json.get("actionBarYOffset").getAsInt() : 72;
                settings.actionBarLines = json.has("actionBarLines") ? json.get("actionBarLines").getAsInt() : 0;
                settings.actionBarScale = readScale(json, "actionBarScale");
                settings.showTargetHealth = !json.has("showTargetHealth") || json.get("showTargetHealth").getAsBoolean();
                settings.targetHealthXOffset = json.has("targetHealthXOffset") ? json.get("targetHealthXOffset").getAsInt() : 0;
                settings.targetHealthYOffset = json.has("targetHealthYOffset") ? json.get("targetHealthYOffset").getAsInt() : 18;
                settings.targetHealthScale = readScale(json, "targetHealthScale");
                settings.showCogitationOverlay = !json.has("showCogitationOverlay") || json.get("showCogitationOverlay").getAsBoolean();
                settings.cogitationXOffset = json.has("cogitationXOffset") ? json.get("cogitationXOffset").getAsInt() : 0;
                settings.cogitationYOffset = json.has("cogitationYOffset") ? json.get("cogitationYOffset").getAsInt() : -70;
                settings.cogitationScale = readScale(json, "cogitationScale");
                settings.showNotifications = !json.has("showNotifications") || json.get("showNotifications").getAsBoolean();
                settings.notificationXOffset = json.has("notificationXOffset") ? json.get("notificationXOffset").getAsInt() : 12;
                settings.notificationYOffset = json.has("notificationYOffset") ? json.get("notificationYOffset").getAsInt() : 12;
                settings.notificationScale = readScale(json, "notificationScale");
                settings.effectSoundVolume = json.has("effectSoundVolume") ? json.get("effectSoundVolume").getAsFloat() : 1.0f;
                settings.enableHallucinations = !json.has("enableHallucinations") || json.get("enableHallucinations").getAsBoolean();
                settings.enableDiscordPresence = !json.has("enableDiscordPresence") || json.get("enableDiscordPresence").getAsBoolean();
                settings.presenceShowMadness = !json.has("presenceShowMadness") || json.get("presenceShowMadness").getAsBoolean();
                settings.useServerMenus = json.has("useServerMenus") && json.get("useServerMenus").getAsBoolean();
                readSlotPlacements(json, settings);
                migrate(settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", settings.enabled);
        json.addProperty("hudX", settings.hudX);
        json.addProperty("hudYOffset", settings.hudYOffset);
        json.addProperty("slotSize", settings.slotSize);
        json.addProperty("showKeybinds", settings.showKeybinds);
        json.addProperty("showAbilityNames", settings.showAbilityNames);
        json.addProperty("showGlowEffect", settings.showGlowEffect);
        json.addProperty("wheelSlots", settings.wheelSlots);
        json.addProperty("activeAbilitySlots", settings.activeAbilitySlots);
        json.addProperty("epilepsyMode", settings.epilepsyMode);
        json.addProperty("showBeyonderHealth", settings.showBeyonderHealth);
        json.addProperty("beyonderHealthAnchor", settings.beyonderHealthAnchor);
        json.addProperty("beyonderHealthXOffset", settings.beyonderHealthXOffset);
        json.addProperty("beyonderHealthYOffset", settings.beyonderHealthYOffset);
        json.addProperty("beyonderHealthScale", settings.beyonderHealthScale);
        json.addProperty("showCharacterPlate", settings.showCharacterPlate);
        json.addProperty("characterPlateAnchor", settings.characterPlateAnchor);
        json.addProperty("characterPlateXOffset", settings.characterPlateXOffset);
        json.addProperty("characterPlateYOffset", settings.characterPlateYOffset);
        json.addProperty("characterPlateScale", settings.characterPlateScale);
        json.addProperty("showMadnessBar", settings.showMadnessBar);
        json.addProperty("madnessXOffset", settings.madnessXOffset);
        json.addProperty("madnessYOffset", settings.madnessYOffset);
        json.addProperty("madnessAnchor", settings.madnessAnchor);
        json.addProperty("madnessScale", settings.madnessScale);
        json.addProperty("layoutVersion", LAYOUT_VERSION);
        json.addProperty("showSpiritualityBar", settings.showSpiritualityBar);
        json.addProperty("spiritualityAnchor", settings.spiritualityAnchor);
        json.addProperty("spiritualityXOffset", settings.spiritualityXOffset);
        json.addProperty("spiritualityYOffset", settings.spiritualityYOffset);
        json.addProperty("spiritualityHideWhenFull", settings.spiritualityHideWhenFull);
        json.addProperty("spiritualityScale", settings.spiritualityScale);
        json.addProperty("showActingBar", settings.showActingBar);
        json.addProperty("actingAnchor", settings.actingAnchor);
        json.addProperty("actingXOffset", settings.actingXOffset);
        json.addProperty("actingYOffset", settings.actingYOffset);
        json.addProperty("actingScale", settings.actingScale);
        json.addProperty("showResourceBars", settings.showResourceBars);
        json.addProperty("resourceAnchor", settings.resourceAnchor);
        json.addProperty("resourceXOffset", settings.resourceXOffset);
        json.addProperty("resourceYOffset", settings.resourceYOffset);
        json.addProperty("resourceMaxBars", settings.resourceMaxBars);
        json.addProperty("resourceScale", settings.resourceScale);
        json.addProperty("showActionBar", settings.showActionBar);
        json.addProperty("actionBarXOffset", settings.actionBarXOffset);
        json.addProperty("actionBarYOffset", settings.actionBarYOffset);
        json.addProperty("actionBarLines", settings.actionBarLines);
        json.addProperty("actionBarScale", settings.actionBarScale);
        json.addProperty("showTargetHealth", settings.showTargetHealth);
        json.addProperty("targetHealthXOffset", settings.targetHealthXOffset);
        json.addProperty("targetHealthYOffset", settings.targetHealthYOffset);
        json.addProperty("targetHealthScale", settings.targetHealthScale);
        json.addProperty("showCogitationOverlay", settings.showCogitationOverlay);
        json.addProperty("cogitationXOffset", settings.cogitationXOffset);
        json.addProperty("cogitationYOffset", settings.cogitationYOffset);
        json.addProperty("cogitationScale", settings.cogitationScale);
        json.addProperty("showNotifications", settings.showNotifications);
        json.addProperty("notificationXOffset", settings.notificationXOffset);
        json.addProperty("notificationYOffset", settings.notificationYOffset);
        json.addProperty("notificationScale", settings.notificationScale);
        json.addProperty("effectSoundVolume", settings.effectSoundVolume);
        json.addProperty("enableHallucinations", settings.enableHallucinations);
        json.addProperty("enableDiscordPresence", settings.enableDiscordPresence);
        json.addProperty("presenceShowMadness", settings.presenceShowMadness);
        json.addProperty("useServerMenus", settings.useServerMenus);
        json.add("slotPlacements", writeSlotPlacements(settings));

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * One per-element scale, clamped into
     * {@link #MIN_ELEMENT_SCALE}..{@link #MAX_ELEMENT_SCALE}; a missing or
     * malformed entry reads as 1.0.
     */
    private static float readScale(JsonObject json, String key) {
        if (!json.has(key)) return 1.0f;
        try {
            return Math.clamp(json.get(key).getAsFloat(), MIN_ELEMENT_SCALE, MAX_ELEMENT_SCALE);
        } catch (RuntimeException e) {
            return 1.0f;
        }
    }

    /**
     * Brings an older config up to {@link #LAYOUT_VERSION}. Each step is
     * guarded by the version it was introduced in, so a file at version 0 gets
     * all of them in order rather than only the newest.
     * <p>
     * Before v2 a TOP-anchored madness bar ignored {@code madnessYOffset} and
     * was pinned at {@link #DEFAULT_MADNESS_Y}; the value in the file was
     * whatever the BOTTOM anchor would have used. Keeping it would teleport
     * every existing player's bar the moment the offset started mattering, so
     * top-anchored bars are reset to where they were actually drawn.
     * <p>
     * v3 retired {@code hudScale} and {@code slotSpacing}. {@code hudScale}
     * used to <em>divide</em> the slot row's {@code hudX}/{@code hudYOffset}
     * rather than scale anything, and {@code slotSpacing} was a second size
     * knob that could disagree with {@code slotSize}; both are now folded into
     * {@code slotSize}. Neither has a position-preserving conversion (the old
     * scale formula depended on the screen height), so the offsets are kept
     * exactly as written and the two keys are simply dropped on the next save.
     * Slots 2..N in the shared row therefore shift to the derived step; the
     * layout editor makes any nudge from there trivial.
     * <p>
     * v4 re-places the Beyonder health bar. {@code HudAnchor.resolve} centres on
     * the element's width, so the stored X offset is only meaningful against the
     * width it was calibrated for — widening the bar from 82 to 100 left every
     * existing config pointing 9px too far left. The offset is now derived from
     * {@code BAR_WIDTH} so it cannot drift again, and a pre-v4 file has the
     * bar's placement reset to those defaults. This only discards a deliberate
     * position if the player had already dragged this one bar, which is a far
     * smaller cost than leaving it visibly off-centre for everyone else.
     */
    private static void migrate(HudSettings s) {
        if (s.layoutVersion < 2 && HudAnchor.parse(s.madnessAnchor).isTop()) {
            s.madnessYOffset = DEFAULT_MADNESS_Y;
        }
        if (s.layoutVersion < 4) {
            s.beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
            s.beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
            s.beyonderHealthYOffset = BeyonderHealthOverlay.DEFAULT_Y_OFFSET;
        }
        s.layoutVersion = LAYOUT_VERSION;
    }

    /**
     * Field-by-field copy, used for the settings screens' working copies and
     * for the layout editor's Cancel snapshot.
     */
    public static void copySettings(HudSettings from, HudSettings to) {
        to.enabled = from.enabled;
        to.hudX = from.hudX;
        to.hudYOffset = from.hudYOffset;
        to.slotSize = from.slotSize;
        to.showKeybinds = from.showKeybinds;
        to.showAbilityNames = from.showAbilityNames;
        to.showGlowEffect = from.showGlowEffect;
        to.wheelSlots = from.wheelSlots;
        to.activeAbilitySlots = from.activeAbilitySlots;
        to.epilepsyMode = from.epilepsyMode;
        to.showBeyonderHealth = from.showBeyonderHealth;
        to.beyonderHealthAnchor = from.beyonderHealthAnchor;
        to.beyonderHealthXOffset = from.beyonderHealthXOffset;
        to.beyonderHealthYOffset = from.beyonderHealthYOffset;
        to.beyonderHealthScale = from.beyonderHealthScale;
        to.showCharacterPlate = from.showCharacterPlate;
        to.characterPlateAnchor = from.characterPlateAnchor;
        to.characterPlateXOffset = from.characterPlateXOffset;
        to.characterPlateYOffset = from.characterPlateYOffset;
        to.characterPlateScale = from.characterPlateScale;
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
        to.effectSoundVolume = from.effectSoundVolume;
        to.enableHallucinations = from.enableHallucinations;
        to.enableDiscordPresence = from.enableDiscordPresence;
        to.presenceShowMadness = from.presenceShowMadness;
        to.useServerMenus = from.useServerMenus;
        to.layoutVersion = from.layoutVersion;
        copySlotPlacements(from, to);
    }

    /**
     * {@link SlotPlacement} is immutable, so sharing the records is safe — only
     * the array itself has to be fresh.
     */
    private static void copySlotPlacements(HudSettings from, HudSettings to) {
        to.slotPlacements = new SlotPlacement[CircleOfImaginationClient.MAX_ABILITIES];
        int n = Math.min(to.slotPlacements.length, from.slotPlacements == null ? 0 : from.slotPlacements.length);
        if (from.slotPlacements != null) {
            System.arraycopy(from.slotPlacements, 0, to.slotPlacements, 0, n);
        }
    }

    /**
     * Puts every ability slot back in the shared row — what the presets and the
     * layout editor's group reset do.
     */
    public static void clearSlotPlacements(HudSettings s) {
        s.slotPlacements = new SlotPlacement[CircleOfImaginationClient.MAX_ABILITIES];
    }

    /**
     * Reads the {@code slotPlacements} array defensively: a missing, short or
     * malformed entry just means "leave that slot in the row".
     */
    private static void readSlotPlacements(JsonObject json, HudSettings s) {
        clearSlotPlacements(s);
        if (!json.has("slotPlacements") || !json.get("slotPlacements").isJsonArray()) return;
        JsonArray array = json.getAsJsonArray("slotPlacements");
        int n = Math.min(array.size(), s.slotPlacements.length);
        for (int i = 0; i < n; i++) {
            JsonElement element = array.get(i);
            if (element == null || !element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("anchor")) continue;
            try {
                s.slotPlacements[i] = new SlotPlacement(
                        entry.get("anchor").getAsString(),
                        entry.has("x") ? entry.get("x").getAsInt() : 0,
                        entry.has("y") ? entry.get("y").getAsInt() : 0);
            } catch (RuntimeException ignored) {
                s.slotPlacements[i] = null;
            }
        }
    }

    private static JsonArray writeSlotPlacements(HudSettings s) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < CircleOfImaginationClient.MAX_ABILITIES; i++) {
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

    public static HudSettings getSettings() {
        return settings;
    }

    public static void setSettings(HudSettings newSettings) {
        settings = newSettings;
        save();
    }

    public static void resetToDefaults() {
        settings = new HudSettings();
        save();
    }

    /**
     * One ability slot pulled out of the row: an anchor name plus the offsets
     * inside that anchor's frame, exactly like the bars use.
     */
    public record SlotPlacement(String anchor, int x, int y) {
    }

    public static class HudSettings {
        public boolean enabled = true;
        public int hudX = 10;
        public int hudYOffset = 60;
        /**
         * On-screen size of one ability slot box. This is the ability HUD's
         * only size knob: the slot is drawn at {@link #BASE_SLOT_SIZE} under a
         * pose scale of {@code slotSize / BASE_SLOT_SIZE}, so the keybind chip,
         * the name and the cooldown readout grow with the box instead of
         * staying stuck at one font size. The gap between slots in the shared
         * row follows it too.
         */
        public int slotSize = DEFAULT_SLOT_SIZE;
        public boolean showKeybinds = true;
        public boolean showAbilityNames = true;
        public boolean showGlowEffect = true;
        public int wheelSlots = 8;
        public int activeAbilitySlots = 6;
        /**
         * Per-slot overrides of the shared row: {@code null} leaves the slot
         * where {@code hudX}/{@code hudYOffset}/{@code slotSize} put it.
         */
        public SlotPlacement[] slotPlacements = new SlotPlacement[CircleOfImaginationClient.MAX_ABILITIES];
        public boolean epilepsyMode = false;
        /**
         * The Beyonder HP pool bar <em>replaces</em> the vanilla hearts rather
         * than sitting beside them, so switching it off is what hands the
         * hearts back - see {@link BeyonderHealthOverlay}. The defaults put it
         * exactly where the hearts were.
         */
        public boolean showBeyonderHealth = true;
        public String beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
        public int beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
        public int beyonderHealthYOffset = BeyonderHealthOverlay.DEFAULT_Y_OFFSET;
        public float beyonderHealthScale = 1.0f;
        /**
         * The character plate supersedes the madness, acting and resource bars:
         * while it is on, those three stand down rather than drawing the same
         * numbers twice. Spirituality is not part of the trade - it keeps its
         * own sprite-built bar either way.
         */
        public boolean showCharacterPlate = true;
        public String characterPlateAnchor = "TOP_LEFT";
        public int characterPlateXOffset = 0;
        public int characterPlateYOffset = CharacterPlateOverlay.DEFAULT_TOP_Y;
        public float characterPlateScale = 1.0f;
        public boolean showMadnessBar = true;
        public int madnessXOffset = 0;
        public int madnessYOffset = DEFAULT_MADNESS_Y;
        public String madnessAnchor = "TOP_LEFT";
        public float madnessScale = 1.0f;
        public boolean showSpiritualityBar = true;
        public String spiritualityAnchor = "TOP_LEFT";
        public int spiritualityXOffset = 0;
        public int spiritualityYOffset = 50;
        public boolean spiritualityHideWhenFull = true;
        public float spiritualityScale = 1.0f;
        public boolean showActingBar = true;
        public String actingAnchor = "TOP_LEFT";
        public int actingXOffset = 0;
        public int actingYOffset = 80;
        public float actingScale = 1.0f;
        public boolean showResourceBars = true;
        public String resourceAnchor = "TOP_LEFT";
        public int resourceXOffset = 0;
        public int resourceYOffset = 100;
        public int resourceMaxBars = 4;
        public float resourceScale = 1.0f;
        public boolean showActionBar = true;
        public int actionBarXOffset = 0;
        public int actionBarYOffset = 72;
        public int actionBarLines = 0;
        public float actionBarScale = 1.0f;
        public boolean showTargetHealth = true;
        public int targetHealthXOffset = 0;
        public int targetHealthYOffset = 18;
        public float targetHealthScale = 1.0f;
        public boolean showCogitationOverlay = true;
        public int cogitationXOffset = 0;
        public int cogitationYOffset = -70;
        public float cogitationScale = 1.0f;
        public boolean showNotifications = true;
        public int notificationXOffset = 12;
        public int notificationYOffset = 12;
        public float notificationScale = 1.0f;
        public float effectSoundVolume = 1.0f;
        public boolean enableHallucinations = true;
        public boolean enableDiscordPresence = true;
        public boolean presenceShowMadness = true;
        /**
         * Opt out of the mod's own menus: every sheet button then asks the
         * server for its original InvUI chest GUI instead of a
         * {@code coi-client:menu} document. Off by default - the point of the
         * menu protocol is that the player never sees a chest again - but the
         * old screens stay one checkbox away.
         */
        public boolean useServerMenus = false;
        /**
         * Layout schema of the file this was read from; see {@link #migrate}.
         */
        public int layoutVersion = LAYOUT_VERSION;
    }
}
