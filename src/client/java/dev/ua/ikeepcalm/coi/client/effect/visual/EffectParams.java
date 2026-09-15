package dev.ua.ikeepcalm.coi.client.effect.visual;

import java.util.function.BiConsumer;

/**
 * Splitter for the {@code key=value,key=value} param strings that arrive with
 * {@code coi-client:effect}. Every effect parses the same shape, so the
 * splitting lives here and each effect only supplies a switch over the keys it
 * understands.
 */
public class EffectParams {

    private EffectParams() {
    }

    /**
     * Splits {@code params} on commas, then on the first {@code =}, and hands
     * each trimmed key/value pair to {@code handler}. Entries without an
     * {@code =} are skipped; a null or blank string yields no calls at all,
     * leaving the effect's defaults in place.
     */
    static void forEach(String params, BiConsumer<String, String> handler) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) handler.accept(kv[0].trim(), kv[1].trim());
        }
    }
}
