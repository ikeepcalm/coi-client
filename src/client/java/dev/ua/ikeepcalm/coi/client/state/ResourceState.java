package dev.ua.ikeepcalm.coi.client.state;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ability resource meters, straight off {@code coi-client:resource}.
 * <p>
 * Each bar is keyed by its {@code id}; a refresh overwrites the entry without
 * moving it, so a self-refreshing server passive doesn't make the stack
 * reshuffle every few hundred milliseconds. Entries with a non-zero
 * {@code ttlMs} expire locally, which is what keeps the HUD clean when the
 * server stops sending (logout, death, ability cancelled).
 */
public class ResourceState {

    /**
     * One bar. {@code expiresAt == 0} means "persistent until an explicit remove".
     */
    public record Entry(String id, String label, double current, double max, int rgb,
                        long expiresAt, boolean percent, long receivedAt) {
    }

    private static final int DEFAULT_RGB = 0xFFFFFF;

    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private ResourceState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String id = JsonRead.string(root, "id");
            if (id.isEmpty()) return;
            if (JsonRead.bool(root, "remove")) {
                entries.remove(id);
                return;
            }
            long now = System.currentTimeMillis();
            long ttl = JsonRead.longOf(root, "ttlMs");
            double max = JsonRead.dbl(root, "max");
            if (!(max > 0)) max = 1;
            double current = Math.clamp(JsonRead.dbl(root, "current"), 0, max);
            // LinkedHashMap keeps the original slot when an existing key is re-put
            entries.put(id, new Entry(
                    id,
                    JsonRead.string(root, "label"),
                    current,
                    max,
                    parseColor(JsonRead.string(root, "color")),
                    ttl > 0 ? now + ttl : 0,
                    !"value".equalsIgnoreCase(JsonRead.string(root, "format")),
                    now
            ));
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed resource payload: {}", json);
        }
    }

    /**
     * Live bars in arrival order; expired ones are swept on the way out.
     */
    public static List<Entry> visible() {
        sweep();
        return new ArrayList<>(entries.values());
    }

    /**
     * Cheap enough for the render gate — no copy, just the sweep.
     */
    public static boolean hasData() {
        sweep();
        return !entries.isEmpty();
    }

    private static void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Entry> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.expiresAt() != 0 && now >= entry.expiresAt()) it.remove();
        }
    }

    public static void reset() {
        entries.clear();
    }

    /**
     * Debug-screen entry point: two fake bars, one of each format, so the
     * overlay can be exercised without a server.
     */
    public static void debugInject() {
        handle("{\"id\":\"debug:rage\",\"label\":\"Rage Meter\",\"current\":62.5,\"max\":100,"
                + "\"color\":\"FF5555\",\"ttlMs\":8000,\"format\":\"percent\"}");
        handle("{\"id\":\"debug:seeds\",\"label\":\"Seeds\",\"current\":3,\"max\":5,"
                + "\"color\":\"55FF55\",\"ttlMs\":8000,\"format\":\"value\"}");
    }

    public static void debugClear() {
        reset();
    }

    private static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT_RGB;
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return DEFAULT_RGB;
        }
    }
}
