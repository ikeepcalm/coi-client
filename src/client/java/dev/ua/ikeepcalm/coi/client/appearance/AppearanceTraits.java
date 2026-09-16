package dev.ua.ikeepcalm.coi.client.appearance;

import dev.ua.ikeepcalm.coi.client.appearance.renderers.BeastTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.ClawTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.DemonessEarsRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.FemaleTraitsRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.HairTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.HornsTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.MushroomTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.renderers.SkinTraitRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registry of every passive appearance trait this client can render, plus the
 * mutually-exclusive families that stop overlapping cosmetics from z-fighting:
 * for each family only the highest-priority active trait actually draws.
 *
 * <p>Priority chains (first entry wins):</p>
 * <ul>
 *     <li>Body: mother → moon → chaos primogenitor → demoness</li>
 *     <li>Hair: long brown → long black → silver → red → blue → black</li>
 *     <li>Chained: wraith → werewolf → zombie physique; Claws: corrosive → werewolf</li>
 *     <li>Skin: devil → stone → wood</li>
 * </ul>
 *
 * <p>Trait ids are the opaque strings the COI server sends via {@code coi-client:appearance};
 * unknown ids are simply never rendered.</p>
 */
public final class AppearanceTraits {

    public record Family(String id, List<String> priorityOrder) {
    }

    public record TraitInfo(String displayName, Family family, AppearanceTraitRenderer renderer) {

        public String id() {
            return renderer.traitId();
        }
    }

    public static final Family BODY = new Family("body",
            List.of("mother_traits", "moon_traits", "chaos_traits", "female_traits"));
    public static final Family HAIR = new Family("hair",
            List.of("long_brown_hair", "long_black_hair", "silver_hair", "red_hair", "blue_hair", "black_hair"));
    public static final Family CHAINED = new Family("chained",
            List.of("wraith_traits", "werewolf_traits", "zombie_traits"));
    public static final Family CLAWS = new Family("claws",
            List.of("corrosive_claws", "werewolf_claws"));
    public static final Family SKIN = new Family("skin",
            List.of("devil_skin", "stone_skin", "wood_skin"));

    /** Families gated by the "Body enhancements" visibility switch. */
    public static final Set<String> BODY_ALTERING_FAMILIES = Set.of(
            BODY.id(), SKIN.id(), CHAINED.id());

    public static final List<Family> FAMILIES = List.of(
            BODY, HAIR, CHAINED, CLAWS, SKIN);

    private static final Map<String, Family> FAMILY_BY_TRAIT = new HashMap<>();

    static {
        for (Family family : FAMILIES) {
            for (String traitId : family.priorityOrder()) {
                FAMILY_BY_TRAIT.put(traitId, family);
            }
        }
    }

