package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AbilityConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("coi_abilities.json");
    
    public static void saveBindings(String[] abilities) {
        JsonObject json = new JsonObject();
        json.addProperty("ability1", abilities[0]);
        json.addProperty("ability2", abilities[1]);
        json.addProperty("ability3", abilities[2]);
        
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String[] loadBindings() {
        String[] abilities = new String[3];
        
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);
                
                abilities[0] = json.has("ability1") ? json.get("ability1").getAsString() : null;
                abilities[1] = json.has("ability2") ? json.get("ability2").getAsString() : null;
                abilities[2] = json.has("ability3") ? json.get("ability3").getAsString() : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return abilities;
    }
}