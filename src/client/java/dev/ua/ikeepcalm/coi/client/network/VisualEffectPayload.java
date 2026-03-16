package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
public record VisualEffectPayload(String effectId, String params) implements CustomPayload {

    public static final CustomPayload.Id<VisualEffectPayload> ID =
            new CustomPayload.Id<>(Identifier.of("coi-client", "effect"));

    public static final PacketCodec<RegistryByteBuf, VisualEffectPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.effectId());
                buf.writeString(value.params());
            },
            buf -> new VisualEffectPayload(buf.readString(), buf.readString())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
