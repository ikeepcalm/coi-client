package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * The little picture in front of a document header, a list row or a grid cell.
 * <p>
 * Four sources: a pathway emblem, a resource-pack item model (the same route
 * ability icons take), a player's head, or one of the mod's own 16px glyphs in
 * {@code textures/gui/icons/}. Anything the client cannot resolve degrades to
 * {@link Kind#NONE} rather than a missing-texture square — an icon is
 * decoration, and a menu that loses one is still usable.
 * <p>
 * {@link Kind#GLYPH} is the only kind whose art ships in this jar, so it is the
 * only one a server can name without knowing what resource pack the player has.
 * Its value is a bare name, never a path: see {@code CoiIcons.glyph}.
 */
public record MenuIcon(Kind kind, String value) {

    public enum Kind {PATHWAY, ITEM, HEAD, GLYPH, ABILITY, NONE}

    public static final MenuIcon NONE = new MenuIcon(Kind.NONE, "");

    public boolean present() {
        return kind != Kind.NONE && !value.isEmpty();
    }

    /**
     * {@code {"kind":"pathway","value":"fool"}}, or nothing usable.
     */
    public static MenuIcon parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) return NONE;
        JsonObject node = element.getAsJsonObject();
        String kind = MenuParser.string(node, "kind", 16).toLowerCase(Locale.ROOT);
        String value = MenuParser.string(node, "value", 128);
        if (value.isEmpty()) return NONE;
        return switch (kind) {
            case "pathway" -> new MenuIcon(Kind.PATHWAY, value);
            case "item" -> new MenuIcon(Kind.ITEM, value);
            case "head" -> new MenuIcon(Kind.HEAD, value);
            case "glyph" -> new MenuIcon(Kind.GLYPH, value);
            case "ability" -> new MenuIcon(Kind.ABILITY, value);
            default -> NONE;
        };
    }
}
