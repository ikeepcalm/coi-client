package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.Random;

public class HermitForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Hermit";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Full-body tattered gray robe covering a humanoid of infinite forbidden knowledge

        // Feet visible at the base of the robe (toes face front = -Z)
        drawBox(pose, consumer, 0.04f, 0.0f, -0.24f, 0.30f, 0.16f, 0.14f, 0.30f, 0.30f, 0.32f, 0.92f, light);
        drawBox(pose, consumer, -0.30f, 0.0f, -0.24f, -0.04f, 0.16f, 0.14f, 0.30f, 0.30f, 0.32f, 0.92f, light);

        // Robe body (back panel at +Z, sides, sweeping skirt)
        drawBox(pose, consumer, -0.44f, 0.16f, 0.26f, 0.44f, 1.95f, 0.34f, 0.37f, 0.37f, 0.39f, 0.96f, light); // back
        drawBox(pose, consumer, -0.46f, 0.16f, -0.26f, -0.28f, 1.95f, 0.26f, 0.35f, 0.35f, 0.37f, 0.96f, light); // left side
        drawBox(pose, consumer, 0.28f, 0.16f, -0.26f, 0.46f, 1.95f, 0.26f, 0.35f, 0.35f, 0.37f, 0.96f, light);  // right side
        // Sleeves
        drawBox(pose, consumer, -0.64f, 1.1f, -0.18f, -0.28f, 1.85f, 0.18f, 0.36f, 0.36f, 0.38f, 0.95f, light);
        drawBox(pose, consumer, 0.28f, 1.1f, -0.18f, 0.64f, 1.85f, 0.18f, 0.36f, 0.36f, 0.38f, 0.95f, light);

        // Hood (large, tattered)
        drawBox(pose, consumer, -0.25f, 1.88f, -0.25f, 0.25f, 2.38f, 0.25f, 0.34f, 0.34f, 0.36f, 0.96f, light);
        // Hood brim (wider shadow)
        drawBox(pose, consumer, -0.30f, 1.88f, -0.30f, 0.30f, 1.96f, 0.30f, 0.30f, 0.30f, 0.32f, 0.95f, light);

        // Falling purple information streams / ancient symbols (interior front)
        int streamCount = 16;
        for (int i = 0; i < streamCount; i++) {
            Random rand = new Random(i * 4545L);
            float scrollSpeed = 0.75f + rand.nextFloat() * 1.5f;
            float scrollY = 1.78f - ((time * 0.038f * scrollSpeed + rand.nextFloat() * 1.75f) % 1.62f);
            float sx = -0.22f + i * (0.44f / streamCount);
            float sz = -0.06f + (float) Math.sin(time * 0.08f + i) * 0.02f;
            float symSize = 0.02f + rand.nextFloat() * 0.02f;
            float r = 0.38f + rand.nextFloat() * 0.42f;
            float g = 0.05f;
            float b = 0.78f + rand.nextFloat() * 0.22f;
            drawBox(pose, consumer, sx - symSize, scrollY - symSize, sz - symSize, sx + symSize, scrollY + symSize, sz + symSize, r, g, b, 0.82f, light);
        }

        // Rows of blinking purple eyes inside the hood and robe
        int eyeCount = 10;
        for (int i = 0; i < eyeCount; i++) {
            Random rand = new Random(i * 1212L);
            float blinkSpeed = 0.04f + rand.nextFloat() * 0.08f;
            float blinkPulse = 0.5f + (float) Math.sin(time * blinkSpeed + i) * 0.5f;
            float alpha = blinkPulse > 0.3f ? (blinkPulse - 0.3f) / 0.7f : 0.0f;

            float ex, ey, ez;
            if (i < 4) { // eyes inside the hood, facing front (-Z)
                ex = -0.12f + rand.nextFloat() * 0.24f;
                ey = 1.94f + rand.nextFloat() * 0.32f;
                ez = -0.06f;
            } else { // eyes in the robe body interior (+Z side)
                ex = -0.20f + rand.nextFloat() * 0.40f;
                ey = 0.3f + rand.nextFloat() * 1.5f;
                ez = 0.06f;
            }

            float eyeSize = 0.026f;
            drawBox(pose, consumer, ex - eyeSize, ey - eyeSize, ez - eyeSize, ex + eyeSize, ey + eyeSize, ez + eyeSize, 0.68f, 0.1f, 0.92f, alpha, light);
            drawBox(pose, consumer, ex - eyeSize * 0.48f, ey - eyeSize * 0.48f, ez - eyeSize * 0.48f, ex + eyeSize * 0.48f, ey + eyeSize * 0.48f, ez + eyeSize * 0.48f, 1.0f, 0.82f, 1.0f, alpha, light);
        }
    }
}
