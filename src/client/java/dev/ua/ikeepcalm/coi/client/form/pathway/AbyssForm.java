package dev.ua.ikeepcalm.coi.client.form.pathway;

import dev.ua.ikeepcalm.coi.client.form.MythicalCreatureForm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Abyss — an ever-changing mass of pitch-black flesh, its blobs arranged along a humanoid
 * outline, with slavering mouths, crooked arms and writhing tentacles.
 *
 * <p>Procedural geometry, drawn once per frame against a blank texture; there is no baked model
 * and no texture atlas behind any of it.
 */
public final class AbyssForm implements MythicalCreatureForm {

    private static final float ARM_THICKNESS = 0.038f;

    @Override
    public String getPathwayName() {
        return "Abyss";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Ever-changing mass of pitch-black flesh — blobs arranged along a humanoid outline

        // 9 shifting flesh blobs distributed along the humanoid silhouette
        float[][] blobCenters = {
                {0.20f, 0.48f},  // right leg
                {-0.20f, 0.48f}, // left leg
                {0.0f, 1.18f},   // lower torso
                {0.0f, 1.68f},   // upper torso
                {0.56f, 1.45f},  // right arm
                {-0.56f, 1.45f}, // left arm
                {0.0f, 2.25f},   // head
                {0.28f, 0.88f},  // right hip
                {-0.28f, 0.88f}, // left hip
        };

        for (int i = 0; i < blobCenters.length; i++) {
            Random rand = new Random(i * 9876L);
            float scaleSpeed = 0.08f + rand.nextFloat() * 0.12f;
            float phase = rand.nextFloat() * 100f;
            float size = 0.26f + (float) Math.sin(time * scaleSpeed + phase) * 0.08f;

            float bx = blobCenters[i][0] + (float) Math.sin(time * 0.04f * scaleSpeed + phase) * 0.08f;
            float by = blobCenters[i][1] + (float) Math.sin(time * 0.05f + i) * 0.07f;
            float bz = (float) Math.cos(time * 0.04f * scaleSpeed + phase) * 0.08f;

            // Pitch-black morphing flesh
            drawCube(pose, consumer, bx, by, bz, size, 0.02f, 0.02f, 0.03f, 0.96f, light);

            // Slavering mouths on alternate blobs (opening and closing)
            if (i % 2 == 0) {
                float mouthOpen = 0.03f + (float) Math.abs(Math.sin(time * 0.15f + i)) * 0.055f;
                // Upper lip
                drawBox(pose, consumer, bx - size * 0.55f, by + mouthOpen, bz - size - 0.02f, bx + size * 0.55f, by + mouthOpen + 0.03f, bz - size + 0.01f, 0.7f, 0.1f, 0.22f, 0.92f, light);
                // Lower lip
                drawBox(pose, consumer, bx - size * 0.55f, by - mouthOpen - 0.03f, bz - size - 0.02f, bx + size * 0.55f, by - mouthOpen, bz - size + 0.01f, 0.7f, 0.1f, 0.22f, 0.92f, light);
                // Fangs
                drawBox(pose, consumer, bx - 0.04f, by + mouthOpen - 0.01f, bz - size - 0.025f, bx - 0.02f, by + mouthOpen + 0.04f, bz - size - 0.01f, 0.92f, 0.92f, 0.9f, 0.92f, light);
                drawBox(pose, consumer, bx + 0.02f, by + mouthOpen - 0.01f, bz - size - 0.025f, bx + 0.04f, by + mouthOpen + 0.04f, bz - size - 0.01f, 0.92f, 0.92f, 0.9f, 0.92f, light);
            }
        }

        // 2 mismatching crooked flesh arms as thick tentacles
        for (int a = 0; a < 2; a++) {
            float armSign = (a == 0) ? 1.0f : -1.0f;
            float armWave = (float) Math.sin(time * 0.08f + a) * 0.22f;
            float sx = armSign * 0.38f, sy = 1.18f, sz = 0.0f;
            float mx = sx + armSign * 0.40f, my = sy + armWave, mz = sz + armWave * 0.5f;
            float ex = mx + armSign * 0.24f, ey = my - 0.22f, ez = mz + 0.12f;
            drawSegment(pose, consumer, sx, sy, sz, mx, my, mz, 0.02f, 0.02f, 0.03f, light);
            drawSegment(pose, consumer, mx, my, mz, ex, ey, ez, 0.22f, 0.14f, 0.1f, light);
        }

        // 4 slimy purple tentacles writhing from the base
        int tentacleCount = 4;
        for (int t = 0; t < tentacleCount; t++) {
            float baseAngle = (float) (t * (Math.PI * 2.0 / tentacleCount)) + time * 0.022f;
            int segments = 7;
            for (int s = 0; s < segments; s++) {
                float sA = baseAngle + (float) Math.sin(time * 0.1f - s * 0.42f) * 0.28f;
                float dist = 0.14f + s * 0.15f;
                float tx = (float) Math.cos(sA) * dist;
                float tz = (float) Math.sin(sA) * dist;
                float ty = 0.48f + s * 0.09f + (float) Math.cos(time * 0.12f + s * 0.5f) * 0.06f;
                float tSize = 0.072f * (1.0f - s * 0.1f);
                drawCube(pose, consumer, tx, ty, tz, tSize, 0.12f, 0.02f, 0.2f, 0.88f - s * 0.05f, light);
            }
        }

        // Pervasive magenta corruption / desire particles orbiting the mass
        for (int i = 0; i < 18; i++) {
            Random rand = new Random(i * 123L);
            float speed = 0.38f + rand.nextFloat() * 0.62f;
            float pTime = time * 0.028f * speed + rand.nextFloat() * 100f;
            float radius = 0.65f + rand.nextFloat() * 0.52f;
            float px = (float) Math.cos(pTime) * radius;
            float pz = (float) Math.sin(pTime) * radius;
            float py = 0.18f + (pTime % 2.4f);
            float pSize = 0.032f;
            drawCube(pose, consumer, px, py, pz, pSize, 0.92f, 0.1f, 0.52f, 0.28f, light);
        }
    }

    /**
     * One joint-to-joint length of a crooked flesh arm.
     */
    private void drawSegment(PoseStack.Pose entry, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b, int light) {
        drawStrut(entry, consumer, x1, y1, z1, x2, y2, z2, ARM_THICKNESS, r, g, b, 0.96f, light);
    }
}
