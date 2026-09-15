package dev.ua.ikeepcalm.coi.client.effect.visual.impact;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Every mark an impact puts on the world or the screen: the emissive
 * translucent primitives {@link WorldImpact} composes itself from — soft edges
 * come from per-vertex alpha gradients, so none of them needs a texture — plus
 * the screen-scope edge pulse.
 */
public final class ImpactRenderer {

    private static final int FULL_BRIGHT = 0x00F000F0;

    private ImpactRenderer() {
    }

    // ---- world-space drawing primitives -------------------------------------

    /**
     * Camera-facing disc with a radial alpha gradient (bright center, transparent edge).
     */
    static void drawGlowDisc(PoseStack.Pose pose, VertexConsumer consumer, float radius, float z, int color, float alpha) {
        PoseStack.Pose bp = billboard(pose);
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            addVertex(bp, consumer, 0, 0, z, color, alpha, 0.5f, 0.5f, 0, 1);
            addVertex(bp, consumer, 0, 0, z, color, alpha, 0.5f, 0.5f, 0, 1);
            addVertex(bp, consumer, (float) Math.cos(a2) * radius, (float) Math.sin(a2) * radius, z, color, 0f, 1, 1, 0, 1);
            addVertex(bp, consumer, (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius, z, color, 0f, 0, 1, 0, 1);
        }
    }

