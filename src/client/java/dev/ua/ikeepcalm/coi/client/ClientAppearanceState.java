package dev.ua.ikeepcalm.coi.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks passive appearance traits (e.g. Demoness female traits from Sequence 7) for
 * tracked players, fed by {@code coi-client:appearance} broadcasts. Unlike the momentary
 * Mythical Form transform, these are persistent — the last known trait set for a player is
 * kept until an explicit clear (empty payload) or a fresh non-empty update arrives, so a
 * missed periodic resync from the server doesn't flicker the trait off.
 */
public final class ClientAppearanceState {

    private static final Map<String, Set<String>> traitsByUuid = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> debugTraitsByUuid = new ConcurrentHashMap<>();

    private ClientAppearanceState() {
    }

    public static void handlePacket(String targetUuid, String traits) {
        if (targetUuid == null || targetUuid.isBlank()) {
            return;
        }
        if (traits == null || traits.isBlank()) {
            traitsByUuid.remove(targetUuid);
            return;
        }

        Set<String> parsed = new HashSet<>();
        for (String trait : traits.split(",")) {
            String normalized = trait.trim();
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }
        if (parsed.isEmpty()) {
            traitsByUuid.remove(targetUuid);
        } else {
            traitsByUuid.put(targetUuid, Set.copyOf(parsed));
        }
    }

    /**
     * The renderable trait set for a player — server-reported traits unioned with local
     * debug overrides, so the preview screen's toggles show up instantly. Never null.
     */
    public static Set<String> getTraits(String playerUuid) {
        if (playerUuid == null) return Set.of();
        Set<String> traits = traitsByUuid.get(playerUuid);
        Set<String> debugTraits = debugTraitsByUuid.get(playerUuid);
        if (traits == null) return debugTraits != null ? debugTraits : Set.of();
        if (debugTraits == null) return traits;
        Set<String> merged = new HashSet<>(traits);
        merged.addAll(debugTraits);
        return Set.copyOf(merged);
    }

    public static boolean hasTrait(String playerUuid, String traitId) {
        if (playerUuid == null) return false;
        Set<String> traits = traitsByUuid.get(playerUuid);
        if (traits != null && traits.contains(traitId)) {
            return true;
        }
        Set<String> debugTraits = debugTraitsByUuid.get(playerUuid);
        return debugTraits != null && debugTraits.contains(traitId);
    }

    public static boolean hasDebugTrait(String playerUuid, String traitId) {
        if (playerUuid == null) return false;
        Set<String> traits = debugTraitsByUuid.get(playerUuid);
        return traits != null && traits.contains(traitId);
    }

    /**
     * Toggles a local-only trait override and returns its new state. Debug traits are kept
     * separate from server state, so appearance resync packets cannot interrupt visual testing.
     */
    public static boolean toggleDebugTrait(String playerUuid, String traitId) {
        if (playerUuid == null || traitId == null || traitId.isBlank()) {
            return false;
        }

        Set<String> updated = new HashSet<>(debugTraitsByUuid.getOrDefault(playerUuid, Set.of()));
        boolean enabled;
        if (updated.remove(traitId)) {
            enabled = false;
        } else {
            updated.add(traitId);
            enabled = true;
        }

        if (updated.isEmpty()) {
            debugTraitsByUuid.remove(playerUuid);
        } else {
            debugTraitsByUuid.put(playerUuid, Set.copyOf(updated));
        }
        return enabled;
    }

    public static void clearDebugTraits(String playerUuid) {
        if (playerUuid != null) {
            debugTraitsByUuid.remove(playerUuid);
        }
    }

    public static void reset() {
        traitsByUuid.clear();
        debugTraitsByUuid.clear();
    }

}
