package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.*;

/**
 * The parts that repeat a cell: a list row, a tile grid, and the side-by-side
 * mini-cards. Each decides its own columns, because a document's {@code
 * columns} is a hint the card's width can overrule.
 */
public class MenuCollectionParts {

    private MenuCollectionParts() {
    }

    public static class RowPart extends MenuPart {
        private final MenuComponent.Row row;

        RowPart(MenuContext ctx, MenuComponent.Row row) {
            super(ctx);
            this.row = row;
            this.height = (row.subtitle().isEmpty() ? 20 : 28) + (row.hasFraction() ? 4 : 0) + 2;
        }

        private boolean clickable() {
            return row.enabled() && !row.action().isEmpty();
        }

        @Override
        int growthCapacity() {
            return 24;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            int h = height - 2;
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, x, top, w, h);
            int tint = MenuTheme.argb(row.rgb(), ctx.accent());

            MenuTheme.panel(g, x, top, w, h, MenuTheme.surface(hover(hovered && clickable())), 0);
            g.fill(x, top + 1, x + 2, top + h - 1, MenuTheme.withAlpha(tint, row.enabled() ? 0.9f : 0.3f));

            int textX = x + 8;
            if (MenuIcons.draw(g, font, row.icon(), textX, top + (h - ICON) / 2, ICON,
                    row.enabled() ? 1f : 0.4f)) {
                textX += ICON + 6;
            }

            int rightEdge = x + w - 6;
            int badgeY = top + (row.meta().isEmpty() ? (h - 11) / 2 : 4);
            if (!row.badge().isEmpty()) {
                rightEdge -= MenuTheme.badge(g, font, row.badge(), rightEdge,
                        badgeY, row.badgeRgb(), ctx.accent()) + 5;
            }
            if (!row.meta().isEmpty()) {
                int metaW = font.width(row.meta());
                g.text(font, row.meta(), x + w - 6 - metaW, badgeY + 12,
                        CoiStyle.TEXT_MUTED, false);
                rightEdge = Math.min(rightEdge, x + w - 6 - metaW - 5);
            }

            int textW = Math.max(10, rightEdge - textX);
            int titleColor = row.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE;
            int titleY = top + (h - (row.subtitle().isEmpty() ? 8 : 19) - (row.hasFraction() ? 4 : 0)) / 2;
            g.text(font, font.plainSubstrByWidth(row.title(), textW),
                    textX, titleY, titleColor, false);
            if (!row.subtitle().isEmpty()) {
                g.text(font, font.plainSubstrByWidth(row.subtitle(), textW),
                        textX, titleY + 11, CoiStyle.TEXT_MUTED, false);
            }
            if (row.hasFraction()) {
                MenuGauges.gauge(g, textX, top + h - 6, Math.max(10, rightEdge - textX), 2,
                        shown(row.fraction()), tint);
            }
            if (hovered) {
                if (!row.enabled() && !row.disabledReason().isEmpty()) ctx.reason(row.disabledReason());
                else ctx.lines(row.tooltip(), "");
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (clickable()) ctx.fire(row.action(), null);
            return true;
        }
    }

    /**
     * Icon tiles. v2 made the cell's own fields visible at last: the title is a
     * caption under the tile, the badge a corner pill and the colour a bottom
     * edge — all three were parsed and thrown away before.
     */
    public static class GridPart extends MenuPart {
        /**
         * How wide a captioned cell gets regardless of its tile. An ability name clipped to a 40px
         * tile is about seven characters, which is not a name — so when a grid carries captions the
         * cell is sized for the words and the tile is centred inside it.
         */
        private static final int CAPTION_MIN_W = 78;
        private static final int CAPTION_MAX_LINES = 2;

        private final List<MenuComponent.Row> cells;
        private final int columns;
        private final int tile;
        private final int cellW;
        private final int cellH;
        private final int captionLines;
        private final boolean captions;
        private final float[] hovers;

