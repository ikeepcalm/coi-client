package dev.ua.ikeepcalm.coi.client.appearance;

import dev.ua.ikeepcalm.coi.client.form.FormPrimitives;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Smooth geometry for the additive appearance traits — tubes, quads and triangles in block units,
 * authored in the 16-pixel model grid the vanilla player parts use.
 *
 * <p>Separate from the raw cuboids of {@link FormPrimitives} because traits grow <em>out of</em> a
 * real player model rather than replacing it: a horn or a stalk has to taper and curve to sit
 * convincingly on a skin, which a stack of boxes cannot do.
 */
public class TraitGeometry implements FormPrimitives {

    public static final TraitGeometry INSTANCE = new TraitGeometry();

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float TAU = (float) (Math.PI * 2.0);
    /**
     * How vertical a tangent has to be before the ring frame switches its reference axis. Picking
     * a reference that is nearly parallel to the tangent makes the cross product degenerate and
     * the ring spin arbitrarily, so near-vertical segments (most horns) frame off +Z instead.
     */
    private static final float VERTICAL_TANGENT_LIMIT = 0.82f;

    private TraitGeometry() {
    }

    public Point pointPixels(float x, float y, float z) {
        return new Point(x * PIXEL, y * PIXEL, z * PIXEL);
    }

    /**
     * A tapering tube swept along {@code path}, closed at the far end with a cone of triangles.
     * {@code colors} is indexed per segment and its last entry is reused for any overrun, so a
     * two-colour gradient needs only two entries.
     */
    public void drawTube(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Point[] path,
            float[] radiiPixels,
            int sides,
            Tint[] colors,
            int light
    ) {
        Point[][] rings = buildRings(path, radiiPixels, sides);

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

    /**
     * One ring of {@code sides} points around each path point, perpendicular to the local tangent.
     * Interior points use a central difference so the frame turns smoothly through a bend instead
     * of stepping at each joint.
     */
    private Point[][] buildRings(Point[] path, float[] radiiPixels, int sides) {
        Point[][] rings = new Point[path.length][sides];
        for (int index = 0; index < path.length; index++) {
            Vec tangent;
            if (index == 0) {
                tangent = path[1].subtract(path[0]).normalize();
            } else if (index == path.length - 1) {
                tangent = path[index].subtract(path[index - 1]).normalize();
            } else {
                tangent = path[index + 1].subtract(path[index - 1]).normalize();
            }

            Vec reference = Math.abs(tangent.y()) > VERTICAL_TANGENT_LIMIT ? new Vec(0, 0, 1) : new Vec(0, 1, 0);
            Vec axisU = tangent.cross(reference).normalize();
            Vec axisV = tangent.cross(axisU).normalize();
            float radius = radiiPixels[index] * PIXEL;
            for (int side = 0; side < sides; side++) {
                float angle = TAU * side / sides;
                Vec offset = axisU.scale((float) Math.cos(angle) * radius)
                        .add(axisV.scale((float) Math.sin(angle) * radius));
                rings[index][side] = path[index].add(offset);
            }
        }
        return rings;
    }

    /**
     * A flat quad in the XZ plane, in pixel coordinates.
     */
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
