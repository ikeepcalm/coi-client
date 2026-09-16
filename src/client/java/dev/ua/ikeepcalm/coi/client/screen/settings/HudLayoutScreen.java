package dev.ua.ikeepcalm.coi.client.screen.settings;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudElement;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudElements;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Drag-to-position editor for the whole HUD.
 * <p>
 * Every overlay stands down while this screen is open ({@link HudLayout}), and
 * the screen draws one sample-data preview of each element in its place — so
 * what the player drags is the element itself, at the size and colours it will
 * really have, over the live world rather than a mock-up.
 * <p>
 * Positions are written straight into the settings object handed in by the
 * caller: the live {@link HudConfig} settings when opened on its own, or the
 * HUD settings screen's working copy when opened from there, so the two can't
 * overwrite each other.
 * <p>
 * Opened with a preselected id - the <em>Align</em> button on a settings row -
 * the screen runs in <b>solo mode</b>: only that element, or only that group
 * (the ten ability slots share the group {@code ability_slots}), is drawn,
 * grabbable and reset. Everything else stays invisible, so one bar can be
 * placed without the rest of the HUD in the way.
 * <p>
 * Holding <b>Ctrl</b> while dragging or nudging a grouped element moves its
 * whole group by the same delta - the ability row keeps its shape while the
 * player slides it about, and the slots pulled out of it come along.
 * <p>
 * <b>The toolbar gets out of the way.</b> It is drawn over the HUD and
 * {@link #inToolbar} swallows every click inside it, so an element parked
 * underneath it cannot be grabbed at all - which is what a bottom-centre
 * element like the Beyonder health bar always is. See {@link #chooseToolbarSide}.
 */
public class HudLayoutScreen extends Screen {

    private static final int GUIDE = 0x30FFFFFF;
    private static final int GUIDE_ACTIVE = 0xC0FFD870;

    private static final int NUDGE = 1;
    private static final int NUDGE_FAST = 8;

    private static final int TOOLBAR_H = 46;
    /** Gap between the toolbar card and the screen edge it sits against. */
    private static final int TOOLBAR_MARGIN = 8;

    private final Screen parent;
    private final HudConfig.HudSettings settings;
    /**
     * Pre-edit copy, restored by Cancel.
     */
    private final HudConfig.HudSettings snapshot = new HudConfig.HudSettings();
    private final @Nullable String preselectId;
    /**
     * True in solo mode: the preselected id resolved to something.
     */
    private final boolean solo;
    /**
     * The elements this screen draws and hit-tests - everything, or just the
     * solo element / solo group.
     */
    private final List<HudElement> elements;

    private @Nullable HudElement selected;
    private @Nullable HudElement hovered;

    private boolean dragging;
    private int dragOriginX, dragOriginY;
    private double dragMouseX, dragMouseY;
    private boolean snappedX, snappedY;

    private int toolbarX, toolbarY, toolbarW;
    /**
     * Which edge the toolbar sits against. Decided once per {@link #init} by
     * {@link #chooseToolbarSide}, unless the player has overridden it.
     */
    private boolean toolbarTop;
    /**
     * True once the player has pressed <b>T</b>. After that the automatic
     * choice stops running, so a window resize cannot move the toolbar back
     * onto the element they just uncovered.
     */
    private boolean toolbarPinned;

    public HudLayoutScreen(Screen parent, @Nullable String preselectId) {
        this(parent, preselectId, HudConfig.getSettings());
    }

    public HudLayoutScreen(Screen parent, @Nullable String preselectId, HudConfig.HudSettings settings) {
        super(Component.translatable("screen.coi.layout_title"));
        this.parent = parent;
        this.settings = settings;
        // activeAbilitySlots decides how many slot elements there are, so the
        // list is built once, here, rather than kept as a constant
        List<HudElement> set = HudElements.soloSet(preselectId, settings);
        this.solo = !set.isEmpty();
        this.preselectId = solo ? preselectId : null;
        this.elements = solo ? set : HudElements.all(settings);
        HudConfig.copySettings(settings, snapshot);
    }

    @Override
    protected void init() {
        HudLayout.setEditing(true);
        this.clearWidgets();

        if (solo) {
            selected = elements.getFirst();
        } else if (selected == null && preselectId != null) {
            selected = HudElements.byId(preselectId);
        }

        toolbarW = Math.min(360, this.width - 20);
        toolbarX = (this.width - toolbarW) / 2;
        if (!toolbarPinned) toolbarTop = chooseToolbarSide();
        toolbarY = toolbarY();

        int buttonW = (toolbarW - 24) / 3;
        int buttonY = toolbarY + 6;

        this.addRenderableWidget(Button.builder(
                Component.translatable(!solo ? "screen.coi.layout_reset_all" : "screen.coi.layout_reset_one"),
                b -> resetAll()).bounds(toolbarX + 8, buttonY, buttonW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.layout_cancel"),
                b -> {
                    HudConfig.copySettings(snapshot, settings);
                    close();
                }).bounds(toolbarX + 8 + buttonW + 4, buttonY, buttonW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.layout_done"),
                b -> {
                    persist();
                    close();
                }).bounds(toolbarX + 8 + (buttonW + 4) * 2, buttonY, buttonW, 20).build());
    }

    /**
     * Top edge of the toolbar card for the side it is currently on.
     */
    private int toolbarY() {
        return toolbarTop ? TOOLBAR_MARGIN : this.height - TOOLBAR_H - TOOLBAR_MARGIN;
    }

    /**
     * Which edge to park the toolbar against: whichever one has fewer of this
     * screen's elements sitting under it.
     * <p>
     * The toolbar is opaque chrome over a live HUD and {@link #inToolbar}
     * turns every click inside it into a no-op, so an element underneath it is
     * not merely hard to see - it cannot be selected, dragged or right-click
     * reset at all. Parked at the bottom it lands squarely on anything
     * bottom-centre, which is where the Beyonder health element lives by
     * definition, and players reported exactly that: the bar they could not
     * move.
     * <p>
     * The count is taken <b>once per {@link #init}</b>, never per frame:
     * re-deciding while the player drags would make the toolbar jump out from
     * under the cursor mid-gesture. Ties keep the bottom, which is where the
     * toolbar has always been and where the eye expects chrome.
     */
    private boolean chooseToolbarSide() {
        return covered(TOOLBAR_MARGIN) < covered(this.height - TOOLBAR_H - TOOLBAR_MARGIN);
    }

    /**
     * How many elements the toolbar card would overlap with its top edge at
     * {@code y}. Only the card counts - the hint lines around it are text and
     * block nothing.
     */
    private int covered(int y) {
        int count = 0;
        for (HudElement element : elements) {
            int[] box = element.bounds(this.width, this.height, settings);
            if (box[0] < toolbarX + toolbarW && box[0] + box[2] > toolbarX
                    && box[1] < y + TOOLBAR_H && box[1] + box[3] > y) {
                count++;
            }
        }
        return count;
    }

    /**
     * Resets every element this screen edits, plus the shared origin of each
     * group represented among them - the ability row's, for the slots.
     */
    private void resetAll() {
        Set<String> groups = new HashSet<>();
        for (HudElement element : elements) {
            element.resetPosition(settings);
            if (element.group() != null && groups.add(element.group())) {
                element.resetGroup(settings);
            }
        }
    }

    /**
     * Done commits, whatever settings object this screen was handed.
     * <p>
     * Opened from HUD Settings it edits <em>that</em> screen's working copy, and
     * leaving the commit to that screen's own Done meant an Esc behind this one
     * threw away every drag the player had just made - which is not something
     * "Done" can mean. So a working copy is written through to the live settings
     * first, then saved.
     * <p>
     * The write-through carries whatever rows the settings screen had already
     * changed in that copy too. That is deliberate: the two screens edit one
     * object, and a rule that committed half of it would be harder to predict
     * than one that commits it.
     * <p>
     * Cancel is unaffected - it restores {@code snapshot} into {@code settings}
     * before closing, so nothing reaches here.
     */
    private void persist() {
        if (settings != HudConfig.getSettings()) {
            HudConfig.copySettings(settings, HudConfig.getSettings());
        }
        HudConfig.save();
    }

    private void close() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        HudLayout.setEditing(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // Esc behaves like Done — the drag already looked like what you get
        persist();
        close();
    }

    // --- Rendering ---

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // No blur: the point of this screen is judging the HUD against the
        // world it will sit on
        graphics.fill(0, 0, this.width, this.height, CoiStyle.VEIL);
        graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, snappedX ? GUIDE_ACTIVE : GUIDE);
        graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, snappedY ? GUIDE_ACTIVE : GUIDE);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long time = System.currentTimeMillis();
        hovered = dragging ? selected : elementAt(mouseX, mouseY);

        LayoutPainter.previews(graphics, this.font, elements, this.width, this.height, settings, time);

        if (hovered != null && hovered != selected) {
            LayoutPainter.highlight(graphics, this.font, hovered, this.width, this.height, settings,
                    LayoutPainter.HOVER_OUTLINE, false);
        }
        if (selected != null) {
            LayoutPainter.highlight(graphics, this.font, selected, this.width, this.height, settings,
                    CoiStyle.ACCENT, true);
        }

        CoiStyle.drawCard(graphics, toolbarX, toolbarY, toolbarW, TOOLBAR_H);
        // The lines outside the card are stacked rather than placed, because
        // two of the three are conditional and the toolbar can be on either
        // edge - a fixed y per line would gap or collide depending on both
        int line = 0;
        if (solo) {
            graphics.centeredText(this.font,
                    Component.translatable("screen.coi.layout_title_one",
                            HudElements.groupLabel(preselectId, elements)),
                    this.width / 2, outsideHintY(line++), CoiStyle.ACCENT);
        }
        if (selected != null && selected.group() != null) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.layout_hint_group"),
                    this.width / 2, outsideHintY(line++), CoiStyle.TEXT_MUTED);
        }
        graphics.centeredText(this.font, Component.translatable("screen.coi.layout_hint_toolbar"),
                this.width / 2, outsideHintY(line), CoiStyle.TEXT_MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font,
                Component.translatable(elements.size() > 1 ? "screen.coi.layout_hint" : "screen.coi.layout_hint_one"),
                this.width / 2, toolbarY + 32, CoiStyle.TEXT_MUTED);
    }

    /**
     * Baseline of hint line {@code row}, counting away from the toolbar card:
     * upward when the card is at the bottom, downward when it is at the top,
     * so the stack always grows into the screen rather than off it.
     */
    private int outsideHintY(int row) {
        return toolbarTop
                ? toolbarY + TOOLBAR_H + 5 + row * 12
                : toolbarY - 13 - row * 12;
    }

    // --- Hit testing ---

    /**
     * Top-most element under the cursor: the list is in draw order, so it is
     * searched backwards.
     */
    private @Nullable HudElement elementAt(double mouseX, double mouseY) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            int[] box = elements.get(i).bounds(this.width, this.height, settings);
            if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
                return elements.get(i);
            }
        }
        return null;
    }

    private boolean inToolbar(double mouseX, double mouseY) {
        return mouseX >= toolbarX && mouseX < toolbarX + toolbarW
                && mouseY >= toolbarY && mouseY < toolbarY + TOOLBAR_H;
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        // The toolbar sits over the HUD; clicks there must never start a drag
        if (inToolbar(event.x(), event.y())) {
            return true;
        }

        HudElement hit = elementAt(event.x(), event.y());
        if (hit == null) {
            // Solo mode has nothing else to select, so a stray click keeps it
            if (!solo) {
                selected = null;
            }
            return true;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            selected = hit;
            hit.resetPosition(settings);
            return true;
        }

        selected = hit;
        int[] box = hit.bounds(this.width, this.height, settings);
        dragging = true;
        dragOriginX = box[0];
        dragOriginY = box[1];
        dragMouseX = event.x();
        dragMouseY = event.y();
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging || selected == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        moveSelected(dragOriginX + (int) Math.round(event.x() - dragMouseX),
                dragOriginY + (int) Math.round(event.y() - dragMouseY),
                !event.hasShiftDown(), event.hasControlDown());
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            snappedX = false;
            snappedY = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    /**
     * Applies a new bounds origin with grid, centre and margin snapping, then
     * clamps the element on screen.
     *
     * @param group move every member of the selection's {@link HudElement#group()}
     *              by the same delta instead of just the selection
     */
    private void moveSelected(int x, int y, boolean snap, boolean group) {
        int[] box = selected.bounds(this.width, this.height, settings);
        LayoutSnap.Landing landing = LayoutSnap.apply(x, y, box[2], box[3], this.width, this.height, snap);
        snappedX = landing.snappedX();
        snappedY = landing.snappedY();

        if (group && selected.group() != null) {
            HudElements.moveGroupBy(selected.group(), elements, landing.x() - box[0], landing.y() - box[1],
                    this.width, this.height, settings);
            return;
        }
        selected.moveTo(landing.x(), landing.y(), this.width, this.height, settings);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        int step = event.hasShiftDown() ? NUDGE_FAST : NUDGE;
        int key = event.key();

        boolean group = event.hasControlDown();

        if (key == InputConstants.KEY_T) {
            // Pinned from here on: the automatic side stops running, so a
            // resize cannot park the toolbar back on what was just uncovered
            toolbarTop = !toolbarTop;
            toolbarPinned = true;
            this.init();
            return true;
        }
        if (key == InputConstants.KEY_TAB) {
            if (elements.size() > 1) {
                cycleSelection(event.hasShiftDown() ? -1 : 1);
            }
            return true;
        }
        if (selected != null) {
            int[] box = selected.bounds(this.width, this.height, settings);
            if (key == InputConstants.KEY_LEFT) {
                moveSelected(box[0] - step, box[1], false, group);
                return true;
            }
            if (key == InputConstants.KEY_RIGHT) {
                moveSelected(box[0] + step, box[1], false, group);
                return true;
            }
            if (key == InputConstants.KEY_UP) {
                moveSelected(box[0], box[1] - step, false, group);
                return true;
            }
            if (key == InputConstants.KEY_DOWN) {
                moveSelected(box[0], box[1] + step, false, group);
                return true;
            }
            if (key == InputConstants.KEY_R) {
                selected.resetPosition(settings);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void cycleSelection(int direction) {
        int index = selected == null ? -1 : elements.indexOf(selected);
        int next = Math.floorMod(index + direction, elements.size());
        selected = elements.get(next);
    }
}
