package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client acting progress (feature {@code acting}).
 * <p>
 * json — {@code {"pathway":…,"sequence":…,"acting":…,"needed":…,"percent":…,
 * "cooldownRemaining":…,"cooldownTotal":…,"overflow":…,"overflowEligible":…,
 * "limited":…,"outer":…,"granted":…,"source":…}}. {@code granted}/{@code source}
 * are only non-zero on the push that follows an actual grant.
 */
public record ActingPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActingPayload> ID = CoiPayloads.type("acting");
    public static final StreamCodec<RegistryFriendlyByteBuf, ActingPayload> CODEC =
            CoiPayloads.text(ActingPayload::json,
                    ActingPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
