package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client rich ability list (protocol 2, feature {@code ability_meta}).
 * <p>
 * json — {@code {"abilities":[{"id":…,"name":…,"cost":…,"icon":…}, …]}}.
 * Replaces {@link AbilitiesPayload} for capable clients; the delimited v1
 * channel keeps serving everyone else.
 * <p>
 * The default {@code writeUtf} cap is 32767 chars, which a full ability list
 * with descriptions blows straight through — both ends use 1 MiB instead.
 */
public record AbilitiesV2Payload(String json) implements CustomPacketPayload {

    public static final int MAX_LENGTH = 1_048_576;

    public static final CustomPacketPayload.Type<AbilitiesV2Payload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "abilities_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesV2Payload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json(), MAX_LENGTH),
            buf -> new AbilitiesV2Payload(buf.readUtf(MAX_LENGTH))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
