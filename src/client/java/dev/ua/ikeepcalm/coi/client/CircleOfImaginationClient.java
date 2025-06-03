package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CircleOfImaginationClient implements ClientModInitializer {

    public static final String MOD_ID = "coi";

    private static final List<String> availableAbilities = new ArrayList<>();
    private static String[] boundAbilities = new String[3];

    private static KeyBinding ability1Key;
    private static KeyBinding ability2Key;
    private static KeyBinding ability3Key;

    private static final boolean[] keyPressed = new boolean[3];

    @Override
    public void onInitializeClient() {
        boundAbilities = AbilityConfig.loadBindings();
        registerPayloads();
        registerKeybindings();
        registerNetworking();
        registerTickHandler();
    }

    private void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(AbilityUsePayload.ID, AbilityUsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityRequestPayload.ID, AbilityRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityListPayload.ID, AbilityListPayload.CODEC);
    }

    private void registerKeybindings() {
        ability1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.coi.abilities"
        ));

        ability2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.coi.abilities"
        ));

        ability3Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coi.ability3",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.coi.abilities"
        ));
    }

    private void registerNetworking() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.send(new AbilityRequestPayload("request_abilities"));
        });

        ClientPlayNetworking.registerGlobalReceiver(AbilityListPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                availableAbilities.clear();
                availableAbilities.addAll(payload.abilities());
            });
        });
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            handleKeyPress(0, ability1Key);
            handleKeyPress(1, ability2Key);
            handleKeyPress(2, ability3Key);
        });
    }

    private void handleKeyPress(int index, KeyBinding key) {
        if (key.isPressed() && !keyPressed[index]) {
            keyPressed[index] = true;
            if (boundAbilities[index] != null) {
                sendAbilityUse(boundAbilities[index]);
            }
        } else if (!key.isPressed()) {
            keyPressed[index] = false;
        }
    }

    private void sendAbilityUse(String abilityId) {
        ClientPlayNetworking.send(new AbilityUsePayload(abilityId));
    }

    public static List<String> getAvailableAbilities() {
        return new ArrayList<>(availableAbilities);
    }

    public static String getBoundAbility(int slot) {
        return boundAbilities[slot];
    }

    public static void setBoundAbility(int slot, String abilityId) {
        if (slot >= 0 && slot < 3) {
            boundAbilities[slot] = abilityId;
            AbilityConfig.saveBindings(boundAbilities);
        }
    }

    public record AbilityUsePayload(String abilityId) implements CustomPayload {
        public static final CustomPayload.Id<AbilityUsePayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "abilities"));

        public static void write(RegistryByteBuf buf, AbilityUsePayload payload) { buf.writeString(payload.abilityId()); }
        public static AbilityUsePayload read(RegistryByteBuf buf) {
            return new AbilityUsePayload(buf.readString());
        }

        public static final PacketCodec<RegistryByteBuf, AbilityUsePayload> CODEC = PacketCodec.ofStatic(
                AbilityUsePayload::write,
                AbilityUsePayload::read
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AbilityRequestPayload(String request) implements CustomPayload {
        public static final CustomPayload.Id<AbilityRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "ability_list"));

        public static void write(RegistryByteBuf buf, AbilityRequestPayload payload) {
            buf.writeString(payload.request());
        }

        public static AbilityRequestPayload read(RegistryByteBuf buf) {
            return new AbilityRequestPayload(buf.readString());
        }

        public static final PacketCodec<RegistryByteBuf, AbilityRequestPayload> CODEC = PacketCodec.ofStatic(
                AbilityRequestPayload::write,
                AbilityRequestPayload::read
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AbilityListPayload(List<String> abilities) implements CustomPayload {
        public static final CustomPayload.Id<AbilityListPayload> ID = new CustomPayload.Id<>(Identifier.of(CircleOfImaginationClient.MOD_ID, "ability_list"));


        public static final PacketCodec<RegistryByteBuf, AbilityListPayload> CODEC = PacketCodec.ofStatic(
                AbilityListPayload::write,
                AbilityListPayload::read
        );

        public static void write(RegistryByteBuf buf, AbilityListPayload payload) {
            buf.writeInt(payload.abilities().size());
            for (String ability : payload.abilities()) {
                buf.writeString(ability);
            }
        }

        public static AbilityListPayload read(RegistryByteBuf buf) {
            int count = buf.readInt();
            List<String> abilities = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                abilities.add(buf.readString());
            }
            return new AbilityListPayload(abilities);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

}