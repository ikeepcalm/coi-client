package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class HangedForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Hanged";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Massive pitch-black humanoid shadow silhouette
        // Legs
        drawBox(pose, consumer, 0.06f, 0.0f, -0.17f, 0.38f, 1.0f, 0.17f, 0.04f, 0.02f, 0.04f, 0.94f, light);
        drawBox(pose, consumer, -0.38f, 0.0f, -0.17f, -0.06f, 1.0f, 0.17f, 0.04f, 0.02f, 0.04f, 0.94f, light);
        // Torso
        drawBox(pose, consumer, -0.44f, 1.0f, -0.27f, 0.44f, 2.0f, 0.27f, 0.04f, 0.02f, 0.04f, 0.94f, light);
        // Arms (raised/outstretched slightly)
        drawBox(pose, consumer, 0.44f, 1.05f, -0.17f, 0.74f, 2.0f, 0.17f, 0.04f, 0.02f, 0.04f, 0.92f, light);
        drawBox(pose, consumer, -0.74f, 1.05f, -0.17f, -0.44f, 2.0f, 0.17f, 0.04f, 0.02f, 0.04f, 0.92f, light);
        // Head (large, looming)
        drawBox(pose, consumer, -0.26f, 2.0f, -0.26f, 0.26f, 2.62f, 0.26f, 0.04f, 0.02f, 0.04f, 0.94f, light);

        // Massive upside-down cross on the BACK (+Z = behind the creature)
        float crossY = 1.35f + (float) Math.sin(time * 0.05f) * 0.04f;
        float crossZ = 0.30f;
        // Vertical beam (longer below the crossbar = upside-down)
        drawBox(pose, consumer, -0.068f, crossY - 0.88f, crossZ - 0.04f, 0.068f, crossY + 0.46f, crossZ + 0.04f, 0.22f, 0.03f, 0.03f, 0.94f, light);
        // Horizontal crossbar (positioned near the top third)
        drawBox(pose, consumer, -0.55f, crossY + 0.18f, crossZ - 0.05f, 0.55f, crossY + 0.33f, crossZ + 0.05f, 0.22f, 0.03f, 0.03f, 0.94f, light);

        // Grotesque shadow chains wrapping the entire humanoid form
        // 18 ring layers ascending from feet to head
        for (int i = 0; i < 18; i++) {
            float cy = 0.08f + i * 0.144f;
            // Radius follows body silhouette
            float wrapRadius = cy < 1.0f ? 0.22f : 0.48f;
            int links = 12;
            for (int l = 0; l < links; l++) {
                float angle = (float) (l * (Math.PI * 2.0 / links)) + i * 0.28f + time * 0.028f;
                float cx = (float) Math.cos(angle) * wrapRadius;
                float cz = (float) Math.sin(angle) * wrapRadius;
                float chainSize = 0.036f;
                drawBox(pose, consumer, cx - chainSize, cy - chainSize, cz - chainSize, cx + chainSize, cy + chainSize, cz + chainSize, 0.12f, 0.12f, 0.13f, 0.84f, light);
            }
        }

        // Swirling blood stains, crying spirit faces, and dark corruption symbols
        int particleCount = 32;
        for (int i = 0; i < particleCount; i++) {
            Random rand = new Random(i * 333L);
            float phase = rand.nextFloat() * 100f;
            float speed = 0.38f + rand.nextFloat() * 0.82f;
            float pTime = time * 0.05f * speed + phase;
            float radius = 0.50f + rand.nextFloat() * 0.28f;
            float px = (float) Math.cos(pTime) * radius;
            float pz = (float) Math.sin(pTime) * radius;
            float py = 0.08f + (pTime * 0.28f % 2.5f);
            boolean isCrimson = rand.nextFloat() > 0.4f;
            float pr = isCrimson ? 0.72f : 0.08f;
            float pg = 0.02f;
            float pb = isCrimson ? 0.02f : 0.14f;
            float pSize = 0.02f + rand.nextFloat() * 0.022f;
            drawBox(pose, consumer, px - pSize, py - pSize, pz - pSize, px + pSize, py + pSize, pz + pSize, pr, pg, pb, 0.75f, light);
        }
    }
}
