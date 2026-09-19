package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Twelve engraved, animated scenes. Coordinates are relative to the portrait aperture.
 */
public class PortraitArcana {
    private PortraitArcana() {
    }

    static void draw(GuiGraphicsExtractor g, String key, int x, int y, int w, int h,
                     float power, double time, int rgb) {
        Paint p = new Paint(g, x, y, w, h, Math.clamp(power, 0, 1), time, rgb);
        switch (key) {
            case "abyss" -> abyss(p);
            case "death" -> death(p);
            case "demoness" -> demoness(p);
            case "darkness" -> darkness(p);
            case "moon" -> moon(p);
            case "mother" -> mother(p);
            case "hanged" -> hanged(p);
            case "giant" -> giant(p);
            case "emperor" -> emperor(p);
            case "justiciar" -> justiciar(p);
            case "visionary" -> visionary(p);
            case "error" -> error(p);
            default -> {
            }
        }
    }

    private static void abyss(Paint p) {
        // Breathing cracks expose watching eyes inside the two walls of the chasm.
        for (int side : new int[]{-1, 1}) {
            double edge = .5 + side * .39;
            double lastX = edge, lastY = .04;
            for (int i = 1; i <= 15; i++) {
                double yy = .04 + i * .058;
                double xx = edge + Math.sin(i * 4.7 + p.t * .22) * (.012 + p.power * .035);
                p.line(lastX, lastY, xx, yy, 140);
                p.line(xx, yy, xx + side * (.025 + p.power * .06), yy - .045, 75);
                lastX = xx;
                lastY = yy;
            }
            int count = 2 + (int) (p.power * 3);
            for (int i = 0; i < count; i++) {
                double yy = .23 + i * .14;
                double breath = .6 + .4 * Math.sin(p.t * .4 + i);
                eye(p, edge - side * .04, yy, .035 + p.power * .012, .007 + breath * .01, 155);
            }
        }
    }

    private static void death(Paint p) {
        // Hooded shades ascend from a shallow river; their trailing hems dissolve.
        int count = 3 + (int) (p.power * 5);
        for (int i = 0; i < count; i++) {
            double phase = fract(i * .183 + p.t * .024);
            double xx = .08 + fract(i * .618) * .84 + Math.sin(p.t * .3 + i) * .018;
            double yy = 1.04 - phase * (.58 + p.power * .39);
            int alpha = (int) (Math.sin(phase * Math.PI) * 145);
            p.ellipse(xx, yy, .021, .027, alpha);
            p.line(xx - .025, yy + .016, xx - .04, yy + .115, alpha);
            p.line(xx + .025, yy + .016, xx + .04, yy + .115, alpha);
            for (int j = 0; j < 4; j++) {
                double fx = xx - .035 + j * .023;
                p.line(fx, yy + .10, fx + Math.sin(p.t + i + j) * .012, yy + .155, alpha / 2);
            }
            p.line(xx - .009, yy, xx + .009, yy, alpha);
        }
        for (int i = 0; i < 4; i++) p.wave(.02, .86 + i * .035, .98, .012, p.t * .25 + i, 50);
    }

    private static void demoness(Paint p) {
        // Floating mirror shards turn independently and grow a branching fracture network.
        for (int i = 0; i < 5 + (int) (p.power * 5); i++) {
            double xx = i % 2 == 0 ? .12 : .86;
            xx += Math.sin(p.t * .2 + i) * .035;
            double yy = .11 + ((double) i / 2) * .16 + Math.cos(p.t * .24 + i) * .018;
            double rx = (.028 + p.power * .026) * (.45 + Math.abs(Math.cos(p.t * .22 + i)) * .55);
            double ry = .055 + p.power * .021;
            p.line(xx - rx, yy - ry, xx + rx, yy - ry * .55, 160);
            p.line(xx + rx, yy - ry * .55, xx + rx * .5, yy + ry, 160);
            p.line(xx + rx * .5, yy + ry, xx - rx, yy - ry, 160);
            p.line(xx - rx, yy - ry, xx, yy, 100);
            p.line(xx, yy, xx + rx, yy - ry * .55, 95);
            p.line(xx, yy, xx + rx * .5, yy + ry, 100);
            if (p.power > .4) p.line(xx, yy, xx - rx * 1.6, yy + ry * .5, 70);
        }
    }

