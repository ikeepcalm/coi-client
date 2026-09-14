package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server capability handshake, sent once on JOIN before the first
 * ability request.
 * <p>
 * json — {@code {"modVersion":"1.2.0","protocol":2,"features":[...]}},
 * built by {@link dev.ua.ikeepcalm.coi.client.ClientFeatures#helloJson()}.
 */
public record HelloPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelloPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new HelloPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
