package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class DemonessForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Demoness";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float pulse = (float) Math.sin(time * 0.08f) * 0.022f;
        float skirtPulse = (float) Math.cos(time * 0.08f) * 0.03f;

        // Mesmerizing female humanoid of dense gray fog — full body

        // Legs (slender fog pillars emerging from the skirt)
        drawBox(pose, consumer, 0.04f, 0.0f, -0.13f, 0.25f, 0.55f, 0.13f, 0.48f, 0.48f, 0.50f, 0.58f, light);
        drawBox(pose, consumer, -0.25f, 0.0f, -0.13f, -0.04f, 0.55f, 0.13f, 0.48f, 0.48f, 0.50f, 0.58f, light);

        // Hips / Skirt (flared fog mass)
        drawBox(pose, consumer, -0.34f - skirtPulse, 0.55f, -0.28f - skirtPulse, 0.34f + skirtPulse, 1.08f, 0.28f + skirtPulse, 0.45f, 0.45f, 0.48f, 0.52f, light);

        // Waist (narrower)
        drawBox(pose, consumer, -0.14f, 1.08f, -0.13f, 0.14f, 1.38f, 0.13f, 0.50f, 0.50f, 0.53f, 0.62f, light);

        // Upper torso (broader at chest)
        drawBox(pose, consumer, -0.24f - pulse, 1.38f, -0.17f, 0.24f + pulse, 1.88f, 0.17f, 0.54f, 0.54f, 0.57f, 0.57f, light);

        // Arms (slender fog limbs)
        drawBox(pose, consumer, 0.24f + pulse, 1.38f, -0.12f, 0.52f, 1.85f, 0.12f, 0.50f, 0.50f, 0.53f, 0.55f, light);
        drawBox(pose, consumer, -0.52f, 1.38f, -0.12f, -0.24f - pulse, 1.85f, 0.12f, 0.50f, 0.50f, 0.53f, 0.55f, light);

        // Head / Face (fog shell)
        drawBox(pose, consumer, -0.17f, 1.88f, -0.14f, 0.17f, 2.30f, 0.14f, 0.58f, 0.58f, 0.62f, 0.57f, light);

        // Spider silk threads (24 helical segments wrapping the body contour)
        int threadSegments = 30;
        for (int i = 0; i < threadSegments; i++) {
            float angle = i * (float) (Math.PI * 2.6 / threadSegments) + time * 0.048f;
            float height = 0.08f + i * (2.1f / threadSegments);

            // Radius mirrors the body silhouette
            float radius;
            if (height < 0.55f) {
                radius = 0.16f;
            } else if (height < 1.08f) {
                radius = 0.34f - (height - 0.55f) * 0.12f;
            } else if (height < 1.38f) {
                radius = 0.15f;
            } else if (height < 1.88f) {
                radius = 0.15f + (height - 1.38f) * 0.18f;
            } else {
                radius = 0.18f;
            }

            float tx = (float) Math.cos(angle) * radius;
            float tz = (float) Math.sin(angle) * radius;
            float thS = 0.011f;
            drawBox(pose, consumer, tx - thS, height - thS, tz - thS, tx + thS, height + thS, tz + thS, 0.95f, 0.95f, 1.0f, 0.72f, light);
        }

        // Writhing purplish-black plague spots and disease symbols inside the fog
        int spotCount = 18;
        for (int i = 0; i < spotCount; i++) {
            Random rand = new Random(i * 1212L);
            float spotTime = time * 0.04f + rand.nextFloat() * 100f;
            float sy = 0.1f + rand.nextFloat() * 2.0f;
            float maxR = getBodyRadius(sy);
            float angle = spotTime * (rand.nextBoolean() ? 1f : -1f);
            float sx = (float) Math.cos(angle) * (rand.nextFloat() * maxR);
            float sz = (float) Math.sin(angle) * (rand.nextFloat() * maxR);
            float spotSize = 0.026f + rand.nextFloat() * 0.022f;
            drawBox(pose, consumer, sx - spotSize, sy - spotSize, sz - spotSize, sx + spotSize, sy + spotSize, sz + spotSize, 0.18f, 0.02f, 0.22f, 0.78f, light);
            drawBox(pose, consumer, sx - spotSize * 0.48f, sy - spotSize * 0.48f, sz - spotSize * 0.48f, sx + spotSize * 0.48f, sy + spotSize * 0.48f, sz + spotSize * 0.48f, 0.35f, 0.0f, 0.46f, 0.92f, light);
        }
    }

    private float getBodyRadius(float y) {
        if (y < 0.55f) return 0.18f;
        if (y < 1.08f) return 0.28f;
        if (y < 1.38f) return 0.12f;
        if (y < 1.88f) return 0.22f;
        return 0.15f;
    }
}
