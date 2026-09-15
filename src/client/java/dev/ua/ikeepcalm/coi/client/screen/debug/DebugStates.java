package dev.ua.ikeepcalm.coi.client.screen.debug;

import dev.ua.ikeepcalm.coi.client.state.ActingState;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;
import dev.ua.ikeepcalm.coi.client.state.ResourceState;

/**
 * The state pokes behind {@link EffectDebugScreen}'s buttons: everything that
 * stands in for a server packet so a HUD element can be judged offline.
 * <p>
 * Each one goes through the same {@code update…} call the network handlers use,
 * so the flash, shake and easing a real update triggers happen here too.
 */
final class DebugStates {

    /**
     * Default max for the debug bar, so the buttons do something useful before
     * a server has ever sent a real one.
     */
    static final int DEBUG_MAX_SPIRIT = 1000;

    /**
     * A Sequence-0 sized pool, so the readout has to solve the four-digit
     * fitting problem the bar was written for.
     */
    static final double DEBUG_MAX_HEALTH = 1750;

    private DebugStates() {
    }

    static void addMadness(double delta) {
        setMadness(BeyonderState.getMadness() + delta);
    }

    /**
     * Jumps to the next stage threshold: 0 → 25 → 50 → 75 → 100 → 0.
     */
    static void cycleStage() {
        double m = BeyonderState.getMadness();
        double next;
        if (m < 25) next = 25;
        else if (m < 50) next = 50;
        else if (m < 75) next = 75;
        else if (m < 100) next = 100;
        else next = 0;
        setMadness(next);
    }

    static void addPermMadness(double delta) {
        double perm = Math.clamp(BeyonderState.getPermanentMadness() + delta, 0.0, 100.0);
        BeyonderState.updateConditions(
                BeyonderState.getMadness(),
                perm,
                BeyonderState.getFreezeStacks(),
                BeyonderState.getMentalPressure(),
                BeyonderState.getTiredness());
    }

    /**
     * Sets madness via updateConditions so increases also trigger the
     * bar's flash/shake animation, exactly like a server update would.
     */
    static void setMadness(double value) {
        BeyonderState.updateConditions(
                Math.clamp(value, 0.0, 100.0),
                BeyonderState.getPermanentMadness(),
                BeyonderState.getFreezeStacks(),
                BeyonderState.getMentalPressure(),
                BeyonderState.getTiredness());
    }

    /**
     * Fills every row the character plate can show, so it can be judged without
     * a server: an identity for the header, a madness value for the sanity
     * gauge with a permanent floor to cap it, an acting grant and its cooldown
     * for the mask row, and the two fake resource meters for the reserves.
     */
    static void seedPlate() {
        if (!BeyonderState.hasIdentity()) BeyonderState.updateIdentity("fool", 5);
        if (BeyonderState.getMadness() <= 0) setMadness(38);
        if (BeyonderState.getPermanentMadness() <= 0) addPermMadness(12);
        ActingState.debugGrant(10);
        ActingState.debugCooldown(252);
        ResourceState.debugInject();
    }

    static int debugMaxSpirit() {
        int max = BeyonderState.getMaxSpirituality();
        return max > 0 ? max : DEBUG_MAX_SPIRIT;
    }

    static void addSpirit(int delta) {
        int max = debugMaxSpirit();
        int current = Math.clamp(BeyonderState.getSpirituality() + delta, 0, max);
        BeyonderState.updateSpirituality(current, max, current < max);
    }

    static void setSpiritMax(int max) {
        int current = Math.min(BeyonderState.getSpirituality(), max);
        BeyonderState.updateSpirituality(current, max, current < max);
    }
}
