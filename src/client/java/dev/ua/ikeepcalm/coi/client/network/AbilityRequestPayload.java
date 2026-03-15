package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AbilityRequestPayload() implements CustomPayload {
    public static final AbilityRequestPayload INSTANCE = new AbilityRequestPayload();
    public static final CustomPayload.Id<AbilityRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("coi-client", "request"));
    public static final PacketCodec<RegistryByteBuf, AbilityRequestPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
