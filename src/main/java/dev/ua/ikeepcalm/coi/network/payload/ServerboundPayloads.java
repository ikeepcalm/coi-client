package dev.ua.ikeepcalm.coi.network.payload;

import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.config.HudConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Every Client -> Server payload. Body shapes: docs/NETWORK_PROTOCOL.md.
 */
public final class ServerboundPayloads {

    private ServerboundPayloads() {
    }

    public record AbilityUsePayload(String abilityId, String action) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AbilityUsePayload> ID = CoiPayloads.type("use");

        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityUsePayload> CODEC =
                CoiPayloads.text2(AbilityUsePayload::abilityId,
                        AbilityUsePayload::action,
                        AbilityUsePayload::new);

        public AbilityUsePayload(String abilityId) {
            this(abilityId, "execute");
        }

        @Override
        public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /**
     * Atomic category selection and normal cast; separate channel prevents legacy miscasts.
     */
    public record AbilityCategoryUsePayload(String abilityId, String category) implements CustomPacketPayload {
        public static final Type<AbilityCategoryUsePayload> ID = CoiPayloads.type("use_category");
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCategoryUsePayload> CODEC =
                CoiPayloads.text2(AbilityCategoryUsePayload::abilityId, AbilityCategoryUsePayload::category,
                        AbilityCategoryUsePayload::new);

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record AbilityRequestPayload() implements CustomPacketPayload {
        public static final AbilityRequestPayload INSTANCE = new AbilityRequestPayload();
        public static final CustomPacketPayload.Type<AbilityRequestPayload> ID = CoiPayloads.type("request");
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequestPayload> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record HelloPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> ID = CoiPayloads.type("hello");
        public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC =
                CoiPayloads.text(HelloPayload::json,
                        HelloPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ActionPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActionPayload> ID = CoiPayloads.type("action");
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
                CoiPayloads.text(ActionPayload::json,
                        ActionPayload::new);

        public static ActionPayload of(String action) {
            JsonObject json = new JsonObject();
            json.addProperty("action", action);
            return new ActionPayload(json.toString());
        }

        /**
         * {@code ui} is a request, not a decision: it only reports {@code useServerMenus}.
         * It used to be suppressed on any server advertising {@code menu_archive} — which is
         * every live one — so the checkbox behind it did nothing. The server owns the final say.
         */
        public static ActionPayload ofOpen(String target) {
            JsonObject json = new JsonObject();
            json.addProperty("action", "open");
            json.addProperty("target", target);
            json.addProperty("ui", HudConfig.getSettings().useServerMenus ? "server" : "client");
            return new ActionPayload(json.toString());
        }

        public static ActionPayload of(String action, String key, String value) {
            JsonObject json = new JsonObject();
            json.addProperty("action", action);
            json.addProperty(key, value);
            return new ActionPayload(json.toString());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record MenuActionPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MenuActionPayload> ID = CoiPayloads.type("menu_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC =
                CoiPayloads.text(MenuActionPayload::json,
                        MenuActionPayload::new,
                        CoiPayloads.MAX_ACTION);

        /**
         * The only two action ids the client mints; every other one is the server's own token.
         */
        public static final String BACK = "__back";
        public static final String CLOSE = "__close";

        public static MenuActionPayload of(String session, int version, String action, String value) {
            JsonObject json = new JsonObject();
            json.addProperty("session", session);
            json.addProperty("version", version);
            json.addProperty("action", action);
            if (value != null) json.addProperty("value", value);
            return new MenuActionPayload(json.toString());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /**
     * Drawn spell: {@code {"session","dict","name","layers":[{"strokes":[{"p":[x,y,…],"t0","t1"}]}]}}.
     * Canvas units are whole numbers {@code 0..1023}, y down. The server re-reads
     * every stroke, so nothing here is trusted.
     */
    public record GlyphSubmitPayload(String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GlyphSubmitPayload> ID = CoiPayloads.type("glyph_submit");
        public static final StreamCodec<RegistryFriendlyByteBuf, GlyphSubmitPayload> CODEC =
                CoiPayloads.text(GlyphSubmitPayload::json,
                        GlyphSubmitPayload::new,
                        CoiPayloads.MAX_ACTION);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
