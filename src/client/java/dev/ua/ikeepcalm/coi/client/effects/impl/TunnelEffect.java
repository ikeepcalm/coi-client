package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;

        // Close phase, hold, open phase — eased so the walls glide, not march
        float closedFrac;
        long remaining = duration - elapsed;
        if (elapsed < closeDuration) {
            closedFrac = (float) elapsed / closeDuration;
        } else if (remaining < closeDuration) {
            closedFrac = Math.max(0f, (float) remaining / closeDuration);
        } else {
            closedFrac = 1f;
        }
        closedFrac = EffectPaint.smoothstep(closedFrac);

        float closed = closedFrac * intensity;   // 0 = full screen visible, intensity = fully closed
        if (closed <= 0.001f) return;

        // The hole breathes and drifts slightly off-center — claustrophobic, organic
        float breathe = 1f + 0.025f * (float) Math.sin(elapsed * 0.0032);
        float rx = w * 0.5f * (1f - closed * 0.93f) * breathe;
        float ry = h * 0.5f * (1f - closed * 0.93f) * breathe;
        float cx = w / 2f + (float) Math.sin(elapsed * 0.0011) * w * 0.008f * closed;
        float cy = h / 2f + (float) Math.cos(elapsed * 0.0009) * h * 0.008f * closed;

        // Three nested ellipses: transparent hole → 35% → 70% → solid black,
        // giving the edge a soft falloff instead of a hard cutout.
        float m1 = 1.14f;
        float m2 = 1.30f;
        int solid = 0xFF000000;
        int band70 = 0xB4000000;
        int band35 = 0x59000000;

        int step = 3;
        for (int y = 0; y < h; y += step) {
            float dy = (y + step / 2f) - cy;

            float fyOuter = ry > 0 ? dy / (ry * m2) : 2f;
            if (Math.abs(fyOuter) >= 1f) {
                // Row entirely outside the outer ellipse
                ctx.fill(0, y, w, y + step, solid);
                continue;
            }
            float xw2 = rx * m2 * (float) Math.sqrt(1.0 - fyOuter * fyOuter);
            int l2 = (int) (cx - xw2);
            int r2 = (int) (cx + xw2);
            if (l2 > 0) ctx.fill(0, y, l2, y + step, solid);
            if (r2 < w) ctx.fill(r2, y, w, y + step, solid);

            float fyMid = ry > 0 ? dy / (ry * m1) : 2f;
            if (Math.abs(fyMid) >= 1f) {
                ctx.fill(l2, y, r2, y + step, band70);
                continue;
            }
            float xw1 = rx * m1 * (float) Math.sqrt(1.0 - fyMid * fyMid);
            int l1 = (int) (cx - xw1);
            int r1 = (int) (cx + xw1);
            if (l1 > l2) ctx.fill(l2, y, l1, y + step, band70);
            if (r2 > r1) ctx.fill(r1, y, r2, y + step, band70);

            float fyInner = ry > 0 ? dy / ry : 2f;
            if (Math.abs(fyInner) >= 1f) {
                ctx.fill(l1, y, r1, y + step, band35);
                continue;
            }
            float xw0 = rx * (float) Math.sqrt(1.0 - fyInner * fyInner);
            int l0 = (int) (cx - xw0);
            int r0 = (int) (cx + xw0);
            if (l0 > l1) ctx.fill(l1, y, l0, y + step, band35);
            if (r1 > r0) ctx.fill(r0, y, r1, y + step, band35);
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
