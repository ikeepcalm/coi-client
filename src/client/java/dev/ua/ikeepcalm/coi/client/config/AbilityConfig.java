package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
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
        for (int i = 0; i < abilities.length; i++) {
            json.addProperty("ability" + (i + 1), abilities[i]);
        }

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] loadBindings() {
        int maxAbilities = CircleOfImaginationClient.MAX_ABILITIES;
        String[] abilities = new String[maxAbilities];

        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = GSON.fromJson(content, JsonObject.class);

                for (int i = 0; i < maxAbilities; i++) {
                    String key = "ability" + (i + 1);
                    abilities[i] = json.has(key) && !json.get(key).isJsonNull() ?
                            json.get(key).getAsString() : null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return abilities;
    }
}