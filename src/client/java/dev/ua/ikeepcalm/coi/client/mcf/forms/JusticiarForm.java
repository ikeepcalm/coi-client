package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class JusticiarForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Justiciar";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Armored judge silhouette — full humanoid with gold pauldrons and balance scale

        // Silver-white armored legs
        drawBox(pose, consumer, 0.06f, 0.0f, -0.18f, 0.36f, 1.0f, 0.18f, 0.84f, 0.84f, 0.88f, 0.55f, light);
        drawBox(pose, consumer, -0.36f, 0.0f, -0.18f, -0.06f, 1.0f, 0.18f, 0.84f, 0.84f, 0.88f, 0.55f, light);

        // Armored torso
        drawBox(pose, consumer, -0.32f, 1.0f, -0.22f, 0.32f, 2.0f, 0.22f, 0.84f, 0.84f, 0.88f, 0.52f, light);

        // Arms
        drawBox(pose, consumer, 0.32f, 1.05f, -0.16f, 0.58f, 2.0f, 0.16f, 0.82f, 0.82f, 0.86f, 0.52f, light);
        drawBox(pose, consumer, -0.58f, 1.05f, -0.16f, -0.32f, 2.0f, 0.16f, 0.82f, 0.82f, 0.86f, 0.52f, light);

        // Gold pauldrons (large shoulder plates)
        drawBox(pose, consumer, 0.30f, 1.65f, -0.26f, 0.56f, 2.0f, 0.26f, 1.0f, 0.86f, 0.2f, 0.75f, light);
        drawBox(pose, consumer, -0.56f, 1.65f, -0.26f, -0.30f, 2.0f, 0.26f, 1.0f, 0.86f, 0.2f, 0.75f, light);

        // Helmet
        drawBox(pose, consumer, -0.18f, 2.0f, -0.18f, 0.18f, 2.52f, 0.18f, 0.82f, 0.82f, 0.86f, 0.62f, light);

        // Giant balance scale of pure golden light — in FRONT of the judge (-Z = front)
        float scaleZ = -0.38f;
        float scaleBaseY = 0.22f;
        float beamTilt = (float) Math.sin(time * 0.06f) * 0.14f;

        // Vertical central gold post
        drawBox(pose, consumer, -0.042f, scaleBaseY, scaleZ - 0.042f, 0.042f, 2.0f, scaleZ + 0.042f, 1.0f, 0.86f, 0.2f, 0.92f, light);
        // Stand base
        drawBox(pose, consumer, -0.28f, scaleBaseY, scaleZ - 0.28f, 0.28f, scaleBaseY + 0.05f, scaleZ + 0.28f, 1.0f, 0.86f, 0.2f, 0.92f, light);

        // Tilting horizontal beam at top
        float beamLength = 0.82f;
        float rx = beamLength, ry = 1.82f + beamTilt;
        float lx = -beamLength, ly = 1.82f - beamTilt;
        drawBeam(pose, consumer, lx, ly, scaleZ, rx, ry, scaleZ, 1.0f, 0.86f, 0.2f, light);

        // Right pan
        float panSize = 0.20f;
        float rPanY = ry - 0.55f;
        drawBox(pose, consumer, rx - 0.012f, rPanY, scaleZ - 0.012f, rx + 0.012f, ry, scaleZ + 0.012f, 0.9f, 0.8f, 0.2f, 0.72f, light);
        drawBox(pose, consumer, rx - panSize, rPanY - 0.022f, scaleZ - panSize, rx + panSize, rPanY, scaleZ + panSize, 1.0f, 0.86f, 0.2f, 0.88f, light);

        // Left pan
        float lPanY = ly - 0.55f;
        drawBox(pose, consumer, lx - 0.012f, lPanY, scaleZ - 0.012f, lx + 0.012f, ly, scaleZ + 0.012f, 0.9f, 0.8f, 0.2f, 0.72f, light);
        drawBox(pose, consumer, lx - panSize, lPanY - 0.022f, scaleZ - panSize, lx + panSize, lPanY, scaleZ + panSize, 1.0f, 0.86f, 0.2f, 0.88f, light);

        // Fields of absolute order — glowing gold rings at the base
        int ringCount = 3;
        for (int r = 0; r < ringCount; r++) {
            float ringY = 0.18f + r * 0.72f;
            float radius = 0.95f - r * 0.08f;
            float ringPulse = 0.22f + (float) Math.sin(time * 0.08f + r) * 0.1f;
            float rEdge = 0.022f;
            drawBox(pose, consumer, -radius, ringY - 0.01f, -radius, radius, ringY + 0.01f, -radius + rEdge, 1.0f, 0.8f, 0.1f, ringPulse, light);
            drawBox(pose, consumer, -radius, ringY - 0.01f, radius - rEdge, radius, ringY + 0.01f, radius, 1.0f, 0.8f, 0.1f, ringPulse, light);
            drawBox(pose, consumer, -radius, ringY - 0.01f, -radius, -radius + rEdge, ringY + 0.01f, radius, 1.0f, 0.8f, 0.1f, ringPulse, light);
            drawBox(pose, consumer, radius - rEdge, ringY - 0.01f, -radius, radius, ringY + 0.01f, radius, 1.0f, 0.8f, 0.1f, ringPulse, light);
        }
    }

    private void drawBeam(PoseStack.Pose entry, VertexConsumer consumer,
                          float x1, float y1, float z1, float x2, float y2, float z2,
                          float r, float g, float b, int light) {
        float minX = Math.min(x1, x2) - 0.022f, maxX = Math.max(x1, x2) + 0.022f;
        float minY = Math.min(y1, y2) - 0.022f, maxY = Math.max(y1, y2) + 0.022f;
        float minZ = Math.min(z1, z2) - 0.022f, maxZ = Math.max(z1, z2) + 0.022f;
        drawBox(entry, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 0.92f, light);
    }
}
