package dev.ua.ikeepcalm.coi.client.mcf;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders the baked model for the active player's {@link PartialFormSpec}, if any, alongside the
 * vanilla player model. {@link dev.ua.ikeepcalm.coi.mixin.PlayerModelMixin} hides the player's
 * legs for the duration so this stands in for the lower body instead of clipping through it.
 */
public class PartialFormLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private record Baked(Model<AvatarRenderState> model, RenderType renderType) {
    }

    private final Map<ModelLayerLocation, Baked> baked = new HashMap<>();

    public PartialFormLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityRendererProvider.Context context) {
        super(renderer);
        for (PartialFormSpec spec : MythicalFormManager.getPartialForms().values()) {
            Model<AvatarRenderState> model = spec.factory().apply(context.bakeLayer(spec.layer()));
            RenderType renderType = RenderTypes.entityCutout(spec.texture());
            baked.put(spec.layer(), new Baked(model, renderType));
        }
    }

    @Override
    public void submit(@NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        PartialFormSpec spec = PartialForms.partial(state);
        if (spec == null) {
            return;
        }
        Baked entry = baked.get(spec.layer());
        if (entry == null) {
            return;
        }

        poseStack.pushPose();
        // The renderer mixin has already moved everything in this pose — the player and every layer
        // on top of it — by the carrier transform, so that the player's upper body follows this
        // model's torso. This model animates that motion itself, so it has to come back out here or
        // the torso would travel twice as far as the body sitting on it.
        Matrix4f carrier = PartialForms.carrierTransform(spec, state);
        if (carrier != null) {
            poseStack.mulPose(carrier.invertAffine());
        }
        poseStack.translate(spec.offsetX(), spec.offsetY(), spec.offsetZ());
        poseStack.scale(spec.scale(), spec.scale(), spec.scale());
        // submitModel rather than submitCustomGeometry: submission is deferred, and this model
        // instance is shared by every player the renderer draws, so the pose has to be applied at
        // draw time from the captured state. Posing it here and letting a captured lambda read it
        // later would give every transformed player on screen the last one's pose.
        collector.order(0).submitModel(
                entry.model(),
                state,
                poseStack,
                entry.renderType(),
                light,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor,
                null
        );
        poseStack.popPose();
    }
}
