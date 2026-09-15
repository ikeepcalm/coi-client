package dev.ua.ikeepcalm.coi.client.hud.layout.element;

/**
 * The stable id of every positionable HUD element. Ids are never localized:
 * they key the {@code screen.coi.layout_el_<id>} lang strings, the HUD settings
 * screen's <em>Align</em> buttons and the editor's solo mode.
 * <p>
 * They live beside the descriptors rather than in
 * {@code HudElements} so an element and its id can be added in one place;
 * {@code HudElements} re-exports each one, which is the name the rest of the
 * mod uses.
 */
public final class ElementIds {

    /**
     * Group id shared by every {@code slot_N} element; also the id the HUD
     * settings screen's <em>Align</em> button passes in.
     */
    public static final String ABILITY_SLOTS = "ability_slots";
    /**
     * Element ids are {@code slot_1} ... {@code slot_10}, one per ability slot.
     */
    public static final String SLOT_PREFIX = "slot_";
    public static final String CHARACTER_PLATE = "character_plate";
    public static final String BEYONDER_HEALTH = "beyonder_health";
    public static final String MADNESS = "madness";
    public static final String SPIRITUALITY = "spirituality";
    public static final String ACTING = "acting";
    public static final String RESOURCES = "resources";
    public static final String ACTION_BAR = "action_bar";
    public static final String TARGET_HEALTH = "target_health";
    public static final String COGITATION = "cogitation";
    public static final String NOTIFICATIONS = "notifications";

    private ElementIds() {
    }
}
