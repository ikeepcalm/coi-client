package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;

public class ClientBeyonderState {

    private static double madness = 0.0;
    private static double permanentMadness = 0.0;
    private static int freezeStacks = 0;
    private static int mentalPressure = 0;
    private static double tiredness = 0.0;

    // Spirituality — only present once a protocol-2 server sends it; until then
    // hasSpiritualityData stays false and the HUD bar never draws
    private static int spirituality = 0;
    private static int maxSpirituality = 0;
    private static boolean spiritualityRegen = false;
    private static boolean hasSpiritualityData = false;
    private static long lastSpiritualityDecreaseTime = 0;

    // Regen prediction — the server raises spirituality in once-a-second steps,
    // so the bar interpolates between packets instead of stepping. The rate is
    // measured from the last two increases and thrown away on any decrease.
    private static long lastSpiritualityPacketMs = 0;
    private static long lastSpiritualityIncreaseMs = 0;
    private static int lastSpiritualityIncreaseValue = 0;
    private static double spiritualityRegenPerMs = 0.0;

    /** Cap on how far ahead of the last increase the prediction may run. */
    private static final long PREDICT_WINDOW_MS = 1500;
    /** No conditions packet for this long: drop back to the raw value. */
    private static final long STALE_PACKET_MS = 5000;

    // The Beyonder HP pool's maximum. Only the *maximum* travels: the pool is a
    // proportional mirror of vanilla health and the server recomputes it from
    // vanilla health after every hit, so the HUD derives the current value
    // itself and never has to wait for — or trust — a pushed number.
    private static double maxHealth = 0.0;
    private static boolean hasHealthData = false;

    // Beyonder identity — pathway name (lowercased, eternalaeon folded onto
    // aeon) and lowest sequence level; empty/-1 until a protocol-2 server says
    private static String pathway = "";
    private static int sequence = -1;

    // For flash / shake animation when madness increases
    private static double lastMadness = 0.0;
    private static long lastMadnessIncreaseTime = 0;
    private static float flashIntensity = 0.0f;

    public static double getMadness() {
        return madness;
    }

    public static double getPermanentMadness() {
        return permanentMadness;
    }

    public static int getFreezeStacks() {
        return freezeStacks;
    }

    public static int getMentalPressure() {
        return mentalPressure;
    }

    public static double getTiredness() {
        return tiredness;
    }

    public static float getFlashIntensity() {
        long elapsed = System.currentTimeMillis() - lastMadnessIncreaseTime;
        if (elapsed >= 500) {
            flashIntensity = 0.0f;
        } else {
            flashIntensity = 1.0f - (elapsed / 500.0f);
        }
        return flashIntensity;
    }

    public static String getPathway() {
        return pathway;
    }

    public static int getSequence() {
        return sequence;
    }

    /**
     * True once the server has named the player's pathway — the cue for
     * Discord presence and the acting bar to stop guessing.
     */
    public static boolean hasIdentity() {
        return !pathway.isEmpty();
    }

    public static void updateIdentity(String pathwayName, int sequenceLevel) {
        pathway = AbilityInfo.normalizePathway(pathwayName);
        sequence = sequenceLevel;
    }

    /**
     * The HP pool's maximum, in pool units. Meaningless while
     * {@link #hasHealthData()} is false.
     */
    public static double maxHealth() {
        return maxHealth;
    }

    /**
     * False until a server sends a positive {@code maxHealth} on
     * {@code conditions} — vanilla and pre-protocol-2 servers never do, so the
     * vanilla hearts stay untouched instead of being replaced by an empty bar.
     */
    public static boolean hasHealthData() {
        return hasHealthData;
    }

    /**
     * A non-positive maximum is the server saying "no pool here", not a pool of
     * zero, so it retires the bar rather than drawing it empty.
     */
    public static void updateMaxHealth(double value) {
        maxHealth = Math.max(0.0, value);
        hasHealthData = value > 0;
    }

    public static int getSpirituality() {
        return spirituality;
    }

    public static int getMaxSpirituality() {
        return maxSpirituality;
    }

    public static boolean isSpiritualityRegen() {
        return spiritualityRegen;
    }

    /**
     * False until a server sends the spirituality keys on {@code conditions} —
     * old servers never do, so the bar stays hidden instead of showing 0/0.
     */
    public static boolean hasSpiritualityData() {
        return hasSpiritualityData;
    }

    /**
     * 1 → 0 over 500ms after spirituality drops, mirroring the madness flash.
     */
    public static float getSpiritualityFlashIntensity() {
        long elapsed = System.currentTimeMillis() - lastSpiritualityDecreaseTime;
        if (lastSpiritualityDecreaseTime == 0 || elapsed >= 500) return 0.0f;
        return 1.0f - (elapsed / 500.0f);
    }

