package dev.ua.ikeepcalm.coi.client.hud.render;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.ui.IconModels;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The fillable gauge symbols on the character plate — a brain for sanity, a
 * mask for acting.
 * <p>
 * A symbol is what lets the plate carry several meters without becoming a stack
 * of interchangeable bars again: the shape says which meter it is, the fill says
 * how much.
 * <p>
 * Each sheet is a single {@value #SIZE}×{@value #SIZE} <em>full-colour</em>
 * sprite, drawn three times over itself:
 * <ol>
 *   <li>the whole sprite under {@link #EMPTY_RGB}, a dark multiply that leaves
 *       the artwork legible but plainly drained;</li>
 *   <li>its bottom {@code fill} rows again under the caller's wash — white for
 *       the art's own colours, a state colour when the gauge wants to shout;</li>
 *   <li>the rows a ceiling puts out of reach, under {@link #CAPPED_RGB}.</li>
 * </ol>
 * Tinting rather than masking is what lets the sprites be ordinary artwork: a
 * pack can drop in any {@value #SIZE}×{@value #SIZE} PNG and it fills correctly,
 * with no silhouette/ink frame pair to author.
 * <p>
 * The partial fills are <em>sub-rect blits</em> and not a scissor on purpose: a
 * scissor is resolved in window pixels and would cut the wrong rows once the
 * plate sits inside a {@link HudScale} push, whereas a sub-rect composes with
 * whatever transform is already on the pose.
 */
public class PlateSymbols {

    /**
     * The sheet's edge, and the size the symbol is drawn at — the two are equal
     * so every blit is 1:1 and the artwork never resamples. The shipped sprites
     * are a 32×32 brain and a 64×64 clown reduced 2:1, both whole-pixel ratios.
     */
    public static final int SIZE = 32;

    private static final int SHEET_W = SIZE;
    private static final int SHEET_H = SIZE;

    /**
     * Wash for "draw the artwork in its own colours".
     */
    public static final int NO_WASH = 0xFFFFFF;

    public static final Identifier BRAIN = sheet("symbol_brain");
    public static final Identifier MASK = sheet("symbol_mask");

    /**
     * The unfilled body. A multiply, so it dims the artwork towards its own
     * shadow rather than flattening it to one grey — dark enough to read as
     * "missing", light enough that the shape is still legible when empty.
     */
    private static final int EMPTY_RGB = 0x4A4A56;
    /**
     * The band a ceiling puts out of reach — kept close to {@link #EMPTY_RGB}
     * so it reads as damage to the gauge itself, never as a value.
     */
    private static final int CAPPED_RGB = 0x40202A;

    /**
     * Whether each sheet is actually in the loaded packs. Blitting a missing
     * texture every frame is a loud failure for a purely cosmetic row, so an
     * absent sheet degrades to {@link #fallback} instead. Cleared on resource
     * reload for the same reason {@code IconModels}' cache is.
     */
    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();

    private PlateSymbols() {
    }

    private static Identifier sheet(String name) {
        return Identifier.fromNamespaceAndPath("coi-client", "textures/gui/hud/" + name + ".png");
    }

    /**
     * Draws one symbol, filled bottom-up to {@code fill}.
     *
     * @param fill 0..1 of the symbol's height
     * @param cap  the highest fraction the fill can ever reach (1 for no
     *             ceiling); the rows above it are drawn dead, which is how
     *             permanent madness shows up as lost headroom
     * @param wash multiplied over the filled rows; {@link #NO_WASH} keeps the
     *             artwork's own colours
     */
    public static void draw(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y,
                            float fill, float cap, int wash) {
        float f = Mth.clamp(fill, 0f, 1f);
        float c = Mth.clamp(cap, 0f, 1f);
        if (!present(symbol)) {
            fallback(ctx, x, y, f, c, wash);
            return;
        }

        // Every tint here is the blit's own alpha channel, so each one goes
        // through HudOpacity or a faded plate would keep solid symbols
        body(ctx, symbol, x, y, HudOpacity.apply(EffectPaint.argb(EMPTY_RGB, 255)));
        fillUp(ctx, symbol, x, y, f, HudOpacity.apply(EffectPaint.argb(wash, 255)));
        // The dead band goes on last so it also covers any fill that a stale
        // value pushed above the ceiling
        capBand(ctx, symbol, x, y, c, HudOpacity.apply(EffectPaint.argb(CAPPED_RGB, 255)));
    }

    /**
     * The whole silhouette in one tint.
     */
    private static void body(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, int argb) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y, 0f, 0f, SIZE, SIZE, SHEET_W, SHEET_H, argb);
    }

    /**
     * The bottom {@code fraction} of the silhouette: source rows
     * {@code v = SIZE - h .. SIZE} land at the matching rows of the destination,
     * so the fill rises rather than squashing.
     */
    private static void fillUp(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, float fraction, int argb) {
        int h = Math.round(SIZE * fraction);
        if (h <= 0) return;
        int v = SIZE - h;
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y + v, 0f, v, SIZE, h, SIZE, h, SHEET_W, SHEET_H, argb);
    }

    /**
     * The top rows a ceiling puts out of reach, measured from the top down.
     */
    private static void capBand(GuiGraphicsExtractor ctx, Identifier symbol, int x, int y, float cap, int argb) {
        int h = SIZE - Math.round(SIZE * cap);
        if (h <= 0) return;
        ctx.blit(RenderPipelines.GUI_TEXTURED, symbol, x, y, 0f, 0f, SIZE, h, SIZE, h, SHEET_W, SHEET_H, argb);
    }

    private static boolean present(Identifier symbol) {
        Boolean cached = PRESENT.get(symbol);
        if (cached != null) return cached;
        boolean found;
        try {
            Minecraft client = Minecraft.getInstance();
            found = client != null && client.getResourceManager() != null
                    && client.getResourceManager().getResource(symbol).isPresent();
        } catch (Exception e) {
            found = false;
        }
        PRESENT.put(symbol, found);
        return found;
    }

    /**
     * Forgets which sheets were found, because a different pack may define (or
     * stop defining) them.
     */
    public static void clearCache() {
        PRESENT.clear();
    }

    /**
     * Stand-in for a sheet the packs don't define: a rounded block filled the
     * same way, so the row still reads as a gauge instead of vanishing.
     */
    private static void fallback(GuiGraphicsExtractor ctx, int x, int y, float fill, float cap, int wash) {
        int left = x + 3;
        int right = x + SIZE - 3;
        int top = y + 2;
        int bottom = y + SIZE - 2;
        int span = bottom - top;

        roundedRect(ctx, left, top, right, bottom, HudOpacity.apply(EffectPaint.argb(EMPTY_RGB, 255)));
        roundedRect(ctx, left, bottom - Math.round(span * fill), right, bottom,
                HudOpacity.apply(EffectPaint.argb(wash, 255)));
        roundedRect(ctx, left, top, right, bottom - Math.round(span * cap),
                HudOpacity.apply(EffectPaint.argb(CAPPED_RGB, 255)));
    }

    /**
     * A filled rect whose top and bottom rows are inset by a pixel, clipped to
     * the band the caller asked for.
     */
    private static void roundedRect(GuiGraphicsExtractor ctx, int left, int top, int right, int bottom, int argb) {
        if (bottom <= top || right - left < 2) return;
        ctx.fill(left + 1, top, right - 1, top + 1, argb);
        if (bottom - top > 2) ctx.fill(left, top + 1, right, bottom - 1, argb);
        if (bottom - top > 1) ctx.fill(left + 1, bottom - 1, right - 1, bottom, argb);
    }
}
