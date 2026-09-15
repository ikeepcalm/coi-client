package dev.ua.ikeepcalm.coi.client.screen.title;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Random;

/**
 * The orbit: two rings and the 24 pathway emblems riding the outer one.
 * <p>
 * The <em>ring</em> turns; the emblems do not. Rotating each emblem with its
 * own angle is the obvious reading of "a turning wheel" and it is the wrong
 * one — half the wheel would be upside down and none of it legible, and these
 * are symbols the player is meant to recognise, not decoration.
 * <p>
 * The emblems are blitted from {@link Pathways#emblemTexture} rather than drawn
 * through {@code CoiIcons.drawPathwayEmblem}: that path renders a 9px bitmap
 * font glyph, which at 32px is four times its own size and reads as mush.
 * <p>
 * Their position lives in the <em>pose matrix</em>, not in the blit's
 * coordinates, which are {@code int} on every overload. At one revolution per
 * four minutes an emblem crosses roughly a tenth of a gui pixel per frame, so
 * rounding to the blit's grid means sitting still for ten frames and then
 * jumping a whole pixel — a stutter, from the same motion the rings render
 * smoothly because {@link EffectPaint#line} takes floats. The fractional part
 * of the position therefore has to be carried by the matrix, and the cursor's
 * push rides that same fractional path for the same reason.
 */
public class PathwayWheel {

    /**
     * One revolution per four minutes — movement you notice only if you wait.
     */
    private static final long PERIOD_MS = 240_000L;

    /**
     * Emblem art is 64x64; 32 is the whole-pixel 2:1 reduction the mod draws icons at.
     */
    private static final int EMBLEM_SRC = 64;
    private static final int EMBLEM = 32;
    private static final int EMBLEM_LIT = 40;

    /**
     * What the cursor's proximity adds to an emblem's drawn size at its closest.
     */
    private static final int EMBLEM_GROWTH = 4;

    /**
     * An unlit emblem at rest: present, but not asking to be read.
     */
    private static final int REST_ALPHA = 77;

    private static final float INNER_RING = 0.86f;
    private static final int INNER_DASHES = 42;
    private static final float TAU = (float) (Math.PI * 2);

    /**
     * One lap of the crest — thirteen times the wheel's own, and travelling
     * against it, the same sense the dashed inner ring counter-rotates in. The
     * inner ring reads as the mechanism; a light running the other way reads as
     * driven by it rather than as a third thing turning on its own account.
     */
    private static final long CREST_PERIOD_MS = 18_000L;

    /**
     * How far the crest reaches, in emblem slots. Just over one, so a single
     * emblem is at full strength and its two neighbours are only warmed.
     */
    private static final float CREST_SPREAD = 1.2f;

    /**
     * Below this the crest's halo is not worth the rings it costs.
     */
    private static final float HALO_THRESHOLD = 0.35f;

    /**
     * How near the cursor has to be to move an emblem at all, and how far it moves it.
     */
    private static final float REPEL_RADIUS = 64f;
    private static final float REPEL_MAX = 16f;

    /**
     * Long enough that a sweep across the wheel drags the ring rather than snapping it.
     */
    private static final float REPEL_EASE_MS = 120f;

    /**
     * Corruption has to clear this before the ring starts cracking.
     */
    private static final float CRACK_THRESHOLD = 0.6f;

    private static final long CORRUPTION_SEED = 0x5E9_1CE_7L;

    /**
     * How often the ring slips, and for how long.
     */
    private static final long JUDDER_CYCLE_MS = 2600L;
    private static final long JUDDER_MS = 200L;

    /**
     * The eased displacement each emblem currently carries, indexed by its slot
     * on the ring. Static because the wheel has exactly one instance on screen
     * and {@code TitleScene}'s mote field already establishes the shape here;
     * <em>render thread only</em> — nothing outside a draw call reads or writes
     * either array.
     */
    private static final float[] PUSH_X = new float[Math.max(1, Pathways.RING.size())];
    private static final float[] PUSH_Y = new float[Math.max(1, Pathways.RING.size())];

    /**
     * An emblem the corruption gnaws at: which one, and the slow cycle it does
     * it on. Chosen once rather than per frame so the same three emblems keep
     * failing instead of the whole wheel shimmering.
     *
     * @param index  the slot on the ring
     * @param period the cycle it forgets itself over
     * @param offset where in that cycle it starts, so they never agree
     */
    private record Flicker(int index, long period, long offset) {
    }

