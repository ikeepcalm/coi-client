package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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

    public static final CustomPacketPayload.Type<VisualEffectPayload> ID = CoiPayloads.type("effect");

    public static final StreamCodec<RegistryFriendlyByteBuf, VisualEffectPayload> CODEC =
            CoiPayloads.text2(VisualEffectPayload::effectId,
                    VisualEffectPayload::params,
                    VisualEffectPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
