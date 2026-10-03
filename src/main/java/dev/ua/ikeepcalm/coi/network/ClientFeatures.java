package dev.ua.ikeepcalm.coi.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/**
 * The string ids are the wire contract: the server maps them onto its own {@code ClientFeature} enum.
 */
public class ClientFeatures {

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
            "menu_ui",
            "menu_specimen", "menu_archive", "ability_categories", "ability_manual",
            // Both name further ids on the existing "effects" channel rather than a new one,
            // so a server that does not know them simply never sends them.
            "ceremony",
            "impact_frame",
            "glyph_canvas"
    );

    private ClientFeatures() {
    }

    public static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer("coi-client")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

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
