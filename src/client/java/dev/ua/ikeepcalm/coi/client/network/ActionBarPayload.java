package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client action-bar entries (feature {@code action_bar}).
 * <p>
 * json — {@code {"maxVisible":2,"entries":[{"channel":"COOLDOWN",
 * "key":"artifact:sword","priority":60,"ttlMs":1850,"text":{…}}]}}, where
 * {@code text} is a vanilla text component object. Each payload replaces the
 * whole list; an empty {@code entries} array clears it.
 */
public record ActionBarPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionBarPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "actionbar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionBarPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new ActionBarPayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
