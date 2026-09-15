package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client cogitation session events (feature {@code cogitation}),
 * replacing the vanilla titles for capable clients.
 * <p>
 * json — one of {@code {"state":"start"}},
 * {@code {"state":"stop","interrupted":false}},
 * {@code {"state":"prompt","action":"TURN_360","label":…,"streak":3,"timeoutMs":5000}},
 * {@code {"state":"fail","streak":0}}.
 */
public record CogitationPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CogitationPayload> ID = CoiPayloads.type("cogitation");
    public static final StreamCodec<RegistryFriendlyByteBuf, CogitationPayload> CODEC =
            CoiPayloads.text(CogitationPayload::json,
                    CogitationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
