package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CooldownPayload(String abilityId, int ticks) implements CustomPayload {
    public static final CustomPayload.Id<CooldownPayload> ID =
        new CustomPayload.Id<>(Identifier.of("coi-client", "cooldown"));
    public static final PacketCodec<RegistryByteBuf, CooldownPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.abilityId());
            buf.writeInt(value.ticks());
        },
        buf -> new CooldownPayload(buf.readString(), buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