    private static void darkness(Paint p) {
        // The eclipse passes across a dim disc while long night veils drift below it.
        double cx = .5, cy = .18, radius = .07 + p.power * .045;
        p.ellipse(cx, cy, radius, radius * p.w / p.h, 140);
        double occultation = Math.sin(p.t * .12) * radius * .55;
        p.ellipse(cx + occultation, cy - .012, radius * .86, radius * .86 * p.w / p.h, 90);
        for (int i = 0; i < 7 + p.power * 7; i++) {
            double start = .035 + i * .93 / (7 + p.power * 7);
            double lastX = start, lastY = .26;
            for (int j = 1; j <= 14; j++) {
                double yy = .26 + j * .05;
                double xx = start + Math.sin(j * .3 + p.t * .18 + i) * (.025 + p.power * .035);
                p.line(lastX, lastY, xx, yy, 22 + (i % 3) * 9);
                lastX = xx;
                lastY = yy;
            }
        }
    }

    private static void moon(Paint p) {
        // An arc of lunar phases revolves around an offset full moon.
        int count = 3 + (int) (p.power * 5);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 1.06 + i * Math.PI * .88 / Math.max(1, count - 1) + Math.sin(p.t * .09) * .06;
            double xx = .5 + Math.cos(angle) * .36;
            double yy = .5 + Math.sin(angle) * .34;
            double r = .022 + p.power * .014;
            p.ellipse(xx, yy, r, r * p.w / p.h, 165);
            // A curved terminator changes slowly, without blinking or swapping textures.
            double phase = Math.cos(i * .8 + p.t * .065);
            for (int j = 0; j < 14; j++) {
                double a = -Math.PI / 2 + j * Math.PI / 14;
                double b = -Math.PI / 2 + (j + 1) * Math.PI / 14;
                p.line(xx + Math.cos(a) * r * phase, yy + Math.sin(a) * r * p.w / p.h,
                        xx + Math.cos(b) * r * phase, yy + Math.sin(b) * r * p.w / p.h, 150);
            }
        }
        for (int i = 0; i < 5; i++) p.wave(.04, .78 + i * .04, .96, .005 + p.power * .006, p.t * .2 + i, 45);
    }

    private static void mother(Paint p) {
        // Rooted stems sway, with opening petals and paired leaves rather than generic branches.
        for (int i = 0; i < 6 + (int) (p.power * 7); i++) {
            double xx = .04 + i * .92 / (5 + (int) (p.power * 7));
            double height = (.16 + p.power * .34) * (.7 + fract(i * .618) * .6);
            double sway = Math.sin(p.t * .35 + i * .8) * .016;
            double tipY = .98 - height, tipX = xx + sway;
            p.line(xx, .99, xx + sway * .5, .98 - height * .5, 115);
            p.line(xx + sway * .5, .98 - height * .5, tipX, tipY, 140);
            for (int side : new int[]{-1, 1}) {
                double yy = .98 - height * (side == 1 ? .42 : .64);
                p.line(xx, yy, xx + side * .035, yy - .027, 120);
                p.line(xx + side * .035, yy - .027, xx + side * .012, yy + .003, 95);
            }
            double bloom = .011 + p.power * .013 + Math.sin(p.t * .23 + i) * .003;
            for (int petal = 0; petal < 5; petal++) {
                double angle = petal * Math.PI * 2 / 5 + p.t * .018;
                p.ellipse(tipX + Math.cos(angle) * bloom, tipY + Math.sin(angle) * bloom * p.w / p.h,
                        bloom * .55, bloom * .55 * p.w / p.h, 145);
            }
        }
    }

    private static void hanged(Paint p) {
        // Suspended inverted crosses trail living roots downward from the upper frame.
        for (int i = 0; i < 3 + (int) (p.power * 3); i++) {
            double xx = .08 + i * .84 / (2 + (int) (p.power * 3));
            double yy = .18 + (i % 2) * .08;
            double sway = Math.sin(p.t * .25 + i) * .018;
            p.line(xx, .01, xx + sway, yy, 100);
            p.line(xx + sway, yy, xx + sway, yy + .15, 170);
            p.line(xx + sway - .035, yy + .105, xx + sway + .035, yy + .105, 170);
            for (int root = 0; root < 3; root++) {
                double lx = xx + sway, ly = yy + .15;
                for (int j = 1; j <= 7; j++) {
                    double ny = yy + .15 + j * (.026 + p.power * .019);
                    double nx = xx + sway + (root - 1) * j * .008 + Math.sin(j * .6 + p.t * .25 + root) * .012;
                    p.line(lx, ly, nx, ny, 100 - j * 8);
                    lx = nx;
                    ly = ny;
                }
            }
        }
    }

    private static void giant(Paint p) {
        // Monumental blades rise at the edges of a moving twilight horizon.
        for (int i = 0; i < 4 + (int) (p.power * 4); i++) {
            double xx = i % 2 == 0 ? .08 + i * .017 : .92 - i * .017;
            double yy = .86 + Math.sin(p.t * .22 + i) * .015;
            double length = .27 + p.power * .33 - ((double) i / 2) * .04;
            p.line(xx - .009, yy, xx - .009, yy - length + .04, 125);
            p.line(xx - .009, yy - length + .04, xx, yy - length, 180);
            p.line(xx, yy - length, xx + .009, yy - length + .04, 180);
            p.line(xx + .009, yy - length + .04, xx + .009, yy, 125);
            p.line(xx - .035, yy - .055, xx + .035, yy - .055, 175);
        }
        for (int i = 0; i < 7; i++) p.wave(0, .69 + i * .045, 1, .004, p.t * .14 + i, 38 + i * 3);
    }

    private static void emperor(Paint p) {
        // Crooked columns bend inward beneath a floating imperial crown.
        for (int side : new int[]{-1, 1}) {
            for (int rib = 0; rib < 3; rib++) {
                double lx = .5 + side * (.39 - rib * .018), ly = .98;
                for (int j = 1; j <= 18; j++) {
                    double yy = .98 - j * .041;
                    double xx = .5 + side * (.39 - rib * .018)
                            + Math.sin(j * .22 + p.t * .2 + side) * (.015 + p.power * .05);
                    p.line(lx, ly, xx, yy, 90 + rib * 18);
                    lx = xx;
                    ly = yy;
                }
            }
        }
        double yy = .16 + Math.sin(p.t * .25) * .012;
        double breadth = .12 + p.power * .055;
        p.line(.5 - breadth, yy + .065, .5 + breadth, yy + .065, 170);
        double[] heights = {0, .04, -.035, .04, 0};
        for (int i = 0; i < 4; i++)
            p.line(.5 - breadth + i * breadth / 2, yy + heights[i],
                    .5 - breadth + (i + 1) * breadth / 2, yy + heights[i + 1], 185);
        p.line(.5 - breadth, yy, .5 - breadth * .82, yy + .065, 150);
        p.line(.5 + breadth, yy, .5 + breadth * .82, yy + .065, 150);
    }

    private static void justiciar(Paint p) {
        // A working balance: the beam inclines slowly and the suspended pans stay level.
        double tilt = Math.sin(p.t * .23) * (.014 + p.power * .012);
        double breadth = .25 + p.power * .09;
        p.line(.5, .075, .5, .33, 160);
        p.line(.5 - breadth, .19 - tilt, .5 + breadth, .19 + tilt, 190);
        for (int side : new int[]{-1, 1}) {
            double xx = .5 + side * breadth, yy = .19 + side * tilt;
            p.line(xx, yy, xx - .055, yy + .15, 120);
            p.line(xx, yy, xx + .055, yy + .15, 120);
            p.line(xx - .065, yy + .15, xx + .065, yy + .15, 185);
            p.line(xx - .065, yy + .15, xx - .035, yy + .175, 150);
            p.line(xx - .035, yy + .175, xx + .035, yy + .175, 150);
            p.line(xx + .035, yy + .175, xx + .065, yy + .15, 150);
            for (int i = 0; i < 4 + p.power * 5; i++) {
                double sy = .46 + i * .05;
                p.line(xx - .025, sy, xx + .025, sy, 65);
            }
        }
        p.ellipse(.5, .075, .019, .019 * p.w / p.h, 180);
    }

    private static void visionary(Paint p) {
        // Dream clouds drift in opposed layers, watched by slowly opening eyes.
        for (int i = 0; i < 5; i++) {
            double center = fract(i * .27 + p.t * .008) * 1.3 - .15;
            double yy = .24 + i * .135;
            for (int j = 0; j < 4; j++)
                p.ellipse(center + j * .07, yy + Math.sin(i + j) * .012,
                        .075 + p.power * .025, .017, 25 + (int) (p.power * 20));
        }
        int count = 2 + (int) (p.power * 4);
        for (int i = 0; i < count; i++) {
            double xx = i % 2 == 0 ? .17 : .83;
            double yy = .19 + ((double) i / 2) * .23;
            double opening = .011 + (.5 + .5 * Math.sin(p.t * .19 + i)) * .009;
            eye(p, xx, yy, .05 + p.power * .015, opening, 150);
        }
    }

    private static void error(Paint p) {
        // Mismatched clock hands counter-rotate while segmented time worms orbit their cases.
        int count = 2 + (int) (p.power * 3);
        for (int i = 0; i < count; i++) {
            double xx = i % 2 == 0 ? .16 : .84, yy = .17 + ((double) i / 2) * .26;
            double r = .05 + p.power * .015, ry = r * p.w / p.h;
            p.ellipse(xx, yy, r, ry, 115);
            for (int tick = 0; tick < 8; tick++) {
                double a = tick * Math.PI / 4;
                p.line(xx + Math.cos(a) * r * .8, yy + Math.sin(a) * ry * .8,
                        xx + Math.cos(a) * r, yy + Math.sin(a) * ry, 150);
            }
            double a = p.t * .35 + i, b = -p.t * .19 + i * 2;
            p.line(xx, yy, xx + Math.cos(a) * r * .7, yy + Math.sin(a) * ry * .7, 190);
            p.line(xx, yy, xx + Math.cos(b) * r * .43, yy + Math.sin(b) * ry * .43, 190);
            for (int j = 0; j < 10; j++) {
                double angle = p.t * .13 + i + j * .16;
                double radius = r * (1.5 + Math.sin(angle * 3 + i) * .12);
                p.ellipse(xx + Math.cos(angle) * radius, yy + Math.sin(angle) * radius * p.w / p.h,
                        .005, .005 * p.w / p.h, 80 + j * 7);
            }
        }
    }

    private static void eye(Paint p, double x, double y, double rx, double ry, int alpha) {
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 12; i++) {
                double a = i / 12.0, b = (i + 1) / 12.0;
                p.line(x - rx + a * rx * 2, y + side * Math.sin(a * Math.PI) * ry,
                        x - rx + b * rx * 2, y + side * Math.sin(b * Math.PI) * ry, alpha);
            }
        }
        p.line(x, y - ry * .68, x, y + ry * .68, alpha + 35);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private record Paint(GuiGraphicsExtractor g, int x, int y, int w, int h, float power, double t, int rgb) {
        void line(double ax, double ay, double bx, double by, int alpha) {
            EffectPaint.line(g, x + (float) (ax * w), y + (float) (ay * h),
                    x + (float) (bx * w), y + (float) (by * h), EffectPaint.argb(rgb, Math.clamp(alpha, 0, 255)), 1);
        }

        void ellipse(double cx, double cy, double rx, double ry, int alpha) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12, b = (i + 1) * Math.PI / 12;
                line(cx + Math.cos(a) * rx, cy + Math.sin(a) * ry,
                        cx + Math.cos(b) * rx, cy + Math.sin(b) * ry, alpha);
            }
        }

        void wave(double left, double yy, double right, double amplitude, double phase, int alpha) {
            for (int i = 0; i < 24; i++) {
                double a = i / 24.0, b = (i + 1) / 24.0;
                line(left + (right - left) * a, yy + Math.sin(a * 5 + phase) * amplitude,
                        left + (right - left) * b, yy + Math.sin(b * 5 + phase) * amplitude, alpha);
            }
        }
    }
}
