package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client toast notification (feature {@code notify}).
 * <p>
 * json — {@code {"kind":"advancement|acting|bounty|madness|info","title":…,
 * "body":…,"color":"RRGGBB","durationMs":4000}}. Text is already resolved in
 * the player's locale server-side.
 */
public record NotifyPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NotifyPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "notify"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NotifyPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new NotifyPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
