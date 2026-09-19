package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Small, bounded procedural scenes: their intensity follows only the server's sequence.
 */
public class PathwayPortraitScene {

    private PathwayPortraitScene() {
    }

    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, String pathway, int sequence) {
        if (w < 8 || h < 8) return;
        String key = Pathways.normalizePathway(pathway);
        float power = sequence < 0 ? 0 : (9 - Math.clamp(sequence, 0, 9)) / 9f;
        double time = HudConfig.getSettings().epilepsyMode ? 0 : (System.nanoTime() / 1_000_000_000.0) % 3600;
        int rgb = Pathways.pathwayRgb(key);
        int dark = shade(rgb, 0.09f);
        g.fillGradient(x, y, x + w, y + h, 0xFF000000 | dark, 0xFF000000 | shade(rgb, 0.22f));
        g.enableScissor(x, y, x + w, y + h);
        try {
            int cx = x + w / 2;
            int cy = y + h * 43 / 100;
            int radius = Math.max(12, Math.min(w * 43 / 100, h * 36 / 100));
            halo(g, cx, cy, radius, rgb, power, time);
            switch (key) {
                case "priest" -> flames(g, x, y, w, h, power, time, 0xFF7035);
                case "fool" -> masks(g, x, y, w, h, power, time);
                case "door" -> stars(g, x, y, w, h, power, time, rgb, true);
                case "sun", "hermit", "tyrant", "chained", "paragon", "tower", "fortune",
                     "aeon", "patriarch", "sublunary" -> PortraitElements.draw(g, key, x, y, w, h, power, time, rgb);
                case "abyss", "death", "demoness", "darkness", "moon", "mother", "hanged",
                     "giant", "emperor", "justiciar", "visionary", "error" ->
                        PortraitArcana.draw(g, key, x, y, w, h, power, time, rgb);
                default -> constellations(g, x, y, w, h, power, time, rgb);
            }
            // A dark grounding line keeps the figure readable without another paper panel.
            g.fillGradient(x, y + h * 4 / 5, x + w, y + h, 0x00101418, 0xD0101418);
            ArchivePaint.registration(g, x + 6, y + 6, w - 12, h - 12, EffectPaint.argb(rgb, 110));
        } finally {
            g.disableScissor();
        }
    }

    private static int shade(int rgb, float factor) {
        return (int) (((rgb >> 16) & 255) * factor) << 16
                | (int) (((rgb >> 8) & 255) * factor) << 8 | (int) ((rgb & 255) * factor);
    }

    private static void halo(GuiGraphicsExtractor g, int cx, int cy, int r, int rgb, float power, double time) {
        ArchivePaint.seal(g, cx, cy, r, EffectPaint.argb(rgb, 65 + (int) (power * 35)));
        double angle = time * 0.045;
        int count = 12;
        for (int i = 0; i < count; i++) {
            double a = angle + i * Math.PI * 2 / count;
            float px = cx + (float) Math.cos(a) * r;
            float py = cy + (float) Math.sin(a) * r;
            float qx = cx + (float) Math.cos(a) * (r - 7);
            float qy = cy + (float) Math.sin(a) * (r - 7);
            EffectPaint.line(g, px, py, qx, qy, EffectPaint.argb(rgb, 160), 1);
            if (i % 3 == 0) diamond(g, (int) px, (int) py, 3, EffectPaint.argb(rgb, 185));
            if (power >= 0.65f) {
                double b = -angle + i * Math.PI * 2 / count;
                diamond(g, cx + (int) (Math.cos(b) * (r + 8)), cy + (int) (Math.sin(b) * (r + 8)), 2,
                        EffectPaint.argb(rgb, 100));
            }
        }
        if (power == 1f) ArchivePaint.seal(g, cx, cy, r + 14, EffectPaint.argb(rgb, 75));
    }

    private static void flames(GuiGraphicsExtractor g, int x, int y, int w, int h, float power, double time, int rgb) {
        int count = 7 + (int) (power * 8);
        for (int i = 0; i < count; i++) {
            float center = x + (i + 0.5f) * w / count;
            float height = h * (0.19f + power * 0.43f) * (0.65f + (i * 17 % 11) / 23f);
            float breadth = Math.max(3, w / (float) count * 0.72f);
            for (int layer = 0; layer < 3; layer++) {
                float flameH = height * (1 - layer * 0.22f);
                int color = layer == 2 ? 0xACFFCD74 : EffectPaint.argb(rgb, layer == 0 ? 68 : 125);
                for (int row = 0; row < flameH; row += 3) {
                    float taper = 1 - row / flameH;
                    float sway = (float) Math.sin(time * 0.8 + i * 2.1 + row / 19f) * breadth * (1 - taper) * 0.65f;
                    float half = breadth * taper * taper * (1 - layer * 0.21f);
                    int base = y + h - row;
                    g.fill((int) (center + sway - half), base - 3, (int) (center + sway + half) + 1, base, color);
                }
            }
        }
        for (int i = 0; i < 8 + power * 18; i++) {
            double phase = (i * 0.173 + time * 0.035) % 1;
            int px = x + 5 + (i * 37 % Math.max(1, w - 10));
            int py = y + h - (int) (phase * h * (0.4 + power * 0.6));
            g.fill(px, py, px + 1, py + 3, 0xAFFFAC63);
        }
    }

    private static void masks(GuiGraphicsExtractor g, int x, int y, int w, int h, float power, double time) {
        int count = 2 + (int) (power * 4);
        for (int i = 0; i < count; i++) {
            boolean left = i % 2 == 0;
            int px = x + (left ? w * 18 / 100 : w * 82 / 100);
            int py = y + 18 + (i / 2) * Math.max(16, (h - 38) / 3);
            py += (int) (Math.sin(time * 0.5 + i * 1.7) * 4);
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(px, py);
            pose.rotate((float) (Math.sin(time * 0.3 + i) * 0.15 + (left ? -0.15 : 0.15)));
            int s = Math.clamp(w / 20, 4, 8);
            g.fill(-s, -s, s, s / 2, 0xD9D4C5D8);
            g.fill(-s + 2, s / 2, s - 2, s + 2, 0xD9D4C5D8);
            g.fill(-s + 2, -2, -1, 0, 0xFF301D40);
            g.fill(2, -2, s - 1, 0, 0xFF301D40);
            EffectPaint.line(g, -3, s - 2, 0, s, 0xFF61446E, 1);
            EffectPaint.line(g, 0, s, 3, s - 2, 0xFF61446E, 1);
            pose.popMatrix();
            if (power > 0.5f) EffectPaint.line(g, px, y, px, py - s, 0x405F4C7A, 1);
        }
    }

    private static void stars(GuiGraphicsExtractor g, int x, int y, int w, int h, float power, double time, int rgb, boolean door) {
        for (int i = 0; i < 16 + power * 30; i++) {
            int px = x + 5 + i * 43 % Math.max(1, w - 10);
            int py = y + 7 + (i * 71 + (int) (time * 1.7)) % Math.max(1, h - 14);
            int color = i % 3 == 0 ? 0xB9DDDEFF : EffectPaint.argb(rgb, 150);
            if (i % 5 == 0) cross(g, px, py, power > 0.6f ? 3 : 2, color);
            else g.fill(px, py, px + 1, py + 1, color);
        }
        if (door) {
            int inset = w / 5;
            int top = y + h / 7;
            int color = EffectPaint.argb(rgb, 75 + (int) (power * 90));
            EffectPaint.line(g, x + inset, y + h - 12, x + inset, top, color, 1);
            EffectPaint.line(g, x + w - inset, y + h - 12, x + w - inset, top, color, 1);
            EffectPaint.line(g, x + inset, top, x + w / 2f, top - 12, color, 1);
            EffectPaint.line(g, x + w / 2f, top - 12, x + w - inset, top, color, 1);
        }
    }

    private static void constellations(GuiGraphicsExtractor g, int x, int y, int w, int h, float power, double time, int rgb) {
        int count = 4 + (int) (power * 5);
        for (int i = 0; i < count; i++) {
            int px = x + 10 + i * 37 % Math.max(1, w - 20);
            int py = y + 10 + i * 61 % Math.max(1, h - 20);
            diamond(g, px, py, 3, EffectPaint.argb(rgb, 145));
            if (i > 0) {
                int prevX = x + 10 + (i - 1) * 37 % Math.max(1, w - 20);
                int prevY = y + 10 + (i - 1) * 61 % Math.max(1, h - 20);
                EffectPaint.line(g, prevX, prevY, px, py, EffectPaint.argb(rgb, 40), 1);
            }
        }
    }

    private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int color) {
        EffectPaint.line(g, x, y - r, x + r, y, color, 1);
        EffectPaint.line(g, x + r, y, x, y + r, color, 1);
        EffectPaint.line(g, x, y + r, x - r, y, color, 1);
        EffectPaint.line(g, x - r, y, x, y - r, color, 1);
    }

    private static void cross(GuiGraphicsExtractor g, int x, int y, int r, int color) {
        g.fill(x - r, y, x + r + 1, y + 1, color);
        g.fill(x, y - r, x + 1, y + r + 1, color);
    }
}
