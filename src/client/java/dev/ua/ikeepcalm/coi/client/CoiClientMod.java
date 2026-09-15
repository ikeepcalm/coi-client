package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.data.ClientDataLoader;
import dev.ua.ikeepcalm.coi.client.effect.EffectManager;
import dev.ua.ikeepcalm.coi.client.effect.HallucinationManager;
import dev.ua.ikeepcalm.coi.client.form.FormModelLayers;
import dev.ua.ikeepcalm.coi.client.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ActingOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ActionBarOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.CogitationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.MadnessOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.NotificationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ResourceOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.SpiritualityOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.TargetHealthOverlay;
import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.client.network.CoiNetworking;
import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.client.presence.DiscordPresenceManager;
import dev.ua.ikeepcalm.coi.client.screen.InventoryHint;
import dev.ua.ikeepcalm.coi.client.screen.TourScreen;
import dev.ua.ikeepcalm.coi.client.state.ActingState;
import dev.ua.ikeepcalm.coi.client.state.ActionBarState;
import dev.ua.ikeepcalm.coi.client.state.AppearanceState;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;
import dev.ua.ikeepcalm.coi.client.state.CogitationState;
import dev.ua.ikeepcalm.coi.client.state.MenuState;
import dev.ua.ikeepcalm.coi.client.state.NotificationState;
import dev.ua.ikeepcalm.coi.client.state.ResourceState;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.state.TargetState;
import dev.ua.ikeepcalm.coi.client.ui.IconModels;
import dev.ua.ikeepcalm.coi.client.ui.IngredientTooltips;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

/**
 * The mod's client entry point: load what is on disk, register what draws and
 * what listens, and wire the two connection events every piece of per-server
 * state hangs off.
 * <p>
 * Nothing here holds state of its own except the pending tour — the pieces it
 * starts up own theirs — so this class reads as the order things happen in.
 */
public class CoiClientMod implements ClientModInitializer {

    private static final ClientDataLoader CLIENT_DATA_LOADER = new ClientDataLoader();

    /**
     * How long after the first ability list the tour waits, so the world has
     * rendered before a screen covers it.
     */
    private static final long TOUR_DELAY_MS = 3000;

    // When > 0, the first-join tour opens at this timestamp (set on the first
    // non-empty abilities payload)
    private static long tourPendingAt = 0;

    @Override
    public void onInitializeClient() {
        HudConfig.load();
        ClientStateStore.load();
        AbilityBindings.load();
        CoiNetworking.registerPayloads();
        CoiKeyBindings.registerKeybindings();
        registerTickHandler();
        registerOverlays();
        registerConnectionEvents();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("coi-client", "client_json_loader"), CLIENT_DATA_LOADER);

        IngredientTooltips.register(CLIENT_DATA_LOADER);
    }

    /**
     * Every HUD element the mod draws, in registration order. All of them
     * attach beside a vanilla element except the Beyonder health bar, which
     * takes the hearts' slot outright.
     */
    private static void registerOverlays() {
        AbilityOverlay.initialize();
        // Replaces the vanilla hearts rather than attaching beside them, so it
        // has to be registered like every other element but reads differently
        BeyonderHealthOverlay.initialize();
        CharacterPlateOverlay.initialize();
        MadnessOverlay.initialize();
        SpiritualityOverlay.initialize();
        ActingOverlay.initialize();
        ResourceOverlay.initialize();
        ActionBarOverlay.initialize();
        TargetHealthOverlay.initialize();
        CogitationOverlay.initialize();
        NotificationOverlay.initialize();
        EffectManager.initialize();
        // Explains the hotbar slot the plugin empties for modded clients
        InventoryHint.register();
        FormModelLayers.register();
    }

    private static void registerConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CoiNetworking.sendHello();
            CoiNetworking.requestAbilitiesFromServer();
            var server = client.getCurrentServer();
            DiscordPresenceManager.onServerJoin(
                    server != null ? server.name : "Singleplayer",
                    server != null ? server.ip : null
            );
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onDisconnect());
    }

    /**
     * Everything the server fed us goes away together. A holder that forgets to
     * reset here is the bug that shows the previous server's madness on the
     * next one, so the list is deliberately exhaustive rather than clever.
     */
    private static void onDisconnect() {
        // Remember how mad we were — the title screen holds a grudge
        ClientStateStore.setLastMadness(BeyonderState.getMadness());
        BeyonderState.reset();
        ActingState.reset();
        ResourceState.reset();
        ActionBarState.reset();
        TargetState.reset();
        CogitationState.reset();
        NotificationState.reset();
        SheetState.reset();
        MenuState.reset();
        AppearanceState.reset();
        ServerCapabilities.reset();
        SpiritualityOverlay.reset();
        BeyonderHealthOverlay.reset();
        // The next server may key its ability icons against a different pack
        IconModels.clearCache();
        EffectManager.stopAll();
        MythicalFormManager.clearAll();
        tourPendingAt = 0;
        DiscordPresenceManager.onDisconnect();
    }

    /**
     * The mod's one client-tick subscription. Everything that needs a tick is
     * called from here in a fixed order rather than registering its own
     * listener, so the order is readable in one place.
     */
    private static void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HallucinationManager.tick(client);
            DiscordPresenceManager.tick();

            if (client.player == null) return;

            tickTour(client);
            CoiKeyBindings.tickKeys(client);
        });
    }

    /**
     * Arms the first-join tour once the server has actually named some
     * abilities — a player with an empty list has nothing to be toured around.
     */
    public static void scheduleTourIfFirstList() {
        if (AbilityRegistry.hasAbilities() && ClientStateStore.isTourNotCompleted() && tourPendingAt == 0) {
            tourPendingAt = System.currentTimeMillis() + TOUR_DELAY_MS;
        }
    }

    /**
     * First-join tour: opens once the delay has passed and no other
     * screen is in the way (stays pending until the way is clear).
     */
    private static void tickTour(Minecraft client) {
        if (tourPendingAt > 0 && System.currentTimeMillis() >= tourPendingAt && client.gui.screen() == null) {
            tourPendingAt = 0;
            if (ClientStateStore.isTourNotCompleted()) {
                client.gui.setScreen(new TourScreen());
            }
        }
    }
}
