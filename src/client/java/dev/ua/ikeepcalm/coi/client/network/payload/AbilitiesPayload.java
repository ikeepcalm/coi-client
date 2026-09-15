package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client ability list, protocol 1.
 * <p>
 * data — {@code id|localizedName|englishName|category|hasLeftClick} per entry,
 * {@code ;} separated. Still the only list a server without
 * {@code ability_meta} sends, which is why it outlived
 * {@link AbilitiesV2Payload}.
 */
public record AbilitiesPayload(String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilitiesPayload> ID = CoiPayloads.type("abilities");
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesPayload> CODEC =
            CoiPayloads.text(AbilitiesPayload::data,
                    AbilitiesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
