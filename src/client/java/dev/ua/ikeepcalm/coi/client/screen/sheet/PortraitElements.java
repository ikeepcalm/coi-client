package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Individually choreographed elemental and scholarly portrait scenes.
 */
public class PortraitElements {
    private PortraitElements() {
    }

    static void draw(GuiGraphicsExtractor g, String key, int x, int y, int w, int h,
                     float p, double t, int rgb) {
        int light = EffectPaint.argb(rgb, 190);
        int dim = EffectPaint.argb(rgb, 80);
        int cx = x + w / 2, cy = y + h / 2;
        switch (key) {
            case "sun" -> {
                for (int i = 0; i < 3 + (int) (p * 4); i++) {
                    double a = t * .12 + i * Math.PI * 2 / (3 + (int) (p * 4));
                    int sx = cx + (int) (Math.cos(a) * w * .37), sy = cy + (int) (Math.sin(a) * h * .36);
                    int r = Math.max(3, (int) (w * (.018 + p * .018)));
                    disc(g, sx, sy, r, 0xFFFFECAF);
                    for (int j = 0; j < 8; j++) {
                        double b = j * Math.PI / 4 - t * .22;
                        line(g, sx + Math.cos(b) * (r + 2), sy + Math.sin(b) * (r + 2),
                                sx + Math.cos(b) * (r + 5 + p * 4), sy + Math.sin(b) * (r + 5 + p * 4), light);
                    }
                }
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8 + t * .025;
                    line(g, cx + Math.cos(a) * w * .25, cy + Math.sin(a) * h * .25,
                            cx + Math.cos(a) * w * .65, cy + Math.sin(a) * h * .65, dim);
                }
            }
            case "hermit" -> {
                for (int i = 0; i < 3 + (int) (p * 3); i++) {
                    int sx = x + (i % 2 == 0 ? w / 6 : w * 5 / 6), sy = y + h / 6 + (i / 2) * h / 3;
                    sy += (int) (Math.sin(t * .45 + i) * 5);
                    var pose = g.pose();
                    pose.pushMatrix();
                    pose.translate(sx, sy);
                    pose.rotate((float) (Math.sin(t * .2 + i) * .12));
                    int rw = Math.max(6, w / 17), rh = Math.max(10, h / 15);
                    g.fill(-rw, -rh, rw, rh, 0xDFCDBA91);
                    g.fill(-rw - 2, -rh - 2, rw + 2, -rh + 1, 0xFFECDDB8);
                    g.fill(-rw - 2, rh - 1, rw + 2, rh + 2, 0xFFA68C64);
                    for (int row = -rh + 5; row < rh - 2; row += 4)
                        g.fill(-rw + 3, row, rw - 2 - (row % 3), row + 1, 0x907C558F);
                    pose.popMatrix();
                }
            }
            case "tyrant" -> {
                for (int i = 0; i < 12 + (int) (p * 20); i++) {
                    double phase = (i * .137 + t * (.16 + p * .07)) % 1;
                    int dx = x + 5 + i * 37 % Math.max(1, w - 10), dy = y + (int) (phase * (h - 12));
                    line(g, dx, dy, dx - 1, dy + 4 + p * 6, 0x9879CFFF);
                    if (i % 4 == 0) disc(g, dx, dy + 5, 1, 0xCDB9E8FF);
                }
                for (int row = 0; row < 5; row++) {
                    int yy = y + h - 6 - row * 6;
                    for (int xx = 0; xx < w - 5; xx += 5) {
                        double a = Math.sin(xx * .06 + t * .8 + row) * (2 + p * 3);
                        double b = Math.sin((xx + 5) * .06 + t * .8 + row) * (2 + p * 3);
                        line(g, x + xx, yy + a, x + xx + 5, yy + b, EffectPaint.argb(0x4CAEDF, 130 - row * 18));
                    }
                }
                for (int side = -1; side <= 1; side += 2) {
                    double sx = cx + side * w * .23;
                    for (int i = 0; i < 12; i++) {
                        double yy = y + h * .38 + i * h * .045;
                        line(g, sx + Math.sin(t + i * .4) * 3, yy, sx + Math.sin(t + (i + 1) * .4) * 3, yy + h * .045, dim);
                    }
                }
            }
            case "chained" -> {
                for (int side = -1; side <= 1; side += 2) {
                    double dx = cx + side * w * .42;
                    chain(g, dx, y + 3, dx + Math.sin(t * .3) * 3, y + h - 5, light);
                }
            }
            case "paragon" -> {
                for (int i = 0; i < 4; i++) {
                    int sx = x + (i % 2 == 0 ? w / 6 : w * 5 / 6), sy = y + h / 4 + (i / 2) * h / 2;
                    gear(g, sx, sy, Math.max(8, w / 12), t * .18 * (i % 2 == 0 ? 1 : -1), light);
                    if (p > .3) {
                        line(g, sx, sy, sx, cy, dim);
                        line(g, sx, cy, cx, cy, dim);
                        double pulse = (t * .2 + i * .23) % 1;
                        disc(g, sx, (int) (sy + (cy - sy) * pulse), 2, 0xFFF0E6BF);
                    }
                }
            }
            case "tower" -> {
                int bottom = y + h - 8;
                for (int side = -1; side <= 1; side += 2) {
                    int sx = cx + side * w / 3, rw = Math.max(5, w / 15);
                    int top = y + (int) (h * (.42 - p * .29));
                    g.fill(sx - rw, top, sx + rw, bottom, 0x395E7098);
                    for (int yy = top; yy < bottom; yy += 12) {
                        line(g, sx - rw, yy, sx + rw, yy, light);
                        int offset = (yy / 12 % 2) * rw;
                        line(g, sx - rw + offset, yy, sx - rw + offset, Math.min(bottom, yy + 12), dim);
                    }
                    double rise = (t * .12 + (side + 1) * .17) % 1;
                    int by = bottom - (int) (rise * (bottom - top));
                    g.outline(sx - rw - 2, by, 2 * rw + 4, 7, 0xAFD9E9FF);
                    line(g, sx, top - 10, sx - rw, top, light);
                    line(g, sx, top - 10, sx + rw, top, light);
                }
            }
            case "fortune" -> {
                for (int i = 0; i < 4 + (int) (p * 3); i++) {
                    double a = t * .15 + i * Math.PI * 2 / (4 + (int) (p * 3));
                    int sx = cx + (int) (Math.cos(a) * w * .36), sy = cy + (int) (Math.sin(a) * h * .34);
                    var pose = g.pose();
                    pose.pushMatrix();
                    pose.translate(sx, sy);
                    pose.rotate((float) (a * .5));
                    int r = Math.max(4, w / 32);
                    g.fill(-r, -r, r + 1, r + 1, 0xFFE2D5AF);
                    g.outline(-r, -r, 2 * r + 1, 2 * r + 1, 0xFF9D7937);
                    for (int dot = 0; dot < i % 6 + 1; dot++)
                        g.fill(-r + 2 + (dot % 2) * (r), -r + 2 + (dot / 2) * 3,
                                -r + 3 + (dot % 2) * r, -r + 3 + (dot / 2) * 3, 0xFF57452B);
                    pose.popMatrix();
                }
            }
            case "aeon" -> {
                for (int side = -1; side <= 1; side += 2) {
                    int sx = cx + side * w / 3, sy = cy, r = Math.max(8, w / 12);
                    line(g, sx - r, sy - r * 2, sx + r, sy - r * 2, light);
                    line(g, sx - r, sy + r * 2, sx + r, sy + r * 2, light);
                    line(g, sx - r, sy - r * 2, sx + r, sy + r * 2, dim);
                    line(g, sx + r, sy - r * 2, sx - r, sy + r * 2, dim);
                    for (int i = 0; i < 8 + (int) (p * 8); i++) {
                        double phase = (i * .13 + t * .16) % 1;
                        double yy = sy - r * 1.8 + phase * r * 3.6;
                        int span = (int) (Math.abs(phase - .5) * r * 1.6);
                        int xx = sx + (i % 2 == 0 ? 1 : -1) * (i * 7 % Math.max(1, span + 1));
                        g.fill(xx, (int) yy, xx + 1, (int) yy + 2, 0xDFFFE1A1);
                    }
                }
            }
            case "patriarch" -> {
                for (int i = 0; i < 3 + (int) (p * 3); i++) {
                    double a = t * .18 + i * 2.4;
                    int sx = cx + (int) (Math.cos(a) * w * .37), sy = y + h / 5 + (int) ((Math.sin(a * .7) * .5 + .5) * h * .55);
                    int span = Math.max(5, w / 22);
                    double flap = Math.sin(t * 1.5 + i) * span * .7;
                    line(g, sx - span, sy + flap, sx, sy, light);
                    line(g, sx, sy, sx + span, sy + flap, light);
                    line(g, sx, sy, sx, sy + 4, dim);
                }
                for (int i = 0; i < 4; i++) {
                    int yy = y + h / 5 + i * h / 5;
                    line(g, x + 8 + Math.sin(t * .4 + i) * 6, yy, x + w / 4, yy - 4, dim);
                }
            }
            case "sublunary" -> {
                for (int i = 0; i < 3; i++) {
                    double angle = t * .08 * (i % 2 == 0 ? 1 : -1) + i;
                    ellipse(g, cx, cy, w * (.27 + i * .055), h * (.12 + i * .035), angle, dim);
                    double a = t * .22 + i * 2.1;
                    double px = Math.cos(a) * w * (.27 + i * .055), py = Math.sin(a) * h * (.12 + i * .035);
                    int sx = cx + (int) (px * Math.cos(angle) - py * Math.sin(angle));
                    int sy = cy + (int) (px * Math.sin(angle) + py * Math.cos(angle));
                    disc(g, sx, sy, 3 + (int) (p * 2), light);
                }
            }
            default -> {
            }
        }
    }

    static void chain(GuiGraphicsExtractor g, double x, double y, double ex, double ey, int color) {
        int links = Math.clamp((int) (Math.hypot(ex - x, ey - y) / 7), 1, 70);
        for (int i = 0; i < links; i++) {
            double f = (i + .5) / links, cx = x + (ex - x) * f, cy = y + (ey - y) * f;
            double angle = Math.atan2(ey - y, ex - x);
            ellipse(g, cx, cy, 4, i % 2 == 0 ? 2 : 1, angle, color);
        }
    }

    private static void gear(GuiGraphicsExtractor g, int x, int y, int r, double t, int color) {
        ellipse(g, x, y, r * .7, r * .7, 0, color);
        ellipse(g, x, y, r * .25, r * .25, 0, color);
        for (int i = 0; i < 10; i++) {
            double a = t + i * Math.PI / 5;
            line(g, x + Math.cos(a) * r * .65, y + Math.sin(a) * r * .65, x + Math.cos(a) * r, y + Math.sin(a) * r, color);
        }
    }

    private static void ellipse(GuiGraphicsExtractor g, double x, double y, double rx, double ry, double angle, int color) {
        int count = 24;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count, b = (i + 1) * Math.PI * 2 / count;
            double ax = Math.cos(a) * rx, ay = Math.sin(a) * ry, bx = Math.cos(b) * rx, by = Math.sin(b) * ry;
            line(g, x + ax * Math.cos(angle) - ay * Math.sin(angle), y + ax * Math.sin(angle) + ay * Math.cos(angle),
                    x + bx * Math.cos(angle) - by * Math.sin(angle), y + bx * Math.sin(angle) + by * Math.cos(angle), color);
        }
    }

    private static void disc(GuiGraphicsExtractor g, int x, int y, int r, int color) {
        for (int row = -r; row <= r; row++) {
            int half = (int) Math.sqrt(r * r - row * row);
            g.fill(x - half, y + row, x + half + 1, y + row + 1, color);
        }
    }

    private static void line(GuiGraphicsExtractor g, double x, double y, double ex, double ey, int color) {
        EffectPaint.line(g, (float) x, (float) y, (float) ex, (float) ey, color, 1);
    }
}
