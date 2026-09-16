package dev.ua.ikeepcalm.coi.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record AbilityUsePayload(String abilityId, String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityUsePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("coi-client", "use"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityUsePayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeUtf(value.abilityId());
                buf.writeUtf(value.action());
            },
            buf -> new AbilityUsePayload(buf.readUtf(), buf.readUtf())
    );

    public AbilityUsePayload(String abilityId) {
        this(abilityId, "execute");
    }

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
