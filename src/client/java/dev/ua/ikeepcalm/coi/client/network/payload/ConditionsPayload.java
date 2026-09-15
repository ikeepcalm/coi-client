package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client Beyonder condition for the local player.
 * <p>
 * data — {@code key=value} pairs separated by {@code ;} (madness,
 * permanentMadness, freezeStacks, mentalPressure, tiredness, spirituality,
 * maxSpirituality, spiritualityRegen, maxHealth, pathway, sequence). The one
 * channel that never became JSON; every key is optional.
 */
public record ConditionsPayload(String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConditionsPayload> ID = CoiPayloads.type("conditions");
    public static final StreamCodec<RegistryFriendlyByteBuf, ConditionsPayload> CODEC =
            CoiPayloads.text(ConditionsPayload::data,
                    ConditionsPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
