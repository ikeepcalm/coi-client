package dev.ua.ikeepcalm.coi.client.config;

public record AbilityInfo(String abilityId, String localizedName, String englishName, String category) {

    /**
     * Returns the ability ID without any display-name suffix (i.e. without " - localizedName").
     */
    public static String extractId(String stored) {
        if (stored == null) return null;
        return stored.contains(" - ") ? stored.split(" - ")[0] : stored;
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
}
