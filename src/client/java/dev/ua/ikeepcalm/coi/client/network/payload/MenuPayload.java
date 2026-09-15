package dev.ua.ikeepcalm.coi.client.network.payload;

import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client menu document (feature {@code menu_ui}).
 * <p>
 * One JSON string describing a whole screen — title, sections, components,
 * buttons — which the client renders natively instead of the plugin opening an
 * InvUI chest GUI. The server owns navigation: every click goes back as a
 * {@link MenuActionPayload} and is answered with another document (or a
 * {@code "closed":true} one).
 *
 * @see dev.ua.ikeepcalm.coi.client.menu.MenuDocument
 */
public record MenuPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MenuPayload> ID = CoiPayloads.type("menu");
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuPayload> CODEC =
            CoiPayloads.text(MenuPayload::json,
                    MenuPayload::new,
                    CoiPayloads.MAX_DOCUMENT);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
