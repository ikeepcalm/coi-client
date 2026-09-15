package dev.ua.ikeepcalm.coi.client.effect.visual.impact;

/**
 * The style presets: each one composes the same primitives differently.
 * <p>
 * One instance carries everything a preset decides — the flags {@link WorldImpact}
 * renders from, and the counts and spark ballistics the {@link ImpactGeometry}
 * generators need — so adding a style is a one-place edit.
 */
final class ImpactStyle {

    float flashStrength;
    boolean delayedFlash;
    boolean flatFlash;
    boolean darkCore;
    int groundRingCount;
    int cameraRingCount;
    boolean implode;
    boolean lances;
    boolean pillar;
    boolean twinkle;
    float sparkGravity;
    float sparkWidthMul = 1f;
    boolean sparksInward;
    float[] slashAngles = new float[0];

    /**
     * The per-style numbers that only the generators in {@link ImpactGeometry}
     * care about, kept apart from the fields the impact renders itself from.
     */
    int spikeCount = 0;
    int sparkCount = 0;
    int shardCount = 0;
    int crackCount = 0;
    boolean jaggedSpikes = false;
    float sparkSpeed = 0f;
    float sparkUpBias = 0f;

    private ImpactStyle() {
    }

    /**
     * Resolves a style name; an unknown name composes the {@code burst} default.
     */
    static ImpactStyle of(String style) {
        ImpactStyle s = new ImpactStyle();

        switch (style) {
            case "slash" -> {
                s.flashStrength = 0.45f;
                s.slashAngles = new float[]{-0.42f, 0.26f};
                s.sparkCount = 16;
                s.sparkSpeed = 8f;
                s.sparkGravity = 9f;
                s.sparkUpBias = 0.25f;
            }
            case "void" -> {
                s.flashStrength = 0.8f;
                s.delayedFlash = true;
                s.darkCore = true;
                s.implode = true;
                s.groundRingCount = 1;
                s.cameraRingCount = 2;
                s.sparkCount = 24;
                s.sparksInward = true;
            }
            case "holy" -> {
                s.flashStrength = 0.9f;
                s.pillar = true;
                s.groundRingCount = 1;
                s.spikeCount = 12;
                s.sparkCount = 22;
                s.sparkSpeed = 2.6f;
                s.sparkGravity = -3.5f;
                s.sparkUpBias = 0.85f;
            }
            case "pierce" -> {
                s.flashStrength = 0.7f;
                s.lances = true;
                s.cameraRingCount = 3;
                s.sparkCount = 12;
                s.sparkSpeed = 9f;
                s.sparkGravity = 6f;
                s.sparkUpBias = 0.2f;
            }
            case "crush" -> {
                s.flashStrength = 0.8f;
                s.flatFlash = true;
                s.groundRingCount = 2;
                s.crackCount = 7;
                s.sparkCount = 26;
                s.sparkSpeed = 5f;
                s.sparkGravity = 18f;
                s.sparkUpBias = 0.7f;
            }
            case "ripple" -> {
                s.flashStrength = 0.35f;
                s.groundRingCount = 3;
                s.cameraRingCount = 2;
                s.sparkCount = 8;
                s.sparkSpeed = 2f;
                s.sparkGravity = 0.5f;
                s.sparkUpBias = 0.3f;
            }
            case "fracture" -> {
                s.flashStrength = 0.8f;
                s.spikeCount = 14;
                s.jaggedSpikes = true;
                s.crackCount = 8;
                s.sparkCount = 20;
                s.sparkSpeed = 7f;
                s.sparkGravity = 10f;
                s.sparkUpBias = 0.45f;
                s.sparkWidthMul = 1.8f;
            }
            case "blood" -> {
                s.flashStrength = 0.55f;
                s.groundRingCount = 1;
                s.sparkCount = 34;
                s.sparkSpeed = 6.5f;
                s.sparkGravity = 16f;
                s.sparkUpBias = 0.55f;
                s.sparkWidthMul = 1.4f;
            }
            case "frost" -> {
                s.flashStrength = 0.5f;
                s.shardCount = 9;
                s.groundRingCount = 1;
                s.twinkle = true;
                s.sparkCount = 18;
                s.sparkSpeed = 1.8f;
                s.sparkGravity = 1.2f;
                s.sparkUpBias = 0.6f;
            }
            default -> {
                s.flashStrength = 1f;
                s.groundRingCount = 2;
                s.cameraRingCount = 1;
                s.spikeCount = 10;
                s.sparkCount = 30;
                s.sparkSpeed = 7f;
                s.sparkGravity = 11f;
                s.sparkUpBias = 0.45f;
            }
        }

        return s;
    }
}
