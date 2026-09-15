package dev.ua.ikeepcalm.coi.client.network;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;

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

    /**
     * What a reply that names no version is recorded as; the empty string means
     * "no reply at all".
     */
    private static final String UNKNOWN_VERSION = "unknown";

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
            pluginVersion = JsonRead.string(root, "pluginVersion", UNKNOWN_VERSION);
            // A reply that names no protocol is a protocol-1 server
            protocol = JsonRead.intOf(root, "protocol", 1);
            features = readFeatures(root);
            known = true;
            CoiLog.LOG.info("Server capabilities — version {}, protocol {}, features {}", pluginVersion, protocol, features);
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed server capabilities payload: {}", json);
        }
    }

    private static Set<String> readFeatures(JsonObject root) {
        if (!root.has("features") || !root.get("features").isJsonArray()) return Set.of();
        Set<String> parsed = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("features")) {
            parsed.add(element.getAsString());
        }
        return Set.copyOf(parsed);
    }

    /**
     * The one question the rest of the client asks: false for every feature
     * until a server has actually replied, which is what hides the UI an old
     * server has no data for.
     */
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
