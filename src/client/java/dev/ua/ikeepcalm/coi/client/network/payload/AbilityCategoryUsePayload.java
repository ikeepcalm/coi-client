package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/** Atomic category selection and normal cast; separate channel prevents legacy miscasts. */
public record AbilityCategoryUsePayload(String abilityId, String category) implements CustomPacketPayload {
    public static final Type<AbilityCategoryUsePayload> ID = CoiPayloads.type("use_category");
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCategoryUsePayload> CODEC =
            CoiPayloads.text2(AbilityCategoryUsePayload::abilityId, AbilityCategoryUsePayload::category,
                    AbilityCategoryUsePayload::new);
    @Override public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}
