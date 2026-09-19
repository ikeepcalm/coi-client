package dev.ua.ikeepcalm.coi.client.ability;

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
    private static final String CATEGORY_MARKER = "#category=";

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

    public AbilityInfo withCastingStats(int newCost, double newDrain, int newCooldown) {
        return new AbilityInfo(abilityId, localizedName, englishName, category, hasLeftClick, kind, description,
                newCost, newDrain, newCooldown, pathway, sequence, active, locked, blocked, blockedBy, icon);
    }

    public AbilityInfo withActive(boolean newActive) {
        return new AbilityInfo(abilityId, localizedName, englishName, category, hasLeftClick, kind, description,
                cost, drainPerSecond, cooldownSeconds, pathway, sequence, newActive, locked, blocked, blockedBy, icon);
    }

    public static String formatStored(String abilityId, String displayName, String action) {
        String storedId = ACTION_LEFT_CLICK.equals(action) ? abilityId + LEFT_CLICK_MARKER : abilityId;
        return storedId + " - " + displayName;
    }

    public static String formatCategory(String abilityId, String displayName, String category) {
        String encoded = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(category.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return abilityId + CATEGORY_MARKER + encoded + " - " + displayName;
    }

    public static String extractCategory(String stored) {
        if (stored == null) return "";
        String id = stored.split(" - ", 2)[0];
        int marker = id.indexOf(CATEGORY_MARKER);
        if (marker < 0) return "";
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(id.substring(marker + CATEGORY_MARKER.length())),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    /**
     * Returns the base ability ID without display-name or client-side action suffixes.
     */
    public static String extractId(String stored) {
        if (stored == null) return null;
        String id = stored.contains(" - ") ? stored.split(" - ")[0] : stored;
        if (id.contains(CATEGORY_MARKER)) return id.substring(0, id.indexOf(CATEGORY_MARKER));
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
     * same way {@link Pathways#pathwayRgb(String)} normalises its key.
     */
    public static String pathwayOf(String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) return "";
        return Pathways.normalizePathway(abilityId.split("-")[0]);
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
     * Pathway accent color (ARGB), from the first segment of the ability id.
     */
    public static int pathwayColor(String abilityId) {
        return pathwayColorByName(pathwayOf(abilityId));
    }

    /**
     * Pathway accent color (ARGB) for a pathway name.
     */
    public static int pathwayColorByName(String pathway) {
        return 0xFF000000 | Pathways.pathwayRgb(pathway);
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
