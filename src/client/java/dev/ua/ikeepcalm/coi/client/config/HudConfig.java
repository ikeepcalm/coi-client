package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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

    public static class HudSettings {
        public boolean enabled = true;
        public int hudX = 20;
        public int hudYOffset = 80;
        public int slotSize = 50;
        public int slotSpacing = 60;
        public boolean showKeybinds = true;
        public boolean showAbilityNames = true;
        public boolean showGlowEffect = true;
        public float hudScale = 1.0f;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);

                settings.enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
                settings.hudX = json.has("hudX") ? json.get("hudX").getAsInt() : 20;
                settings.hudYOffset = json.has("hudYOffset") ? json.get("hudYOffset").getAsInt() : 80;
                settings.slotSize = json.has("slotSize") ? json.get("slotSize").getAsInt() : 50;
                settings.slotSpacing = json.has("slotSpacing") ? json.get("slotSpacing").getAsInt() : 60;
                settings.showKeybinds = json.has("showKeybinds") ? json.get("showKeybinds").getAsBoolean() : true;
                settings.showAbilityNames = json.has("showAbilityNames") ? json.get("showAbilityNames").getAsBoolean() : true;
                settings.showGlowEffect = json.has("showGlowEffect") ? json.get("showGlowEffect").getAsBoolean() : true;
                settings.hudScale = json.has("hudScale") ? json.get("hudScale").getAsFloat() : 1.0f;
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
}