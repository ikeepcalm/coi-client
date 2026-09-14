package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ActingHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ActionBarHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.CharacterPlateOverlay;
import dev.ua.ikeepcalm.coi.client.hud.CogitationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.MadnessHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.NotificationOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ResourceHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.SpiritualityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.TargetHealthOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Every positionable HUD element, in draw order (bottom-most first). The
 * layout editor walks this list forwards to render and backwards to hit-test,
 * so the thing drawn on top is the thing you grab.
 */
public final class HudElements {

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

    /**
     * How close to the screen's centre line an element has to land before it
     * snaps to a CENTER anchor / a zero offset.
     */
    private static final int CENTER_SNAP = 12;

    /**
     * The margin a LEFT- or RIGHT-anchored bar sits at with a zero offset -
     * the constant baked into {@link HudAnchor#resolve}.
     */
    private static final int EDGE_MARGIN = HudAnchor.MARGIN;

    /**
     * The label line every bar hangs above itself, plus the 1px frame.
     */
    private static final int BAR_LABEL_PAD = 11;
    private static final int BAR_FRAME_PAD = 1;

    /**
     * One per ability slot, in slot order. Only the first
     * {@code activeAbilitySlots} of them are ever listed.
     */
    private static final List<HudElement> SLOTS = buildSlots();

    /**
     * Everything that is not an ability slot, in draw order.
     */
    private static final List<HudElement> BARS = List.of(
            new CharacterPlate(),
            new BeyonderHealth(),
            new Madness(),
            new Spirituality(),
            new Acting(),
            new Resources(),
            new ActionBar(),
            new TargetHealth(),
            new Cogitation(),
            new Notifications());

    private HudElements() {
    }

