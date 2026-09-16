package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Short, separate fingertip talons. The old tubes started inside the wrist and were too thin
 * to read reliably; these start at the hand edge, widen at the knuckle, and taper forward.
 */
public final class ClawTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {CORROSIVE, WEREWOLF}

    private static final net.minecraft.client.renderer.rendertype.RenderType MATERIAL =
            net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("coi-client", "textures/entity/claw_grain.png"));

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint claw;
    private final TraitGeometry.Tint symbol;

    public ClawTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.claw = switch (style) {
            case CORROSIVE -> new TraitGeometry.Tint(0.18f, 0.20f, 0.21f, 1);
            case WEREWOLF -> new TraitGeometry.Tint(0.19f, 0.16f, 0.12f, 1);
        };
        this.symbol = switch (style) {
            case CORROSIVE -> new TraitGeometry.Tint(0.34f, 0.40f, 0.35f, 1);
            case WEREWOLF -> new TraitGeometry.Tint(0.38f, 0.32f, 0.23f, 1);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        submitHand(stack, collector, state, model.leftArm, true);
        submitHand(stack, collector, state, model.rightArm, false);
    }

    private void submitHand(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart arm, boolean left) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        float center = (left ? 1.0f : -1.0f) * (slim ? 0.5f : 1.0f);
        var settings = AppearanceConfig.get();
        stack.translate(center / 16.0f, settings.clawYOffsetPixels / 16.0f, settings.clawZOffsetPixels / 16.0f);
        int light = state.lightCoords;
        collector.order(3).submitCustomGeometry(stack, MATERIAL,
                (pose, consumer) -> drawClaws(pose, consumer, light, slim, left));
        stack.popPose();
    }

    private void drawClaws(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, boolean slim, boolean left) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        var settings = AppearanceConfig.get();
        float length = settings.clawLength;
        for (int index = 0; index < 3; index++) {
            float x = (index - 1) * (slim ? 0.8f : 1.1f) * settings.clawSpread;
            TraitGeometry.Point[] path = new TraitGeometry.Point[8];
            float[] radii = new float[8];
            TraitGeometry.Tint[] bands = new TraitGeometry.Tint[7];
            for (int n = 0; n < path.length; n++) {
                float t = n / 7.0f;
                path[n] = g.pointPixels(x, 9.6f + 2.8f * t * length, -2.05f - (t * .8f + t * t * .75f) * length);
                radii[n] = .35f * (1 - t) + .035f;
                if (n < bands.length) bands[n] = n % 3 == 0 ? symbol : claw;
            }
            g.drawTube(pose, consumer, path, radii, 6, bands, light);
            // A narrow polished ridge catches light; the darker bands read as worn keratin/metal.
            var edge = style == Style.CORROSIVE ? new TraitGeometry.Tint(.63f, .67f, .68f, 1)
                    : new TraitGeometry.Tint(.65f, .57f, .42f, 1);
            for (int n = 0; n < path.length; n++) path[n] = path[n].add(new TraitGeometry.Vec((left ? 1 : -1) * radii[n] / 16, 0, 0));
            g.drawTube(pose, consumer, path, new float[]{.055f,.055f,.05f,.05f,.045f,.04f,.03f,.01f}, 3,
                    new TraitGeometry.Tint[]{edge,edge,edge,edge,edge,edge,edge}, light);
        }
    }
}
