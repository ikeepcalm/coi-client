package dev.ua.ikeepcalm.coi.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Every Server -> Client payload. Body shapes: docs/NETWORK_PROTOCOL.md.
 */
public final class ClientboundPayloads {

    private ClientboundPayloads() {
    }

    public record AbilitiesPayload(String data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AbilitiesPayload> ID = CoiPayloads.type("abilities");
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesPayload> CODEC =
                CoiPayloads.text(AbilitiesPayload::data,
                        AbilitiesPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record AbilitiesV2Payload(String json) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AbilitiesV2Payload> ID = CoiPayloads.type("abilities_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilitiesV2Payload> CODEC =
                CoiPayloads.text(AbilitiesV2Payload::json,
                        AbilitiesV2Payload::new,
                        CoiPayloads.MAX_DOCUMENT);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

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

    public record ActingPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActingPayload> ID = CoiPayloads.type("acting");
        public static final StreamCodec<RegistryFriendlyByteBuf, ActingPayload> CODEC =
                CoiPayloads.text(ActingPayload::json,
                        ActingPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ActionBarPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActionBarPayload> ID = CoiPayloads.type("actionbar");
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionBarPayload> CODEC =
                CoiPayloads.text(ActionBarPayload::json,
                        ActionBarPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /**
     * targetUuid is who is drawn, not who receives this: every client in range is told.
     */
    public record AppearancePayload(String targetUuid, String traits) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AppearancePayload> ID = CoiPayloads.type("appearance");

        public static final StreamCodec<RegistryFriendlyByteBuf, AppearancePayload> CODEC =
                CoiPayloads.text2(AppearancePayload::targetUuid,
                        AppearancePayload::traits,
                        AppearancePayload::new);

        @Override
        public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record CogitationPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CogitationPayload> ID = CoiPayloads.type("cogitation");
        public static final StreamCodec<RegistryFriendlyByteBuf, CogitationPayload> CODEC =
                CoiPayloads.text(CogitationPayload::json,
                        CogitationPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ConditionsPayload(String data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConditionsPayload> ID = CoiPayloads.type("conditions");
        public static final StreamCodec<RegistryFriendlyByteBuf, ConditionsPayload> CODEC =
                CoiPayloads.text(ConditionsPayload::data,
                        ConditionsPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record CooldownPayload(String abilityId, int ticks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CooldownPayload> ID = CoiPayloads.type("cooldown");
        public static final StreamCodec<RegistryFriendlyByteBuf, CooldownPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeUtf(value.abilityId());
                    buf.writeInt(value.ticks());
                },
                buf -> new CooldownPayload(buf.readUtf(), buf.readInt())
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /**
     * Drawing magic (feature {@code glyph_canvas}): one JSON string, either
     * {@code {"type":"open",…}} (open the canvas with a session, the layer cap, the limits and
     * the reference glyphs) or {@code {"type":"result",…}} (what the server made of a submit).
     *
     * @see dev.ua.ikeepcalm.coi.domain.glyph.GlyphSheet
     */
    public record GlyphPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GlyphPayload> ID = CoiPayloads.type("glyph");
        public static final StreamCodec<RegistryFriendlyByteBuf, GlyphPayload> CODEC =
                CoiPayloads.text(GlyphPayload::json,
                        GlyphPayload::new,
                        CoiPayloads.MAX_DOCUMENT);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

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

    /**
     * targetUuid is who is drawn, not who receives this: every client in range is told.
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

    public record NotifyPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<NotifyPayload> ID = CoiPayloads.type("notify");
        public static final StreamCodec<RegistryFriendlyByteBuf, NotifyPayload> CODEC =
                CoiPayloads.text(NotifyPayload::json,
                        NotifyPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

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

    public record ServerInfoPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerInfoPayload> ID = CoiPayloads.type("server");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerInfoPayload> CODEC =
                CoiPayloads.text(ServerInfoPayload::json,
                        ServerInfoPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

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

    public record TargetHealthPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TargetHealthPayload> ID = CoiPayloads.type("target");
        public static final StreamCodec<RegistryFriendlyByteBuf, TargetHealthPayload> CODEC =
                CoiPayloads.text(TargetHealthPayload::json,
                        TargetHealthPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /**
     * params {@code stop} or {@code stop,fade=N} removes; {@code effectId = "all"} clears every effect.
     */
    public record VisualEffectPayload(String effectId, String params) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<VisualEffectPayload> ID = CoiPayloads.type("effect");

        public static final StreamCodec<RegistryFriendlyByteBuf, VisualEffectPayload> CODEC =
                CoiPayloads.text2(VisualEffectPayload::effectId,
                        VisualEffectPayload::params,
                        VisualEffectPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
