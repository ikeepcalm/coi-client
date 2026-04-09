package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AbilityRequestPayload() implements CustomPacketPayload {
    public static final AbilityRequestPayload INSTANCE = new AbilityRequestPayload();
    public static final CustomPacketPayload.Type<AbilityRequestPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
