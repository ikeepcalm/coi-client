package dev.ua.ikeepcalm.coi.client.network;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
    public static final CustomPacketPayload.Type<MenuActionPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "menu_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json(), 32_768),
            buf -> new MenuActionPayload(buf.readUtf(32_768))
    );

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
