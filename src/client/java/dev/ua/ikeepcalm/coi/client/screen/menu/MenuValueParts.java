package dev.ua.ikeepcalm.coi.client.screen.menu;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.GAP;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.HERO_ICON;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.HERO_RING;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.HOVER_MS;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.SMALL_ICON;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.STAT_RING;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The parts that carry a value: a stat and its gauge, a two-column table, a
 * checklist, a row of pills, and the hero block that is the screen's own
 * identity.
 */
final class MenuValueParts {

    private MenuValueParts() {
    }

    /**
     * Label, value and a gauge, where the gauge can be a bar, ten notches or a
     * 24px arc — the arc is for the one number on the screen that is the point
     * of the screen.
     */
    static final class StatPart extends MenuPart {
        private final MenuComponent.Stat stat;
        private final int color;
        private final boolean ring;

        StatPart(MenuContext ctx, MenuComponent.Stat stat) {
            super(ctx);
            this.stat = stat;
            this.color = MenuTheme.argb(stat.rgb(), ctx.accent());
            this.ring = stat.hasBar() && stat.style() == MenuComponent.GaugeStyle.RING;
            this.height = (ring ? STAT_RING : 11 + (stat.hasBar() ? 8 : 0)) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            // The arc lives at the right edge, so the text line stops short of it
            int right = x + w - (ring ? STAT_RING + 6 : 0);
            int labelX = x;
            if (MenuIcons.draw(g, font, stat.icon(), x, top, SMALL_ICON, 1f)) {
                labelX += SMALL_ICON + 3;
            }

            int deltaW = 0;
            if (!stat.delta().isEmpty()) {
                int dw = font.width(stat.delta());
                g.text(font, stat.delta(), right - dw, top + 1,
                        MenuTheme.deltaColor(stat.delta()), false);
                deltaW = dw + 5;
            }
            int valueW = font.width(stat.value());
            g.text(font, stat.value(), right - deltaW - valueW, top + 1, color, false);
            g.text(font, font.plainSubstrByWidth(stat.label(),
                            Math.max(10, right - deltaW - valueW - 6 - labelX)),
                    labelX, top + 1, CoiStyle.TEXT_BODY, false);

            if (stat.hasBar()) {
                double fill = shown(stat.fraction());
                switch (stat.style()) {
                    case RING -> MenuGauges.ring(g, x + w - STAT_RING / 2, top + STAT_RING / 2,
                            STAT_RING / 2, 3, fill, color);
                    case SEGMENTS -> {
                        MenuGauges.segments(g, x, top + 12, w, 4,
                                stat.hasCap() ? Math.min(fill, stat.cap()) : fill, color, 10);
                        if (stat.hasCap()) MenuGauges.capBand(g, x, top + 12, w, 4, stat.cap(), color);
                    }
                    case BAR -> {
                        if (stat.hasCap()) MenuGauges.gauge(g, x, top + 12, w, 4, fill, color, stat.cap());
                        else MenuGauges.gauge(g, x, top + 12, w, 4, fill, color);
                    }
                }
            }
            if (!stat.hint().isEmpty() && MenuMetrics.inBox(mouseX, mouseY, x, top, w, height)) ctx.hint(stat.hint());
        }
    }

    static final class KvPart extends MenuPart {
        private final List<MenuComponent.KvRow> rows;

        KvPart(MenuContext ctx, MenuComponent.Kv kv) {
            super(ctx);
            this.rows = kv.rows();
            this.height = rows.size() * 11 + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            int y = top + 1;
            for (MenuComponent.KvRow row : rows) {
                int labelX = x;
                if (MenuIcons.draw(g, font, row.icon(), x, y - 2, SMALL_ICON, 1f)) {
                    labelX += SMALL_ICON + 3;
                }
                int valueW = font.width(row.value());
                g.text(font,
                        font.plainSubstrByWidth(row.label(),
                                Math.max(10, w - valueW - 6 - (labelX - x))),
                        labelX, y, CoiStyle.TEXT_MUTED, false);
                g.text(font, row.value(), x + w - valueW, y,
                        MenuTheme.argb(row.rgb(), CoiStyle.TEXT_BODY), false);
                if (!row.hint().isEmpty() && MenuMetrics.inBox(mouseX, mouseY, x, y - 1, w, 11)) ctx.hint(row.hint());
                y += 11;
            }
        }
    }

