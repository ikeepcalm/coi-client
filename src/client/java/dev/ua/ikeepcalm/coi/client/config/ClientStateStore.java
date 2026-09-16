package dev.ua.ikeepcalm.coi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small persistent scratch state that survives sessions — the last-known
 * madness values (so the title screen remembers how corrupted the player
 * was when they logged off) and whether the first-join tour has been shown.
 * State, not preference: it deliberately survives resets of coi_hud.json.
 */
public class ClientStateStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("coi_client_state.json");

    private static double lastPermanentMadness = 0.0;
    private static double lastMadness = 0.0;
    private static boolean tourCompleted = false;

    public static void load() {
        if (!Files.exists(STATE_PATH)) return;
        try {
            JsonObject json = GSON.fromJson(Files.readString(STATE_PATH), JsonObject.class);
            if (json == null) return;
            if (json.has("lastPermanentMadness")) {
                lastPermanentMadness = json.get("lastPermanentMadness").getAsDouble();
            }
            if (json.has("lastMadness")) {
                lastMadness = json.get("lastMadness").getAsDouble();
            }
            if (json.has("tourCompleted")) {
                tourCompleted = json.get("tourCompleted").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("COI Client: Failed to read client state");
            e.printStackTrace();
        }
    }

    /**
     * Persists only when the value actually changes — permanent madness
     * moves rarely, so disk writes stay rare and the value survives crashes.
     */
    public static void setPermanentMadness(double value) {
        if (Math.abs(value - lastPermanentMadness) < 0.001) return;
        lastPermanentMadness = value;
        save();
    }

    /**
     * Regular madness changes constantly, so it is persisted once at
     * disconnect (with whatever value the session ended on — including
     * values set through the debug screen) rather than on every update.
     */
    public static void setLastMadness(double value) {
        if (Math.abs(value - lastMadness) < 0.001) return;
        lastMadness = value;
        save();
    }

    public static double getLastPermanentMadness() {
        return lastPermanentMadness;
    }

    public static double getLastMadness() {
        return lastMadness;
    }

    /** How corrupted the player was when last seen — drives the title screen haunting. */
    public static double getCorruption() {
        return Math.max(lastMadness, lastPermanentMadness);
    }

    public static boolean isTourCompleted() {
        return tourCompleted;
    }

    public static void setTourCompleted(boolean value) {
        if (tourCompleted == value) return;
        tourCompleted = value;
        save();
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("lastPermanentMadness", lastPermanentMadness);
        json.addProperty("lastMadness", lastMadness);
        json.addProperty("tourCompleted", tourCompleted);
        try {
            Files.writeString(STATE_PATH, GSON.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
