package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * The defensive reads a menu document is parsed with.
 * <p>
 * Nothing here throws, and that is the whole contract: the document comes from
 * a plugin that may be newer than this client and is rendered as a whole
 * screen, so a missing key, a value of the wrong JSON type and a number that
 * will not parse all read as "absent" rather than as a failure. Lengths are
 * trimmed and arrays are capped at the call site, because a runaway
 * server-side loop must cost a truncated row, not a card a hundred thousand
 * pixels tall.
 */
final class MenuJson {

    private MenuJson() {
    }

    /**
     * The objects in an array, in order, up to {@code cap}. Anything in the
     * array that is not an object is skipped rather than counted.
     */
    static List<JsonObject> objects(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        JsonArray array = element.getAsJsonArray();
        List<JsonObject> objects = new ArrayList<>();
        for (JsonElement item : array) {
            if (objects.size() >= cap) break;
            if (item.isJsonObject()) objects.add(item.getAsJsonObject());
        }
        return objects;
    }

    /**
     * The strings in an array, each trimmed to {@link MenuLimits#MAX_TEXT}.
     */
    static List<String> strings(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<String> lines = new ArrayList<>();
        for (JsonElement line : element.getAsJsonArray()) {
            if (lines.size() >= cap) break;
            if (line.isJsonPrimitive()) lines.add(trim(line.getAsString(), MenuLimits.MAX_TEXT));
        }
        return List.copyOf(lines);
    }

    /**
     * Absent means enabled: most components are, and a server that has to spell
     * out {@code "enabled":true} on every button will eventually forget one.
     */
    static boolean enabled(JsonObject node) {
        return !node.has("enabled") || bool(node, "enabled");
    }

    static String string(JsonObject node, String key, int max) {
        if (node == null || !node.has(key)) return "";
        JsonElement element = node.get(key);
        if (!element.isJsonPrimitive()) return "";
        return trim(element.getAsString(), max);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    static int intOf(JsonObject node, String key, int fallback) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsInt() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static double dbl(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsDouble() : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static boolean bool(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() && node.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * A 0..1 fraction, clamped — a server that sends 1.4 gets a full bar rather
     * than one drawn past its own frame.
     */
    static double fraction(JsonObject node, String key) {
        return Math.clamp(dbl(node, key), 0.0, 1.0);
    }

    /**
     * Six hex digits with no {@code #} to 0xRRGGBB; 0 means "unspecified", which
     * every caller reads as "use the document's accent or the default".
     */
    static int hex(String value) {
        String clean = value.startsWith("#") ? value.substring(1) : value;
        if (clean.length() != 6) return 0;
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * The colour at {@code key}, read and converted in one step — the pairing
     * these two are always used in.
     */
    static int color(JsonObject node, String key) {
        return hex(string(node, key, MenuLimits.MAX_COLOR));
    }
}
