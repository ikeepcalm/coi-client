package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class DarknessForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Darkness";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float bodyBob = (float) Math.sin(time * 0.05f) * 0.04f;

        // Hulking hunched humanoid of absolute pitch-black fur — bipedal stance
        // Wide powerful legs
        drawBox(pose, consumer, 0.06f, 0.0f, -0.18f, 0.40f, 1.0f, 0.18f, 0.01f, 0.01f, 0.02f, 0.97f, light);
        drawBox(pose, consumer, -0.40f, 0.0f, -0.18f, -0.06f, 1.0f, 0.18f, 0.01f, 0.01f, 0.02f, 0.97f, light);
        // Massive wide torso (hound-like chest, hunched forward)
        drawBox(pose, consumer, -0.52f, 1.0f + bodyBob, -0.32f, 0.52f, 1.95f + bodyBob, 0.32f, 0.01f, 0.01f, 0.02f, 0.97f, light);
        // Short powerful arms
        drawBox(pose, consumer, 0.52f, 1.0f + bodyBob, -0.18f, 0.82f, 1.78f + bodyBob, 0.18f, 0.01f, 0.01f, 0.02f, 0.96f, light);
        drawBox(pose, consumer, -0.82f, 1.0f + bodyBob, -0.18f, -0.52f, 1.78f + bodyBob, 0.18f, 0.01f, 0.01f, 0.02f, 0.96f, light);
        // Massive low-set head
        drawBox(pose, consumer, -0.30f, 1.92f + bodyBob, -0.24f, 0.30f, 2.58f + bodyBob, 0.24f, 0.01f, 0.01f, 0.02f, 0.97f, light);

        // 8 spider/hound legs extending from the back and sides (4 pairs)
        for (int i = 0; i < 8; i++) {
            boolean isLeft = (i % 2 == 0);
            int pairIdx = i / 2;
            float sideSign = isLeft ? -1.0f : 1.0f;
            float legWave = (float) Math.sin(time * 0.10f + i * 0.85f) * 0.22f;

            float startX = sideSign * 0.44f;
            float startY = 1.25f + bodyBob;
            float startZ = -0.22f + pairIdx * 0.19f;

            float midX = startX + sideSign * 0.58f;
            float midY = startY + 0.32f + legWave;
            float midZ = startZ + 0.07f;

            float endX = midX + sideSign * 0.32f;
            float endY = 0.02f;
            float endZ = midZ + 0.14f;

            drawLeg(pose, consumer, startX, startY, startZ, midX, midY, midZ, light);
            drawLeg(pose, consumer, midX, midY, midZ, endX, endY, endZ, light);
        }

        // 24 layered obsidian-like eyes covering the entire body
        for (int i = 0; i < 24; i++) {
            Random rand = new Random(i * 10101L);
            float ex, ey, ez;
            if (i < 9) { // eyes on the massive head
                ex = (rand.nextFloat() - 0.5f) * 0.55f;
                ey = 2.0f + rand.nextFloat() * 0.5f + bodyBob;
                ez = -0.24f - rand.nextFloat() * 0.04f;
            } else { // eyes covering the torso and arms
                ex = (rand.nextFloat() - 0.5f);
                ey = 1.0f + rand.nextFloat() * 0.9f + bodyBob;
                ez = (rand.nextFloat() - 0.5f) * 0.62f;
            }
            float eyePulse = 0.5f + (float) Math.sin(time * 0.08f + i) * 0.48f;
            float eyeSize = 0.030f;
            // Obsidian violet glow
            drawBox(pose, consumer, ex - eyeSize, ey - eyeSize, ez - eyeSize, ex + eyeSize, ey + eyeSize, ez + eyeSize, 0.38f, 0.07f, 0.84f, eyePulse, light);
            drawBox(pose, consumer, ex - eyeSize * 0.38f, ey - eyeSize * 0.38f, ez - eyeSize * 0.38f, ex + eyeSize * 0.38f, ey + eyeSize * 0.38f, ez + eyeSize * 0.38f, 0.0f, 0.0f, 0.0f, eyePulse, light);
        }

        // Slow-floating sweet dream / eternal silence particles
        for (int i = 0; i < 20; i++) {
            Random rand = new Random(i * 888L);
            float pTime = time * 0.016f * (0.28f + rand.nextFloat() * 0.5f) + rand.nextFloat() * 200f;
            float radius = 0.68f + rand.nextFloat() * 0.58f;
            float px = (float) Math.cos(pTime) * radius;
            float pz = (float) Math.sin(pTime) * radius;
            float py = 0.04f + (pTime % 2.55f);
            float pSize = 0.034f;
            drawBox(pose, consumer, px - pSize, py - pSize, pz - pSize, px + pSize, py + pSize, pz + pSize, 0.11f, 0.04f, 0.28f, 0.30f, light);
        }
    }

    private void drawLeg(PoseStack.Pose entry, VertexConsumer consumer,
                         float x1, float y1, float z1, float x2, float y2, float z2, int light) {
        float minX = Math.min(x1, x2) - 0.032f;
        float maxX = Math.max(x1, x2) + 0.032f;
        float minY = Math.min(y1, y2) - 0.032f;
        float maxY = Math.max(y1, y2) + 0.032f;
        float minZ = Math.min(z1, z2) - 0.032f;
        float maxZ = Math.max(z1, z2) + 0.032f;
        drawBox(entry, consumer, minX, minY, minZ, maxX, maxY, maxZ, 0.01f, 0.01f, 0.02f, 0.96f, light);
    }
}
