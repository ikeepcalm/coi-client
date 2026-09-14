package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;

/**
 * Acting progress for the local player, straight off {@code coi-client:acting}.
 * <p>
 * The server pushes on join, on request, every 60 ticks and immediately after
 * a grant. Between pushes the method cooldown is counted down locally
 * ({@link #cooldownRemainingNow()}) so the HUD reads like a real timer rather
 * than a value that jumps once every three seconds.
 */
public final class ClientActingState {

    /**
     * How long a {@code +N} gain popup stays on screen.
     */
    public static final long GRANT_POPUP_MS = 1200;

    private static String pathway = "";
    private static int sequence = -1;
    private static int acting = 0;
    private static int needed = 0;
    private static double percent = 0;
    private static int cooldownRemaining = 0;
    private static int cooldownTotal = 0;
    private static int overflow = 0;
    private static boolean overflowEligible = false;
    private static boolean limited = false;
    private static boolean outer = false;
    private static boolean hasData = false;
    private static long lastPacketMs = 0;

    private static int lastGrant = 0;
    private static String lastGrantSource = "";
    private static long lastGrantMs = 0;

    private ClientActingState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            // An empty object means "this player has no pathway" — drop back to no data
            if (root.entrySet().isEmpty()) {
                reset();
                return;
            }
            pathway = AbilityInfo.normalizePathway(string(root, "pathway"));
            sequence = root.has("sequence") ? root.get("sequence").getAsInt() : -1;
            acting = intOf(root, "acting");
            needed = intOf(root, "needed");
            percent = root.has("percent") ? root.get("percent").getAsDouble()
                    : (needed > 0 ? acting * 100.0 / needed : 0);
            cooldownRemaining = intOf(root, "cooldownRemaining");
            cooldownTotal = intOf(root, "cooldownTotal");
            overflow = intOf(root, "overflow");
            overflowEligible = bool(root, "overflowEligible");
            limited = bool(root, "limited");
            outer = bool(root, "outer");
            hasData = true;
            lastPacketMs = System.currentTimeMillis();

            int granted = intOf(root, "granted");
            if (granted > 0) {
                lastGrant = granted;
                lastGrantSource = string(root, "source");
                lastGrantMs = lastPacketMs;
            }
        } catch (Exception e) {
            System.err.println("COI Client: malformed acting payload: " + json);
        }
    }

    private static String string(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : "";
    }

    private static int intOf(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsInt() : 0;
    }

    private static boolean bool(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() && root.get(key).getAsBoolean();
    }

    /**
     * Method cooldown in seconds, counted down from the last packet.
     */
    public static int cooldownRemainingNow() {
        if (cooldownRemaining <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - lastPacketMs) / 1000L;
        return (int) Math.max(0, cooldownRemaining - elapsed);
    }

    /**
     * {@code mm:ss} for {@link #cooldownRemainingNow()}.
     */
    public static String cooldownClock() {
        int seconds = cooldownRemainingNow();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * The most recent grant while its popup is still alive, else 0.
     */
    public static int activeGrant() {
        return System.currentTimeMillis() - lastGrantMs < GRANT_POPUP_MS ? lastGrant : 0;
    }

    /**
     * 0 → 1 over the life of the gain popup.
     */
    public static float grantProgress() {
        long elapsed = System.currentTimeMillis() - lastGrantMs;
        if (elapsed < 0 || elapsed >= GRANT_POPUP_MS) return 1f;
        return elapsed / (float) GRANT_POPUP_MS;
    }

    public static String getPathway() {
        return pathway;
    }

    public static int getSequence() {
        return sequence;
    }

    public static int getActing() {
        return acting;
    }

    public static int getNeeded() {
        return needed;
    }

    public static double getPercent() {
        return percent;
    }

    public static int getCooldownTotal() {
        return cooldownTotal;
    }

    public static int getOverflow() {
        return overflow;
    }

    public static boolean isOverflowEligible() {
        return overflowEligible;
    }

    public static boolean isLimited() {
        return limited;
    }

    /**
     * Outer pathways have no acting progression, so the bar stays hidden.
     */
    public static boolean isOuter() {
        return outer;
    }

    public static boolean hasData() {
        return hasData;
    }

    public static String getLastGrantSource() {
        return lastGrantSource;
    }

    /**
     * Debug-screen entry point: fakes a grant of {@code percentPoints}% of the
     * requirement so the bar and its popup can be exercised without a server.
     */
    public static void debugGrant(int percentPoints) {
        if (!hasData) {
            pathway = ClientBeyonderState.hasIdentity() ? ClientBeyonderState.getPathway() : "fool";
            sequence = ClientBeyonderState.getSequence();
            needed = 5000;
            acting = 0;
            outer = false;
            hasData = true;
        }
        int granted = Math.max(1, needed * percentPoints / 100);
        acting = Math.clamp(acting + granted, 0, Math.max(needed, acting + granted));
        percent = needed > 0 ? acting * 100.0 / needed : 0;
        lastPacketMs = System.currentTimeMillis();
        lastGrant = granted;
        lastGrantSource = "DEBUG";
        lastGrantMs = lastPacketMs;
    }

    /**
     * Debug-screen entry point: starts a method cooldown of {@code seconds}.
     */
    public static void debugCooldown(int seconds) {
        if (!hasData) debugGrant(0);
        cooldownRemaining = seconds;
        cooldownTotal = Math.max(seconds, cooldownTotal);
        lastPacketMs = System.currentTimeMillis();
    }

    public static void reset() {
        pathway = "";
        sequence = -1;
        acting = 0;
        needed = 0;
        percent = 0;
        cooldownRemaining = 0;
        cooldownTotal = 0;
        overflow = 0;
        overflowEligible = false;
        limited = false;
        outer = false;
        hasData = false;
        lastPacketMs = 0;
        lastGrant = 0;
        lastGrantSource = "";
        lastGrantMs = 0;
    }
}
