package dev.ua.ikeepcalm.coi.client.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * What this client tells the server it can render, and the protocol version
 * that shape is pinned to.
 * <p>
 * The string ids are the wire contract — the server maps them onto its own
 * {@code ClientFeature} enum. A server that receives no hello at all falls
 * back to the legacy set (everything here except {@code spirituality_hud}
 * and {@code menu_action}).
 */
public final class ClientFeatures {

    public static final int PROTOCOL = 2;

    public static final List<String> SUPPORTED = List.of(
            "ability_hud",
            "hotkeys",
            "effects",
            "appearance",
            "mythical",
            "conditions",
            "spirituality_hud",
            "menu_action",
            "ability_meta",
            "ability_state",
            "acting_hud",
            "action_bar",
            "target_health",
            "cogitation",
            "notify",
            "character_sheet",
            "resource_bar",
            "menu_ui"
    );

    private ClientFeatures() {
    }

    /**
     * The mod's own version, straight out of fabric.mod.json.
     */
    public static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer("coi-client")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    /**
     * {@code {"modVersion":"…","protocol":2,"features":[…]}} — the body of
     * {@code coi-client:hello}.
     */
    public static String helloJson() {
        JsonObject json = new JsonObject();
        json.addProperty("modVersion", modVersion());
        json.addProperty("protocol", PROTOCOL);
        JsonArray features = new JsonArray();
        SUPPORTED.forEach(features::add);
        json.add("features", features);
        return json.toString();
    }
}
