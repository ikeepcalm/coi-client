package dev.ua.ikeepcalm.coi.client.network.payload;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server click inside a {@code coi-client:menu} document.
 * <p>
 * {@code {"session":"…","version":N,"action":"…","value":"…"}} — the session
 * token and version come straight off the document the click happened on, so
 * the server can drop a click aimed at a screen it has already replaced. The
 * action ids are opaque tokens minted by the server when it built the document;
 * the client never invents one, except for the reserved lifecycle actions
 * {@code __back} and {@code __close}.
 */
public record MenuActionPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MenuActionPayload> ID = CoiPayloads.type("menu_action");
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC =
            CoiPayloads.text(MenuActionPayload::json,
                    MenuActionPayload::new,
                    CoiPayloads.MAX_ACTION);

    public static final String BACK = "__back";
    public static final String CLOSE = "__close";

    public static MenuActionPayload of(String session, int version, String action, String value) {
        JsonObject json = new JsonObject();
        json.addProperty("session", session);
        json.addProperty("version", version);
        json.addProperty("action", action);
        if (value != null) json.addProperty("value", value);
        return new MenuActionPayload(json.toString());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
