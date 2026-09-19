package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Which ability is bound to which slot, in {@code config/coi_abilities.json}.
 * <p>
 * All three binding sets share one file and one flat key space —
 * {@code ability1..N}, {@code wheel1..N} and {@code gesture_<id>} — so a save
 * always writes all three together and a load reads whichever prefix it was
 * asked for. An absent key is an unbound slot, never an error: the file is
 * written by whatever the player last bound, and a set that has never been
 * touched simply is not in it.
 */
public class AbilityConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_abilities.json");

    private AbilityConfig() {
    }

    /**
     * Writes all three sets at once — the file has no notion of a partial
     * save, so every caller hands over everything it holds.
     */
    public static void saveBindings(String[] abilities, String[] wheelAbilities, String[] gestureAbilities) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < abilities.length; i++) {
            json.addProperty(abilityKey(i), abilities[i]);
        }
        for (int i = 0; i < wheelAbilities.length; i++) {
            json.addProperty(wheelKey(i), wheelAbilities[i]);
        }
        for (int i = 0; i < gestureAbilities.length; i++) {
            json.addProperty(gestureKey(i), gestureAbilities[i]);
        }

        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write ability bindings", e);
        }
    }

    public static String[] loadBindings() {
        return load(AbilityBindings.MAX_ABILITIES, AbilityConfig::abilityKey, "key-slot");
    }

    public static String[] loadWheelBindings() {
        return load(AbilityBindings.MAX_WHEEL_SIZE, AbilityConfig::wheelKey, "wheel");
    }

    public static String[] loadGestureBindings() {
        return load(GestureType.values().length, AbilityConfig::gestureKey, "gesture");
    }

    /**
     * Reads {@code size} slots keyed by {@code keyFor}. A missing file, an
     * unreadable one or a missing key all mean the same thing — that slot is
     * unbound — so the array comes back full of nulls rather than short.
     *
     * @param what names the set in the warning, so a failed read says which
     */
    private static String[] load(int size, IntFunction<String> keyFor, String what) {
        String[] bound = new String[size];
        if (!Files.exists(CONFIG_PATH)) return bound;
        try {
            JsonObject json = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            for (int i = 0; i < size; i++) {
                String key = keyFor.apply(i);
                bound[i] = json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
            }
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to read {} bindings", what, e);
        }
        return bound;
    }

    private static String abilityKey(int slot) {
        return "ability" + (slot + 1);
    }

    private static String wheelKey(int slot) {
        return "wheel" + (slot + 1);
    }

    /**
     * Gestures are keyed by name, not index: the shapes are an enum, and
     * inserting one would otherwise re-point every binding after it.
     */
    private static String gestureKey(int slot) {
        return "gesture_" + GestureType.values()[slot].id();
    }
}
