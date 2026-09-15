package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.screen.ability.PickerModel.Row;
import dev.ua.ikeepcalm.coi.client.screen.ability.PickerModel.RowKind;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Modal ability chooser: a search box over a list bracketed by pathway <em>and
 * sequence</em>, each ability a two-line card carrying the numbers that tell one
 * spell from another (cost, cooldown, category, kind). Not a screen child — the owning
 * screen renders it last (on its own stratum) and routes all input here while
 * {@link #isOpen()}.
 * <p>
 * The list is a flat {@link Row} list mixing clickable entries with inert
 * headers and messages, so scrolling, hit-testing and keyboard arithmetic all
 * walk the same structure instead of guessing at index offsets. This class owns
 * the panel geometry and the input; {@link PickerModel} owns the rows and the
 * pixel arithmetic, {@link PickerPainter} draws them.
 */
public class AbilityPickerOverlay {

    private boolean open = false;
    private Component title = Component.empty();
    private String currentSelection;
    private Consumer<String> onSelect;
    private EditBox searchBox;
    private final PickerModel model = new PickerModel();
    private int scrollOffset = 0;
    /** Whether this connection carries cost/cooldown numbers at all — see {@link PickerModel#detectMeta()}. */
    private boolean metaAvailable = false;

    // Panel geometry, recomputed every render frame; mouse events reuse the last frame's values
    private int panelX, panelY, panelW, panelH, listTop, listH;

    // Row under the cursor this frame; the tooltip for it is emitted after the
    // list's scissor is popped, otherwise it would be clipped away
    private String hoveredOption;

    public void open(Component contextTitle, String currentSelection, Consumer<String> onSelect) {
        this.open = true;
        this.title = contextTitle;
        this.currentSelection = currentSelection;
        this.onSelect = onSelect;
        this.scrollOffset = 0;
        this.metaAvailable = PickerModel.detectMeta();

        Font font = Minecraft.getInstance().font;
        this.searchBox = new EditBox(font, 0, 0, 100, PickerMetrics.SEARCH_H, Component.translatable("screen.coi.picker_search_hint"));
        searchBox.setMaxLength(60);
        searchBox.setHint(Component.translatable("screen.coi.picker_search_hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(s -> {
            refilter();
            scrollOffset = 0;
        });
        searchBox.setFocused(true);
        refilter();
    }

    public void close() {
        open = false;
        searchBox = null;
    }

    public boolean isOpen() {
        return open;
    }

    private void refilter() {
        model.refilter(query());
    }

    private String query() {
        return searchBox == null ? "" : searchBox.getValue();
    }

    // --- Geometry ---

    private void updateGeometry(int screenW, int screenH) {
        panelW = Math.clamp(screenW * 3 / 5, PickerMetrics.MIN_PANEL_W, Math.max(PickerMetrics.MIN_PANEL_W, screenW - 20));
        panelW = Math.min(panelW, PickerMetrics.MAX_PANEL_W);

        int headerH = headerHeight();
        // The floor keeps Unbind plus one real ability row reachable even at gui
        // scale 4 in a small window, where the honest fit would be zero rows.
        int floor = PickerMetrics.UNBIND_H + PickerMetrics.ROW_H;
        int room = screenH - 60 - headerH - PickerMetrics.PAD;
        listH = Math.min(model.contentHeight(), Math.max(room, floor));

        panelH = headerH + listH + PickerMetrics.PAD;
        panelX = Math.max(0, (screenW - panelW) / 2);
        panelY = Math.max(0, (screenH - panelH) / 2);
        listTop = panelY + headerH;

        scrollOffset = Mth.clamp(scrollOffset, 0, model.maxScroll(listH));

        if (searchBox != null) {
            searchBox.setX(panelX + PickerMetrics.PAD);
            searchBox.setY(panelY + searchBoxOffset());
            searchBox.setWidth(panelW - PickerMetrics.PAD * 2);
        }
    }

    /** Title line, then the search box, then the gap above the list. */
    private static int headerHeight() {
        return searchBoxOffset() + PickerMetrics.SEARCH_H + 6;
    }

    private static int searchBoxOffset() {
        return PickerMetrics.PAD + 9 + 6;
    }

    // --- Rendering ---

    public void render(GuiGraphicsExtractor graphics, Font font, int screenW, int screenH, int mouseX, int mouseY, float delta) {
        if (!open) return;
        updateGeometry(screenW, screenH);

        graphics.fill(0, 0, screenW, screenH, CoiStyle.BACKDROP);
        CoiStyle.drawCard(graphics, panelX, panelY, panelW, panelH);

        Component trimmedTitle = Component.literal(font.plainSubstrByWidth(title.getString(), panelW - PickerMetrics.PAD * 2));
        graphics.text(font, trimmedTitle, panelX + PickerMetrics.PAD, panelY + PickerMetrics.PAD, CoiStyle.ACCENT);

        if (searchBox != null) {
            searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        int listBottom = listTop + listH;
        renderRows(graphics, font, listBottom, mouseX, mouseY);

        if (hoveredOption != null) {
            PickerPainter.tooltip(graphics, font, hoveredOption, mouseX, mouseY);
        }

        int content = model.contentHeight();
        if (content > listH) {
            PickerPainter.scrollbar(graphics, panelX, panelW, listTop, listBottom, listH,
                    model.heightAbove(scrollOffset), content);
        }
    }

    private void renderRows(GuiGraphicsExtractor graphics, Font font, int listBottom, int mouseX, int mouseY) {
        List<Row> rows = model.rows();
        hoveredOption = null;
        graphics.enableScissor(panelX + 1, listTop, panelX + panelW - 1, listBottom);
        int y = listTop;
        for (int i = scrollOffset; i < rows.size() && y < listBottom; i++) {
            Row row = rows.get(i);
            int height = PickerModel.rowHeight(row);
            boolean hovered = row.clickable() && isRowHovered(mouseX, mouseY, y, height, listBottom);
            switch (row.kind()) {
                case UNBIND -> PickerPainter.unbindRow(graphics, font, panelX, panelW, y, hovered);
                case HEADER -> PickerPainter.headerRow(graphics, font, panelX, panelW, row, y);
                case MESSAGE -> PickerPainter.messageRow(graphics, font, panelX, panelW, y, query());
                case ABILITY -> {
                    if (hovered) hoveredOption = row.option();
                    PickerPainter.abilityRow(graphics, font, panelX, panelW, row, y, hovered,
                            metaAvailable, isCurrentSelection(row.option()));
                }
            }
            y += height;
        }
        graphics.disableScissor();
    }

    private boolean isRowHovered(int mouseX, int mouseY, int rowY, int height, int listBottom) {
        return mouseX >= panelX + 1 && mouseX < panelX + panelW - 1
                && mouseY >= rowY && mouseY < rowY + height
                && mouseY >= listTop && mouseY < listBottom;
    }

    private boolean isCurrentSelection(String option) {
        if (currentSelection == null) return false;
        return AbilityInfo.extractId(option).equals(AbilityInfo.extractId(currentSelection))
                && AbilityInfo.extractAction(option).equals(AbilityInfo.extractAction(currentSelection));
    }

    // --- Input ---

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!open) return false;
        double mx = event.x();
        double my = event.y();

        if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
            close();
            return true;
        }

        if (searchBox != null && searchBox.isMouseOver(mx, my)) {
            searchBox.setFocused(true);
            searchBox.mouseClicked(event, false);
            return true;
        }

        int listBottom = listTop + listH;
        if (my >= listTop && my < listBottom) {
            pickRowAt(my, listBottom);
        }
        return true;
    }

    private void pickRowAt(double my, int listBottom) {
        List<Row> rows = model.rows();
        int y = listTop;
        for (int i = scrollOffset; i < rows.size() && y < listBottom; i++) {
            Row row = rows.get(i);
            int height = PickerModel.rowHeight(row);
            if (my >= y && my < y + height) {
                // Headers and messages simply swallow the click
                if (row.kind() == RowKind.UNBIND) pick(null);
                else if (row.kind() == RowKind.ABILITY) pick(row.option());
                return;
            }
            y += height;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return false;
        scrollOffset = Mth.clamp(scrollOffset + (verticalAmount > 0 ? -1 : 1), 0, model.maxScroll(listH));
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!open) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            List<String> filtered = model.filtered();
            if (!filtered.isEmpty()) {
                pick(filtered.getFirst());
            }
            return true;
        }
        if (searchBox != null) {
            searchBox.keyPressed(event);
        }
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) return false;
        if (searchBox != null) {
            searchBox.charTyped(event);
        }
        return true;
    }

    private void pick(String option) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (onSelect != null) {
            onSelect.accept(option);
        }
        close();
    }
}
