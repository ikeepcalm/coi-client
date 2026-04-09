package dev.ua.ikeepcalm.coi.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
import dev.ua.ikeepcalm.coi.client.network.*;
import dev.ua.ikeepcalm.coi.client.screen.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.client.screen.EffectDebugScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircleOfImaginationClient implements ClientModInitializer {

    // Configurable maximum number of abilities (change this to support more abilities)
    public static final int MAX_ABILITIES = 6;

    private static final List<String> availableAbilities = new ArrayList<>();
    private static final Map<String, AbilityInfo> abilityInfoMap = new HashMap<>();

    private static String[] boundAbilities = new String[MAX_ABILITIES];

    public static KeyMapping[] abilityKeys = new KeyMapping[MAX_ABILITIES];
    public static KeyMapping abilityMenu;
    public static KeyMapping effectDebugMenu; // null when not in dev environment

    private static final boolean[] keyPressed = new boolean[MAX_ABILITIES + 2];

    @Override
    public void onInitializeClient() {
        boundAbilities = AbilityConfig.loadBindings();
        registerPayloads();
        registerKeybindings();
        registerTickHandler();
        AbilityHudOverlay.initialize();
        EffectManager.initialize();
    }

    private void registerPayloads() {
        // C2S (client → server = serverboundPlay)
        PayloadTypeRegistry.serverboundPlay().register(AbilityUsePayload.ID, AbilityUsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AbilityRequestPayload.ID, AbilityRequestPayload.CODEC);
        // S2C (server → client = clientboundPlay)
        PayloadTypeRegistry.clientboundPlay().register(AbilitiesPayload.ID, AbilitiesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownPayload.ID, CooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualEffectPayload.ID, VisualEffectPayload.CODEC);

        // S2C receivers
        ClientPlayNetworking.registerGlobalReceiver(AbilitiesPayload.ID,
                (payload, context) -> context.client().execute(() -> handleAbilityData(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.ID,
                (payload, context) -> context.client().execute(() -> handleCooldownData(payload.abilityId(), payload.ticks())));
        ClientPlayNetworking.registerGlobalReceiver(VisualEffectPayload.ID,
                (payload, context) -> context.client().execute(() -> EffectManager.trigger(payload.effectId(), payload.params())));
    }

    private void registerKeybindings() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("category.coi.abilities"));

        // Default keybindings for first 6 abilities: Z, X, C, V, B, N
        int[] defaultKeys = {
                GLFW.GLFW_KEY_Z,
                GLFW.GLFW_KEY_X,
                GLFW.GLFW_KEY_C,
                GLFW.GLFW_KEY_V,
                GLFW.GLFW_KEY_B,
                GLFW.GLFW_KEY_N
        };

        for (int i = 0; i < MAX_ABILITIES; i++) {
            int defaultKey = defaultKeys[i];
            abilityKeys[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.coi.ability" + (i + 1),
                    InputConstants.Type.KEYSYM,
                    defaultKey,
                    category
            ));
        }

        abilityMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "screen.coi.ability_binding",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                category
        ));

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            effectDebugMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "screen.coi.effect_debug",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F8,
                    category
            ));
        }
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            for (int i = 0; i < MAX_ABILITIES; i++) {
                handleKeyPress(i, abilityKeys[i], client);
            }

            handleKeyPress(MAX_ABILITIES, abilityMenu, client);
            if (effectDebugMenu != null) {
                handleKeyPress(MAX_ABILITIES + 1, effectDebugMenu, client);
            }
        });
    }

    private void handleKeyPress(int index, KeyMapping key, Minecraft client) {
        if (key.isDown() && !keyPressed[index]) {
            keyPressed[index] = true;

            if (index == MAX_ABILITIES) {
                Minecraft.getInstance().setScreen(new AbilityBindingScreen(null));
                return;
            }
            if (index == MAX_ABILITIES + 1) {
                Minecraft.getInstance().setScreen(new EffectDebugScreen(null));
                return;
            }

            if (boundAbilities[index] != null) {
                useAbility(boundAbilities[index]);
            }
        } else if (!key.isDown()) {
            keyPressed[index] = false;
        }
    }

    private static void useAbility(String abilityIdWithName) {
        if (abilityIdWithName == null) return;

        String abilityId = abilityIdWithName.contains(" - ") ? abilityIdWithName.split(" - ")[0] : abilityIdWithName;

        ClientPlayNetworking.send(new AbilityUsePayload(abilityId));

        AbilityInfo info = getAbilityInfo(abilityId);
        String displayName = info != null ? info.englishName() : AbilityInfo.extractDisplayName(abilityIdWithName);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.translatable("notification.coi.ability_used", displayName));
        }
    }

    public static void handleAbilityData(String data) {
        availableAbilities.clear();

        if (data.isEmpty()) {
            System.out.println("COI Client: Received empty ability data");
            return;
        }

        System.out.println("COI Client: Received ability data: " + data);

        abilityInfoMap.clear();
        String[] abilities = data.split(";");
        for (String ability : abilities) {
            if (!ability.isEmpty()) {
                String[] parts = ability.split("\\|");
                if (parts.length >= 2) {
                    String id = parts[0];
                    String localizedName = parts[1];
                    String englishName = parts.length > 2 ? parts[2] : localizedName;
                    String category = parts.length > 3 ? parts[3] : "uncategorized";

                    String formatted = id + " - " + englishName;
                    availableAbilities.add(formatted);
                    abilityInfoMap.put(id, new AbilityInfo(id, localizedName, englishName, category));
                    System.out.println("COI Client: Added ability: " + formatted);
                }
            }
        }

        System.out.println("COI Client: Total abilities loaded: " + availableAbilities.size());
        updateHudWithCurrentBindings();
    }

    public static void handleCooldownData(String abilityId, int cooldownTicks) {
        AbilityHudOverlay.setCooldown(abilityId, cooldownTicks);
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
            if (boundAbilities[i] == null) continue;

            String boundId = AbilityInfo.extractId(boundAbilities[i]);
            String freshEntry = availableAbilities.stream()
                    .filter(a -> a.startsWith(boundId + " - "))
                    .findFirst()
                    .orElse(null);

            if (freshEntry == null) {
                System.out.println("COI Client: Clearing invalid bound ability: " + boundAbilities[i]);
                boundAbilities[i] = null;
                needsSave = true;
            } else if (!freshEntry.equals(boundAbilities[i])) {
                boundAbilities[i] = freshEntry;
                needsSave = true;
            }
        }

        if (needsSave) {
            AbilityConfig.saveBindings(boundAbilities);
        }
    }

    public static List<String> getAvailableAbilities() {
        return new ArrayList<>(availableAbilities);
    }

    public static AbilityInfo getAbilityInfo(String abilityId) {
        return abilityInfoMap.get(abilityId);
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
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            System.out.println("COI Client: Requesting abilities from server...");
            ClientPlayNetworking.send(AbilityRequestPayload.INSTANCE);
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
