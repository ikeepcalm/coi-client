package dev.ua.ikeepcalm.coi.client.ui;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Stationery and engraved registration marks, drawn at the current GUI resolution. */
public class ArchivePaint {
    public static final int PAPER = 0xFFD6CCB5;
    public static final int INK = 0xFF35352F;
    public static final int FAINT_INK = 0xFF797363;
    public static final int DESK = 0xFA191D1C;
    public static final int RULE = 0xFF494C43;
    public static final int LABEL = 0xFFE4D9BF;

    private ArchivePaint() {}

    public static void folio(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 4, y + 5, x + w + 4, y + h + 5, 0x60000000);
        g.fill(x, y, x + w, y + h, DESK);
        g.outline(x, y, w, h, RULE);
        g.fill(x + 4, y + 1, x + 6, y + h - 1, 0xFF32372E);
        g.fill(x + 8, y + 1, x + 9, y + h - 1, 0xFF101410);
    }

    public static void paper(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PAPER);
        g.outline(x, y, w, h, 0xFFB3A991);
        // Sparse fixed fibres: no random work or animated noise in a reading surface.
        for (int row = 9; row < h - 5; row += 19) {
            int inset = 7 + (row * 13 % Math.max(1, w - 20));
            g.fill(x + inset, y + row, x + Math.min(w - 4, inset + 7), y + row + 1, 0x0D554B36);
        }
        registration(g, x + 7, y + 7, w - 14, h - 14, 0x50797363);
    }

    public static void registration(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w < 16 || h < 16) return;
        for (int dx : new int[]{0, w}) {
            for (int dy : new int[]{0, h}) {
                int sx = dx == 0 ? 1 : -1;
                int sy = dy == 0 ? 1 : -1;
                EffectPaint.line(g, x + dx, y + dy, x + dx + sx * 7, y + dy, color, 1);
                EffectPaint.line(g, x + dx, y + dy, x + dx, y + dy + sy * 7, color, 1);
            }
        }
    }

    public static void seal(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        for (int ring = 0; ring < 2; ring++) {
            int r = radius - ring * 4;
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI / 24;
                double b = (i + 1) * Math.PI / 24;
                EffectPaint.line(g, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r,
                        cx + (float) Math.cos(b) * r, cy + (float) Math.sin(b) * r, color, 1);
            }
        }
    }
}
