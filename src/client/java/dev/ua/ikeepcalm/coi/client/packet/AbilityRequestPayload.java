package dev.ua.ikeepcalm.coi.client.packet;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AbilityRequestPayload(String request) implements CustomPayload {
        public static final CustomPayload.Id<AbilityRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "ability_list"));

        public static void write(RegistryByteBuf buf, AbilityRequestPayload payload) {
            buf.writeString(payload.request());
        }

        public static AbilityRequestPayload read(RegistryByteBuf buf) {
            return new AbilityRequestPayload(buf.readString());
        }

        public static final PacketCodec<RegistryByteBuf, AbilityRequestPayload> CODEC = PacketCodec.ofStatic(
                AbilityRequestPayload::write,
                AbilityRequestPayload::read
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }