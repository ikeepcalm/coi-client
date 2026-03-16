package dev.ua.ikeepcalm.coi.client.effects.impl;

import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private float intensity = 0.7f;
    private long duration = -1;
    private long startTime;
    private String[] textPool = DEFAULT_POOL;

    private final List<Whisper> active = new ArrayList<>();
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
    public void render(DrawContext ctx, int w, int h, float tickDelta) {
        long elapsed = System.currentTimeMillis() - startTime;
        var font = MinecraftClient.getInstance().textRenderer;

        // Spawn new whispers on a schedule driven by intensity
        if (elapsed >= nextSpawn) {
            spawnWhisper(w, h, elapsed);
            long interval = (long) (2500 - 1800 * intensity);  // 700ms – 2500ms between spawns
            nextSpawn = elapsed + interval;
        }

        active.removeIf(wh -> elapsed > wh.spawnTime + wh.lifetime);

        for (Whisper wh : active) {
            long age = elapsed - wh.spawnTime;
            float a = computeAlpha(age, wh.lifetime);
            if (a <= 0) continue;

            // Encode alpha into the color — drawTextWithShadow respects ARGB
            int alpha = (int) (200 * a * intensity);
            int color = (alpha << 24) | 0xCCCCCC;

            ctx.drawTextWithShadow(font, wh.text, wh.x, wh.y, color);
        }
    }

    private float computeAlpha(long age, long lifetime) {
        long fadeIn = 600;
        long fadeOut = 700;
        if (age < fadeIn) return age / (float) fadeIn;
        if (age > lifetime - fadeOut) return Math.max(0f, (lifetime - age) / (float) fadeOut);
        return 1f;
    }

    private void spawnWhisper(int w, int h, long elapsed) {
        Random rng = new Random(elapsed ^ startTime);
        String text = textPool[rng.nextInt(textPool.length)];

        var font = MinecraftClient.getInstance().textRenderer;
        int textW = font.getWidth(text);

        // Prefer screen edges and mid-periphery; avoid exact center
        int margin = 20;
        int x, y;
        int attempt = 0;
        do {
            x = margin + rng.nextInt(Math.max(1, w - margin * 2 - textW));
            y = margin + rng.nextInt(Math.max(1, h - margin * 2 - 10));
            attempt++;
        } while (isTooClose(x, y) && attempt < 8);

        long lifetime = 2000 + (long) (rng.nextFloat() * 2000);
        active.add(new Whisper(text, x, y, elapsed, lifetime));
    }

    private boolean isTooClose(int x, int y) {
        for (Whisper wh : active) {
            if (Math.abs(wh.x - x) < 60 && Math.abs(wh.y - y) < 20) return true;
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
        if (params == null || params.isBlank()) return;
        for (String part : params.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) switch (kv[0].trim()) {
                case "intensity" -> intensity = Float.parseFloat(kv[1].trim());
                case "duration" -> duration = Long.parseLong(kv[1].trim());
                case "text" -> textPool = kv[1].trim().split("\\|");
            }
        }
    }

    private record Whisper(String text, int x, int y, long spawnTime, long lifetime) {
    }
}
