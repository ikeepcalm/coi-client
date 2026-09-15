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
 */
public class HudLayoutScreen extends Screen {

    private static final int GUIDE = 0x30FFFFFF;
    private static final int GUIDE_ACTIVE = 0xC0FFD870;

    private static final int NUDGE = 1;
    private static final int NUDGE_FAST = 8;

    private static final int TOOLBAR_H = 46;

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
        toolbarY = this.height - TOOLBAR_H - 8;

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
     * Only the live settings object is ours to write; a working copy belongs to
     * the screen that made it.
     */
    private void persist() {
        if (settings == HudConfig.getSettings()) {
            HudConfig.save();
        }
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
        if (solo) {
            graphics.centeredText(this.font,
                    Component.translatable("screen.coi.layout_title_one",
                            HudElements.groupLabel(preselectId, elements)),
                    this.width / 2, toolbarY - 13, CoiStyle.ACCENT);
        }
        if (selected != null && selected.group() != null) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.layout_hint_group"),
                    this.width / 2, toolbarY - 25, CoiStyle.TEXT_MUTED);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font,
                Component.translatable(elements.size() > 1 ? "screen.coi.layout_hint" : "screen.coi.layout_hint_one"),
                this.width / 2, toolbarY + 32, CoiStyle.TEXT_MUTED);
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
