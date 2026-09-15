package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client cooldown for one ability, in ticks. The HUD converts it to
 * wall-clock time so the sweep animates smoothly between packets.
 */
public record CooldownPayload(String abilityId, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CooldownPayload> ID = CoiPayloads.type("cooldown");
    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeUtf(value.abilityId());
                buf.writeInt(value.ticks());
            },
            buf -> new CooldownPayload(buf.readUtf(), buf.readInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
