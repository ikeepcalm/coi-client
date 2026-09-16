package dev.ua.ikeepcalm.coi.client.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class ResourceLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> implements PreparableReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER_NAME = "jsons";
    private static final Identifier PATHWAY_DATA = Identifier.fromNamespaceAndPath("coi-client", "archive/pathway-data.json");

    private final Map<String, IngredientInfo> ingredientLookup = new HashMap<>();
    private final Map<String, ChatFormatting> pathwayColors = new HashMap<>();

    @Override
    protected @NonNull Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
        Map<Identifier, JsonElement> map = new HashMap<>();

        resourceManager.listResources(FOLDER_NAME, path -> path.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement element = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                map.put(location, element);
            } catch (Exception e) {
                System.err.println("Failed to parse client JSON resource: " + location + " - " + e.getMessage());
            }
        });

        resourceManager.getResource(PATHWAY_DATA).ifPresent(resource -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement element = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                map.put(PATHWAY_DATA, element);
            } catch (Exception e) {
                System.err.println("Failed to load pathway data: " + e.getMessage());
            }
        });

        return map;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, @NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
        ingredientLookup.clear();
        pathwayColors.clear();

        prepared.forEach((_, element) -> {
            if (element.isJsonObject() && element.getAsJsonObject().has("pathways")) {
                parsePathwayData(element.getAsJsonObject());
            }
        });

        System.out.println("Loaded " + prepared.size() + " client JSON files, " + ingredientLookup.size() + " ingredients indexed.");
    }

    private void parsePathwayData(JsonObject root) {
        JsonObject pathways = root.getAsJsonObject("pathways");
        for (var pathwayEntry : pathways.entrySet()) {
            String pathwayKey = pathwayEntry.getKey();
            JsonObject pathway = pathwayEntry.getValue().getAsJsonObject();
            ChatFormatting color = parseColor(pathway.has("color") ? pathway.get("color").getAsString() : "white");
            pathwayColors.put(pathwayKey, color);
            JsonObject sequences = pathway.getAsJsonObject("sequences");
            if (sequences == null) continue;

            for (var seqEntry : sequences.entrySet()) {
                JsonObject sequence = seqEntry.getValue().getAsJsonObject();
                int level = sequence.get("level").getAsInt();
                JsonObject ingredients = sequence.getAsJsonObject("ingredients");
                if (ingredients == null) continue;

                parseIngredientList(ingredients, "main", pathwayKey, level, true, color);
                parseIngredientList(ingredients, "supplementary", pathwayKey, level, false, color);
            }
        }
    }

    private void parseIngredientList(JsonObject ingredients, String key, String pathway, int sequence, boolean isMain, ChatFormatting color) {
        if (!ingredients.has(key)) return;
        for (JsonElement elem : ingredients.getAsJsonArray(key)) {
            String id = elem.getAsJsonObject().get("id").getAsString();
            ingredientLookup.put(id, new IngredientInfo(pathway, sequence, isMain, color));
        }
    }

    private ChatFormatting parseColor(String colorStr) {
        return switch (colorStr) {
            case "gold" -> ChatFormatting.GOLD;
            case "aqua" -> ChatFormatting.AQUA;
            case "dark_gray" -> ChatFormatting.DARK_GRAY;
            case "yellow" -> ChatFormatting.YELLOW;
            case "green" -> ChatFormatting.GREEN;
            case "gray" -> ChatFormatting.GRAY;
            case "dark_purple" -> ChatFormatting.DARK_PURPLE;
            case "dark_red" -> ChatFormatting.DARK_RED;
            case "light_purple" -> ChatFormatting.LIGHT_PURPLE;
            case "red" -> ChatFormatting.RED;
            case "blue" -> ChatFormatting.BLUE;
            default -> ChatFormatting.WHITE;
        };
    }

    public IngredientInfo getIngredient(String id) {
        return ingredientLookup.get(id);
    }

    public ChatFormatting getPathwayColor(String pathway) {
        return pathwayColors.getOrDefault(pathway, ChatFormatting.WHITE);
    }
}
