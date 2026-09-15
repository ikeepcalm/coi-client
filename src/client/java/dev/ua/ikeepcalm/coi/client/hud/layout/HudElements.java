package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.ability.AbilityBindings;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.ActingElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.ActionBarElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.BeyonderHealthElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.CharacterPlateElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.CogitationElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.ElementIds;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.MadnessElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.NotificationElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.ResourceElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.SlotElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.SpiritualityElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.TargetHealthElement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Every positionable HUD element, in draw order (bottom-most first). The
 * layout editor walks this list forwards to render and backwards to hit-test,
 * so the thing drawn on top is the thing you grab.
 * <p>
 * This class is the registry and nothing else. Each element's own geometry
 * lives in {@code layout.element}, the anchor/snap arithmetic they share in
 * {@link LayoutGeometry}, and the Ctrl-drag and <em>Align</em> rules in
 * {@link LayoutGroups}.
 */
public final class HudElements {

    /**
     * Group id shared by every {@code slot_N} element; also the id the HUD
     * settings screen's <em>Align</em> button passes in.
     */
    public static final String ABILITY_SLOTS = ElementIds.ABILITY_SLOTS;
    /**
     * Element ids are {@code slot_1} ... {@code slot_10}, one per ability slot.
     */
    public static final String SLOT_PREFIX = ElementIds.SLOT_PREFIX;
    public static final String CHARACTER_PLATE = ElementIds.CHARACTER_PLATE;
    public static final String BEYONDER_HEALTH = ElementIds.BEYONDER_HEALTH;
    public static final String MADNESS = ElementIds.MADNESS;
    public static final String SPIRITUALITY = ElementIds.SPIRITUALITY;
    public static final String ACTING = ElementIds.ACTING;
    public static final String RESOURCES = ElementIds.RESOURCES;
    public static final String ACTION_BAR = ElementIds.ACTION_BAR;
    public static final String TARGET_HEALTH = ElementIds.TARGET_HEALTH;
    public static final String COGITATION = ElementIds.COGITATION;
    public static final String NOTIFICATIONS = ElementIds.NOTIFICATIONS;

    /**
     * One per ability slot, in slot order. Only the first
     * {@code activeAbilitySlots} of them are ever listed.
     */
    private static final List<HudElement> SLOTS = buildSlots();

    /**
     * Everything that is not an ability slot, in draw order.
     */
    private static final List<HudElement> BARS = List.of(
            new CharacterPlateElement(),
            new BeyonderHealthElement(),
            new MadnessElement(),
            new SpiritualityElement(),
            new ActingElement(),
            new ResourceElement(),
            new ActionBarElement(),
            new TargetHealthElement(),
            new CogitationElement(),
            new NotificationElement());

    private HudElements() {
    }

    private static List<HudElement> buildSlots() {
        List<HudElement> slots = new ArrayList<>();
        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            slots.add(new SlotElement(i));
        }
        return List.copyOf(slots);
    }

    /**
     * Every positionable element for these settings: the ability slots the
     * player has switched on, then the bars and overlays. Recomputed whenever
     * the editor opens, since {@code activeAbilitySlots} decides its length.
     */
    public static List<HudElement> all(HudConfig.HudSettings s) {
        int count = Math.clamp(s.activeAbilitySlots, 1, SLOTS.size());
        List<HudElement> elements = new ArrayList<>(SLOTS.subList(0, count));
        for (HudElement element : BARS) {
            if (!supersededByPlate(element.id(), s)) elements.add(element);
        }
        return List.copyOf(elements);
    }

    /**
     * A bar the character plate has taken over is dropped from the editor
     * entirely rather than left in it as a hidden ghost. The editor's usual
     * treatment of an invisible element — draw it under a scrim so it can
     * still be positioned — is right for something the player switched off,
     * and wrong for something that no longer exists as a separate element at
     * all. The settings screen hides the matching rows for the same reason.
     */
    private static boolean supersededByPlate(String id, HudConfig.HudSettings s) {
        if (!s.showCharacterPlate) return false;
        return MADNESS.equals(id) || ACTING.equals(id) || RESOURCES.equals(id);
    }

    /**
     * The elements an <em>Align</em> button opens: a whole group when the id
     * names one, otherwise the single element with that id. Empty for an id
     * nothing answers to.
     */
    public static List<HudElement> soloSet(String id, HudConfig.HudSettings s) {
        return LayoutGroups.soloSet(id, all(s));
    }

    /**
     * Label for a solo set's title line: the group's own name when the id is a
     * group, otherwise the element's.
     */
    public static Component groupLabel(String id, List<HudElement> set) {
        return LayoutGroups.groupLabel(id, set);
    }

    /**
     * Ctrl-drag: shifts every member of {@code groupId} by the same delta.
     */
    public static void moveGroupBy(String groupId, List<HudElement> members, int dx, int dy,
                                   int w, int h, HudConfig.HudSettings s) {
        LayoutGroups.moveGroupBy(groupId, members, dx, dy, w, h, s);
    }

    /**
     * @return the element with this id, or {@code null} for an unknown one
     */
    public static HudElement byId(String id) {
        if (id == null) return null;
        for (HudElement element : SLOTS) {
            if (element.id().equals(id)) return element;
        }
        for (HudElement element : BARS) {
            if (element.id().equals(id)) return element;
        }
        return null;
    }
}
