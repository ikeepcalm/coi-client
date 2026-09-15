package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client mythical creature form for one player.
 * <p>
 * params — {@code pathway:<unused>:start|stop}. The UUID is a target, not the
 * receiver: every client in range is told, since the form replaces how that
 * player is drawn for everyone.
 */
public record MythicalFormPayload(String targetUuid, String params) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MythicalFormPayload> ID = CoiPayloads.type("mythical");

    public static final StreamCodec<RegistryFriendlyByteBuf, MythicalFormPayload> CODEC =
            CoiPayloads.text2(MythicalFormPayload::targetUuid,
                    MythicalFormPayload::params,
                    MythicalFormPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