    static final class ChecklistPart extends MenuPart {
        private final List<MenuComponent.Check> items;

        ChecklistPart(MenuContext ctx, MenuComponent.Checklist checklist) {
            super(ctx);
            this.items = checklist.items();
            int h = 3;
            for (MenuComponent.Check item : items) h += item.detail().isEmpty() ? 11 : 20;
            this.height = h;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int y = top + 1;
            for (MenuComponent.Check item : items) {
                // An icon stands in for the glyph entirely: a tick beside a
                // symbol says the same thing twice
                if (!MenuIcons.draw(g, font, item.icon(), x, y - 2, SMALL_ICON, 1f)) {
                    MenuTheme.check(g, x, y, item.state(), item.rgb());
                }
                g.text(font,
                        font.plainSubstrByWidth(item.label(), ctx.contentW() - 16),
                        x + 16, y, MenuTheme.argb(item.rgb(), MenuTheme.checkLabelColor(item.state())), false);
                y += 11;
                if (!item.detail().isEmpty()) {
                    g.text(font,
                            font.plainSubstrByWidth(item.detail(), ctx.contentW() - 16),
                            x + 16, y, CoiStyle.TEXT_MUTED, false);
                    y += 9;
                }
            }
        }
    }

    /**
     * A wrapping row of pills. The layout is decided once, in the constructor,
     * because a wrapping row's height is not knowable from its item count.
     */
    static final class ChipsPart extends MenuPart {
        private final List<MenuComponent.Chip> items;
        private final int[][] spots;
        private final float[] hovers;

        ChipsPart(MenuContext ctx, MenuComponent.Chips chips) {
            super(ctx);
            this.items = chips.items();
            this.spots = new int[items.size()][3];
            this.hovers = new float[items.size()];
            int w = ctx.contentW();
            int x = 0;
            int y = 0;
            for (int i = 0; i < items.size(); i++) {
                int cw = Math.min(w, MenuTheme.chipWidth(font, items.get(i)));
                if (x > 0 && x + cw > w) {
                    x = 0;
                    y += MenuTheme.CHIP_H + GAP;
                }
                spots[i] = new int[]{x, y, cw};
                x += cw + GAP;
            }
            this.height = (items.isEmpty() ? 0 : y + MenuTheme.CHIP_H) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            for (int i = 0; i < items.size(); i++) {
                int cx = x + spots[i][0];
                int cy = top + 1 + spots[i][1];
                boolean hovered = MenuMetrics.inBox(mouseX, mouseY, cx, cy, spots[i][2], MenuTheme.CHIP_H);
                // Per chip, not per part: one shared tween would fade the whole
                // row whenever any one of them is pointed at
                hovers[i] = ctx.approach(hovers[i], hovered ? 1f : 0f, HOVER_MS);
                MenuTheme.chip(g, font, items.get(i), cx, cy, ctx.accent(), hovers[i]);
                if (hovered) ctx.hint(items.get(i).hint());
            }
        }
    }

    /**
     * The screen's identity block. Everything else on the card is a band of the
     * same weight; this one is a panel in the document's own colour, with the
     * title at 1.5× and the one number that matters as a bar or an arc.
     */
    static final class HeroPart extends MenuPart {
        private final MenuComponent.Hero hero;
        private final int color;
        private final boolean ringGauge;
        private final boolean barGauge;
        private final int core;
        private final float[] hovers;

