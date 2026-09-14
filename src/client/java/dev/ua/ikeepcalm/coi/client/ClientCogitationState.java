package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The running cogitation session, driven by {@code coi-client:cogitation}.
 * <p>
 * The server used to deliver prompts as vanilla titles; for capable clients it
 * sends the action id, its localized label, the streak and the timeout
 * instead, and the overlay draws them. Note the server only checks for
 * timeouts every 40 ticks, so the local time bar can sit at 0 for up to two
 * seconds before a {@code fail} actually arrives.
 */
public final class ClientCogitationState {

    /**
     * How long the "Wrong!" line stays up after a failed action.
     */
    public static final long FAIL_MS = 600;

    private static boolean active = false;
    private static String action = "";
    private static String label = "";
    private static int streak = 0;
    private static long promptAt = 0;
    private static long timeoutMs = 0;
    private static long failAt = 0;

    private ClientCogitationState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String state = root.has("state") ? root.get("state").getAsString() : "";
            switch (state) {
                case "start" -> start();
                case "prompt" -> prompt(root);
                case "fail" -> fail(root);
                case "stop" -> reset();
                default -> {
                }
            }
        } catch (Exception e) {
            System.err.println("COI Client: malformed cogitation payload: " + json);
        }
    }

    private static void start() {
        reset();
        active = true;
    }

    private static void prompt(JsonObject root) {
        active = true;
        action = root.has("action") ? root.get("action").getAsString() : "";
        label = root.has("label") && !root.get("label").isJsonNull() ? root.get("label").getAsString() : action;
        streak = root.has("streak") ? root.get("streak").getAsInt() : 0;
        timeoutMs = root.has("timeoutMs") ? root.get("timeoutMs").getAsLong() : 5000L;
        promptAt = System.currentTimeMillis();
        failAt = 0;
    }

    private static void fail(JsonObject root) {
        streak = root.has("streak") ? root.get("streak").getAsInt() : 0;
        failAt = System.currentTimeMillis();
    }

    /**
     * True while a session is running and a prompt has actually been issued.
     */
    public static boolean hasPrompt() {
        return active && promptAt > 0 && !label.isEmpty();
    }

    /**
     * 1 → 0 over the prompt's timeout, clamped at 0 once it lapses.
     */
    public static float timeFraction(long now) {
        if (promptAt <= 0 || timeoutMs <= 0) return 0f;
        return Math.clamp(1f - (now - promptAt) / (float) timeoutMs, 0f, 1f);
    }

    public static boolean isFailing(long now) {
        return failAt > 0 && now - failAt < FAIL_MS;
    }

    public static boolean isActive() {
        return active;
    }

    public static String getAction() {
        return action;
    }

    public static String getLabel() {
        return label;
    }

    public static int getStreak() {
        return streak;
    }

    /**
     * Debug-screen entry point: a live TURN_360 prompt with no server.
     */
    public static void debugPrompt() {
        active = true;
        action = "TURN_360";
        label = "Turn Around (360°)";
        streak = 3;
        timeoutMs = 5000;
        promptAt = System.currentTimeMillis();
        failAt = 0;
    }

    public static void reset() {
        active = false;
        action = "";
        label = "";
        streak = 0;
        promptAt = 0;
        timeoutMs = 0;
        failAt = 0;
    }
}
