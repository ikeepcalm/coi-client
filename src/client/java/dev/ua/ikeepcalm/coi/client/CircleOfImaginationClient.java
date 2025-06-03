package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.screen.AbilityBindingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CircleOfImaginationClient implements ClientModInitializer {

    private static final String ABILITY_USE_PREFIX = "_0_0_2_2_r";

    private static final List<String> availableAbilities = new ArrayList<>();
    private static String[] boundAbilities = new String[3];

    public static KeyBinding ability1Key;
    public static KeyBinding ability2Key;
    public static KeyBinding ability3Key;
    public static KeyBinding abilityMenu;

    private static final boolean[] keyPressed = new boolean[4];

    @Override
    public void onInitializeClient() {
        boundAbilities = AbilityConfig.loadBindings();
        registerKeybindings();
        registerTickHandler();
    }

    private void registerKeybindings() {
        ability1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.coi.abilities"
        ));

        ability2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.coi.abilities"
        ));

        ability3Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability3",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.coi.abilities"
        ));

        abilityMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "screen.coi.ability_binding",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.coi.abilities"
        ));
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            handleKeyPress(0, ability1Key, client);
            handleKeyPress(1, ability2Key, client);
            handleKeyPress(2, ability3Key, client);
            handleKeyPress(3, abilityMenu, client);
        });
    }

    private void handleKeyPress(int index, KeyBinding key, MinecraftClient client) {
        if (key.isPressed() && !keyPressed[index]) {
            keyPressed[index] = true;

            if (index == 3) {
                MinecraftClient.getInstance().setScreen(new AbilityBindingScreen(null));
                return;
            }

            if (boundAbilities[index] != null) {
                sendAbilityUse(boundAbilities[index], client);
            }
        } else if (!key.isPressed()) {
            keyPressed[index] = false;
        }
    }

    private void sendAbilityUse(String abilityIdWithName, MinecraftClient client) {
        if (client.player != null && abilityIdWithName != null) {
            String abilityId = abilityIdWithName;
            if (abilityIdWithName.contains(" - ")) {
                abilityId = abilityIdWithName.split(" - ")[0];
            }
            client.player.networkHandler.sendChatMessage(ABILITY_USE_PREFIX + "USE:" + abilityId);

            client.player.sendMessage(Text.translatable("notification.coi.ability_used",
                    abilityIdWithName.contains(" - ") ? abilityIdWithName.split(" - ")[1] : abilityIdWithName), true);
        }
    }

    public static void handleAbilityData(String data) {
        availableAbilities.clear();

        if (data.isEmpty()) return;

        String[] abilities = data.split(";");
        for (String ability : abilities) {
            if (!ability.isEmpty()) {
                String[] parts = ability.split("\\|");
                if (parts.length == 2) {
                    availableAbilities.add(parts[0] + " - " + parts[1]);
                }
            }
        }
    }

    public static List<String> getAvailableAbilities() {
        return new ArrayList<>(availableAbilities);
    }

    public static String getBoundAbility(int slot) {
        return boundAbilities[slot];
    }

    public static void setBoundAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < 3) {
            boundAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities);
        }
    }
}