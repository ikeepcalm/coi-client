package dev.ua.ikeepcalm.coi.client.presence;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityButton;
import de.jcm.discordgamesdk.activity.ActivityButtonsMode;
import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Connects lazily on the first server join, silently no-ops when Discord
 * isn't running (retrying at most once a minute), and re-sends the activity
 * only when its content actually changes — never more often than once per
 * 15s (Discord throttles at ~15s anyway). Everything runs on the client
 * tick thread; connecting to a local pipe is instant or fails instantly.
 * <p>
 * Shown state is all client-known: server name, dominant pathway derived
 * from bound ability id prefixes, and a madness-stage flavor word. Large
 * image keys are the pathway names ("fool", "door", …) with "logo" as the
 * fallback
 */
public final class DiscordPresenceManager {

    private static final long APP_ID = 1525950133848506419L;

    private static final long RECONNECT_INTERVAL_MS = 60_000;
    private static final long MIN_UPDATE_INTERVAL_MS = 15_000;
    private static final String FALLBACK_SERVER_NAME = "Mysterria";
    private static final String WEBSITE_URL = "https://mysterria.net";

    private static final Set<String> PATHWAY_ASSET_KEYS = Set.of(
            "abyss", "eternalaeon", "chained", "darkness", "death",
            "demoness", "door", "emperor", "error", "fool", "fortune",
            "giant", "hanged", "hermit", "justiciar", "moon", "mother",
            "paragon", "patriarch", "priest", "sublunary", "sun", "tower",
            "tyrant", "visionary"
    );

    private static Core core;
    private static boolean everJoined = false;
    private static boolean inWorld = false;
    private static String serverName = FALLBACK_SERVER_NAME;
    private static String serverAddress;
    private static Instant sessionStart;
    private static long lastConnectAttempt = 0;
    private static long lastUpdateAt = 0;
    private static String lastSignature = null;

    private DiscordPresenceManager() {
    }

    public static void onServerJoin(String name, String address) {
        everJoined = true;
        inWorld = true;
        serverName = (name == null || name.isBlank()) ? FALLBACK_SERVER_NAME : name;
        serverAddress = (address == null || address.isBlank()) ? null : address;
        if (sessionStart == null) {
            sessionStart = Instant.now();
        }
        lastSignature = null;
    }

    public static void onDisconnect() {
        inWorld = false;
        lastSignature = null;
    }

    /** Called every client tick (menus included). */
    public static void tick() {
        if (APP_ID == 0) return;

        if (!HudConfig.getSettings().enableDiscordPresence) {
            shutdown();
            return;
        }
        if (!everJoined) return; // lazy: don't touch Discord until the mod matters

        long now = System.currentTimeMillis();
        if (core == null) {
            if (now - lastConnectAttempt < RECONNECT_INTERVAL_MS) return;
            lastConnectAttempt = now;
            connect();
            if (core == null) return;
        }

        try {
            core.runCallbacks();
        } catch (Throwable t) {
            // Discord quit or the pipe broke — drop the core, retry in 60s
            shutdown();
            return;
        }

        String pathway = inWorld ? dominantPathway() : null;
        String details = inWorld ? serverDetails() : "In the menus";
        String state = inWorld ? buildStateLine(pathway) : "Circle of Imagination";
        String imageKey = pathway != null ? pathway : "logo";
        String imageText = pathway != null ? pathwayLabel(pathway) : "Circle of Imagination";

        String signature = details + "\n" + state + "\n" + imageKey;
        if (signature.equals(lastSignature) || now - lastUpdateAt < MIN_UPDATE_INTERVAL_MS) {
            return;
        }

        try {
            // Not try-with-resources: close() is deprecated in the pure-Java SDK (nothing to free)
            Activity activity = new Activity();
            activity.setDetails(details);
            activity.setState(state);
            if (sessionStart != null) {
                activity.timestamps().setStart(sessionStart);
            }
            activity.assets().setLargeImage(imageKey);
            activity.assets().setLargeText(imageText);
            activity.setActivityButtonsMode(ActivityButtonsMode.BUTTONS);
            activity.addButton(new ActivityButton("Visit Website", WEBSITE_URL));
            core.activityManager().updateActivity(activity);
            lastSignature = signature;
            lastUpdateAt = now;
        } catch (Throwable t) {
            shutdown();
        }
    }

    private static void connect() {
        try {
            CreateParams params = new CreateParams();
            params.setClientID(APP_ID);
            params.setFlags(CreateParams.getDefaultFlags());
            core = new Core(params);
            lastSignature = null;
        } catch (Throwable t) {
            // Discord isn't running (or refused us) — stay silent, retry in 60s
            core = null;
        }
    }

    public static void shutdown() {
        if (core != null) {
            try {
                core.close();
            } catch (Throwable ignored) {
            }
            core = null;
        }
    }

    private static String buildStateLine(String pathway) {
        if (pathway == null) {
            return "Ordinary Human";
        }
        String label = pathwayLabel(pathway);
        if (inWorld && HudConfig.getSettings().presenceShowMadness) {
            return label + " — " + madnessFlavor(ClientBeyonderState.getMadness());
        }
        return label;
    }

    private static String serverDetails() {
        return serverAddress == null ? serverName : serverName + " (" + serverAddress + ")";
    }

    private static String pathwayLabel(String pathway) {
        return Character.toUpperCase(pathway.charAt(0)) + pathway.substring(1) + " Pathway";
    }

    private static String madnessFlavor(double madness) {
        if (madness < 25) return "Sane";
        if (madness < 50) return "Uneasy";
        if (madness < 75) return "Slipping";
        if (madness < 100) return "Fraying";
        return "Rampaging";
    }

    /**
     * Most frequent pathway prefix (first id segment before '-', same rule
     * as {@link AbilityInfo#pathwayColor}) across key-bound and wheel-bound
     * abilities, or null when nothing is bound.
     */
    private static String dominantPathway() {
        Map<String, Integer> counts = new HashMap<>();
        countPathways(CircleOfImaginationClient.getBoundAbilitiesSnapshot(), counts);
        countPathways(CircleOfImaginationClient.getWheelAbilitiesSnapshot(), counts);

        // A newly awakened Beyonder may have learned abilities without having
        // opened the binding screen yet. Prefer their bindings when present,
        // but fall back to all server-provided abilities instead of calling
        // them an Ordinary Human.
        if (counts.isEmpty()) {
            countPathways(CircleOfImaginationClient.getAvailableAbilities().toArray(String[]::new), counts);
        }

        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static void countPathways(String[] stored, Map<String, Integer> counts) {
        for (String entry : stored) {
            String id = AbilityInfo.extractId(entry);
            if (id == null || id.isEmpty()) continue;
            String pathway = id.split("-")[0].toLowerCase();
            if (!PATHWAY_ASSET_KEYS.contains(pathway)) continue;
            counts.merge(pathway, 1, Integer::sum);
        }
    }
}
