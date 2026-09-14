package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client reply to {@link HelloPayload}.
 * <p>
 * json — {@code {"pluginVersion":"1.3.2","protocol":2,"features":[...]}},
 * consumed by {@link dev.ua.ikeepcalm.coi.client.ServerCapabilities}.
 * Old servers never send it, which is how the client knows to hide UI the
 * server will not feed.
 */
public record ServerInfoPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerInfoPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerInfoPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new ServerInfoPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
