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
 * Subtle material overlays for devil armor, wood, stone, zombie and wraith. These leave
 * the base player skin readable instead of drawing an opaque second mannequin over it.
 */
public final class SkinTraitRenderer implements AppearanceTraitRenderer {

    public enum Style {DEVIL_ARMOR, WOOD, STONE, CHITIN, ZOMBIE, WRAITH}

    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint primary;
    private final TraitGeometry.Tint detail;

    public SkinTraitRenderer(String traitId, Style style) {
        this.traitId = traitId;
        this.style = style;
        this.primary = switch (style) {
            case DEVIL_ARMOR -> new TraitGeometry.Tint(0.08f, 0.025f, 0.09f, 0.65f);
            case WOOD -> new TraitGeometry.Tint(0.38f, 0.20f, 0.07f, 0.60f);
            case STONE -> new TraitGeometry.Tint(0.48f, 0.50f, 0.54f, 0.60f);
            case CHITIN -> new TraitGeometry.Tint(0.16f, 0.08f, 0.18f, 0.65f);
            case ZOMBIE -> new TraitGeometry.Tint(0.24f, 0.40f, 0.25f, 0.55f);
            case WRAITH -> new TraitGeometry.Tint(0.35f, 0.70f, 0.82f, 0.35f);
        };
        this.detail = switch (style) {
            case DEVIL_ARMOR -> new TraitGeometry.Tint(0.70f, 0.06f, 0.10f, 0.58f);
            case WOOD -> new TraitGeometry.Tint(0.62f, 0.34f, 0.10f, 0.48f);
            case STONE -> new TraitGeometry.Tint(0.72f, 0.75f, 0.80f, 0.46f);
            case CHITIN -> new TraitGeometry.Tint(0.55f, 0.12f, 0.62f, 0.52f);
            case ZOMBIE -> new TraitGeometry.Tint(0.18f, 0.32f, 0.18f, 0.42f);
            case WRAITH -> new TraitGeometry.Tint(0.72f, 0.92f, 1.0f, 0.25f);
        };
    }

    @Override
    public String traitId() {
        return traitId;
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        submitPart(stack, collector, state, model.head, Part.HEAD, slim, 0);
        submitPart(stack, collector, state, model.body, Part.BODY, slim, 0);
        submitPart(stack, collector, state, model.leftArm, Part.ARM, slim, slim ? 0.5f : 1.0f);
        submitPart(stack, collector, state, model.rightArm, Part.ARM, slim, slim ? -0.5f : -1.0f);
        submitPart(stack, collector, state, model.leftLeg, Part.LEG, slim, 0);
        submitPart(stack, collector, state, model.rightLeg, Part.LEG, slim, 0);
    }

    private void submitPart(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, ModelPart part,
                            Part partType, boolean slim, float offsetX) {
        stack.pushPose();
        part.translateAndRotate(stack);
        stack.translate(offsetX / 16.0f, 0, 0);
        collector.order(1).submitCustomGeometry(
                stack,
                TraitRenderSupport.TRANSLUCENT,
                (pose, consumer) -> drawPart(pose, consumer, state.lightCoords, partType, slim)
        );
        stack.popPose();
    }

    private void drawPart(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, Part part, boolean slim) {
        TraitGeometry g = TraitGeometry.INSTANCE;
        float opacity = AppearanceConfig.get().overlayOpacity;
        TraitGeometry.Tint primary = withOpacity(this.primary, opacity);
        TraitGeometry.Tint detail = withOpacity(this.detail, opacity);

        // Keep the overlay just outside the vanilla cuboid. Larger inflation detached
        // shoulders/hips and made the devil skin read as a black silhouette.
        float inflate = 0.055f;
        switch (part) {
            case HEAD -> g.boxPixels(pose, consumer, -4 - inflate, -8 - inflate, -4 - inflate,
                    4 + inflate, inflate, 4 + inflate, primary, light);
            case BODY -> g.boxPixels(pose, consumer, -4 - inflate, -inflate, -2 - inflate,
                    4 + inflate, 12 + inflate, 2 + inflate, primary, light);
            case ARM -> {
                float halfWidth = slim ? 1.5f : 2.0f;
                g.boxPixels(pose, consumer, -halfWidth - inflate, -2 - inflate, -2 - inflate,
                        halfWidth + inflate, 10 + inflate, 2 + inflate, primary, light);
            }
            case LEG -> g.boxPixels(pose, consumer, -2 - inflate, -inflate, -2 - inflate,
                    2 + inflate, 12 + inflate, 2 + inflate, primary, light);
        }

        if (style == Style.WRAITH) return;
        switch (part) {
            case HEAD -> {
                g.boxPixels(pose, consumer, -3.35f, -7.95f, -4.48f, -1.95f, -6.7f, -4.16f, detail, light);
                g.boxPixels(pose, consumer, 1.55f, -1.65f, -4.48f, 3.45f, -0.4f, -4.16f, detail, light);
            }
            case BODY -> {
                g.boxPixels(pose, consumer, -3.75f, 1.1f, -2.48f, -0.35f, 2.0f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, 0.35f, 5.2f, -2.48f, 3.75f, 6.15f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, -2.2f, 9.2f, -2.48f, 1.25f, 10.15f, -2.16f, detail, light);
            }
            case ARM, LEG -> {
                float edge = part == Part.ARM && slim ? 1.35f : 1.85f;
                g.boxPixels(pose, consumer, -edge, 1.1f, -2.48f, 0.55f, 2.0f, -2.16f, detail, light);
                g.boxPixels(pose, consumer, -0.55f, 7.1f, -2.48f, edge, 8.0f, -2.16f, detail, light);
            }
        }
    }

    private static TraitGeometry.Tint withOpacity(TraitGeometry.Tint tint, float opacity) {
        if (opacity >= 0.999f) return tint;
        return new TraitGeometry.Tint(tint.r(), tint.g(), tint.b(), tint.a() * opacity);
    }

    private enum Part {HEAD, BODY, ARM, LEG}
}
