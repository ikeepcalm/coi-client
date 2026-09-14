package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client target health after an ability hit (feature
 * {@code target_health}).
 * <p>
 * json — {@code {"uuid":…,"name":…,"before":0.82,"after":0.75,"health":150.0,
 * "max":200.0,"damage":12.5,"kind":"beyonder"}}. {@code before}/{@code after}
 * are 0..1 fractions; one packet replaces the server's 60-tick action-bar
 * animation.
 */
public record TargetHealthPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TargetHealthPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetHealthPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new TargetHealthPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
