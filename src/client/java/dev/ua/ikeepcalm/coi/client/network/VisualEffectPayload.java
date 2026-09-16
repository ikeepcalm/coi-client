package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client payload to trigger (or stop) a visual effect.
 * <p>
 * effectId – registered effect name (e.g. "cracks", "eyes", "vignette")
 * params   – comma-separated key=value pairs, or "stop" to remove the effect,
 * or "all=stop" to remove every active effect.
 * <p>
 * Examples:
 * VisualEffectPayload("cracks",  "intensity=0.8,pulse=true,duration=8000")
 * VisualEffectPayload("eyes",    "count=3")
 * VisualEffectPayload("cracks",  "stop")
 * VisualEffectPayload("all",     "stop")
 */
public record VisualEffectPayload(String effectId, String params) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VisualEffectPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VisualEffectPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeUtf(value.effectId());
                buf.writeUtf(value.params());
            },
            buf -> new VisualEffectPayload(buf.readUtf(), buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
