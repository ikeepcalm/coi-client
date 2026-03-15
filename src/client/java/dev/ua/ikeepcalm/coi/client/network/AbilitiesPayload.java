package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AbilitiesPayload(String data) implements CustomPayload {
    public static final CustomPayload.Id<AbilitiesPayload> ID =
        new CustomPayload.Id<>(Identifier.of("coi-client", "abilities"));
    public static final PacketCodec<RegistryByteBuf, AbilitiesPayload> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeString(value.data()),
        buf -> new AbilitiesPayload(buf.readString())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
