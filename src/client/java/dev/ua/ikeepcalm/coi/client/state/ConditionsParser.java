package dev.ua.ikeepcalm.coi.client.state;

import dev.ua.ikeepcalm.coi.CoiLog;

/**
 * The {@code coi-client:conditions} body: {@code key=value} pairs separated by
 * semicolons, the one channel that never became JSON.
 * <p>
 * Every key is optional and every group is applied only if the packet actually
 * carried it — a server that sends madness alone must not be read as saying the
 * player has no pathway and no spirituality. Unknown keys are ignored outright,
 * so a newer plugin can add one without this client noticing.
 */
final class ConditionsParser {

    private ConditionsParser() {
    }

    /**
     * Mutable scratch for one packet, seeded with the values already on screen
     * so an absent key leaves its field alone.
     */
    private static final class Reading {
        double madness = BeyonderState.getMadness();
        double permanentMadness = BeyonderState.getPermanentMadness();
        int freezeStacks = BeyonderState.getFreezeStacks();
        int mentalPressure = BeyonderState.getMentalPressure();
        double tiredness = BeyonderState.getTiredness();

        int spirituality = BeyonderState.getSpirituality();
        int maxSpirituality = BeyonderState.getMaxSpirituality();
        boolean spiritualityRegen = BeyonderState.isSpiritualityRegen();
        boolean sawSpirituality;

        String pathway = BeyonderState.getPathway();
        int sequence = BeyonderState.getSequence();
        boolean sawPathway;

        double maxHealth = BeyonderState.maxHealth();
        boolean sawMaxHealth;
    }

    /**
     * Reads {@code data} and pushes whatever it named onto
     * {@link BeyonderState}. A malformed packet is logged and dropped; the
     * groups read before it went wrong are not applied, since the apply happens
     * only once the whole body has parsed.
     */
    static void apply(String data) {
        if (data == null || data.isEmpty()) return;
        Reading reading = new Reading();
        try {
            for (String pair : data.split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) read(reading, kv[0].trim(), kv[1].trim());
            }
            publish(reading);
        } catch (Exception e) {
            CoiLog.LOG.warn("Failed to parse Beyonder conditions: {}", data, e);
        }
    }

    private static void read(Reading reading, String key, String value) {
        switch (key) {
            case "madness" -> reading.madness = Double.parseDouble(value);
            case "permanentMadness" -> reading.permanentMadness = Double.parseDouble(value);
            case "freezeStacks" -> reading.freezeStacks = Integer.parseInt(value);
            case "mentalPressure" -> reading.mentalPressure = Integer.parseInt(value);
            case "tiredness" -> reading.tiredness = Double.parseDouble(value);
            case "spirituality" -> {
                reading.spirituality = Integer.parseInt(value);
                reading.sawSpirituality = true;
            }
            case "maxSpirituality" -> reading.maxSpirituality = Integer.parseInt(value);
            case "spiritualityRegen" -> reading.spiritualityRegen = Boolean.parseBoolean(value);
            case "maxHealth" -> {
                reading.maxHealth = Double.parseDouble(value);
                reading.sawMaxHealth = true;
            }
            case "pathway" -> {
                reading.pathway = value;
                reading.sawPathway = true;
            }
            case "sequence" -> reading.sequence = Integer.parseInt(value);
            default -> {
                // A key this client has never heard of: a newer plugin's to know about
            }
        }
    }

    private static void publish(Reading reading) {
        BeyonderState.updateConditions(reading.madness, reading.permanentMadness,
                reading.freezeStacks, reading.mentalPressure, reading.tiredness);
        if (reading.sawSpirituality) {
            BeyonderState.updateSpirituality(reading.spirituality, reading.maxSpirituality,
                    reading.spiritualityRegen);
        }
        if (reading.sawPathway) {
            BeyonderState.updateIdentity(reading.pathway, reading.sequence);
        }
        if (reading.sawMaxHealth) {
            BeyonderState.updateMaxHealth(reading.maxHealth);
        }
    }
}
