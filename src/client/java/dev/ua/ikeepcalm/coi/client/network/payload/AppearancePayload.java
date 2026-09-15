package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Server → Client passive appearance traits for one player.
 * <p>
 * traits — comma-separated ids, or empty to clear them. Unlike a mythical
 * form this is persistent: the last non-empty set for a UUID stands until an
 * explicit clear, so a missed resync does not flicker a trait off.
 */
public record AppearancePayload(String targetUuid, String traits) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AppearancePayload> ID = CoiPayloads.type("appearance");

    public static final StreamCodec<RegistryFriendlyByteBuf, AppearancePayload> CODEC =
            CoiPayloads.text2(AppearancePayload::targetUuid,
                    AppearancePayload::traits,
                    AppearancePayload::new);

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
