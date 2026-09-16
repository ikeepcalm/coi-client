package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class FortuneForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Fortune";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Semi-transparent humanoid silhouette at the center (fate incarnate)
        float r = 0.82f, g = 0.78f, b = 0.55f, a = 0.28f;
        drawBox(pose, consumer, 0.05f, 0.0f, -0.14f, 0.32f, 1.0f, 0.14f, r, g, b, a, light);
        drawBox(pose, consumer, -0.32f, 0.0f, -0.14f, -0.05f, 1.0f, 0.14f, r, g, b, a, light);
        drawBox(pose, consumer, -0.36f, 1.0f, -0.20f, 0.36f, 2.0f, 0.20f, r, g, b, a, light);
        drawBox(pose, consumer, 0.36f, 1.05f, -0.12f, 0.60f, 2.0f, 0.12f, r, g, b, a, light);
        drawBox(pose, consumer, -0.60f, 1.05f, -0.12f, -0.36f, 2.0f, 0.12f, r, g, b, a, light);
        drawBox(pose, consumer, -0.20f, 2.0f, -0.20f, 0.20f, 2.55f, 0.20f, r, g, b, a + 0.05f, light);

        // Three multi-layered concentric wheels rotating on different axes around the body

        // Outer stone wheel — rotates in X-Y plane (around Z axis, like a ferris wheel)
        float outerAngle = time * 0.028f;
        int outerSegs = 10;
        float outerRad = 1.05f;
        float centerY = 1.28f;
        for (int i = 0; i < outerSegs; i++) {
            float angle = outerAngle + (float) (i * Math.PI * 2.0 / outerSegs);
            float wx = (float) Math.cos(angle) * outerRad;
            float wy = centerY + (float) Math.sin(angle) * outerRad;
            drawBox(pose, consumer, wx - 0.11f, wy - 0.11f, -0.06f, wx + 0.11f, wy + 0.11f, 0.06f, 0.44f, 0.44f, 0.46f, 0.88f, light);
        }

        // Middle crystal wheel — rotates in X-Z plane (around Y axis, like a spinning top)
        float midAngle = -time * 0.042f;
        int midSegs = 8;
        float midRad = 0.78f;
        for (int i = 0; i < midSegs; i++) {
            float angle = midAngle + (float) (i * Math.PI * 2.0 / midSegs);
            float wx = (float) Math.cos(angle) * midRad;
            float wz = (float) Math.sin(angle) * midRad;
            drawBox(pose, consumer, wx - 0.08f, centerY - 0.06f, wz - 0.08f, wx + 0.08f, centerY + 0.06f, wz + 0.08f, 0.32f, 0.74f, 0.88f, 0.68f, light);
        }

        // Inner gold wheel — rotates in Y-Z plane (around X axis)
        float innerAngle = time * 0.062f;
        int innerSegs = 6;
        float innerRad = 0.44f;
        for (int i = 0; i < innerSegs; i++) {
            float angle = innerAngle + (float) (i * Math.PI * 2.0 / innerSegs);
            float wy = centerY + (float) Math.cos(angle) * innerRad;
            float wz = (float) Math.sin(angle) * innerRad;
            drawBox(pose, consumer, -0.055f, wy - 0.06f, wz - 0.06f, 0.055f, wy + 0.06f, wz + 0.06f, 0.92f, 0.78f, 0.2f, 0.92f, light);
        }

        // 4 orbiting tarot / divination cards
        int cardCount = 4;
        for (int i = 0; i < cardCount; i++) {
            float cardAngle = time * 0.022f + i * (float) (Math.PI * 2.0 / cardCount);
            float cRadius = 1.22f + (float) Math.sin(time * 0.06f + i) * 0.08f;
            float cx = (float) Math.cos(cardAngle) * cRadius;
            float cz = (float) Math.sin(cardAngle) * cRadius;
            float cy = centerY + (float) Math.cos(cardAngle * 2.0f) * 0.22f;
            float cw = 0.085f, ch = 0.145f;
            drawBox(pose, consumer, cx - cw, cy - ch, cz - 0.01f, cx + cw, cy + ch, cz + 0.01f, 0.86f, 0.82f, 0.65f, 0.92f, light);
            drawBox(pose, consumer, cx - cw * 0.6f, cy - ch * 0.6f, cz + 0.012f, cx + cw * 0.6f, cy + ch * 0.6f, cz + 0.015f, 0.14f, 0.1f, 0.32f, 0.92f, light);
        }

        // Fluctuating golden threads of fate spiraling around
        int threadSegs = 30;
        for (int i = 0; i < threadSegs; i++) {
            float angle = i * (float) (Math.PI * 3.2 / threadSegs) + time * 0.058f;
            float radius = 0.82f + (float) Math.sin(time * 0.04f + i) * 0.16f;
            float tx = (float) Math.cos(angle) * radius;
            float tz = (float) Math.sin(angle) * radius;
            float ty = centerY + (float) Math.sin(angle * 2.0f) * 0.44f;
            float thSize = 0.014f;
            drawBox(pose, consumer, tx - thSize, ty - thSize, tz - thSize, tx + thSize, ty + thSize, tz + thSize, 1.0f, 0.86f, 0.2f, 0.82f, light);
        }
    }
}
