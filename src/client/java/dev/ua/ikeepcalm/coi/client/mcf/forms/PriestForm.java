package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class PriestForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Priest";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float pulse = (float) Math.sin(time * 0.14f) * 0.04f;

        // Towering crimson-flame warlord silhouette — layered fire humanoid

        // Fire legs
        drawFireColumn(pose, consumer, 0.05f, 0.0f, -0.17f, 0.38f, 1.0f, 0.17f, pulse, light);
        drawFireColumn(pose, consumer, -0.38f, 0.0f, -0.17f, -0.05f, 1.0f, 0.17f, pulse, light);

        // Armored fire torso (3 layers)
        float bW = 0.42f + pulse;
        drawBox(pose, consumer, -bW, 1.0f, -bW, bW, 2.1f, bW, 1.0f, 0.1f, 0.0f, 0.24f, light);
        float mW = bW * 0.74f;
        drawBox(pose, consumer, -mW, 1.02f, -mW, mW, 2.08f, mW, 1.0f, 0.4f, 0.0f, 0.40f, light);
        float cW = bW * 0.42f;
        drawBox(pose, consumer, -cW, 1.05f, -cW, cW, 2.05f, cW, 1.0f, 0.8f, 0.0f, 0.62f, light);

        // Fire arms
        drawFireColumn(pose, consumer, 0.42f + pulse, 1.05f, -0.18f, 0.72f - pulse, 1.98f, 0.18f, pulse, light);
        drawFireColumn(pose, consumer, -0.72f - pulse, 1.05f, -0.18f, -0.42f + pulse, 1.98f, 0.18f, pulse, light);

        // Warlord helmet/crown of fire
        drawBox(pose, consumer, -0.22f, 2.1f, -0.22f, 0.22f, 2.52f, 0.22f, 0.8f, 0.1f, 0.0f, 0.72f, light);
        drawBox(pose, consumer, -0.16f, 2.12f, -0.16f, 0.16f, 2.50f, 0.16f, 1.0f, 0.6f, 0.0f, 0.58f, light);
        // Crown peak (glowing gold)
        drawBox(pose, consumer, -0.05f, 2.52f, -0.05f, 0.05f, 2.75f, 0.05f, 1.0f, 0.92f, 0.0f, 0.92f, light);

        // Two tattered war banners on back poles (+Z = back of creature)
        for (int side = 0; side < 2; side++) {
            float sideSign = side == 0 ? 1.0f : -1.0f;
            float bx = sideSign * 0.28f;
            float bz = 0.44f;
            // Banner pole
            drawBox(pose, consumer, bx - 0.022f, 0.5f, bz - 0.022f, bx + 0.022f, 2.45f, bz + 0.022f, 0.5f, 0.3f, 0.1f, 0.92f, light);
            // Waving banner cloth (5 horizontal segments, extending further back)
            for (int i = 0; i < 5; i++) {
                float segZ = bz + i * 0.13f;
                float segX = bx + (float) Math.sin(time * 0.1f - i * 0.42f) * 0.09f;
                float bSizeY = 0.42f;
                drawBox(pose, consumer, segX - 0.016f, 1.55f - bSizeY, segZ - 0.065f, segX + 0.016f, 1.55f + bSizeY, segZ + 0.065f, 0.9f, 0.05f, 0.05f, 0.68f - i * 0.05f, light);
            }
        }

        // Rising embers / heat-distortion sparks
        int sparkCount = 22;
        for (int i = 0; i < sparkCount; i++) {
            Random rand = new Random(i * 9911L);
            float spiralSpeed = 1.0f + rand.nextFloat() * 1.5f;
            float offsetAngle = rand.nextFloat() * 100f;
            float sTime = time * 0.03f * spiralSpeed + offsetAngle;
            float sy = sTime % 2.75f;
            float radius = 0.2f + rand.nextFloat() * 0.45f;
            float sx = (float) Math.cos(sTime * 2.5f) * radius;
            float sz = (float) Math.sin(sTime * 2.5f) * radius;
            float sparkSize = 0.014f + rand.nextFloat() * 0.014f;
            drawBox(pose, consumer, sx - sparkSize, sy - sparkSize, sz - sparkSize, sx + sparkSize, sy + sparkSize, sz + sparkSize, 1.0f, 0.7f + rand.nextFloat() * 0.3f, 0.1f, 0.82f * (1.0f - sy / 2.75f), light);
        }
    }

    private void drawFireColumn(PoseStack.Pose pose, VertexConsumer consumer,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float pulse, int light) {
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 1.0f, 0.1f, 0.0f, 0.24f, light);
        float s = 0.05f;
        if (x2 - x1 > s * 2 && y2 - y1 > s * 2 && z2 - z1 > s * 2) {
            drawBox(pose, consumer, x1 + s, y1 + s, z1 + s, x2 - s, y2 - s, z2 - s, 1.0f, 0.45f, 0.0f, 0.38f, light);
        }
    }
}
