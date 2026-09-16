package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.AbilityOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * One ability slot. Slots start out sharing the row that {@code hudX} /
 * {@code hudYOffset} / {@code slotSize} describe; dragging one on its
 * own pins it to its own anchor instead ({@code slotPlacements[index]}),
 * which is how a player puts two slots on the left and two on the right.
 */
public final class SlotElement extends AbstractElement {

    /**
     * Where the shared row sits out of the box: 10px in from the left edge,
     * 60px up from the bottom.
     */
    private static final int DEFAULT_ROW_X = 10;
    private static final int DEFAULT_ROW_Y_OFFSET = 60;

    private final int index;

    public SlotElement(int index) {
        super(ElementIds.SLOT_PREFIX + (index + 1));
        this.index = index;
    }

    @Override
    public Component label() {
        return Component.translatable("screen.coi.layout_el_slot", index + 1);
    }

    @Override
    public String group() {
        return ElementIds.ABILITY_SLOTS;
    }

    /**
     * True while this slot still follows the shared row.
     */
    public boolean inRow(HudConfig.HudSettings s) {
        return AbilityOverlay.placement(s, index) == null;
    }

    /**
     * Switched off by {@code showAbilityHud} rather than dropped from
     * {@link dev.ua.ikeepcalm.coi.client.hud.layout.HudElements#all}: an
     * element the player hid stays in the editor under a scrim so it can still
     * be positioned, and only an element something else has <em>superseded</em>
     * (the bars the character plate absorbs) leaves the list outright.
     */
    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.enabled && s.showAbilityHud;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        return AbilityOverlay.slotBounds(index, w, h, s);
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        // bounds starts at the slot box; the name line hangs below it.
        // The box must be measured exactly as AbilityOverlay.slotBounds
        // reports it, or bounds and moveTo stop being inverses and a
        // dragged slot jumps off the cursor
        int box = AbilityOverlay.boxSize(s);
        String[] anchor = new String[1];
        int[] offsets = new int[2];
        LayoutGeometry.applyAnchoredMove(newX, newY, box, box, w, h,
                value -> anchor[0] = value,
                x -> offsets[0] = x,
                y -> offsets[1] = y);
        if (s.slotPlacements == null || index >= s.slotPlacements.length) return;
        s.slotPlacements[index] = new HudConfig.SlotPlacement(anchor[0], offsets[0], offsets[1]);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        AbilityOverlay.renderSlotPreview(ctx, index, w, h, s);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        if (s.slotPlacements != null && index < s.slotPlacements.length) {
            s.slotPlacements[index] = null;
        }
    }

    @Override
    public void resetGroup(HudConfig.HudSettings s) {
        s.hudX = DEFAULT_ROW_X;
        s.hudYOffset = DEFAULT_ROW_Y_OFFSET;
    }
}
