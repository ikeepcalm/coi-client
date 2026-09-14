package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The character sheet snapshot from {@code coi-client:sheet}.
 * <p>
 * Every field is optional on the wire: an older server may omit whole
 * sections, so each getter has a sane default and {@link #acting()} is simply
 * {@code null} for outer pathways. The screen re-reads this holder every frame
 * so the 60-tick pushes show up live while it is open.
 */
public final class ClientSheetState {

    /**
     * One acting ledger row: how much a single source has contributed and the
     * cap it is allowed to reach ({@code unlimited} means there is none).
     */
    public record Source(String source, String label, int contributed, int cap, boolean unlimited) {
        public boolean capped() {
            return !unlimited && cap > 0 && contributed >= cap;
        }
    }

    /**
     * The overflow bank that catches acting earned past the requirement.
     */
    public record Overflow(boolean eligible, int banked, int ceiling, boolean uncapped, int foreignThrottlePercent) {
    }

    /**
     * Acting progress plus its ledger. Absent for outer pathways.
     */
    public record Acting(int acting, int needed, double percent, boolean limited,
                         int cooldownRemaining, int cooldownTotal,
                         List<Source> sources, Overflow overflow) {
    }

    /**
     * A present-or-not 0..1 meter, e.g. the Line of Life and Death.
     */
    public record Gauge(boolean present, double value) {
    }

    /**
     * Frenzied Mage's Presence stacks, present only when the player owns it.
     */
    public record Pressure(boolean present, int stacks, int cap) {
    }

    /**
     * Which server-side sub-menus this player may open. {@code terrainDamage}
     * is the odd one out: it is the current toggle state, not a gate.
     */
    public record Actions(boolean church, boolean abilities, boolean mythical, boolean uniqueness,
                          boolean honorific, boolean map, boolean seat, boolean terrainDamage) {
        public static final Actions NONE = new Actions(false, false, false, false, false, false, false, false);
    }

    private static String pathway = "";
    private static String pathwayName = "";
    private static int pathwayColor = 0;
    private static int sequence = -1;
    private static String sequenceName = "";
    private static boolean outer = false;

    private static double health = 0;
    private static double maxHealth = 0;
    private static int spirituality = 0;
    private static int maxSpirituality = 0;

    private static double madness = 0;
    private static double permanentFloor = 0;
    private static double godhoodFloor = 0;
    private static int madnessStage = 0;
    private static double tiredness = 0;
    private static int tirednessStage = 0;

    private static Acting acting = null;
    private static Gauge lifeAndDeath = new Gauge(false, 0);
    private static Pressure pressure = new Pressure(false, 0, 0);
    private static boolean anomaly = false;
    private static Actions actions = Actions.NONE;

    private static boolean hasData = false;
    private static long receivedAt = 0;

    private ClientSheetState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            readIdentity(root);
            readVitals(root);
            readMind(root);
            acting = root.has("acting") && root.get("acting").isJsonObject()
                    ? readActing(root.getAsJsonObject("acting")) : null;
            lifeAndDeath = readGauge(obj(root, "lifeAndDeath"));
            pressure = readPressure(obj(root, "pressure"));
            anomaly = bool(root, "anomaly");
            actions = readActions(obj(root, "actions"));
            hasData = true;
            receivedAt = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("COI Client: malformed sheet payload: " + json);
        }
    }

    private static void readIdentity(JsonObject root) {
        pathway = AbilityInfo.normalizePathway(string(root, "pathway"));
        pathwayName = string(root, "pathwayName");
        pathwayColor = parseHexColor(string(root, "pathwayColor"));
        sequence = root.has("sequence") ? root.get("sequence").getAsInt() : -1;
        sequenceName = string(root, "sequenceName");
        outer = bool(root, "outer");
    }

    private static void readVitals(JsonObject root) {
        health = dbl(root, "health");
        maxHealth = dbl(root, "maxHealth");
        spirituality = intOf(root, "spirituality");
        maxSpirituality = intOf(root, "maxSpirituality");
    }

    private static void readMind(JsonObject root) {
        madness = dbl(root, "madness");
        permanentFloor = dbl(root, "permanentFloor");
        godhoodFloor = dbl(root, "godhoodFloor");
        madnessStage = Math.clamp(intOf(root, "madnessStage"), 0, 4);
        tiredness = dbl(root, "tiredness");
        tirednessStage = Math.clamp(intOf(root, "tirednessStage"), 0, 4);
    }

    private static Acting readActing(JsonObject node) {
        List<Source> sources = new ArrayList<>();
        if (node.has("sources") && node.get("sources").isJsonArray()) {
            for (JsonElement element : node.getAsJsonArray("sources")) {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                sources.add(new Source(string(row, "source"), string(row, "label"),
                        intOf(row, "contributed"), intOf(row, "cap"), bool(row, "unlimited")));
            }
        }
        int current = intOf(node, "acting");
        int needed = intOf(node, "needed");
        double percent = node.has("percent") ? node.get("percent").getAsDouble()
                : (needed > 0 ? current * 100.0 / needed : 0);
        return new Acting(current, needed, percent, bool(node, "limited"),
                intOf(node, "cooldownRemaining"), intOf(node, "cooldownTotal"),
                List.copyOf(sources), readOverflow(obj(node, "overflow")));
    }

    private static Overflow readOverflow(JsonObject node) {
        if (node == null) return new Overflow(false, 0, 0, false, 0);
        return new Overflow(bool(node, "eligible"), intOf(node, "banked"), intOf(node, "ceiling"),
                bool(node, "uncapped"), intOf(node, "foreignThrottlePercent"));
    }

    private static Gauge readGauge(JsonObject node) {
        if (node == null) return new Gauge(false, 0);
        return new Gauge(bool(node, "present"), dbl(node, "meter"));
    }

    private static Pressure readPressure(JsonObject node) {
        if (node == null) return new Pressure(false, 0, 0);
        return new Pressure(bool(node, "present"), intOf(node, "stacks"), intOf(node, "cap"));
    }

    private static Actions readActions(JsonObject node) {
        if (node == null) return Actions.NONE;
        return new Actions(bool(node, "church"), bool(node, "abilities"), bool(node, "mythical"),
                bool(node, "uniqueness"), bool(node, "honorific"), bool(node, "map"),
                bool(node, "seat"), bool(node, "terrainDamage"));
    }

    // --- JSON helpers (every read tolerates a missing or null key) ---

    private static JsonObject obj(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : null;
    }

    private static String string(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : "";
    }

    private static int intOf(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsInt() : 0;
    }

    private static double dbl(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsDouble() : 0;
    }

    private static boolean bool(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() && root.get(key).getAsBoolean();
    }

    /**
     * Six hex characters ("B347CC") to 0xRRGGBB, or 0 when unusable.
     */
    private static int parseHexColor(String hex) {
        if (hex == null) return 0;
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        if (clean.length() != 6) return 0;
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- Getters ---

    public static String pathway() {
        return pathway;
    }

    public static String pathwayName() {
        return pathwayName.isEmpty() ? pathway : pathwayName;
    }

    /**
     * Opaque ARGB accent for this pathway — the server's colour when it sent
     * one, otherwise the mod's own pathway table.
     */
    public static int pathwayArgb() {
        int rgb = pathwayColor != 0 ? pathwayColor : AbilityInfo.pathwayRgb(pathway);
        return 0xFF000000 | rgb;
    }

    public static int sequence() {
        return sequence;
    }

    public static String sequenceName() {
        return sequenceName;
    }

    public static boolean outer() {
        return outer;
    }

    public static double health() {
        return health;
    }

    public static double maxHealth() {
        return maxHealth;
    }

    public static int spirituality() {
        return spirituality;
    }

    public static int maxSpirituality() {
        return maxSpirituality;
    }

    public static double madness() {
        return madness;
    }

    public static double permanentFloor() {
        return permanentFloor;
    }

    public static double godhoodFloor() {
        return godhoodFloor;
    }

    public static int madnessStage() {
        return madnessStage;
    }

    public static double tiredness() {
        return tiredness;
    }

    public static int tirednessStage() {
        return tirednessStage;
    }

    public static Acting acting() {
        return acting;
    }

    public static Gauge lifeAndDeath() {
        return lifeAndDeath;
    }

    public static Pressure pressure() {
        return pressure;
    }

    public static boolean anomaly() {
        return anomaly;
    }

    public static Actions actions() {
        return actions;
    }

    public static boolean hasData() {
        return hasData;
    }

    public static long receivedAt() {
        return receivedAt;
    }

    /**
     * Acting method cooldown in seconds, counted down from the last push so
     * the sheet reads like a timer instead of a value that steps every 3 s.
     */
    public static int actingCooldownNow() {
        if (acting == null || acting.cooldownRemaining() <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - receivedAt) / 1000L;
        return (int) Math.max(0, acting.cooldownRemaining() - elapsed);
    }

    /**
     * {@code mm:ss} for {@link #actingCooldownNow()}.
     */
    public static String actingCooldownClock() {
        int seconds = actingCooldownNow();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Debug-screen entry point: a fully populated fake sheet so the screen can
     * be checked with no server attached.
     */
    public static void debugInject() {
        handle("""
                {"pathway":"fool","pathwayName":"Fool","pathwayColor":"B347CC","sequence":7,\
                "sequenceName":"Magician","outer":false,\
                "health":80.0,"maxHealth":120.0,"spirituality":300,"maxSpirituality":500,\
                "madness":62.5,"permanentFloor":10.0,"godhoodFloor":5.0,"madnessStage":2,\
                "tiredness":48.0,"tirednessStage":1,\
                "acting":{"acting":1234,"needed":5000,"percent":24.68,"limited":false,\
                "cooldownRemaining":95,"cooldownTotal":1800,\
                "sources":[{"source":"ABILITY_GAMEPLAY","label":"Ability gameplay","contributed":600,"cap":2500,"unlimited":false},\
                {"source":"ROLEPLAY","label":"Roleplay","contributed":420,"cap":1000,"unlimited":false},\
                {"source":"EVENT","label":"Events","contributed":214,"cap":214,"unlimited":false},\
                {"source":"STAFF","label":"Staff grant","contributed":0,"cap":-1,"unlimited":true}],\
                "overflow":{"eligible":true,"banked":300,"ceiling":17500,"uncapped":false,"foreignThrottlePercent":25}},\
                "lifeAndDeath":{"present":true,"meter":0.42},\
                "pressure":{"present":true,"stacks":3,"cap":10},\
                "anomaly":true,\
                "actions":{"church":true,"abilities":true,"mythical":false,"uniqueness":true,\
                "honorific":false,"map":true,"seat":true,"terrainDamage":true}}""");
    }

    public static void reset() {
        pathway = "";
        pathwayName = "";
        pathwayColor = 0;
        sequence = -1;
        sequenceName = "";
        outer = false;
        health = 0;
        maxHealth = 0;
        spirituality = 0;
        maxSpirituality = 0;
        madness = 0;
        permanentFloor = 0;
        godhoodFloor = 0;
        madnessStage = 0;
        tiredness = 0;
        tirednessStage = 0;
        acting = null;
        lifeAndDeath = new Gauge(false, 0);
        pressure = new Pressure(false, 0, 0);
        anomaly = false;
        actions = Actions.NONE;
        hasData = false;
        receivedAt = 0;
    }
}
