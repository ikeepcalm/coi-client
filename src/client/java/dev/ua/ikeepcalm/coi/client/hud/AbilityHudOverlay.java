package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AbilityHudOverlay {

    private static final Identifier ABILITY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "abilities");
    private static AbilitySlotWidget[] abilitySlots;
    /**
     * Never-bound widgets used for the layout editor's preview, so the real
     * slots keep their cooldowns and cast animations untouched.
     */
    private static AbilitySlotWidget[] previewSlots;

    /**
     * Height of the ability-name line drawn under each slot.
     */
    public static final int NAME_LINE = 13;

    /**
     * Gap between two slots in the shared row at {@link
     * HudConfig#BASE_SLOT_SIZE}; scales with the box. 40 + 10 reproduces the
     * old default pair of {@code slotSize 40} / {@code slotSpacing 50}.
     */
    private static final int SLOT_GAP = 10;

    public static void initialize() {
        int maxAbilities = CircleOfImaginationClient.MAX_ABILITIES;
        abilitySlots = new AbilitySlotWidget[maxAbilities];

        for (int i = 0; i < maxAbilities; i++) {
            abilitySlots[i] = new AbilitySlotWidget(i);
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ABILITY_LAYER, AbilityHudOverlay::renderAbilities);
    }

    private static void renderAbilities(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.gui.hud.isHidden() || HudLayout.editing() || !settings.enabled) {
            return;
        }

        float tickDelta = 1.0f;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Only render active slots that have abilities bound to them
        int activeSlots = CircleOfImaginationClient.getActiveAbilitySlots();
        List<AbilitySlotWidget> visible = new ArrayList<>();
        List<Integer> visibleIndices = new ArrayList<>();
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.getSlotIndex() < activeSlots && !abilitySlot.isEmpty()) {
                visible.add(abilitySlot);
                visibleIndices.add(abilitySlot.getSlotIndex());
            }
        }

        HudGaslight.update(visibleIndices);

        for (AbilitySlotWidget abilitySlot : visible) {
            int index = abilitySlot.getSlotIndex();
            // At high madness two slots may briefly trade places: each one is
            // drawn at the other's origin
            int swapped = HudGaslight.swappedWith(index);
            if (swapped >= 0 && swapped < abilitySlots.length && !abilitySlots[swapped].isEmpty()) {
                index = swapped;
            }

            int[] origin = slotOrigin(index, screenWidth, screenHeight, settings);
            // The widget draws at its natural size; the pose grows the whole
            // slot about its own origin, so the keybind chip, the name and the
            // cooldown text scale with the box instead of staying font-sized
            HudScale.push(context, origin[0], origin[1], slotScale(settings));
            abilitySlot.render(context, origin[0], origin[1], HudConfig.BASE_SLOT_SIZE, tickDelta);
            HudScale.pop(context);
        }
    }

    /**
     * Where one ability slot's box goes - the single source of truth the HUD,
     * the layout editor and the tour spotlight all read.
     * <p>
     * A slot with no {@link HudConfig.SlotPlacement} sits in the shared row;
     * the rest resolve their own anchor exactly like a bar does. Either way the
     * box is clamped on screen, so a narrow window or a stale offset can never
     * push a slot out of sight.
     */
    public static int[] slotOrigin(int index, int screenW, int screenH, HudConfig.HudSettings s) {
        int box = boxSize(s);
        HudConfig.SlotPlacement placement = placement(s, index);
        int x;
        int y;
        if (placement == null) {
            int[] row = rowOrigin(screenH, s);
            x = row[0] + index * rowStep(s);
            y = row[1];
        } else {
            int[] pos = HudAnchor.parse(placement.anchor())
                    .resolve(screenW, screenH, box, placement.x(), placement.y(), placement.y());
            x = pos[0];
            y = pos[1];
        }
        return new int[]{Mth.clamp(x, 0, Math.max(0, screenW - box)),
                Mth.clamp(y, 0, Math.max(0, screenH - box))};
    }

    /**
     * The ability HUD's pose scale, derived from the single size knob the
     * player has: a {@code slotSize} of {@link HudConfig#BASE_SLOT_SIZE} is
     * 1.0, and the slider's range maps exactly onto
     * {@code MIN_ELEMENT_SCALE}..{@code MAX_ELEMENT_SCALE}.
     */
    public static float slotScale(HudConfig.HudSettings s) {
        return HudScale.clamp(s.slotSize / (float) HudConfig.BASE_SLOT_SIZE);
    }

    /**
     * The drawn size of one slot box. Equal to {@code slotSize} by definition —
     * the widget draws at the base size under {@link #slotScale} — but read it
     * from here so the relationship stays in one place.
     */
    public static int boxSize(HudConfig.HudSettings s) {
        return HudScale.size(HudConfig.BASE_SLOT_SIZE, slotScale(s));
    }

    /**
     * Distance between two slots in the shared row. There is no separate
     * spacing setting any more: the gap is a fixed share of the box, so a
     * bigger slot can never overlap its neighbour and a smaller one never
     * strands itself in whitespace.
     */
    public static int rowStep(HudConfig.HudSettings s) {
        return boxSize(s) + HudScale.size(SLOT_GAP, slotScale(s));
    }

    /**
     * The per-slot override, or {@code null} when the slot follows the row.
     */
    public static HudConfig.SlotPlacement placement(HudConfig.HudSettings s, int index) {
        if (s.slotPlacements == null || index < 0 || index >= s.slotPlacements.length) return null;
        return s.slotPlacements[index];
    }

    /**
     * {@code {x, y, width, height}} of one slot, including the ability-name
     * line drawn under the box when that is switched on. The keybind chip is
     * drawn inside the box.
     */
    public static int[] slotBounds(int index, int screenW, int screenH, HudConfig.HudSettings s) {
        int[] origin = slotOrigin(index, screenW, screenH, s);
        int box = boxSize(s);
        return new int[]{origin[0], origin[1], box,
                box + (s.showAbilityNames ? HudScale.size(NAME_LINE, slotScale(s)) : 0)};
    }

    /**
     * Union of every active slot's {@link #slotBounds} - what the tour
     * spotlights now that slots can be scattered.
     */
    public static int[] rowBounds(int screenW, int screenH, HudConfig.HudSettings s) {
        int count = Math.clamp(s.activeAbilitySlots, 1, CircleOfImaginationClient.MAX_ABILITIES);
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            int[] box = slotBounds(i, screenW, screenH, s);
            x0 = Math.min(x0, box[0]);
            y0 = Math.min(y0, box[1]);
            x1 = Math.max(x1, box[0] + box[2]);
            y1 = Math.max(y1, box[1] + box[3]);
        }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /**
     * Nudges the shared row by a screen-pixel delta. The offsets are plain
     * screen pixels, so the delta goes in untouched — {@code slotSize} grows
     * the slots about this origin and leaves the origin itself alone.
     */
    public static void shiftRow(int dx, int dy, int screenH, HudConfig.HudSettings s) {
        s.hudX = Math.max(0, s.hudX + dx);
        s.hudYOffset = Mth.clamp(s.hudYOffset - dy, 0, screenH);
    }

    /**
     * Top-left corner of the slot row, in screen pixels. The slot scale is
     * deliberately absent: it grows the slots themselves (about this very
     * point, see {@link HudScale}) rather than displacing the row, so the
     * offsets the player dragged out in the layout editor mean what they say.
     */
    public static int[] rowOrigin(int screenH, HudConfig.HudSettings s) {
        return new int[]{s.hudX, screenH - s.hudYOffset};
    }

    /**
     * One empty slot frame at its configured position - what the layout editor
     * drags around when the player has nothing bound yet either.
     */
    public static void renderSlotPreview(GuiGraphicsExtractor ctx, int index, int screenW, int screenH,
                                         HudConfig.HudSettings s) {
        if (previewSlots == null) {
            previewSlots = new AbilitySlotWidget[CircleOfImaginationClient.MAX_ABILITIES];
            for (int i = 0; i < previewSlots.length; i++) {
                previewSlots[i] = new AbilitySlotWidget(i);
            }
        }
        if (index < 0 || index >= previewSlots.length) return;
        int[] origin = slotOrigin(index, screenW, screenH, s);
        HudScale.push(ctx, origin[0], origin[1], slotScale(s));
        previewSlots[index].render(ctx, origin[0], origin[1], HudConfig.BASE_SLOT_SIZE, 1.0f);
        HudScale.pop(ctx);
    }

    /**
     * Runs {@code action} on every slot bound to this ability — the same
     * ability can legitimately sit in several slots (e.g. once as a normal
     * cast and once as its left-click variant), and all of them have to react.
     */
    private static void forEachSlot(String abilityId, Consumer<AbilitySlotWidget> action) {
        if (abilitySlots == null || abilityId == null) return;
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.hasAbility(abilityId)) {
                action.accept(abilitySlot);
            }
        }
    }

    /**
     * Reactive cooldown: the server tells us an ability just went on cooldown
     * and the slot times it out locally.
     */
    public static void setCooldown(String abilityId, int cooldownTicks) {
        forEachSlot(abilityId, slot -> {
            if (!slot.isOnCooldown()) {
                slot.setCooldown(cooldownTicks);
            }
        });
    }

    /**
     * Authoritative cooldown from the v2 ability list: remaining out of a max
     * that is known before the ability has ever been cast.
     */
    public static void setCooldown(String abilityId, int remainingTicks, int maxTicks) {
        forEachSlot(abilityId, slot -> slot.setCooldown(remainingTicks, maxTicks));
    }

    /**
     * Toggle state for activated/passive abilities ({@code coi-client:state}).
     */
    public static void setActive(String abilityId, boolean active) {
        forEachSlot(abilityId, slot -> slot.setToggled(active));
    }

    /**
     * Current category of a category-switching ability, shown next to its name.
     */
    public static void setCategoryLabel(String abilityId, String label) {
        forEachSlot(abilityId, slot -> slot.setCategoryLabel(label));
    }

    public static void onAbilityCast(String abilityId) {
        forEachSlot(abilityId, AbilitySlotWidget::triggerCastAnimation);
    }

    public static void updateAbilitySlot(int slot, String abilityId) {
        if (abilitySlots != null && slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot].setAbility(abilityId);
        }
    }
}
