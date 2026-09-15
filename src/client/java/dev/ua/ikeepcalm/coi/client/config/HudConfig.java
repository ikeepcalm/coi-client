package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CharacterPlateOverlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Every HUD preference, and the {@code config/coi_hud.json} behind it.
 * <p>
 * The settings themselves are {@link HudSettings}; the field names there are
 * the on-disk keys, so renaming one silently resets that setting for every
 * existing player. The three jobs around them live next door —
 * {@link HudConfigReader} turns the file into settings, {@link HudConfigWriter}
 * turns settings back into the file, and {@link HudConfigMigrations} brings an
 * older file forward — leaving this class as the holder, the defaults and the
 * two disk calls.
 */
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
     * — see {@link HudConfigMigrations}.
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

    /**
     * Reads the file if there is one, migrates it, and writes a fresh one at
     * the defaults if there is not.
     */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            HudConfigReader.read(json, settings);
            HudConfigMigrations.migrate(settings);
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to read HUD settings", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(HudConfigWriter.write(settings)));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write HUD settings", e);
        }
    }

    /**
     * Field-by-field copy, used for the settings screens' working copies and
     * for the layout editor's Cancel snapshot.
     */
    public static void copySettings(HudSettings from, HudSettings to) {
        HudSettingsCopy.copy(from, to);
    }

    /**
     * Puts every ability slot back in the shared row — what the layout editor's
     * group reset does.
     */
    public static void clearSlotPlacements(HudSettings s) {
        s.slotPlacements = new SlotPlacement[AbilityBindings.MAX_ABILITIES];
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
        public SlotPlacement[] slotPlacements = new SlotPlacement[AbilityBindings.MAX_ABILITIES];
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
         * The Lord of the Mysteries main menu. Off falls the whole takeover
         * through to vanilla — panorama, logo, splash placement and button
         * sprites — leaving only the title-screen haunting, which predates it.
         */
        public boolean coiTitleScreen = true;
        /**
         * Layout schema of the file this was read from; see
         * {@link HudConfigMigrations}.
         */
        public int layoutVersion = LAYOUT_VERSION;
    }
}
