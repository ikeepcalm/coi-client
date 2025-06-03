package dev.ua.ikeepcalm.coi.client.packet;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AbilityUsePayload(String abilityId) implements CustomPayload {
        public static final CustomPayload.Id<AbilityUsePayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "abilities"));

        public static void write(RegistryByteBuf buf, AbilityUsePayload payload) { buf.writeString(payload.abilityId()); }
        public static AbilityUsePayload read(RegistryByteBuf buf) {
            return new AbilityUsePayload(buf.readString());
        }

        public static final PacketCodec<RegistryByteBuf, AbilityUsePayload> CODEC = PacketCodec.ofStatic(
                AbilityUsePayload::write,
                AbilityUsePayload::read
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }