package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.Coi3dPrimitives;

public final class TraitGeometry implements Coi3dPrimitives {

    public static final TraitGeometry INSTANCE = new TraitGeometry();

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float TAU = (float) (Math.PI * 2.0);

    private TraitGeometry() {
    }

    public Point pointPixels(float x, float y, float z) {
        return new Point(x * PIXEL, y * PIXEL, z * PIXEL);
    }

    /**
     * Six-face box in part-local pixel space with a single tint — the workhorse for
     * hair slabs, side locks and fringe segments. Drawn closed so silhouettes read
     * from every angle.
     */
    public void boxPixels(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            Tint tint,
            int light
    ) {
        quad(pose, consumer, pointPixels(minX, minY, minZ), pointPixels(maxX, minY, minZ),
                pointPixels(maxX, maxY, minZ), pointPixels(minX, maxY, minZ), tint, light);
        quad(pose, consumer, pointPixels(minX, minY, maxZ), pointPixels(minX, maxY, maxZ),
                pointPixels(maxX, maxY, maxZ), pointPixels(maxX, minY, maxZ), tint, light);
        quad(pose, consumer, pointPixels(minX, maxY, minZ), pointPixels(maxX, maxY, minZ),
                pointPixels(maxX, maxY, maxZ), pointPixels(minX, maxY, maxZ), tint, light);
        quad(pose, consumer, pointPixels(minX, minY, minZ), pointPixels(minX, minY, maxZ),
                pointPixels(maxX, minY, maxZ), pointPixels(maxX, minY, minZ), tint, light);
        quad(pose, consumer, pointPixels(maxX, minY, minZ), pointPixels(maxX, minY, maxZ),
                pointPixels(maxX, maxY, maxZ), pointPixels(maxX, maxY, minZ), tint, light);
        quad(pose, consumer, pointPixels(minX, minY, minZ), pointPixels(minX, maxY, minZ),
                pointPixels(minX, maxY, maxZ), pointPixels(minX, minY, maxZ), tint, light);
    }

    public void drawTube(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Point[] path,
            float[] radiiPixels,
            int sides,
            Tint[] colors,
            int light
    ) {
        Point[][] rings = new Point[path.length][sides];
        Vec previousAxisV = null;
        for (int index = 0; index < path.length; index++) {
            Vec tangent;
            if (index == 0) {
                tangent = path[1].subtract(path[0]).normalize();
            } else if (index == path.length - 1) {
                tangent = path[index].subtract(path[index - 1]).normalize();
            } else {
                tangent = path[index + 1].subtract(path[index - 1]).normalize();
            }

            Vec reference = Math.abs(tangent.y()) > 0.82f ? new Vec(0, 0, 1) : new Vec(0, 1, 0);
            // Carry the previous frame along the curve instead of flipping at a tangent threshold.
            Vec axisU = previousAxisV == null ? tangent.cross(reference).normalize()
                    : previousAxisV.cross(tangent).normalize();
            Vec axisV = tangent.cross(axisU).normalize();
            previousAxisV = axisV;
            float radius = radiiPixels[index] * PIXEL;
            for (int side = 0; side < sides; side++) {
                float angle = TAU * side / sides;
                Vec offset = axisU.scale((float) Math.cos(angle) * radius)
                        .add(axisV.scale((float) Math.sin(angle) * radius));
                rings[index][side] = path[index].add(offset);
            }
        }

        for (int segment = 0; segment < path.length - 1; segment++) {
            Tint tint = colors[Math.min(segment, colors.length - 1)];
            for (int side = 0; side < sides; side++) {
                int next = (side + 1) % sides;
                quad(
                        pose,
                        consumer,
                        rings[segment][side],
                        rings[segment][next],
                        rings[segment + 1][next],
                        rings[segment + 1][side],
                        tint,
                        light
                );
            }
        }

        Point end = path[path.length - 1];
        Tint endTint = colors[colors.length - 1];
        for (int side = 0; side < sides; side++) {
            int next = (side + 1) % sides;
            triangle(pose, consumer, end, rings[path.length - 1][side], rings[path.length - 1][next], endTint, light);
        }
    }

    public void horizontalQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float minXPixels,
            float maxXPixels,
            float yPixels,
            float minZPixels,
            float maxZPixels,
            Tint tint,
            int light
    ) {
        quad(
                pose,
                consumer,
                pointPixels(minXPixels, yPixels, minZPixels),
                pointPixels(maxXPixels, yPixels, minZPixels),
                pointPixels(maxXPixels, yPixels, maxZPixels),
                pointPixels(minXPixels, yPixels, maxZPixels),
                tint,
                light
        );
    }

    public void triangle(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Point a,
            Point b,
            Point c,
            Tint tint,
            int light
    ) {
        Vec normal = b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(pose, consumer, a, tint, normal, 0, 0, light);
        vertex(pose, consumer, b, tint, normal, 1, 0, light);
        vertex(pose, consumer, c, tint, normal, 1, 1, light);
        vertex(pose, consumer, c, tint, normal, 0, 1, light);
    }

    public void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Point a,
            Point b,
            Point c,
            Point d,
            Tint tint,
            int light
    ) {
        Vec normal = b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(pose, consumer, a, tint, normal, 0, 0, light);
        vertex(pose, consumer, b, tint, normal, 1, 0, light);
        vertex(pose, consumer, c, tint, normal, 1, 1, light);
        vertex(pose, consumer, d, tint, normal, 0, 1, light);
    }

    public void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Point point,
            Tint tint,
            Vec normal,
            float u,
            float v,
            int light
    ) {
        addVertex(
                pose,
                consumer,
                point.x(), point.y(), point.z(),
                tint.r(), tint.g(), tint.b(), tint.a(),
                u, v,
                normal.x(), normal.y(), normal.z(),
                light
        );
    }

    public record Point(float x, float y, float z) {

        public Vec subtract(Point other) {
            return new Vec(x - other.x, y - other.y, z - other.z);
        }

        public Point add(Vec vector) {
            return new Point(x + vector.x, y + vector.y, z + vector.z);
        }
    }

    public record Vec(float x, float y, float z) {

        public Vec add(Vec other) {
            return new Vec(x + other.x, y + other.y, z + other.z);
        }

        public Vec scale(float scalar) {
            return new Vec(x * scalar, y * scalar, z * scalar);
        }

        public Vec cross(Vec other) {
            return new Vec(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        public Vec normalize() {
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length < 1.0e-5f) {
                return new Vec(0, 0, -1);
            }
            return new Vec(x / length, y / length, z / length);
        }
    }

    public record Tint(float r, float g, float b, float a) {
    }
}
