package dev.ua.ikeepcalm.coi.client.ability;

import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;

import java.util.Objects;

/**
 * Which ability sits in which slot — the key-bound row, the wheel and the
 * gestures — and the persistence behind all three.
 */
public class AbilityBindings {

    private AbilityBindings() {
    }

    // Hard ceiling for key-bound ability slots. Keymappings can only be registered
    // once at init, so all MAX_ABILITIES are registered up front and the player-facing
    // count is HudConfig.activeAbilitySlots (see getActiveAbilitySlots()).
    public static final int MAX_ABILITIES = 10;
    public static final int MAX_WHEEL_SIZE = 16;

    private static String[] boundAbilities = new String[MAX_ABILITIES];
    private static String[] wheelAbilities = new String[MAX_WHEEL_SIZE];
    private static String[] gestureAbilities = new String[GestureType.values().length];

    /**
     * Reads all three binding sets back off disk. Called once at init, before
     * anything can ask which ability a slot holds.
     */
    public static void load() {
        boundAbilities = AbilityConfig.loadBindings();
        wheelAbilities = AbilityConfig.loadWheelBindings();
        gestureAbilities = AbilityConfig.loadGestureBindings();
    }

    public static String getBoundAbility(int slot) {
        return boundAbilities[slot];
    }

    public static void setBoundAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < MAX_ABILITIES) {
            boundAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities, wheelAbilities, gestureAbilities);
            AbilityOverlay.updateAbilitySlot(slot, abilityId);
        }
    }

    public static String getWheelAbility(int slot) {
        if (slot >= 0 && slot < MAX_WHEEL_SIZE) {
            return wheelAbilities[slot];
        }
        return null;
    }

    public static void setWheelAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < MAX_WHEEL_SIZE) {
            wheelAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities, wheelAbilities, gestureAbilities);
        }
    }

    public static String getGestureAbility(GestureType type) {
        return gestureAbilities[type.ordinal()];
    }

    public static String getGestureAbility(int slot) {
        if (slot >= 0 && slot < gestureAbilities.length) {
            return gestureAbilities[slot];
        }
        return null;
    }

    public static void setGestureAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < gestureAbilities.length) {
            gestureAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities, wheelAbilities, gestureAbilities);
        }
    }

    public static boolean hasAnyGestureBound() {
        for (String ability : gestureAbilities) {
            if (ability != null) return true;
        }
        return false;
    }

    public static int getWheelSize() {
        return HudConfig.getSettings().wheelSlots;
    }

    public static int getMaxAbilities() {
        return MAX_ABILITIES;
    }

    public static int getActiveAbilitySlots() {
        return Math.clamp(HudConfig.getSettings().activeAbilitySlots, 1, MAX_ABILITIES);
    }

    public static String[] getBoundAbilitiesSnapshot() {
        return boundAbilities.clone();
    }

    public static String[] getWheelAbilitiesSnapshot() {
        return wheelAbilities.clone();
    }

    static void updateHudWithCurrentBindings() {
        validateBoundAbilities();

        for (int i = 0; i < MAX_ABILITIES; i++) {
            AbilityOverlay.updateAbilitySlot(i, boundAbilities[i]);
        }
    }

    private static void validateBoundAbilities() {
        for (int i = 0; i < getActiveAbilitySlots(); i++) {
            boundAbilities[i] = refreshAbilityEntry(boundAbilities[i]);
        }

        for (int i = 0; i < MAX_WHEEL_SIZE; i++) {
            wheelAbilities[i] = refreshAbilityEntry(wheelAbilities[i]);
        }

        for (int i = 0; i < gestureAbilities.length; i++) {
            gestureAbilities[i] = refreshAbilityEntry(gestureAbilities[i]);
        }
    }

    private static String refreshAbilityEntry(String stored) {
        if (stored == null) return null;
        String freshEntry = findFreshAbilityEntry(stored);
        return freshEntry != null ? freshEntry : stored;
    }

    private static String findFreshAbilityEntry(String storedAbility) {
        String boundId = AbilityInfo.extractId(storedAbility);
        String boundAction = AbilityInfo.extractAction(storedAbility);
        return AbilityRegistry.entries().stream()
                .filter(a -> Objects.equals(AbilityInfo.extractId(a), boundId))
                .filter(a -> Objects.equals(AbilityInfo.extractAction(a), boundAction))
                .filter(a -> Objects.equals(AbilityInfo.extractCategory(a), AbilityInfo.extractCategory(storedAbility)))
                .findFirst()
                .orElse(null);
    }
}
