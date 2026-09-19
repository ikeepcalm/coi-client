package dev.ua.ikeepcalm.coi.client.ability;

import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-supplied casting categories. Keys stay opaque; labels never identify a cast. */
public final class AbilityCategories {
    public record Category(String id, String name, int cooldownSeconds, long readyAt) {
        public int remainingTicks() {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0, readyAt - System.currentTimeMillis() + 49) / 50);
        }
    }
    private static final Map<String, List<Category>> CATEGORIES = new HashMap<>();
    private static final Map<String, String> SELECTED = new HashMap<>();
    private static final Map<String, Long> LOCKS = new HashMap<>();
    private AbilityCategories() {}

    public static List<Category> get(String id) { return CATEGORIES.getOrDefault(id, List.of()); }
    public static String selected(String id) { return SELECTED.getOrDefault(id, ""); }
    public static Category find(String id, String category) {
        return get(id).stream().filter(c -> c.id().equals(category)).findFirst().orElse(null);
    }
    public static void clear() { CATEGORIES.clear(); SELECTED.clear(); LOCKS.clear(); }
    public static int lockTicks(String id) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, LOCKS.getOrDefault(id, 0L) - System.currentTimeMillis() + 49) / 50);
    }

    public static void read(String id, JsonObject json) {
        if (json.has("abilityLockTicks")) LOCKS.put(id,
                System.currentTimeMillis() + Math.max(0, JsonRead.intOf(json, "abilityLockTicks")) * 50L);
        if (json.has("selectedCategory")) SELECTED.put(id, JsonRead.string(json, "selectedCategory"));
        else if (json.has("category")) SELECTED.put(id, JsonRead.string(json, "category"));
        if (!json.has("castCategories") || !json.get("castCategories").isJsonArray()) return;
        List<Category> entries = new ArrayList<>();
        for (var element : json.getAsJsonArray("castCategories")) {
            if (entries.size() >= 128) break;
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String key = JsonRead.string(entry, "id");
            if (key.isBlank() || key.length() > 128 || entries.stream().anyMatch(c -> c.id().equals(key))) continue;
            int remaining = Math.max(0, JsonRead.intOf(entry, "cooldownRemainingTicks"));
            entries.add(new Category(key, JsonRead.string(entry, "name", key),
                    Math.max(0, JsonRead.intOf(entry, "cooldownSeconds")), System.currentTimeMillis() + remaining * 50L));
        }
        CATEGORIES.put(id, List.copyOf(entries));
    }
}
