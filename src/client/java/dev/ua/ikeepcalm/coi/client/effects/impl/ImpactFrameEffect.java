package dev.ua.ikeepcalm.coi.client.effects.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MMORPG-style world-space spell impact VFX: a soft core flash, expanding
 * shockwave rings, radial light spikes and physical spark streaks composed
 * per style. Rendered as emissive translucent geometry at the impact point;
 * soft edges come from per-vertex alpha gradients (no textures needed).
 * <p>
 * The optional screen component (scope=screen/both) is a single subtle
 * accent-colored edge pulse — no full-screen frame flashing.
 */
public class ImpactFrameEffect implements VisualEffect {

    public static final String ID = "impact";
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final long MIN_WORLD_DURATION = 450;

    private static final List<WorldImpact> ACTIVE_IMPACTS = new ArrayList<>();
    private static boolean initialized = false;

    private ImpactParams activeParams;
    private long screenStartTime;

    public static void initializeWorldRenderer() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ACTIVE_IMPACTS.removeIf(WorldImpact::isFinished));

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (ACTIVE_IMPACTS.isEmpty()) return;

            PoseStack poseStack = context.poseStack();
            Vec3 cameraPos = context.levelState().cameraRenderState.pos;
            SubmitNodeCollector collector = context.submitNodeCollector();

            for (WorldImpact impact : new ArrayList<>(ACTIVE_IMPACTS)) {
                Vec3 camLocal = cameraPos.subtract(impact.position);
                poseStack.pushPose();
                poseStack.translate(
                        impact.position.x - cameraPos.x,
                        impact.position.y - cameraPos.y,
                        impact.position.z - cameraPos.z
                );
                collector.order(900).submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE),
                        (pose, consumer) -> impact.render(pose, consumer, camLocal)
                );
                poseStack.popPose();
            }
        });
    }

    public static void clearWorldImpacts() {
        ACTIVE_IMPACTS.clear();
    }

    /**
     * Camera-facing disc with a radial alpha gradient (bright center, transparent edge).
     */
    private static void drawGlowDisc(PoseStack.Pose pose, VertexConsumer consumer, float radius, float z, int color, float alpha) {
        PoseStack.Pose bp = billboard(pose);
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            addVertex(bp, consumer, 0, 0, z, color, alpha, 0.5f, 0.5f, 0, 0, 1);
            addVertex(bp, consumer, 0, 0, z, color, alpha, 0.5f, 0.5f, 0, 0, 1);
            addVertex(bp, consumer, (float) Math.cos(a2) * radius, (float) Math.sin(a2) * radius, z, color, 0f, 1, 1, 0, 0, 1);
            addVertex(bp, consumer, (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius, z, color, 0f, 0, 1, 0, 0, 1);
        }
    }

    /**
     * Flat disc on the ground plane with a radial alpha gradient.
     */
    private static void drawGroundGlow(PoseStack.Pose pose, VertexConsumer consumer, float radius, float y, int color, float alpha) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            addVertex(pose, consumer, 0, y, 0, color, alpha, 0.5f, 0.5f, 0, 1, 0);
            addVertex(pose, consumer, 0, y, 0, color, alpha, 0.5f, 0.5f, 0, 1, 0);
            addVertex(pose, consumer, (float) Math.cos(a2) * radius, y, (float) Math.sin(a2) * radius, color, 0f, 1, 1, 0, 1, 0);
            addVertex(pose, consumer, (float) Math.cos(a1) * radius, y, (float) Math.sin(a1) * radius, color, 0f, 0, 1, 0, 1, 0);
        }
    }

    /**
     * Horizontal shockwave ring with soft inner and outer edges.
     */
    private static void drawGroundRing(PoseStack.Pose pose, VertexConsumer consumer, float radius, float thickness, float y, int color, float alpha) {
        int segments = 48;
        float rIn = Math.max(0.01f, radius - thickness);
        float rOut = radius + thickness;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);

            addVertex(pose, consumer, c1 * rIn, y, s1 * rIn, color, 0f, 0, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * rIn, y, s2 * rIn, color, 0f, 1, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * radius, y, s2 * radius, color, alpha, 1, 1, 0, 1, 0);
            addVertex(pose, consumer, c1 * radius, y, s1 * radius, color, alpha, 0, 1, 0, 1, 0);

            addVertex(pose, consumer, c1 * radius, y, s1 * radius, color, alpha, 0, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * radius, y, s2 * radius, color, alpha, 1, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * rOut, y, s2 * rOut, color, 0f, 1, 1, 0, 1, 0);
            addVertex(pose, consumer, c1 * rOut, y, s1 * rOut, color, 0f, 0, 1, 0, 1, 0);
        }
    }

    /**
     * Camera-facing shockwave ring with soft inner and outer edges.
     */
    private static void drawBillboardRing(PoseStack.Pose pose, VertexConsumer consumer, float radius, float thickness, float z, int color, float alpha) {
        PoseStack.Pose bp = billboard(pose);
        int segments = 48;
        float rIn = Math.max(0.01f, radius - thickness);
        float rOut = radius + thickness;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);

            addVertex(bp, consumer, c1 * rIn, s1 * rIn, z, color, 0f, 0, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * rIn, s2 * rIn, z, color, 0f, 1, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * radius, s2 * radius, z, color, alpha, 1, 1, 0, 0, 1);
            addVertex(bp, consumer, c1 * radius, s1 * radius, z, color, alpha, 0, 1, 0, 0, 1);

            addVertex(bp, consumer, c1 * radius, s1 * radius, z, color, alpha, 0, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * radius, s2 * radius, z, color, alpha, 1, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * rOut, s2 * rOut, z, color, 0f, 1, 1, 0, 0, 1);
            addVertex(bp, consumer, c1 * rOut, s1 * rOut, z, color, 0f, 0, 1, 0, 0, 1);
        }
    }

    /**
     * Tapered ray in the billboard plane: full width/alpha at the base, a point at the tip.
     */
    private static void drawSpike(PoseStack.Pose bp, VertexConsumer consumer, float angle, float r0, float r1, float width, float z, int color, float alpha) {
        float dx = (float) Math.cos(angle);
        float dy = (float) Math.sin(angle);
        float sx = -dy * width;
        float sy = dx * width;

        addVertex(bp, consumer, dx * r0 - sx, dy * r0 - sy, z, color, alpha, 0, 0, 0, 0, 1);
        addVertex(bp, consumer, dx * r0 + sx, dy * r0 + sy, z, color, alpha, 1, 0, 0, 0, 1);
        addVertex(bp, consumer, dx * r1, dy * r1, z, color, 0f, 1, 1, 0, 0, 1);
        addVertex(bp, consumer, dx * r1, dy * r1, z, color, 0f, 0, 1, 0, 0, 1);
    }

    /**
     * Vertical light plane spanning (-dx,-dz)→(dx,dz), bright at the base and fading to the top.
     */
    private static void drawPillarPlane(PoseStack.Pose pose, VertexConsumer consumer, float dx, float dz, float height, int color, float alpha) {
        addVertex(pose, consumer, -dx, 0f, -dz, color, alpha, 0, 0, 0, 0, 1);
        addVertex(pose, consumer, dx, 0f, dz, color, alpha, 1, 0, 0, 0, 1);
        addVertex(pose, consumer, dx, height, dz, color, 0f, 1, 1, 0, 0, 1);
        addVertex(pose, consumer, -dx, height, -dz, color, 0f, 0, 1, 0, 0, 1);
    }

    private static PoseStack.Pose billboard(PoseStack.Pose pose) {
        PoseStack stack = new PoseStack();
        stack.mulPose(pose.pose());
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null) {
            stack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        }
        return stack.last();
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int color, float alpha, float u, float v, float nx, float ny, float nz) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * clamp(alpha, 0f, 1f));
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // ---- world-space drawing primitives -------------------------------------

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Impact Frame";
    }

    @Override
    public String getDefaultParams() {
        return "style=burst,scope=world,color=FFFFFF,accent=FF7A22,intensity=0.85,radius=2.0,duration=900";
    }

    @Override
    public void start(String params) {
        ImpactParams parsed = ImpactParams.parse(params);
        if (parsed.world) {
            parsed.resolveMissingPosition();
            if (parsed.position != null) {
                ACTIVE_IMPACTS.add(new WorldImpact(parsed));
            } else {
                System.out.println("COI Effects: Impact frame has no world position; rendering screen scope only");
            }
        }

        activeParams = parsed.screen ? parsed : null;
        screenStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (activeParams == null) return;

        long elapsed = System.currentTimeMillis() - screenStartTime;
        long pulseDuration = Math.min(activeParams.duration, 450);
        if (elapsed > pulseDuration) return;

        float fade = 1f - elapsed / (float) pulseDuration;
        fade *= fade;

        int accent = activeParams.accent;
        int maxA = (int) (110 * activeParams.intensity * fade);
        int vigH = (int) (h * 0.30f);
        ctx.fillGradient(0, 0, w, vigH, argb(accent, maxA), argb(accent, 0));
        ctx.fillGradient(0, h - vigH, w, h, argb(accent, 0), argb(accent, maxA));

        // Left / right — banded strips (no horizontal gradient available)
        int bands = 10;
        int stepW = Math.max(1, (int) (w * 0.22f) / bands);
        for (int i = 0; i < bands; i++) {
            float t = (bands - i) / (float) bands;
            int a = (int) (maxA * t * t);
            ctx.fill(i * stepW, 0, i * stepW + stepW, h, argb(accent, a));
            ctx.fill(w - i * stepW - stepW, 0, w - i * stepW, h, argb(accent, a));
        }
    }

    @Override
    public boolean isFinished() {
        return activeParams == null || System.currentTimeMillis() - screenStartTime > activeParams.duration;
    }

    @Override
    public void stop() {
        activeParams = null;
        clearWorldImpacts();
    }

    private record Spike(float angle, float lengthMul) {
    }

    private record Spark(float dirX, float dirY, float dirZ, float speed, float sizeMul, float lifeFrac, int colorIdx,
                         float phase) {
    }

    private record Shard(float x, float z, float height, float width, boolean accented) {
    }

    private static class WorldImpact {
        private final Vec3 position;
        private final int color;
        private final int accent;
        private final float intensity;
        private final float radius;
        private final long duration;
        private final long startTime = System.currentTimeMillis();
        private final List<Spike> spikes = new ArrayList<>();
        private final List<Spark> sparks = new ArrayList<>();
        private final List<Shard> shards = new ArrayList<>();
        private final List<float[]> cracks = new ArrayList<>();
        // Style composition
        private final float flashStrength;
        private boolean delayedFlash;
        private boolean flatFlash;
        private boolean darkCore;
        private int groundRingCount;
        private int cameraRingCount;
        private boolean implode;
        private boolean lances;
        private boolean pillar;
        private boolean twinkle;
        private float sparkGravity;
        private float sparkWidthMul = 1f;
        private boolean sparksInward;
        private float[] slashAngles = new float[0];

        private WorldImpact(ImpactParams params) {
            this.position = params.position;
            this.color = params.color;
            this.accent = params.accent;
            this.intensity = params.intensity;
            this.radius = params.radius;
            this.duration = Math.max(MIN_WORLD_DURATION, params.duration);

            int spikeCount = 0;
            int sparkCount = 0;
            int shardCount = 0;
            int crackCount = 0;
            boolean jaggedSpikes = false;
            float sparkSpeed = 0f;
            float sparkUpBias = 0f;

            switch (params.style) {
                case "slash" -> {
                    flashStrength = 0.45f;
                    slashAngles = new float[]{-0.42f, 0.26f};
                    sparkCount = 16;
                    sparkSpeed = 8f;
                    sparkGravity = 9f;
                    sparkUpBias = 0.25f;
                }
                case "void" -> {
                    flashStrength = 0.8f;
                    delayedFlash = true;
                    darkCore = true;
                    implode = true;
                    groundRingCount = 1;
                    cameraRingCount = 2;
                    sparkCount = 24;
                    sparksInward = true;
                }
                case "holy" -> {
                    flashStrength = 0.9f;
                    pillar = true;
                    groundRingCount = 1;
                    spikeCount = 12;
                    sparkCount = 22;
                    sparkSpeed = 2.6f;
                    sparkGravity = -3.5f;
                    sparkUpBias = 0.85f;
                }
                case "pierce" -> {
                    flashStrength = 0.7f;
                    lances = true;
                    cameraRingCount = 3;
                    sparkCount = 12;
                    sparkSpeed = 9f;
                    sparkGravity = 6f;
                    sparkUpBias = 0.2f;
                }
                case "crush" -> {
                    flashStrength = 0.8f;
                    flatFlash = true;
                    groundRingCount = 2;
                    crackCount = 7;
                    sparkCount = 26;
                    sparkSpeed = 5f;
                    sparkGravity = 18f;
                    sparkUpBias = 0.7f;
                }
                case "ripple" -> {
                    flashStrength = 0.35f;
                    groundRingCount = 3;
                    cameraRingCount = 2;
                    sparkCount = 8;
                    sparkSpeed = 2f;
                    sparkGravity = 0.5f;
                    sparkUpBias = 0.3f;
                }
                case "fracture" -> {
                    flashStrength = 0.8f;
                    spikeCount = 14;
                    jaggedSpikes = true;
                    crackCount = 8;
                    sparkCount = 20;
                    sparkSpeed = 7f;
                    sparkGravity = 10f;
                    sparkUpBias = 0.45f;
                    sparkWidthMul = 1.8f;
                }
                case "blood" -> {
                    flashStrength = 0.55f;
                    groundRingCount = 1;
                    sparkCount = 34;
                    sparkSpeed = 6.5f;
                    sparkGravity = 16f;
                    sparkUpBias = 0.55f;
                    sparkWidthMul = 1.4f;
                }
                case "frost" -> {
                    flashStrength = 0.5f;
                    shardCount = 9;
                    groundRingCount = 1;
                    twinkle = true;
                    sparkCount = 18;
                    sparkSpeed = 1.8f;
                    sparkGravity = 1.2f;
                    sparkUpBias = 0.6f;
                }
                default -> {
                    flashStrength = 1f;
                    groundRingCount = 2;
                    cameraRingCount = 1;
                    spikeCount = 10;
                    sparkCount = 30;
                    sparkSpeed = 7f;
                    sparkGravity = 11f;
                    sparkUpBias = 0.45f;
                }
            }

            Random rng = new Random(params.seed);

            for (int i = 0; i < spikeCount; i++) {
                float angle = (float) (Math.PI * 2 * i / spikeCount + rng.nextDouble() * 0.3);
                float lengthMul = jaggedSpikes ? 0.45f + rng.nextFloat() * 0.75f : 0.85f + rng.nextFloat() * 0.3f;
                spikes.add(new Spike(angle, lengthMul));
            }

            sparkCount = Math.round(sparkCount * (0.4f + 0.6f * intensity));
            for (int i = 0; i < sparkCount; i++) {
                float yaw = (float) (rng.nextDouble() * Math.PI * 2);
                float up = clamp(sparkUpBias + (rng.nextFloat() - 0.5f) * 0.7f, -1f, 1f);
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

        private boolean isFinished() {
            return System.currentTimeMillis() - startTime > duration;
        }

        private void render(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal) {
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
            if (flashStrength <= 0f) return;

            float alpha;
            float grow;
            if (delayedFlash) {
                // Void: implosion first, then the snap of light mid-way through
                float pf = clamp((p - 0.35f) / 0.3f, 0f, 1f);
                alpha = (pf <= 0f || pf >= 1f) ? 0f : 1f - Math.abs(2f * pf - 1f);
                grow = easeOutCubic(pf);
            } else {
                float pf = clamp(p / 0.3f, 0f, 1f);
                alpha = (1f - pf) * (1f - pf);
                grow = easeOutCubic(pf);
            }
            alpha *= flashStrength * energy;
            if (alpha <= 0.01f) return;

            if (flatFlash) {
                drawGroundGlow(pose, consumer, radius * (0.5f + 1.3f * grow), 0.05f, accent, alpha * 0.8f);
                return;
            }

            drawGlowDisc(pose, consumer, radius * (0.75f + 1.4f * grow), 0.02f, accent, alpha * 0.65f);
            drawGlowDisc(pose, consumer, radius * (0.35f + 0.75f * grow), 0.03f, 0xFFFFFF, alpha);
            if (darkCore) {
                drawGlowDisc(pose, consumer, radius * (0.3f + 0.5f * grow), 0.04f, 0x05030A, Math.min(1f, alpha * 1.2f));
            }
        }

        private void renderGroundRings(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            for (int i = 0; i < groundRingCount; i++) {
                float delay = 0.10f * i;
                float pr = clamp((p - delay) / (1f - delay), 0f, 1f);
                if (pr <= 0f) continue;

                float alpha = (float) Math.pow(1f - pr, 1.5) * 0.8f * energy;
                if (alpha <= 0.01f) continue;

                float r = implode
                        ? radius * (0.15f + 2.0f * (1f - easeOutCubic(pr)))
                        : radius * (0.2f + 2.0f * easeOutCubic(pr));
                float thickness = radius * (0.18f - 0.08f * pr);
                drawGroundRing(pose, consumer, r, thickness, 0.06f + 0.02f * i, i % 2 == 0 ? accent : color, alpha);
            }
        }

        private void renderCameraRings(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            for (int i = 0; i < cameraRingCount; i++) {
                float delay = 0.08f * i;
                float pr = clamp((p - delay) / (1f - delay), 0f, 1f);
                if (pr <= 0f) continue;

                float alpha = (float) Math.pow(1f - pr, 1.5) * 0.6f * energy;
                if (alpha <= 0.01f) continue;

                float r = implode
                        ? radius * (0.12f + 1.7f * (1f - easeOutCubic(pr)))
                        : radius * (0.15f + 1.7f * easeOutCubic(pr));
                float thickness = radius * (0.14f - 0.06f * pr);
                drawBillboardRing(pose, consumer, r, thickness, 0.05f + 0.01f * i, i % 2 == 0 ? accent : color, alpha);
            }
        }

        private void renderSpikes(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            if (spikes.isEmpty()) return;

            float grow = easeOutCubic(clamp(p / 0.18f, 0f, 1f));
            float alpha = (float) Math.pow(1f - clamp(p / 0.6f, 0f, 1f), 1.6) * energy;
            if (alpha <= 0.01f) return;

            PoseStack.Pose bp = billboard(pose);
            int i = 0;
            for (Spike spike : spikes) {
                float length = radius * 1.9f * spike.lengthMul() * grow;
                float width = radius * 0.05f * (1f + 0.5f * (1f - grow));
                drawSpike(bp, consumer, spike.angle(), radius * 0.15f, radius * 0.15f + length, width, 0.06f,
                        i++ % 2 == 0 ? 0xFFFFFF : accent, alpha);
            }
        }

        private void renderLances(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            if (!lances) return;

            float grow = easeOutCubic(clamp(p / 0.25f, 0f, 1f));
            float alpha = (float) Math.pow(1f - clamp(p / 0.8f, 0f, 1f), 1.4) * energy;
            if (alpha <= 0.01f) return;

            PoseStack.Pose bp = billboard(pose);
            float length = radius * 3.4f * grow;
            drawSpike(bp, consumer, 0f, 0f, length, radius * 0.09f, 0.07f, accent, alpha);
            drawSpike(bp, consumer, (float) Math.PI, 0f, length, radius * 0.09f, 0.07f, accent, alpha);
            drawSpike(bp, consumer, 0f, 0f, length * 0.9f, radius * 0.035f, 0.08f, 0xFFFFFF, alpha);
            drawSpike(bp, consumer, (float) Math.PI, 0f, length * 0.9f, radius * 0.035f, 0.08f, 0xFFFFFF, alpha);
        }

        private void renderSlashes(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            for (int i = 0; i < slashAngles.length; i++) {
                float delay = 0.05f * i;
                float grow = easeOutCubic(clamp((p - delay) / 0.12f, 0f, 1f));
                if (grow <= 0f) continue;

                float alpha = (float) Math.pow(1f - clamp((p - delay) / 0.5f, 0f, 1f), 1.5) * energy;
                if (alpha <= 0.01f) continue;

                float angle = slashAngles[i];
                float length = radius * 1.8f * (0.5f + 0.5f * grow) * (i == 0 ? 1f : 0.72f);
                float width = radius * 0.14f * (1f - 0.4f * p);
                PoseStack.Pose bp = billboard(pose);
                drawSpike(bp, consumer, angle, 0f, length, width, 0.08f, accent, alpha);
                drawSpike(bp, consumer, angle + (float) Math.PI, 0f, length, width, 0.08f, accent, alpha);
                drawSpike(bp, consumer, angle, 0f, length * 0.9f, width * 0.38f, 0.09f, 0xFFFFFF, alpha);
                drawSpike(bp, consumer, angle + (float) Math.PI, 0f, length * 0.9f, width * 0.38f, 0.09f, 0xFFFFFF, alpha);
            }
        }

        private void renderPillar(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            if (!pillar) return;

            float grow = easeOutCubic(clamp(p / 0.2f, 0f, 1f));
            float alpha = (float) Math.pow(1f - clamp((p - 0.3f) / 0.7f, 0f, 1f), 1.3) * energy;
            if (alpha <= 0.01f) return;

            float height = radius * 3.4f * grow;
            float width = radius * 0.45f * (1f - 0.3f * p);
            drawPillarPlane(pose, consumer, width, 0f, height, accent, alpha * 0.8f);
            drawPillarPlane(pose, consumer, 0f, width, height, accent, alpha * 0.8f);
            float core = width * 0.4f * 0.7071f;
            drawPillarPlane(pose, consumer, core, core, height * 0.9f, 0xFFFFFF, alpha);
            drawPillarPlane(pose, consumer, core, -core, height * 0.9f, 0xFFFFFF, alpha);
            drawGroundGlow(pose, consumer, radius * (0.8f + 0.6f * grow), 0.04f, accent, alpha * 0.6f);
        }

        private void renderShards(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal, float p, float energy) {
            if (shards.isEmpty()) return;

            float grow = easeOutCubic(clamp(p / 0.22f, 0f, 1f));
            float alpha = (float) Math.pow(1f - clamp((p - 0.45f) / 0.55f, 0f, 1f), 1.4) * energy;
            if (alpha <= 0.01f) return;

            for (Shard shard : shards) {
                float dx = shard.x() - (float) camLocal.x;
                float dz = shard.z() - (float) camLocal.z;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len < 0.001f) continue;
                float sx = -dz / len * shard.width();
                float sz = dx / len * shard.width();
                float tipY = shard.height() * grow;
                int shardColor = shard.accented() ? accent : color;

                addVertex(pose, consumer, shard.x() - sx, 0f, shard.z() - sz, shardColor, alpha * 0.9f, 0, 0, 0, 0, 1);
                addVertex(pose, consumer, shard.x() + sx, 0f, shard.z() + sz, shardColor, alpha * 0.9f, 1, 0, 0, 0, 1);
                addVertex(pose, consumer, shard.x(), tipY, shard.z(), 0xFFFFFF, alpha * 0.4f, 1, 1, 0, 0, 1);
                addVertex(pose, consumer, shard.x(), tipY, shard.z(), 0xFFFFFF, alpha * 0.4f, 0, 1, 0, 0, 1);
            }
        }

        private void renderCracks(PoseStack.Pose pose, VertexConsumer consumer, float p, float energy) {
            if (cracks.isEmpty()) return;

            float grow = easeOutCubic(clamp(p / 0.12f, 0f, 1f));
            float alpha = (float) Math.pow(1f - clamp((p - 0.35f) / 0.65f, 0f, 1f), 1.2) * 0.9f * energy;
            if (alpha <= 0.01f) return;

            float baseWidth = radius * 0.05f;
            for (float[] pts : cracks) {
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

                    addVertex(pose, consumer, x1 - sx, 0.04f, z1 - sz, accent, alpha, 0, 0, 0, 1, 0);
                    addVertex(pose, consumer, x1 + sx, 0.04f, z1 + sz, accent, alpha, 1, 0, 0, 1, 0);
                    addVertex(pose, consumer, x2 + sx, 0.04f, z2 + sz, accent, alpha * 0.7f, 1, 1, 0, 1, 0);
                    addVertex(pose, consumer, x2 - sx, 0.04f, z2 - sz, accent, alpha * 0.7f, 0, 1, 0, 1, 0);
                }
            }
        }

        private void renderSparks(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camLocal, float p, float energy) {
            if (sparks.isEmpty()) return;

            float t = (System.currentTimeMillis() - startTime) / 1000f;
            float durationSec = duration / 1000f;

            for (Spark spark : sparks) {
                float life = durationSec * spark.lifeFrac();
                float sp = t / life;
                if (sp >= 1f) continue;

                float alpha = (float) Math.pow(1f - sp, 0.9) * energy;
                if (twinkle) {
                    alpha *= 0.55f + 0.45f * (float) Math.sin(t * 30f + spark.phase());
                }
                if (alpha <= 0.01f) continue;

                float[] head;
                float[] tail;
                if (sparksInward) {
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

                // Side vector = streak direction × view direction, so the quad always has thickness on screen
                float wx = head[0] - (float) camLocal.x;
                float wy = head[1] - (float) camLocal.y;
                float wz = head[2] - (float) camLocal.z;
                float cx = vy * wz - vz * wy;
                float cy = vz * wx - vx * wz;
                float cz = vx * wy - vy * wx;
                float cLen = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
                if (cLen < 1.0e-5f) {
                    cx = -vz;
                    cy = 0f;
                    cz = vx;
                    cLen = (float) Math.sqrt(cx * cx + cz * cz);
                    if (cLen < 1.0e-5f) continue;
                }
                float width = radius * 0.045f * spark.sizeMul() * sparkWidthMul * (1f - 0.5f * sp);
                cx = cx / cLen * width;
                cy = cy / cLen * width;
                cz = cz / cLen * width;

                int sparkColor = switch (spark.colorIdx()) {
                    case 0 -> 0xFFFFFF;
                    case 1 -> accent;
                    default -> color;
                };

                addVertex(pose, consumer, head[0] - cx, head[1] - cy, head[2] - cz, sparkColor, alpha, 0, 0, 0, 0, 1);
                addVertex(pose, consumer, head[0] + cx, head[1] + cy, head[2] + cz, sparkColor, alpha, 1, 0, 0, 0, 1);
                addVertex(pose, consumer, tail[0] + cx, tail[1] + cy, tail[2] + cz, sparkColor, alpha * 0.15f, 1, 1, 0, 0, 1);
                addVertex(pose, consumer, tail[0] - cx, tail[1] - cy, tail[2] - cz, sparkColor, alpha * 0.15f, 0, 1, 0, 0, 1);
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
                    spark.dirY() * spark.speed() * travel - 0.5f * sparkGravity * t * t,
                    spark.dirZ() * spark.speed() * travel
            };
        }

        private float[] inwardPos(Spark spark, float sp) {
            float dist = radius * 1.25f * (1f - easeOutCubic(clamp(sp, 0f, 1f)));
            return new float[]{spark.dirX() * dist, spark.dirY() * dist, spark.dirZ() * dist};
        }
    }

    private static class ImpactParams {
        private Vec3 position;
        private String style = "burst";
        private boolean screen = false;
        private boolean world = true;
        private int color = 0xFFFFFF;
        private int accent = 0xFF7A22;
        private float intensity = 0.85f;
        private float radius = 2.0f;
        private long duration = 900;
        private long seed = System.nanoTime();

        private static ImpactParams parse(String params) {
            ImpactParams parsed = new ImpactParams();
            if (params == null || params.isBlank()) return parsed;

            Double x = null;
            Double y = null;
            Double z = null;

            for (String part : params.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String value = kv[1].trim();
                switch (key) {
                    case "style" -> parsed.style = value.toLowerCase();
                    case "scope" -> {
                        String scope = value.toLowerCase();
                        parsed.screen = "screen".equals(scope) || "both".equals(scope);
                        parsed.world = !"screen".equals(scope);
                    }
                    case "x" -> x = Double.parseDouble(value);
                    case "y" -> y = Double.parseDouble(value);
                    case "z" -> z = Double.parseDouble(value);
                    case "color" -> parsed.color = Integer.parseInt(value, 16);
                    case "accent" -> parsed.accent = Integer.parseInt(value, 16);
                    case "intensity" -> parsed.intensity = clamp(Float.parseFloat(value), 0f, 1f);
                    case "radius" -> parsed.radius = Math.max(0.25f, Float.parseFloat(value));
                    case "duration" -> parsed.duration = Math.max(80, Long.parseLong(value));
                    case "seed" -> parsed.seed = Long.parseLong(value);
                    // "frames" is accepted and ignored for backwards compatibility
                }
            }

            if (x != null && y != null && z != null) {
                parsed.position = new Vec3(x, y, z);
            }

            return parsed;
        }

        private void resolveMissingPosition() {
            if (position != null) return;

            Minecraft client = Minecraft.getInstance();
            if (client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
                position = client.hitResult.getLocation();
                return;
            }

            Entity camera = client.getCameraEntity();
            if (camera != null) {
                position = camera.getEyePosition().add(camera.getLookAngle().scale(4.0));
            }
        }
    }
}
