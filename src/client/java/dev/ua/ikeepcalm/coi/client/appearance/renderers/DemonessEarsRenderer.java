package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.mcf.Coi3dPrimitives;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The Demoness cat ears, extracted from the old {@code female_traits} geometry so they can
 * appear on their own (and so body-shape traits stay independent of ear styling).
 */
public class DemonessEarsRenderer implements AppearanceTraitRenderer, Coi3dPrimitives {

    private static final float TEXTURE_SIZE = 64.0f;

    @Override
    public String traitId() {
        return "demoness_ears";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        Identifier skinTexture = state.skin.body().texturePath();
        RenderType renderType = RenderTypes.entityCutout(skinTexture);

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

    private void drawCatEars(PoseStack.Pose pose, VertexConsumer consumer, AvatarRenderState state, boolean hatLayer) {
        drawEar(pose, consumer, state.lightCoords, -2.35f, -0.45f, hatLayer);
        drawEar(pose, consumer, state.lightCoords, 2.35f, 0.45f, hatLayer);
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

        float uOffset = hatLayer ? 44.0f : 12.0f;

        SkinVertex leftFront = earVertex(centerX - halfWidth, baseY, frontZ, baseY, tipY, uOffset);
        SkinVertex rightFront = earVertex(centerX + halfWidth, baseY, frontZ, baseY, tipY, uOffset);
        SkinVertex tipFront = earVertex(tipX, tipY, tipFrontZ, baseY, tipY, uOffset);
        SkinVertex leftBack = earVertex(centerX - halfWidth, baseY, backZ, baseY, tipY, uOffset);
        SkinVertex rightBack = earVertex(centerX + halfWidth, baseY, backZ, baseY, tipY, uOffset);
        SkinVertex tipBack = earVertex(tipX, tipY, tipBackZ, baseY, tipY, uOffset);

        addTexturedTriangle(pose, consumer,
                rightFront.x(), rightFront.y(), rightFront.z(), rightFront.u(), rightFront.v(),
                leftFront.x(), leftFront.y(), leftFront.z(), leftFront.u(), leftFront.v(),
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

    private SkinVertex earVertex(float x, float y, float z, float baseY, float tipY, float uOffset) {
        float heightProgress = (baseY - y) / (baseY - tipY);
        return new SkinVertex(
                x / 16.0f,
                y / 16.0f,
                z / 16.0f,
                (uOffset + x) / TEXTURE_SIZE,
                (8.0f - heightProgress * 8.0f) / TEXTURE_SIZE
        );
    }

    private record SkinVertex(float x, float y, float z, float u, float v) {
    }
}
