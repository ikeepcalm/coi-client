package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MythicalFormPayload(String targetUuid, String params) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MythicalFormPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "mythical"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MythicalFormPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeUtf(value.targetUuid());
                buf.writeUtf(value.params());
            },
            buf -> new MythicalFormPayload(buf.readUtf(), buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
