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

public class HornsTraitRenderer implements AppearanceTraitRenderer {

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);
    private static final TraitGeometry.Tint HORN_DARK =
            new TraitGeometry.Tint(0.12f, 0.08f, 0.10f, 1.0f);
    private static final TraitGeometry.Tint HORN_RIDGE =
            new TraitGeometry.Tint(0.32f, 0.07f, 0.10f, 1.0f);

    @Override
    public String traitId() {
        return "horns";
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.order(0).submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> {
                    drawHorn(pose, consumer, state.lightCoords, -1.0f);
                    drawHorn(pose, consumer, state.lightCoords, 1.0f);
                }
        );
        poseStack.popPose();
    }

    private void drawHorn(PoseStack.Pose pose, VertexConsumer consumer, int light, float side) {
        TraitGeometry geometry = TraitGeometry.INSTANCE;
        TraitGeometry.Point[] path = {
                geometry.pointPixels(side * 2.85f, -7.35f, 0.90f),
                geometry.pointPixels(side * 4.15f, -8.35f, 1.05f),
                geometry.pointPixels(side * 5.15f, -8.72f, 1.48f),
                geometry.pointPixels(side * 5.72f, -7.72f, 1.82f),
                geometry.pointPixels(side * 5.56f, -6.48f, 1.50f),
                geometry.pointPixels(side * 4.82f, -5.75f, 0.68f)
        };
        geometry.drawTube(
                pose,
                consumer,
                path,
                new float[]{1.05f, 0.98f, 0.82f, 0.64f, 0.43f, 0.13f},
                7,
                new TraitGeometry.Tint[]{HORN_DARK, HORN_RIDGE, HORN_DARK, HORN_RIDGE, HORN_DARK},
                light
        );
    }
}
