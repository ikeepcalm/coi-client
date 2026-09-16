package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConditionsPayload(String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConditionsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "conditions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConditionsPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.data()),
            buf -> new ConditionsPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
