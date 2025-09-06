package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
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
        AbilityHudOverlay.initialize();
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
                sendAbilityUse(boundAbilities[index], client, index);
            }
        } else if (!key.isPressed()) {
            keyPressed[index] = false;
        }
    }

    private void sendAbilityUse(String abilityIdWithName, MinecraftClient client, int slot) {
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

        if (data.isEmpty()) {
            System.out.println("COI Client: Received empty ability data");
            return;
        }

        System.out.println("COI Client: Received ability data: " + data);

        String[] abilities = data.split(";");
        for (String ability : abilities) {
            if (!ability.isEmpty()) {
                String[] parts = ability.split("\\|");
                if (parts.length == 2) {
                    String formatted = parts[0] + " - " + parts[1];
                    availableAbilities.add(formatted);
                    System.out.println("COI Client: Added ability: " + formatted);
                }
            }
        }

        System.out.println("COI Client: Total abilities loaded: " + availableAbilities.size());
        updateHudWithCurrentBindings();
    }

    public static void handleCooldownData(String data) {
        if (data.isEmpty()) return;

        String[] parts = data.split(":");
        if (parts.length == 2) {
            String abilityId = parts[0];
            try {
                int cooldownTicks = Integer.parseInt(parts[1]);
                AbilityHudOverlay.setCooldown(abilityId, cooldownTicks);
            } catch (NumberFormatException e) {
                System.err.println("Invalid cooldown data: " + data);
            }
        }
    }

    private static void updateHudWithCurrentBindings() {
        validateBoundAbilities();
        
        for (int i = 0; i < 3; i++) {
            AbilityHudOverlay.updateAbilitySlot(i, boundAbilities[i]);
        }
    }
    
    private static void validateBoundAbilities() {
        boolean needsSave = false;
        
        for (int i = 0; i < 3; i++) {
            if (boundAbilities[i] != null) {
                boolean isStillAvailable = availableAbilities.contains(boundAbilities[i]);
                
                if (!isStillAvailable) {
                    System.out.println("COI Client: Clearing invalid bound ability: " + boundAbilities[i]);
                    boundAbilities[i] = null;
                    needsSave = true;
                }
            }
        }
        
        if (needsSave) {
            AbilityConfig.saveBindings(boundAbilities);
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
            AbilityHudOverlay.updateAbilitySlot(slot, abilityId);
        }
    }
    
    public static void requestAbilitiesFromServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            System.out.println("COI Client: Requesting abilities from server...");
            client.player.networkHandler.sendChatMessage(ABILITY_USE_PREFIX + "REQUEST");
        }
    }

    public static void addTestAbilities() {
        if (availableAbilities.isEmpty()) {
            System.out.println("COI Client: Adding test abilities for debugging...");
            availableAbilities.add("fireball - Fireball");
            availableAbilities.add("heal - Healing Light");
            availableAbilities.add("teleport - Teleportation");
            availableAbilities.add("shield - Magic Shield");
            System.out.println("COI Client: Added " + availableAbilities.size() + " test abilities");
        }
    }
}