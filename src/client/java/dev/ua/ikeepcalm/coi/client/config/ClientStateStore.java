package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Small persistent scratch state that survives sessions — the last-known
 * madness values (so the title screen remembers how corrupted the player
 * was when they logged off), the pathway it lights on its wheel, whether the
 * first-join tour has been shown, and whether the inventory hint has been
 * dismissed.
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
    private static boolean inventoryHintDismissed = false;
    private static String lastPathway = "";

    public static void load() {
        if (!Files.exists(STATE_PATH)) return;
        try {
            JsonObject json = GSON.fromJson(Files.readString(STATE_PATH), JsonObject.class);
            if (json == null) return;
            lastPermanentMadness = JsonRead.dbl(json, "lastPermanentMadness", lastPermanentMadness);
            lastMadness = JsonRead.dbl(json, "lastMadness", lastMadness);
            tourCompleted = JsonRead.bool(json, "tourCompleted");
            inventoryHintDismissed = JsonRead.bool(json, "inventoryHintDismissed");
            lastPathway = JsonRead.string(json, "lastPathway", lastPathway);
        } catch (Exception e) {
            CoiLog.LOG.warn("Failed to read client state", e);
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

    public static boolean isTourNotCompleted() {
        return !tourCompleted;
    }

    public static void setTourCompleted(boolean value) {
        if (tourCompleted == value) return;
        tourCompleted = value;
        save();
    }

    /**
     * Whether the player has already been told the slot-9 shortcut item is
     * gone. Answered once and then never again — which is why it lives here
     * rather than in coi_hud.json, where a reset would bring the hint back.
     */
    public static boolean isInventoryHintDismissed() {
        return inventoryHintDismissed;
    }

    public static void setInventoryHintDismissed(boolean value) {
        if (inventoryHintDismissed == value) return;
        inventoryHintDismissed = value;
        save();
    }

    /**
     * The pathway the player was walking when last seen, so the title screen
     * can light their own emblem on the wheel before any server has spoken.
     * Empty for a fresh install, which is the correct "nothing lit" state.
     */
    public static String getLastPathway() {
        return lastPathway;
    }

    public static void setLastPathway(String value) {
        String next = value == null ? "" : value;
        if (lastPathway.equals(next)) return;
        lastPathway = next;
        save();
    }

    /**
     * Rewrites the whole file. Every setter above calls this only when its
     * value actually changed, so a session that touches nothing never writes.
     */
    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("lastPermanentMadness", lastPermanentMadness);
        json.addProperty("lastMadness", lastMadness);
        json.addProperty("tourCompleted", tourCompleted);
        json.addProperty("inventoryHintDismissed", inventoryHintDismissed);
        json.addProperty("lastPathway", lastPathway);
        try {
            Files.writeString(STATE_PATH, GSON.toJson(json));
        } catch (IOException e) {
            CoiLog.LOG.warn("Failed to write client state", e);
        }
    }
}
