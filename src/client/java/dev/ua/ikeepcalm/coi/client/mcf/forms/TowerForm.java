package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class TowerForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Tower";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        float pulse = (float) Math.sin(time * 0.08f) * 0.04f;

        // Humanoid pillar of blinding white/yellow knowledge-light
        // Legs
        drawLightPillar(pose, consumer, 0.05f, 0.0f, -0.15f, 0.35f, 1.0f, 0.15f, pulse, light);
        drawLightPillar(pose, consumer, -0.35f, 0.0f, -0.15f, -0.05f, 1.0f, 0.15f, pulse, light);
        // Torso (slightly wider at shoulders to suggest an imposing figure)
        drawLightPillar(pose, consumer, -0.40f, 1.0f, -0.24f, 0.40f, 2.0f, 0.24f, pulse, light);
        // Arms
        drawLightPillar(pose, consumer, 0.40f, 1.05f, -0.15f, 0.70f, 2.0f, 0.15f, pulse, light);
        drawLightPillar(pose, consumer, -0.70f, 1.05f, -0.15f, -0.40f, 2.0f, 0.15f, pulse, light);
        // Head (brightest node)
        drawLightPillar(pose, consumer, -0.24f, 2.0f, -0.24f, 0.24f, 2.58f, 0.24f, pulse + 0.05f, light);

        // Swirling symbols and text in a double-helix path spiraling around the entire form
        int symbolCount = 58;
        for (int i = 0; i < symbolCount; i++) {
            Random rand = new Random(i * 314159L);
            float spiralSpeed = 0.6f + rand.nextFloat() * 1.2f;
            float spiralPhase = rand.nextFloat() * 100f;

            float symTime = time * 0.024f * spiralSpeed + spiralPhase;
            float height = (rand.nextFloat() * 2.55f + time * 0.007f * spiralSpeed) % 2.55f;

            // Radius follows the silhouette: narrow at legs/head, wider at torso
            float bodyRadius;
            if (height < 1.0f) bodyRadius = 0.22f;
            else if (height > 2.0f) bodyRadius = 0.26f;
            else bodyRadius = 0.44f;
            float outerRadius = bodyRadius + 0.14f + (float) Math.sin(time * 0.04f + i) * 0.07f;

            float angle = symTime * 2.3f + (float) (i % 2 == 0 ? 0 : Math.PI);
            float sx = (float) Math.cos(angle) * outerRadius;
            float sz = (float) Math.sin(angle) * outerRadius;

            float r = 0.88f + rand.nextFloat() * 0.12f;
            float g = 0.88f + rand.nextFloat() * 0.12f;
            float b = 0.70f + rand.nextFloat() * 0.30f;
            float alpha = 0.52f + rand.nextFloat() * 0.38f;
            float symSize = 0.022f + rand.nextFloat() * 0.022f;

            drawBox(pose, consumer, sx - symSize, height - symSize, sz - symSize, sx + symSize, height + symSize, sz + symSize, r, g, b, alpha, light);
        }
    }

    private void drawLightPillar(PoseStack.Pose pose, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float pulse, int light) {
        drawBox(pose, consumer, x1, y1, z1, x2, y2, z2, 1.0f, 1.0f, 0.92f, 0.42f + pulse, light);
        float s = 0.055f;
        drawBox(pose, consumer, x1 + s, y1 + s, z1 + s, x2 - s, y2 - s, z2 - s, 1.0f, 1.0f, 1.0f, 0.75f + pulse, light);
    }
}
