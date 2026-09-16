package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class TyrantForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Tyrant";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Towering dark blue humanoid silhouette with deep-blue smooth scales
        // Legs
        drawBox(pose, consumer, 0.06f, 0.0f, -0.17f, 0.38f, 1.0f, 0.17f, 0.04f, 0.13f, 0.40f, 0.88f, light);
        drawBox(pose, consumer, -0.38f, 0.0f, -0.17f, -0.06f, 1.0f, 0.17f, 0.04f, 0.13f, 0.40f, 0.88f, light);
        // Torso
        drawBox(pose, consumer, -0.44f, 1.0f, -0.26f, 0.44f, 2.0f, 0.26f, 0.04f, 0.14f, 0.44f, 0.85f, light);
        // Arms
        drawBox(pose, consumer, 0.44f, 1.05f, -0.17f, 0.74f, 2.0f, 0.17f, 0.04f, 0.12f, 0.38f, 0.85f, light);
        drawBox(pose, consumer, -0.74f, 1.05f, -0.17f, -0.44f, 2.0f, 0.17f, 0.04f, 0.12f, 0.38f, 0.85f, light);
        // Head
        drawBox(pose, consumer, -0.23f, 2.0f, -0.23f, 0.23f, 2.58f, 0.23f, 0.05f, 0.15f, 0.46f, 0.88f, light);

        // Glowing blue symbols (strange scales) on the body surface
        int symbolCount = 10;
        for (int i = 0; i < symbolCount; i++) {
            float sy = 0.12f + i * 0.24f;
            float spulse = 0.42f + (float) Math.sin(time * 0.1f + i) * 0.36f;
            drawBox(pose, consumer, -0.46f, sy, -0.08f, -0.43f, sy + 0.08f, 0.08f, 0.15f, 0.78f, 1.0f, spulse, light);
            drawBox(pose, consumer, 0.43f, sy, -0.08f, 0.46f, sy + 0.08f, 0.08f, 0.15f, 0.78f, 1.0f, spulse, light);
        }

        // 5 spinning hurricane/storm wind rings at different body heights
        int ringCount = 5;
        for (int r = 0; r < ringCount; r++) {
            float ry = 0.16f + r * 0.50f;
            float ringSpeed = 0.15f + r * 0.05f;
            float currentAngle = time * ringSpeed * (r % 2 == 0 ? 1f : -1f);
            float radius = 0.78f + (float) Math.sin(time * 0.07f + r) * 0.06f;
            int panels = 10;
            for (int p = 0; p < panels; p++) {
                float angle = (float) (p * (Math.PI * 2.0 / panels)) + currentAngle;
                float px = (float) Math.cos(angle) * radius;
                float pz = (float) Math.sin(angle) * radius;
                float size = 0.068f;
                drawBox(pose, consumer, px - size, ry - 0.018f, pz - size, px + size, ry + 0.018f, pz + size, 0.07f, 0.38f, 0.62f, 0.22f, light);
            }
        }

        // 6 slimy writhing tentacles from the lower body
        int tentacleCount = 6;
        for (int t = 0; t < tentacleCount; t++) {
            float baseAngle = (float) (t * (Math.PI * 2.0 / tentacleCount));
            float tTime = time * 0.07f + t * 42f;
            int segments = 9;
            for (int s = 0; s < segments; s++) {
                float tAngle = baseAngle + (float) Math.sin(tTime - s * 0.3f) * 0.28f;
                float dist = 0.22f + s * 0.14f;
                float tx = (float) Math.cos(tAngle) * dist;
                float tz = (float) Math.sin(tAngle) * dist;
                float ty = 0.12f + s * 0.072f + (float) Math.cos(tTime * 1.4f + s * 0.5f) * 0.05f;
                float tSize = 0.068f * (1.0f - s * 0.075f);
                drawBox(pose, consumer, tx - tSize, ty - tSize, tz - tSize, tx + tSize, ty + tSize, tz + tSize, 0.02f, 0.1f, 0.34f, 0.78f - s * 0.045f, light);
            }
        }

        // Violent lightning bolts striking intermittently from above
        Random lRand = new Random((long) (time / 3.5f));
        if (lRand.nextInt(3) == 0) {
            float sX = (lRand.nextFloat() - 0.5f) * 1.5f;
            float sZ = (lRand.nextFloat() - 0.5f) * 1.5f;
            float curX = sX, curY = 2.75f, curZ = sZ;
            for (int l = 0; l < 7; l++) {
                float nextY = 2.75f - (l + 1) * (2.75f / 7);
                float nextX = curX + (lRand.nextFloat() - 0.5f) * 0.34f;
                float nextZ = curZ + (lRand.nextFloat() - 0.5f) * 0.34f;
                drawBox(pose, consumer,
                        Math.min(curX, nextX) - 0.022f, Math.min(curY, nextY) - 0.022f, Math.min(curZ, nextZ) - 0.022f,
                        Math.max(curX, nextX) + 0.022f, Math.max(curY, nextY) + 0.022f, Math.max(curZ, nextZ) + 0.022f,
                        0.72f, 0.92f, 1.0f, 0.94f, light);
                curX = nextX;
                curY = nextY;
                curZ = nextZ;
            }
        }
    }
}
