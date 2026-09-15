package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client rich ability list (protocol 2, feature {@code ability_meta}).
 * <p>
 * json — {@code {"abilities":[{"id":…,"name":…,"cost":…,"icon":…}, …]}}.
 * Replaces {@link AbilitiesPayload} for capable clients; the delimited v1
 * channel keeps serving everyone else.
 * <p>
 * The default {@code writeUtf} cap is 32767 chars, which a full ability list
 * with descriptions blows straight through — both ends use
 * {@link CoiPayloads#MAX_DOCUMENT} instead.
 */
public record AbilitiesV2Payload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilitiesV2Payload> ID = CoiPayloads.type("abilities_v2");
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesV2Payload> CODEC =
            CoiPayloads.text(AbilitiesV2Payload::json,
                    AbilitiesV2Payload::new,
                    CoiPayloads.MAX_DOCUMENT);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
