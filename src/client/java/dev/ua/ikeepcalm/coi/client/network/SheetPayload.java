package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client character sheet snapshot (feature {@code character_sheet}).
 * <p>
 * One JSON string carrying identity, vitals, mind, acting ledger and the
 * availability gates for the server-side sub-menus. Pushed when the client
 * announces {@code sheet_open}, after a terrain-damage toggle, and every 60
 * ticks while the sheet stays open.
 */
public record SheetPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SheetPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "sheet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SheetPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json(), 1_048_576),
            buf -> new SheetPayload(buf.readUtf(1_048_576))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
