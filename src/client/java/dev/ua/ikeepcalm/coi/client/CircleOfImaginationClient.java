package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.packet.AbilityListPayload;
import dev.ua.ikeepcalm.coi.client.packet.AbilityRequestPayload;
import dev.ua.ikeepcalm.coi.client.packet.AbilityUsePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CircleOfImaginationClient implements ClientModInitializer {

    public static final String MOD_ID = "coi";

    private static final List<String> availableAbilities = new ArrayList<>();
    private static String[] boundAbilities = new String[3];

    private static KeyBinding ability1Key;
    private static KeyBinding ability2Key;
    private static KeyBinding ability3Key;

    private static final boolean[] keyPressed = new boolean[3];

    @Override
    public void onInitializeClient() {
        boundAbilities = AbilityConfig.loadBindings();
        registerPayloads();
        registerKeybindings();
        registerNetworking();
        registerTickHandler();
    }

    private void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(AbilityUsePayload.ID, AbilityUsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityRequestPayload.ID, AbilityRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityListPayload.ID, AbilityListPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(AbilityListPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                System.out.println("[COI DEBUG] Received specific packet: " + payload.getId() + ", typed payload: " + payload);
            });
        });
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
    }

    private void registerNetworking() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("[COI DEBUG] Sending AbilityRequestPayload");
            ClientPlayNetworking.send(new AbilityRequestPayload("request_abilities"));
        });

        ClientPlayNetworking.registerGlobalReceiver(AbilityListPayload.ID, (payload, context) -> {
            System.out.println("[COI DEBUG] Received AbilityListPayload: " + payload.abilities());
            context.client().execute(() -> {
                availableAbilities.clear();
                availableAbilities.addAll(payload.abilities());
            });
        });
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            handleKeyPress(0, ability1Key);
            handleKeyPress(1, ability2Key);
            handleKeyPress(2, ability3Key);
        });
    }

    private void handleKeyPress(int index, KeyBinding key) {
        if (key.isPressed() && !keyPressed[index]) {
            keyPressed[index] = true;
            if (boundAbilities[index] != null) {
                sendAbilityUse(boundAbilities[index]);
            }
        } else if (!key.isPressed()) {
            keyPressed[index] = false;
        }
    }

    private void sendAbilityUse(String abilityId) {
        System.out.println("[COI DEBUG] Sending AbilityUsePayload for ability: " + abilityId);
        ClientPlayNetworking.send(new AbilityUsePayload(abilityId));
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