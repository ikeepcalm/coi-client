package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AbilityUsePayload(String abilityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilityUsePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityUsePayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.abilityId()),
            buf -> new AbilityUsePayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
