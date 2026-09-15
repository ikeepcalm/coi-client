package dev.ua.ikeepcalm.coi.client.appearance.trait;

import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.form.FormPrimitives;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Draws the {@code female_traits} appearance trait: a rounded chest contour lifted out of the flat
 * torso, and a pair of cat ears on the head.
 *
 * <p>Both are built from the player's <em>own</em> skin texture rather than a texture of their own —
 * each generated vertex carries the UV of the skin pixel that would have been at that spot on the
 * flat model, so the new geometry inherits whatever skin the player is wearing.
 */
public class FemaleTraitsRenderer implements AppearanceTraitRenderer, FormPrimitives {

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float TEXTURE_SIZE = 64.0f;

    private static final float[] CHEST_X = {-4.0f, -2.75f, -1.25f, 0.0f, 1.25f, 2.75f, 4.0f};
    private static final float[] CHEST_X_LIFT = {0.0f, 0.68f, 0.96f, 1.0f, 0.96f, 0.68f, 0.0f};
    private static final float[] CHEST_Y = {1.25f, 2.75f, 4.5f, 6.25f, 8.0f};
    private static final float[] CHEST_Y_LIFT = {0.0f, 0.62f, 1.0f, 0.65f, 0.0f};
    private static final float CHEST_DEPTH = 1.35f;

    @Override
    public String traitId() {
        return "female_traits";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        RenderType renderType = RenderTypes.entityCutout(state.skin.body().texturePath());

        // Chest armor owns the visible torso surface. Hiding the skin bump here prevents
        // lifted skin pixels from poking through chestplates, elytra, or other chest gear.
        if (state.chestEquipment.isEmpty()) {
            submitOnPart(poseStack, collector, renderType, model.body, state.showJacket,
                    (pose, consumer, overlay) -> drawTorsoBump(pose, consumer, state, overlay));
        }
        submitOnPart(poseStack, collector, renderType, model.head, state.showHat,
                (pose, consumer, overlay) -> drawCatEars(pose, consumer, state, overlay));
    }

