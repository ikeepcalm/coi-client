package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client toast notification (feature {@code notify}).
 * <p>
 * json — {@code {"kind":"advancement|acting|bounty|madness|info","title":…,
 * "body":…,"color":"RRGGBB","durationMs":4000}}. Text is already resolved in
 * the player's locale server-side.
 */
public record NotifyPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NotifyPayload> ID = CoiPayloads.type("notify");
    public static final StreamCodec<RegistryFriendlyByteBuf, NotifyPayload> CODEC =
            CoiPayloads.text(NotifyPayload::json,
                    NotifyPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
