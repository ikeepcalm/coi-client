package dev.ua.ikeepcalm.coi.client.effect.visual.impact;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.effect.visual.impact.ImpactGeometry.Shard;
import dev.ua.ikeepcalm.coi.client.effect.visual.impact.ImpactGeometry.Spark;
import dev.ua.ikeepcalm.coi.client.effect.visual.impact.ImpactGeometry.Spike;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/**
 * One impact playing out at a world position: the {@link ImpactStyle} preset
 * says which layers compose it, {@link ImpactGeometry} holds the shapes it was
 * seeded with, and every layer is drawn through {@link ImpactRenderer} against
 * a single normalised progress {@code p}.
 */
public final class WorldImpact {

    private static final long MIN_WORLD_DURATION = 450;

    private final Vec3 position;
    private final int color;
    private final int accent;
    private final float intensity;
    private final float radius;
    private final long duration;
    private final long startTime = System.currentTimeMillis();
    private final ImpactStyle style;
    private final ImpactGeometry geometry;

    public WorldImpact(Vec3 position, int color, int accent, float intensity, float radius, long duration,
                       String style, long seed) {
        this.position = position;
        this.color = color;
        this.accent = accent;
        this.intensity = intensity;
        this.radius = radius;
        this.duration = Math.max(MIN_WORLD_DURATION, duration);

        this.style = ImpactStyle.of(style);
        this.geometry = new ImpactGeometry(this.style, seed, intensity, radius);
    }

    public Vec3 position() {
        return position;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration;
    }

