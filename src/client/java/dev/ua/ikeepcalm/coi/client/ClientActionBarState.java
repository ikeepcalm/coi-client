package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.List;

/**
 * The COI action bar, moved off the vanilla one.
 * <p>
 * The server sends its whole sorted entry list on {@code coi-client:actionbar}
 * instead of composing a single vanilla action-bar line; each payload replaces
 * the list wholesale, and entries then expire locally at
 * {@code receivedAt + ttlMs} so the HUD keeps draining between pushes.
 */
public final class ClientActionBarState {

    /**
     * One channel's message. {@code expiresAtMs} is already absolute.
     */
    public record Entry(String channel, String key, int priority, long expiresAtMs, Component text) {
    }

    private static final List<Entry> entries = new ArrayList<>();
    private static int maxVisible = 2;

    private ClientActionBarState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            long now = System.currentTimeMillis();
            List<Entry> parsed = new ArrayList<>();
            if (root.has("entries") && root.get("entries").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("entries")) {
                    Entry entry = parseEntry(element.getAsJsonObject(), now);
                    if (entry != null) parsed.add(entry);
                }
            }
            synchronized (entries) {
                entries.clear();
                entries.addAll(parsed);
            }
            maxVisible = root.has("maxVisible") ? Math.max(1, root.get("maxVisible").getAsInt()) : 2;
        } catch (Exception e) {
            System.err.println("COI Client: malformed action bar payload: " + json);
        }
    }

    private static Entry parseEntry(JsonObject entry, long now) {
        Component text = deserialize(entry.get("text"));
        if (text == null) return null;
        long ttl = entry.has("ttlMs") ? entry.get("ttlMs").getAsLong() : 2000L;
        return new Entry(
                entry.has("channel") ? entry.get("channel").getAsString() : "",
                entry.has("key") ? entry.get("key").getAsString() : "",
                entry.has("priority") ? entry.get("priority").getAsInt() : 0,
                now + Math.max(0L, ttl),
                text);
    }

    /**
     * Vanilla text-component JSON → {@link Component}, through the
     * registry-free {@link ComponentSerialization#CODEC}. Adventure's Gson
     * serializer (what the plugin uses) writes the same shape. Anything the
     * codec rejects degrades to its plain string form rather than dropping
     * the line.
     */
    private static Component deserialize(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .result()
                .orElseGet(() -> element.isJsonPrimitive() ? Component.literal(element.getAsString()) : null);
    }

    /**
     * Unexpired entries, newest-server-order preserved, truncated to
     * {@code limit} (the smaller of the server's {@code maxVisible} and the
     * player's own line cap).
     */
    public static List<Entry> visibleEntries(long now, int limit) {
        List<Entry> visible = new ArrayList<>();
        synchronized (entries) {
            for (Entry entry : entries) {
                if (entry.expiresAtMs() > now) visible.add(entry);
                if (visible.size() >= limit) break;
            }
        }
        return visible;
    }

    public static int getMaxVisible() {
        return maxVisible;
    }

    /**
     * Debug-screen entry point: two fake lines that expire in {@code ttlMs}.
     */
    public static void debugInject(long ttlMs) {
        long expiry = System.currentTimeMillis() + ttlMs;
        synchronized (entries) {
            entries.clear();
            entries.add(new Entry("COOLDOWN", "artifact:sword", 60, expiry,
                    Component.literal("Sword of Judgement ready in 3s")));
            entries.add(new Entry("RESERVE", "sun:light", 40, expiry,
                    Component.literal("Holy Light reserve 42/60")));
        }
        maxVisible = 2;
    }

    public static void reset() {
        synchronized (entries) {
            entries.clear();
        }
        maxVisible = 2;
    }
}
