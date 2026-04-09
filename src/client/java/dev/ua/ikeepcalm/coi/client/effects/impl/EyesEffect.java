package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class EyesEffect implements VisualEffect {

    public static final String ID = "eyes";

    private static final long STAGGER = 500L;   // ms between each eye spawning
    private static final long OPEN_DURATION = 900L;   // eye1 → eye4

    private static final long[] CLOSE_FRAME_MS = {200L, 250L, 1000L, 200L, 150L};
    private static final long CLOSE_DURATION; // sum of CLOSE_FRAME_MS

    static {
        long sum = 0;
        for (long t : CLOSE_FRAME_MS) sum += t;
        CLOSE_DURATION = sum;
    }

    private static final long FADE_MS = 300L;
    private static final long PUPIL_FRAME_MS = 300L;   // ms per pupil-shift frame (eye4.1–4.4)
    private static final long PUPIL_CYCLE_MS = PUPIL_FRAME_MS * 4; // full loop = 1200ms

    // Texture arrays
    private static final Identifier[] OPEN_FRAMES = new Identifier[4]; // eye1..eye4
    private static final Identifier[] PUPIL_FRAMES = new Identifier[4]; // eye4.1..eye4.4
    private static final Identifier[] CLOSE_FRAMES = new Identifier[5]; // eye5..eye9

    static {
        for (int i = 0; i < 4; i++)
            OPEN_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye" + (i + 1) + ".png");
        for (int i = 0; i < 4; i++)
            PUPIL_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye4." + (i + 1) + ".png");
        for (int i = 0; i < 5; i++)
            CLOSE_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye" + (i + 5) + ".png");
    }

    private int count = 2;
    // Minimum time (ms) the LAST eye stares before all eyes start closing.
    // Must be >= PUPIL_CYCLE_MS to guarantee at least one full pupil loop.
    private long stareMs = PUPIL_CYCLE_MS * 2;

    private long startTime;
    // Elapsed ms from startTime at which ALL eyes begin closing simultaneously.
    // Computed in spawnEyes() once screen dimensions are known.
    private long globalCloseStart;

    private final List<EyeData> eyes = new ArrayList<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Creepy Eyes";
    }

    @Override
    public String getDefaultParams() {
        return "count=2,stare=2400";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        eyes.clear();
        globalCloseStart = 0;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (eyes.isEmpty()) spawnEyes(w, h);

        long elapsed = System.currentTimeMillis() - startTime;

        for (EyeData eye : eyes) {
            long eyeElapsed = elapsed - eye.offset;
            if (eyeElapsed < 0) continue;

            Identifier frame;
            float alpha;

            if (elapsed < globalCloseStart) {
                // ── Opening or staring (pupil loop) ──────────────────────────────
                if (eyeElapsed < OPEN_DURATION) {
                    // eye1 → eye4
                    int idx = (int) (eyeElapsed * 4 / OPEN_DURATION);
                    frame = OPEN_FRAMES[Math.min(idx, 3)];
                    alpha = Math.min(1f, eyeElapsed / (float) FADE_MS);
                } else {
                    // Pupil shifting: eye4.1 → 4.4, loops until globalCloseStart
                    long stareElapsed = eyeElapsed - OPEN_DURATION;
                    int idx = (int) ((stareElapsed % PUPIL_CYCLE_MS) / PUPIL_FRAME_MS);
                    frame = PUPIL_FRAMES[idx];
                    alpha = 1f;
                }
            } else {
                // ── All eyes close simultaneously ─────────────────────────────────
                long closeElapsed = elapsed - globalCloseStart;
                if (closeElapsed > CLOSE_DURATION) continue;

                int idx = 4;
                long acc = 0;
                for (int f = 0; f < CLOSE_FRAME_MS.length; f++) {
                    acc += CLOSE_FRAME_MS[f];
                    if (closeElapsed < acc) {
                        idx = f;
                        break;
                    }
                }
                frame = CLOSE_FRAMES[idx];
                float fadeStart = CLOSE_DURATION - FADE_MS;
                alpha = closeElapsed > fadeStart
                        ? 1f - (closeElapsed - fadeStart) / (float) FADE_MS
                        : 1f;
            }

            drawEye(ctx, eye, frame, Math.max(0f, Math.min(1f, alpha)));
        }
    }

    private void drawEye(GuiGraphicsExtractor ctx, EyeData eye, Identifier texture, float alpha) {
        if (alpha <= 0) return;
        int x = eye.cx - eye.halfW;
        int y = eye.cy - eye.halfH;
        int w = eye.halfW * 2;
        int h = eye.halfH * 2;

        var matrices = ctx.pose();
        matrices.pushMatrix();
        matrices.translate((float) eye.cx, (float) eye.cy);
        matrices.rotate((float) Math.toRadians(eye.rotation));
        matrices.translate((float) -eye.cx, (float) -eye.cy);

        int argb = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h, argb);

        matrices.popMatrix();
    }

    private void spawnEyes(int w, int h) {
        Random rng = new Random(startTime);
        // Grid divides the screen so candidates are spread out before rejection sampling
        int cols = Math.min(count, 3);
        int rows = (count + cols - 1) / cols;
        int cellW = w / cols;
        int cellH = h / rows;

        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;
            int cellOriginX = col * cellW;
            int cellOriginY = row * cellH;

            int halfW = 70 + rng.nextInt(40); // 140–218 px wide
            int halfH = halfW / 2;            // 2:1 aspect ratio
            float rotation = rng.nextFloat() * 80f - 25f; // –25° to +25°

            // Safe bounds: center must be far enough from every screen edge
            // so the eye body (halfW / halfH) doesn't cross the border
            int borderPad = 20; // extra gap beyond the eye's own half-size
            int minCx = halfW + borderPad;
            int maxCx = w - halfW - borderPad;
            int minCy = halfH + borderPad;
            int maxCy = h - halfH - borderPad;
            // Constrain to cell, then clamp to safe bounds
            int cellMinCx = Math.max(minCx, cellOriginX + halfW);
            int cellMaxCx = Math.min(maxCx, cellOriginX + cellW - halfW);
            int cellMinCy = Math.max(minCy, cellOriginY + halfH);
            int cellMaxCy = Math.min(maxCy, cellOriginY + cellH - halfH);
            // Fallback to cell centre if the cell is too small
            if (cellMinCx >= cellMaxCx) {
                cellMinCx = cellMaxCx = (cellOriginX + cellW / 2);
            }
            if (cellMinCy >= cellMaxCy) {
                cellMinCy = cellMaxCy = (cellOriginY + cellH / 2);
            }

            // Rejection sampling: try up to 20 candidates, keep the one furthest from neighbours
            int bestCx = (cellMinCx + cellMaxCx) / 2;
            int bestCy = (cellMinCy + cellMaxCy) / 2;
            float bestMinDist = -Float.MAX_VALUE;
            for (int attempt = 0; attempt < 20; attempt++) {
                int cx = cellMinCx + rng.nextInt(Math.max(1, cellMaxCx - cellMinCx));
                int cy = cellMinCy + rng.nextInt(Math.max(1, cellMaxCy - cellMinCy));
                float minDist = Float.MAX_VALUE;
                for (EyeData placed : eyes) {
                    float dx = cx - placed.cx;
                    float dy = cy - placed.cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float required = halfW + placed.halfW + 20f;
                    minDist = Math.min(minDist, dist - required);
                }
                if (eyes.isEmpty()) {
                    bestCx = cx;
                    bestCy = cy;
                    break;
                }
                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    bestCx = cx;
                    bestCy = cy;
                    if (minDist >= 0) break; // non-overlapping — good enough
                }
            }

            eyes.add(new EyeData(bestCx, bestCy, halfW, halfH, rotation, (long) i * STAGGER));
        }

        // All eyes close together after the LAST eye finishes opening + stareMs
        globalCloseStart = (long) (count - 1) * STAGGER + OPEN_DURATION + stareMs;
    }

    @Override
    public boolean isFinished() {
        if (eyes.isEmpty()) return false;
        return System.currentTimeMillis() - startTime > globalCloseStart + CLOSE_DURATION + FADE_MS;
    }

    @Override
    public void stop() {
        eyes.clear();
    }

    private void parseParams(String params) {
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "count" -> count = Integer.parseInt(kv[1].trim());
                case "stare" -> stareMs = Long.parseLong(kv[1].trim());
            }
        }
    }

    private record EyeData(int cx, int cy, int halfW, int halfH, float rotation, long offset) {
    }
}
