package dev.ua.ikeepcalm.coi.client.hud.render;

import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Every pixel of the Beyonder health element, one method per
 * {@link HealthStyle}. Stateless, like the rest of {@code hud.render}: the
 * live overlay and the layout editor's preview both call the same methods with
 * explicit values, so the two can never draw different things.
 * <p>
 * <b>The chrome is drawn, never blitted.</b> The mod's shipped artwork is
 * detailed 64px source reduced by whole pixels, and 16px is its floor — below
 * that the detail averages into a blur. {@link HealthStyle#ORNATE}'s frame is
 * one pixel thick and lives under {@code beyonderHealthScale}, which runs from
 * 0.5 to 2.0, so a PNG would either be mush at the small end or a resample at
 * every size in between. A frame made of {@code fill} calls is exactly as
 * sharp at every scale and costs no asset.
 * <p>
 * <b>Absorption is never invisible.</b> Gold continues past the main fill
 * while there is room for it, and is drawn <em>over</em> the fill's right-hand
 * end when there is not — a shielded player at full pool still sees gold. The
 * original bar clamped the gold segment to the space the red had left, which
 * at full health is zero width, so the one state absorption most needs to
 * announce itself in was the one state that showed nothing.
 */
public class HealthBarPaint {

    /**
     * Below this fraction the pool is "low": the fill has finished blending to
     * the alarm colour and the overlay's pulse starts. One number, so the
     * colour shift and the throb read as one cue rather than two thresholds
     * the player has to learn separately.
     */
    public static final float LOW_FRACTION = 0.30f;

    /**
     * Where the fill starts warming up toward {@link #LOW_FRACTION}.
     */
    private static final float ALERT_START_FRACTION = 0.60f;

    private static final int BORDER_COLOR = 0xCC000000;
    /**
     * The fill is not one red but a blend between two, chosen by how much pool
     * is left. A bar that is bright scarlet at 225/225 spends its loudest
     * colour on the one state that needs no attention, and leaves nothing to
     * escalate to.
     */
    private static final int CALM_TOP = 0xFFA83A3A;
    private static final int CALM_BOTTOM = 0xFF4E1212;
    private static final int ALERT_TOP = 0xFFFF4444;
    private static final int ALERT_BOTTOM = 0xFF8E1414;

    private static final int ABSORB_TOP = 0xFFFFD75E;
    private static final int ABSORB_BOTTOM = 0xFFB07C0C;
    private static final int ABSORB_TEXT = 0xFFFFD75E;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /**
     * The carved chrome of {@link HealthStyle#ORNATE}: a near-black outer
     * border, a bevel cut into the box's own edge, and short gold brackets that
     * only touch the corners — a full gold outline at this size reads as a
     * highlighter, where four corners read as a mount.
     */
    private static final int ORNATE_BORDER = 0xF005050A;
    private static final int BEVEL_SHADE = 0x66000000;
    private static final int BEVEL_LIGHT = 0x38FFFFFF;
    private static final int BRACKET_LEN = 4;

    /**
     * Ten segments, one gap of a single pixel between each pair.
     */
    private static final int PIP_COUNT = 10;
    private static final int PIP_GAP = 1;

    /**
     * Gap between the readout and the gold absorption figure in
     * {@link HealthStyle#HEARTS}.
     */
    private static final int ABSORB_TEXT_GAP = 3;

    private HealthBarPaint() {
    }

    /**
     * The element in whichever style is selected, from explicit values.
     *
     * @param absorption already converted into pool units by the caller
     * @param flash      1 &rarr; 0 white wash right after a drop
     * @param pulse      0 &rarr; 1 &rarr; 0 brightening while the pool is low
     */
    public static void draw(GuiGraphicsExtractor ctx, HealthStyle style, int x, int y, int w, int h,
                            double current, double max, double absorption, float flash, float pulse) {
        switch (style) {
            case HEARTS -> hearts(ctx, x, y, w, h, current, max, absorption);
            case BAR -> bar(ctx, x, y, w, h, current, max, absorption, flash, pulse);
            case ORNATE -> ornate(ctx, x, y, w, h, current, max, absorption, flash, pulse);
            case PIPS -> pips(ctx, x, y, w, h, current, max, absorption, flash, pulse);
        }
    }

    /**
     * The readout alone, over vanilla's untouched heart row.
     * <p>
     * The box is where the <em>numbers</em> go, not where the hearts go:
     * vanilla owns the hearts' position and this style deliberately does not
     * move them, so dragging this element in the layout editor slides the
     * readout around and leaves the hearts where every other mod and every
     * screenshot expects them.
     * <p>
     * No flash and no pulse: vanilla's own hearts already shake and flash on
     * damage, and a second, differently-timed alarm beside them would read as a
     * glitch rather than as emphasis.
     */
    private static void hearts(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                               double current, double max, double absorption) {
        Font font = Minecraft.getInstance().font;
        Component readout = readout(font, current, max, w);
        Component absorb = absorbText(absorption);

        int width = font.width(readout);
        if (absorb != null) width += ABSORB_TEXT_GAP + font.width(absorb);

        int textX = x + (w - width) / 2;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        ctx.text(font, readout, textX, textY, TEXT_COLOR, true);
        if (absorb != null) {
            ctx.text(font, absorb, textX + font.width(readout) + ABSORB_TEXT_GAP, textY, ABSORB_TEXT, true);
        }
    }

    /**
     * The compact health bar, with absorption layered over the health pool.
     */
    private static void bar(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                            double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, BORDER_COLOR);

        int fillW = CoiBar.lerpWidth(current, max, w);
        fillPool(ctx, x, y, h, fillW, current, max, pulse);

        int[] span = absorbSpan(x, w, fillW, absorption, max);
        if (span != null) CoiBar.fill(ctx, span[0], y, h, span[1], ABSORB_TOP, ABSORB_BOTTOM);

        flash(ctx, x, y, w, h, flash);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    /**
     * The same geometry under carved chrome: a near-black outer border, a bevel
     * that reads as a recess cut into the plate, and gold corner brackets.
     */
    private static void ornate(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                               double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, ORNATE_BORDER);

        int fillW = CoiBar.lerpWidth(current, max, w);
        fillPool(ctx, x, y, h, fillW, current, max, pulse);

        // Inset by a pixel all round, so the bevel below stays visible through
        // the gold instead of being painted over by it
        int[] span = absorbSpan(x + 1, w - 2, Math.max(0, fillW - 1), absorption, max);
        if (span != null) CoiBar.fill(ctx, span[0], y + 1, h - 2, span[1], ABSORB_TOP, ABSORB_BOTTOM);

        bevel(ctx, x, y, w, h);
        flash(ctx, x, y, w, h, flash);
        brackets(ctx, x, y, w, h);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    /**
     * The pool as ten segments that drain the way a heart row does, last one
     * partially. The gaps are cut <em>after</em> every fill, so a notch shows
     * through red and gold alike and the row cannot be mistaken for one long
     * bar with a stripe pattern.
     * <p>
     * Segment widths come from a running division of the whole box rather than
     * from a single {@code (w - gaps) / 10}: the nominal width is 8.7px at the
     * shipped width, and rounding each pip down separately would leave the row
     * several pixels short of the box the layout editor reports.
     */
    private static void pips(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                             double current, double max, double absorption, float flash, float pulse) {
        CoiBar.frame(ctx, x, y, w, h, BORDER_COLOR);

        float urgency = urgency(current, max);
        int top = lerpColor(CALM_TOP, ALERT_TOP, urgency);
        int bottom = lerpColor(CALM_BOTTOM, ALERT_BOTTOM, urgency);
        double filled = max <= 0 ? 0 : Mth.clamp(current / max, 0, 1) * PIP_COUNT;

        for (int i = 0; i < PIP_COUNT; i++) {
            int px = pipStart(x, w, i);
            int pw = pipEnd(x, w, i) - px;
            int part = (int) Math.round(Mth.clamp(filled - i, 0, 1) * pw);
            if (part <= 0) continue;
            CoiBar.fill(ctx, px, y, h, part, top, bottom);
            if (pulse > 0) ctx.fill(px, y, px + part, y + h, CoiBar.withAlpha(0x38FFFFFF, pulse));
        }

        // Gold grows inward from the right-hand end, which is where a shield
        // reads as "on top of" the pool rather than as part of it
        double shield = max <= 0 ? 0 : Mth.clamp(absorption / max, 0, 1) * PIP_COUNT;
        for (int i = PIP_COUNT - 1; i >= 0 && shield > 0; i--) {
            int px = pipStart(x, w, i);
            int pw = pipEnd(x, w, i) - px;
            int part = (int) Math.round(Mth.clamp(shield - (PIP_COUNT - 1 - i), 0, 1) * pw);
            if (part <= 0) break;
            CoiBar.fill(ctx, px + pw - part, y, h, part, ABSORB_TOP, ABSORB_BOTTOM);
        }

        for (int i = 1; i < PIP_COUNT; i++) {
            ctx.fill(pipEnd(x, w, i - 1), y, pipStart(x, w, i), y + h, BORDER_COLOR);
        }

        flash(ctx, x, y, w, h, flash);
        readoutInside(ctx, x, y, w, h, current, max);
    }

    /**
     * Left edge of segment {@code i}, and {@link #pipEnd} its exclusive right
     * edge. Both are running divisions of {@code w + PIP_GAP}, so segment 0
     * starts exactly at {@code x} and segment 9 ends exactly at {@code x + w}
     * whatever the width divides into.
     */
    private static int pipStart(int x, int w, int i) {
        return x + i * (w + PIP_GAP) / PIP_COUNT;
    }

    private static int pipEnd(int x, int w, int i) {
        return x + (i + 1) * (w + PIP_GAP) / PIP_COUNT - PIP_GAP;
    }

    /**
     * The red fill plus the low-pool brightening over it.
     */
    private static void fillPool(GuiGraphicsExtractor ctx, int x, int y, int h, int fillW,
                                 double current, double max, float pulse) {
        float urgency = urgency(current, max);
        CoiBar.fill(ctx, x, y, h, fillW,
                lerpColor(CALM_TOP, ALERT_TOP, urgency),
                lerpColor(CALM_BOTTOM, ALERT_BOTTOM, urgency));
        if (pulse > 0 && fillW > 0) {
            ctx.fill(x, y, x + fillW, y + h, CoiBar.withAlpha(0x38FFFFFF, pulse));
        }
    }

    /**
     * Where the gold goes: immediately past the main fill while the box has
     * room for it, otherwise over the fill's own right-hand end.
     *
     * @return {@code {x, width}}, or null when there is no absorption to draw
     */
    private static int[] absorbSpan(int x, int w, int fillW, double absorption, double max) {
        int absorbW = Math.min(w, CoiBar.lerpWidth(absorption, max, w));
        if (absorbW <= 0) return null;
        int start = Math.min(x + fillW, x + w - absorbW);
        return new int[]{Math.max(x, start), absorbW};
    }

    /**
     * The recess: shade along the top and left edges, light along the bottom
     * and right ones. Drawn over the fill rather than under it, so the carve
     * reads as the plate the pool sits in rather than as a tint on the pool.
     */
    private static void bevel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + 1, BEVEL_SHADE);
        ctx.fill(x, y, x + 1, y + h, BEVEL_SHADE);
        ctx.fill(x, y + h - 1, x + w, y + h, BEVEL_LIGHT);
        ctx.fill(x + w - 1, y, x + w, y + h, BEVEL_LIGHT);
    }

    /**
     * Two gold ticks at each corner, laid on the outer border ring. Their
     * length is capped at half the box, so the brackets stay brackets rather
     * than meeting in the middle at a small {@code beyonderHealthScale}.
     */
    private static void brackets(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int gold = CoiStyle.ACCENT;
        int lx = Math.min(BRACKET_LEN, w / 2);
        int ly = Math.min(BRACKET_LEN, h / 2);
        int x0 = x - 1, y0 = y - 1, x1 = x + w + 1, y1 = y + h + 1;

        ctx.fill(x0, y0, x0 + lx, y0 + 1, gold);
        ctx.fill(x0, y0, x0 + 1, y0 + ly, gold);
        ctx.fill(x1 - lx, y0, x1, y0 + 1, gold);
        ctx.fill(x1 - 1, y0, x1, y0 + ly, gold);
        ctx.fill(x0, y1 - 1, x0 + lx, y1, gold);
        ctx.fill(x0, y1 - ly, x0 + 1, y1, gold);
        ctx.fill(x1 - lx, y1 - 1, x1, y1, gold);
        ctx.fill(x1 - 1, y1 - ly, x1, y1, gold);
    }

    private static void flash(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float flash) {
        if (flash > 0) ctx.fill(x, y, x + w, y + h, CoiBar.withAlpha(0xB0FFFFFF, flash));
    }

    /**
     * Inside the fill, not above it: the bar has the hearts' footprint and
     * nothing to spare. The shadow is what keeps it legible over both the lit
     * and the empty half.
     */
    private static void readoutInside(GuiGraphicsExtractor ctx, int x, int y, int w, int h,
                                      double current, double max) {
        Font font = Minecraft.getInstance().font;
        Component readout = readout(font, current, max, w);
        int textX = x + (w - font.width(readout)) / 2;
        ctx.text(font, readout, textX, y + (h - font.lineHeight) / 2 + 1, TEXT_COLOR, true);
    }

    /**
     * The widest readout that still fits, measured rather than assumed:
     * grouped digits first, then ungrouped, then the current value alone.
     * Four-digit pools plus a scale of 0.5 make "1,234 / 1,750" wider than the
     * box, and a number spilling past the frame looks broken in a way a plainer
     * number does not.
     */
    private static Component readout(Font font, double current, double max, int w) {
        int room = w - 4;
        Component grouped = Component.translatable("hud.coi.health_readout",
                format(current, true), format(max, true));
        if (font.width(grouped) <= room) return grouped;

        Component plain = Component.translatable("hud.coi.health_readout",
                format(current, false), format(max, false));
        if (font.width(plain) <= room) return plain;

        return Component.translatable("hud.coi.health_current", format(current, false));
    }

    /**
     * The {@code +N} shield figure, or null when there is no shield to name —
     * absorption that rounds to nothing is not worth a line of its own.
     */
    private static Component absorbText(double absorption) {
        long shield = Math.round(absorption);
        if (shield <= 0) return null;
        return Component.translatable("hud.coi.health_absorb", format(absorption, true));
    }

    /**
     * 0 while the pool is comfortable, 1 once it is low — the blend between the
     * calm and the alarm fill.
     */
    private static float urgency(double current, double max) {
        if (max <= 0) return 0f;
        float fraction = Mth.clamp((float) (current / max), 0f, 1f);
        return Mth.clamp((ALERT_START_FRACTION - fraction)
                / (ALERT_START_FRACTION - LOW_FRACTION), 0f, 1f);
    }

    /**
     * Per-channel blend. Both ends are opaque, so alpha is taken from {@code a}
     * rather than interpolated.
     */
    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = ar + Math.round((br - ar) * t);
        int g = ag + Math.round((bg - ag) * t);
        int bl = ab + Math.round((bb - ab) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /**
     * Pool units are whole numbers to the player; the fraction only matters to
     * the mirror.
     */
    private static String format(double value, boolean grouped) {
        return String.format(Locale.ROOT, grouped ? "%,d" : "%d", Math.max(0L, Math.round(value)));
    }
}
