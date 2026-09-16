package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class FoolForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Fool";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Translucent purple humanoid silhouette (the spirit body beneath the worms)
        float r = 0.72f, g = 0.5f, b = 0.92f, a = 0.22f;
        drawBox(pose, consumer, 0.05f, 0.0f, -0.15f, 0.35f, 1.0f, 0.15f, r, g, b, a, light);
        drawBox(pose, consumer, -0.35f, 0.0f, -0.15f, -0.05f, 1.0f, 0.15f, r, g, b, a, light);
        drawBox(pose, consumer, -0.38f, 1.0f, -0.22f, 0.38f, 2.0f, 0.22f, r, g, b, a, light);
        drawBox(pose, consumer, 0.38f, 1.05f, -0.14f, 0.66f, 2.0f, 0.14f, r, g, b, a, light);
        drawBox(pose, consumer, -0.66f, 1.05f, -0.14f, -0.38f, 2.0f, 0.14f, r, g, b, a, light);
        drawBox(pose, consumer, -0.22f, 2.0f, -0.22f, 0.22f, 2.55f, 0.22f, r, g, b, a + 0.05f, light);

        // Worms of Spirit orbiting and threading through the humanoid form
        int wormCount = 24;
        for (int i = 0; i < wormCount; i++) {
            java.util.Random rand = new java.util.Random(i * 12345L);
            float baseY = 0.1f + rand.nextFloat() * 2.35f;
            float orbitRadius = 0.25f + rand.nextFloat() * 0.5f;
            float speed = 0.45f + rand.nextFloat() * 1.1f;
            float phase = rand.nextFloat() * 100f;
            float wormTime = time * 0.038f * speed + phase;

            int segments = 7;
            for (int s = 0; s < segments; s++) {
                float sT = wormTime - s * 0.11f;
                float sr = orbitRadius + (float) Math.sin(sT * 3.8f) * 0.05f;
                float sx = (float) Math.cos(sT) * sr;
                float sz = (float) Math.sin(sT) * sr;
                float sy = baseY + (float) Math.sin(sT * 2.3f) * 0.11f;

                float size = 0.048f * (1.0f - s * 0.08f);
                float wr = 0.78f + (float) Math.sin(sT) * 0.1f;
                float wg = 0.52f + (float) Math.cos(sT) * 0.1f;
                float wb = 0.96f;
                float wa = 0.72f - s * 0.06f;
                drawBox(pose, consumer, sx - size, sy - size, sz - size, sx + size, sy + size, sz + size, wr, wg, wb, wa, light);
            }
        }
    }
}
