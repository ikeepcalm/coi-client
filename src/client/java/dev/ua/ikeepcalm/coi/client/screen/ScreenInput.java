package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The keyboard concerns the mod's own screens share.
 * <p>
 * Several of them ({@link TourScreen}, the gesture canvas, the ability wheel)
 * are meant to sit over a world that keeps moving, and several of them name a
 * keybind in their own text — both of which are the same few lines wherever
 * they appear.
 */
public class ScreenInput {

    private ScreenInput() {
    }

    /**
     * The key a mapping is currently bound to, as it should be printed.
     */
    public static Component keyName(KeyMapping key) {
        return KeyMappingHelper.getBoundKeyOf(key).getDisplayName();
    }

    /**
     * Screens normally swallow keyboard input, freezing the player. Feed the
     * raw key state back into the movement bindings so the player can keep
     * moving while the screen is open.
     */
    public static void keepMovementKeysAlive(Minecraft client) {
        if (client.player == null) return;
        var options = client.options;
        KeyMapping[] movementKeys = {
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint
        };
        for (KeyMapping key : movementKeys) {
            key.setDown(CoiKeyBindings.isKeyDown(key));
        }
    }
}
