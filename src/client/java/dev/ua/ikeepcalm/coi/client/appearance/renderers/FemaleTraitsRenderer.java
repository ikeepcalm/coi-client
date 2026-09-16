package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.client.mcf.Coi3dPrimitives;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Feminine chest profile on the player's own skin — the original main-branch torso
 * mesh, scaled per pathway (mother fullest) and adjustable via the appearance settings
 * (size, separation, vertical position, roundness). Ears stay on female_traits only;
 * {@code demoness_ears} is the standalone variant.
 */
public final class FemaleTraitsRenderer implements AppearanceTraitRenderer, Coi3dPrimitives {

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float TEXTURE_SIZE = 64.0f;
    private static final float CHEST_DEPTH = 1.35f;
    private static final float CHEST_TOP = 1.25f;

    // Exact points and lift values from the original renderer on main.
    private static final float[] CHEST_X = {-4.0f, -2.75f, -1.25f, 0.0f, 1.25f, 2.75f, 4.0f};
    private static final float[] CHEST_X_LIFT = {0.0f, 0.68f, 0.96f, 1.0f, 0.96f, 0.68f, 0.0f};
    private static final float[] CHEST_Y = {1.25f, 2.75f, 4.5f, 6.25f, 8.0f};
    private static final float[] CHEST_Y_LIFT = {0.0f, 0.62f, 1.0f, 0.65f, 0.0f};

    private final String traitId;
    private final float pathwayScale;

