package dev.ua.ikeepcalm.coi.client.presence;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityButton;
import de.jcm.discordgamesdk.activity.ActivityButtonsMode;
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
public class DiscordPresenceManager {

    private static final long APP_ID = 1525950133848506419L;

    private static final long RECONNECT_INTERVAL_MS = 60_000;
    private static final long MIN_UPDATE_INTERVAL_MS = 15_000;
    private static final String FALLBACK_SERVER_NAME = "Mysterria";
    private static final String WEBSITE_URL = "https://mysterria.net";

    /**
     * What is shown outside a world, and the art key for a player whose
     * pathway is still unknown.
     */
    private static final String MOD_NAME = "Circle of Imagination";
    private static final String IDLE_DETAILS = "In the menus";
    private static final String FALLBACK_IMAGE_KEY = "logo";

    private static final Set<String> PATHWAY_ASSET_KEYS = Set.of(
            "abyss", "aeon", "eternalaeon", "chained", "darkness", "death",
            "demoness", "door", "emperor", "error", "fool", "fortune",
            "giant", "hanged", "hermit", "justiciar", "moon", "mother",
            "paragon", "patriarch", "priest", "sublunary", "sun", "tower",
            "tyrant", "visionary"
    );

    /**
     * Pathway names the Discord app art is keyed under differently from the
     * name the plugin uses.
     */
    private static final Map<String, String> ASSET_ALIASES = Map.of("aeon", "eternalaeon");

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
        if (!pumpCallbacks(now)) return;

        String pathway = inWorld ? dominantPathway() : null;
        String details = inWorld ? serverDetails() : IDLE_DETAILS;
        String state = inWorld ? buildStateLine(pathway) : MOD_NAME;
        String imageKey = pathway != null ? assetKey(pathway) : FALLBACK_IMAGE_KEY;
        String imageText = pathway != null ? pathwayLabel(pathway) : MOD_NAME;

        // What Discord would actually see: re-sending an identical activity
        // just burns the rate limit
        String signature = details + "\n" + state + "\n" + imageKey;
        if (signature.equals(lastSignature) || now - lastUpdateAt < MIN_UPDATE_INTERVAL_MS) {
            return;
        }

        if (updateActivity(details, state, imageKey, imageText)) {
            lastSignature = signature;
            lastUpdateAt = now;
        }
    }

    /**
     * Connects if needed and services the SDK.
     *
     * @return false when there is nothing to publish to this tick — Discord
     * is not running, or the pipe just broke
     */
    private static boolean pumpCallbacks(long now) {
        if (core == null) {
            if (now - lastConnectAttempt < RECONNECT_INTERVAL_MS) return false;
            lastConnectAttempt = now;
            connect();
            if (core == null) return false;
        }
        try {
            core.runCallbacks();
            return true;
        } catch (Throwable t) {
            // Discord quit or the pipe broke — drop the core, retry in 60s
            shutdown();
            return false;
        }
    }

    /**
     * @return whether the activity actually reached Discord; a failure drops
     * the core so the next tick reconnects instead of republishing into it
     */
    private static boolean updateActivity(String details, String state, String imageKey, String imageText) {
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
            return true;
        } catch (Throwable t) {
            shutdown();
            return false;
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

    private static String assetKey(String pathway) {
        return ASSET_ALIASES.getOrDefault(pathway, pathway);
    }

    /**
     * The second presence line: who the player is, and — if they left the
     * setting on — how badly they are coping.
     */
    private static String buildStateLine(String pathway) {
        if (pathway == null) {
            return "Ordinary Human";
        }
        String label = pathwayLabel(pathway);
        // The server knows the exact sequence; the binding heuristic never did
        int sequence = BeyonderState.getSequence();
        if (sequence >= 0) {
            label = "Seq " + sequence + " · " + label;
        }
        if (inWorld && HudConfig.getSettings().presenceShowMadness) {
            return label + " — " + madnessFlavor(BeyonderState.getMadness());
        }
        return label;
    }

    private static String serverDetails() {
        return serverAddress == null ? serverName : serverName + " (" + serverAddress + ")";
    }

    private static String pathwayLabel(String pathway) {
        return Character.toUpperCase(pathway.charAt(0)) + pathway.substring(1) + " Pathway";
    }

    /**
     * One word per madness stage — the same 25 / 50 / 75 / 100 brackets the
     * HUD colours the madness bar by, so the two never disagree about which
     * stage the player is in.
     */
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
        // A protocol-2 server names the pathway outright; guessing is the fallback
        if (BeyonderState.hasIdentity()) {
            return BeyonderState.getPathway();
        }
        Map<String, Integer> counts = new HashMap<>();
        countPathways(AbilityBindings.getBoundAbilitiesSnapshot(), counts);
        countPathways(AbilityBindings.getWheelAbilitiesSnapshot(), counts);

        // A newly awakened Beyonder may have learned abilities without having
        // opened the binding screen yet. Prefer their bindings when present,
        // but fall back to all server-provided abilities instead of calling
        // them an Ordinary Human.
        if (counts.isEmpty()) {
            countPathways(AbilityRegistry.getAvailableAbilities().toArray(String[]::new), counts);
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
