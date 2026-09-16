package dev.ua.ikeepcalm.coi.client.mcf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Hand-authored cuboid geometry helpers shared by {@link MythicalCreatureForm} (full-body
 * replacement) and the passive additive appearance trait renderer — both submit raw
 * vertex-colored boxes against a blank texture via the same {@code submitCustomGeometry}
 * pipeline.
 */
public interface Coi3dPrimitives {

    default void drawBox(PoseStack.Pose entry, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a, int light) {
        // Front face
        addVertex(entry, consumer, minX, minY, maxZ, r, g, b, a, 0, 0, 0, 0, 1, light);
        addVertex(entry, consumer, maxX, minY, maxZ, r, g, b, a, 1, 0, 0, 0, 1, light);
        addVertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a, 1, 1, 0, 0, 1, light);
        addVertex(entry, consumer, minX, maxY, maxZ, r, g, b, a, 0, 1, 0, 0, 1, light);

        // Back face
        addVertex(entry, consumer, minX, minY, minZ, r, g, b, a, 0, 0, 0, 0, -1, light);
        addVertex(entry, consumer, minX, maxY, minZ, r, g, b, a, 0, 1, 0, 0, -1, light);
        addVertex(entry, consumer, maxX, maxY, minZ, r, g, b, a, 1, 1, 0, 0, -1, light);
        addVertex(entry, consumer, maxX, minY, minZ, r, g, b, a, 1, 0, 0, 0, -1, light);

        // Top face
        addVertex(entry, consumer, minX, maxY, minZ, r, g, b, a, 0, 0, 0, 1, 0, light);
        addVertex(entry, consumer, minX, maxY, maxZ, r, g, b, a, 0, 1, 0, 1, 0, light);
        addVertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a, 1, 1, 0, 1, 0, light);
        addVertex(entry, consumer, maxX, maxY, minZ, r, g, b, a, 1, 0, 0, 1, 0, light);

        // Bottom face
        addVertex(entry, consumer, minX, minY, minZ, r, g, b, a, 0, 0, 0, -1, 0, light);
        addVertex(entry, consumer, maxX, minY, minZ, r, g, b, a, 1, 0, 0, -1, 0, light);
        addVertex(entry, consumer, maxX, minY, maxZ, r, g, b, a, 1, 1, 0, -1, 0, light);
        addVertex(entry, consumer, minX, minY, maxZ, r, g, b, a, 0, 1, 0, -1, 0, light);

        // Right face
        addVertex(entry, consumer, maxX, minY, minZ, r, g, b, a, 0, 0, 1, 0, 0, light);
        addVertex(entry, consumer, maxX, maxY, minZ, r, g, b, a, 0, 1, 1, 0, 0, light);
        addVertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a, 1, 1, 1, 0, 0, light);
        addVertex(entry, consumer, maxX, minY, maxZ, r, g, b, a, 1, 0, 1, 0, 0, light);

        // Left face
        addVertex(entry, consumer, minX, minY, minZ, r, g, b, a, 0, 0, -1, 0, 0, light);
        addVertex(entry, consumer, minX, minY, maxZ, r, g, b, a, 1, 0, -1, 0, 0, light);
        addVertex(entry, consumer, minX, maxY, maxZ, r, g, b, a, 1, 1, -1, 0, 0, light);
        addVertex(entry, consumer, minX, maxY, minZ, r, g, b, a, 0, 1, -1, 0, 0, light);
    }

    default void addVertex(PoseStack.Pose entry, VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float a, float u, float v, float nx, float ny, float nz, int light) {
        consumer.addVertex(entry, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }

    default void addTexturedTriangle(
            PoseStack.Pose entry,
            VertexConsumer consumer,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            int light
    ) {
        float[] normal = faceNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        addVertex(entry, consumer, x0, y0, z0, 1, 1, 1, 1, u0, v0, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x1, y1, z1, 1, 1, 1, 1, u1, v1, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x2, y2, z2, 1, 1, 1, 1, u2, v2, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x2, y2, z2, 1, 1, 1, 1, u2, v2, normal[0], normal[1], normal[2], light);
    }

    default void addTexturedQuad(
            PoseStack.Pose entry,
            VertexConsumer consumer,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            int light
    ) {
        float[] normal = faceNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        addVertex(entry, consumer, x0, y0, z0, 1, 1, 1, 1, u0, v0, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x1, y1, z1, 1, 1, 1, 1, u1, v1, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x2, y2, z2, 1, 1, 1, 1, u2, v2, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x3, y3, z3, 1, 1, 1, 1, u3, v3, normal[0], normal[1], normal[2], light);
    }

    /**
     * A single triangle, submitted as a quad with its last vertex duplicated (a common,
     * cheap way to get a triangle through a quad-based vertex consumer). The face normal is
     * derived from the winding order, so tapered/pointed shapes (pyramids, cones) light
     * correctly without needing a separate normal per call site.
     */
    default void addTriangle(PoseStack.Pose entry, VertexConsumer consumer, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a, int light) {
        float[] normal = faceNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        addVertex(entry, consumer, x0, y0, z0, r, g, b, a, 0, 0, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x1, y1, z1, r, g, b, a, 1, 0, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x2, y2, z2, r, g, b, a, 1, 1, normal[0], normal[1], normal[2], light);
        addVertex(entry, consumer, x2, y2, z2, r, g, b, a, 0, 1, normal[0], normal[1], normal[2], light);
    }

    private static float[] faceNormal(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2) {
        float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        float vx = x2 - x0, vy = y2 - y0, vz = z2 - z0;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0e-5f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return new float[]{nx, ny, nz};
    }

}
