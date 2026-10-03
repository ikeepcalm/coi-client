package dev.ua.ikeepcalm.coi.screen.glyph;

import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.domain.glyph.GlyphDrawing;
import dev.ua.ikeepcalm.coi.domain.glyph.GlyphSheet;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Every pixel of the glyph canvas: the parchment with its ring guide, the
 * strokes, and the reference sheet. Holds no state; the screen hands it
 * everything.
 */
final class GlyphCanvasPainter {

    /**
     * Where the guide ring sits, as a fraction of the canvas side. The server
     * reads signs centred up to {@link #SIGN_BAND} ring radii out, which fits
     * inside the square.
     */
    static final float RING = 0.30f;
    private static final float SIGIL_ZONE = 0.45f;
    private static final float SIGN_SLOT = 1.2f;
    private static final float SIGN_BAND = 1.3f;

    private static final int PARCHMENT = 0xF01A1622;
    private static final int GUIDE = 0x50E8D8B0;
    private static final int GUIDE_FAINT = 0x24E8D8B0;
    static final int INK = 0xFFEDE4FF;
    private static final int INK_ACTIVE = 0xFFFFE9A8;
    private static final int CELL_HOVER = 0x20FFFFFF;

    private GlyphCanvasPainter() {
    }

    /**
     * The drawing surface of one layer. Odd layers (1st, 3rd, …) mark the cross
     * slots, even layers the corners, as in the server's GUI editor.
     */
    static void canvas(GuiGraphicsExtractor g, int x, int y, int size, boolean evenLayer,
                       List<GlyphDrawing.Stroke> strokes, List<double[]> active) {
        g.fill(x, y, x + size, y + size, PARCHMENT);
        g.outline(x, y, size, size, CoiStyle.BORDER);
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float ring = size * RING;
        circle(g, cx, cy, ring, GUIDE);
        circle(g, cx, cy, ring * SIGIL_ZONE, GUIDE_FAINT);
        circle(g, cx, cy, ring * SIGN_BAND, GUIDE_FAINT);
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(-90 + k * 90 + (evenLayer ? -45 : 0));
            float sx = cx + (float) Math.cos(a) * ring * SIGN_SLOT;
            float sy = cy + (float) Math.sin(a) * ring * SIGN_SLOT;
            g.fill((int) sx - 2, (int) sy - 2, (int) sx + 2, (int) sy + 2, GUIDE);
        }
        float scale = size / (float) GlyphDrawing.CANVAS;
        for (GlyphDrawing.Stroke stroke : strokes) {
            int[] xy = stroke.xy();
            for (int i = 2; i < xy.length; i += 2) {
                EffectPaint.line(g, x + xy[i - 2] * scale, y + xy[i - 1] * scale,
                        x + xy[i] * scale, y + xy[i + 1] * scale, INK, 2);
            }
        }
        for (int i = 1; i < active.size(); i++) {
            EffectPaint.line(g, x + (float) active.get(i - 1)[0] * scale, y + (float) active.get(i - 1)[1] * scale,
                    x + (float) active.get(i)[0] * scale, y + (float) active.get(i)[1] * scale, INK_ACTIVE, 2);
        }
    }

    /**
     * The reference sheet: sigils first, then signs, in a grid that shrinks to
     * fit. Returns the glyph under the cursor, for the tooltip.
     */
    static GlyphSheet.Glyph reference(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                                      List<GlyphSheet.Glyph> glyphs, int mouseX, int mouseY) {
        CoiStyle.drawCard(g, x, y, w, h);
        int columns = Math.max(1, (w - 8) / 44);
        int cellW = (w - 8) / columns;
        long signCount = glyphs.stream().filter(GlyphSheet.Glyph::sign).count();
        long rows = Math.ceilDiv(glyphs.size() - signCount, columns) + Math.ceilDiv(signCount, columns);
        int cellH = (int) Math.clamp((h - 34) / Math.max(1, rows), 14, 46);
        int cursorY = y + 5;
        GlyphSheet.Glyph hovered = null;
        for (boolean signs : new boolean[]{false, true}) {
            g.text(font, Component.translatable(signs ? "screen.coi.glyph_signs" : "screen.coi.glyph_sigils"),
                    x + 5, cursorY, CoiStyle.ACCENT, false);
            cursorY += 12;
            int column = 0;
            for (GlyphSheet.Glyph glyph : glyphs) {
                if (glyph.sign() != signs) continue;
                int cx = x + 4 + column * cellW;
                if (mouseX >= cx && mouseX < cx + cellW && mouseY >= cursorY && mouseY < cursorY + cellH) {
                    g.fill(cx, cursorY, cx + cellW, cursorY + cellH, CELL_HOVER);
                    hovered = glyph;
                }
                cell(g, font, glyph, cx, cursorY, cellW, cellH);
                if (++column == columns) {
                    column = 0;
                    cursorY += cellH;
                }
            }
            if (column != 0) cursorY += cellH;
        }
        return hovered;
    }

    private static void cell(GuiGraphicsExtractor g, Font font, GlyphSheet.Glyph glyph, int x, int y, int w, int h) {
        boolean named = h >= 30;
        int art = Math.max(8, Math.min(w, named ? h - 11 : h) - 6);
        glyph(g, glyph, x + w / 2f, y + 3 + art / 2f, art);
        if (named) {
            String name = font.plainSubstrByWidth(glyph.name(), w - 2);
            g.text(font, name, x + (w - font.width(name)) / 2, y + h - 10, CoiStyle.TEXT_MUTED, false);
        }
    }

    /**
     * One template, scaled into a square of {@code size}. Directed signs get a
     * dot where the stroke starts; sigils get one per stroke, because the
     * server wants a sigil drawn with the same number of strokes.
     */
    static void glyph(GuiGraphicsExtractor g, GlyphSheet.Glyph glyph, float cx, float cy, int size) {
        float scale = size / 200f;
        for (int[] xy : glyph.strokes()) {
            for (int i = 2; i < xy.length; i += 2) {
                EffectPaint.line(g, cx + xy[i - 2] * scale, cy + xy[i - 1] * scale,
                        cx + xy[i] * scale, cy + xy[i + 1] * scale, INK, 1);
            }
            if ((!glyph.sign() || !"any".equals(glyph.flow())) && xy.length >= 2) {
                int sx = (int) (cx + xy[0] * scale);
                int sy = (int) (cy + xy[1] * scale);
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, CoiStyle.ACCENT);
            }
        }
    }

    private static void circle(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a0 = 2 * Math.PI * i / segments;
            double a1 = 2 * Math.PI * (i + 1) / segments;
            EffectPaint.line(g, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r,
                    cx + (float) Math.cos(a1) * r, cy + (float) Math.sin(a1) * r, color, 1);
        }
    }
}
