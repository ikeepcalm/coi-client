package dev.ua.ikeepcalm.coi.client.input;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;
import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.client.network.payload.AbilityUsePayload;
import dev.ua.ikeepcalm.coi.client.network.payload.ActionPayload;
import dev.ua.ikeepcalm.coi.client.screen.GestureScreen;
import dev.ua.ikeepcalm.coi.client.screen.ability.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.client.screen.ability.AbilityWheelScreen;
import dev.ua.ikeepcalm.coi.client.screen.debug.EffectDebugScreen;
import dev.ua.ikeepcalm.coi.client.screen.sheet.CharacterSheetScreen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's keymappings and what pressing one does.
 * <p>
 * All {@link AbilityBindings#MAX_ABILITIES} ability keys are registered up
 * front even though only {@code activeAbilitySlots} of them are read: a
 * keymapping can only be registered at init, so lowering the slot count has to
 * hide a binding rather than remove it.
 * <p>
 * Presses are edge-triggered by hand ({@code keyPressed}) rather than through
 * {@code consumeClick}, because the wheel and the gesture screen need to know
 * the key is still <em>held</em>, which a consumed click no longer says.
 */
public final class CoiKeyBindings {

    /**
     * Slots in {@link #keyPressed} past the ability keys. They are indices into
     * the same edge-trigger array, not ability slots.
     */
    private static final int PRESS_BINDING_SCREEN = AbilityBindings.MAX_ABILITIES;
    private static final int PRESS_DEBUG_SCREEN = AbilityBindings.MAX_ABILITIES + 1;
    private static final int PRESS_OPEN_MENU = AbilityBindings.MAX_ABILITIES + 2;
    private static final int TRACKED_KEYS = AbilityBindings.MAX_ABILITIES + 3;

    /**
     * Ability slots 1-6 default to Z, X, C, V, B, N. Slots 7+ default unbound —
     * the player assigns keys in vanilla Controls.
     */
    private static final int[] DEFAULT_ABILITY_KEYS = {
            GLFW.GLFW_KEY_Z,
            GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_V,
            GLFW.GLFW_KEY_B,
            GLFW.GLFW_KEY_N
    };

    private static final boolean[] keyPressed = new boolean[TRACKED_KEYS];
    private static final KeyMapping[] abilityKeys = new KeyMapping[AbilityBindings.MAX_ABILITIES];

    public static KeyMapping abilityMenu;
    public static KeyMapping abilityWheel;
    public static KeyMapping openMenu;
    public static KeyMapping gestureCast;
    /**
     * Null outside the development environment, where the debug screen does not
     * exist as far as the player is concerned.
     */
    public static KeyMapping effectDebugMenu;

    private CoiKeyBindings() {
    }

    public static void registerKeybindings() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("category.coi.abilities"));

        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            int defaultKey = i < DEFAULT_ABILITY_KEYS.length
                    ? DEFAULT_ABILITY_KEYS[i] : GLFW.GLFW_KEY_UNKNOWN;
            abilityKeys[i] = register("key.coi.ability" + (i + 1), defaultKey, category);
        }

        abilityMenu = register("screen.coi.ability_binding", GLFW.GLFW_KEY_K, category);
        abilityWheel = register("key.coi.ability_wheel", GLFW.GLFW_KEY_G, category);
        openMenu = register("key.coi.open_menu", GLFW.GLFW_KEY_M, category);
        gestureCast = register("key.coi.gesture", GLFW.GLFW_KEY_LEFT_ALT, category);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            effectDebugMenu = register("screen.coi.effect_debug", GLFW.GLFW_KEY_F8, category);
        }
    }

    private static KeyMapping register(String translationKey, int defaultKey, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, category));
    }

    /**
     * One tick of the mod's keys. Called from the client's single tick
     * subscription, after the {@code player == null} guard.
     */
    public static void tickKeys(Minecraft client) {
        for (int i = 0; i < AbilityBindings.getActiveAbilitySlots(); i++) {
            int slot = i;
            onPress(i, abilityKeys[i], () -> useAbility(AbilityBindings.getBoundAbility(slot)));
        }

        onPress(PRESS_BINDING_SCREEN, abilityMenu, () -> client.gui.setScreen(new AbilityBindingScreen(null)));
        onPress(PRESS_OPEN_MENU, openMenu, () -> openServerMenu(client));
        if (effectDebugMenu != null) {
            onPress(PRESS_DEBUG_SCREEN, effectDebugMenu, () -> client.gui.setScreen(new EffectDebugScreen(null)));
        }

        if (abilityWheel.isDown() && client.gui.screen() == null) {
            client.gui.setScreen(new AbilityWheelScreen());
        }

        // Gesture casting: inert until at least one gesture has an ability bound
        if (gestureCast.isDown() && client.gui.screen() == null && AbilityBindings.hasAnyGestureBound()) {
            client.gui.setScreen(new GestureScreen());
        }
    }

    /**
     * Runs {@code action} on the frame the key goes down, and not again until
     * it has been released.
     */
    private static void onPress(int index, KeyMapping key, Runnable action) {
        if (!key.isDown()) {
            keyPressed[index] = false;
            return;
        }
        if (keyPressed[index]) return;
        keyPressed[index] = true;
        action.run();
    }

    /**
     * Sends the ability to the server and starts its HUD cast animation.
     */
    public static void useAbility(String abilityIdWithName) {
        if (abilityIdWithName == null) return;

        String abilityId = AbilityInfo.extractId(abilityIdWithName);
        String action = AbilityInfo.extractAction(abilityIdWithName);

        ClientPlayNetworking.send(new AbilityUsePayload(abilityId, action));
        AbilityOverlay.onAbilityCast(abilityId);
    }

    /**
     * Opens whichever "my Beyonder" surface the server can actually feed: the
     * mod's own character sheet, else the plugin's chest menu, else a message.
     * A server that advertised neither has nothing listening, so say so instead
     * of sending into the void.
     */
    private static void openServerMenu(Minecraft client) {
        if (client.player == null) return;
        if (ServerCapabilities.has("character_sheet")) {
            client.gui.setScreen(new CharacterSheetScreen(null));
        } else if (ServerCapabilities.has("menu_action")) {
            ClientPlayNetworking.send(ActionPayload.of("open_menu"));
        } else {
            client.player.sendOverlayMessage(Component.translatable("notification.coi.menu_unsupported"));
        }
    }

    /**
     * The keymapping of ability slot {@code index}, counting from 0.
     */
    public static KeyMapping abilityKey(int index) {
        return abilityKeys[index];
    }

    /**
     * Whether the key is physically down right now, asked of GLFW rather than
     * of the keymapping — the wheel and the gesture screen open <em>while</em>
     * a key is held, and a screen's own input handling swallows the mapping.
     */
    public static boolean isKeyDown(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        long window = Minecraft.getInstance().getWindow().handle();
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(keyBinding);

        return switch (key.getType()) {
            case KEYSYM -> GLFW.glfwGetKey(window, key.getValue()) != GLFW.GLFW_RELEASE;
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) != GLFW.GLFW_RELEASE;
            default -> false;
        };
    }
}