    /**
     * Submits {@code part}'s geometry in the model part's own space, once for the base skin layer
     * and — only when the viewer has that layer switched on — a second time for the slightly
     * inflated overlay (jacket, hat) that sits above it.
     */
    private void submitOnPart(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                              ModelPart part, boolean showOverlay, LayeredGeometry geometry) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(poseStack, renderType,
                (pose, consumer) -> geometry.draw(pose, consumer, false));
        if (showOverlay) {
            collector.order(0).submitCustomGeometry(poseStack, renderType,
                    (pose, consumer) -> geometry.draw(pose, consumer, true));
        }
        poseStack.popPose();
    }

    /**
     * Geometry that draws itself differently for the base skin layer and the overlay layer above it.
     */
    @FunctionalInterface
    private interface LayeredGeometry {
        void draw(PoseStack.Pose pose, VertexConsumer consumer, boolean overlayLayer);
    }

    private void drawTorsoBump(PoseStack.Pose pose, VertexConsumer consumer, AvatarRenderState state, boolean jacketLayer) {
        float front = jacketLayer ? -2.27f : -2.01f;
        float vOffset = jacketLayer ? 36.0f : 20.0f;

        for (int yIndex = 0; yIndex < CHEST_Y.length - 1; yIndex++) {
            for (int xIndex = 0; xIndex < CHEST_X.length - 1; xIndex++) {
                SkinVertex topLeft = chestVertex(xIndex, yIndex, front, vOffset);
                SkinVertex topRight = chestVertex(xIndex + 1, yIndex, front, vOffset);
                SkinVertex bottomLeft = chestVertex(xIndex, yIndex + 1, front, vOffset);
                SkinVertex bottomRight = chestVertex(xIndex + 1, yIndex + 1, front, vOffset);

                texturedTriangle(pose, consumer, topRight, topLeft, bottomLeft, state.lightCoords);
                texturedTriangle(pose, consumer, topRight, bottomLeft, bottomRight, state.lightCoords);
            }
        }
    }

    private void drawCatEars(PoseStack.Pose pose, VertexConsumer consumer, AvatarRenderState state, boolean hatLayer) {
        drawEar(pose, consumer, state.lightCoords, -2.35f, -0.45f, hatLayer);
        drawEar(pose, consumer, state.lightCoords, 2.35f, 0.45f, hatLayer);
    }

    private SkinVertex chestVertex(int xIndex, int yIndex, float front, float vOffset) {
        float x = CHEST_X[xIndex];
        float y = CHEST_Y[yIndex];
        float lift = CHEST_X_LIFT[xIndex] * CHEST_Y_LIFT[yIndex];
        float z = front - CHEST_DEPTH * lift;
        return new SkinVertex(
                x * PIXEL,
                y * PIXEL,
                z * PIXEL,
                (24.0f + x) / TEXTURE_SIZE,
                (vOffset + y) / TEXTURE_SIZE
        );
    }

    private void drawEar(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            float centerX,
            float outwardLean,
            boolean hatLayer
    ) {
        float inflation = hatLayer ? 0.10f : 0.0f;
        float baseY = hatLayer ? -8.27f : -8.02f;
        float tipY = -12.15f - inflation;
        float halfWidth = 1.28f + inflation;
        float frontZ = -1.45f - inflation;
        float backZ = 1.05f + inflation;
        float tipX = centerX + outwardLean;
        float tipFrontZ = -0.35f - inflation;
        float tipBackZ = -0.05f + inflation;

        SkinVertex leftFront = earVertex(centerX - halfWidth, baseY, frontZ, baseY, tipY, hatLayer);
        SkinVertex rightFront = earVertex(centerX + halfWidth, baseY, frontZ, baseY, tipY, hatLayer);
        SkinVertex tipFront = earVertex(tipX, tipY, tipFrontZ, baseY, tipY, hatLayer);
        SkinVertex leftBack = earVertex(centerX - halfWidth, baseY, backZ, baseY, tipY, hatLayer);
        SkinVertex rightBack = earVertex(centerX + halfWidth, baseY, backZ, baseY, tipY, hatLayer);
        SkinVertex tipBack = earVertex(tipX, tipY, tipBackZ, baseY, tipY, hatLayer);

        texturedTriangle(pose, consumer, leftFront, rightFront, tipFront, light);
        texturedTriangle(pose, consumer, leftBack, tipBack, rightBack, light);
        texturedQuad(pose, consumer, leftFront, tipFront, tipBack, leftBack, light);
        texturedQuad(pose, consumer, rightFront, rightBack, tipBack, tipFront, light);
        texturedQuad(pose, consumer, leftFront, leftBack, rightBack, rightFront, light);
    }

    private SkinVertex earVertex(float x, float y, float z, float baseY, float tipY, boolean hatLayer) {
        float uOffset = hatLayer ? 44.0f : 12.0f;
        float heightProgress = (baseY - y) / (baseY - tipY);
        return new SkinVertex(
                x * PIXEL,
                y * PIXEL,
                z * PIXEL,
                (uOffset + x) / TEXTURE_SIZE,
                (8.0f - heightProgress * 8.0f) / TEXTURE_SIZE
        );
    }

    private void texturedTriangle(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            SkinVertex a,
            SkinVertex b,
            SkinVertex c,
            int light
    ) {
        addTexturedTriangle(
                pose,
                consumer,
                a.x(), a.y(), a.z(), a.u(), a.v(),
                b.x(), b.y(), b.z(), b.u(), b.v(),
                c.x(), c.y(), c.z(), c.u(), c.v(),
                light
        );
    }

    private void texturedQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            SkinVertex a,
            SkinVertex b,
            SkinVertex c,
            SkinVertex d,
            int light
    ) {
        addTexturedQuad(
                pose,
                consumer,
                a.x(), a.y(), a.z(), a.u(), a.v(),
                b.x(), b.y(), b.z(), b.u(), b.v(),
                c.x(), c.y(), c.z(), c.u(), c.v(),
                d.x(), d.y(), d.z(), d.u(), d.v(),
                light
        );
    }

    private record SkinVertex(float x, float y, float z, float u, float v) {
    }
}
