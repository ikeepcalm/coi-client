package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Eyes opening out of the darkness, staring with a shifting pupil, then closing
 * again. The screen darkens while they watch; each eye pops in slightly
 * under-sized, bobs, carries a pulsing red halo and trembles just before it
 * shuts. Eyes are spread over a grid of up to three columns.
 * <p>
 * Params: {@code count} (how many eyes), {@code stare} (ms the last eye stares
 * before they all close together).
 */
public class EyesEffect implements VisualEffect {

    public static final String ID = "eyes";

    private static final long STAGGER = 500L;   // ms between each eye spawning
    private static final long OPEN_DURATION = 900L;   // eye1 → eye4

    private static final long[] CLOSE_FRAME_MS = {200L, 250L, 1000L, 200L, 150L};
    private static final long CLOSE_DURATION; // sum of CLOSE_FRAME_MS
    private static final long FADE_MS = 300L;
    private static final long PUPIL_FRAME_MS = 300L;   // ms per pupil-shift frame (eye4.1–4.4)
    private static final long PUPIL_CYCLE_MS = PUPIL_FRAME_MS * 4; // full loop = 1200ms
    // Texture arrays
    private static final Identifier[] OPEN_FRAMES = new Identifier[4]; // eye1..eye4
    private static final Identifier[] PUPIL_FRAMES = new Identifier[4]; // eye4.1..eye4.4
    private static final Identifier[] CLOSE_FRAMES = new Identifier[5]; // eye5..eye9

    /** Ambient darkening ramps in over this long once the first eye appears. */
    private static final float AMBIENT_FADE_MS = 800f;
    /** How long before the shutdown the eyes start trembling. */
    private static final long TREMBLE_LEAD_MS = 350L;
    /** Largest grid width the eyes are spread over. */
    private static final int MAX_COLUMNS = 3;
    /** Candidate positions tried per eye when looking for a free spot. */
    private static final int PLACEMENT_ATTEMPTS = 20;
    /** Extra gap beyond the eye's own half-size, kept clear of the screen edge. */
    private static final int BORDER_PAD = 20;
    /** Extra gap kept between two eyes' bodies. */
    private static final int NEIGHBOUR_GAP = 20;

    static {
        long sum = 0;
        for (long t : CLOSE_FRAME_MS) sum += t;
        CLOSE_DURATION = sum;
    }

    static {
        for (int i = 0; i < 4; i++)
            OPEN_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye" + (i + 1) + ".png");
        for (int i = 0; i < 4; i++)
            PUPIL_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye4." + (i + 1) + ".png");
        for (int i = 0; i < 5; i++)
            CLOSE_FRAMES[i] = Identifier.fromNamespaceAndPath("coi-client", "textures/eyes/eye" + (i + 5) + ".png");
    }

    private final List<EyeData> eyes = new ArrayList<>();
    private int count = 2;
    // Minimum time (ms) the LAST eye stares before all eyes start closing.
    // Must be >= PUPIL_CYCLE_MS to guarantee at least one full pupil loop.
    private long stareMs = PUPIL_CYCLE_MS * 2;
    private long startTime;
    // Elapsed ms from startTime at which ALL eyes begin closing simultaneously.
    // Computed in spawnEyes() once screen dimensions are known.
    private long globalCloseStart;

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

        // The world darkens while the eyes are watching
        EffectPaint.vignette(ctx, w, h, 0x000000, (int) (115 * ambient(elapsed)), 0.28f, 0.32f);

        for (EyeData eye : eyes) {
            long eyeElapsed = elapsed - eye.offset;
            if (eyeElapsed < 0) continue;

            Pose pose = elapsed < globalCloseStart ? openingPose(eyeElapsed) : closingPose(elapsed);
            if (pose == null) continue;

            // Slow unsettling bob; anxious tremble right before the eyes shut
            float bobY = (float) Math.sin(elapsed * 0.0016 + eye.phase) * 2.5f;
            float jitterX = 0f;
            long untilClose = globalCloseStart - elapsed;
            if (untilClose > 0 && untilClose < TREMBLE_LEAD_MS) {
                jitterX = (float) Math.sin(elapsed * 0.11 + eye.phase * 7f) * 1.8f;
            }

            // Red halo pulses slowly, independently per eye
            float glow = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.003 + eye.phase);

