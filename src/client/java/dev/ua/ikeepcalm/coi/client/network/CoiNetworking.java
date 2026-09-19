package dev.ua.ikeepcalm.coi.client.network;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.effect.EffectManager;
import dev.ua.ikeepcalm.coi.client.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.menu.MenuParser;
import dev.ua.ikeepcalm.coi.client.network.payload.*;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.client.screen.sheet.CharacterSheetScreen;
import dev.ua.ikeepcalm.coi.client.state.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

/**
 * The wire: every payload this client speaks, and the receivers that hand an
 * incoming one to whichever piece of state owns it.
 */
public class CoiNetworking {

    private CoiNetworking() {
    }

    /**
     * Declares every payload type and attaches a receiver to each S2C one.
     * Called once at init, before anything can connect.
     */
    public static void registerPayloads() {
        registerTypes();
        registerReceivers();
    }

    private static void registerTypes() {
        // C2S (client → server = serverboundPlay)
        PayloadTypeRegistry.serverboundPlay().register(AbilityUsePayload.ID, AbilityUsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AbilityCategoryUsePayload.ID, AbilityCategoryUsePayload.CODEC);
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
    }

    /**
     * Every receiver hops onto the client thread before touching state: a
     * payload arrives on the netty thread, and the holders it feeds are read by
     * the renderer.
     */
    private static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(AbilitiesPayload.ID,
                (payload, context) -> context.client().execute(() -> AbilityRegistry.handleAbilityData(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.ID,
                (payload, context) -> context.client().execute(() -> AbilityRegistry.handleCooldownData(payload.abilityId(), payload.ticks())));
        ClientPlayNetworking.registerGlobalReceiver(VisualEffectPayload.ID,
                (payload, context) -> context.client().execute(() -> EffectManager.trigger(payload.effectId(), payload.params())));
        ClientPlayNetworking.registerGlobalReceiver(MythicalFormPayload.ID,
                (payload, context) -> context.client().execute(() -> MythicalFormManager.handlePacket(payload.targetUuid(), payload.params())));
        ClientPlayNetworking.registerGlobalReceiver(ConditionsPayload.ID,
                (payload, context) -> context.client().execute(() -> BeyonderState.parseAndUpdate(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(AppearancePayload.ID,
                (payload, context) -> context.client().execute(() -> AppearanceState.handlePacket(payload.targetUuid(), payload.traits())));
        ClientPlayNetworking.registerGlobalReceiver(ServerInfoPayload.ID,
                (payload, context) -> context.client().execute(() -> ServerCapabilities.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(AbilitiesV2Payload.ID,
                (payload, context) -> context.client().execute(() -> AbilityRegistry.handleAbilityDataV2(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(AbilityStatePayload.ID,
                (payload, context) -> context.client().execute(() -> AbilityRegistry.handleAbilityState(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ActingPayload.ID,
                (payload, context) -> context.client().execute(() -> ActingState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ResourcePayload.ID,
                (payload, context) -> context.client().execute(() -> ResourceState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(ActionBarPayload.ID,
                (payload, context) -> context.client().execute(() -> ActionBarState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(TargetHealthPayload.ID,
                (payload, context) -> context.client().execute(() -> TargetState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(CogitationPayload.ID,
                (payload, context) -> context.client().execute(() -> CogitationState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(NotifyPayload.ID,
                (payload, context) -> context.client().execute(() -> NotificationState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(SheetPayload.ID,
                (payload, context) -> context.client().execute(() -> SheetState.handle(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(MenuPayload.ID,
                (payload, context) -> context.client().execute(() -> handleMenu(payload.json())));
    }

    /**
     * Capability handshake — always first, so the server knows which surfaces
     * this client can render before it starts feeding any of them.
     */
    public static void sendHello() {
        if (Minecraft.getInstance().player == null) return;
        ClientPlayNetworking.send(new HelloPayload(ClientFeatures.helloJson()));
    }

    public static void requestAbilitiesFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && ClientPlayNetworking.canSend(AbilityRequestPayload.ID)) {
            CoiLog.LOG.info("Requesting abilities from server");
            ClientPlayNetworking.send(AbilityRequestPayload.INSTANCE);
        }
    }

    /**
     * A menu document: open the screen, refresh the open one, or take it away.
     * <p>
     * Refreshing is just adopting the document — {@link MenuScreen} re-reads
     * {@link MenuState} and rebuilds itself — so a push that lands while
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
                    && MenuState.openedFromSheet()
                    && ServerCapabilities.has("character_sheet");
            MenuState.clear();
            // The server already knows the menu is gone; echoing __close back
            // would only race its next open
            if (client.gui.screen() instanceof MenuScreen menu) menu.closeQuietly();
            if (toSheet) client.gui.setScreen(new CharacterSheetScreen(null));
            return;
        }
        MenuState.adopt(document);
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
}
