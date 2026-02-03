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
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CircleOfImaginationClient implements ClientModInitializer {

    // Configurable maximum number of abilities (change this to support more abilities)
    public static final int MAX_ABILITIES = 6;

    private static final String ABILITY_USE_PREFIX = "_0_0_2_2_r";

    private static final List<String> availableAbilities = new ArrayList<>();

    private static String[] boundAbilities = new String[MAX_ABILITIES];

    public static KeyBinding[] abilityKeys = new KeyBinding[MAX_ABILITIES];
    public static KeyBinding abilityMenu;

    private static final boolean[] keyPressed = new boolean[MAX_ABILITIES + 1];

    // Chat message queue to prevent spam
    private static final Queue<String> messageQueue = new LinkedList<>();
    private static long lastMessageTime = 0;
    private static final long MESSAGE_DELAY_MS = 500; // 500ms delay between messages

    @Override
    public void onInitializeClient() {
        boundAbilities = AbilityConfig.loadBindings();
        registerKeybindings();
        registerTickHandler();
        AbilityHudOverlay.initialize();
    }

    private void registerKeybindings() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("category.coi.abilities"));

        // Default keybindings for first 6 abilities: Z, X, C, V, B, N
        int[] defaultKeys = {
            GLFW.GLFW_KEY_Z,
            GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_V,
            GLFW.GLFW_KEY_B,
            GLFW.GLFW_KEY_N
        };

        // Register ability keybindings dynamically
        for (int i = 0; i < MAX_ABILITIES; i++) {
            int defaultKey = i < defaultKeys.length ? defaultKeys[i] : InputUtil.UNKNOWN_KEY.getCode();
            abilityKeys[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.coi.ability" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    defaultKey,
                    category
            ));
        }

        abilityMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "screen.coi.ability_binding",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                category
        ));
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle all ability keys dynamically
            for (int i = 0; i < MAX_ABILITIES; i++) {
                handleKeyPress(i, abilityKeys[i], client);
            }

            // Handle menu key
            handleKeyPress(MAX_ABILITIES, abilityMenu, client);

            // Process message queue with delay
            processMessageQueue(client);
        });
    }

    private void handleKeyPress(int index, KeyBinding key, MinecraftClient client) {
        if (key.isPressed() && !keyPressed[index]) {
            keyPressed[index] = true;

            // Menu key
            if (index == MAX_ABILITIES) {
                MinecraftClient.getInstance().setScreen(new AbilityBindingScreen(null));
                return;
            }

            // Ability key
            if (index < MAX_ABILITIES && boundAbilities[index] != null) {
                queueAbilityUse(boundAbilities[index], index);
            }
        } else if (!key.isPressed()) {
            keyPressed[index] = false;
        }
    }

    private void queueAbilityUse(String abilityIdWithName, int slot) {
        if (abilityIdWithName != null) {
            String abilityId = abilityIdWithName;
            if (abilityIdWithName.contains(" - ")) {
                abilityId = abilityIdWithName.split(" - ")[0];
            }
            messageQueue.offer(ABILITY_USE_PREFIX + "USE:" + abilityId + "|" + abilityIdWithName);
        }
    }

    private static void processMessageQueue(MinecraftClient client) {
        if (client.player == null || messageQueue.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime >= MESSAGE_DELAY_MS) {
            String message = messageQueue.poll();
            if (message != null) {
                String[] parts = message.split("\\|", 2);
                String chatMessage = parts[0];
                String abilityName = parts.length > 1 ? parts[1] : "";

                client.player.networkHandler.sendChatMessage(chatMessage);
                lastMessageTime = currentTime;

                // Show notification
                if (!abilityName.isEmpty()) {
                    String displayName = abilityName.contains(" - ") ?
                        abilityName.split(" - ")[1] : abilityName;
                    client.player.sendMessage(Text.translatable("notification.coi.ability_used",
                        displayName), true);
                }
            }
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

        for (int i = 0; i < MAX_ABILITIES; i++) {
            AbilityHudOverlay.updateAbilitySlot(i, boundAbilities[i]);
        }
    }

    private static void validateBoundAbilities() {
        boolean needsSave = false;

        for (int i = 0; i < MAX_ABILITIES; i++) {
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
        if (slot >= 0 && slot < MAX_ABILITIES) {
            boundAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities);
            AbilityHudOverlay.updateAbilitySlot(slot, abilityId);
        }
    }

    public static int getMaxAbilities() {
        return MAX_ABILITIES;
    }

    public static void requestAbilitiesFromServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            System.out.println("COI Client: Requesting abilities from server...");
            messageQueue.offer(ABILITY_USE_PREFIX + "REQUEST|");
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