        GridPart(MenuContext ctx, MenuComponent.Grid grid) {
            super(ctx);
            this.cells = grid.cells();
            this.hovers = new float[grid.cells().size()];
            // `columns` is a hint, not a instruction, in both directions.
            //
            // Downward it always was: a document asking for more columns than the
            // card can hold gets fewer, rather than a row running off the edge.
            // Upward is new — a grid that honoured "5" literally on a wide card
            // drew five tiles at their authored size and left the rest of the row
            // empty, turning width the player has into vertical scroll they did
            // not want. So the row fills at the authored tile size, never past
            // the number of cells there actually are.
            int wanted = Math.max(1, grid.columns());
            int preferred = maxTile(grid.size());
            this.captions = cells.stream().anyMatch(cell -> !cell.title().isEmpty());

            // A captioned cell is as wide as its words need; a bare one is just its tile.
            int wantW = captions ? Math.max(preferred, CAPTION_MIN_W) : preferred;
            int minW = captions ? Math.max(MIN_TILE, CAPTION_MIN_W / 2) : MIN_TILE;
            int fitsAtWanted = Math.max(1, (ctx.contentW() + GAP) / (wantW + GAP));
            int floor = Math.max(1, (ctx.contentW() + GAP) / (minW + GAP));
            this.columns = Math.clamp(Math.max(wanted, fitsAtWanted), 1,
                    Math.min(floor, Math.max(1, cells.size())));
            this.cellW = Math.max(minW, (ctx.contentW() - (columns - 1) * GAP) / columns);
            this.tile = Math.clamp(Math.min(cellW, preferred), MIN_TILE, preferred);

            // Every row is as tall as the wordiest caption in the grid, so the tiles stay on a
            // line even when one name wraps and its neighbours do not.
            int lines = 1;
            if (captions) {
                for (MenuComponent.Row cell : cells) {
                    if (cell.title().isEmpty()) continue;
                    lines = Math.max(lines, font
                            .split(Component.literal(cell.title()), cellW).size());
                }
                lines = Math.min(lines, CAPTION_MAX_LINES);
            }
            this.captionLines = lines;
            this.cellH = tile + (captions ? captionLines * 9 + 3 : 0);
            int rows = (cells.size() + columns - 1) / columns;
            this.height = rows * (cellH + GAP) + 2;
        }

        private int maxTile(MenuComponent.TileSize size) {
            return switch (size) {
                case SMALL -> 18;
                case MEDIUM -> 28;
                case LARGE -> 40;
            };
        }

        private int cellX(int x, int index) {
            return x + (index % columns) * (cellW + GAP);
        }

        /** The tile sits centred in its cell, which may be wider to fit the caption. */
        private int tileX(int x, int index) {
            return cellX(x, index) + (cellW - tile) / 2;
        }

