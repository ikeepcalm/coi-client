package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The last Beyonder the local player hit with an ability.
 * <p>
 * One {@code coi-client:target} packet per hit replaces the server's 60
 * one-tick action-bar publishes; the drain animation from {@code before} to
 * {@code after} is played out client-side.
 */
public final class ClientTargetState {

    /**
     * How long the bar stays on screen after the last hit.
     */
    public static final long VISIBLE_MS = 3000;

    /**
     * How long the lost chunk stays white before settling to dark red.
     */
    public static final long FLASH_MS = 600;

    private static String name = "";
    private static float before = 0f;
    private static float after = 0f;
    private static double health = 0;
    private static double max = 0;
    private static long receivedAt = 0;

    private ClientTargetState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            name = root.has("name") && !root.get("name").isJsonNull() ? root.get("name").getAsString() : "";
            before = clamp01(root.has("before") ? root.get("before").getAsFloat() : 0f);
            after = clamp01(root.has("after") ? root.get("after").getAsFloat() : 0f);
            health = root.has("health") ? root.get("health").getAsDouble() : 0;
            max = root.has("max") ? root.get("max").getAsDouble() : 0;
            receivedAt = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("COI Client: malformed target payload: " + json);
        }
    }

    private static float clamp01(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    public static boolean isVisible(long now) {
        return receivedAt > 0 && now - receivedAt < VISIBLE_MS;
    }

    /**
     * True while the chunk the hit removed is still flashing white.
     */
    public static boolean isFlashing(long now) {
        return now - receivedAt < FLASH_MS;
    }

    public static String getName() {
        return name;
    }

    public static float getBefore() {
        return before;
    }

    public static float getAfter() {
        return after;
    }

    public static double getHealth() {
        return health;
    }

    public static double getMax() {
        return max;
    }

    /**
     * Debug-screen entry point: a fake 0.82 → 0.75 hit on a 200 HP target.
     */
    public static void debugHit() {
        name = "Steve";
        before = 0.82f;
        after = 0.75f;
        health = 150.0;
        max = 200.0;
        receivedAt = System.currentTimeMillis();
    }

    public static void reset() {
        name = "";
        before = 0f;
        after = 0f;
        health = 0;
        max = 0;
        receivedAt = 0;
    }
}
