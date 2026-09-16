package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class ParagonForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Paragon";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Industrial machine humanoid — each body region is a gear/piston assembly

        // Leg cylinders (bronze structural columns)
        drawBronzePillar(pose, consumer, 0.06f, 0.0f, -0.15f, 0.36f, 1.0f, 0.15f, light);
        drawBronzePillar(pose, consumer, -0.36f, 0.0f, -0.15f, -0.06f, 1.0f, 0.15f, light);

        // Torso — central gear hub assembly
        drawBronzePillar(pose, consumer, -0.40f, 1.0f, -0.22f, 0.40f, 2.0f, 0.22f, light);

        // Arm pistons (left/right sliding rods)
        float pistonL = (float) Math.sin(time * 0.12f) * 0.28f;
        float pistonR = (float) Math.cos(time * 0.12f) * 0.28f;
        // Right arm (piston rod)
        drawBox(pose, consumer, 0.40f, 1.0f, -0.02f, 0.46f, 1.92f + pistonR, 0.06f, 0.58f, 0.58f, 0.62f, 0.92f, light);
        drawBox(pose, consumer, 0.37f, 1.88f + pistonR, -0.05f, 0.49f, 2.0f + pistonR, 0.09f, 0.7f, 0.45f, 0.2f, 0.92f, light);
        // Left arm (counter piston)
        drawBox(pose, consumer, -0.46f, 1.0f, -0.02f, -0.40f, 1.92f + pistonL, 0.06f, 0.58f, 0.58f, 0.62f, 0.92f, light);
        drawBox(pose, consumer, -0.49f, 1.88f + pistonL, -0.05f, -0.37f, 2.0f + pistonL, 0.09f, 0.7f, 0.45f, 0.2f, 0.92f, light);

        // Head — rotating gear crown
        drawBronzePillar(pose, consumer, -0.20f, 2.0f, -0.20f, 0.20f, 2.45f, 0.20f, light);

        // 3 interlocking gears at different body levels
        drawGear(pose, consumer, 0.0f, 0.45f, 0.0f, 0.62f, time * 0.055f, true, light);   // base gear (horizontal)
        drawGear(pose, consumer, 0.0f, 1.45f, 0.0f, 0.46f, -time * 0.072f, false, light); // mid gear (vertical)
        drawGear(pose, consumer, 0.0f, 2.22f, 0.0f, 0.32f, time * 0.095f, true, light);   // head gear (horizontal)

        // Glowing cyan circuit pathways connecting body regions
        int circuitNodes = 10;
        for (int i = 0; i < circuitNodes; i++) {
            float cy = 0.2f + i * 0.22f;
            float cpulse = 0.45f + (float) Math.sin(time * 0.1f + i) * 0.32f;
            float cx = (i % 2 == 0) ? -0.40f : 0.40f;
            float minX = Math.min(0.0f, cx);
            float maxX = Math.max(0.0f, cx);
            drawBox(pose, consumer, minX, cy, -0.01f, maxX, cy + 0.018f, 0.01f, 0.1f, 0.92f, 1.0f, cpulse, light);
        }

        // 4 orbiting blue blueprint panels
        int bpCount = 4;
        for (int i = 0; i < bpCount; i++) {
            Random rand = new Random(i * 1234L);
            float bpAngle = time * 0.022f + i * (float) (Math.PI * 2.0 / bpCount);
            float radius = 0.90f;
            float bx = (float) Math.cos(bpAngle) * radius;
            float bz = (float) Math.sin(bpAngle) * radius;
            float by = 0.55f + i * 0.38f + (float) Math.sin(time * 0.055f + i) * 0.1f;
            float size = 0.21f;
            drawBox(pose, consumer, bx - size, by - size, bz - 0.01f, bx + size, by + size, bz + 0.01f, 0.1f, 0.42f, 0.92f, 0.28f, light);
            drawBox(pose, consumer, bx - size, by - 0.005f, bz - 0.016f, bx + size, by + 0.005f, bz + 0.016f, 0.9f, 0.95f, 1.0f, 0.45f, light);
            drawBox(pose, consumer, bx - 0.005f, by - size, bz - 0.016f, bx + 0.005f, by + size, bz + 0.016f, 0.9f, 0.95f, 1.0f, 0.45f, light);
        }
    }

    private void drawBronzePillar(PoseStack.Pose pose, VertexConsumer consumer,
                                  float x1, float y1, float z1, float x2, float y2, float z2, int light) {
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 0.62f, 0.38f, 0.14f, 0.92f, light);
        float s = 0.04f;
        if (x2 - x1 > s * 2 && y2 - y1 > s * 2 && z2 - z1 > s * 2) {
            drawBox(pose, consumer, x1 + s, y1 + s, z1 + s, x2 - s, y2 - s, z2 - s, 0.50f, 0.28f, 0.08f, 0.92f, light);
        }
    }

    private void drawGear(PoseStack.Pose pose, VertexConsumer consumer,
                          float cx, float cy, float cz, float radius, float angle, boolean horizontal, int light) {
        float hubSize = 0.075f;
        drawBox(pose, consumer, cx - hubSize, cy - hubSize, cz - hubSize, cx + hubSize, cy + hubSize, cz + hubSize, 0.5f, 0.3f, 0.1f, 0.95f, light);

        int teeth = 7;
        for (int t = 0; t < teeth; t++) {
            float tAngle = angle + (float) (t * Math.PI * 2.0 / teeth);
            float dx = (float) Math.cos(tAngle) * radius;
            float dz = (float) Math.sin(tAngle) * radius;
            if (horizontal) {
                drawBox(pose, consumer, cx, cy - 0.02f, cz, cx + dx, cy + 0.02f, cz + dz, 0.64f, 0.40f, 0.15f, 0.92f, light);
                drawBox(pose, consumer, cx + dx - 0.04f, cy - 0.03f, cz + dz - 0.04f, cx + dx + 0.04f, cy + 0.03f, cz + dz + 0.04f, 0.70f, 0.46f, 0.2f, 0.92f, light);
            } else {
                float dy = (float) Math.sin(tAngle) * radius;
                drawBox(pose, consumer, cx, cy, cz - 0.02f, cx + dx, cy + dy, cz + 0.02f, 0.64f, 0.40f, 0.15f, 0.92f, light);
                drawBox(pose, consumer, cx + dx - 0.04f, cy + dy - 0.04f, cz - 0.03f, cx + dx + 0.04f, cy + dy + 0.04f, cz + 0.03f, 0.70f, 0.46f, 0.2f, 0.92f, light);
            }
        }
    }
}
