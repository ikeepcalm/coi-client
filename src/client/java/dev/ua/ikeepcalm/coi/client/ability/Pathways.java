package dev.ua.ikeepcalm.coi.client.ability;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Everything the client knows about a pathway as a pathway: the key it is
 * normalised to, the colour it is drawn in, the emblem glyph it carries and
 * the way its name is spelled for a human.
 * <p>
 * Kept in one class so a pathway can never be gold in one place and grey in
 * another, nor wear two different symbols on two different screens.
 */
public class Pathways {

    private Pathways() {
    }

    private static final Identifier PATHWAY_ICONS_FONT = Identifier.fromNamespaceAndPath("coi-client", "pathway_icons");
    private static final Map<String, String> PATHWAY_ICONS = Map.ofEntries(
            Map.entry("abyss", ""),
            Map.entry("aeon", ""),
            Map.entry("chained", ""),
            Map.entry("darkness", ""),
            Map.entry("death", ""),
            Map.entry("demoness", ""),
            Map.entry("door", ""),
            Map.entry("emperor", ""),
            Map.entry("error", ""),
            Map.entry("fool", ""),
            Map.entry("fortune", ""),
            Map.entry("giant", ""),
            Map.entry("hanged", ""),
            Map.entry("hermit", ""),
            Map.entry("justiciar", ""),
            Map.entry("moon", ""),
            Map.entry("mother", ""),
            Map.entry("paragon", ""),
            Map.entry("patriarch", ""),
            Map.entry("priest", ""),
            Map.entry("sublunary", ""),
            Map.entry("sun", ""),
            Map.entry("tower", ""),
            Map.entry("tyrant", ""),
            Map.entry("visionary", "")
    );

    /**
     * Every pathway that belongs on the title screen's wheel: the emblem keys
     * minus {@code error}, which is the unknown-pathway fallback rather than a
     * pathway anyone walks. Sorted so the ring's order is the same on every
     * launch — {@link #PATHWAY_ICONS} is a {@code Map.ofEntries} and its
     * iteration order is not.
     */
    public static final List<String> RING = PATHWAY_ICONS.keySet().stream()
            .filter(key -> !"error".equals(key))
            .sorted()
            .toList();

    /**
     * The 64x64 emblem artwork for a pathway, for anything that wants the
     * picture rather than the {@link #pathwayEmblem} font glyph — the glyph is
     * a 9px bitmap and turns to mush above its own size.
     * <p>
     * {@link #normalizePathway} folds {@code eternalaeon} onto {@code aeon},
     * but the file on disk is still {@code eternalaeon.png}; the fold is undone
     * here rather than at every call site, and here only.
     */
    public static Identifier emblemTexture(String pathway) {
        String key = normalizePathway(pathway);
        if (!PATHWAY_ICONS.containsKey(key)) return null;
        return Identifier.fromNamespaceAndPath("coi-client",
                "textures/pathways/" + ("aeon".equals(key) ? "eternalaeon" : key) + ".png");
    }

    /** Original-resolution menu artwork; the three extra pathways retain their bundled emblems. */
    public static Identifier qualityEmblemTexture(String pathway) {
        String key = normalizePathway(pathway);
        if (!PATHWAY_ICONS.containsKey(key)) return null;
        String file = switch (key) {
            case "aeon", "patriarch", "sublunary" -> null;
            case "emperor" -> "black-emperor";
            case "giant" -> "twilight-giant";
            case "hanged" -> "hanged-man";
            case "priest" -> "red-priest";
            case "tower" -> "white-tower";
            case "fortune" -> "weel-of-fortune";
            default -> key;
        };
        return file == null ? null : Identifier.fromNamespaceAndPath("coi-client",
                "textures/pathways/quality/" + file + "-min.png");
    }

    /**
     * The pathway's emblem glyph from the {@code pathway_icons} font, or null
     * when that pathway has none. Shared by the tooltip decorators and the
     * character sheet header.
     */
    public static Component pathwayEmblem(String pathway) {
        String icon = PATHWAY_ICONS.get(normalizePathway(pathway));
        if (icon == null) return null;
        return Component.literal(icon).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(PATHWAY_ICONS_FONT)));
    }

    public static String formatPathwayName(String pathway) {
        return Character.toUpperCase(pathway.charAt(0)) + pathway.substring(1);
    }

    /**
     * Lower-cased pathway key, with {@code eternalaeon} folded onto
     * {@code aeon} — the plugin uses both spellings.
     */
    public static String normalizePathway(String pathway) {
        if (pathway == null) return "";
        String key = pathway.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").trim();
        return "eternalaeon".equals(key) ? "aeon" : key;
    }

    /**
     * The single pathway colour table for the whole mod — HUD slots, picker
     * rows, acting bar and the mythical-form burst all read from here, so a
     * pathway can never be gold in one place and grey in another.
     */
    public static int pathwayRgb(String pathway) {
        return switch (normalizePathway(pathway)) {
            case "fool" -> 0xB347CC;
            case "door" -> 0x5B7FE6;
            case "sun" -> 0xFFE55C;
            case "tyrant" -> 0x4AA3FF;
            case "demoness" -> 0xB22222;
            case "priest" -> 0xFF6B35;
            case "error" -> 0x999999;
            case "tower" -> 0x7788AA;
            case "visionary" -> 0x44CCBB;
            case "hanged" -> 0x3A6E4F;
            case "darkness" -> 0x4A1A6E;
            case "death" -> 0xC8D0E8;
            case "giant" -> 0xC08840;
            case "paragon" -> 0xE8E8FF;
            case "hermit" -> 0x8855CC;
            case "fortune" -> 0xFFD700;
            case "chained" -> 0x666677;
            case "abyss" -> 0x551133;
            case "justiciar" -> 0xEEDD88;
            case "emperor" -> 0xDD9922;
            case "moon" -> 0xC7B8E8;
            case "mother" -> 0x7FBF6B;
            case "patriarch" -> 0x3FA9C9;
            case "sublunary" -> 0x9AA7C4;
            case "aeon" -> 0xFFF1C0;
            default -> 0xCCCCFF;
        };
    }
}
