package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class DeathForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Death";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Upright bone skeleton humanoid (the feathered serpent coils around it)
        // Skeletal legs
        drawBox(pose, consumer, 0.05f, 0.0f, -0.13f, 0.32f, 1.0f, 0.13f, 0.93f, 0.93f, 0.86f, 0.95f, light);
        drawBox(pose, consumer, -0.32f, 0.0f, -0.13f, -0.05f, 1.0f, 0.13f, 0.93f, 0.93f, 0.86f, 0.95f, light);
        // Ribcage torso
        drawBox(pose, consumer, -0.36f, 1.0f, -0.21f, 0.36f, 2.0f, 0.21f, 0.94f, 0.94f, 0.88f, 0.92f, light);
        // Rib bones (4 pairs extending outward)
        for (int r = 0; r < 4; r++) {
            float ry = 1.14f + r * 0.22f;
            float ribW = 0.46f;
            drawBox(pose, consumer, 0.36f, ry, -0.04f, ribW, ry + 0.04f, 0.04f, 0.88f, 0.88f, 0.82f, 0.92f, light);
            drawBox(pose, consumer, -ribW, ry, -0.04f, -0.36f, ry + 0.04f, 0.04f, 0.88f, 0.88f, 0.82f, 0.92f, light);
        }
        // Skeletal arms
        drawBox(pose, consumer, 0.36f, 1.05f, -0.12f, 0.62f, 2.0f, 0.12f, 0.92f, 0.92f, 0.86f, 0.92f, light);
        drawBox(pose, consumer, -0.62f, 1.05f, -0.12f, -0.36f, 2.0f, 0.12f, 0.92f, 0.92f, 0.86f, 0.92f, light);
        // Skull head
        drawBox(pose, consumer, -0.20f, 2.0f, -0.20f, 0.20f, 2.55f, 0.20f, 0.95f, 0.95f, 0.90f, 0.95f, light);
        // Jaw protrusion
        drawBox(pose, consumer, -0.14f, 1.88f, -0.12f, 0.14f, 2.04f, 0.12f, 0.88f, 0.88f, 0.82f, 0.92f, light);

        // Pale blue soul fires burning in the eye sockets (front = -Z)
        float firePulse = 0.65f + (float) Math.sin(time * 0.14f) * 0.30f;
        float fireSize = 0.042f;
        drawBox(pose, consumer, 0.068f, 2.32f, -0.16f - fireSize, 0.068f + fireSize, 2.32f + fireSize, -0.16f, 0.18f, 0.82f, 1.0f, firePulse, light);
        drawBox(pose, consumer, -0.068f - fireSize, 2.32f, -0.16f - fireSize, -0.068f, 2.32f + fireSize, -0.16f, 0.18f, 0.82f, 1.0f, firePulse, light);

        // Feathered serpent vertebrae winding around the skeleton (14 segments)
        int segments = 14;
        for (int i = 0; i < segments; i++) {
            float waveX = (float) Math.sin(time * 0.07f - i * 0.52f) * 0.40f;
            float waveZ = (float) Math.cos(time * 0.055f - i * 0.42f) * 0.30f;
            float segY = 0.18f + i * 0.175f;
            float boneSize = 0.105f * (1.0f - i * 0.02f);

            // Alternate sides so the serpent winds around the skeleton
            float sideOffset = (i % 3 == 0) ? 0.50f : (i % 3 == 1) ? -0.50f : 0.0f;
            float bx = waveX + sideOffset;

            // Vertebra
            drawBox(pose, consumer, bx - boneSize, segY - boneSize, waveZ - boneSize, bx + boneSize, segY + boneSize, waveZ + boneSize, 0.91f, 0.91f, 0.84f, 0.94f, light);
            // Side rib protrusions
            float ribOff = boneSize * 1.65f;
            drawBox(pose, consumer, bx - ribOff - 0.02f, segY - 0.017f, waveZ - 0.017f, bx - boneSize, segY + 0.017f, waveZ + 0.017f, 0.84f, 0.84f, 0.78f, 0.88f, light);
            drawBox(pose, consumer, bx + boneSize, segY - 0.017f, waveZ - 0.017f, bx + ribOff + 0.02f, segY + 0.017f, waveZ + 0.017f, 0.84f, 0.84f, 0.78f, 0.88f, light);

            // Dark green underworld gas clouds surrounding each segment
            Random gasRand = new Random(i * 777L);
            for (int g = 0; g < 3; g++) {
                float gasAngle = gasRand.nextFloat() * (float) (Math.PI * 2.0) + time * 0.04f;
                float gasRad = boneSize * 2.5f;
                float gx = bx + (float) Math.cos(gasAngle) * gasRad;
                float gz = waveZ + (float) Math.sin(gasAngle) * gasRad;
                float gy = segY + (gasRand.nextFloat() - 0.5f) * boneSize;
                float gasSize = 0.075f + gasRand.nextFloat() * 0.065f;
                drawBox(pose, consumer, gx - gasSize, gy - gasSize, gz - gasSize, gx + gasSize, gy + gasSize, gz + gasSize, 0.04f, 0.38f, 0.14f, 0.20f, light);
            }
        }
    }
}
