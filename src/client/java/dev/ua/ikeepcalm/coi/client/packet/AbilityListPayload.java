package dev.ua.ikeepcalm.coi.client.packet;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record AbilityListPayload(List<String> abilities) implements CustomPayload {
        public static final CustomPayload.Id<AbilityListPayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "ability_list"));

        public static final PacketCodec<RegistryByteBuf, AbilityListPayload> CODEC = PacketCodec.ofStatic(
                AbilityListPayload::write,
                AbilityListPayload::read
        );

        public static void write(RegistryByteBuf buf, AbilityListPayload payload) {
            buf.writeInt(payload.abilities().size());
            for (String ability : payload.abilities()) {
                buf.writeString(ability);
            }
        }

        public static AbilityListPayload read(RegistryByteBuf buf) {
            System.out.println("[COI DEBUG] Reading AbilityListPayload");
            int count = buf.readInt();
            List<String> abilities = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                abilities.add(buf.readString());
            }
            System.out.println(abilities);
            return new AbilityListPayload(abilities);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }