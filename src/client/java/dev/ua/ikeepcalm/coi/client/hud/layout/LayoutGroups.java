package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.element.SlotElement;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Everything the editor does to a {@link HudElement#group() group} rather than
 * to one element: resolving an <em>Align</em> id into the set solo mode shows,
 * naming that set, and the Ctrl-drag that moves a whole family at once.
 * <p>
 * The ability slots are the only group today, and the only reason this is not
 * three lines: a slot still sitting in the shared row has to move <em>with the
 * row</em> rather than be pinned where it happens to be standing.
 */
public class LayoutGroups {

    private LayoutGroups() {
    }

    /**
     * The elements an <em>Align</em> button opens: a whole group when the id
     * names one, otherwise the single element with that id. Empty for an id
     * nothing answers to.
     */
    public static List<HudElement> soloSet(String id, List<HudElement> candidates) {
        if (id == null) return List.of();
        List<HudElement> found = new ArrayList<>();
        for (HudElement element : candidates) {
            if (id.equals(element.group()) || id.equals(element.id())) found.add(element);
        }
        return List.copyOf(found);
    }

    /**
     * Label for a solo set's title line: the group's own name when the id is a
     * group, otherwise the element's.
     */
    public static Component groupLabel(String id, List<HudElement> set) {
        if (!set.isEmpty() && id.equals(set.getFirst().group())) {
            return Component.translatable("screen.coi.layout_el_" + id);
        }
        return set.isEmpty() ? Component.empty() : set.getFirst().label();
    }

    /**
     * Ctrl-drag: shifts every member of {@code groupId} by the same delta.
     * <p>
     * Ability slots still sitting in the shared row move with the row itself
     * (once, via {@code hudX}/{@code hudYOffset}) rather than being pinned to
     * their current spot; the ones the player pulled out keep their own anchor
     * and just take the delta.
     */
    public static void moveGroupBy(String groupId, List<HudElement> members, int dx, int dy,
                                   int w, int h, HudConfig.HudSettings s) {
        if (groupId == null) return;
        if (rowFollows(groupId, members, s)) {
            AbilityOverlay.shiftRow(dx, dy, h, s);
        }
        for (HudElement element : members) {
            if (!groupId.equals(element.group())) continue;
            if (element instanceof SlotElement slot && slot.inRow(s)) continue;
            int[] box = element.bounds(w, h, s);
            element.moveTo(Mth.clamp(box[0] + dx, 0, Math.max(0, w - box[2])),
                    Mth.clamp(box[1] + dy, 0, Math.max(0, h - box[3])), w, h, s);
        }
    }

    /**
     * Whether any member of the group is still riding the shared ability row —
     * in which case the row's own origin takes the delta, exactly once.
     */
    private static boolean rowFollows(String groupId, List<HudElement> members, HudConfig.HudSettings s) {
        for (HudElement element : members) {
            if (groupId.equals(element.group()) && element instanceof SlotElement slot && slot.inRow(s)) {
                return true;
            }
        }
        return false;
    }
}