    public void render(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal) {
        float p = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) duration);
        float energy = 0.55f + 0.45f * intensity;

        renderFlash(pose, consumer, p, energy);
        renderGroundRings(pose, consumer, p, energy);
        renderCameraRings(pose, consumer, p, energy);
        renderSpikes(pose, consumer, p, energy);
        renderLances(pose, consumer, p, energy);
        renderSlashes(pose, consumer, p, energy);
        renderPillar(pose, consumer, p, energy);
        renderShards(pose, consumer, camLocal, p, energy);
        renderCracks(pose, consumer, p, energy);
        renderSparks(pose, consumer, camLocal, p, energy);
    }

    private void renderFlash(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        if (style.flashStrength <= 0f) return;

        float alpha;
        float grow;
        if (style.delayedFlash) {
            // Void: implosion first, then the snap of light mid-way through
            float pf = EffectPaint.clamp((p - 0.35f) / 0.3f, 0f, 1f);
            alpha = (pf <= 0f || pf >= 1f) ? 0f : 1f - Math.abs(2f * pf - 1f);
            grow = EffectPaint.easeOutCubic(pf);
        } else {
            float pf = EffectPaint.clamp(p / 0.3f, 0f, 1f);
            alpha = (1f - pf) * (1f - pf);
            grow = EffectPaint.easeOutCubic(pf);
        }
        alpha *= style.flashStrength * energy;
        if (alpha <= 0.01f) return;

        if (style.flatFlash) {
            ImpactRenderer.drawGroundGlow(pose, consumer, radius * (0.5f + 1.3f * grow), 0.05f, accent, alpha * 0.8f);
            return;
        }

        ImpactRenderer.drawGlowDisc(pose, consumer, radius * (0.75f + 1.4f * grow), 0.02f, accent, alpha * 0.65f);
        ImpactRenderer.drawGlowDisc(pose, consumer, radius * (0.35f + 0.75f * grow), 0.03f, 0xFFFFFF, alpha);
        if (style.darkCore) {
            ImpactRenderer.drawGlowDisc(pose, consumer, radius * (0.3f + 0.5f * grow), 0.04f, 0x05030A, Math.min(1f, alpha * 1.2f));
        }
    }

    private void renderGroundRings(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        for (int i = 0; i < style.groundRingCount; i++) {
            float delay = 0.10f * i;
            float pr = EffectPaint.clamp((p - delay) / (1f - delay), 0f, 1f);
            if (pr <= 0f) continue;

            float alpha = (float) Math.pow(1f - pr, 1.5) * 0.8f * energy;
            if (alpha <= 0.01f) continue;

            float r = style.implode
                    ? radius * (0.15f + 2.0f * (1f - EffectPaint.easeOutCubic(pr)))
                    : radius * (0.2f + 2.0f * EffectPaint.easeOutCubic(pr));
            float thickness = radius * (0.18f - 0.08f * pr);
            ImpactRenderer.drawGroundRing(pose, consumer, r, thickness, 0.06f + 0.02f * i, i % 2 == 0 ? accent : color, alpha);
        }
    }

    private void renderCameraRings(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        for (int i = 0; i < style.cameraRingCount; i++) {
            float delay = 0.08f * i;
            float pr = EffectPaint.clamp((p - delay) / (1f - delay), 0f, 1f);
            if (pr <= 0f) continue;

            float alpha = (float) Math.pow(1f - pr, 1.5) * 0.6f * energy;
            if (alpha <= 0.01f) continue;

            float r = style.implode
                    ? radius * (0.12f + 1.7f * (1f - EffectPaint.easeOutCubic(pr)))
                    : radius * (0.15f + 1.7f * EffectPaint.easeOutCubic(pr));
            float thickness = radius * (0.14f - 0.06f * pr);
            ImpactRenderer.drawBillboardRing(pose, consumer, r, thickness, 0.05f + 0.01f * i, i % 2 == 0 ? accent : color, alpha);
        }
    }

    private void renderSpikes(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        if (geometry.spikes().isEmpty()) return;

        float grow = EffectPaint.easeOutCubic(EffectPaint.clamp(p / 0.18f, 0f, 1f));
        float alpha = (float) Math.pow(1f - EffectPaint.clamp(p / 0.6f, 0f, 1f), 1.6) * energy;
        if (alpha <= 0.01f) return;

        PoseStack.Pose bp = ImpactRenderer.billboard(pose);
        int i = 0;
        for (Spike spike : geometry.spikes()) {
            float length = radius * 1.9f * spike.lengthMul() * grow;
            float width = radius * 0.05f * (1f + 0.5f * (1f - grow));
            ImpactRenderer.drawSpike(bp, consumer, spike.angle(), radius * 0.15f, radius * 0.15f + length, width, 0.06f,
                    i++ % 2 == 0 ? 0xFFFFFF : accent, alpha);
        }
    }

    private void renderLances(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        if (!style.lances) return;

        float grow = EffectPaint.easeOutCubic(EffectPaint.clamp(p / 0.25f, 0f, 1f));
        float alpha = (float) Math.pow(1f - EffectPaint.clamp(p / 0.8f, 0f, 1f), 1.4) * energy;
        if (alpha <= 0.01f) return;

        PoseStack.Pose bp = ImpactRenderer.billboard(pose);
        float length = radius * 3.4f * grow;
        ImpactRenderer.drawSpike(bp, consumer, 0f, 0f, length, radius * 0.09f, 0.07f, accent, alpha);
        ImpactRenderer.drawSpike(bp, consumer, (float) Math.PI, 0f, length, radius * 0.09f, 0.07f, accent, alpha);
        ImpactRenderer.drawSpike(bp, consumer, 0f, 0f, length * 0.9f, radius * 0.035f, 0.08f, 0xFFFFFF, alpha);
        ImpactRenderer.drawSpike(bp, consumer, (float) Math.PI, 0f, length * 0.9f, radius * 0.035f, 0.08f, 0xFFFFFF, alpha);
    }

    private void renderSlashes(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        for (int i = 0; i < style.slashAngles.length; i++) {
            float delay = 0.05f * i;
            float grow = EffectPaint.easeOutCubic(EffectPaint.clamp((p - delay) / 0.12f, 0f, 1f));
            if (grow <= 0f) continue;

            float alpha = (float) Math.pow(1f - EffectPaint.clamp((p - delay) / 0.5f, 0f, 1f), 1.5) * energy;
            if (alpha <= 0.01f) continue;

            float angle = style.slashAngles[i];
            float length = radius * 1.8f * (0.5f + 0.5f * grow) * (i == 0 ? 1f : 0.72f);
            float width = radius * 0.14f * (1f - 0.4f * p);
            PoseStack.Pose bp = ImpactRenderer.billboard(pose);
            ImpactRenderer.drawSpike(bp, consumer, angle, 0f, length, width, 0.08f, accent, alpha);
            ImpactRenderer.drawSpike(bp, consumer, angle + (float) Math.PI, 0f, length, width, 0.08f, accent, alpha);
            ImpactRenderer.drawSpike(bp, consumer, angle, 0f, length * 0.9f, width * 0.38f, 0.09f, 0xFFFFFF, alpha);
            ImpactRenderer.drawSpike(bp, consumer, angle + (float) Math.PI, 0f, length * 0.9f, width * 0.38f, 0.09f, 0xFFFFFF, alpha);
        }
    }

    private void renderPillar(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        if (!style.pillar) return;

        float grow = EffectPaint.easeOutCubic(EffectPaint.clamp(p / 0.2f, 0f, 1f));
        float alpha = (float) Math.pow(1f - EffectPaint.clamp((p - 0.3f) / 0.7f, 0f, 1f), 1.3) * energy;
        if (alpha <= 0.01f) return;

        float height = radius * 3.4f * grow;
        float width = radius * 0.45f * (1f - 0.3f * p);
        ImpactRenderer.drawPillarPlane(pose, consumer, width, 0f, height, accent, alpha * 0.8f);
        ImpactRenderer.drawPillarPlane(pose, consumer, 0f, width, height, accent, alpha * 0.8f);
        float core = width * 0.4f * 0.7071f;
        ImpactRenderer.drawPillarPlane(pose, consumer, core, core, height * 0.9f, 0xFFFFFF, alpha);
        ImpactRenderer.drawPillarPlane(pose, consumer, core, -core, height * 0.9f, 0xFFFFFF, alpha);
        ImpactRenderer.drawGroundGlow(pose, consumer, radius * (0.8f + 0.6f * grow), 0.04f, accent, alpha * 0.6f);
    }

    private void renderShards(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal, float p, float energy) {
        if (geometry.shards().isEmpty()) return;

        float grow = EffectPaint.easeOutCubic(EffectPaint.clamp(p / 0.22f, 0f, 1f));
        float alpha = (float) Math.pow(1f - EffectPaint.clamp((p - 0.45f) / 0.55f, 0f, 1f), 1.4) * energy;
        if (alpha <= 0.01f) return;

        for (Shard shard : geometry.shards()) {
            float dx = shard.x() - (float) camLocal.x;
            float dz = shard.z() - (float) camLocal.z;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len < 0.001f) continue;
            float sx = -dz / len * shard.width();
            float sz = dx / len * shard.width();
            float tipY = shard.height() * grow;
            int shardColor = shard.accented() ? accent : color;

            ImpactRenderer.addVertex(pose, consumer, shard.x() - sx, 0f, shard.z() - sz, shardColor, alpha * 0.9f, 0, 0, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, shard.x() + sx, 0f, shard.z() + sz, shardColor, alpha * 0.9f, 1, 0, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, shard.x(), tipY, shard.z(), 0xFFFFFF, alpha * 0.4f, 1, 1, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, shard.x(), tipY, shard.z(), 0xFFFFFF, alpha * 0.4f, 0, 1, 0, 1);
        }
    }

    private void renderCracks(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
        if (geometry.cracks().isEmpty()) return;

        float grow = EffectPaint.easeOutCubic(EffectPaint.clamp(p / 0.12f, 0f, 1f));
        float alpha = (float) Math.pow(1f - EffectPaint.clamp((p - 0.35f) / 0.65f, 0f, 1f), 1.2) * 0.9f * energy;
        if (alpha <= 0.01f) return;

        float baseWidth = radius * 0.05f;
        for (float[] pts : geometry.cracks()) {
            for (int j = 0; j < 3; j++) {
                float x1 = pts[j * 2] * grow;
                float z1 = pts[j * 2 + 1] * grow;
                float x2 = pts[j * 2 + 2] * grow;
                float z2 = pts[j * 2 + 3] * grow;
                float dx = x2 - x1;
                float dz = z2 - z1;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len < 0.001f) continue;
                float width = baseWidth * (1f - j * 0.28f);
                float sx = -dz / len * width;
                float sz = dx / len * width;

                ImpactRenderer.addVertex(pose, consumer, x1 - sx, 0.04f, z1 - sz, accent, alpha, 0, 0, 1, 0);
                ImpactRenderer.addVertex(pose, consumer, x1 + sx, 0.04f, z1 + sz, accent, alpha, 1, 0, 1, 0);
                ImpactRenderer.addVertex(pose, consumer, x2 + sx, 0.04f, z2 + sz, accent, alpha * 0.7f, 1, 1, 1, 0);
                ImpactRenderer.addVertex(pose, consumer, x2 - sx, 0.04f, z2 - sz, accent, alpha * 0.7f, 0, 1, 1, 0);
            }
        }
    }

    private void renderSparks(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal, float p, float energy) {
        if (geometry.sparks().isEmpty()) return;

        float t = (System.currentTimeMillis() - startTime) / 1000f;
        float durationSec = duration / 1000f;

        for (Spark spark : geometry.sparks()) {
            float life = durationSec * spark.lifeFrac();
            float sp = t / life;
            if (sp >= 1f) continue;

            float alpha = (float) Math.pow(1f - sp, 0.9) * energy;
            if (style.twinkle) {
                alpha *= 0.55f + 0.45f * (float) Math.sin(t * 30f + spark.phase());
            }
            if (alpha <= 0.01f) continue;

            float[] head;
            float[] tail;
            if (style.sparksInward) {
                head = inwardPos(spark, sp);
                tail = inwardPos(spark, Math.max(0f, sp - 0.06f));
            } else {
                head = ballisticPos(spark, t);
                tail = ballisticPos(spark, Math.max(0f, t - 0.05f));
            }

            float vx = head[0] - tail[0];
            float vy = head[1] - tail[1];
            float vz = head[2] - tail[2];
            if (vx * vx + vy * vy + vz * vz < 1.0e-6f) {
                tail[0] = head[0] - spark.dirX() * 0.05f;
                tail[1] = head[1] - spark.dirY() * 0.05f;
                tail[2] = head[2] - spark.dirZ() * 0.05f;
                vx = head[0] - tail[0];
                vy = head[1] - tail[1];
                vz = head[2] - tail[2];
            }

            float width = radius * 0.045f * spark.sizeMul() * style.sparkWidthMul * (1f - 0.5f * sp);
            float[] side = ImpactRenderer.streakSide(vx, vy, vz, head, camLocal, width);
            if (side == null) continue;
            float cx = side[0];
            float cy = side[1];
            float cz = side[2];

            int sparkColor = switch (spark.colorIdx()) {
                case 0 -> 0xFFFFFF;
                case 1 -> accent;
                default -> color;
            };

            ImpactRenderer.addVertex(pose, consumer, head[0] - cx, head[1] - cy, head[2] - cz, sparkColor, alpha, 0, 0, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, head[0] + cx, head[1] + cy, head[2] + cz, sparkColor, alpha, 1, 0, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, tail[0] + cx, tail[1] + cy, tail[2] + cz, sparkColor, alpha * 0.15f, 1, 1, 0, 1);
            ImpactRenderer.addVertex(pose, consumer, tail[0] - cx, tail[1] - cy, tail[2] - cz, sparkColor, alpha * 0.15f, 0, 1, 0, 1);
        }
    }

    /**
     * Analytic ballistic motion with horizontal drag, so sparks stay
     * smooth at any framerate without per-tick integration.
     */
    private float[] ballisticPos(Spark spark, float t) {
        float drag = 2.6f;
        float travel = (1f - (float) Math.exp(-drag * t)) / drag;
        return new float[]{
                spark.dirX() * spark.speed() * travel,
                spark.dirY() * spark.speed() * travel - 0.5f * style.sparkGravity * t * t,
                spark.dirZ() * spark.speed() * travel
        };
    }

    private float[] inwardPos(Spark spark, float sp) {
        float dist = radius * 1.25f * (1f - EffectPaint.easeOutCubic(EffectPaint.clamp(sp, 0f, 1f)));
        return new float[]{spark.dirX() * dist, spark.dirY() * dist, spark.dirZ() * dist};
    }
}
