package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class ChainedForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Chained";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Full distorted humanoid figure wrapped in black bandages — straining to burst free

        // Bandaged legs
        drawBox(pose, consumer, 0.06f, 0.0f, -0.16f, 0.36f, 1.0f, 0.16f, 0.12f, 0.12f, 0.14f, 0.96f, light);
        drawBox(pose, consumer, -0.36f, 0.0f, -0.16f, -0.06f, 1.0f, 0.16f, 0.12f, 0.12f, 0.14f, 0.96f, light);
        // Torso (bandaged, slightly bulging)
        drawBox(pose, consumer, -0.38f, 1.0f, -0.25f, 0.38f, 2.0f, 0.25f, 0.13f, 0.13f, 0.15f, 0.96f, light);
        // Arms
        drawBox(pose, consumer, 0.38f, 1.05f, -0.16f, 0.66f, 2.0f, 0.16f, 0.12f, 0.12f, 0.14f, 0.94f, light);
        drawBox(pose, consumer, -0.66f, 1.05f, -0.16f, -0.38f, 2.0f, 0.16f, 0.12f, 0.12f, 0.14f, 0.94f, light);
        // Head
        drawBox(pose, consumer, -0.20f, 2.0f, -0.20f, 0.20f, 2.55f, 0.20f, 0.12f, 0.12f, 0.14f, 0.96f, light);

        // Grotesque animalistic fur/fang/eye protrusions constantly bursting outward
        int burstCount = 12;
        for (int i = 0; i < burstCount; i++) {
            Random rand = new Random(i * 9999L);
            float pulseSpeed = 0.1f + rand.nextFloat() * 0.15f;
            float pulse = 0.1f + (float) Math.sin(time * pulseSpeed + i) * 0.22f;
            float by = 0.15f + i * 0.2f;
            float angle = rand.nextFloat() * (float) (Math.PI * 2.0);
            // Radius follows body silhouette
            float bodyR = by < 1.0f ? 0.25f : 0.38f;
            float bx = (float) Math.cos(angle) * (bodyR + pulse);
            float bz = (float) Math.sin(angle) * (bodyR + pulse);

            // Reddish-brown fur protrusion
            float prSize = 0.052f + rand.nextFloat() * 0.05f;
            drawBox(pose, consumer, bx - prSize, by - prSize, bz - prSize, bx + prSize, by + prSize, bz + prSize, 0.38f, 0.2f, 0.1f, 0.92f, light);

            // Ivory fangs on every 3rd protrusion
            if (i % 3 == 0) {
                float fx = bx * 1.12f, fz = bz * 1.12f;
                drawBox(pose, consumer, fx - 0.014f, by - 0.032f, fz - 0.014f, fx + 0.014f, by + 0.032f, fz + 0.014f, 0.9f, 0.88f, 0.8f, 0.92f, light);
            }

            // Bloodshot eyes on alternating protrusions
            if (i % 2 == 0) {
                float ex = bx * 1.06f, ez = bz * 1.06f;
                float eyePulse = 0.58f + (float) Math.sin(time * 0.1f + i) * 0.42f;
                drawBox(pose, consumer, ex - 0.022f, by - 0.022f, ez - 0.022f, ex + 0.022f, by + 0.022f, ez + 0.022f, 0.85f, 0.2f, 0.2f, eyePulse, light);
                drawBox(pose, consumer, ex - 0.011f, by - 0.011f, ez - 0.024f, ex + 0.011f, by + 0.011f, ez + 0.024f, 0.05f, 0.05f, 0.05f, eyePulse, light);
            }
        }

        // 4 heavy rust-colored iron chain rings (one per body region)
        float[] chainHeights = {0.32f, 0.72f, 1.32f, 1.72f};
        for (float cy : chainHeights) {
            float cw = cy < 1.0f ? 0.30f : 0.44f; // width follows body
            drawBox(pose, consumer, -cw, cy - 0.042f, cw, cw, cy + 0.042f, cw + 0.032f, 0.46f, 0.26f, 0.15f, 0.96f, light);
            drawBox(pose, consumer, -cw, cy - 0.042f, -cw - 0.032f, cw, cy + 0.042f, -cw, 0.46f, 0.26f, 0.15f, 0.96f, light);
            drawBox(pose, consumer, cw, cy - 0.042f, -cw, cw + 0.032f, cy + 0.042f, cw, 0.46f, 0.26f, 0.15f, 0.96f, light);
            drawBox(pose, consumer, -cw - 0.032f, cy - 0.042f, -cw, -cw, cy + 0.042f, cw, 0.46f, 0.26f, 0.15f, 0.96f, light);
        }
    }
}
