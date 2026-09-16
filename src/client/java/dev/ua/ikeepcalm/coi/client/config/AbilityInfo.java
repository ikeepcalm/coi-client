package dev.ua.ikeepcalm.coi.client.config;

public record AbilityInfo(String abilityId, String localizedName, String englishName, String category,
                          boolean hasLeftClick) {

    public static final String ACTION_EXECUTE = "execute";
    public static final String ACTION_LEFT_CLICK = "left_click";
    private static final String LEFT_CLICK_MARKER = "#left_click";

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
     * Pathway accent color (ARGB), from the first segment of the ability id.
     */
    public static int pathwayColor(String abilityId) {
        if (abilityId == null) return 0xFF666666;
        String pathway = abilityId.split("-")[0];
        return switch (pathway.toLowerCase()) {
            case "fool" -> 0xFFB347CC;
            case "door" -> 0xFF5B7FE6;
            case "sun" -> 0xFFFFE55C;
            case "tyrant" -> 0xFF4AA3FF;
            case "demoness" -> 0xFFB22222;
            case "priest" -> 0xFFFF6B35;
            case "error" -> 0xFF999999;
            default -> 0xFF777777;
        };
    }

    /**
     * Icon tier folder name derived from the sequence number in the ability id.
     */
    public static String tierOf(String rawId) {
        if (rawId == null) return "low";
        String id = rawId.contains(" - ") ? rawId.split(" - ")[0] : rawId;
        String[] parts = id.split("-");
        if (parts.length < 2) return "low";
        try {
            int seq = Integer.parseInt(parts[1]);
            return switch (seq) {
                case 0 -> "divine";
                case 1, 2 -> "fair";
                case 3, 4 -> "high";
                case 5, 6, 7 -> "mid";
                default -> "low";
            };
        } catch (NumberFormatException e) {
            return "low";
        }
    }
}
