package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class EmperorForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Emperor";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Towering shadow-infused imperial robe — full humanoid

        // Robed legs visible beneath the skirt
        drawBox(pose, consumer, 0.06f, 0.0f, -0.22f, 0.38f, 0.58f, 0.22f, 0.06f, 0.03f, 0.09f, 0.94f, light);
        drawBox(pose, consumer, -0.38f, 0.0f, -0.22f, -0.06f, 0.58f, 0.22f, 0.06f, 0.03f, 0.09f, 0.94f, light);

        // Large outer robe skirt (billowing shadow)
        drawBox(pose, consumer, -0.48f, 0.58f, -0.48f, 0.48f, 1.08f, 0.48f, 0.06f, 0.03f, 0.09f, 0.96f, light);

        // Upper robe body
        drawBox(pose, consumer, -0.36f, 1.08f, -0.35f, 0.36f, 2.02f, 0.35f, 0.08f, 0.04f, 0.13f, 0.96f, light);

        // Majestic wide pauldrons
        drawBox(pose, consumer, 0.34f, 1.55f, -0.38f, 0.58f, 2.0f, 0.38f, 0.05f, 0.02f, 0.08f, 0.96f, light);
        drawBox(pose, consumer, -0.58f, 1.55f, -0.38f, -0.34f, 2.0f, 0.38f, 0.05f, 0.02f, 0.08f, 0.96f, light);

        // Gold trims on the robe front (imperial aesthetic, front = -Z)
        drawBox(pose, consumer, -0.50f, 0.58f, -0.50f, -0.44f, 1.08f, -0.46f, 0.92f, 0.76f, 0.15f, 0.92f, light);
        drawBox(pose, consumer, 0.44f, 0.58f, -0.50f, 0.50f, 1.08f, -0.46f, 0.92f, 0.76f, 0.15f, 0.92f, light);
        drawBox(pose, consumer, -0.38f, 1.08f, -0.37f, -0.32f, 2.02f, -0.33f, 0.92f, 0.76f, 0.15f, 0.92f, light);
        drawBox(pose, consumer, 0.32f, 1.08f, -0.37f, 0.38f, 2.02f, -0.33f, 0.92f, 0.76f, 0.15f, 0.92f, light);
        // Gold collar
        drawBox(pose, consumer, -0.28f, 2.00f, -0.28f, 0.28f, 2.06f, 0.28f, 0.92f, 0.76f, 0.15f, 0.92f, light);

        // Head cover (deep shadow)
        drawBox(pose, consumer, -0.18f, 2.02f, -0.18f, 0.18f, 2.38f, 0.18f, 0.02f, 0.02f, 0.02f, 0.96f, light);

        // Floating spiked golden crown (hovering slightly above the head)
        float crownY = 2.42f + (float) Math.sin(time * 0.05f) * 0.035f;
        drawBox(pose, consumer, -0.16f, crownY, -0.16f, 0.16f, crownY + 0.045f, 0.16f, 1.0f, 0.86f, 0.2f, 0.96f, light);
        // 5 crown spikes
        drawBox(pose, consumer, -0.015f, crownY + 0.045f, -0.16f, 0.015f, crownY + 0.175f, -0.14f, 1.0f, 0.86f, 0.2f, 0.96f, light); // front (tallest)
        drawBox(pose, consumer, -0.15f, crownY + 0.045f, -0.15f, -0.13f, crownY + 0.13f, -0.13f, 1.0f, 0.86f, 0.2f, 0.96f, light);
        drawBox(pose, consumer, 0.13f, crownY + 0.045f, -0.15f, 0.15f, crownY + 0.13f, -0.13f, 1.0f, 0.86f, 0.2f, 0.96f, light);
        drawBox(pose, consumer, -0.15f, crownY + 0.045f, 0.13f, -0.13f, crownY + 0.13f, 0.15f, 1.0f, 0.86f, 0.2f, 0.96f, light);
        drawBox(pose, consumer, 0.13f, crownY + 0.045f, 0.13f, 0.15f, crownY + 0.13f, 0.15f, 1.0f, 0.86f, 0.2f, 0.96f, light);

        // Distorted law / authority rings expanding from the base
        int ringCount = 3;
        for (int r = 0; r < ringCount; r++) {
            float ringPhase = (time * 0.022f + r * 0.42f) % 1.0f;
            float radius = 0.3f + ringPhase * 1.2f;
            float ringY = 0.06f;
            float alpha = 0.52f * (1.0f - ringPhase);
            float rEdge = 0.022f;
            drawBox(pose, consumer, -radius, ringY - 0.01f, -radius, radius, ringY + 0.01f, -radius + rEdge, 0.72f, 0.1f, 0.92f, alpha, light);
            drawBox(pose, consumer, -radius, ringY - 0.01f, radius - rEdge, radius, ringY + 0.01f, radius, 0.72f, 0.1f, 0.92f, alpha, light);
            drawBox(pose, consumer, -radius, ringY - 0.01f, -radius, -radius + rEdge, ringY + 0.01f, radius, 0.72f, 0.1f, 0.92f, alpha, light);
            drawBox(pose, consumer, radius - rEdge, ringY - 0.01f, -radius, radius, ringY + 0.01f, radius, 0.72f, 0.1f, 0.92f, alpha, light);
        }

        // Dark authority particles drifting upward alongside the robe
        for (int i = 0; i < 10; i++) {
            Random rand = new Random(i * 9911L);
            float gSpeed = 0.5f + rand.nextFloat();
            float gTime = time * 0.045f * gSpeed + rand.nextFloat() * 100f;
            float gy = 0.18f + (gTime % 2.25f);
            float gradius = 0.55f + rand.nextFloat() * 0.35f;
            float gx = (rand.nextBoolean() ? 1 : -1) * gradius;
            float gz = (rand.nextBoolean() ? 1 : -1) * (0.1f + rand.nextFloat() * 0.4f);
            float gSize = 0.024f + rand.nextFloat() * 0.024f;
            drawBox(pose, consumer, gx - gSize, gy - gSize, gz - gSize, gx + gSize, gy + gSize, gz + gSize, 0.42f, 0.05f, 0.62f, 0.62f, light);
        }
    }
}