    public FemaleTraitsRenderer(String traitId, float pathwayScale) {
        this.traitId = traitId;
        this.pathwayScale = pathwayScale;
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector,
                       AvatarRenderState state, PlayerModel model) {
        Identifier skinTexture = state.skin.body().texturePath();
        RenderType renderType = RenderTypes.entityCutout(skinTexture);

        // Chest equipment owns the visible torso surface.
        if (state.chestEquipment.isEmpty()) {
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            collector.order(0).submitCustomGeometry(
                    poseStack,
                    renderType,
                    (pose, consumer) -> drawTorsoBump(pose, consumer, state, false)
            );
            if (state.showJacket && AppearanceConfig.get().projectJacket) {
                collector.order(0).submitCustomGeometry(
                        poseStack,
                        renderType,
                        (pose, consumer) -> drawTorsoBump(pose, consumer, state, true)
                );
            }
            poseStack.popPose();
        }

        // Demoness keeps the original ears; the other body profiles only use the torso.
        if (traitId.equals("female_traits")) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            collector.order(0).submitCustomGeometry(
                    poseStack,
                    renderType,
                    (pose, consumer) -> drawCatEars(pose, consumer, state, false)
            );
            if (state.showHat) {
                collector.order(0).submitCustomGeometry(
                        poseStack,
                        renderType,
                        (pose, consumer) -> drawCatEars(pose, consumer, state, true)
                );
            }
            poseStack.popPose();
        }
    }

    private void drawTorsoBump(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                               AvatarRenderState state, boolean jacketLayer) {
        float front = jacketLayer ? -2.27f : -2.01f;
        float vOffset = jacketLayer ? 36.0f : 20.0f;

        for (int yIndex = 0; yIndex < CHEST_Y.length - 1; yIndex++) {
            for (int xIndex = 0; xIndex < CHEST_X.length - 1; xIndex++) {
                SkinVertex topLeft = chestVertex(xIndex, yIndex, front, vOffset);
                SkinVertex topRight = chestVertex(xIndex + 1, yIndex, front, vOffset);
                SkinVertex bottomLeft = chestVertex(xIndex, yIndex + 1, front, vOffset);
                SkinVertex bottomRight = chestVertex(xIndex + 1, yIndex + 1, front, vOffset);

                addTexturedTriangle(pose, consumer,
                        topRight.x(), topRight.y(), topRight.z(), topRight.u(), topRight.v(),
                        topLeft.x(), topLeft.y(), topLeft.z(), topLeft.u(), topLeft.v(),
                        bottomLeft.x(), bottomLeft.y(), bottomLeft.z(), bottomLeft.u(), bottomLeft.v(),
                        state.lightCoords);
                addTexturedTriangle(pose, consumer,
                        topRight.x(), topRight.y(), topRight.z(), topRight.u(), topRight.v(),
                        bottomLeft.x(), bottomLeft.y(), bottomLeft.z(), bottomLeft.u(), bottomLeft.v(),
                        bottomRight.x(), bottomRight.y(), bottomRight.z(), bottomRight.u(), bottomRight.v(),
                        state.lightCoords);
            }
        }
    }

    private SkinVertex chestVertex(int xIndex, int yIndex, float front, float vOffset) {
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        float scale = pathwayScale * settings.chestScale;
        // Vertical growth is heavily damped (and never reaches past mid-torso): larger
        // pathway profiles must project outward and widen, not drag the bust down to
        // the stomach. The lift profile itself stays anchored at the upper chest.
        float yScale = 1.0f + (scale - 1.0f) * 0.35f;
        float sourceX = CHEST_X[xIndex];
        float sourceY = CHEST_Y[yIndex];
        float x = sourceX * scale;
        float y = Math.min(CHEST_TOP + (sourceY - CHEST_TOP) * yScale + settings.chestYOffsetPixels,
                CHEST_TOP + (CHEST_Y[CHEST_Y.length - 1] - CHEST_TOP) + settings.chestYOffsetPixels + 0.6f);

        float xLift = CHEST_X_LIFT[xIndex];
        if (xIndex == 3) {
            // The default of zero leaves the original main mesh completely unchanged.
            xLift *= 1.0f - settings.chestSeparationPixels * 0.30f;
        }
        float lift = Math.max(0.0f, xLift * CHEST_Y_LIFT[yIndex]);
        if (lift > 0.0f && settings.chestFullness != 1.0f) {
            lift = (float) Math.pow(lift, 1.0f / settings.chestFullness);
        }
        float z = front - CHEST_DEPTH * scale * lift;

        return new SkinVertex(
                x * PIXEL,
                y * PIXEL,
                z * PIXEL,
                (24.0f + sourceX) / TEXTURE_SIZE,
                (vOffset + sourceY) / TEXTURE_SIZE
        );
    }

    private void drawCatEars(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                             AvatarRenderState state, boolean hatLayer) {
        drawEar(pose, consumer, state.lightCoords, -2.35f, -0.45f, hatLayer);
        drawEar(pose, consumer, state.lightCoords, 2.35f, 0.45f, hatLayer);
    }

    private void drawEar(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light,
                         float centerX, float outwardLean, boolean hatLayer) {
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

        addTexturedTriangle(pose, consumer,
                leftFront.x(), leftFront.y(), leftFront.z(), leftFront.u(), leftFront.v(),
                rightFront.x(), rightFront.y(), rightFront.z(), rightFront.u(), rightFront.v(),
                tipFront.x(), tipFront.y(), tipFront.z(), tipFront.u(), tipFront.v(), light);
        addTexturedTriangle(pose, consumer,
                leftBack.x(), leftBack.y(), leftBack.z(), leftBack.u(), leftBack.v(),
                tipBack.x(), tipBack.y(), tipBack.z(), tipBack.u(), tipBack.v(),
                rightBack.x(), rightBack.y(), rightBack.z(), rightBack.u(), rightBack.v(), light);
        addTexturedQuad(pose, consumer,
                leftFront.x(), leftFront.y(), leftFront.z(), leftFront.u(), leftFront.v(),
                tipFront.x(), tipFront.y(), tipFront.z(), tipFront.u(), tipFront.v(),
                tipBack.x(), tipBack.y(), tipBack.z(), tipBack.u(), tipBack.v(),
                leftBack.x(), leftBack.y(), leftBack.z(), leftBack.u(), leftBack.v(), light);
        addTexturedQuad(pose, consumer,
                rightFront.x(), rightFront.y(), rightFront.z(), rightFront.u(), rightFront.v(),
                rightBack.x(), rightBack.y(), rightBack.z(), rightBack.u(), rightBack.v(),
                tipBack.x(), tipBack.y(), tipBack.z(), tipBack.u(), tipBack.v(),
                tipFront.x(), tipFront.y(), tipFront.z(), tipFront.u(), tipFront.v(), light);
        addTexturedQuad(pose, consumer,
                leftFront.x(), leftFront.y(), leftFront.z(), leftFront.u(), leftFront.v(),
                leftBack.x(), leftBack.y(), leftBack.z(), leftBack.u(), leftBack.v(),
                rightBack.x(), rightBack.y(), rightBack.z(), rightBack.u(), rightBack.v(),
                rightFront.x(), rightFront.y(), rightFront.z(), rightFront.u(), rightFront.v(), light);
    }

    private SkinVertex earVertex(float x, float y, float z,
                                 float baseY, float tipY, boolean hatLayer) {
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

    private record SkinVertex(float x, float y, float z, float u, float v) {
    }
}