            drawEye(ctx, eye, pose.frame(), EffectPaint.clamp(pose.alpha(), 0f, 1f), pose.scale(), jitterX, bobY, glow);
        }
    }

    private float ambient(long elapsed) {
        if (elapsed < globalCloseStart) return Math.min(1f, elapsed / AMBIENT_FADE_MS);
        return Math.max(0f, 1f - (elapsed - globalCloseStart) / (float) CLOSE_DURATION);
    }

    /**
     * Opening (eye1 → eye4, popping in slightly under-sized) or, past that, the
     * looping pupil shift (eye4.1 → 4.4) that runs until globalCloseStart.
     */
    private Pose openingPose(long eyeElapsed) {
        if (eyeElapsed < OPEN_DURATION) {
            int idx = (int) (eyeElapsed * 4 / OPEN_DURATION);
            return new Pose(
                    OPEN_FRAMES[Math.min(idx, 3)],
                    Math.min(1f, eyeElapsed / (float) FADE_MS),
                    0.88f + 0.12f * EffectPaint.easeOutCubic(eyeElapsed / (float) OPEN_DURATION));
        }

        long stareElapsed = eyeElapsed - OPEN_DURATION;
        int idx = (int) ((stareElapsed % PUPIL_CYCLE_MS) / PUPIL_FRAME_MS);
        return new Pose(PUPIL_FRAMES[idx], 1f, 1f);
    }

    /**
     * All eyes close simultaneously, on the frame schedule in CLOSE_FRAME_MS.
     * Returns null once the eye has finished shutting and should not be drawn.
     */
    private Pose closingPose(long elapsed) {
        long closeElapsed = elapsed - globalCloseStart;
        if (closeElapsed > CLOSE_DURATION) return null;

        int idx = 4;
        long acc = 0;
        for (int f = 0; f < CLOSE_FRAME_MS.length; f++) {
            acc += CLOSE_FRAME_MS[f];
            if (closeElapsed < acc) {
                idx = f;
                break;
            }
        }
        float fadeStart = CLOSE_DURATION - FADE_MS;
        float alpha = closeElapsed > fadeStart
                ? 1f - (closeElapsed - fadeStart) / (float) FADE_MS
                : 1f;
        return new Pose(CLOSE_FRAMES[idx], alpha, 1f - 0.05f * closeElapsed / CLOSE_DURATION);
    }

    private void drawEye(GuiGraphicsExtractor ctx, EyeData eye, Identifier texture, float alpha,
                         float scale, float offsetX, float offsetY, float glow) {
        if (alpha <= 0) return;
        int w = eye.halfW * 2;
        int h = eye.halfH * 2;

        var matrices = ctx.pose();
        matrices.pushMatrix();
        matrices.translate(eye.cx + offsetX, eye.cy + offsetY);
        matrices.rotate((float) Math.toRadians(eye.rotation));
        matrices.scale(scale, scale);

        // Soft red halo: the eye texture itself, enlarged and tinted, behind the eye
        int gw = (int) (w * 1.30f);
        int gh = (int) (h * 1.38f);
        int glowArgb = ((int) (alpha * (55 + 60 * glow)) << 24) | 0xAA1414;
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -gw / 2, -gh / 2, 0f, 0f, gw, gh, gw, gh, glowArgb);

        int argb = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, -eye.halfW, -eye.halfH, 0f, 0f, w, h, w, h, argb);

        matrices.popMatrix();
    }

    private void spawnEyes(int w, int h) {
        Random rng = new Random(startTime);
        // Grid divides the screen so candidates are spread out before rejection sampling
        int cols = Math.min(count, MAX_COLUMNS);
        int rows = (count + cols - 1) / cols;
        int cellW = w / cols;
        int cellH = h / rows;

        for (int i = 0; i < count; i++) {
            int cellOriginX = (i % cols) * cellW;
            int cellOriginY = (i / cols) * cellH;

            int halfW = 70 + rng.nextInt(40); // 140–218 px wide
            int halfH = halfW / 2;            // 2:1 aspect ratio
            float rotation = rng.nextFloat() * 80f - 25f; // –25° to +25°

            int[] centre = pickCentre(rng, w, h, halfW, halfH, cellOriginX, cellOriginY, cellW, cellH);

            eyes.add(new EyeData(centre[0], centre[1], halfW, halfH, rotation, (long) i * STAGGER,
                    (float) (rng.nextDouble() * Math.PI * 2)));
        }

        // All eyes close together after the LAST eye finishes opening + stareMs
        globalCloseStart = (long) (count - 1) * STAGGER + OPEN_DURATION + stareMs;
    }

    /**
     * Rejection sampling inside the eye's grid cell: try a handful of
     * candidates and keep the one furthest from the eyes already placed,
     * stopping early at the first that overlaps nothing.
     */
    private int[] pickCentre(Random rng, int w, int h, int halfW, int halfH,
                             int cellOriginX, int cellOriginY, int cellW, int cellH) {
        // Safe bounds: center must be far enough from every screen edge
        // so the eye body (halfW / halfH) doesn't cross the border
        int minCx = halfW + BORDER_PAD;
        int maxCx = w - halfW - BORDER_PAD;
        int minCy = halfH + BORDER_PAD;
        int maxCy = h - halfH - BORDER_PAD;
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

        int bestCx = (cellMinCx + cellMaxCx) / 2;
        int bestCy = (cellMinCy + cellMaxCy) / 2;
        float bestMinDist = -Float.MAX_VALUE;
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            int cx = cellMinCx + rng.nextInt(Math.max(1, cellMaxCx - cellMinCx));
            int cy = cellMinCy + rng.nextInt(Math.max(1, cellMaxCy - cellMinCy));
            float minDist = Float.MAX_VALUE;
            for (EyeData placed : eyes) {
                float dx = cx - placed.cx;
                float dy = cy - placed.cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float required = halfW + placed.halfW + (float) NEIGHBOUR_GAP;
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
        return new int[]{bestCx, bestCy};
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
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "count" -> count = Integer.parseInt(value);
                case "stare" -> stareMs = Long.parseLong(value);
            }
        });
    }

    private record EyeData(int cx, int cy, int halfW, int halfH, float rotation, long offset, float phase) {
    }

    /**
     * One eye's appearance this frame: which texture, how opaque, how large.
     */
    private record Pose(Identifier frame, float alpha, float scale) {
    }
}