        HeroPart(MenuContext ctx, MenuComponent.Hero hero) {
            super(ctx);
            this.hero = hero;
            this.color = MenuTheme.argb(hero.rgb(), ctx.accent());
            this.ringGauge = hero.hasFraction() && hero.style() == MenuComponent.HeroStyle.RING;
            this.barGauge = hero.hasFraction() && hero.style() == MenuComponent.HeroStyle.PLAIN;
            this.hovers = new float[hero.chips().size()];
            int titleBlock = 12 + (hero.subtitle().isEmpty() ? 0 : 11);
            // The ring is drawn *beside* the text, so it widens the block's
            // minimum rather than stacking under it — the contract's additive
            // formula would leave a 34px empty band under a gauge that is not
            // there
            this.core = Math.max(Math.max(HERO_ICON, titleBlock), ringGauge ? HERO_RING + 6 : 0);
            this.height = 10 + core + (barGauge ? 10 : 0)
                    + (hero.chips().isEmpty() ? 0 : MenuTheme.CHIP_H + GAP) + 8 + 4;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            int h = height - 4;
            MenuTheme.panel(g, x, top, w, h, MenuTheme.withAlpha(ctx.accent(), 0.08f),
                    MenuTheme.withAlpha(ctx.accent(), 0.30f));

            int left = x + 8;
            int right = x + w - 8;
            int coreTop = top + 10;

            // The ring claims the right edge *before* the badge is placed. Both used
            // to anchor there independently, so a hero with a gauge and a state word
            // — the mythical form screen, every time it is recovering — drew the
            // badge straight through the ring.
            if (ringGauge) {
                int centreX = x + w - 8 - HERO_RING / 2;
                int centreY = coreTop + core / 2;
                MenuGauges.ring(g, centreX, centreY, HERO_RING / 2, 4, shown(hero.fraction()), color);
                if (!hero.fractionLabel().isEmpty()) {
                    int lw = font.width(hero.fractionLabel());
                    g.text(font, hero.fractionLabel(), centreX - lw / 2, centreY - 4,
                            CoiStyle.TEXT_BODY, false);
                }
                right = x + w - 8 - HERO_RING - 8;
            }
            if (!hero.badge().isEmpty()) {
                right -= MenuTheme.badge(g, font, hero.badge(), right, top + 6,
                        hero.badgeRgb(), ctx.accent()) + 6;
            }
            if (MenuIcons.draw(g, font, hero.icon(), left,
                    coreTop + (core - HERO_ICON) / 2, HERO_ICON, 1f)) {
                left += HERO_ICON + 8;
            }

            int textW = Math.max(20, right - left);
            int textY = coreTop + (core - (hero.subtitle().isEmpty() ? 12 : 23)) / 2;
            // plainSubstrByWidth measures at 1×, so the budget is divided by the
            // scale before clipping rather than after
            MenuTheme.scaledText(g, font,
                    font.plainSubstrByWidth(hero.title(), (int) (textW / 1.5f)),
                    left, textY, 1.5f, color, true);
            if (!hero.subtitle().isEmpty()) {
                g.text(font,
                        font.plainSubstrByWidth(hero.subtitle(), textW),
                        left, textY + 14, CoiStyle.TEXT_MUTED, false);
            }

            int y = coreTop + core;
            if (barGauge) {
                if (!hero.fractionLabel().isEmpty()) {
                    int lw = font.width(hero.fractionLabel());
                    g.text(font, hero.fractionLabel(), x + w - 8 - lw, y - 10,
                            CoiStyle.TEXT_MUTED, false);
                }
                MenuGauges.gauge(g, x + 8, y, w - 16, 6, shown(hero.fraction()), color);
                y += 10;
            }
            if (!hero.chips().isEmpty()) drawChips(g, x + 8, y + GAP, w - 16, mouseX, mouseY);
        }

        /**
         * One row, clipped rather than wrapped: the hero is a fixed-height block
         * and the contract caps it at four chips, so a fifth would only make the
         * card jump.
         */
        private void drawChips(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
            int chipX = x;
            for (int i = 0; i < hero.chips().size(); i++) {
                MenuComponent.Chip chip = hero.chips().get(i);
                int cw = MenuTheme.chipWidth(font, chip);
                if (chipX + cw > x + w && chipX > x) break;
                boolean hovered = MenuMetrics.inBox(mouseX, mouseY, chipX, y, cw, MenuTheme.CHIP_H);
                hovers[i] = ctx.approach(hovers[i], hovered ? 1f : 0f, HOVER_MS);
                MenuTheme.chip(g, font, chip, chipX, y, ctx.accent(), hovers[i]);
                if (hovered) ctx.hint(chip.hint());
                chipX += cw + GAP;
            }
        }
    }

}
