package dev.ua.ikeepcalm.coi.client.ability;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.CoiClientMod;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;
import dev.ua.ikeepcalm.coi.client.json.JsonRead;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The server-provided ability catalogue: what this player can cast, and
 * everything the server said about each entry.
 * <p>
 * Both list formats land here — the protocol-1 delimited string and the
 * protocol-2 JSON — and both end up in the same two collections, so the rest
 * of the client never has to know which one the server speaks.
 */
public class AbilityRegistry {

    private AbilityRegistry() {
    }

    /**
     * The protocol-1 list format: entries separated by {@code ;}, fields
     * within an entry by {@code |} (a regex, hence the escape).
     */
    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = "\\|";

    /**
     * What an ability that names no category falls back to; the picker
     * translates it like any other.
     */
    private static final String DEFAULT_CATEGORY = "uncategorized";

    private static final List<String> availableAbilities = new ArrayList<>();
    private static final Map<String, AbilityInfo> abilityInfoMap = new HashMap<>();

    public static void reset() {
        availableAbilities.clear();
        abilityInfoMap.clear();
        AbilityCategories.clear();
        AbilityBindings.updateHudWithCurrentBindings();
        AbilityOverlay.clearServerState();
    }

    public static void handleAbilityData(String data) {
        availableAbilities.clear();
        abilityInfoMap.clear();
        AbilityCategories.clear();

        if (data.isEmpty()) {
            CoiLog.LOG.info("Received empty ability data");
            return;
        }

        CoiLog.LOG.info("Received ability data: {}", data);

        abilityInfoMap.clear();
        for (String entry : data.split(ENTRY_SEPARATOR)) {
            AbilityInfo info = parseDelimited(entry);
            if (info == null) continue;
            storeAbility(info);
            CoiLog.LOG.info("Added ability: {}", info.abilityId());
        }

        CoiLog.LOG.info("Total abilities loaded: {}", availableAbilities.size());
        AbilityBindings.updateHudWithCurrentBindings();
        CoiClientMod.scheduleTourIfFirstList();
    }

    /**
     * One {@code id|localizedName|englishName|category|hasLeftClick} entry.
     * Only the first two fields are required; the rest of the format grew over
     * time and an older server simply stops early.
     *
     * @return null for an entry too short to name an ability
     */
    private static AbilityInfo parseDelimited(String entry) {
        if (entry.isEmpty()) return null;
        String[] parts = entry.split(FIELD_SEPARATOR);
        if (parts.length < 2) return null;
        String localizedName = parts[1];
        return AbilityInfo.of(parts[0], localizedName,
                parts.length > 2 ? parts[2] : localizedName,
                parts.length > 3 ? parts[3] : DEFAULT_CATEGORY,
                parts.length >= 5 && Boolean.parseBoolean(parts[4]));
    }

