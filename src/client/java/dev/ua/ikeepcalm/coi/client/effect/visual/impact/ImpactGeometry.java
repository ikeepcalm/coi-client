package dev.ua.ikeepcalm.coi.client.effect.visual.impact;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The randomised shapes one impact is composed of — radial spikes, spark
 * ballistics, ground shards and ground cracks — generated once when the impact
 * is created and then only read while rendering.
 */
final class ImpactGeometry {

    record Spike(float angle, float lengthMul) {
    }

    record Spark(float dirX, float dirY, float dirZ, float speed, float sizeMul, float lifeFrac, int colorIdx,
                 float phase) {
    }

    record Shard(float x, float z, float height, float width, boolean accented) {
    }

    private final List<Spike> spikes = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Shard> shards = new ArrayList<>();
    private final List<float[]> cracks = new ArrayList<>();

    ImpactGeometry(ImpactStyle style, long seed, float intensity, float radius) {
        // One shared RNG, consumed in a fixed order, so a given seed always
        // composes the same impact.
        Random rng = new Random(seed);
        buildSpikes(rng, style.spikeCount, style.jaggedSpikes);
        buildSparks(rng, style.sparkCount, style.sparkSpeed, style.sparkUpBias, intensity);
        buildShards(rng, style.shardCount, radius);
        buildCracks(rng, style.crackCount, radius);
    }

    List<Spike> spikes() {
        return spikes;
    }

    List<Spark> sparks() {
        return sparks;
    }

    List<Shard> shards() {
        return shards;
    }

    List<float[]> cracks() {
        return cracks;
    }

    private void buildSpikes(Random rng, int spikeCount, boolean jaggedSpikes) {
        for (int i = 0; i < spikeCount; i++) {
            float angle = (float) (Math.PI * 2 * i / spikeCount + rng.nextDouble() * 0.3);
            float lengthMul = jaggedSpikes ? 0.45f + rng.nextFloat() * 0.75f : 0.85f + rng.nextFloat() * 0.3f;
            spikes.add(new Spike(angle, lengthMul));
        }
    }

    private void buildSparks(Random rng, int sparkCount, float sparkSpeed, float sparkUpBias, float intensity) {
        sparkCount = Math.round(sparkCount * (0.4f + 0.6f * intensity));
        for (int i = 0; i < sparkCount; i++) {
            float yaw = (float) (rng.nextDouble() * Math.PI * 2);
            float up = EffectPaint.clamp(sparkUpBias + (rng.nextFloat() - 0.5f) * 0.7f, -1f, 1f);
            float horiz = (float) Math.sqrt(Math.max(0f, 1f - up * up));
            sparks.add(new Spark(
                    (float) Math.cos(yaw) * horiz, up, (float) Math.sin(yaw) * horiz,
                    sparkSpeed * (0.55f + rng.nextFloat() * 0.75f),
                    0.7f + rng.nextFloat() * 0.8f,
                    0.55f + rng.nextFloat() * 0.45f,
                    rng.nextInt(3),
                    (float) (rng.nextDouble() * Math.PI * 2)
            ));
        }
    }

    private void buildShards(Random rng, int shardCount, float radius) {
        for (int i = 0; i < shardCount; i++) {
            float angle = (float) (Math.PI * 2 * i / shardCount + rng.nextDouble() * 0.6);
            float dist = radius * (0.15f + rng.nextFloat() * 0.5f);
            shards.add(new Shard(
                    (float) Math.cos(angle) * dist,
                    (float) Math.sin(angle) * dist,
                    radius * (0.6f + rng.nextFloat() * 0.7f),
                    radius * (0.08f + rng.nextFloat() * 0.07f),
                    i % 2 == 0
            ));
        }
    }

    /**
     * Each crack is a 4-point polyline out from the centre, stored flat as
     * {x0, z0, x1, z1, ...} with the first point at the origin.
     */
    private void buildCracks(Random rng, int crackCount, float radius) {
        for (int i = 0; i < crackCount; i++) {
            float angle = (float) (Math.PI * 2 * i / crackCount + (rng.nextDouble() - 0.5) * 0.5);
            float totalLen = radius * (1.0f + rng.nextFloat());
            float[] pts = new float[8];
            float x = 0f, z = 0f;
            for (int j = 1; j < 4; j++) {
                angle += (rng.nextFloat() - 0.5f) * 0.7f;
                x += (float) Math.cos(angle) * totalLen / 3f;
                z += (float) Math.sin(angle) * totalLen / 3f;
                pts[j * 2] = x;
                pts[j * 2 + 1] = z;
            }
            cracks.add(pts);
        }
    }
}
