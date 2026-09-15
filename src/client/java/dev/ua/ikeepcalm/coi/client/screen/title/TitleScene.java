package dev.ua.ikeepcalm.coi.client.screen.title;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The void the wheel turns in: a vertical gradient, a gold bloom behind the
 * centre, and a field of drifting dust.
 * <p>
 * The motes are generated once from a fixed seed, the same trick
 * {@code CracksEffect} uses for its fracture pattern — except here the point is
 * the opposite one. A crack pattern is seeded by its trigger time so every
 * shatter differs; the menu's dust is seeded by a constant so the menu looks
 * like itself on every launch, and a player who notices a mote is not being
 * shown a different sky each time they quit to title.
 */
public class TitleScene {

    private static final int SKY_TOP = 0xFF07060B;
    private static final int SKY_MID = 0xFF120E1A;
    private static final int SKY_BOTTOM = 0xFF05040A;

    private static final int MOTE_COUNT = 140;
    private static final long MOTE_SEED = 0xC01_0F_D05_7L;

    /**
     * Scanline height of the bloom. The falloff is soft enough that 3px bands
     * do not read as bands, and it is a third of the fills 1px would cost.
     */
    private static final int BLOOM_STEP = 3;

    /** Indices into a mote's {@code {x, y, speed, sway, swayPhase, period, twinklePhase}}. */
    private static final int X = 0;
    private static final int Y = 1;
    private static final int SPEED = 2;
    private static final int SWAY = 3;
    private static final int SWAY_PHASE = 4;
    private static final int PERIOD = 5;
    private static final int TWINKLE_PHASE = 6;

    private static final float[][] MOTES = new float[MOTE_COUNT][];

    static {
        Random random = new Random(MOTE_SEED);
        for (int i = 0; i < MOTE_COUNT; i++) {
            MOTES[i] = new float[]{
                    random.nextFloat(),
                    random.nextFloat(),
                    0.004f + random.nextFloat() * 0.010f,
                    3f + random.nextFloat() * 11f,
                    random.nextFloat() * (float) (Math.PI * 2),
                    1700f + random.nextFloat() * 4200f,
                    random.nextFloat()
            };
        }
    }

    private TitleScene() {
    }

    static void draw(GuiGraphicsExtractor graphics, int w, int h,
                     TitleTakeover.Geometry geo, int accent, float corruption) {
        sky(graphics, w, h, geo.cy());
        bloom(graphics, geo, accent, corruption);
        motes(graphics, w, h, accent);
    }

    /**
     * Two gradients rather than a stack of bands: the horizon the eye reads is
     * the one at the ring's centre, so the split goes there and each half is a
     * single fill.
     */
    private static void sky(GuiGraphicsExtractor graphics, int w, int h, int cy) {
        int split = Math.clamp(cy, 1, h - 1);
        graphics.fillGradient(0, 0, w, split, SKY_TOP, SKY_MID);
        graphics.fillGradient(0, split, w, h, SKY_MID, SKY_BOTTOM);
    }

    /**
     * A radial glow built from horizontal gradients, one pair per scanline: the
     * alpha at the centre of a row falls off with the row's distance from the
     * centre, and the row itself fades to nothing at the circle's own edge.
     * Two fills a row is what makes a round glow affordable — there is no
     * radial primitive, and stroking rings or filling discs costs an order of
     * magnitude more quads for a softer result.
     */
    private static void bloom(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                              int accent, float corruption) {
        int clear = accent & 0xFFFFFF;
        int reach = Math.round(geo.radius() * 1.1f);
        // Corruption smothers the light rather than reddening it twice over —
        // the accent has already turned by the time this is read.
        int peak = Math.round(62 * (1f - 0.35f * corruption));

        for (int dy = -reach; dy <= reach; dy += BLOOM_STEP) {
            float t = dy / (float) reach;
            float chord = 1f - t * t;
            if (chord <= 0f) continue;
            int half = Math.round(reach * (float) Math.sqrt(chord));
            if (half < 2) continue;
            float falloff = (1f - Math.abs(t));
            int alpha = Math.round(peak * falloff * falloff);
            if (alpha <= 0) continue;
            int core = EffectPaint.argb(clear, alpha);
            int y = geo.cy() + dy;
            EffectPaint.hGradient(graphics, geo.cx() - half, y, geo.cx(), y + BLOOM_STEP, clear, core);
            EffectPaint.hGradient(graphics, geo.cx(), y, geo.cx() + half, y + BLOOM_STEP, core, clear);
        }
    }

    /**
     * Dust rising through the void. Positions wrap vertically, so the field
     * never empties and never has to be respawned.
     * <p>
     * {@code epilepsyMode} freezes the twinkle at its mid brightness rather
     * than removing the motes: the drift is a slow pan, the twinkle is the part
     * that flickers.
     */
    private static void motes(GuiGraphicsExtractor graphics, int w, int h, int accent) {
        float seconds = TitleTakeover.now() / 1000f;
        boolean steady = TitleTakeover.steady();
        int rgb = MenuTheme.shade(accent, 0.45f) & 0xFFFFFF;

        for (float[] mote : MOTES) {
            float rise = (mote[Y] - seconds * mote[SPEED]) % 1f;
            if (rise < 0) rise += 1f;
            int y = Math.round(rise * h);
            int x = Math.round(mote[X] * w
                    + (float) Math.sin(seconds * 0.6f + mote[SWAY_PHASE]) * mote[SWAY]);
            if (x < 0 || x >= w) continue;

            float twinkle = steady ? 0.5f
                    : 0.5f + 0.5f * (float) Math.sin(Math.PI * 2
                    * (seconds * 1000f / mote[PERIOD] + mote[TWINKLE_PHASE]));
            int alpha = Math.round(26 + 90 * twinkle);
            graphics.fill(x, y, x + 1, y + 1, EffectPaint.argb(rgb, alpha));
        }
    }
}