    /**
     * Milliseconds since spirituality last went <em>up</em>, or a large number
     * if it never has. The HUD flares the fill's leading edge right after a
     * regen tick; tying it to gains (not to every {@code conditions} packet)
     * keeps the flare a pulse rather than a constant glow.
     */
    public static long spiritualityGainAgeMs() {
        if (lastSpiritualityIncreaseMs == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastSpiritualityIncreaseMs;
    }

    /**
     * Spirituality extrapolated forward from the last increase at the measured
     * regen rate, so the bar glides instead of stepping once per second. Falls
     * back to the raw value while not regenerating or before a rate is known,
     * and never reads below {@link #getSpirituality()} or above the max.
     */
    public static double predictedSpirituality() {
        if (!spiritualityRegen || spiritualityRegenPerMs <= 0 || lastSpiritualityIncreaseMs == 0) {
            return spirituality;
        }
        long now = System.currentTimeMillis();
        // Gone quiet: stop guessing rather than drift away from the server
        if (now - lastSpiritualityPacketMs > STALE_PACKET_MS) return spirituality;
        long dt = Math.min(PREDICT_WINDOW_MS, now - lastSpiritualityIncreaseMs);
        if (dt <= 0) return spirituality;
        double predicted = lastSpiritualityIncreaseValue + spiritualityRegenPerMs * dt;
        return Math.max(spirituality, Math.min(maxSpirituality, predicted));
    }

    public static void updateSpirituality(int current, int max, boolean regen) {
        long now = System.currentTimeMillis();
        current = Math.max(0, current);
        max = Math.max(0, max);
        if (hasSpiritualityData && current < spirituality) {
            lastSpiritualityDecreaseTime = now;
            forgetSpiritualityRate();
        } else if (hasSpiritualityData && current > spirituality && regen) {
            measureSpiritualityRate(current, max, now);
        }
        spirituality = current;
        maxSpirituality = max;
        spiritualityRegen = regen;
        hasSpiritualityData = true;
        lastSpiritualityPacketMs = now;
    }

    /**
     * Δvalue / Δtime across the last two increases, clamped to at most half the
     * pool per second so one odd packet can't send the bar racing.
     */
    private static void measureSpiritualityRate(int current, int max, long now) {
        if (lastSpiritualityIncreaseMs > 0 && now > lastSpiritualityIncreaseMs) {
            double rate = (current - lastSpiritualityIncreaseValue) / (double) (now - lastSpiritualityIncreaseMs);
            spiritualityRegenPerMs = Math.clamp(rate, 0.0, Math.max(1, max) / 2.0 / 1000.0);
        }
        lastSpiritualityIncreaseMs = now;
        lastSpiritualityIncreaseValue = current;
    }

    private static void forgetSpiritualityRate() {
        spiritualityRegenPerMs = 0.0;
        lastSpiritualityIncreaseMs = 0;
        lastSpiritualityIncreaseValue = 0;
    }

    public static void updateConditions(double madnessVal, double permMadnessVal, int freezeVal, int pressureVal, double tirednessVal) {
        if (madnessVal > madness) {
            lastMadnessIncreaseTime = System.currentTimeMillis();
            flashIntensity = 1.0f;
        }
        lastMadness = madness;
        madness = madnessVal;
        permanentMadness = permMadnessVal;
        ClientStateStore.setPermanentMadness(permMadnessVal);
        freezeStacks = freezeVal;
        mentalPressure = pressureVal;
        tiredness = tirednessVal;
    }

    public static void parseAndUpdate(String data) {
        if (data == null || data.isEmpty()) return;

        double newMadness = madness;
        double newPermMadness = permanentMadness;
        int newFreeze = freezeStacks;
        int newPressure = mentalPressure;
        double newTiredness = tiredness;

        int newSpirituality = spirituality;
        int newMaxSpirituality = maxSpirituality;
        boolean newSpiritualityRegen = spiritualityRegen;
        boolean sawSpirituality = false;

        String newPathway = pathway;
        int newSequence = sequence;
        boolean sawPathway = false;

        double newMaxHealth = maxHealth;
        boolean sawMaxHealth = false;

        try {
            String[] pairs = data.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    switch (key) {
                        case "madness" -> newMadness = Double.parseDouble(value);
                        case "permanentMadness" -> newPermMadness = Double.parseDouble(value);
                        case "freezeStacks" -> newFreeze = Integer.parseInt(value);
                        case "mentalPressure" -> newPressure = Integer.parseInt(value);
                        case "tiredness" -> newTiredness = Double.parseDouble(value);
                        case "spirituality" -> {
                            newSpirituality = Integer.parseInt(value);
                            sawSpirituality = true;
                        }
                        case "maxSpirituality" -> newMaxSpirituality = Integer.parseInt(value);
                        case "spiritualityRegen" -> newSpiritualityRegen = Boolean.parseBoolean(value);
                        case "maxHealth" -> {
                            newMaxHealth = Double.parseDouble(value);
                            sawMaxHealth = true;
                        }
                        case "pathway" -> {
                            newPathway = value;
                            sawPathway = true;
                        }
                        case "sequence" -> newSequence = Integer.parseInt(value);
                    }
                }
            }
            updateConditions(newMadness, newPermMadness, newFreeze, newPressure, newTiredness);
            if (sawSpirituality) {
                updateSpirituality(newSpirituality, newMaxSpirituality, newSpiritualityRegen);
            }
            if (sawPathway) {
                updateIdentity(newPathway, newSequence);
            }
            if (sawMaxHealth) {
                updateMaxHealth(newMaxHealth);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Beyonder conditions: " + data);
            e.printStackTrace();
        }
    }

    public static void reset() {
        madness = 0.0;
        permanentMadness = 0.0;
        freezeStacks = 0;
        mentalPressure = 0;
        tiredness = 0.0;
        lastMadness = 0.0;
        lastMadnessIncreaseTime = 0;
        flashIntensity = 0.0f;
        spirituality = 0;
        maxSpirituality = 0;
        spiritualityRegen = false;
        hasSpiritualityData = false;
        lastSpiritualityDecreaseTime = 0;
        lastSpiritualityPacketMs = 0;
        forgetSpiritualityRate();
        pathway = "";
        sequence = -1;
        maxHealth = 0.0;
        hasHealthData = false;
    }
}
