package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AbilityUsePayload(String abilityId) implements CustomPayload {
    public static final CustomPayload.Id<AbilityUsePayload> ID =
        new CustomPayload.Id<>(Identifier.of("coi-client", "use"));
    public static final PacketCodec<RegistryByteBuf, AbilityUsePayload> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeString(value.abilityId()),
        buf -> new AbilityUsePayload(buf.readString())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