    private static List<HudElement> buildSlots() {
        List<HudElement> slots = new ArrayList<>();
        for (int i = 0; i < CircleOfImaginationClient.MAX_ABILITIES; i++) {
            slots.add(new Slot(i));
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
        if (id == null) return List.of();
        List<HudElement> found = new ArrayList<>();
        for (HudElement element : all(s)) {
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
        boolean rowFollows = false;
        for (HudElement element : members) {
            if (groupId.equals(element.group()) && element instanceof Slot slot && slot.inRow(s)) {
                rowFollows = true;
                break;
            }
        }
        if (rowFollows) {
            AbilityHudOverlay.shiftRow(dx, dy, h, s);
        }
        for (HudElement element : members) {
            if (!groupId.equals(element.group())) continue;
            if (element instanceof Slot slot && slot.inRow(s)) continue;
            int[] box = element.bounds(w, h, s);
            element.moveTo(Mth.clamp(box[0] + dx, 0, Math.max(0, w - box[2])),
                    Mth.clamp(box[1] + dy, 0, Math.max(0, h - box[3])), w, h, s);
        }
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

    /**
     * Turns a dragged bar position back into anchor + offsets.
     * <p>
     * {@code (pointX, pointY)} is the bar's new fill origin. The anchor follows
     * the half of the screen the bar ended up in, so a bar dragged to the
     * bottom keeps its distance from the bottom edge when the window is
     * resized — which is the whole point of having anchors.
     */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int barH, int w, int h,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        applyAnchoredMove(pointX, pointY, barW, w, h, pointY + barH / 2 < h / 2, anchor, xOffset, yOffset);
    }

    /**
     * Same, for stacks whose vertical extent is bigger than one bar and which
     * therefore decide their own top/bottom half.
     */
    public static void applyAnchoredMove(int pointX, int pointY, int barW, int w, int h, boolean top,
                                         Consumer<String> anchor, IntConsumer xOffset, IntConsumer yOffset) {
        int px = Mth.clamp(pointX, 0, Math.max(0, w - barW));
        int py = Mth.clamp(pointY, 0, Math.max(0, h));
        boolean center = Math.abs(px + barW / 2 - w / 2) <= CENTER_SNAP;
        // Off centre, the offset is measured from whichever edge is nearer, so
        // the element keeps its margin when the window is resized
        boolean right = !center && px + barW / 2 > w / 2;

        anchor.accept((top ? "TOP_" : "BOTTOM_") + (center ? "CENTER" : right ? "RIGHT" : "LEFT"));

        int xo;
        if (center) {
            xo = px - (w - barW) / 2;
            if (Math.abs(xo) <= CENTER_SNAP) xo = 0;
        } else if (right) {
            xo = px - (w - barW - EDGE_MARGIN);
        } else {
            xo = px - EDGE_MARGIN;
        }
        xOffset.accept(xo);
        yOffset.accept(top ? py : h - py);
    }

    /**
     * Faint dashed rectangle marking a slot that only fills up once the server
     * sends something — the remaining resource rows, the other two toasts.
     */
    static void ghostOutline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            int x1 = Math.min(x + w, x + i + 2);
            ctx.fill(x + i, y, x1, y + 1, color);
            ctx.fill(x + i, y + h - 1, x1, y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            int y1 = Math.min(y + h, y + i + 2);
            ctx.fill(x, y + i, x + 1, y1, color);
            ctx.fill(x + w - 1, y + i, x + w, y1, color);
        }
    }

    /**
     * Shared id/label plumbing.
     */
    private abstract static class Element implements HudElement {
        private final String id;

        Element(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    /**
     * A 182-wide bar with its label 10px above it: madness, acting, resources.
     * Bounds cover the frame and that label line.
     * <p>
     * Everything the bar occupies is measured through
     * {@link HudScale#size}, so {@link #bounds} and {@link #moveTo} stay exact
     * inverses at any {@link #scale}: the padding one adds is the padding the
     * other takes back off.
     */
    private abstract static class Bar extends Element {
        private final int barHeight;

        Bar(String id, int barHeight) {
            super(id);
            this.barHeight = barHeight;
        }

        /**
         * Fill origin, straight from the overlay's own anchor math. Already
         * resolved against the scaled bar width.
         */
        abstract int[] fillOrigin(int w, int h, HudConfig.HudSettings s);

        abstract void writeAnchor(HudConfig.HudSettings s, String anchor);

        abstract void writeOffsets(HudConfig.HudSettings s, int x, int y);

        /**
         * This bar's own per-element scale setting.
         */
        abstract float scale(HudConfig.HudSettings s);

        final int barWidth() {
            return MadnessHudOverlay.BAR_WIDTH;
        }

        final int boundsHeight() {
            return BAR_LABEL_PAD + barHeight + BAR_FRAME_PAD;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = fillOrigin(w, h, s);
            float scale = scale(s);
            return new int[]{pos[0] - HudScale.size(BAR_FRAME_PAD, scale),
                    pos[1] - HudScale.size(BAR_LABEL_PAD, scale),
                    HudScale.size(barWidth() + BAR_FRAME_PAD * 2, scale),
                    HudScale.size(boundsHeight(), scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = scale(s);
            applyAnchoredMove(newX + HudScale.size(BAR_FRAME_PAD, scale),
                    newY + HudScale.size(BAR_LABEL_PAD, scale),
                    HudScale.size(barWidth(), scale), HudScale.size(barHeight, scale), w, h,
                    anchor -> writeAnchor(s, anchor),
                    x -> writeOffsets(s, x, Integer.MIN_VALUE),
                    y -> writeOffsets(s, Integer.MIN_VALUE, y));
        }
    }

    // --- Ability slots ---

    /**
     * One ability slot. Slots start out sharing the row that {@code hudX} /
     * {@code hudYOffset} / {@code slotSize} describe; dragging one on its
     * own pins it to its own anchor instead ({@code slotPlacements[index]}),
     * which is how a player puts two slots on the left and two on the right.
     */
    private static final class Slot extends Element {
        private final int index;

        Slot(int index) {
            super(SLOT_PREFIX + (index + 1));
            this.index = index;
        }

        @Override
        public Component label() {
            return Component.translatable("screen.coi.layout_el_slot", index + 1);
        }

        @Override
        public String group() {
            return ABILITY_SLOTS;
        }

        /**
         * True while this slot still follows the shared row.
         */
        boolean inRow(HudConfig.HudSettings s) {
            return AbilityHudOverlay.placement(s, index) == null;
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.enabled;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            return AbilityHudOverlay.slotBounds(index, w, h, s);
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            // bounds starts at the slot box; the name line hangs below it.
            // The box must be measured exactly as AbilityHudOverlay.slotBounds
            // reports it, or bounds and moveTo stop being inverses and a
            // dragged slot jumps off the cursor
            int box = AbilityHudOverlay.boxSize(s);
            String[] anchor = new String[1];
            int[] offsets = new int[2];
            applyAnchoredMove(newX, newY, box, box, w, h,
                    value -> anchor[0] = value,
                    x -> offsets[0] = x,
                    y -> offsets[1] = y);
            if (s.slotPlacements == null || index >= s.slotPlacements.length) return;
            s.slotPlacements[index] = new HudConfig.SlotPlacement(anchor[0], offsets[0], offsets[1]);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            AbilityHudOverlay.renderSlotPreview(ctx, index, w, h, s);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            if (s.slotPlacements != null && index < s.slotPlacements.length) {
                s.slotPlacements[index] = null;
            }
        }

        @Override
        public void resetGroup(HudConfig.HudSettings s) {
            s.hudX = 10;
            s.hudYOffset = 60;
        }
    }

    // --- Character plate ---

    /**
     * The one element whose bounds origin is also its fill origin: the card has
     * nothing hanging above it, so {@link #bounds} and {@link #moveTo} are
     * inverses with no padding to add and take back off, at any scale.
     * <p>
     * Its height is the <em>preview's</em>, not the live card's. The card grows
     * and shrinks with whatever the server is sending, but the editor draws the
     * preview, so the grab box has to match that instead.
     */
    private static final class CharacterPlate extends Element {
        CharacterPlate() {
            super(CHARACTER_PLATE);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showCharacterPlate;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = CharacterPlateOverlay.anchor(w, h, s);
            float scale = s.characterPlateScale;
            return new int[]{pos[0], pos[1],
                    HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                    HudScale.size(CharacterPlateOverlay.previewHeight(), scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.characterPlateScale;
            applyAnchoredMove(newX, newY,
                    HudScale.size(CharacterPlateOverlay.CARD_W, scale),
                    HudScale.size(CharacterPlateOverlay.previewHeight(), scale), w, h,
                    anchor -> s.characterPlateAnchor = anchor,
                    x -> s.characterPlateXOffset = x,
                    y -> s.characterPlateYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            CharacterPlateOverlay.renderPreview(ctx, w, h, s, timeMs);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.characterPlateAnchor = "TOP_LEFT";
            s.characterPlateXOffset = 0;
            s.characterPlateYOffset = CharacterPlateOverlay.DEFAULT_TOP_Y;
        }
    }

    // --- Beyonder health ---

    /**
     * The HP pool bar that stands in for the vanilla hearts. It carries its
     * readout <em>inside</em> the fill rather than above it, so unlike
     * {@link Bar} the only padding between the fill origin and the drawn bounds
     * is the 1px frame - and since the same scaled pad is added here and taken
     * back off in {@link #moveTo}, the two stay exact inverses at any scale.
     */
    private static final class BeyonderHealth extends Element {
        BeyonderHealth() {
            super(BEYONDER_HEALTH);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showBeyonderHealth;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = BeyonderHealthOverlay.anchor(w, h, s);
            float scale = s.beyonderHealthScale;
            int pad = HudScale.size(BAR_FRAME_PAD, scale);
            return new int[]{pos[0] - pad, pos[1] - pad,
                    HudScale.size(BeyonderHealthOverlay.BAR_WIDTH + BAR_FRAME_PAD * 2, scale),
                    HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT + BAR_FRAME_PAD * 2, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.beyonderHealthScale;
            int pad = HudScale.size(BAR_FRAME_PAD, scale);
            applyAnchoredMove(newX + pad, newY + pad,
                    HudScale.size(BeyonderHealthOverlay.BAR_WIDTH, scale),
                    HudScale.size(BeyonderHealthOverlay.BAR_HEIGHT, scale), w, h,
                    anchor -> s.beyonderHealthAnchor = anchor,
                    x -> s.beyonderHealthXOffset = x,
                    y -> s.beyonderHealthYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            BeyonderHealthOverlay.renderPreview(ctx, w, h, s);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
            s.beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
            s.beyonderHealthYOffset = BeyonderHealthOverlay.DEFAULT_Y_OFFSET;
        }
    }

    // --- Madness ---

    private static final class Madness extends Bar {
        Madness() {
            super(MADNESS, MadnessHudOverlay.BAR_HEIGHT);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showMadnessBar && !s.showCharacterPlate;
        }

        @Override
        int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return MadnessHudOverlay.anchor(w, h, s);
        }

        @Override
        void writeAnchor(HudConfig.HudSettings s, String anchor) {
            s.madnessAnchor = anchor;
        }

        @Override
        void writeOffsets(HudConfig.HudSettings s, int x, int y) {
            if (x != Integer.MIN_VALUE) s.madnessXOffset = x;
            if (y != Integer.MIN_VALUE) s.madnessYOffset = y;
        }

        @Override
        float scale(HudConfig.HudSettings s) {
            return s.madnessScale;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = fillOrigin(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.madnessScale);
            MadnessHudOverlay.drawBarAt(ctx, pos[0], pos[1], 42, 10, timeMs);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.madnessAnchor = "TOP_LEFT";
            s.madnessXOffset = 0;
            s.madnessYOffset = MadnessHudOverlay.DEFAULT_TOP_Y;
        }
    }

    // --- Spirituality ---

    /**
     * The one element whose geometry isn't a plain 182×6 rectangle: its frame
     * sprite overhangs the fill on every side, so the offsets between the fill
     * origin and the drawn bounds come from the overlay itself.
     */
    private static final class Spirituality extends Element {
        Spirituality() {
            super(SPIRITUALITY);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showSpiritualityBar;
        }

        private static int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return SpiritualityHudOverlay.anchor(w, h, s);
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            return SpiritualityHudOverlay.bounds(w, h, s);
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            // Whatever the frame's overhang is today, it is the gap between the
            // fill origin and the bounds origin
            int[] fill = fillOrigin(w, h, s);
            int[] box = bounds(w, h, s);
            applyAnchoredMove(newX + (fill[0] - box[0]), newY + (fill[1] - box[1]),
                    HudScale.size(SpiritualityHudOverlay.BAR_WIDTH, s.spiritualityScale),
                    HudScale.size(SpiritualityHudOverlay.BAR_HEIGHT, s.spiritualityScale), w, h,
                    anchor -> s.spiritualityAnchor = anchor,
                    x -> s.spiritualityXOffset = x,
                    y -> s.spiritualityYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            SpiritualityHudOverlay.renderPreview(ctx, w, h, s, timeMs);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.spiritualityAnchor = "TOP_LEFT";
            s.spiritualityXOffset = 0;
            s.spiritualityYOffset = 50;
        }
    }

    // --- Acting ---

    private static final class Acting extends Bar {
        Acting() {
            super(ACTING, ActingHudOverlay.BAR_HEIGHT);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showActingBar && !s.showCharacterPlate;
        }

        @Override
        int[] fillOrigin(int w, int h, HudConfig.HudSettings s) {
            return ActingHudOverlay.anchor(w, h, s);
        }

        @Override
        void writeAnchor(HudConfig.HudSettings s, String anchor) {
            s.actingAnchor = anchor;
        }

        @Override
        void writeOffsets(HudConfig.HudSettings s, int x, int y) {
            if (x != Integer.MIN_VALUE) s.actingXOffset = x;
            if (y != Integer.MIN_VALUE) s.actingYOffset = y;
        }

        @Override
        float scale(HudConfig.HudSettings s) {
            return s.actingScale;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = fillOrigin(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.actingScale);
            ActingHudOverlay.drawBarAt(ctx, pos[0], pos[1], 63,
                    AbilityInfo.pathwayRgb("fool"),
                    I18n.get("hud.coi.acting_label", String.format(Locale.ROOT, "%.1f", 63.0)));
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.actingAnchor = "TOP_LEFT";
            s.actingXOffset = 0;
            s.actingYOffset = 80;
        }
    }

    // --- Resource bars ---

    private static final class Resources extends Element {
        private static final int GHOST = 0x60FFFFFF;

        Resources() {
            super(RESOURCES);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showResourceBars && !s.showCharacterPlate;
        }

        private static int rows(HudConfig.HudSettings s) {
            return Math.max(1, s.resourceMaxBars);
        }

        /**
         * Gap between two stacked bars once the scale is applied.
         */
        private static int stride(HudConfig.HudSettings s) {
            return HudScale.size(ResourceHudOverlay.STRIDE, s.resourceScale);
        }

        private static int stackHeight(HudConfig.HudSettings s) {
            return (rows(s) - 1) * stride(s) + HudScale.size(
                    BAR_LABEL_PAD + ResourceHudOverlay.BAR_HEIGHT + BAR_FRAME_PAD, s.resourceScale);
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = ResourceHudOverlay.anchor(w, h, s);
            float scale = s.resourceScale;
            int grow = (rows(s) - 1) * stride(s);
            // BOTTOM anchors stack upward, so the first bar is the stack's floor
            int top = pos[1] - HudScale.size(BAR_LABEL_PAD, scale)
                    - (HudAnchor.parse(s.resourceAnchor).isTop() ? 0 : grow);
            return new int[]{pos[0] - HudScale.size(BAR_FRAME_PAD, scale), top,
                    HudScale.size(ResourceHudOverlay.BAR_WIDTH + BAR_FRAME_PAD * 2, scale), stackHeight(s)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.resourceScale;
            boolean top = newY + stackHeight(s) / 2 < h / 2;
            int grow = (rows(s) - 1) * stride(s);
            int fillY = newY + HudScale.size(BAR_LABEL_PAD, scale) + (top ? 0 : grow);
            applyAnchoredMove(newX + HudScale.size(BAR_FRAME_PAD, scale), fillY,
                    HudScale.size(ResourceHudOverlay.BAR_WIDTH, scale), w, h, top,
                    anchor -> s.resourceAnchor = anchor,
                    x -> s.resourceXOffset = x,
                    y -> s.resourceYOffset = y);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = ResourceHudOverlay.anchor(w, h, s);
            // Inside the push everything keeps its unscaled geometry
            int step = HudAnchor.parse(s.resourceAnchor).isTop()
                    ? ResourceHudOverlay.STRIDE : -ResourceHudOverlay.STRIDE;

            HudScale.push(ctx, pos[0], pos[1], s.resourceScale);
            ResourceHudOverlay.drawBarAt(ctx, pos[0], pos[1], "Rage Meter", 62.5, 100, 0xFF5555, true);
            if (rows(s) > 1) {
                ResourceHudOverlay.drawBarAt(ctx, pos[0], pos[1] + step, "Seeds", 3, 5, 0x55FF7F, false);
            }
            // The rest of the stack only appears when the server sends more
            for (int i = 2; i < rows(s); i++) {
                ghostOutline(ctx, pos[0] - BAR_FRAME_PAD, pos[1] + step * i - BAR_FRAME_PAD,
                        ResourceHudOverlay.BAR_WIDTH + BAR_FRAME_PAD * 2,
                        ResourceHudOverlay.BAR_HEIGHT + BAR_FRAME_PAD * 2, GHOST);
            }
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.resourceAnchor = "TOP_LEFT";
            s.resourceXOffset = 0;
            s.resourceYOffset = 100;
        }
    }

    // --- Action bar ---

    private static final class ActionBar extends Element {
        private static final int SAMPLE_W = 200;
        /**
         * Two 10px lines stacked upward from the base line, plus the descender
         * room the lower one needs.
         */
        private static final int SAMPLE_H = 20;
        private static final int ABOVE_BASE = 11;

        ActionBar() {
            super(ACTION_BAR);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showActionBar;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            float scale = s.actionBarScale;
            int sampleW = HudScale.size(SAMPLE_W, scale);
            return new int[]{w / 2 + s.actionBarXOffset - sampleW / 2,
                    h - s.actionBarYOffset - HudScale.size(ABOVE_BASE, scale),
                    sampleW, HudScale.size(SAMPLE_H, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.actionBarScale;
            int xo = newX + HudScale.size(SAMPLE_W, scale) / 2 - w / 2;
            s.actionBarXOffset = Math.abs(xo) <= CENTER_SNAP ? 0 : xo;
            s.actionBarYOffset = Mth.clamp(h - newY - HudScale.size(ABOVE_BASE, scale), 0, h);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int centerX = w / 2 + s.actionBarXOffset;
            int baseY = h - s.actionBarYOffset;
            HudScale.push(ctx, centerX, baseY, s.actionBarScale);
            ActionBarHudOverlay.drawLineAt(ctx, Component.literal("Cooldown · Sword 1.8s"), "COOLDOWN", centerX, baseY);
            ActionBarHudOverlay.drawLineAt(ctx, Component.literal("Status · Active"), "STATUS",
                    centerX, baseY - ActionBarHudOverlay.LINE_SPACING);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.actionBarXOffset = 0;
            s.actionBarYOffset = 72;
        }
    }

    // --- Target health ---

    private static final class TargetHealth extends Element {
        /**
         * Name line above the bar, readout below it.
         */
        private static final int ABOVE = 11;
        private static final int BELOW = 12;

        TargetHealth() {
            super(TARGET_HEALTH);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showTargetHealth;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = TargetHealthOverlay.anchor(w, h, s);
            float scale = s.targetHealthScale;
            return new int[]{pos[0] - HudScale.size(BAR_FRAME_PAD, scale), pos[1] - HudScale.size(ABOVE, scale),
                    HudScale.size(TargetHealthOverlay.BAR_WIDTH + BAR_FRAME_PAD * 2, scale),
                    HudScale.size(ABOVE + TargetHealthOverlay.BAR_HEIGHT + BELOW, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            float scale = s.targetHealthScale;
            s.targetHealthXOffset = newX + HudScale.size(BAR_FRAME_PAD, scale)
                    - (w / 2 - HudScale.size(TargetHealthOverlay.BAR_WIDTH, scale) / 2);
            s.targetHealthYOffset = newY + HudScale.size(ABOVE, scale) - h / 2;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = TargetHealthOverlay.anchor(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.targetHealthScale);
            TargetHealthOverlay.drawBarAt(ctx, pos[0], pos[1], "Steve", 0.68f, 0.85f, 13.6, 20, true);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.targetHealthXOffset = 0;
            s.targetHealthYOffset = TargetHealthOverlay.DEFAULT_Y_OFFSET;
        }
    }

    // --- Cogitation ---

    private static final class Cogitation extends Element {
        Cogitation() {
            super(COGITATION);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showCogitationOverlay;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = CogitationOverlay.anchor(w, h, s);
            float scale = s.cogitationScale;
            return new int[]{pos[0], pos[1],
                    HudScale.size(CogitationOverlay.CARD_W, scale),
                    HudScale.size(CogitationOverlay.CARD_H, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            s.cogitationXOffset = newX - (w / 2 - HudScale.size(CogitationOverlay.CARD_W, s.cogitationScale) / 2);
            s.cogitationYOffset = newY - h / 2;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = CogitationOverlay.anchor(w, h, s);
            HudScale.push(ctx, pos[0], pos[1], s.cogitationScale);
            CogitationOverlay.drawCardAt(ctx, pos[0], pos[1], "Turn around", 3, 0.6, false);
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.cogitationXOffset = 0;
            s.cogitationYOffset = CogitationOverlay.DEFAULT_Y_OFFSET;
        }
    }

    // --- Notifications ---

    private static final class Notifications extends Element {
        private static final int SLOTS = 3;
        private static final int STRIDE = NotificationOverlay.CARD_H_1_LINE + NotificationOverlay.CARD_GAP;
        private static final int GHOST = 0x60FFFFFF;

        Notifications() {
            super(NOTIFICATIONS);
        }

        @Override
        public boolean visible(HudConfig.HudSettings s) {
            return s.showNotifications;
        }

        @Override
        public int[] bounds(int w, int h, HudConfig.HudSettings s) {
            int[] pos = NotificationOverlay.anchor(w, s);
            float scale = s.notificationScale;
            return new int[]{pos[0], pos[1], HudScale.size(NotificationOverlay.CARD_W, scale),
                    SLOTS * HudScale.size(STRIDE, scale) - HudScale.size(NotificationOverlay.CARD_GAP, scale)};
        }

        @Override
        public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
            // Stored as a margin from the right edge, so the stack stays in the
            // corner when the window widens
            s.notificationXOffset = w - (newX + HudScale.size(NotificationOverlay.CARD_W, s.notificationScale));
            s.notificationYOffset = newY;
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
            int[] pos = NotificationOverlay.anchor(w, s);
            HudScale.push(ctx, pos[0], pos[1], s.notificationScale);
            NotificationOverlay.drawToastAt(ctx, pos[0], pos[1],
                    "Advancement", "Sequence 8 reached", 0xFFFFD870, 1f);
            for (int i = 1; i < SLOTS; i++) {
                ghostOutline(ctx, pos[0], pos[1] + i * STRIDE,
                        NotificationOverlay.CARD_W, NotificationOverlay.CARD_H_1_LINE, GHOST);
            }
            HudScale.pop(ctx);
        }

        @Override
        public void resetPosition(HudConfig.HudSettings s) {
            s.notificationXOffset = NotificationOverlay.DEFAULT_MARGIN;
            s.notificationYOffset = NotificationOverlay.DEFAULT_MARGIN;
        }
    }
}
