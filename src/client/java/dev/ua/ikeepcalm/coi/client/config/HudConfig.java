package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_hud.json");

    private static HudSettings settings = new HudSettings();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);

                settings.enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
                settings.hudX = json.has("hudX") ? json.get("hudX").getAsInt() : 10;
                settings.hudYOffset = json.has("hudYOffset") ? json.get("hudYOffset").getAsInt() : 60;
                settings.slotSize = json.has("slotSize") ? json.get("slotSize").getAsInt() : 40;
                settings.slotSpacing = json.has("slotSpacing") ? json.get("slotSpacing").getAsInt() : 50;
                settings.showKeybinds = !json.has("showKeybinds") || json.get("showKeybinds").getAsBoolean();
                settings.showAbilityNames = !json.has("showAbilityNames") || json.get("showAbilityNames").getAsBoolean();
                settings.showGlowEffect = !json.has("showGlowEffect") || json.get("showGlowEffect").getAsBoolean();
                settings.hudScale = json.has("hudScale") ? json.get("hudScale").getAsFloat() : 1.0f;
                settings.wheelSlots = json.has("wheelSlots") ? json.get("wheelSlots").getAsInt() : 8;
                settings.activeAbilitySlots = json.has("activeAbilitySlots")
                        ? Math.clamp(json.get("activeAbilitySlots").getAsInt(), 1, CircleOfImaginationClient.MAX_ABILITIES) : 6;
                settings.epilepsyMode = json.has("epilepsyMode") && json.get("epilepsyMode").getAsBoolean();
                settings.showMadnessBar = !json.has("showMadnessBar") || json.get("showMadnessBar").getAsBoolean();
                settings.madnessXOffset = json.has("madnessXOffset") ? json.get("madnessXOffset").getAsInt() : 0;
                settings.madnessYOffset = json.has("madnessYOffset") ? json.get("madnessYOffset").getAsInt() : 55;
                settings.madnessAnchor = json.has("madnessAnchor") ? json.get("madnessAnchor").getAsString() : "TOP_LEFT";
                settings.effectSoundVolume = json.has("effectSoundVolume") ? json.get("effectSoundVolume").getAsFloat() : 1.0f;
                settings.enableHallucinations = !json.has("enableHallucinations") || json.get("enableHallucinations").getAsBoolean();
                settings.enableDiscordPresence = !json.has("enableDiscordPresence") || json.get("enableDiscordPresence").getAsBoolean();
                settings.presenceShowMadness = !json.has("presenceShowMadness") || json.get("presenceShowMadness").getAsBoolean();
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
        json.addProperty("slotSpacing", settings.slotSpacing);
        json.addProperty("showKeybinds", settings.showKeybinds);
        json.addProperty("showAbilityNames", settings.showAbilityNames);
        json.addProperty("showGlowEffect", settings.showGlowEffect);
        json.addProperty("hudScale", settings.hudScale);
        json.addProperty("wheelSlots", settings.wheelSlots);
        json.addProperty("activeAbilitySlots", settings.activeAbilitySlots);
        json.addProperty("epilepsyMode", settings.epilepsyMode);
        json.addProperty("showMadnessBar", settings.showMadnessBar);
        json.addProperty("madnessXOffset", settings.madnessXOffset);
        json.addProperty("madnessYOffset", settings.madnessYOffset);
        json.addProperty("madnessAnchor", settings.madnessAnchor);
        json.addProperty("effectSoundVolume", settings.effectSoundVolume);
        json.addProperty("enableHallucinations", settings.enableHallucinations);
        json.addProperty("enableDiscordPresence", settings.enableDiscordPresence);
        json.addProperty("presenceShowMadness", settings.presenceShowMadness);

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static class HudSettings {
        public boolean enabled = true;
        public int hudX = 10;
        public int hudYOffset = 60;
        public int slotSize = 40;
        public int slotSpacing = 50;
        public boolean showKeybinds = true;
        public boolean showAbilityNames = true;
        public boolean showGlowEffect = true;
        public float hudScale = 1.0f;
        public int wheelSlots = 8;
        public int activeAbilitySlots = 6;
        public boolean epilepsyMode = false;
        public boolean showMadnessBar = true;
        public int madnessXOffset = 0;
        public int madnessYOffset = 55;
        public String madnessAnchor = "TOP_LEFT";
        public float effectSoundVolume = 1.0f;
        public boolean enableHallucinations = true;
        public boolean enableDiscordPresence = true;
        public boolean presenceShowMadness = true;
    }
}
