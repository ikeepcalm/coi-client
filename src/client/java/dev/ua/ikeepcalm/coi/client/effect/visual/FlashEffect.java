package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Full-screen color wash with a sharp attack, a quadratic decay and a brighter
 * center bloom, followed by a brief dark afterimage as the eyes readjust. Can
 * re-strike several times within one duration, like lightning.
 * <p>
 * Params: {@code color} (hex RGB, no {@code #}), {@code intensity} (0-1, peak
 * opacity), {@code duration} (ms), {@code flashes} (re-strikes, each weaker).
 */
public class FlashEffect implements VisualEffect {

    public static final String ID = "flash";

    /** Fraction of one strike spent on the attack, the rest on the decay. */
    private static final float ATTACK_FRAC = 0.1f;
    /** Each re-strike peaks at this fraction of the previous one. */
    private static final double STRIKE_FALLOFF = 0.6;
    /** The afterimage runs for this fraction of the duration past the flash. */
    private static final float AFTERIMAGE_FRAC = 0.6f;

    private int rgb = 0xFFFFFF;
    private float intensity = 0.6f;
    private long duration = 500;
    private int flashes = 1;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Color Flash";
    }

    @Override
    public String getDefaultParams() {
        return "color=FFFFFF,intensity=0.6,duration=500,flashes=1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed <= duration) {
            renderFlash(ctx, w, h, elapsed);
        } else {
            renderAfterimage(ctx, w, h, elapsed);
        }
    }

    private void renderFlash(GuiGraphicsExtractor ctx, int w, int h, long elapsed) {
        // Lightning-style envelope: the duration is split across `flashes`
        // re-strikes, each weaker than the last
        float cycle = (float) elapsed / duration * flashes;
        int strike = Math.min(flashes - 1, (int) cycle);
        float ft = cycle - strike;
        float peak = (float) Math.pow(STRIKE_FALLOFF, strike);

        // Sharp attack, quadratic decay
        float alpha = ft < ATTACK_FRAC ? ft / ATTACK_FRAC : 1f - (ft - ATTACK_FRAC) / (1f - ATTACK_FRAC);
        alpha = Math.max(0f, alpha);
        alpha *= alpha * peak;

        int a = (int) (230 * intensity * alpha);
        if (a <= 0) return;

        ctx.fill(0, 0, w, h, EffectPaint.argb(rgb, a));
        renderCenterBloom(ctx, w, h, a);
    }

    /**
     * Brighter core cross, so the flash feels radial rather than flat.
     */
    private void renderCenterBloom(GuiGraphicsExtractor ctx, int w, int h, int a) {
        int bloom = (int) (a * 0.55f);
        int clear = rgb & 0xFFFFFF;
        ctx.fillGradient(0, 0, w, h / 2, clear, EffectPaint.argb(rgb, bloom));
        ctx.fillGradient(0, h / 2, w, h, EffectPaint.argb(rgb, bloom), clear);
        EffectPaint.hGradient(ctx, 0, 0, w / 2, h, clear, EffectPaint.argb(rgb, bloom));
        EffectPaint.hGradient(ctx, w / 2, 0, w, h, EffectPaint.argb(rgb, bloom), clear);
    }

    /**
     * Dark afterimage as the eyes readjust.
     */
    private void renderAfterimage(GuiGraphicsExtractor ctx, int w, int h, long elapsed) {
        float at = (elapsed - duration) / (duration * AFTERIMAGE_FRAC);
        if (at >= 1f) return;
        int a = (int) (45 * intensity * 4f * at * (1f - at));
        if (a > 0) ctx.fill(0, 0, w, h, a << 24);
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration * 1.6f;
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "color" -> rgb = Integer.parseInt(value, 16);
                case "intensity" -> intensity = Float.parseFloat(value);
                case "duration" -> duration = Math.max(80, Long.parseLong(value));
                case "flashes" -> flashes = Math.max(1, Integer.parseInt(value));
            }
        });
    }
}
