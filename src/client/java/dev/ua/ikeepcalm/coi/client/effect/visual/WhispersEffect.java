package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Cryptic phrases fading in and out around the screen periphery, drifting
 * slowly upward with a ghost double-image and a candle-like flicker. Each
 * whisper takes a random size and tilt; SHOUTED phrases are tinted blood red.
 * <p>
 * Params: {@code intensity} (0-1, spawn frequency), {@code duration} (ms,
 * {@code -1} = until stopped), {@code text} (pipe-separated phrase pool that
 * replaces the built-in one).
 */
public class WhispersEffect implements VisualEffect {

    public static final String ID = "whispers";

    private static final String[] DEFAULT_POOL = {
            "they can see you",
            "don't look back",
            "it knows YOUR name",
            "you should not be here",
            "RUN",
            "the door IS open",
            "forget",
            "help me",
            "it's behind you",
            "turn around",
            "not real",
            "listen",
            "almost",
            "wrong PLACE",
            "he is watching",
            "HE IS WATCHING11!!!!!1!!",
    };

    private static final long FADE_IN_MS = 600;
    private static final long FADE_OUT_MS = 700;
    /**
     * Upper-case letters a phrase needs before it counts as SHOUTED.
     */
    private static final int SHOUT_THRESHOLD = 3;
    private static final int SHOUT_RGB = 0xC03333;
    private static final int WHISPER_RGB = 0xCCCCCC;
    /**
     * Vanilla treats near-zero text alpha as fully opaque, so skip below this.
     */
    private static final int MIN_TEXT_ALPHA = 8;

    private final List<Whisper> active = new ArrayList<>();
    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;
    private String[] textPool = DEFAULT_POOL;
    private long nextSpawn = 0;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Whispers";
    }

    @Override
    public String getDefaultParams() {
        return "intensity=0.7,duration=-1";
    }

    @Override
    public void start(String params) {
        parseParams(params);
        startTime = System.currentTimeMillis();
        active.clear();
        nextSpawn = 0;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;
        var font = Minecraft.getInstance().font;

        // Spawn new whispers on a schedule driven by intensity
        if (elapsed >= nextSpawn) {
            spawnWhisper(w, h, elapsed);
            long interval = (long) (2500 - 1800 * intensity);  // 700ms – 2500ms between spawns
            nextSpawn = elapsed + interval;
        }

        active.removeIf(wh -> elapsed > wh.spawnTime + wh.lifetime);

        for (Whisper wh : active) {
            drawWhisper(ctx, font, wh, elapsed);
        }
    }

    private void drawWhisper(GuiGraphicsExtractor ctx, Font font, Whisper wh, long elapsed) {
        long age = elapsed - wh.spawnTime;
        float a = computeAlpha(age, wh.lifetime);

        // Uneven candle-like flicker, plus rare hard dropouts
        a *= 0.8f + 0.2f * (float) Math.sin(elapsed * 0.02 + wh.phase);
        if (((elapsed / 90) + (long) (wh.phase * 100)) % 23 == 0) a *= 0.3f;

        int alpha = (int) (210 * a * intensity);
        if (alpha < MIN_TEXT_ALPHA) return; // vanilla treats near-zero text alpha as opaque

        // Whispers drift slowly, mostly upward, like escaping breath
        float fx = wh.x + wh.driftX * age / 1000f;
        float fy = wh.y + wh.driftY * age / 1000f;

        int textW = font.width(wh.text);
        var matrices = ctx.pose();
        matrices.pushMatrix();
        matrices.translate(fx, fy);
        matrices.rotate(wh.rot);
        matrices.scale(wh.scale, wh.scale);

        // Ghost double-image behind the main text
        ctx.text(font, wh.text, -textW / 2 + 2, 1, (alpha / 3 << 24) | (wh.rgb & 0xFFFFFF), false);
        ctx.text(font, wh.text, -textW / 2, 0, (alpha << 24) | (wh.rgb & 0xFFFFFF), false);

        matrices.popMatrix();
    }

    private float computeAlpha(long age, long lifetime) {
        if (age < FADE_IN_MS) return age / (float) FADE_IN_MS;
        if (age > lifetime - FADE_OUT_MS) return Math.max(0f, (lifetime - age) / (float) FADE_OUT_MS);
        return 1f;
    }

    private void spawnWhisper(int w, int h, long elapsed) {
        Random rng = new Random(elapsed ^ startTime);
        String text = textPool[rng.nextInt(textPool.length)];
        int rgb = isShouted(text) ? SHOUT_RGB : WHISPER_RGB;

        // Prefer the periphery: reject candidates landing in the center box
        int margin = 30;
        int x = w / 2, y = h / 2;
        for (int attempt = 0; attempt < 10; attempt++) {
            x = margin + rng.nextInt(Math.max(1, w - margin * 2));
            y = margin + rng.nextInt(Math.max(1, h - margin * 2));
            boolean inCenter = x > w * 0.32f && x < w * 0.68f && y > h * 0.35f && y < h * 0.65f;
            if (!inCenter && !isTooClose(x, y)) break;
        }

        long lifetime = 2400 + (long) (rng.nextFloat() * 2600);
        active.add(new Whisper(
                text, x, y, elapsed, lifetime,
                0.75f + rng.nextFloat() * 0.9f,
                (rng.nextFloat() - 0.5f) * 0.24f,
                (rng.nextFloat() - 0.5f) * 8f,
                -3f - rng.nextFloat() * 5f,
                rgb,
                (float) (rng.nextDouble() * Math.PI * 2)
        ));
    }

    /**
     * Shouted whispers read as a threat — they get tinted blood red.
     */
    private static boolean isShouted(String text) {
        int upper = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) upper++;
        }
        return upper >= SHOUT_THRESHOLD;
    }

    private boolean isTooClose(int x, int y) {
        for (Whisper wh : active) {
            if (Math.abs(wh.x - x) < 70 && Math.abs(wh.y - y) < 24) return true;
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        return duration > 0 && (System.currentTimeMillis() - startTime) > duration;
    }

    @Override
    public void stop() {
        active.clear();
    }

    private void parseParams(String params) {
        EffectParams.forEach(params, (key, value) -> {
            switch (key) {
                case "intensity" -> intensity = Float.parseFloat(value);
                case "duration" -> duration = Long.parseLong(value);
                case "text" -> textPool = value.split("\\|");
            }
        });
    }

    private record Whisper(String text, float x, float y, long spawnTime, long lifetime,
                           float scale, float rot, float driftX, float driftY, int rgb, float phase) {
    }
}
