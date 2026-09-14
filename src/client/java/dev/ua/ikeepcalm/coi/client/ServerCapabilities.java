package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashSet;
import java.util.Set;

/**
 * What the connected server said it can feed us, from the
 * {@code coi-client:server} reply to our hello.
 * <p>
 * Everything is optional and tolerated when missing: a server that never
 * replies leaves {@link #known()} false, and every {@link #has(String)} check
 * returns false — which is exactly how the client hides UI an old server has
 * no data for. Cleared on disconnect.
 */
public final class ServerCapabilities {

    private static String pluginVersion = "";
    private static int protocol = 0;
    private static Set<String> features = Set.of();
    private static boolean known = false;

    private ServerCapabilities() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            pluginVersion = root.has("pluginVersion") ? root.get("pluginVersion").getAsString() : "unknown";
            protocol = root.has("protocol") ? root.get("protocol").getAsInt() : 1;
            Set<String> parsed = new HashSet<>();
            if (root.has("features") && root.get("features").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("features")) {
                    parsed.add(element.getAsString());
                }
            }
            features = Set.copyOf(parsed);
            known = true;
            System.out.println("COI Client: server capabilities — version " + pluginVersion
                    + ", protocol " + protocol + ", features " + features);
        } catch (Exception e) {
            System.err.println("COI Client: malformed server capabilities payload: " + json);
        }
    }

    public static boolean has(String feature) {
        return features.contains(feature);
    }

    public static boolean known() {
        return known;
    }

    public static String pluginVersion() {
        return pluginVersion;
    }

    public static int protocol() {
        return protocol;
    }

    public static void reset() {
        pluginVersion = "";
        protocol = 0;
        features = Set.of();
        known = false;
    }
}
