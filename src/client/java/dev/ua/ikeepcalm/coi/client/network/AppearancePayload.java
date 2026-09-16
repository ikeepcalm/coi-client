package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record AppearancePayload(String targetUuid, String traits) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AppearancePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "appearance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AppearancePayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeUtf(value.targetUuid());
                buf.writeUtf(value.traits());
            },
            buf -> new AppearancePayload(buf.readUtf(), buf.readUtf())
    );

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
