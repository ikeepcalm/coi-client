package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client menu document (feature {@code menu_ui}).
 * <p>
 * One JSON string describing a whole screen — title, sections, components,
 * buttons — which the client renders natively instead of the plugin opening an
 * InvUI chest GUI. The server owns navigation: every click goes back as a
 * {@link MenuActionPayload} and is answered with another document (or a
 * {@code "closed":true} one).
 *
 * @see dev.ua.ikeepcalm.coi.client.menu.MenuDocument
 */
public record MenuPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MenuPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json(), 1_048_576),
            buf -> new MenuPayload(buf.readUtf(1_048_576))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
