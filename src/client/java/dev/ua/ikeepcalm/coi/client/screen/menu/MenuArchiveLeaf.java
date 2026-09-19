package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The left leaf carries an engraved subject and an index of real document sections.
 */
public class MenuArchiveLeaf {
    private final List<Target> targets = new ArrayList<>();

    private static int active(List<Anchor> anchors, int scroll) {
        int result = 0;
        for (int i = 0; i < anchors.size(); i++) if (anchors.get(i).offset() <= scroll + 8) result = i;
        return result;
    }

    private static void ornament(GuiGraphicsExtractor g, String template, int cx, int cy, int r, int accent) {
        int dim = MenuTheme.withAlpha(accent, .32f);
        int bright = MenuTheme.withAlpha(accent, .75f);
        switch (template) {
            case "atlas" -> {
                // A navigator's astrolabe: meridians, compass points and a fixed star field.
                ArchivePaint.seal(g, cx, cy, r, dim);
                for (int i = -2; i <= 2; i++) {
                    int offset = i * r / 3;
                    line(g, cx - r, cy + offset, cx + r, cy + offset, dim);
                    line(g, cx + offset, cy - r, cx + offset, cy + r, dim);
                }
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r);
                    line(g, cx, cy, px, py, bright);
                }
            }
            case "relic" -> {
                // Faceted reliquary suspended in a containment frame.
                ArchivePaint.registration(g, cx - r, cy - r, r * 2, r * 2, bright);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3, b = (i + 1) * Math.PI / 3;
                    int px = cx + (int) (Math.cos(a) * r * .8), py = cy + (int) (Math.sin(a) * r * .8);
                    int qx = cx + (int) (Math.cos(b) * r * .8), qy = cy + (int) (Math.sin(b) * r * .8);
                    line(g, px, py, qx, qy, bright);
                    line(g, px, py, cx, cy, dim);
                }
            }
            case "inscription" -> {
                // An engraved tablet; no invented script or gameplay information.
                g.outline(cx - r * 3 / 4, cy - r, r * 3 / 2, r * 2, dim);
                g.outline(cx - r * 3 / 4 + 5, cy - r + 5, r * 3 / 2 - 10, r * 2 - 10, dim);
                for (int i = 0; i < 5; i++) {
                    int yy = cy + r / 3 + i * 6;
                    if (yy < cy + r - 8) line(g, cx - r / 2, yy, cx + r / 2, yy, dim);
                }
                ArchivePaint.seal(g, cx, cy - r / 4, Math.max(8, r / 3), bright);
            }
            case "challenge" -> {
                // Ascending dais with a crown at its summit.
                for (int i = 0; i < 5; i++) {
                    int half = r - i * r / 7, yy = cy + r - i * r / 5;
                    line(g, cx - half, yy, cx + half, yy, dim);
                    line(g, cx - half, yy, cx - half, yy - r / 5, dim);
                    line(g, cx + half, yy, cx + half, yy - r / 5, dim);
                }
                line(g, cx - r / 2, cy - r / 3, cx - r * 2 / 3, cy - r, bright);
                line(g, cx - r * 2 / 3, cy - r, cx - r / 4, cy - r * 2 / 3, bright);
                line(g, cx - r / 4, cy - r * 2 / 3, cx, cy - r, bright);
                line(g, cx, cy - r, cx + r / 4, cy - r * 2 / 3, bright);
                line(g, cx + r / 4, cy - r * 2 / 3, cx + r * 2 / 3, cy - r, bright);
                line(g, cx + r * 2 / 3, cy - r, cx + r / 2, cy - r / 3, bright);
            }
            default -> {
                // Bound church register: paired columns, ribs and a circular seal.
                for (int side : new int[]{-1, 1}) {
                    int xx = cx + side * r * 3 / 4;
                    line(g, xx, cy - r / 2, xx, cy + r, bright);
                    line(g, xx + side * 5, cy - r / 2, xx + side * 5, cy + r, dim);
                    line(g, xx, cy - r / 2, cx, cy - r, bright);
                }
                ArchivePaint.seal(g, cx, cy, Math.max(12, r / 2), dim);
                line(g, cx - r, cy + r, cx + r, cy + r, bright);
            }
        }
    }

    private static void line(GuiGraphicsExtractor g, int x, int y, int x2, int y2, int color) {
        EffectPaint.line(g, x, y, x2, y2, color, 1);
    }

    void draw(GuiGraphicsExtractor g, Font font, MenuDocument doc, List<Anchor> anchors,
              int x, int y, int w, int h, int scroll, int mx, int my, boolean compact) {
        targets.clear();
        ArchivePaint.folio(g, x, y, w, h);
        int accent = doc.accentArgb();
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        if (compact) {
            int cellW = Math.max(45, (w - 16) / Math.max(1, Math.min(anchors.size(), 4)));
            int shown = Math.min(4, anchors.size());
            // A short strip follows the current section, keeping long documents navigable.
            int active = active(anchors, scroll);
            int start = Math.clamp(active - 1, 0, Math.max(0, anchors.size() - shown));
            for (int i = 0; i < shown; i++) {
                int index = start + i;
                index(g, font, anchors.get(index), index, x + 8 + i * cellW, y + 7,
                        cellW - 3, 22, index == active, accent, mx, my);
            }
            if (anchors.isEmpty()) ornament(g, doc.presentation().template(), x + w / 2, y + h / 2, 15, accent);
        } else {
            int indexH = Math.min(h / 2, anchors.size() * 25 + 12);
            int plateH = Math.max(60, h - indexH - 20);
            int radius = Math.max(22, Math.min(w / 2 - 24, plateH / 2 - 14));
            ornament(g, doc.presentation().template(), x + w / 2, y + 12 + plateH / 2, radius, accent);
            int iconY = y + 12 + plateH / 2;
            if (doc.presentation().template().equals("inscription")) iconY -= radius / 4;
            int iconSize = doc.presentation().template().equals("inscription")
                    ? Math.min(28, Math.max(10, radius / 2)) : 28;
            MenuIcons.draw(g, font, doc.icon(), x + w / 2 - iconSize / 2,
                    iconY - iconSize / 2, iconSize, 1f);
            int top = y + plateH + 18;
            g.fill(x + 16, top - 7, x + w - 16, top - 6, ArchivePaint.RULE);
            int shown = Math.max(1, (h - (top - y) - 8) / 25);
            int active = active(anchors, scroll);
            int start = Math.clamp(active - shown / 2, 0, Math.max(0, anchors.size() - shown));
            for (int i = start; i < Math.min(anchors.size(), start + shown); i++) {
                index(g, font, anchors.get(i), i, x + 12, top + (i - start) * 25,
                        w - 24, 23, i == active, accent, mx, my);
            }
        }
        g.disableScissor();
    }

    private void index(GuiGraphicsExtractor g, Font font, Anchor anchor, int number,
                       int x, int y, int w, int h, boolean selected, int accent, int mx, int my) {
        boolean hover = MenuMetrics.inBox(mx, my, x, y, w, h);
        if (hover || selected) g.fill(x, y, x + w, y + h, MenuTheme.withAlpha(accent, hover ? .15f : .08f));
        if (selected) g.fill(x, y + 3, x + 2, y + h - 3, accent);
        String ordinal = String.format(Locale.ROOT, "%02d", number + 1);
        g.text(font, ordinal, x + 6, y + 7, selected ? accent : 0xFF858575, false);
        g.text(font, font.plainSubstrByWidth(anchor.title(), Math.max(1, w - 31)), x + 25, y + 7,
                selected || hover ? ArchivePaint.LABEL : 0xFFA6AA9B, false);
        targets.add(new Target(x, y, w, h, anchor.offset()));
    }

    int hit(double x, double y) {
        for (Target target : targets)
            if (MenuMetrics.inBox(x, y, target.x, target.y, target.w, target.h)) return target.offset;
        return -1;
    }

    public record Anchor(String title, int offset) {
    }

    private record Target(int x, int y, int w, int h, int offset) {
    }
}
