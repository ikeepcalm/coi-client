package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FlashEffect implements VisualEffect {

    public static final String ID = "flash";

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
            // Lightning-style envelope: the duration is split across `flashes`
            // re-strikes, each weaker than the last
            float cycle = (float) elapsed / duration * flashes;
            int strike = Math.min(flashes - 1, (int) cycle);
            float ft = cycle - strike;
            float peak = (float) Math.pow(0.6, strike);

            // Sharp attack, quadratic decay
            float alpha = ft < 0.1f ? ft / 0.1f : 1f - (ft - 0.1f) / 0.9f;
            alpha = Math.max(0f, alpha);
            alpha *= alpha * peak;

            int a = (int) (230 * intensity * alpha);
            if (a > 0) {
                ctx.fill(0, 0, w, h, EffectPaint.argb(rgb, a));

                // Center bloom: brighter core cross so the flash feels radial
                int bloom = (int) (a * 0.55f);
                int clear = rgb & 0xFFFFFF;
                ctx.fillGradient(0, 0, w, h / 2, clear, EffectPaint.argb(rgb, bloom));
                ctx.fillGradient(0, h / 2, w, h, EffectPaint.argb(rgb, bloom), clear);
                EffectPaint.hGradient(ctx, 0, 0, w / 2, h, clear, EffectPaint.argb(rgb, bloom));
                EffectPaint.hGradient(ctx, w / 2, 0, w, h, EffectPaint.argb(rgb, bloom), clear);
            }
        } else {
            // Dark afterimage as the eyes readjust
            float at = (elapsed - duration) / (duration * 0.6f);
            if (at < 1f) {
                int a = (int) (45 * intensity * 4f * at * (1f - at));
                if (a > 0) ctx.fill(0, 0, w, h, a << 24);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration * 1.6f;
    }

    private void parseParams(String params) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "color" -> rgb = Integer.parseInt(kv[1].trim(), 16);
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "duration" -> duration = Math.max(80, Long.parseLong(kv[1].trim()));
                case "flashes" -> flashes = Math.max(1, Integer.parseInt(kv[1].trim()));
            }
        }
    }
}
