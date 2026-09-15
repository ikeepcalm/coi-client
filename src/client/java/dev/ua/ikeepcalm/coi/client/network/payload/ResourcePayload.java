package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client resource meter (feature {@code resource_bar}).
 * <p>
 * json — {@code {"id":…,"label":…,"current":…,"max":…,"color":"RRGGBB",
 * "ttlMs":…,"format":"percent"|"value"}}, one object per update. The {@code id}
 * is the stable key: a later packet with the same id replaces that bar in
 * place. {@code {"id":…,"remove":true}} takes it off the HUD.
 */
public record ResourcePayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResourcePayload> ID = CoiPayloads.type("resource");
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourcePayload> CODEC =
            CoiPayloads.text(ResourcePayload::json,
                    ResourcePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
