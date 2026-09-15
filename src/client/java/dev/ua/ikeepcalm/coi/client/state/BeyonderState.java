package dev.ua.ikeepcalm.coi.client.state;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;

/**
 * The local player's Beyonder condition, fed by {@code coi-client:conditions}.
 * <p>
 * Everything here is "unknown until a server says so": the pathway is empty,
 * the sequence is -1 and both {@link #hasSpiritualityData()} and
 * {@link #hasHealthData()} are false, which is what keeps the spirituality and
 * HP bars off a vanilla or pre-protocol-2 server instead of showing 0/0.
 * <p>
 * The wire format lives in {@code ConditionsParser} and the spirituality regen
 * prediction in {@code SpiritualityTracker}; what is left here is the state
 * itself and the accessors the HUD reads every frame.
 */
public class BeyonderState {

    /**
     * How long the madness flash takes to fade after an increase.
     */
    private static final long FLASH_MS = 500;

    private static double madness = 0.0;
    private static double permanentMadness = 0.0;
    private static int freezeStacks = 0;
    private static int mentalPressure = 0;
    private static double tiredness = 0.0;
    private static long lastMadnessIncreaseTime = 0;

    private static final SpiritualityTracker SPIRITUALITY = new SpiritualityTracker();

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

    /**
     * 1 → 0 over {@value #FLASH_MS} ms after madness goes up, for the HUD's
     * flash and shake.
     */
    public static float getFlashIntensity() {
        long elapsed = System.currentTimeMillis() - lastMadnessIncreaseTime;
        if (elapsed >= FLASH_MS) return 0.0f;
        return 1.0f - (elapsed / (float) FLASH_MS);
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
        pathway = Pathways.normalizePathway(pathwayName);
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
        return SPIRITUALITY.current();
    }

    public static int getMaxSpirituality() {
        return SPIRITUALITY.max();
    }

    public static boolean isSpiritualityRegen() {
        return SPIRITUALITY.regenerating();
    }

    /**
     * False until a server sends the spirituality keys on {@code conditions} —
     * old servers never do, so the bar stays hidden instead of showing 0/0.
     */
    public static boolean hasSpiritualityData() {
        return SPIRITUALITY.hasData();
    }

    /**
     * 1 → 0 over 500ms after spirituality drops, mirroring the madness flash.
     */
    public static float getSpiritualityFlashIntensity() {
        return SPIRITUALITY.flashIntensity();
    }

    /**
     * Milliseconds since spirituality last went <em>up</em>, or a large number
     * if it never has. The HUD flares the fill's leading edge right after a
     * regen tick; tying it to gains (not to every {@code conditions} packet)
     * keeps the flare a pulse rather than a constant glow.
     */
    public static long spiritualityGainAgeMs() {
        return SPIRITUALITY.gainAgeMs();
    }

    /**
     * Spirituality extrapolated forward from the last increase at the measured
     * regen rate, so the bar glides instead of stepping once per second.
     *
     * @see SpiritualityTracker#predicted()
     */
    public static double predictedSpirituality() {
        return SPIRITUALITY.predicted();
    }

    public static void updateSpirituality(int current, int max, boolean regen) {
        SPIRITUALITY.update(current, max, regen);
    }

    public static void updateConditions(double madnessVal, double permMadnessVal, int freezeVal,
                                        int pressureVal, double tirednessVal) {
        if (madnessVal > madness) {
            lastMadnessIncreaseTime = System.currentTimeMillis();
        }
        madness = madnessVal;
        permanentMadness = permMadnessVal;
        ClientStateStore.setPermanentMadness(permMadnessVal);
        freezeStacks = freezeVal;
        mentalPressure = pressureVal;
        tiredness = tirednessVal;
    }

    /**
     * Entry point for the {@code coi-client:conditions} payload.
     */
    public static void parseAndUpdate(String data) {
        ConditionsParser.apply(data);
    }

    public static void reset() {
        madness = 0.0;
        permanentMadness = 0.0;
        freezeStacks = 0;
        mentalPressure = 0;
        tiredness = 0.0;
        lastMadnessIncreaseTime = 0;
        SPIRITUALITY.reset();
        pathway = "";
        sequence = -1;
        maxHealth = 0.0;
        hasHealthData = false;
    }
}
