package dev.ua.ikeepcalm.coi.client.network.payload;

import dev.ua.ikeepcalm.coi.client.network.ClientFeatures;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server capability handshake, sent once on JOIN before the first
 * ability request.
 * <p>
 * json — {@code {"modVersion":"1.2.0","protocol":2,"features":[...]}},
 * built by {@link dev.ua.ikeepcalm.coi.client.network.ClientFeatures#helloJson()}.
 */
public record HelloPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelloPayload> ID = CoiPayloads.type("hello");
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC =
            CoiPayloads.text(HelloPayload::json,
                    HelloPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
