package dev.ua.ikeepcalm.coi.client.config;

import java.util.Locale;

/**
 * In-memory metadata for one ability.
 * <p>
 * Protocol 1 servers only fill the first five components (see
 * {@link #of(String, String, String, String, boolean)}); protocol 2 servers
 * send the whole record on {@code coi-client:abilities_v2}.
 */
public record AbilityInfo(String abilityId, String localizedName, String englishName, String category,
                          boolean hasLeftClick, String kind, String description, int cost,
                          double drainPerSecond, int cooldownSeconds, String pathway, int sequence,
                          boolean active, boolean locked, boolean blocked, String blockedBy, String icon) {

    public static final String ACTION_EXECUTE = "execute";
    public static final String ACTION_LEFT_CLICK = "left_click";
    private static final String LEFT_CLICK_MARKER = "#left_click";

    /**
     * Wire values of the {@code kind} field.
     */
    public static final String KIND_ACTIVE = "active";
    public static final String KIND_ACTIVATED = "activated";
    public static final String KIND_PASSIVE = "passive";

    /**
     * The five fields a protocol-1 {@code coi-client:abilities} entry carries;
     * everything richer defaults to empty/zero so the UI simply shows less.
     */
    public static AbilityInfo of(String abilityId, String localizedName, String englishName, String category,
                                 boolean hasLeftClick) {
        return new AbilityInfo(abilityId, localizedName, englishName, category, hasLeftClick,
                KIND_ACTIVE, "", 0, 0.0, 0, pathwayOf(abilityId), sequenceOf(abilityId),
                false, false, false, "", "");
    }

    /**
     * True for abilities whose state the server tracks — i.e. everything that
     * can light up the HUD slot's toggle outline.
     */
    public boolean isToggle() {
        return !KIND_ACTIVE.equals(kind);
    }

    /**
     * True when the ability cannot be cast right now for a reason the player
     * should see (madness lock, grazing block, suppressed contract).
     */
    public boolean isUnavailable() {
        return locked || blocked;
    }

    public static String formatStored(String abilityId, String displayName, String action) {
        String storedId = ACTION_LEFT_CLICK.equals(action) ? abilityId + LEFT_CLICK_MARKER : abilityId;
        return storedId + " - " + displayName;
    }

    /**
     * Returns the base ability ID without display-name or client-side action suffixes.
     */
    public static String extractId(String stored) {
        if (stored == null) return null;
        String id = stored.contains(" - ") ? stored.split(" - ")[0] : stored;
        return id.endsWith(LEFT_CLICK_MARKER) ? id.substring(0, id.length() - LEFT_CLICK_MARKER.length()) : id;
    }

    public static String extractAction(String stored) {
        if (stored == null) return ACTION_EXECUTE;
        String id = stored.contains(" - ") ? stored.split(" - ")[0] : stored;
        return id.endsWith(LEFT_CLICK_MARKER) ? ACTION_LEFT_CLICK : ACTION_EXECUTE;
    }

    /**
     * Returns the display-name suffix from the stored "id - localizedName" format.
     */
    public static String extractDisplayName(String stored) {
        if (stored == null) return null;
        if (stored.contains(" - ")) {
            String[] parts = stored.split(" - ", 2);
            return parts.length > 1 ? parts[1] : stored;
        }
        return stored;
    }

    /**
     * Pathway name from the first segment of the ability id, normalised the
     * same way {@link #pathwayRgb(String)} normalises its key.
     */
    public static String pathwayOf(String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) return "";
        return normalizePathway(abilityId.split("-")[0]);
    }

    /**
     * Sequence number from the second segment of the ability id, or -1.
     */
    public static int sequenceOf(String abilityId) {
        if (abilityId == null) return -1;
        String[] parts = abilityId.split("-");
        if (parts.length < 2) return -1;
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Lower-cased pathway key, with {@code eternalaeon} folded onto
     * {@code aeon} — the plugin uses both spellings.
     */
    public static String normalizePathway(String pathway) {
        if (pathway == null) return "";
        String key = pathway.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").trim();
        return "eternalaeon".equals(key) ? "aeon" : key;
    }

    /**
     * Pathway accent color (ARGB), from the first segment of the ability id.
     */
    public static int pathwayColor(String abilityId) {
        return pathwayColorByName(pathwayOf(abilityId));
    }

    /**
     * Pathway accent color (ARGB) for a pathway name.
     */
    public static int pathwayColorByName(String pathway) {
        return 0xFF000000 | pathwayRgb(pathway);
    }

    /**
     * The single pathway colour table for the whole mod — HUD slots, picker
     * rows, acting bar and the mythical-form burst all read from here, so a
     * pathway can never be gold in one place and grey in another.
     */
    public static int pathwayRgb(String pathway) {
        return switch (normalizePathway(pathway)) {
            case "fool" -> 0xB347CC;
            case "door" -> 0x5B7FE6;
            case "sun" -> 0xFFE55C;
            case "tyrant" -> 0x4AA3FF;
            case "demoness" -> 0xB22222;
            case "priest" -> 0xFF6B35;
            case "error" -> 0x999999;
            case "tower" -> 0x7788AA;
            case "visionary" -> 0x44CCBB;
            case "hanged" -> 0x3A6E4F;
            case "darkness" -> 0x4A1A6E;
            case "death" -> 0xC8D0E8;
            case "giant" -> 0xC08840;
            case "paragon" -> 0xE8E8FF;
            case "hermit" -> 0x8855CC;
            case "fortune" -> 0xFFD700;
            case "chained" -> 0x666677;
            case "abyss" -> 0x551133;
            case "justiciar" -> 0xEEDD88;
            case "emperor" -> 0xDD9922;
            case "moon" -> 0xC7B8E8;
            case "mother" -> 0x7FBF6B;
            case "patriarch" -> 0x3FA9C9;
            case "sublunary" -> 0x9AA7C4;
            case "aeon" -> 0xFFF1C0;
            default -> 0xCCCCFF;
        };
    }

    /**
     * Icon tier folder name derived from the sequence number in the ability id.
     */
    public static String tierOf(String rawId) {
        if (rawId == null) return "low";
        String id = rawId.contains(" - ") ? rawId.split(" - ")[0] : rawId;
        int seq = sequenceOf(id);
        return switch (seq) {
            case 0 -> "divine";
            case 1, 2 -> "fair";
            case 3, 4 -> "high";
            case 5, 6, 7 -> "mid";
            default -> "low";
        };
    }
}
