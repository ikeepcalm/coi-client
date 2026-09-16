package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Werewolf traits — dark fur hide over head and body, a protruding muzzle with black
 * nose, triangular ears and a light chest patch. Claws are rendered separately so the
 * Werewolf Claws ability has its own progression and visual treatment.
 */
public final class BeastTraitRenderer implements AppearanceTraitRenderer {

    private static final TraitGeometry.Tint FUR = new TraitGeometry.Tint(0.17f, 0.16f, 0.18f, 0.92f);
    private static final TraitGeometry.Tint FUR_LIGHT = new TraitGeometry.Tint(0.34f, 0.32f, 0.31f, 0.92f);
    private static final TraitGeometry.Tint NOSE = new TraitGeometry.Tint(0.025f, 0.018f, 0.02f, 1.0f);
    private final String traitId;

    public BeastTraitRenderer(String traitId) {
        this.traitId = traitId;
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        boolean slim = state.skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        stack.pushPose();
        model.head.translateAndRotate(stack);
        collector.order(3).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawHead(pose, consumer, state.lightCoords));
        stack.popPose();

        stack.pushPose();
        model.body.translateAndRotate(stack);
        collector.order(2).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawBody(pose, consumer, state.lightCoords));
        stack.popPose();

        submitArm(stack, collector, state, model.leftArm, slim, true);
        submitArm(stack, collector, state, model.rightArm, slim, false);
    }

    private void submitArm(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart arm, boolean slim, boolean left) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        stack.translate((left ? 1.0f : -1.0f) * (slim ? 0.5f : 1.0f) / 16.0f, 0.0f, 0.0f);
        collector.order(3).submitCustomGeometry(stack, TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawArm(pose, consumer, state.lightCoords, slim));
        stack.popPose();
    }

    private void drawHead(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        g.boxPixels(pose, consumer, -4.28f, -8.28f, -4.28f, 4.28f, 0.28f, 4.28f, withOpacity(FUR), light);
        g.boxPixels(pose, consumer, -2.65f, -2.6f, -6.3f, 2.65f, 0.15f, -4.0f, withOpacity(FUR_LIGHT), light);
        g.boxPixels(pose, consumer, -1.25f, -1.8f, -6.55f, 1.25f, -0.45f, -6.25f, withOpacity(NOSE), light);
        drawEar(g, pose, consumer, light, -1.0f);
        drawEar(g, pose, consumer, light, 1.0f);
    }

    private void drawEar(TraitGeometry g, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float side) {
        TraitGeometry.Point a = g.pointPixels(side * 1.4f, -8.1f, 0.8f);
        TraitGeometry.Point b = g.pointPixels(side * 4.0f, -8.0f, 1.6f);
        TraitGeometry.Point c = g.pointPixels(side * 3.1f, -12.0f, 1.7f);
        g.triangle(pose, consumer, a, b, c, withOpacity(FUR), light);
        g.triangle(pose, consumer, c, b, a, withOpacity(FUR), light);
    }

    private void drawBody(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        g.boxPixels(pose, consumer, -4.25f, -0.15f, -2.25f, 4.25f, 12.2f, 2.25f, withOpacity(FUR), light);
        g.boxPixels(pose, consumer, -2.8f, 1.1f, -2.48f, 2.8f, 8.9f, -2.18f, withOpacity(FUR_LIGHT), light);
    }

    private static TraitGeometry.Tint withOpacity(TraitGeometry.Tint tint) {
        return new TraitGeometry.Tint(tint.r(), tint.g(), tint.b(), tint.a() * AppearanceConfig.get().overlayOpacity);
    }

    private void drawArm(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, boolean slim) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        float half = slim ? 1.68f : 2.18f;
        g.boxPixels(pose, consumer, -half, -2.15f, -2.18f, half, 10.18f, 2.18f, withOpacity(FUR), light);
    }
}
