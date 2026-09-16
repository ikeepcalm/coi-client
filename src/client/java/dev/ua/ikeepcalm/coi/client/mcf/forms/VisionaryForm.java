package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.CoiModelLayers;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import dev.ua.ikeepcalm.coi.client.mcf.PartialFormSpec;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class VisionaryForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Visionary";
    }

    @Override
    public PartialFormSpec partialForm() {
        return CoiModelLayers.VISIONARY_LOWER_SPEC;
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        // Superseded by the baked model in partialForm() — kept as a fallback in case the
        // model/texture fails to load, so the pathway still renders something.
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float bodyWave = (float) Math.sin(time * 0.055f) * 0.03f;

        // Bipedal golden dragon stance — humanoid proportions with dragon features
        // Dragon-scale legs
        drawScaled(pose, consumer, 0.06f, 0.0f, -0.16f, 0.38f, 1.0f, 0.16f, light);
        drawScaled(pose, consumer, -0.38f, 0.0f, -0.16f, -0.06f, 1.0f, 0.16f, light);
        // Wider dragon chest/torso
        drawScaled(pose, consumer, -0.44f, 1.0f, -0.26f + bodyWave, 0.44f, 2.0f, 0.26f + bodyWave, light);
        // Dragon forelimbs / arms
        drawScaled(pose, consumer, 0.44f, 1.1f, -0.16f, 0.72f, 1.9f, 0.16f, light);
        drawScaled(pose, consumer, -0.72f, 1.1f, -0.16f, -0.44f, 1.9f, 0.16f, light);

        // Dragon neck
        float neckWave = (float) Math.sin(time * 0.07f) * 0.06f;
        drawBox(pose, consumer, -0.14f + neckWave, 2.0f, -0.14f, 0.14f + neckWave, 2.28f, 0.14f, 1.0f, 0.82f, 0.14f, 0.78f, light);

        // Dragon head
        float headX = neckWave * 1.4f;
        float headY = 2.28f + (float) Math.sin(time * 0.08f) * 0.04f;
        float headHalf = 0.22f;
        drawBox(pose, consumer, headX - headHalf, headY - 0.04f, -headHalf, headX + headHalf, headY + headHalf + 0.06f, headHalf, 1.0f, 0.82f, 0.12f, 0.78f, light);
        // Snout/jaw
        drawBox(pose, consumer, headX - headHalf * 0.7f, headY - 0.18f, -headHalf * 0.4f, headX + headHalf * 0.7f, headY - 0.02f, headHalf * 0.55f, 0.9f, 0.72f, 0.1f, 0.82f, light);

        // Shifting civilization eyes at the FRONT of the head (-Z = visible to viewer)
        float eyeR = 0.5f + (float) Math.sin(time * 0.02f) * 0.5f;
        float eyeG = 0.5f + (float) Math.sin(time * 0.033f + 2.0f) * 0.5f;
        float eyeB = 0.5f + (float) Math.cos(time * 0.015f) * 0.5f;
        float eyeSize = 0.052f;
        drawBox(pose, consumer, headX + 0.09f, headY + 0.04f, -headHalf - eyeSize, headX + 0.09f + eyeSize, headY + 0.04f + eyeSize, -headHalf + 0.01f, eyeR, eyeG, eyeB, 0.95f, light);
        drawBox(pose, consumer, headX - 0.09f - eyeSize, headY + 0.04f, -headHalf - eyeSize, headX - 0.09f, headY + 0.04f + eyeSize, -headHalf + 0.01f, eyeR, eyeG, eyeB, 0.95f, light);

        // Wings extending from upper BACK (+Z = behind the creature)
        float wingWave = (float) Math.sin(time * 0.07f) * 0.28f;
        for (int w = 0; w < 5; w++) {
            float wp = w / 4.0f;
            float wx = 0.44f + w * 0.30f;
            float wy = 1.88f - w * 0.14f + wingWave * wp;
            float wz = 0.38f + w * 0.08f;
            float wSX = 0.115f, wSY = 0.30f - w * 0.04f, wSZ = 0.022f;
            drawBox(pose, consumer, wx - wSX, wy - wSY, wz - wSZ, wx + wSX, wy + wSY, wz + wSZ, 1.0f, 0.86f, 0.18f, 0.38f - w * 0.04f, light);
            drawBox(pose, consumer, -wx - wSX, wy - wSY, wz - wSZ, -wx + wSX, wy + wSY, wz + wSZ, 1.0f, 0.86f, 0.18f, 0.38f - w * 0.04f, light);
        }

        // Floating crystal scales orbiting the body
        int scaleCount = 18;
        for (int i = 0; i < scaleCount; i++) {
            Random rand = new Random(i * 123L);
            float scaleAngle = (float) (i * (Math.PI * 2.0 / scaleCount)) + time * 0.022f;
            float scaleH = 0.2f + rand.nextFloat() * 2.2f;
            float scaleR = 0.6f + rand.nextFloat() * 0.28f;
            float scaleX = (float) Math.cos(scaleAngle) * scaleR;
            float scaleZ = (float) Math.sin(scaleAngle) * scaleR;
            float sSize = 0.036f;
            drawBox(pose, consumer, scaleX - sSize, scaleH - sSize, scaleZ - sSize, scaleX + sSize, scaleH + sSize, scaleZ + sSize, 1.0f, 0.92f, 0.45f, 0.75f, light);
        }
    }

    private void drawScaled(PoseStack.Pose pose, VertexConsumer consumer,
                            float x1, float y1, float z1, float x2, float y2, float z2, int light) {
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 0.92f, 0.75f, 0.18f, 0.58f, light);
        float s = 0.042f;
        drawBox(pose, consumer, x1 + s, y1 + s, z1 + s, x2 - s, y2 - s, z2 - s, 1.0f, 0.92f, 0.45f, 0.34f, light);
    }
}
