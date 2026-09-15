package dev.ua.ikeepcalm.coi.client.network.payload;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server request to run a server-side action, e.g. opening the
 * Beyonder menu that used to live on the slot-9 shortcut item.
 * <p>
 * json — {@code {"action":"open_menu"}}. Only sent when the server advertised
 * the {@code menu_action} feature in its hello reply.
 */
public record ActionPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionPayload> ID = CoiPayloads.type("action");
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
            CoiPayloads.text(ActionPayload::json,
                    ActionPayload::new);

    public static ActionPayload of(String action) {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        return new ActionPayload(json.toString());
    }

    /**
     * {@code {"action":"open","target":…,"ui":"client"|"server"}} - the sheet's
     * sub-menu buttons. {@code ui} is the player's own preference
     * ({@code useServerMenus}): {@code client} asks for a
     * {@code coi-client:menu} document, {@code server} for the original InvUI
     * chest GUI. A server that predates the menu protocol ignores the field and
     * opens its chest GUI either way.
     */
    public static ActionPayload ofOpen(String target) {
        JsonObject json = new JsonObject();
        json.addProperty("action", "open");
        json.addProperty("target", target);
        json.addProperty("ui", HudConfig.getSettings().useServerMenus
                ? "server" : "client");
        return new ActionPayload(json.toString());
    }

    /**
     * Same, with one extra field — used by the character sheet for
     * {@code {"action":"open","target":"church"}}.
     */
    public static ActionPayload of(String action, String key, String value) {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty(key, value);
        return new ActionPayload(json.toString());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
