package dev.ua.ikeepcalm.coi.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityRegistry;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.*;
import dev.ua.ikeepcalm.coi.domain.effect.EffectManager;
import dev.ua.ikeepcalm.coi.domain.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.domain.glyph.GlyphSheet;
import dev.ua.ikeepcalm.coi.domain.menu.model.MenuDocument;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuParser;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuState;
import dev.ua.ikeepcalm.coi.network.payload.ClientboundPayloads.*;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.*;
import dev.ua.ikeepcalm.coi.screen.glyph.GlyphCanvasScreen;
import dev.ua.ikeepcalm.coi.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.screen.sheet.CharacterSheetScreen;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

public class CoiNetworking {

    private CoiNetworking() {
    }

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
        PayloadTypeRegistry.serverboundPlay().register(GlyphSubmitPayload.ID, GlyphSubmitPayload.CODEC);
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
        PayloadTypeRegistry.clientboundPlay().register(GlyphPayload.ID, GlyphPayload.CODEC);
    }

    /**
     * Every receiver hops onto the client thread: payloads arrive on netty, state is read by the renderer.
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
        ClientPlayNetworking.registerGlobalReceiver(GlyphPayload.ID,
                (payload, context) -> context.client().execute(() -> handleGlyph(payload.json())));
    }

    /** Always sent before the first ability request, so the server knows what to feed. */
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

    private static void handleMenu(String json) {
        MenuDocument document = MenuParser.parse(json);
        if (document == null) return;
        Minecraft client = Minecraft.getInstance();
        if (document.closed()) {
            // A close carrying "back" is the back arrow at the root of the server's stack:
            // nothing underneath on its side, but the sheet may be where the player came from.
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
     * Drawing magic: an {@code open} replaces whatever screen is up with a
     * fresh canvas; a {@code result} goes only to the canvas whose session id
     * it names exactly, so a late answer never lands on a newer canvas. The
     * server echoes the submitted id even when it has no session for us.
     */
    private static void handleGlyph(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            Minecraft client = Minecraft.getInstance();
            String type = JsonRead.string(root, "type", "");
            if ("open".equals(type)) {
                GlyphSheet sheet = GlyphSheet.parse(root);
                if (sheet != null) client.gui.setScreen(new GlyphCanvasScreen(sheet));
            } else if ("result".equals(type) && client.gui.screen() instanceof GlyphCanvasScreen canvas) {
                String session = JsonRead.string(root, "session", "");
                if (session.isEmpty() || !session.equals(canvas.session())) return;
                canvas.showResult(JsonRead.bool(root, "ok"), JsonRead.string(root, "message", ""),
                        JsonRead.intOf(root, "layer", -1));
            }
        } catch (RuntimeException e) {
            CoiLog.LOG.warn("Malformed glyph payload: {}", e.getMessage());
        }
    }

    /**
     * Read off the raw document rather than through {@link MenuParser}: it describes the
     * transition, and a closed document has no screen for the parser to describe.
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
