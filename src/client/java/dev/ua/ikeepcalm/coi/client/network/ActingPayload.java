package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client acting progress (feature {@code acting}).
 * <p>
 * json — {@code {"pathway":…,"sequence":…,"acting":…,"needed":…,"percent":…,
 * "cooldownRemaining":…,"cooldownTotal":…,"overflow":…,"overflowEligible":…,
 * "limited":…,"outer":…,"granted":…,"source":…}}. {@code granted}/{@code source}
 * are only non-zero on the push that follows an actual grant.
 */
public record ActingPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActingPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "acting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActingPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new ActingPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
