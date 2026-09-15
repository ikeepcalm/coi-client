package dev.ua.ikeepcalm.coi.client.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client single-ability state change (feature {@code ability_state}).
 * <p>
 * json — either a toggle ({@code {"id":"sun-9-0","active":true}}) or a
 * category switch ({@code {"id":…,"category":"fire","categoryName":…}}).
 * These are the HUD replacement for the plugin's STATUS action-bar spam.
 */
public record AbilityStatePayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilityStatePayload> ID = CoiPayloads.type("state");
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityStatePayload> CODEC =
            CoiPayloads.text(AbilityStatePayload::json,
                    AbilityStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
