package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;

public class TunnelEffect implements VisualEffect {

    public static final String ID = "tunnel";

    private float intensity = 0.7f;
    private long duration = 6000;
    private long closeDuration = 2000;
    private long startTime;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Tunnel Vision";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=6000,closeDuration=2000";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        // Open phase: fade out at the end
        float closedFrac;
        long remaining = duration - elapsed;
        if (elapsed < closeDuration) {
            closedFrac = (float) elapsed / closeDuration;
        } else if (remaining < closeDuration) {
            closedFrac = Math.max(0f, (float) remaining / closeDuration);
        } else {
            closedFrac = 1f;
        }

        float closed = closedFrac * intensity;   // 0 = full screen visible, intensity = fully closed

        // Oval dimensions of the visible "hole"
        // At closed=0: rx=w/2, ry=h/2  → full screen visible
        // At closed=1 (intensity): much smaller hole
        float rx = w * 0.5f * (1f - closed * 0.92f);
        float ry = h * 0.5f * (1f - closed * 0.92f);
        int cx = w / 2, cy = h / 2;

        int col = 0xFF000000; // solid black

        // Scanline: every 3 rows — ~h/3 fill calls per frame
        int step = 3;
        for (int y = 0; y < h; y += step) {
            float dy = (y + step / 2f) - cy;
            float fracY = ry > 0 ? dy / ry : 2f;

            if (Math.abs(fracY) >= 1f) {
                // Completely outside the oval
                ctx.fill(0, y, w, y + step, col);
            } else {
                float fracX = (float) Math.sqrt(1.0 - fracY * fracY);
                int innerL = (int) (cx - rx * fracX);
                int innerR = (int) (cx + rx * fracX);
                if (innerL > 0) ctx.fill(0, y, innerL, y + step, col);
                if (innerR < w) ctx.fill(innerR, y, w, y + step, col);
            }
        }
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
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
                case "closeDuration" -> closeDuration = Long.parseLong(kv[1].trim());
            }
        }
    }
}
