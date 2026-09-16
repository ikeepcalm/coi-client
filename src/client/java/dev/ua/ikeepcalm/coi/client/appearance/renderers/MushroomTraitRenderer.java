package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class MushroomTraitRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);
    private static final TraitGeometry.Tint STEM =
            new TraitGeometry.Tint(0.78f, 0.66f, 0.46f, 1.0f);
    private static final TraitGeometry.Tint MUSHROOM_RED =
            new TraitGeometry.Tint(0.72f, 0.07f, 0.06f, 1.0f);
    private static final TraitGeometry.Tint MUSHROOM_DARK =
            new TraitGeometry.Tint(0.47f, 0.035f, 0.03f, 1.0f);
    private static final TraitGeometry.Tint MUSHROOM_SPOT =
            new TraitGeometry.Tint(0.95f, 0.88f, 0.68f, 1.0f);

    @Override
    public String traitId() {
        return "mushroom";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> drawMushroom(pose, consumer, state.lightCoords)
        );
        poseStack.popPose();
    }

    private void drawMushroom(PoseStack.Pose pose, VertexConsumer consumer, int light) {
        TraitGeometry geometry = TraitGeometry.INSTANCE;
        TraitGeometry.Point[] stemPath = {
                geometry.pointPixels(0.65f, -8.10f, 0.45f),
                geometry.pointPixels(0.62f, -9.85f, 0.42f),
                geometry.pointPixels(0.52f, -11.45f, 0.34f)
        };
        geometry.drawTube(
                pose,
                consumer,
                stemPath,
                new float[]{0.72f, 0.62f, 0.52f},
                7,
                new TraitGeometry.Tint[]{STEM, STEM},
                light
        );

        TraitGeometry.Point[] capPath = {
                geometry.pointPixels(0.52f, -11.28f, 0.34f),
                geometry.pointPixels(0.52f, -11.72f, 0.34f),
                geometry.pointPixels(0.52f, -12.42f, 0.34f),
                geometry.pointPixels(0.52f, -13.02f, 0.34f),
                geometry.pointPixels(0.52f, -13.28f, 0.34f)
        };
        geometry.drawTube(
                pose,
                consumer,
                capPath,
                new float[]{1.10f, 2.45f, 2.20f, 1.30f, 0.18f},
                10,
                new TraitGeometry.Tint[]{MUSHROOM_DARK, MUSHROOM_RED, MUSHROOM_RED, MUSHROOM_RED},
                light
        );

        geometry.horizontalQuad(pose, consumer, -0.20f, 0.38f, -13.22f, -0.10f, 0.55f, MUSHROOM_SPOT, light);
        geometry.horizontalQuad(pose, consumer, 0.78f, 1.36f, -13.18f, 0.48f, 1.08f, MUSHROOM_SPOT, light);
        geometry.horizontalQuad(pose, consumer, 0.25f, 0.72f, -13.24f, -0.96f, -0.50f, MUSHROOM_SPOT, light);
    }
}
