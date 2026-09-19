package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.client.screen.ScreenInput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.resources.language.I18n;

/** The existing three binding stores, exposed as destinations for an ability or category. */
final class ManualBindings {
    record Target(String label, String existing, Consumer<String> assign, boolean conflict) {}
    static List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        for (int i = 0; i < AbilityBindings.getActiveAbilitySlots(); i++) {
            int slot = i;
            String key = ScreenInput.keyName(CoiKeyBindings.abilityKey(i)).getString();
            boolean conflict = false;
            for (int j = 0; j < AbilityBindings.getActiveAbilitySlots(); j++) {
                if (i != j && !CoiKeyBindings.abilityKey(i).isUnbound()
                        && key.equals(ScreenInput.keyName(CoiKeyBindings.abilityKey(j)).getString())) conflict = true;
            }
            targets.add(new Target(I18n.get("screen.coi.manual_key_slot", i + 1, key),
                    AbilityBindings.getBoundAbility(i), value -> AbilityBindings.setBoundAbility(slot, value), conflict));
        }
        for (int i = 0; i < AbilityBindings.getWheelSize(); i++) {
            int slot = i;
            targets.add(new Target(I18n.get("screen.coi.manual_wheel_slot", i + 1),
                    AbilityBindings.getWheelAbility(i), value -> AbilityBindings.setWheelAbility(slot, value), false));
        }
        for (GestureType gesture : GestureType.values()) {
            int slot = gesture.ordinal();
            targets.add(new Target(I18n.get("screen.coi.manual_gesture_slot", gesture.displayName().getString()),
                    AbilityBindings.getGestureAbility(slot), value -> AbilityBindings.setGestureAbility(slot, value), false));
        }
        return targets;
    }
}
