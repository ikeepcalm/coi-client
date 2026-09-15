package dev.ua.ikeepcalm.coi.client.network.payload;

import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client reply to {@link HelloPayload}.
 * <p>
 * json — {@code {"pluginVersion":"1.3.2","protocol":2,"features":[...]}},
 * consumed by {@link dev.ua.ikeepcalm.coi.client.network.ServerCapabilities}.
 * Old servers never send it, which is how the client knows to hide UI the
 * server will not feed.
 */
public record ServerInfoPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerInfoPayload> ID = CoiPayloads.type("server");
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerInfoPayload> CODEC =
            CoiPayloads.text(ServerInfoPayload::json,
                    ServerInfoPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
