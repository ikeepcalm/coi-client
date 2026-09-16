package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class SunForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Sun";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float pulse = (float) Math.sin(time * 0.12f) * 0.05f;
        float legPulse = (float) Math.sin(time * 0.14f) * 0.04f;

        // Blazing humanoid form — each body part is layered fire (outer red → mid orange → inner white-hot)

        // Fire legs
        drawFirePart(pose, consumer, 0.05f - legPulse, 0.0f, -0.18f, 0.37f + legPulse, 1.0f, 0.18f, light, false);
        drawFirePart(pose, consumer, -0.37f - legPulse, 0.0f, -0.18f, -0.05f + legPulse, 1.0f, 0.18f, light, false);

        // Fire torso (largest, densest fire mass)
        drawFirePart(pose, consumer, -0.44f + pulse, 1.0f, -0.28f, 0.44f - pulse, 2.0f, 0.28f, light, true);

        // Fire arms (plumes)
        drawFirePart(pose, consumer, 0.44f + pulse, 1.05f, -0.18f, 0.74f - pulse, 1.95f, 0.18f, light, false);
        drawFirePart(pose, consumer, -0.74f - pulse, 1.05f, -0.18f, -0.44f + pulse, 1.95f, 0.18f, light, false);

        // Sun-head (blinding white-hot orb)
        float hP = (float) Math.sin(time * 0.1f) * 0.04f;
        drawBox(pose, consumer, -0.30f + hP, 2.0f, -0.30f + hP, 0.30f - hP, 2.6f, 0.30f - hP, 1.0f, 0.65f, 0.08f, 0.22f, light);
        drawBox(pose, consumer, -0.24f, 2.05f, -0.24f, 0.24f, 2.55f, 0.24f, 1.0f, 0.88f, 0.42f, 0.42f, light);
        drawBox(pose, consumer, -0.15f, 2.12f, -0.15f, 0.15f, 2.48f, 0.15f, 1.0f, 1.0f, 0.92f, 0.72f, light);

        // Swirling abstract single-cell organisms (symbols) inside the torso
        int organisms = 16;
        for (int i = 0; i < organisms; i++) {
            Random rand = new Random(i * 98765L);
            float speed = 0.48f + rand.nextFloat();
            float phase = rand.nextFloat() * 100f;
            float oAngle = time * 0.05f * speed + phase;
            float oH = 1.1f + rand.nextFloat() * 1.35f;
            float oRad = 0.07f + rand.nextFloat() * 0.2f;
            float ox = (float) Math.cos(oAngle) * oRad;
            float oz = (float) Math.sin(oAngle) * oRad;
            float orgSize = 0.028f;
            drawBox(pose, consumer, ox - orgSize, oH - orgSize, oz - orgSize, ox + orgSize, oH + orgSize, oz + orgSize, 1.0f, 0.96f, 0.62f, 0.8f, light);
        }

        // Raining golden liquid drops falling from the blazing form
        int dropCount = 20;
        for (int i = 0; i < dropCount; i++) {
            Random rand = new Random(i * 12345L);
            float fallSpeed = 1.4f + rand.nextFloat() * 2.0f;
            float dropTime = time * 0.02f * fallSpeed;
            float yOffset = (dropTime + rand.nextFloat()) % 2.4f;
            float dy = 2.2f - yOffset;
            float angle = rand.nextFloat() * (float) (Math.PI * 2.0);
            float rad = rand.nextFloat() * 0.58f;
            float dx = (float) Math.cos(angle) * rad;
            float dz2 = (float) Math.sin(angle) * rad;
            float dropW = 0.013f, dropH = 0.055f;
            drawBox(pose, consumer, dx - dropW, dy - dropH, dz2 - dropW, dx + dropW, dy + dropH, dz2 + dropW, 1.0f, 0.85f, 0.2f, 0.65f * (1.0f - yOffset / 2.4f), light);
        }
    }

    private void drawFirePart(PoseStack.Pose pose, VertexConsumer consumer,
                              float x1, float y1, float z1, float x2, float y2, float z2, int light, boolean intense) {
        // Outer inferno (deep red)
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 1.0f, 0.28f, 0.0f, 0.20f, light);
        // Mid orange layer
        float s1 = 0.055f;
        drawBox(pose, consumer, x1 + s1, y1 + s1, z1 + s1, x2 - s1, y2 - s1, z2 - s1, 1.0f, 0.62f, 0.0f, intense ? 0.36f : 0.30f, light);
        // Inner white-yellow core
        float s2 = 0.105f;
        if (x2 - x1 > s2 * 2 && y2 - y1 > s2 * 2 && z2 - z1 > s2 * 2) {
            drawBox(pose, consumer, x1 + s2, y1 + s2, z1 + s2, x2 - s2, y2 - s2, z2 - s2, 1.0f, 0.95f, 0.55f, intense ? 0.54f : 0.38f, light);
        }
    }
}