        private int cellY(int top, int index) {
            return top + 1 + (index / columns) * (cellH + GAP);
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.Row cell = cells.get(i);
                int cx = tileX(x, i);
                int cy = cellY(top, i);
                boolean hovered = MenuMetrics.inBox(mouseX, mouseY, cellX(x, i), cy, cellW, cellH);
                int border = cell.enabled() ? MenuTheme.argb(cell.rgb(), CoiStyle.BORDER) : MenuTheme.BORDER_OFF;
                hovers[i] = ctx.approach(hovers[i], hovered && cell.enabled() ? 1f : 0f, HOVER_MS);
                MenuTheme.panel(g, cx, cy, tile, tile, MenuTheme.surface(hovers[i]), border);
                // The art grows with the tile. Drawing a fixed 16 inside a 40px LARGE tile left
                // the icon floating in the middle of its own box, which read as the tile being
                // empty rather than as a large tile.
                int art = Math.max(ICON, tile - 12);
                int inset = (tile - art) / 2;
                MenuIcons.draw(g, font, cell.icon(), cx + inset, cy + inset, art,
                        cell.enabled() ? 1f : 0.4f);
                if (cell.rgb() != 0) {
                    g.fill(cx + 1, cy + tile - 2, cx + tile - 1, cy + tile,
                            MenuTheme.withAlpha(MenuTheme.argb(cell.rgb(), ctx.accent()),
                                    cell.enabled() ? 0.9f : 0.3f));
                }
                if (!cell.badge().isEmpty()) drawBadge(g, cell, cx, cy);
                if (captions && !cell.title().isEmpty()) {
                    int capX = cellX(x, i);
                    int capColor = cell.enabled() ? CoiStyle.TEXT_MUTED : CoiStyle.INACTIVE;
                    List<FormattedCharSequence> wrapped =
                            font.split(Component.literal(cell.title()), cellW);
                    for (int line = 0; line < Math.min(wrapped.size(), captionLines); line++) {
                        var seq = wrapped.get(line);
                        int lw = font.width(seq);
                        g.text(font, seq, capX + (cellW - lw) / 2,
                                cy + tile + 3 + line * 9, capColor, false);
                    }
                }
                if (hovered) {
                    if (!cell.enabled() && !cell.disabledReason().isEmpty()) ctx.reason(cell.disabledReason());
                    else ctx.lines(cell.tooltip(), cell.title());
                }
            }
        }

        /**
         * A pill needs room for its own padding; on a small tile it becomes a
         * corner dot, which still says "this one is different" without covering
         * the art the grid exists for.
         */
        private void drawBadge(GuiGraphicsExtractor g, MenuComponent.Row cell, int cx, int cy) {
            if (tile >= 28) {
                MenuTheme.badge(g, font, cell.badge(), cx + tile - 1, cy + 1,
                        cell.badgeRgb(), ctx.accent());
            } else {
                g.fill(cx + tile - 4, cy + 1, cx + tile - 1, cy + 4,
                        MenuTheme.argb(cell.badgeRgb(), ctx.accent()));
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            for (int i = 0; i < cells.size(); i++) {
                if (MenuMetrics.inBox(mx, my, cellX(x, i), cellY(top, i), cellW, cellH)) {
                    MenuComponent.Row cell = cells.get(i);
                    if (cell.enabled() && !cell.action().isEmpty()) ctx.fire(cell.action(), null);
                    return true;
                }
            }
            return true;
        }
    }

    /**
     * Side-by-side mini-cards — the one shape the renderer had no answer for,
     * since every other component is a full-width band. A cell with an
     * {@code action} is a button; one without is a read-only stat card.
     */
    public static class PanelsPart extends MenuPart {
        private final List<MenuComponent.PanelCell> cells;
        private final int columns;
        private final int cellW;
        private final int[] rowH;
        private final List<List<FormattedCharSequence>> subs = new ArrayList<>();
        private final float[] hovers;
        private final float[] fills;

        PanelsPart(MenuContext ctx, MenuComponent.Panels panels) {
            super(ctx);
            this.cells = panels.cells();
            // Columns yield to a cell floor, the same bargain the grid's tile
            // makes: fewer columns beats a cell too narrow to read
            int wanted = Math.clamp(panels.columns(), 1, 3);
            int fits = Math.max(1, (ctx.contentW() + GAP) / (MIN_PANEL_W + GAP));
            this.columns = Math.min(wanted, fits);
            this.cellW = (ctx.contentW() - (columns - 1) * GAP) / columns;
            this.hovers = new float[cells.size()];
            this.fills = new float[cells.size()];

            int rows = (cells.size() + columns - 1) / columns;
            this.rowH = new int[rows];
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.PanelCell cell = cells.get(i);
                List<FormattedCharSequence> sub = cell.subtitle().isEmpty() ? List.of()
                        : font.split(Component.literal(cell.subtitle()), cellW - 12);
                // Two lines is the whole budget: a mini-card that grows a
                // paragraph is a note wearing the wrong component
                subs.add(sub.size() > 2 ? sub.subList(0, 2) : sub);
                rowH[i / columns] = Math.max(rowH[i / columns], cellHeight(cell, subs.get(i).size()));
            }
            int h = 2;
            for (int r : rowH) h += r + GAP;
            this.height = h;
        }

