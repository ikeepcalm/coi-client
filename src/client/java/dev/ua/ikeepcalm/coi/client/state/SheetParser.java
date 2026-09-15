package dev.ua.ikeepcalm.coi.client.state;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code coi-client:sheet} JSON to a {@link SheetState.Snapshot}.
 * <p>
 * Every section is optional: a server that omits {@code acting} means "this
 * player has no acting progression", not "something went wrong", so each
 * sub-reader answers a missing object with the record's own empty value. Only
 * a body that is not usable at all returns null, which leaves the previous
 * sheet on screen rather than blanking it.
 */
final class SheetParser {

    /**
     * The stage ceiling both madness and tiredness are reported against.
     */
    private static final int MAX_STAGE = 4;

    private SheetParser() {
    }

    /**
     * @return the snapshot, or null when the body is unusable — the signal to
     * keep whatever was on screen
     */
    static SheetState.Snapshot parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return new SheetState.Snapshot(
                    identity(root),
                    vitals(root),
                    mind(root),
                    acting(JsonRead.object(root, "acting")),
                    gauge(JsonRead.object(root, "lifeAndDeath")),
                    pressure(JsonRead.object(root, "pressure")),
                    JsonRead.bool(root, "anomaly"),
                    actions(JsonRead.object(root, "actions")),
                    true,
                    System.currentTimeMillis());
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed sheet payload: {}", json);
            return null;
        }
    }

    private static SheetState.Identity identity(JsonObject root) {
        return new SheetState.Identity(
                Pathways.normalizePathway(JsonRead.string(root, "pathway")),
                JsonRead.string(root, "pathwayName"),
                hexColor(JsonRead.string(root, "pathwayColor")),
                JsonRead.intOf(root, "sequence", -1),
                JsonRead.string(root, "sequenceName"),
                JsonRead.bool(root, "outer"));
    }

    private static SheetState.Vitals vitals(JsonObject root) {
        return new SheetState.Vitals(
                JsonRead.dbl(root, "health"),
                JsonRead.dbl(root, "maxHealth"),
                JsonRead.intOf(root, "spirituality"),
                JsonRead.intOf(root, "maxSpirituality"));
    }

    private static SheetState.Mind mind(JsonObject root) {
        return new SheetState.Mind(
                JsonRead.dbl(root, "madness"),
                JsonRead.dbl(root, "permanentFloor"),
                JsonRead.dbl(root, "godhoodFloor"),
                Math.clamp(JsonRead.intOf(root, "madnessStage"), 0, MAX_STAGE),
                JsonRead.dbl(root, "tiredness"),
                Math.clamp(JsonRead.intOf(root, "tirednessStage"), 0, MAX_STAGE));
    }

    /**
     * @return null for an absent section, which is how an outer pathway says it
     * has no acting progression at all
     */
    private static SheetState.Acting acting(JsonObject node) {
        if (node == null) return null;
        int current = JsonRead.intOf(node, "acting");
        int needed = JsonRead.intOf(node, "needed");
        double percent = JsonRead.dbl(node, "percent", needed > 0 ? current * 100.0 / needed : 0);
        return new SheetState.Acting(current, needed, percent, JsonRead.bool(node, "limited"),
                JsonRead.intOf(node, "cooldownRemaining"), JsonRead.intOf(node, "cooldownTotal"),
                sources(node.get("sources")), overflow(JsonRead.object(node, "overflow")));
    }

    private static List<SheetState.Source> sources(JsonElement element) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<SheetState.Source> sources = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) continue;
            JsonObject row = entry.getAsJsonObject();
            sources.add(new SheetState.Source(JsonRead.string(row, "source"), JsonRead.string(row, "label"),
                    JsonRead.intOf(row, "contributed"), JsonRead.intOf(row, "cap"),
                    JsonRead.bool(row, "unlimited")));
        }
        return List.copyOf(sources);
    }

    private static SheetState.Overflow overflow(JsonObject node) {
        if (node == null) return new SheetState.Overflow(false, 0, 0, false, 0);
        return new SheetState.Overflow(JsonRead.bool(node, "eligible"), JsonRead.intOf(node, "banked"),
                JsonRead.intOf(node, "ceiling"), JsonRead.bool(node, "uncapped"),
                JsonRead.intOf(node, "foreignThrottlePercent"));
    }

    private static SheetState.Gauge gauge(JsonObject node) {
        if (node == null) return SheetState.Gauge.ABSENT;
        return new SheetState.Gauge(JsonRead.bool(node, "present"), JsonRead.dbl(node, "meter"));
    }

    private static SheetState.Pressure pressure(JsonObject node) {
        if (node == null) return SheetState.Pressure.ABSENT;
        return new SheetState.Pressure(JsonRead.bool(node, "present"), JsonRead.intOf(node, "stacks"),
                JsonRead.intOf(node, "cap"));
    }

    private static SheetState.Actions actions(JsonObject node) {
        if (node == null) return SheetState.Actions.NONE;
        return new SheetState.Actions(JsonRead.bool(node, "church"), JsonRead.bool(node, "abilities"),
                JsonRead.bool(node, "mythical"), JsonRead.bool(node, "uniqueness"),
                JsonRead.bool(node, "honorific"), JsonRead.bool(node, "map"),
                JsonRead.bool(node, "seat"), JsonRead.bool(node, "terrainDamage"));
    }

    /**
     * Six hex characters ("B347CC") to 0xRRGGBB, or 0 when unusable.
     */
    private static int hexColor(String hex) {
        if (hex == null) return 0;
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        if (clean.length() != 6) return 0;
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * A fully populated sheet for the F8 debug screen. It goes through
     * {@link #parse} rather than building records directly, so the offline
     * check covers this parser too.
     */
    static final String SAMPLE = """
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
            "honorific":false,"map":true,"seat":true,"terrainDamage":true}}""";
}
