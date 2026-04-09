package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FlashEffect implements VisualEffect {

    public static final String ID = "flash";

    private int rgb = 0xFFFFFF;
    private float intensity = 0.6f;
    private long duration = 500;
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
        return "color=FFFFFF,intensity=0.6,duration=500";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;
        float t = (float) elapsed / duration;   // 0 → 1

        // Quick rise (first 15%), slow fade (rest)
        float alpha;
        if (t < 0.15f) alpha = t / 0.15f;
        else alpha = 1f - (t - 0.15f) / 0.85f;

        alpha = Math.max(0f, alpha);

        int a = (int) (220 * intensity * alpha);
        if (a <= 0) return;

        ctx.fill(0, 0, w, h, (a << 24) | rgb);
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration;
    }

    private void parseParams(String params) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "color" -> rgb = Integer.parseInt(kv[1].trim(), 16);
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