    /**
     * Protocol-2 ability list: same two collections as {@link #handleAbilityData},
     * plus the metadata the delimited format had no room for. Cooldowns and
     * toggle state are applied after the slots have been rebound, so a slot
     * that was already showing this ability picks them up too.
     */
    public static void handleAbilityDataV2(String json) {
        availableAbilities.clear();
        abilityInfoMap.clear();
        AbilityCategories.clear();

        if (json == null || json.isBlank()) {
            CoiLog.LOG.info("Received empty ability data (v2)");
            return;
        }

        Map<String, Integer> cooldowns = new HashMap<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("abilities")) return;
            for (JsonElement element : root.getAsJsonArray("abilities")) {
                JsonObject entry = element.getAsJsonObject();
                AbilityInfo info = parseAbility(entry);
                if (info == null) continue;
                AbilityCategories.read(info.abilityId(), entry);
                storeAbility(info);
                int remaining = JsonRead.intOf(entry, "cooldownRemainingTicks");
                if (remaining > 0) cooldowns.put(info.abilityId(), remaining);
            }
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed abilities_v2 payload", e);
            return;
        }

        CoiLog.LOG.info("Total abilities loaded (v2): {}", availableAbilities.size());
        AbilityBindings.updateHudWithCurrentBindings();
        applyServerAbilityState(cooldowns);
        CoiClientMod.scheduleTourIfFirstList();
    }

    /**
     * One entry of the v2 list. Pathway and sequence fall back to what the id
     * itself says, so a server that omits them is no worse off than a
     * protocol-1 one.
     *
     * @return null for an entry with no id, which is the one field nothing can
     * stand in for
     */
    private static AbilityInfo parseAbility(JsonObject json) {
        String id = JsonRead.string(json, "id");
        if (id.isEmpty()) return null;
        String localizedName = JsonRead.string(json, "name", id);
        return new AbilityInfo(id, localizedName,
                JsonRead.string(json, "englishName", localizedName),
                JsonRead.string(json, "category", DEFAULT_CATEGORY),
                JsonRead.bool(json, "hasLeftClick"),
                JsonRead.string(json, "kind", AbilityInfo.KIND_ACTIVE),
                JsonRead.string(json, "description"),
                JsonRead.intOf(json, "cost"),
                JsonRead.dbl(json, "drainPerSecond"),
                JsonRead.intOf(json, "cooldownSeconds"),
                Pathways.normalizePathway(JsonRead.string(json, "pathway", AbilityInfo.pathwayOf(id))),
                JsonRead.intOf(json, "sequence", AbilityInfo.sequenceOf(id)),
                JsonRead.bool(json, "active"), JsonRead.bool(json, "locked"), JsonRead.bool(json, "blocked"),
                JsonRead.string(json, "blockedBy"), JsonRead.string(json, "icon"));
    }

    private static void storeAbility(AbilityInfo info) {
        if (info == null) return;
        availableAbilities.add(AbilityInfo.formatStored(info.abilityId(), info.englishName(), AbilityInfo.ACTION_EXECUTE));
        if (info.hasLeftClick()) {
            availableAbilities.add(AbilityInfo.formatStored(info.abilityId(),
                    info.englishName() + " (Left Click)", AbilityInfo.ACTION_LEFT_CLICK));
        }
        abilityInfoMap.put(info.abilityId(), info);
        for (var category : AbilityCategories.get(info.abilityId())) {
            availableAbilities.add(AbilityInfo.formatCategory(info.abilityId(),
                    info.englishName() + " · " + category.name(), category.id()));
        }
    }

    /**
     * Pushes toggle state and the cooldowns that were already running when the
     * list arrived onto the slots.
     */
    private static void applyServerAbilityState(Map<String, Integer> cooldowns) {
        for (AbilityInfo info : abilityInfoMap.values()) {
            AbilityOverlay.setActive(info.abilityId(), info.active());
            if (!AbilityCategories.get(info.abilityId()).isEmpty()) {
                AbilityOverlay.applyCategories(info.abilityId());
                continue;
            }
            Integer remaining = cooldowns.get(info.abilityId());
            if (remaining != null) {
                AbilityOverlay.setCooldown(info.abilityId(), remaining,
                        Math.max(remaining, info.cooldownSeconds() * 20));
            }
        }
    }

    /**
     * Single-ability update from {@code coi-client:state}: either a toggle or
     * a category switch.
     */
    public static void handleAbilityState(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String id = JsonRead.string(root, "id");
            if (id.isEmpty()) return;
            AbilityCategories.read(id, root);
            AbilityInfo info = abilityInfoMap.get(id);
            if (info != null && root.has("selectedCost")) abilityInfoMap.put(id, info.withCastingStats(
                    JsonRead.intOf(root, "selectedCost"), JsonRead.dbl(root, "selectedDrainPerSecond"),
                    JsonRead.intOf(root, "selectedCooldownSeconds")));
            if (root.has("castCategories") && abilityInfoMap.containsKey(id)) {
                availableAbilities.removeIf(option -> id.equals(AbilityInfo.extractId(option)));
                storeAbility(abilityInfoMap.get(id));
            }
            if (root.has("castCategories") || root.has("category")) AbilityOverlay.applyCategories(id);
            if (root.has("active")) {
                boolean active = root.get("active").getAsBoolean();
                abilityInfoMap.computeIfPresent(id, (key, current) -> current.withActive(active));
                AbilityOverlay.setActive(id, active);
            }
            if (root.has("category") || root.has("categoryName")) {
                AbilityOverlay.setCategoryLabel(id,
                        JsonRead.string(root, "categoryName", JsonRead.string(root, "category")));
            }
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed ability state payload: {}", json);
        }
    }

    public static void handleCooldownData(String abilityId, int cooldownTicks) {
        if (!AbilityCategories.get(abilityId).isEmpty()) return;
        AbilityOverlay.setCooldown(abilityId, cooldownTicks);
    }

    public static List<String> getAvailableAbilities() {
        return new ArrayList<>(availableAbilities);
    }

    /**
     * The live catalogue, for the binding rebind pass next door — the public
     * accessor hands out a copy, which is the wrong thing to walk once per slot.
     */
    static List<String> entries() {
        return availableAbilities;
    }

    /**
     * True once the server has sent at least one ability.
     */
    public static boolean hasAbilities() {
        return !availableAbilities.isEmpty();
    }

    public static AbilityInfo getAbilityInfo(String abilityId) {
        return abilityInfoMap.get(abilityId);
    }

    public static boolean hasLeftClick(String abilityIdWithName) {
        AbilityInfo info = getAbilityInfo(AbilityInfo.extractId(abilityIdWithName));
        return info != null && info.hasLeftClick();
    }

    /**
     * Four fake abilities for the binding screen, so it can be opened and
     * judged with no server attached. A no-op the moment a real list exists.
     */
    public static void addTestAbilities() {
        if (availableAbilities.isEmpty()) {
            CoiLog.LOG.info("Adding test abilities for debugging");
            availableAbilities.add("fireball - Fireball");
            availableAbilities.add("heal - Healing Light");
            availableAbilities.add("teleport - Teleportation");
            availableAbilities.add("shield - Magic Shield");
            CoiLog.LOG.info("Added {} test abilities", availableAbilities.size());
        }
    }
}
