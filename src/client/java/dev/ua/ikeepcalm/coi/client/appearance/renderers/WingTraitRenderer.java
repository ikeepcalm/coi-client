package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Membranous wings drawn as bone tubes with double-sided membrane triangles between
 * them, rooted behind the shoulder blades and flapping on a slow idle cycle. The
 * silhouette stays readable from the side because the bones curve through 3D space
 * instead of lying flat against the back.
 */
public final class WingTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {ILLUSORY, DEVIL, NATURAL}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint membrane;
    private final TraitGeometry.Tint bone;

    public WingTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.membrane = switch (style) {
            case ILLUSORY -> new TraitGeometry.Tint(0.055f, 0.02f, 0.10f, 0.48f);
            case DEVIL -> new TraitGeometry.Tint(0.18f, 0.008f, 0.015f, 0.86f);
            case NATURAL -> new TraitGeometry.Tint(0.22f, 0.075f, 0.08f, 0.78f);
        };
        this.bone = switch (style) {
            case ILLUSORY -> new TraitGeometry.Tint(0.26f, 0.08f, 0.38f, 0.70f);
            case DEVIL -> new TraitGeometry.Tint(0.055f, 0.018f, 0.02f, 0.98f);
            case NATURAL -> new TraitGeometry.Tint(0.10f, 0.035f, 0.04f, 0.94f);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        if (!state.chestEquipment.isEmpty() && state.chestEquipment.is(net.minecraft.world.item.Items.ELYTRA)) {
            return; // the elytra owns the back
        }
        stack.pushPose();
        model.body.translateAndRotate(stack);
        float scale = AppearanceConfig.get().wingScale;
        stack.scale(scale, scale, scale);
        collector.order(0).submitCustomGeometry(
                stack,
                TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawWings(pose, consumer, state.lightCoords)
        );
        stack.popPose();
    }

    private void drawWings(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light) {
        float flap = (float) Math.sin(System.currentTimeMillis() * 0.0045 * dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().wingFlapSpeed) * (style == Style.ILLUSORY ? 0.9f : 0.55f);
        drawWing(pose, consumer, light, -1.0f, flap);
        drawWing(pose, consumer, light, 1.0f, flap);
    }

    private void drawWing(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float side, float flap) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        TraitGeometry.Point root = g.pointPixels(side * 3.2f, 1.1f, 2.35f);
        TraitGeometry.Point elbow = g.pointPixels(side * 8.0f, -1.3f, 2.9f + flap);
        TraitGeometry.Point tip = g.pointPixels(side * 14.0f, 1.8f, 3.5f + flap * 1.5f);
        TraitGeometry.Point lowerOuter = g.pointPixels(side * 10.5f, 8.5f, 3.25f + flap);
        TraitGeometry.Point lowerMid = g.pointPixels(side * 6.2f, 6.2f, 2.9f + flap * 0.6f);
        TraitGeometry.Point lowerRoot = g.pointPixels(side * 3.6f, 10.6f, 2.45f);

        doubleTriangle(g, pose, consumer, root, elbow, lowerMid, membrane, light);
        doubleTriangle(g, pose, consumer, elbow, tip, lowerOuter, membrane, light);
        doubleTriangle(g, pose, consumer, elbow, lowerOuter, lowerMid, membrane, light);
        doubleTriangle(g, pose, consumer, root, lowerMid, lowerRoot, membrane, light);

        g.drawTube(pose, consumer, new TraitGeometry.Point[]{root, elbow, tip},
                new float[]{0.34f, 0.29f, 0.10f}, 6, new TraitGeometry.Tint[]{bone, bone}, light);
        g.drawTube(pose, consumer, new TraitGeometry.Point[]{elbow, lowerOuter},
                new float[]{0.24f, 0.08f}, 5, new TraitGeometry.Tint[]{bone}, light);
        g.drawTube(pose, consumer, new TraitGeometry.Point[]{root, lowerMid, lowerRoot},
                new float[]{0.26f, 0.18f, 0.06f}, 5, new TraitGeometry.Tint[]{bone, bone}, light);
    }

    private void doubleTriangle(TraitGeometry g, PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                TraitGeometry.Point a, TraitGeometry.Point b, TraitGeometry.Point c,
                                TraitGeometry.Tint tint, int light) {
        g.triangle(pose, consumer, a, b, c, tint, light);
        g.triangle(pose, consumer, c, b, a, tint, light);
    }
}
