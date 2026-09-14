package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client single-ability state change (feature {@code ability_state}).
 * <p>
 * json — either a toggle ({@code {"id":"sun-9-0","active":true}}) or a
 * category switch ({@code {"id":…,"category":"fire","categoryName":…}}).
 * These are the HUD replacement for the plugin's STATUS action-bar spam.
 */
public record AbilityStatePayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilityStatePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityStatePayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeUtf(value.json()),
            buf -> new AbilityStatePayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
