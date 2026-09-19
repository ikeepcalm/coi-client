package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.hud.render.SpiritSprites;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * The spirituality bar — the client-side replacement for the server's paired
 * boss bars, rebuilt from the very sprites those boss bars used (see
 * {@link SpiritSprites}). Only drawn once a protocol-2 server has actually sent
 * spirituality on {@code coi-client:conditions}.
 * <p>
 * Behaviour mirrors the boss bar it replaces: the cracked frame below 30% of
 * max, red numbers below 25%, and (when {@code spiritualityHideWhenFull} is on)
 * the bar retires at full. Appearing is instant so a cast shows up on the very
 * next frame; only the retreat is slow. With the setting off it stays as a
 * dimmed idle bar instead of vanishing.
 */
public class SpiritualityOverlay {

    private static final Identifier SPIRITUALITY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "spirituality");

    public static final int BAR_WIDTH = SpiritSprites.FILL_W;
    public static final int BAR_HEIGHT = SpiritSprites.FILL_H;

    public static final int FRAME_W = SpiritSprites.FRAME_W;
    public static final int FRAME_H = SpiritSprites.FRAME_H;
    public static final int FRAME_DX = SpiritSprites.FRAME_DX;
    public static final int FRAME_DY = SpiritSprites.FRAME_DY;

    /** Numbers sit one row above the frame, clear of the flask neck on rows 1..8. */
    private static final int LABEL_DY = -10;

    private static final int LABEL_COLOR = 0xFF55FFFF;
    private static final int LABEL_LOW_COLOR = 0xFFFF5555;
    private static final int GLOW_RGB = 0xBFFFFF;
    private static final int MOTE_RGB = 0xDDFFFF;
    private static final int TRAIL_RGB = 0xA8F0FF;
    private static final int FLASK_DIM = 0x4A5E70;

    private static final float CRITICAL_THRESHOLD = 0.30f;
    private static final float LABEL_LOW_THRESHOLD = 0.25f;

    /** Hide-when-full: linger, then ease away. Coming back is instant. */
    private static final long HOLD_MS = 1500;
    private static final long FADE_OUT_MS = 900;
    private static final long CRITICAL_FADE_MS = 250;
    private static final long TRAIL_MS = 400;
    private static final long REGEN_FLARE_MS = 250;

    /** Glide rates: gentle up, ~150ms down, and a hard snap on a real cost. */
    private static final float UP_RATE = 10f;
    private static final float DOWN_RATE = 20f;
    private static final float SNAP_DROP_RATIO = 0.15f;

    private static final int MOTE_COUNT = 6;

    /**
     * Below this the bar is gone rather than nearly gone, and nothing is drawn.
     */
    private static final float HIDDEN_ALPHA = 0.01f;

    // Smoothed display value
    private static double shown = -1;
    private static double lastTarget = -1;
    private static long lastFrameMs = 0;
    private static long frameDeltaMs = 16;

    // Hide-when-full easing
    private static float opacity = 1f;
    private static long fullSince = 0;

    // Cracked-frame crossfade, 0 = normal frame, 1 = critical frame
    private static float criticalMix = 0f;

    // Drain trail: the width the bar had when the value last dropped
    private static double trailValue = 0;
    private static long trailAt = 0;

    private SpiritualityOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, SPIRITUALITY_LAYER, SpiritualityOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings)
                || !settings.showSpiritualityBar || !BeyonderState.hasSpiritualityData()) {
            return;
        }

        int max = Math.max(1, BeyonderState.getMaxSpirituality());
        int current = Math.clamp(BeyonderState.getSpirituality(), 0, max);
        double predicted = Math.clamp(BeyonderState.predictedSpirituality(), 0, max);
        boolean regen = BeyonderState.isSpiritualityRegen();
        boolean full = current >= max && !regen;
        long time = System.currentTimeMillis();

        float alpha = visibility(full && settings.spiritualityHideWhenFull, time);
        if (alpha < HIDDEN_ALPHA) {
            park(predicted);
            return;
        }

        int[] pos = anchor(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), settings);
        boolean idle = full && !settings.spiritualityHideWhenFull;
        boolean epilepsy = settings.epilepsyMode;
        float ratio = current / (float) max;

        double display = smoothed(predicted, max, time);
        int fillW = CoiBar.lerpWidth(display, max, BAR_WIDTH);
        float mix = criticalMix(ratio < CRITICAL_THRESHOLD, epilepsy);

        HudScale.push(ctx, pos[0], pos[1], settings.spiritualityScale);
        drawTrail(ctx, pos[0], pos[1], fillW, max, alpha, time);
        drawFill(ctx, pos[0], pos[1], fillW, alpha, idle, epilepsy, time, ratio, regenFlare(regen, epilepsy));
        if (!epilepsy) drawFlash(ctx, pos[0], pos[1], fillW, alpha);
        drawFrames(ctx, pos[0], pos[1], alpha, mix, ratio);
        drawLabel(ctx, client.font, pos[0], pos[1], Math.round(predicted), max, ratio, alpha);
        HudScale.pop(ctx);
    }

    /**
     * While the bar is hidden the smoothing is parked on the real value, so
     * coming back is instant instead of gliding in from wherever it stopped.
     */
    private static void park(double predicted) {
        shown = predicted;
        lastTarget = predicted;
        lastFrameMs = 0;
    }

    /**
     * {@code {x, y, w, h}} of everything this bar draws — the frame with its
     * ornaments, plus the one row the numbers poke above it.
     */
    public static int[] bounds(int screenW, int screenH, HudConfig.HudSettings s) {
        int[] pos = anchor(screenW, screenH, s);
        float scale = s.spiritualityScale;
        return new int[]{pos[0] + HudScale.size(FRAME_DX, scale), pos[1] + HudScale.size(LABEL_DY, scale),
                HudScale.size(FRAME_W, scale), HudScale.size(FRAME_H + (FRAME_DY - LABEL_DY), scale)};
    }

    /**
     * A representative bar at ~72% with the regen luster running, always fully
     * opaque — no hide-when-full, no data requirement, no shared state touched.
     */
    public static void renderPreview(GuiGraphicsExtractor ctx, int screenW, int screenH, HudConfig.HudSettings s, long timeMs) {
        renderPreview(ctx, screenW, screenH, s, timeMs, 0.72f);
    }

    public static void renderPreview(GuiGraphicsExtractor ctx, int screenW, int screenH,
                                     HudConfig.HudSettings s, long timeMs, float ratio) {
        Minecraft client = Minecraft.getInstance();
        int[] pos = anchor(screenW, screenH, s);
        int max = 500;
        ratio = Math.clamp(ratio, 0f, 1f);
        int fillW = CoiBar.lerpWidth(max * ratio, max, BAR_WIDTH);
        float flare = s.epilepsyMode ? 1f : 1f + 0.6f * (float) Math.abs(Math.sin(timeMs * 0.0015));

        HudScale.push(ctx, pos[0], pos[1], s.spiritualityScale);
        drawFill(ctx, pos[0], pos[1], fillW, 1f, false, s.epilepsyMode, timeMs, ratio, flare);
        drawFrames(ctx, pos[0], pos[1], 1f, ratio < CRITICAL_THRESHOLD ? 1f : 0f, ratio);
        drawLabel(ctx, client.font, pos[0], pos[1], Math.round(max * ratio), max, ratio, 1f);
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the fill — the point the bar scales about, and the
     * one the layout editor's {@code Spirituality} element asks for.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.spiritualityAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.spiritualityScale), s.spiritualityXOffset,
                s.spiritualityYOffset, s.spiritualityYOffset);
    }

    /**
     * The fill sprite plus its luster: leading-edge bloom and drifting motes.
     */
    private static void drawFill(GuiGraphicsExtractor ctx, int x, int y, int fillW, float alpha,
                                 boolean idle, boolean epilepsy, long time,
                                 float ratio, float flare) {
        float tint = 1f;
        if (ratio < CRITICAL_THRESHOLD && !epilepsy) {
            tint = 0.80f + 0.20f * (float) Math.sin(time * 0.0031);
        }
        float fillAlpha = idle ? alpha * 0.6f : alpha;
        SpiritSprites.fill(ctx, x, y, fillW, grayTint(tint, fillAlpha));
        if (fillW <= 0) return;

        if (!idle && !epilepsy) drawEdgeGlow(ctx, x, y, fillW, fillAlpha, flare);
        drawMotes(ctx, x, y, fillW, fillAlpha, epilepsy, time);
    }

    /**
     * A 6px bloom hugging the fill's right edge, brightest just after a regen tick.
     */
    private static void drawEdgeGlow(GuiGraphicsExtractor ctx, int x, int y, int fillW, float alpha, float flare) {
        int edge = x + fillW;
        int[] widths = {6, 4, 2};
        int[] alphas = {26, 60, 108};
        for (int i = 0; i < widths.length; i++) {
            int x0 = Math.max(x, edge - widths[i]);
            if (x0 >= edge) continue;
            int a = (int) Math.min(255, alphas[i] * flare * alpha);
            ctx.fill(x0, y, edge, y + BAR_HEIGHT, EffectPaint.argb(GLOW_RGB, a));
        }
    }

    /**
     * One-pixel motes drifting rightwards inside the fill, each on its own
     * phase; frozen in place under epilepsy mode.
     */
    private static void drawMotes(GuiGraphicsExtractor ctx, int x, int y, int fillW, float alpha,
                                  boolean epilepsy, long time) {
        // time is wrapped before scaling: a raw epoch millis in float loses the
        // low bits and the motes would jitter in ~1s steps instead of drifting
        double drift = time % 600_000L;
        for (int i = 0; i < MOTE_COUNT; i++) {
            double base = i * 31.7;
            double speed = 0.008 + (i % 3) * 0.004;
            double mx = (epilepsy ? base : base + drift * speed) % fillW;
            int px = x + (int) mx;
            if (px < x || px >= x + fillW) continue;
            float twinkle = epilepsy ? 0.55f : (float) (0.35 + 0.65 * Math.sin(time * 0.003 + i * 1.7));
            if (twinkle <= 0) continue;
            int py = y + 1 + (i % 3);
            ctx.fill(px, py, px + 1, py + 1, EffectPaint.argb(MOTE_RGB, (int) (190 * twinkle * alpha)));
        }
    }

    /**
     * The segment just lost, held as a lighter band while it fades.
     */
    private static void drawTrail(GuiGraphicsExtractor ctx, int x, int y, int fillW, int max, float alpha, long time) {
        if (trailAt == 0) return;
        long elapsed = time - trailAt;
        if (elapsed >= TRAIL_MS) return;
        int fromW = CoiBar.lerpWidth(trailValue, max, BAR_WIDTH);
        if (fromW <= fillW) return;
        float a = 1f - elapsed / (float) TRAIL_MS;
        ctx.fill(x + fillW, y, x + fromW, y + BAR_HEIGHT, EffectPaint.argb(TRAIL_RGB, (int) (150 * a * alpha)));
    }

    private static void drawFlash(GuiGraphicsExtractor ctx, int x, int y, int fillW, float alpha) {
        float flash = BeyonderState.getSpiritualityFlashIntensity();
        if (flash > 0 && fillW > 0) {
            ctx.fill(x, y, x + fillW, y + BAR_HEIGHT, CoiBar.withAlpha(0xFFFFFFFF, flash * 0.63f * alpha));
        }
    }

    /**
     * The rails, crossfading to the cracked variant, with the flask ornament's
     * bulb dimmed down to the current level.
     */
    private static void drawFrames(GuiGraphicsExtractor ctx, int x, int y, float alpha, float mix, float ratio) {
        if (mix < 1f) SpiritSprites.frame(ctx, x, y, false, grayTint(1f, alpha * (1f - mix)));
        if (mix > 0f) SpiritSprites.frame(ctx, x, y, true, grayTint(1f, alpha * mix));
        if (mix < 1f) SpiritSprites.flaskLevel(ctx, x, y, ratio, EffectPaint.argb(FLASK_DIM, (int) (255 * alpha * (1f - mix))));
    }

    /**
     * Numbers only — the flask is the identity — right-aligned to the fill's
     * right edge, where the frame is transparent.
     */
    private static void drawLabel(GuiGraphicsExtractor ctx, Font font, int x, int y, long value, int max,
                                 float ratio, float alpha) {
        if (alpha < 0.05f) return;
        String text = value + " / " + max;
        int color = ratio < LABEL_LOW_THRESHOLD ? LABEL_LOW_COLOR : LABEL_COLOR;
        int labelX = x + BAR_WIDTH - font.width(text);
        ctx.text(font, text, labelX, y + LABEL_DY, CoiBar.withAlpha(color, alpha), true);
    }

    /**
     * Glides toward the (predicted) value. A big drop snaps so the cost of an
     * ability reads as an instant hit; a small one — including the prediction
     * being corrected downwards by a packet — eases over ~150ms instead.
     */
    private static double smoothed(double target, int max, long time) {
        if (shown < 0) shown = target;
        if (lastTarget >= 0 && target < lastTarget - 0.01) {
            trailValue = shown;
            trailAt = time;
        }
        lastTarget = target;

        frameDeltaMs = lastFrameMs == 0 ? 16 : Math.clamp(time - lastFrameMs, 0, 100);
        float dt = frameDeltaMs / 1000f;
        lastFrameMs = time;

        if (shown - target > max * SNAP_DROP_RATIO) {
            shown = target;
        } else {
            float rate = target < shown ? DOWN_RATE : UP_RATE;
            shown += (target - shown) * Math.min(1f, dt * rate);
            if (Math.abs(shown - target) < 0.5) shown = target;
        }
        return shown;
    }

    /**
     * Hide-when-full visibility: snaps to 1 the moment the pool is spendable
     * again, and only leaves after a hold plus a slow smoothstep fade.
     */
    private static float visibility(boolean wantHidden, long time) {
        if (!wantHidden) {
            fullSince = 0;
            opacity = 1f;
            return 1f;
        }
        if (fullSince == 0) fullSince = time;
        long elapsed = time - fullSince;
        if (elapsed < HOLD_MS) return opacity = 1f;
        float t = EffectPaint.clamp((elapsed - HOLD_MS) / (float) FADE_OUT_MS, 0f, 1f);
        return opacity = 1f - EffectPaint.smoothstep(t);
    }

    private static float criticalMix(boolean critical, boolean epilepsy) {
        float target = critical ? 1f : 0f;
        if (epilepsy) return criticalMix = target;
        float step = Math.min(1f, frameDeltaMs / (float) CRITICAL_FADE_MS);
        criticalMix += (target - criticalMix) * step;
        if (Math.abs(target - criticalMix) < 0.01f) criticalMix = target;
        return criticalMix;
    }

    /**
     * 1 normally, up to 1.8 for {@value #REGEN_FLARE_MS}ms after a regen packet.
     */
    private static float regenFlare(boolean regen, boolean epilepsy) {
        if (!regen || epilepsy) return 1f;
        long age = BeyonderState.spiritualityGainAgeMs();
        if (age >= REGEN_FLARE_MS) return 1f;
        return 1f + 0.8f * (1f - age / (float) REGEN_FLARE_MS);
    }

    /**
     * A white tint scaled to {@code brightness}, carrying {@code alpha} — the
     * multiply colour every sprite blit takes.
     */
    private static int grayTint(float brightness, float alpha) {
        int v = (int) (Math.clamp(brightness, 0f, 1f) * 255);
        int a = (int) (Math.clamp(alpha, 0f, 1f) * 255);
        return (a << 24) | (v << 16) | (v << 8) | v;
    }

    public static void reset() {
        shown = -1;
        lastTarget = -1;
        lastFrameMs = 0;
        frameDeltaMs = 16;
        opacity = 1f;
        fullSince = 0;
        criticalMix = 0f;
        trailValue = 0;
        trailAt = 0;
    }
}
