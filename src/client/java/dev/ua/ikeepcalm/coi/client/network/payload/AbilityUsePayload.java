package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Client → Server ability activation.
 * <p>
 * action is {@code execute} (the keybind, the wheel, a gesture) or
 * {@code left_click} for the abilities that have a second, separate use — the
 * one-argument constructor is the common case.
 */
public record AbilityUsePayload(String abilityId, String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityUsePayload> ID = CoiPayloads.type("use");

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityUsePayload> CODEC =
            CoiPayloads.text2(AbilityUsePayload::abilityId,
                    AbilityUsePayload::action,
                    AbilityUsePayload::new);

    public AbilityUsePayload(String abilityId) {
        this(abilityId, "execute");
    }

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