    /** Authoritative trait registry. Display names are debug-only and intentionally not localized. */
    public static final List<TraitInfo> TRAITS = List.of(
            new TraitInfo("Demoness Figure", BODY, new FemaleTraitsRenderer("female_traits", 1.08f)),
            new TraitInfo("Mother Figure", BODY, new FemaleTraitsRenderer("mother_traits", 1.38f)),
            new TraitInfo("Moon Figure", BODY, new FemaleTraitsRenderer("moon_traits", 1.28f)),
            new TraitInfo("Chaos Figure", BODY, new FemaleTraitsRenderer("chaos_traits", 1.18f)),
            new TraitInfo("Cat Ears", null, new DemonessEarsRenderer()),
            new TraitInfo("Long Brown Hair", HAIR,
                    new HairTraitRenderer("long_brown_hair", HairTraitRenderer.Style.LAYERED, 0.42f, 0.22f, 0.11f)),
            new TraitInfo("Long Black Hair", HAIR,
                    new HairTraitRenderer("long_black_hair", HairTraitRenderer.Style.TIED, 0.025f, 0.018f, 0.03f)),
            new TraitInfo("Silver Hair", HAIR,
                    new HairTraitRenderer("silver_hair", HairTraitRenderer.Style.SWEPT, 0.72f, 0.76f, 0.82f)),
            new TraitInfo("Red Hair", HAIR,
                    new HairTraitRenderer("red_hair", HairTraitRenderer.Style.TOUSLED, 0.58f, 0.025f, 0.025f)),
            new TraitInfo("Blue Hair", HAIR,
                    new HairTraitRenderer("blue_hair", HairTraitRenderer.Style.WAVY, 0.035f, 0.23f, 0.62f)),
            new TraitInfo("Black Hair", HAIR,
                    new HairTraitRenderer("black_hair", HairTraitRenderer.Style.CROPPED, 0.018f, 0.014f, 0.025f)),
            new TraitInfo("Werewolf Traits", CHAINED, new BeastTraitRenderer("werewolf_traits")),
            new TraitInfo("Werewolf Claws", CLAWS,
                    new ClawTraitRenderer("werewolf_claws", ClawTraitRenderer.Style.WEREWOLF)),
            new TraitInfo("Wraith Traits", CHAINED,
                    new SkinTraitRenderer("wraith_traits", SkinTraitRenderer.Style.WRAITH)),
            new TraitInfo("Zombie Traits", CHAINED,
                    new SkinTraitRenderer("zombie_traits", SkinTraitRenderer.Style.ZOMBIE)),
            new TraitInfo("Devil Armor Skin", SKIN,
                    new SkinTraitRenderer("devil_skin", SkinTraitRenderer.Style.DEVIL_ARMOR)),
            new TraitInfo("Wood Skin", SKIN, new SkinTraitRenderer("wood_skin", SkinTraitRenderer.Style.WOOD)),
            new TraitInfo("Stone Skin", SKIN, new SkinTraitRenderer("stone_skin", SkinTraitRenderer.Style.STONE)),
            new TraitInfo("Corrosive Claws", CLAWS,
                    new ClawTraitRenderer("corrosive_claws", ClawTraitRenderer.Style.CORROSIVE)),
            new TraitInfo("Abyss Horns", null, new HornsTraitRenderer()),
            new TraitInfo("Head Mushroom", null, new MushroomTraitRenderer())
    );

    private static final Map<String, AppearanceTraitRenderer> RENDERER_BY_TRAIT = new HashMap<>();

    static {
        for (TraitInfo trait : TRAITS) {
            if (!Objects.equals(FAMILY_BY_TRAIT.get(trait.id()), trait.family())) {
                throw new IllegalStateException("Appearance trait family mismatch: " + trait.id());
            }
            AppearanceTraitRenderer previous = RENDERER_BY_TRAIT.put(trait.id(), trait.renderer());
            if (previous != null) {
                throw new IllegalStateException("Duplicate appearance trait id: " + trait.id());
            }
        }
    }

    private AppearanceTraits() {
    }

    public static Family familyOf(String traitId) {
        return FAMILY_BY_TRAIT.get(traitId);
    }

    public static int traitCount() {
        return TRAITS.size();
    }

    /**
     * Resolves which renderers draw for a player carrying {@code activeTraits}: every
     * independent trait passes through, and within each family only the highest-priority
     * active trait is kept. Order follows {@link #TRAITS} for stable debug layout.
     */
    public static List<AppearanceTraitRenderer> resolve(Set<String> activeTraits) {
        if (activeTraits.isEmpty()) {
            return List.of();
        }

        List<AppearanceTraitRenderer> result = new ArrayList<>();

        Map<String, String> winnerByFamily = new HashMap<>();
        for (Family family : FAMILIES) {
            for (String candidate : family.priorityOrder()) {
                if (activeTraits.contains(candidate)) {
                    winnerByFamily.put(family.id(), candidate);
                    break;
                }
            }
        }

        for (TraitInfo trait : TRAITS) {
            AppearanceTraitRenderer renderer = trait.renderer();
            String traitId = renderer.traitId();
            if (!activeTraits.contains(traitId)) {
                continue;
            }
            Family family = FAMILY_BY_TRAIT.get(traitId);
            if (family != null && !traitId.equals(winnerByFamily.get(family.id()))) {
                continue;
            }
            result.add(renderer);
        }
        return result;
    }

    public static AppearanceTraitRenderer rendererFor(String traitId) {
        return RENDERER_BY_TRAIT.get(traitId);
    }
}