    /**
     * Flat disc on the ground plane with a radial alpha gradient.
     */
    static void drawGroundGlow(PoseStack.Pose pose, VertexConsumer consumer, float radius, float y, int color, float alpha) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            addVertex(pose, consumer, 0, y, 0, color, alpha, 0.5f, 0.5f, 1, 0);
            addVertex(pose, consumer, 0, y, 0, color, alpha, 0.5f, 0.5f, 1, 0);
            addVertex(pose, consumer, (float) Math.cos(a2) * radius, y, (float) Math.sin(a2) * radius, color, 0f, 1, 1, 1, 0);
            addVertex(pose, consumer, (float) Math.cos(a1) * radius, y, (float) Math.sin(a1) * radius, color, 0f, 0, 1, 1, 0);
        }
    }

    /**
     * Horizontal shockwave ring with soft inner and outer edges.
     */
    static void drawGroundRing(PoseStack.Pose pose, VertexConsumer consumer, float radius, float thickness, float y, int color, float alpha) {
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

            addVertex(pose, consumer, c1 * rIn, y, s1 * rIn, color, 0f, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * rIn, y, s2 * rIn, color, 0f, 1, 0, 1, 0);
            addVertex(pose, consumer, c2 * radius, y, s2 * radius, color, alpha, 1, 1, 1, 0);
            addVertex(pose, consumer, c1 * radius, y, s1 * radius, color, alpha, 0, 1, 1, 0);

            addVertex(pose, consumer, c1 * radius, y, s1 * radius, color, alpha, 0, 0, 1, 0);
            addVertex(pose, consumer, c2 * radius, y, s2 * radius, color, alpha, 1, 0, 1, 0);
            addVertex(pose, consumer, c2 * rOut, y, s2 * rOut, color, 0f, 1, 1, 1, 0);
            addVertex(pose, consumer, c1 * rOut, y, s1 * rOut, color, 0f, 0, 1, 1, 0);
        }
    }

    /**
     * Camera-facing shockwave ring with soft inner and outer edges.
     */
    static void drawBillboardRing(PoseStack.Pose pose, VertexConsumer consumer, float radius, float thickness, float z, int color, float alpha) {
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

            addVertex(bp, consumer, c1 * rIn, s1 * rIn, z, color, 0f, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * rIn, s2 * rIn, z, color, 0f, 1, 0, 0, 1);
            addVertex(bp, consumer, c2 * radius, s2 * radius, z, color, alpha, 1, 1, 0, 1);
            addVertex(bp, consumer, c1 * radius, s1 * radius, z, color, alpha, 0, 1, 0, 1);

            addVertex(bp, consumer, c1 * radius, s1 * radius, z, color, alpha, 0, 0, 0, 1);
            addVertex(bp, consumer, c2 * radius, s2 * radius, z, color, alpha, 1, 0, 0, 1);
            addVertex(bp, consumer, c2 * rOut, s2 * rOut, z, color, 0f, 1, 1, 0, 1);
            addVertex(bp, consumer, c1 * rOut, s1 * rOut, z, color, 0f, 0, 1, 0, 1);
        }
    }

    /**
     * Tapered ray in the billboard plane: full width/alpha at the base, a point at the tip.
     */
    static void drawSpike(PoseStack.Pose bp, VertexConsumer consumer, float angle, float r0, float r1, float width, float z, int color, float alpha) {
        float dx = (float) Math.cos(angle);
        float dy = (float) Math.sin(angle);
        float sx = -dy * width;
        float sy = dx * width;

        addVertex(bp, consumer, dx * r0 - sx, dy * r0 - sy, z, color, alpha, 0, 0, 0, 1);
        addVertex(bp, consumer, dx * r0 + sx, dy * r0 + sy, z, color, alpha, 1, 0, 0, 1);
        addVertex(bp, consumer, dx * r1, dy * r1, z, color, 0f, 1, 1, 0, 1);
        addVertex(bp, consumer, dx * r1, dy * r1, z, color, 0f, 0, 1, 0, 1);
    }

    /**
     * Vertical light plane spanning (-dx,-dz)→(dx,dz), bright at the base and fading to the top.
     */
    static void drawPillarPlane(PoseStack.Pose pose, VertexConsumer consumer, float dx, float dz, float height, int color, float alpha) {
        addVertex(pose, consumer, -dx, 0f, -dz, color, alpha, 0, 0, 0, 1);
        addVertex(pose, consumer, dx, 0f, dz, color, alpha, 1, 0, 0, 1);
        addVertex(pose, consumer, dx, height, dz, color, 0f, 1, 1, 0, 1);
        addVertex(pose, consumer, -dx, height, -dz, color, 0f, 0, 1, 0, 1);
    }

    static PoseStack.Pose billboard(PoseStack.Pose pose) {
        PoseStack stack = new PoseStack();
        stack.mulPose(pose.pose());
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null) {
            stack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        }
        return stack.last();
    }

    static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int color, float alpha, float u, float v, float ny, float nz) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * EffectPaint.clamp(alpha, 0f, 1f));
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, (float) 0, ny, nz);
    }

    /**
     * Side vector = streak direction × view direction, so the quad always has
     * thickness on screen, scaled to {@code width}. Falls back to a
     * horizontal perpendicular when the streak points straight at the
     * camera, and returns null when even that is degenerate.
     */
    static float[] streakSide(float vx, float vy, float vz, float[] head, Vec3 camLocal, float width) {
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
            if (cLen < 1.0e-5f) return null;
        }
        return new float[]{cx / cLen * width, cy / cLen * width, cz / cLen * width};
    }

    // ---- screen-scope edge pulse --------------------------------------------

    /**
     * The screen component: a single subtle accent-colored edge pulse — no
     * full-screen frame flashing.
     */
    public static void screenPulse(GuiGraphicsExtractor ctx, int w, int h, int accent, float intensity, float fade) {
        int maxA = (int) (110 * intensity * fade);
        int vigH = (int) (h * 0.30f);
        ctx.fillGradient(0, 0, w, vigH, EffectPaint.argb(accent, maxA), EffectPaint.argb(accent, 0));
        ctx.fillGradient(0, h - vigH, w, h, EffectPaint.argb(accent, 0), EffectPaint.argb(accent, maxA));

        renderSideBands(ctx, w, h, accent, maxA);
    }

    /**
     * Left / right — banded strips (no horizontal gradient available).
     */
    private static void renderSideBands(GuiGraphicsExtractor ctx, int w, int h, int accent, int maxA) {
        int bands = 10;
        int stepW = Math.max(1, (int) (w * 0.22f) / bands);
        for (int i = 0; i < bands; i++) {
            float t = (bands - i) / (float) bands;
            int a = (int) (maxA * t * t);
            ctx.fill(i * stepW, 0, i * stepW + stepW, h, EffectPaint.argb(accent, a));
            ctx.fill(w - i * stepW - stepW, 0, w - i * stepW, h, EffectPaint.argb(accent, a));
        }
    }
}
