package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class GiantForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Giant";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float glowPulse = 0.28f + (float) Math.sin(time * 0.04f) * 0.14f;

        // Colossal armored giant — full humanoid with impending-sunset glow bloom

        // Iron-gray armored legs
        drawBox(pose, consumer, 0.07f, 0.0f, -0.22f, 0.40f, 1.0f, 0.22f, 0.33f, 0.33f, 0.35f, 0.95f, light);
        drawBox(pose, consumer, -0.40f, 0.0f, -0.22f, -0.07f, 1.0f, 0.22f, 0.33f, 0.33f, 0.35f, 0.95f, light);
        // Sunset bloom on legs
        drawBox(pose, consumer, 0.06f, -0.01f, -0.23f, 0.41f, 1.01f, 0.23f, 0.9f, 0.36f, 0.05f, glowPulse, light);
        drawBox(pose, consumer, -0.41f, -0.01f, -0.23f, -0.06f, 1.01f, 0.23f, 0.9f, 0.36f, 0.05f, glowPulse, light);

        // Armored torso (chestplate)
        drawBox(pose, consumer, -0.46f, 1.0f, -0.34f, 0.46f, 2.2f, 0.34f, 0.33f, 0.33f, 0.35f, 0.95f, light);
        drawBox(pose, consumer, -0.48f, 0.98f, -0.36f, 0.48f, 2.22f, 0.36f, 0.9f, 0.36f, 0.05f, glowPulse, light);

        // Armored arms
        drawBox(pose, consumer, 0.46f, 1.0f, -0.24f, 0.76f, 1.75f, 0.24f, 0.32f, 0.32f, 0.34f, 0.93f, light);
        drawBox(pose, consumer, -0.76f, 1.0f, -0.24f, -0.46f, 1.75f, 0.24f, 0.32f, 0.32f, 0.34f, 0.93f, light);
        drawBox(pose, consumer, 0.44f, 0.98f, -0.25f, 0.78f, 1.77f, 0.25f, 0.9f, 0.36f, 0.05f, glowPulse * 0.8f, light);
        drawBox(pose, consumer, -0.78f, 0.98f, -0.25f, -0.44f, 1.77f, 0.25f, 0.9f, 0.36f, 0.05f, glowPulse * 0.8f, light);

        // Giant pauldrons (shoulder plates)
        drawBox(pose, consumer, 0.44f, 1.75f, -0.38f, 0.84f, 2.22f, 0.38f, 0.30f, 0.30f, 0.32f, 0.95f, light);
        drawBox(pose, consumer, 0.42f, 1.73f, -0.40f, 0.86f, 2.24f, 0.40f, 0.9f, 0.36f, 0.05f, glowPulse, light);
        drawBox(pose, consumer, -0.84f, 1.75f, -0.38f, -0.44f, 2.22f, 0.38f, 0.30f, 0.30f, 0.32f, 0.95f, light);
        drawBox(pose, consumer, -0.86f, 1.73f, -0.40f, -0.42f, 2.24f, 0.40f, 0.9f, 0.36f, 0.05f, glowPulse, light);

        // Helmet/Head
        drawBox(pose, consumer, -0.25f, 2.2f, -0.25f, 0.25f, 2.72f, 0.25f, 0.35f, 0.35f, 0.37f, 0.95f, light);
        drawBox(pose, consumer, -0.27f, 2.18f, -0.27f, 0.27f, 2.74f, 0.27f, 0.9f, 0.36f, 0.05f, glowPulse, light);
        // Glowing sunset visor slit (front = -Z)
        drawBox(pose, consumer, -0.18f, 2.42f, -0.26f, 0.18f, 2.50f, -0.24f, 1.0f, 0.44f, 0.0f, 0.94f, light);

        // Decaying ancient runes and falling ash particles
        int runeCount = 22;
        for (int i = 0; i < runeCount; i++) {
            Random rand = new Random(i * 5555L);
            float fallSpeed = 0.5f + rand.nextFloat() * 0.5f;
            float fallTime = time * 0.02f * fallSpeed + rand.nextFloat() * 12f;
            float currentY = rand.nextFloat() * 2.5f - (fallTime % 2.5f);
            if (currentY < 0.0f) currentY += 2.5f;

            float angle = rand.nextFloat() * (float) (Math.PI * 2.0);
            float radius = 0.52f + rand.nextFloat() * 0.28f;
            float rx = (float) Math.cos(angle) * radius;
            float rz = (float) Math.sin(angle) * radius;
            float runeSize = 0.024f;
            float decayType = rand.nextFloat();
            float rr = decayType > 0.4f ? 0.9f : 0.38f;
            float rg = decayType > 0.4f ? 0.38f : 0.33f;
            float rb = decayType > 0.4f ? 0.08f : 0.33f;
            float alpha = 0.38f + rand.nextFloat() * 0.42f;
            drawBox(pose, consumer, rx - runeSize, currentY - runeSize, rz - runeSize, rx + runeSize, currentY + runeSize, rz + runeSize, rr, rg, rb, alpha, light);
        }
    }
}