    /**
     * Three candidates, of which corruption uses the first one, two or three.
     * Building them in order means raising corruption <em>adds</em> a failing
     * emblem rather than reshuffling which ones fail.
     */
    private static final Flicker[] FLICKERS = flickers();

    private PathwayWheel() {
    }

    private static Flicker[] flickers() {
        Random random = new Random(CORRUPTION_SEED);
        int size = Math.max(1, Pathways.RING.size());
        Flicker[] out = new Flicker[3];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Flicker(random.nextInt(size), 3400L + random.nextInt(5200), random.nextInt(4000));
        }
        return out;
    }

    static void draw(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                     int accent, float corruption, int mouseX, int mouseY) {
        List<String> ring = Pathways.RING;
        if (ring.isEmpty()) return;

        float spin = spin(corruption);
        int inner = Math.round(geo.radius() * INNER_RING);

        strokeCircle(graphics, geo.cx(), geo.cy(), geo.radius(), MenuTheme.withAlpha(accent, 0.22f));
        dashedCircle(graphics, geo.cx(), geo.cy(), inner, -spin, MenuTheme.withAlpha(accent, 0.16f));
        if (corruption > CRACK_THRESHOLD) {
            cracks(graphics, geo, accent, corruption);
        }

        String lit = Pathways.normalizePathway(TitleTakeover.litPathway());
        float crest = crest(ring.size());
        for (int i = 0; i < ring.size(); i++) {
            String pathway = ring.get(i);
            // -PI/2 puts the first emblem at the top of the circle, where a
            // wheel is read from.
            double angle = spin + i * TAU / ring.size() - Math.PI / 2;
            float x = (float) (geo.cx() + Math.cos(angle) * geo.radius());
            float y = (float) (geo.cy() + Math.sin(angle) * geo.radius());

            float near = repel(i, x, y, mouseX, mouseY);
            float px = x + PUSH_X[i];
            float py = y + PUSH_Y[i];
            int grown = Math.round(near * EMBLEM_GROWTH);

            if (!lit.isEmpty() && lit.equals(pathway)) {
                // The crest passes over the player's own emblem without touching
                // it: that one is already at full strength with a halo of its
                // own, so a travelling light has nothing to add and a permanent
                // mark that blinks reads as a fault rather than as life. The
                // cursor still moves it, and the halo and the inward tick are
                // both drawn from the displaced centre so they ride along.
                drawLit(graphics, geo, pathway, px, py, angle, EMBLEM_LIT + grown, inner, accent);
                continue;
            }

            float shine = shine(i, ring.size(), crest);
            int rgb = flickerRgb(i, pathway, corruption);
            crestHalo(graphics, px, py, EMBLEM + grown, rgb, shine);
            // One proximity value, two effects: the emblem the cursor pushes is
            // the emblem it brightens. Deciding the brightness by hit-testing
            // where the emblem is *now* would dim it as it slides out from under
            // the cursor it is fleeing, which reads as a bug rather than a rule.
            float glow = Math.max(near, shine);
            drawEmblem(graphics, pathway, px, py, EMBLEM + grown,
                    EffectPaint.argb(rgb, Math.round(REST_ALPHA + (255 - REST_ALPHA) * glow)));
        }
    }

    /**
     * Where the travelling shine sits, as a fractional slot on the ring. A pure
     * function of the clock and the ring's size — deliberately not "pick an
     * emblem every N seconds", which needs state, pops when it changes target,
     * and has to decide what happens when the ring's size changes under it.
     */
    private static float crest(int size) {
        float phase = (TitleTakeover.now() % CREST_PERIOD_MS) / (float) CREST_PERIOD_MS;
        return (1f - phase) * size;
    }

    /**
     * How lit the crest has this slot, 0..1, measured the shorter way round the ring.
     */
    private static float shine(int index, int size, float crest) {
        float gap = Math.abs(index - crest);
        gap = Math.min(gap, size - gap);
        return smoothstep(1f - gap / CREST_SPREAD);
    }

    /**
     * How hard the cursor pushes the emblem whose undisturbed centre is
     * {@code (x, y)} — and, as the return, how near it is, 0..1.
     * <p>
     * The distance is measured from that undisturbed centre, never from where
     * the emblem has already been pushed to. A displacement fed back into its
     * own input oscillates: a pushed emblem is further from the cursor, so it is
     * pushed less, so it comes back; and one the cursor follows runs away for as
     * long as it is followed. Reading the resting position keeps the target a
     * pure function of the cursor, and the easing the only memory involved.
     */
    private static float repel(int index, float x, float y, int mouseX, int mouseY) {
        float dx = x - mouseX;
        float dy = y - mouseY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float near = smoothstep(1f - distance / REPEL_RADIUS);
        // Normalising against at least one pixel is also the answer to the cursor
        // sitting exactly on a centre, where "away from it" names no direction:
        // the push fades out over that last pixel instead of dividing by zero.
        float reach = near * REPEL_MAX / Math.max(distance, 1f);
        PUSH_X[index] = TitleTakeover.approach(PUSH_X[index], dx * reach, REPEL_EASE_MS);
        PUSH_Y[index] = TitleTakeover.approach(PUSH_Y[index], dy * reach, REPEL_EASE_MS);
        return near;
    }

    /**
     * Hermite ease over 0..1, clamped. Both falloffs use it because a linear
     * ramp leaves a visible corner exactly where it reaches zero — an emblem
     * would stop moving, and stop brightening, with a crease.
     */
    private static float smoothstep(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t * t * (3f - 2f * t);
    }

    /**
     * The wheel's angle. Corruption adds a judder: a short window every few
     * seconds where the ring slips, rather than a permanent wobble — something
     * intermittent is what reads as wrong, and a constant tremor would only
     * read as a bad easing curve.
     */
    private static float spin(float corruption) {
        long now = TitleTakeover.now();
        float base = (now % PERIOD_MS) / (float) PERIOD_MS * TAU;
        if (corruption <= 0f || TitleTakeover.steady()) return base;

        long window = now % JUDDER_CYCLE_MS;
        if (window > JUDDER_MS) return base;
        float through = window / (float) JUDDER_MS;
        return base + (float) Math.sin(through * Math.PI * 3) * 0.05f * corruption;
    }

    /**
     * An unlit emblem's colour: its own, unless corruption has picked this one
     * to forget, in which case it drifts toward {@code error}'s grey and back.
     */
    private static int flickerRgb(int index, String pathway, float corruption) {
        int own = Pathways.pathwayRgb(pathway);
        if (corruption <= 0f || TitleTakeover.steady()) return own;

        int count = 1 + Math.round(corruption * 2f);
        for (int i = 0; i < Math.min(count, FLICKERS.length); i++) {
            Flicker flicker = FLICKERS[i];
            if (flicker.index() != index) continue;
            float phase = ((TitleTakeover.now() + flicker.offset()) % flicker.period())
                    / (float) flicker.period();
            float bite = Math.max(0f, (float) Math.sin(phase * TAU)) * corruption;
            return MenuTheme.lerpArgb(0xFF000000 | own,
                    0xFF000000 | Pathways.pathwayRgb("error"), bite) & 0xFFFFFF;
        }
        return own;
    }

    /**
     * The crest at its peak: two faint rings, and only there. The cursor does
     * not raise it — proximity already says which emblem the pointer is over by
     * moving it, and a halo drawn under the pointer is a halo behind the hand.
     */
    private static void crestHalo(GuiGraphicsExtractor graphics, float x, float y,
                                  int size, int rgb, float shine) {
        if (shine <= HALO_THRESHOLD) return;
        float bite = (shine - HALO_THRESHOLD) / (1f - HALO_THRESHOLD);
        strokeCircle(graphics, x, y, size / 2 + 4, EffectPaint.argb(rgb, Math.round(34 * bite)));
        strokeCircle(graphics, x, y, size / 2 + 9, EffectPaint.argb(rgb, Math.round(15 * bite)));
    }

    /**
     * The player's own pathway: larger, at full strength, with a halo behind it
     * and a tick joining it inward to the dashed ring — so the eye finds it
     * without the other 23 having to be dimmer than they already are.
     */
    private static void drawLit(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                                String pathway, float x, float y, double angle,
                                int size, int inner, int accent) {
        int rgb = Pathways.pathwayRgb(pathway);
        for (int step = 4; step >= 1; step--) {
            strokeCircle(graphics, x, y, size / 2 + step * 4,
                    EffectPaint.argb(rgb, 10 + (5 - step) * 6));
        }
        EffectPaint.line(graphics,
                (float) (x - Math.cos(angle) * (size / 2f + 3)),
                (float) (y - Math.sin(angle) * (size / 2f + 3)),
                (float) (geo.cx() + Math.cos(angle) * (inner + 2)),
                (float) (geo.cy() + Math.sin(angle) * (inner + 2)),
                MenuTheme.withAlpha(accent, 0.55f), 1);
        drawEmblem(graphics, pathway, x, y, size, EffectPaint.argb(rgb, 255));
    }

    /**
     * Blitted at the origin, with the real, fractional position in the pose.
     */
    private static void drawEmblem(GuiGraphicsExtractor graphics, String pathway,
                                   float cx, float cy, int size, int argb) {
        Identifier texture = Pathways.emblemTexture(pathway);
        if (texture == null) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx - size / 2f, cy - size / 2f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0,
                0f, 0f, size, size, EMBLEM_SRC, EMBLEM_SRC, EMBLEM_SRC, EMBLEM_SRC, argb);
        pose.popMatrix();
    }

    /**
     * A circle stroked as a closed polyline. There is no circle primitive, and
     * a ring of 1px dots costs several hundred fills at the menu's radius for a
     * dotted result. The segment count follows the radius, so the emblem haloes
     * do not pay the big ring's price.
     * <p>
     * The centre is a {@code float} so an emblem's halo can sit on the same
     * fractional point the emblem does; rounding it would leave the rings
     * crawling against the symbol they belong to.
     */
    private static void strokeCircle(GuiGraphicsExtractor graphics, float cx, float cy, int radius, int color) {
        if (radius < 2) return;
        int segments = Math.clamp(radius, 24, 96);
        float previousX = cx + radius;
        float previousY = cy;
        for (int i = 1; i <= segments; i++) {
            double angle = i * TAU / segments;
            float x = (float) (cx + Math.cos(angle) * radius);
            float y = (float) (cy + Math.sin(angle) * radius);
            EffectPaint.line(graphics, previousX, previousY, x, y, color, 1);
            previousX = x;
            previousY = y;
        }
    }

    /**
     * The inner ring, broken into dashes and counter-rotating against the outer one.
     */
    private static void dashedCircle(GuiGraphicsExtractor graphics, int cx, int cy,
                                     int radius, float rotation, int color) {
        if (radius < 2) return;
        float step = TAU / INNER_DASHES;
        for (int i = 0; i < INNER_DASHES; i++) {
            double from = rotation + i * step;
            double to = from + step * 0.45;
            EffectPaint.line(graphics,
                    (float) (cx + Math.cos(from) * radius), (float) (cy + Math.sin(from) * radius),
                    (float) (cx + Math.cos(to) * radius), (float) (cy + Math.sin(to) * radius),
                    color, 1);
        }
    }

    /**
     * Hairlines struck across the ring, as chords rather than radii — a crack
     * goes through a thing, it does not radiate from its middle.
     */
    private static void cracks(GuiGraphicsExtractor graphics, TitleTakeover.Geometry geo,
                               int accent, float corruption) {
        float bite = (corruption - CRACK_THRESHOLD) / (1f - CRACK_THRESHOLD);
        int count = 1 + Math.round(bite * 3f);
        Random random = new Random(CORRUPTION_SEED * 31L);
        int color = MenuTheme.withAlpha(MenuTheme.shade(accent, -0.35f), 0.25f + 0.35f * bite);
        for (int i = 0; i < count; i++) {
            double from = random.nextDouble() * TAU;
            double to = from + 0.8 + random.nextDouble() * 1.6;
            EffectPaint.line(graphics,
                    (float) (geo.cx() + Math.cos(from) * geo.radius()),
                    (float) (geo.cy() + Math.sin(from) * geo.radius()),
                    (float) (geo.cx() + Math.cos(to) * geo.radius()),
                    (float) (geo.cy() + Math.sin(to) * geo.radius()),
                    color, 1);
        }
    }
}
