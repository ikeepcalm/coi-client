package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local controls for the appearance system, persisted to {@code config/coi_appearance.json}:
 * visibility switches, and per-element fit knobs (chest shape, hair length/offset, wing
 * scale/flap, skin overlay opacity) so cosmetics can be tuned to the player's own skin.
 */
public final class AppearanceConfig {

    private static final int CONFIG_VERSION = 6;
    private static final Logger LOGGER = LoggerFactory.getLogger(AppearanceConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("coi_appearance.json");
    private static Settings settings = new Settings();

    private AppearanceConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            Settings loaded = GSON.fromJson(Files.readString(CONFIG_PATH), Settings.class);
            settings = loaded == null ? new Settings() : loaded;
            sanitize();
        } catch (IOException | JsonParseException exception) {
            LOGGER.warn("Failed to read appearance settings; using defaults", exception);
            settings = new Settings();
        }
    }

    public static void save() {
        sanitize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException exception) {
            LOGGER.warn("Failed to save appearance settings", exception);
        }
    }

    public static Settings get() {
        return settings;
    }

    public static Settings copySettings() {
        return GSON.fromJson(GSON.toJson(settings), Settings.class);
    }

    /** Changes the local preview; persistence remains an explicit save operation. */
    public static void setSettings(Settings value) {
        settings = value;
    }

    public static void reset() {
        settings = new Settings();
        save();
    }

    /**
     * Master visibility gate for trait rendering on a player: the global switch plus
     * the self/other split (the local player sees their own traits only when the camera
     * is not first-person — handled by the uniqueness manager for particles and by the
     * player being effectively unseen for their own body in first person).
     */
    public static boolean shouldRender(String playerUuid) {
        if (!settings.enabled || playerUuid == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        boolean self = minecraft.player != null
                && playerUuid.equals(minecraft.player.getUUID().toString());
        return self ? settings.showSelf : settings.showOthers;
    }

    public static boolean shouldRenderUniqueness(String playerUuid) {
        if (!shouldRender(playerUuid) || !settings.enableUniquenessEffects) return false;
        var player = Minecraft.getInstance().player;
        boolean self = player != null && playerUuid.equals(player.getUUID().toString());
        return self ? settings.uniquenessShowSelf : settings.uniquenessShowOthers;
    }

    private static void sanitize() {
        if (settings.configVersion < CONFIG_VERSION) {
            if (settings.overlayOpacity == 0.55f) settings.overlayOpacity = 0.8f;
            settings.configVersion = CONFIG_VERSION;
        }
        settings.chestScale = Math.clamp(settings.chestScale, 0.80f, 1.50f);
        settings.chestSeparationPixels = Math.clamp(settings.chestSeparationPixels, -0.40f, 1.0f);
        settings.chestYOffsetPixels = Math.clamp(settings.chestYOffsetPixels, -1.5f, 1.5f);
        settings.chestFullness = Math.clamp(settings.chestFullness, 0.75f, 1.35f);
        settings.hairLength = Math.clamp(settings.hairLength, 0.50f, 1.60f);
        settings.hairYOffsetPixels = Math.clamp(settings.hairYOffsetPixels, -1.5f, 1.5f);
        settings.wingScale = Math.clamp(settings.wingScale, 0.60f, 1.50f);
        settings.wingFlapSpeed = Math.clamp(settings.wingFlapSpeed, 0.20f, 3.0f);
        settings.overlayOpacity = Math.clamp(settings.overlayOpacity, 0.20f, 1.0f);
        settings.clawLength = Math.clamp(settings.clawLength, 0.5f, 1.5f);
        settings.clawSpread = Math.clamp(settings.clawSpread, 0.7f, 1.3f);
        settings.clawYOffsetPixels = Math.clamp(settings.clawYOffsetPixels, -1.0f, 1.0f);
        settings.clawZOffsetPixels = Math.clamp(settings.clawZOffsetPixels, -1.0f, 1.0f);
        settings.uniquenessParticleIntensity = Math.clamp(settings.uniquenessParticleIntensity, 0.15f, 1.0f);
    }

    public static final class Settings {
        public int configVersion = CONFIG_VERSION;
        // Visibility
        public boolean enabled = true;
        public boolean showSelf = true;
        public boolean showOthers = true;
        public boolean showBodyChanges = true;
        public boolean projectJacket = true;
        // Chest shape
        public float chestScale = 1.0f;
        public float chestSeparationPixels = 0.0f;
        public float chestYOffsetPixels = 0.0f;
        public float chestFullness = 1.0f;
        // Hair fit
        public float hairLength = 1.0f;
        public float hairYOffsetPixels = 0.0f;
        // Wings
        public float wingFlapSpeed = 1.0f;
        public float wingScale = 1.0f;
        // Skin overlay blending
        /** Readable material color while retaining the base skin's features. */
        public float overlayOpacity = 0.8f;
        public float clawLength = 1.0f;
        public float clawSpread = 1.0f;
        public float clawYOffsetPixels = 0.0f;
        public float clawZOffsetPixels = 0.0f;
        // Uniqueness particles
        public boolean enableUniquenessEffects = true;
        public boolean uniquenessShowSelf = true;
        public boolean uniquenessShowOthers = true;
        /** Sampling rate for pathway particles; stationary sigils remain readable at every value. */
        public float uniquenessParticleIntensity = 1.0f;
    }
}
