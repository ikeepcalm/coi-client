package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.HallucinationManager;
import dev.ua.ikeepcalm.coi.client.gesture.GestureScreen;
import dev.ua.ikeepcalm.coi.client.gesture.GestureType;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ActingHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ActionBarHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.client.hud.CogitationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.MadnessHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.NotificationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ResourceHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.SpiritualityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.TargetHealthOverlay;
import dev.ua.ikeepcalm.coi.client.mcf.CoiModelLayers;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.menu.ClientMenuState;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.menu.MenuParser;
import dev.ua.ikeepcalm.coi.client.network.*;
import dev.ua.ikeepcalm.coi.client.presence.DiscordPresenceManager;
import dev.ua.ikeepcalm.coi.client.resources.IngredientInfo;
import dev.ua.ikeepcalm.coi.client.resources.ResourceLoader;
import dev.ua.ikeepcalm.coi.client.screen.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.client.screen.AbilityWheelScreen;
import dev.ua.ikeepcalm.coi.client.screen.CharacterSheetScreen;
import dev.ua.ikeepcalm.coi.client.screen.EffectDebugScreen;
import dev.ua.ikeepcalm.coi.client.screen.InventoryHint;
import dev.ua.ikeepcalm.coi.client.screen.TourScreen;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.util.IconModels;
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
    /**
     * The pathway's emblem glyph from the {@code pathway_icons} font, or null
     * when that pathway has none. Shared by the tooltip decorators and the
     * character sheet header.
     */
    public static Component pathwayEmblem(String pathway) {
        String icon = PATHWAY_ICONS.get(AbilityInfo.normalizePathway(pathway));
        if (icon == null) return null;
        return Component.literal(icon).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(PATHWAY_ICONS_FONT)));
    }

    private static final boolean[] keyPressed = new boolean[MAX_ABILITIES + 3];
    public static KeyMapping[] abilityKeys = new KeyMapping[MAX_ABILITIES];
    public static KeyMapping abilityMenu;
    public static KeyMapping abilityWheel;
    public static KeyMapping openMenu;
    public static KeyMapping gestureCast;
    public static KeyMapping effectDebugMenu; // null when not in dev environment
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
                    abilityInfoMap.put(id, AbilityInfo.of(id, localizedName, englishName, category, hasLeftClick));
                    System.out.println("COI Client: Added ability: " + formatted);
                }
            }
        }

        System.out.println("COI Client: Total abilities loaded: " + availableAbilities.size());
        updateHudWithCurrentBindings();
        scheduleTourIfFirstList();
    }

    /**
     * Protocol-2 ability list: same two collections as {@link #handleAbilityData},
     * plus the metadata the delimited format had no room for. Cooldowns and
     * toggle state are applied after the slots have been rebound, so a slot
     * that was already showing this ability picks them up too.
     */
    public static void handleAbilityDataV2(String json) {
        availableAbilities.clear();
        abilityInfoMap.clear();

        if (json == null || json.isBlank()) {
            System.out.println("COI Client: Received empty ability data (v2)");
            return;
        }

        Map<String, Integer> cooldowns = new HashMap<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("abilities")) return;
            for (JsonElement element : root.getAsJsonArray("abilities")) {
                JsonObject entry = element.getAsJsonObject();
                AbilityInfo info = parseAbility(entry);
                if (info == null) continue;
                storeAbility(info);
                int remaining = num(entry, "cooldownRemainingTicks");
                if (remaining > 0) cooldowns.put(info.abilityId(), remaining);
            }
        } catch (Exception e) {
            System.err.println("COI Client: malformed abilities_v2 payload: " + e.getMessage());
            return;
        }

        System.out.println("COI Client: Total abilities loaded (v2): " + availableAbilities.size());
        updateHudWithCurrentBindings();
        applyServerAbilityState(cooldowns);
        scheduleTourIfFirstList();
    }

    private static AbilityInfo parseAbility(JsonObject json) {
        String id = str(json, "id", "");
        if (id.isEmpty()) return null;
        String localizedName = str(json, "name", id);
        int sequence = json.has("sequence") ? json.get("sequence").getAsInt() : AbilityInfo.sequenceOf(id);
        String pathway = str(json, "pathway", AbilityInfo.pathwayOf(id));
        return new AbilityInfo(id, localizedName, str(json, "englishName", localizedName),
                str(json, "category", "uncategorized"), bool(json, "hasLeftClick"),
                str(json, "kind", AbilityInfo.KIND_ACTIVE), str(json, "description", ""),
                num(json, "cost"), json.has("drainPerSecond") ? json.get("drainPerSecond").getAsDouble() : 0.0,
                num(json, "cooldownSeconds"), AbilityInfo.normalizePathway(pathway), sequence,
                bool(json, "active"), bool(json, "locked"), bool(json, "blocked"),
                str(json, "blockedBy", ""), str(json, "icon", ""));
    }

    private static void storeAbility(AbilityInfo info) {
        if (info == null) return;
        availableAbilities.add(AbilityInfo.formatStored(info.abilityId(), info.englishName(), AbilityInfo.ACTION_EXECUTE));
        if (info.hasLeftClick()) {
            availableAbilities.add(AbilityInfo.formatStored(info.abilityId(),
                    info.englishName() + " (Left Click)", AbilityInfo.ACTION_LEFT_CLICK));
        }
        abilityInfoMap.put(info.abilityId(), info);
    }

    /**
     * Pushes toggle state and the cooldowns that were already running when the
     * list arrived onto the slots.
     */
    private static void applyServerAbilityState(Map<String, Integer> cooldowns) {
        for (AbilityInfo info : abilityInfoMap.values()) {
            AbilityHudOverlay.setActive(info.abilityId(), info.active());
            Integer remaining = cooldowns.get(info.abilityId());
            if (remaining != null) {
                AbilityHudOverlay.setCooldown(info.abilityId(), remaining,
                        Math.max(remaining, info.cooldownSeconds() * 20));
            }
        }
    }

    /**
     * Single-ability update from {@code coi-client:state}: either a toggle or
     * a category switch.
     */
    public static void handleAbilityState(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String id = str(root, "id", "");
            if (id.isEmpty()) return;
            if (root.has("active")) {
                AbilityHudOverlay.setActive(id, root.get("active").getAsBoolean());
            }
            if (root.has("category") || root.has("categoryName")) {
                AbilityHudOverlay.setCategoryLabel(id, str(root, "categoryName", str(root, "category", "")));
            }
        } catch (Exception e) {
            System.err.println("COI Client: malformed ability state payload: " + json);
        }
    }

    private static String str(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static int num(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : 0;
    }

    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
    }

    private static void scheduleTourIfFirstList() {
        if (!availableAbilities.isEmpty() && ClientStateStore.isTourNotCompleted() && tourPendingAt == 0) {
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

    /**
     * Capability handshake — always first, so the server knows which surfaces
     * this client can render before it starts feeding any of them.
     */
    public static void sendHello() {
        if (Minecraft.getInstance().player == null) return;
        ClientPlayNetworking.send(new HelloPayload(ClientFeatures.helloJson()));
    }

    /**
     * Asks the server to open the Beyonder menu that used to live on the
     * slot-9 shortcut item. Servers that never advertised {@code menu_action}
     * have nothing listening, so say so instead of sending into the void.
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

    @Override
    public void onInitializeClient() {
        HudConfig.load();
        ClientStateStore.load();
        boundAbilities = AbilityConfig.loadBindings();
        wheelAbilities = AbilityConfig.loadWheelBindings();
        gestureAbilities = AbilityConfig.loadGestureBindings();
        registerPayloads();
        registerKeybindings();
        registerTickHandler();
        AbilityHudOverlay.initialize();
        // Replaces the vanilla hearts rather than attaching beside them, so it
        // has to be registered like every other element but reads differently
        BeyonderHealthOverlay.initialize();
        CharacterPlateOverlay.initialize();
        MadnessHudOverlay.initialize();
        SpiritualityHudOverlay.initialize();
        ActingHudOverlay.initialize();
        ResourceHudOverlay.initialize();
        ActionBarHudOverlay.initialize();
        TargetHealthOverlay.initialize();
        CogitationOverlay.initialize();
        NotificationOverlay.initialize();
        EffectManager.initialize();
        // Explains the hotbar slot the plugin empties for modded clients
        InventoryHint.register();
        CoiModelLayers.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sendHello();
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
            ClientActingState.reset();
            ClientResourceState.reset();
            ClientActionBarState.reset();
            ClientTargetState.reset();
            ClientCogitationState.reset();
            ClientNotificationState.reset();
            ClientSheetState.reset();
            ClientMenuState.reset();
            ClientAppearanceState.reset();
            ServerCapabilities.reset();
            SpiritualityHudOverlay.reset();
            BeyonderHealthOverlay.reset();
            // The next server may key its ability icons against a different pack
            IconModels.clearCache();
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
        PayloadTypeRegistry.serverboundPlay().register(HelloPayload.ID, HelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MenuActionPayload.ID, MenuActionPayload.CODEC);
        // S2C (server → client = clientboundPlay)
        PayloadTypeRegistry.clientboundPlay().register(AbilitiesPayload.ID, AbilitiesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownPayload.ID, CooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualEffectPayload.ID, VisualEffectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MythicalFormPayload.ID, MythicalFormPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConditionsPayload.ID, ConditionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AppearancePayload.ID, AppearancePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerInfoPayload.ID, ServerInfoPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AbilitiesV2Payload.ID, AbilitiesV2Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AbilityStatePayload.ID, AbilityStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActingPayload.ID, ActingPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ResourcePayload.ID, ResourcePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActionBarPayload.ID, ActionBarPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TargetHealthPayload.ID, TargetHealthPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CogitationPayload.ID, CogitationPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NotifyPayload.ID, NotifyPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SheetPayload.ID, SheetPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MenuPayload.ID, MenuPayload.CODEC);

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
        ClientPlayNetworking.registerGlobalReceiver(ServerInfoPayload.ID,
                (payload, context) -> context.client().execute(() -> ServerCapabilities.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(AbilitiesV2Payload.ID,
                (payload, context) -> context.client().execute(() -> handleAbilityDataV2(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(AbilityStatePayload.ID,
                (payload, context) -> context.client().execute(() -> handleAbilityState(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ActingPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientActingState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ResourcePayload.ID,
                (payload, context) -> context.client().execute(() -> ClientResourceState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ActionBarPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientActionBarState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(TargetHealthPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientTargetState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(CogitationPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientCogitationState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(NotifyPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientNotificationState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(SheetPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientSheetState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(MenuPayload.ID,
                (payload, context) -> context.client().execute(() -> handleMenu(payload.json())));
    }

    /**
     * A menu document: open the screen, refresh the open one, or take it away.
     * <p>
     * Refreshing is just adopting the document — {@link MenuScreen} re-reads
     * {@link ClientMenuState} and rebuilds itself — so a push that lands while
     * a different screen is open still replaces that screen, which is what the
     * server asked for by sending it.
     */
    private static void handleMenu(String json) {
        MenuDocument document = MenuParser.parse(json);
        if (document == null) return;
        Minecraft client = Minecraft.getInstance();
        if (document.closed()) {
            // A close carrying "back" is the back arrow at the root of the
            // server's stack: there is nothing underneath on its side, but the
            // character sheet may be what the player came from on ours.
            boolean toSheet = readBack(json)
                    && ClientMenuState.openedFromSheet()
                    && ServerCapabilities.has("character_sheet");
            ClientMenuState.clear();
            // The server already knows the menu is gone; echoing __close back
            // would only race its next open
            if (client.gui.screen() instanceof MenuScreen menu) menu.closeQuietly();
            if (toSheet) client.gui.setScreen(new CharacterSheetScreen(null));
            return;
        }
        ClientMenuState.adopt(document);
        if (!(client.gui.screen() instanceof MenuScreen)) {
            client.gui.setScreen(new MenuScreen(null));
        }
    }

    /**
     * The {@code back} flag off the raw document, read here rather than in
     * {@link MenuParser} because it says something about the *transition*, not
     * about the screen — a closed document has no screen to describe.
     * <p>
     * Defensive like everything else on this channel: absent, null or any
     * non-boolean reads as false, so an older plugin (or a newer one that
     * repurposes the name) simply closes the menu as it always did.
     */
    private static boolean readBack(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return false;
            JsonElement back = root.getAsJsonObject().get("back");
            return back != null && back.isJsonPrimitive()
                    && back.getAsJsonPrimitive().isBoolean() && back.getAsBoolean();
        } catch (Exception e) {
            return false;
        }
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

        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.coi.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
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
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HallucinationManager.tick(client);
            DiscordPresenceManager.tick();

            if (client.player == null) return;

            // First-join tour: opens once the delay has passed and no other
            // screen is in the way (stays pending until the way is clear)
            if (tourPendingAt > 0 && System.currentTimeMillis() >= tourPendingAt && client.gui.screen() == null) {
                tourPendingAt = 0;
                if (ClientStateStore.isTourNotCompleted()) {
                    client.gui.setScreen(new TourScreen());
                }
            }

            for (int i = 0; i < getActiveAbilitySlots(); i++) {
                handleKeyPress(i, abilityKeys[i], client);
            }

            handleKeyPress(MAX_ABILITIES, abilityMenu, client);
            handleKeyPress(MAX_ABILITIES + 2, openMenu, client);
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
            if (index == MAX_ABILITIES + 2) {
                openServerMenu(client);
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
