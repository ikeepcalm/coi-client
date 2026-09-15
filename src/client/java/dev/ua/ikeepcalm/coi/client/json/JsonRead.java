package dev.ua.ikeepcalm.coi.client.json;

import com.google.gson.JsonObject;

/**
 * The defensive reads every server-fed JSON payload is parsed with.
 * <p>
 * Each of the {@code coi-client:*} JSON channels is optional in both
 * directions: an older plugin omits whole objects, a newer one adds keys this
 * client has never heard of, and either end may send {@code null} where a value
 * is expected. Every accessor here therefore answers "absent" with the caller's
 * default instead of throwing, so a state holder's {@code handle} only has to
 * guard the two things that genuinely are fatal — a body that is not an object,
 * and a value of the wrong Java type.
 * <p>
 * Deliberately not a parser: it reads scalars off an object that has already
 * been parsed, which is what made the same six methods appear in every
 * {@code client.state} class before they were lifted here.
 */
public final class JsonRead {

    private JsonRead() {
    }

    /**
     * @return the nested object, or null when the key is absent or holds
     * something that is not an object
     */
    public static JsonObject object(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : null;
    }

    public static boolean present(JsonObject root, String key) {
        return root != null && root.has(key) && !root.get(key).isJsonNull();
    }

    public static String string(JsonObject root, String key) {
        return string(root, key, "");
    }

    public static String string(JsonObject root, String key, String fallback) {
        return present(root, key) ? root.get(key).getAsString() : fallback;
    }

    public static int intOf(JsonObject root, String key) {
        return intOf(root, key, 0);
    }

    public static int intOf(JsonObject root, String key, int fallback) {
        return present(root, key) ? root.get(key).getAsInt() : fallback;
    }

    public static long longOf(JsonObject root, String key) {
        return longOf(root, key, 0L);
    }

    public static long longOf(JsonObject root, String key, long fallback) {
        return present(root, key) ? root.get(key).getAsLong() : fallback;
    }

    public static double dbl(JsonObject root, String key) {
        return dbl(root, key, 0);
    }

    public static double dbl(JsonObject root, String key, double fallback) {
        return present(root, key) ? root.get(key).getAsDouble() : fallback;
    }

    public static float floatOf(JsonObject root, String key, float fallback) {
        return present(root, key) ? root.get(key).getAsFloat() : fallback;
    }

    /**
     * Absent reads as false, so a flag only ever has to be sent when it is on.
     */
    public static boolean bool(JsonObject root, String key) {
        return present(root, key) && root.get(key).getAsBoolean();
    }
}
