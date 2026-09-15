package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server "send me my abilities", with no body at all — which is why
 * it is a singleton under {@code StreamCodec.unit}. Sent on JOIN, right after
 * the {@link HelloPayload}.
 */
public record AbilityRequestPayload() implements CustomPacketPayload {
    public static final AbilityRequestPayload INSTANCE = new AbilityRequestPayload();
    public static final CustomPacketPayload.Type<AbilityRequestPayload> ID = CoiPayloads.type("request");
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
