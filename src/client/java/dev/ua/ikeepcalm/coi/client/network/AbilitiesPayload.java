package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AbilitiesPayload(String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilitiesPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "abilities"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.data()),
            buf -> new AbilitiesPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
