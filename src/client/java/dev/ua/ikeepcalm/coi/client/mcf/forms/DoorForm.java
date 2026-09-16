package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class DoorForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Door";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Humanoid body composed of layered translucent light-bubble shells
        drawLightBody(pose, consumer, 0.05f, 0.0f, -0.15f, 0.35f, 1.0f, 0.15f, light);
        drawLightBody(pose, consumer, -0.35f, 0.0f, -0.15f, -0.05f, 1.0f, 0.15f, light);
        drawLightBody(pose, consumer, -0.38f, 1.0f, -0.22f, 0.38f, 2.0f, 0.22f, light);
        drawLightBody(pose, consumer, 0.38f, 1.05f, -0.14f, 0.66f, 2.0f, 0.14f, light);
        drawLightBody(pose, consumer, -0.66f, 1.05f, -0.14f, -0.38f, 2.0f, 0.14f, light);
        drawLightBody(pose, consumer, -0.22f, 2.0f, -0.22f, 0.22f, 2.55f, 0.22f, light);

        // 10 glowing bubbles orbiting the humanoid, each containing doors of light and an eye
        int bubbleCount = 10;
        for (int b = 0; b < bubbleCount; b++) {
            Random rand = new Random(b * 56789L);
            float orbitHeight = 0.25f + rand.nextFloat() * 2.1f;
            float orbitRadius = 0.4f + rand.nextFloat() * 0.5f;
            float speed = 0.3f + rand.nextFloat() * 0.65f;
            float phase = rand.nextFloat() * 200f;

            float angle = time * 0.028f * speed + phase;
            float bx = (float) Math.cos(angle) * orbitRadius;
            float bz = (float) Math.sin(angle) * orbitRadius;
            float by = orbitHeight + (float) Math.sin(angle * 0.7f) * 0.18f;
            float bubbleSize = 0.16f + rand.nextFloat() * 0.1f;

            // Outer glowing bubble shell (two nested translucent shells)
            drawBox(pose, consumer, bx - bubbleSize, by - bubbleSize, bz - bubbleSize, bx + bubbleSize, by + bubbleSize, bz + bubbleSize, 0.4f, 0.7f, 1.0f, 0.17f, light);
            drawBox(pose, consumer, bx - bubbleSize * 0.76f, by - bubbleSize * 0.76f, bz - bubbleSize * 0.76f, bx + bubbleSize * 0.76f, by + bubbleSize * 0.76f, bz + bubbleSize * 0.76f, 0.55f, 0.88f, 1.0f, 0.14f, light);

            // Interior layered doors of light
            float inner = bubbleSize * 0.44f;
            float pulse = (float) Math.sin(time * 0.1f + b) * 0.04f;
            drawBox(pose, consumer, bx - inner + pulse, by - inner, bz - 0.01f, bx + inner - pulse, by + inner, bz + 0.01f, 0.2f, 0.35f, 0.8f, 0.45f, light);
            drawBox(pose, consumer, bx - 0.01f, by - inner + pulse, bz - inner, bx + 0.01f, by + inner - pulse, bz + inner, 0.3f, 0.45f, 0.9f, 0.35f, light);

            // Eye hiding in the dark center
            float eyeSize = 0.026f + (float) Math.cos(time * 0.08f + b) * 0.008f;
            drawBox(pose, consumer, bx - eyeSize, by - eyeSize, bz - eyeSize, bx + eyeSize, by + eyeSize, bz + eyeSize, 0.04f, 0.04f, 0.1f, 0.88f, light);
            drawBox(pose, consumer, bx - eyeSize * 0.44f, by - eyeSize * 0.44f, bz - eyeSize * 0.44f, bx + eyeSize * 0.44f, by + eyeSize * 0.44f, bz + eyeSize * 0.44f, 0.92f, 0.78f, 0.18f, 0.92f, light);

            // Worms of Cosmos orbiting each bubble (3 worms, 4 segments each)
            for (int w = 0; w < 3; w++) {
                float wPhase = w * (float) (Math.PI * 2.0 / 3);
                float wAngle = time * 0.09f + wPhase + b;
                float wRad = bubbleSize + 0.04f;
                for (int s = 0; s < 4; s++) {
                    float sA = wAngle - s * 0.18f;
                    float sx = bx + (float) Math.cos(sA) * wRad;
                    float sz2 = bz + (float) Math.sin(sA) * wRad;
                    float sy = by + (float) Math.sin(sA * 2f) * 0.04f;
                    float sSize = 0.017f * (1.0f - s * 0.15f);
                    drawBox(pose, consumer, sx - sSize, sy - sSize, sz2 - sSize, sx + sSize, sy + sSize, sz2 + sSize, 0.78f, 0.92f, 1.0f, 0.65f - s * 0.1f, light);
                }
            }
        }
    }

    private void drawLightBody(PoseStack.Pose pose, VertexConsumer consumer,
                               float x1, float y1, float z1, float x2, float y2, float z2, int light) {
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 0.35f, 0.65f, 1.0f, 0.22f, light);
        float s = 0.04f;
        drawBox(pose, consumer, x1 + s, y1 + s, z1 + s, x2 - s, y2 - s, z2 - s, 0.58f, 0.84f, 1.0f, 0.16f, light);
    }
}
