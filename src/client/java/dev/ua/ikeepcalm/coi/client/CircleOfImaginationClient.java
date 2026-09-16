package dev.ua.ikeepcalm.coi.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ua.ikeepcalm.coi.client.appearance.UniquenessParticleManager;
import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.HallucinationManager;
import dev.ua.ikeepcalm.coi.client.gesture.GestureScreen;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.MadnessHudOverlay;
import dev.ua.ikeepcalm.coi.client.mcf.CoiModelLayers;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.network.*;
import dev.ua.ikeepcalm.coi.client.presence.DiscordPresenceManager;
import dev.ua.ikeepcalm.coi.client.resources.IngredientInfo;
import dev.ua.ikeepcalm.coi.client.resources.ResourceLoader;
import dev.ua.ikeepcalm.coi.client.screen.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.client.screen.AbilityWheelScreen;
import dev.ua.ikeepcalm.coi.client.screen.EffectDebugScreen;
import dev.ua.ikeepcalm.coi.client.screen.TourScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class CircleOfImaginationClient implements ClientModInitializer {

    // Hard ceiling for key-bound ability slots. Keymappings can only be registered
    // once at init, so all MAX_ABILITIES are registered up front and the player-facing
    // count is HudConfig.activeAbilitySlots (see getActiveAbilitySlots()).
    public static final int MAX_ABILITIES = 10;
    public static final int MAX_WHEEL_SIZE = 16;

    private static final List<String> availableAbilities = new ArrayList<>();
    private static final Map<String, AbilityInfo> abilityInfoMap = new HashMap<>();
    private static final ResourceLoader CLIENT_DATA_LOADER = new ResourceLoader();

    private static final Identifier PATHWAY_ICONS_FONT = Identifier.fromNamespaceAndPath("coi-client", "pathway_icons");
    private static final Map<String, String> PATHWAY_ICONS = Map.ofEntries(
            Map.entry("abyss", ""),
            Map.entry("aeon", ""),
            Map.entry("chained", ""),
            Map.entry("darkness", ""),
            Map.entry("death", ""),
            Map.entry("demoness", ""),
            Map.entry("door", ""),
            Map.entry("emperor", ""),
            Map.entry("error", ""),
            Map.entry("fool", ""),
            Map.entry("fortune", ""),
            Map.entry("giant", ""),
            Map.entry("hanged", ""),
            Map.entry("hermit", ""),
            Map.entry("justiciar", ""),
            Map.entry("moon", ""),
            Map.entry("mother", ""),
            Map.entry("paragon", ""),
            Map.entry("patriarch", ""),
            Map.entry("priest", ""),
            Map.entry("sublunary", ""),
            Map.entry("sun", ""),
            Map.entry("tower", ""),
            Map.entry("tyrant", ""),
            Map.entry("visionary", "")
    );
    private static final boolean[] keyPressed = new boolean[MAX_ABILITIES + 3];
    public static KeyMapping[] abilityKeys = new KeyMapping[MAX_ABILITIES];
    public static KeyMapping abilityMenu;
    public static KeyMapping abilityWheel;
    public static KeyMapping gestureCast;
    public static KeyMapping effectDebugMenu; // null outside development environments
    private static String[] boundAbilities = new String[MAX_ABILITIES];
    private static String[] wheelAbilities = new String[MAX_WHEEL_SIZE];
    private static String[] gestureAbilities = new String[GestureType.values().length];
    // When > 0, the first-join tour opens at this timestamp (set on the first
    // non-empty abilities payload; the delay lets the world render first)
    private static long tourPendingAt = 0;

    private static void useAbility(String abilityIdWithName) {
        if (abilityIdWithName == null) return;

        String abilityId = AbilityInfo.extractId(abilityIdWithName);
        String action = AbilityInfo.extractAction(abilityIdWithName);

        ClientPlayNetworking.send(new AbilityUsePayload(abilityId, action));
        AbilityHudOverlay.onAbilityCast(abilityId);

        AbilityInfo info = getAbilityInfo(abilityId);
        String displayName = info != null ? info.englishName() : AbilityInfo.extractDisplayName(abilityIdWithName);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && AbilityInfo.ACTION_EXECUTE.equals(action)) {
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
                    boolean hasLeftClick = parts.length >= 5 && Boolean.parseBoolean(parts[4]);

                    String formatted = AbilityInfo.formatStored(id, englishName, AbilityInfo.ACTION_EXECUTE);
                    availableAbilities.add(formatted);
                    if (hasLeftClick) {
                        availableAbilities.add(AbilityInfo.formatStored(id, englishName + " (Left Click)", AbilityInfo.ACTION_LEFT_CLICK));
                    }
                    abilityInfoMap.put(id, new AbilityInfo(id, localizedName, englishName, category, hasLeftClick));
                    System.out.println("COI Client: Added ability: " + formatted);
                }
            }
        }

        System.out.println("COI Client: Total abilities loaded: " + availableAbilities.size());
        updateHudWithCurrentBindings();

        if (!availableAbilities.isEmpty() && !ClientStateStore.isTourCompleted() && tourPendingAt == 0) {
            tourPendingAt = System.currentTimeMillis() + 3000;
        }
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
        return availableAbilities.stream()
                .filter(a -> Objects.equals(AbilityInfo.extractId(a), boundId))
                .filter(a -> Objects.equals(AbilityInfo.extractAction(a), boundAction))
                .findFirst()
                .orElse(null);
    }

    public static List<String> getAvailableAbilities() {
        return new ArrayList<>(availableAbilities);
    }

    public static AbilityInfo getAbilityInfo(String abilityId) {
        return abilityInfoMap.get(abilityId);
    }

    public static boolean hasLeftClick(String abilityIdWithName) {
        AbilityInfo info = getAbilityInfo(AbilityInfo.extractId(abilityIdWithName));
        return info != null && info.hasLeftClick();
    }

    public static String getBoundAbility(int slot) {
        return boundAbilities[slot];
    }

    public static void setBoundAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < MAX_ABILITIES) {
            boundAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities, wheelAbilities, gestureAbilities);
            AbilityHudOverlay.updateAbilitySlot(slot, abilityId);
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

    public static boolean isKeyDown(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        Minecraft client = Minecraft.getInstance();

        long window = client.getWindow().handle();
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(keyBinding);

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) != GLFW.GLFW_RELEASE;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) != GLFW.GLFW_RELEASE;
        }

        return false;
    }

    public static void useAbilityById(String abilityIdWithName) {
        useAbility(abilityIdWithName);
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

    private static String formatPathwayName(String pathway) {
        return Character.toUpperCase(pathway.charAt(0)) + pathway.substring(1);
    }

    public static void requestAbilitiesFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            System.out.println("COI Client: Requesting abilities from server...");
            ClientPlayNetworking.send(AbilityRequestPayload.INSTANCE);
        }
    }

    @Override
    public void onInitializeClient() {
        HudConfig.load();
        AppearanceConfig.load();
        ClientStateStore.load();
        boundAbilities = AbilityConfig.loadBindings();
        wheelAbilities = AbilityConfig.loadWheelBindings();
        gestureAbilities = AbilityConfig.loadGestureBindings();
        registerPayloads();
        registerKeybindings();
        registerTickHandler();
        AbilityHudOverlay.initialize();
        MadnessHudOverlay.initialize();
        EffectManager.initialize();
        CoiModelLayers.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            requestAbilitiesFromServer();
            var server = client.getCurrentServer();
            DiscordPresenceManager.onServerJoin(
                    server != null ? server.name : "Singleplayer",
                    server != null ? server.ip : null
            );
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Remember how mad we were — the title screen holds a grudge
            ClientStateStore.setLastMadness(ClientBeyonderState.getMadness());
            ClientBeyonderState.reset();
            ClientAppearanceState.reset();
            UniquenessParticleManager.reset();
            EffectManager.stopAll();
            MythicalFormManager.clearAll();
            tourPendingAt = 0;
            DiscordPresenceManager.onDisconnect();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());

        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(Identifier.fromNamespaceAndPath("coi-client", "client_json_loader"), CLIENT_DATA_LOADER);

        ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return;

            CompoundTag tag = customData.copyTag();
            if (!tag.contains("PublicBukkitValues")) return;

            Optional<CompoundTag> values = tag.getCompound("PublicBukkitValues");
            if (values.isEmpty()) return;

            CompoundTag pdc = values.get();

            if (pdc.contains("circleofimagination:ingredient")) {
                Optional<String> ingredientId = pdc.getString("circleofimagination:ingredient");
                if (ingredientId.isEmpty()) return;

                IngredientInfo info = CLIENT_DATA_LOADER.getIngredient(ingredientId.get());
                if (info == null) return;

                String icon = PATHWAY_ICONS.get(info.pathway());
                if (icon != null && !lines.isEmpty()) {
                    Component name = lines.getFirst();
                    lines.set(0, Component.empty()
                            .append(Component.literal(icon).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(PATHWAY_ICONS_FONT))))
                            .append(Component.literal(" "))
                            .append(name));
                }

                lines.add(Component.literal(info.isMain() ? "Main ingredient" : "Supplementary ingredient").withStyle(info.color()));
                lines.add(Component.literal("Sequence " + info.sequence() + " of the " + formatPathwayName(info.pathway()) + " pathway").withStyle(info.color()));

            } else if (pdc.contains("venturetothesubspace:loot_shard_id")) {
                Optional<String> shardIdOpt = pdc.getString("venturetothesubspace:loot_shard_id");
                if (shardIdOpt.isEmpty()) return;

                // format: "coi:ingredients-{pathway}-{sequence}", e.g. "coi:ingredients-demoness-9"
                String path = shardIdOpt.get().contains(":") ? shardIdOpt.get().split(":", 2)[1] : shardIdOpt.get();
                String[] parts = path.split("-", 3);
                if (parts.length < 3) return;

                String pathway = parts[1];
                int sequence;
                try {
                    sequence = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return;
                }

                ChatFormatting color = CLIENT_DATA_LOADER.getPathwayColor(pathway);

                String icon = PATHWAY_ICONS.get(pathway);
                if (icon != null && !lines.isEmpty()) {
                    Component name = lines.getFirst();
                    lines.set(0, Component.empty()
                            .append(Component.literal(icon).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(PATHWAY_ICONS_FONT))))
                            .append(Component.literal(" "))
                            .append(name));
                }

                lines.add(Component.literal("Sequence " + sequence + " of the " + formatPathwayName(pathway) + " pathway").withStyle(color));
            }
        });

    }

    private void registerPayloads() {
        // C2S (client → server = serverboundPlay)
        PayloadTypeRegistry.serverboundPlay().register(AbilityUsePayload.ID, AbilityUsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AbilityRequestPayload.ID, AbilityRequestPayload.CODEC);
        // S2C (server → client = clientboundPlay)
        PayloadTypeRegistry.clientboundPlay().register(AbilitiesPayload.ID, AbilitiesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownPayload.ID, CooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualEffectPayload.ID, VisualEffectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MythicalFormPayload.ID, MythicalFormPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConditionsPayload.ID, ConditionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AppearancePayload.ID, AppearancePayload.CODEC);

        // S2C receivers
        ClientPlayNetworking.registerGlobalReceiver(AbilitiesPayload.ID,
                (payload, context) -> context.client().execute(() -> handleAbilityData(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.ID,
                (payload, context) -> context.client().execute(() -> handleCooldownData(payload.abilityId(), payload.ticks())));
        ClientPlayNetworking.registerGlobalReceiver(VisualEffectPayload.ID,
                (payload, context) -> context.client().execute(() -> EffectManager.trigger(payload.effectId(), payload.params())));
        ClientPlayNetworking.registerGlobalReceiver(MythicalFormPayload.ID,
                (payload, context) -> context.client().execute(() -> MythicalFormManager.handlePacket(payload.targetUuid(), payload.params())));
        ClientPlayNetworking.registerGlobalReceiver(ConditionsPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientBeyonderState.parseAndUpdate(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(AppearancePayload.ID,
                (payload, context) -> context.client().execute(() -> ClientAppearanceState.handlePacket(payload.targetUuid(), payload.traits())));
    }

    private void registerKeybindings() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("category.coi.abilities"));

        // Default keybindings for first 6 abilities: Z, X, C, V, B, N.
        // Slots 7+ default unbound — the player assigns keys in vanilla Controls.
        int[] defaultKeys = {
                GLFW.GLFW_KEY_Z,
                GLFW.GLFW_KEY_X,
                GLFW.GLFW_KEY_C,
                GLFW.GLFW_KEY_V,
                GLFW.GLFW_KEY_B,
                GLFW.GLFW_KEY_N
        };

        for (int i = 0; i < MAX_ABILITIES; i++) {
            int defaultKey = i < defaultKeys.length ? defaultKeys[i] : GLFW.GLFW_KEY_UNKNOWN;
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

        abilityWheel = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.coi.ability_wheel",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));

        gestureCast = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.coi.gesture",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
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
        UniquenessParticleManager.initializeWorldRenderer();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HallucinationManager.tick(client);
            UniquenessParticleManager.tick(client);
            DiscordPresenceManager.tick();

            if (client.player == null) return;

            // First-join tour: opens once the delay has passed and no other
            // screen is in the way (stays pending until the way is clear)
            if (tourPendingAt > 0 && System.currentTimeMillis() >= tourPendingAt && client.gui.screen() == null) {
                tourPendingAt = 0;
                if (!ClientStateStore.isTourCompleted()) {
                    client.gui.setScreen(new TourScreen());
                }
            }

            for (int i = 0; i < getActiveAbilitySlots(); i++) {
                handleKeyPress(i, abilityKeys[i], client);
            }

            handleKeyPress(MAX_ABILITIES, abilityMenu, client);
            if (effectDebugMenu != null) {
                handleKeyPress(MAX_ABILITIES + 1, effectDebugMenu, client);
            }

            // Enhanced Ability Wheel trigger logic
            if (abilityWheel.isDown()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new AbilityWheelScreen());
                }
            }

            // Gesture casting: inert until at least one gesture has an ability bound
            if (gestureCast.isDown() && client.gui.screen() == null && hasAnyGestureBound()) {
                client.gui.setScreen(new GestureScreen());
            }
        });
    }

    private void handleKeyPress(int index, KeyMapping key, Minecraft client) {
        if (key.isDown() && !keyPressed[index]) {
            keyPressed[index] = true;

            if (index == MAX_ABILITIES) {
                Minecraft.getInstance().gui.setScreen(new AbilityBindingScreen(null));
                return;
            }
            if (index == MAX_ABILITIES + 1) {
                Minecraft.getInstance().gui.setScreen(new EffectDebugScreen(null));
                return;
            }

            if (boundAbilities[index] != null) {
                useAbility(boundAbilities[index]);
            }
        } else if (!key.isDown()) {
            keyPressed[index] = false;
        }
    }
}
