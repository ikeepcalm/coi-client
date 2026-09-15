package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client character sheet snapshot (feature {@code character_sheet}).
 * <p>
 * One JSON string carrying identity, vitals, mind, acting ledger and the
 * availability gates for the server-side sub-menus. Pushed when the client
 * announces {@code sheet_open}, after a terrain-damage toggle, and every 60
 * ticks while the sheet stays open.
 */
public record SheetPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SheetPayload> ID = CoiPayloads.type("sheet");
    public static final StreamCodec<RegistryFriendlyByteBuf, SheetPayload> CODEC =
            CoiPayloads.text(SheetPayload::json,
                    SheetPayload::new,
                    CoiPayloads.MAX_DOCUMENT);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
