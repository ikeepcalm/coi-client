package dev.ua.ikeepcalm.coi.client.network.payload;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The shape every {@code coi-client} payload is built from.
 * <p>
 * Almost all of them are one or two strings under a {@code coi-client:…}
 * identifier, and the plugin's own payload classes are their exact mirror — so
 * the namespace, the length caps and the read/write pair are spelled out once
 * here rather than 22 times. A payload class is then just its record
 * components, its id and its codec, which is what makes a mismatch with the
 * server visible at a glance.
 * <p>
 * The caps matter: {@code writeUtf} defaults to 32767 characters, which a full
 * ability list or a whole menu document blows straight through, and both ends
 * have to agree on the number or the packet is rejected mid-flight.
 */
public class CoiPayloads {

    /**
     * The plugin-message namespace both repos use.
     */
    public static final String NAMESPACE = "coi-client";

    /**
     * 1 MiB, for the channels that carry a whole document: the v2 ability
     * list, the character sheet and a menu screen.
     */
    public static final int MAX_DOCUMENT = 1_048_576;

    /**
     * 32 KiB, for a click going back to the server — far more than an action
     * id and a value need, and still well under the default cap.
     */
    public static final int MAX_ACTION = 32_768;

    private CoiPayloads() {
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }

    /**
     * One string, at the default {@code writeUtf} cap.
     */
    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text(
            Function<T, String> getter, Function<String, T> factory) {
        return StreamCodec.ofMember(
                (value, buf) -> buf.writeUtf(getter.apply(value)),
                buf -> factory.apply(buf.readUtf()));
    }

    /**
     * One string at an explicit cap — {@link #MAX_DOCUMENT} or
     * {@link #MAX_ACTION}. The server writes with the same number.
     */
    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text(
            Function<T, String> getter, Function<String, T> factory, int max) {
        return StreamCodec.ofMember(
                (value, buf) -> buf.writeUtf(getter.apply(value), max),
                buf -> factory.apply(buf.readUtf(max)));
    }

    /**
     * Two strings, written and read in the order given.
     */
    static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> text2(
            Function<T, String> first, Function<T, String> second, BiFunction<String, String, T> factory) {
        return StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeUtf(first.apply(value));
                    buf.writeUtf(second.apply(value));
                },
                buf -> factory.apply(buf.readUtf(), buf.readUtf()));
    }
}