        private int cellHeight(MenuComponent.PanelCell cell, int subLines) {
            boolean chrome = cell.icon().present() || !cell.badge().isEmpty();
            int h = 6 + (chrome ? ICON + 3 : 0) + 11;
            if (!cell.value().isEmpty()) h += 13;
            h += subLines * 9;
            if (cell.hasFraction()) h += 6;
            return h + 6;
        }

        private int cellX(int x, int index) {
            return x + (index % columns) * (cellW + GAP);
        }

        private int cellY(int top, int index) {
            int y = top + 1;
            for (int r = 0; r < index / columns; r++) y += rowH[r] + GAP;
            return y;
        }

        private boolean clickable(MenuComponent.PanelCell cell) {
            return cell.enabled() && !cell.action().isEmpty();
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < cells.size(); i++) {
                MenuComponent.PanelCell cell = cells.get(i);
                int cx = cellX(x, i);
                int cy = cellY(top, i);
                int ch = rowH[i / columns];
                boolean hovered = MenuMetrics.inBox(mouseX, mouseY, cx, cy, cellW, ch);
                hovers[i] = ctx.approach(hovers[i], hovered && clickable(cell) ? 1f : 0f, HOVER_MS);

                int color = MenuTheme.argb(cell.rgb(), ctx.accent());
                MenuTheme.panel(g, cx, cy, cellW, ch, MenuTheme.surface(hovers[i]),
                        cell.enabled() ? CoiStyle.BORDER : MenuTheme.BORDER_OFF);
                if (cell.rgb() != 0) {
                    g.fill(cx + 1, cy, cx + cellW - 1, cy + 2,
                            MenuTheme.withAlpha(color, cell.enabled() ? 0.9f : 0.3f));
                }

                int left = cx + 6;
                int right = cx + cellW - 6;
                int y = cy + 6;
                boolean chrome = cell.icon().present() || !cell.badge().isEmpty();
                if (chrome) {
                    MenuIcons.draw(g, font, cell.icon(), left, y, ICON,
                            cell.enabled() ? 1f : 0.4f);
                    if (!cell.badge().isEmpty()) {
                        MenuTheme.badge(g, font, cell.badge(), right, y + 2,
                                cell.badgeRgb(), ctx.accent());
                    }
                    y += ICON + 3;
                }

                int textW = cellW - 12;
                g.text(font,
                        font.plainSubstrByWidth(cell.title(), textW), left, y,
                        cell.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE, false);
                y += 11;
                if (!cell.value().isEmpty()) {
                    MenuTheme.scaledText(g, font,
                            font.plainSubstrByWidth(cell.value(), (int) (textW / 1.25f)),
                            left, y, 1.25f, cell.enabled() ? color : CoiStyle.INACTIVE, false);
                    y += 13;
                }
                for (FormattedCharSequence line : subs.get(i)) {
                    g.text(font, line, left, y, CoiStyle.TEXT_MUTED, false);
                    y += 9;
                }
                if (cell.hasFraction()) {
                    fills[i] = ctx.approach(fills[i], (float) cell.fraction(), GAUGE_MS);
                    MenuGauges.gauge(g, left, cy + ch - 7, textW, 3, fills[i], color);
                }

                if (!hovered) continue;
                if (!cell.enabled() && !cell.disabledReason().isEmpty()) ctx.reason(cell.disabledReason());
                else ctx.lines(cell.tooltip(), "");
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            for (int i = 0; i < cells.size(); i++) {
                if (!MenuMetrics.inBox(mx, my, cellX(x, i), cellY(top, i), cellW, rowH[i / columns])) continue;
                MenuComponent.PanelCell cell = cells.get(i);
                if (clickable(cell)) ctx.fire(cell.action(), null);
                return true;
            }
            return true;
        }
    }

}
