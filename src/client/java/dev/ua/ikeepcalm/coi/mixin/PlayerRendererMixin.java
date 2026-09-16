package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.mcf.PartialFormSpec;
import dev.ua.ikeepcalm.coi.client.mcf.PartialForms;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Unique
    private static final float COI_MYTHICAL_WALK_EPSILON = 0.015F;

    @Unique
    private static final RenderType COI_MYTHICAL_RENDER_TYPE =
            RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png"));

    /**
     * Whether {@link #coi$renderMythicalForm} / {@link #coi$rideFormCarrier} pushed their pose this
     * call. Pairing the pops off a flag rather than off a second form lookup keeps the pose stack
     * balanced even if the player's form is cleared mid-frame between the two injections.
     */
    @Unique
    private boolean coi$hipRaisePushed;

    @Unique
    private boolean coi$carrierPushed;

    @Unique
    private static void coi$applyMythicalWalkAnimation(LivingEntityRenderState state, PoseStack poseStack) {
        float walkAmount = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        if (walkAmount <= COI_MYTHICAL_WALK_EPSILON) {
            return;
        }

        float stride = state.walkAnimationPos * 0.6662F;
        float swing = Mth.sin(stride);
        float step = Mth.abs(Mth.cos(stride));
        float intensity = walkAmount * walkAmount;

        poseStack.translate(swing * 0.025F * intensity, step * 0.055F * intensity, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(swing * 3.25F * intensity));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(stride) * 1.75F * intensity));
    }

    @Unique
    private static void coi$submitMythicalHeldItems(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!state.rightHandItemState.isEmpty()) {
            coi$submitMythicalHeldItem(state, poseStack, collector, HumanoidArm.RIGHT);
        }

        if (!state.leftHandItemState.isEmpty()) {
            coi$submitMythicalHeldItem(state, poseStack, collector, HumanoidArm.LEFT);
        }
    }

    @Unique
    private static void coi$submitMythicalHeldItem(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, HumanoidArm arm) {
        boolean left = arm == HumanoidArm.LEFT;
        float side = left ? 1.0F : -1.0F;

        poseStack.pushPose();
        poseStack.translate(side * 0.68F, 1.22F, -0.28F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);

        if (left) {
            state.leftHandItemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        } else {
            state.rightHandItemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        poseStack.popPose();
    }

    @Inject(method = "submit*", at = @At("HEAD"), cancellable = true)
    private void coi$renderMythicalForm(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        coi$hipRaisePushed = false;
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        MythicalCreatureForm form = PartialForms.form(avatarState);
        if (form == null) {
            return;
        }

        PartialFormSpec partial = form.partialForm();
        if (partial != null) {
            // Partial forms keep the vanilla render running (skin, armor, held items, name tag).
            // Leg visibility is handled in PlayerModelMixin (setupAnim), not here — this render
            // pipeline defers geometry submission, so toggling shared model state around this
            // call and restoring it before the deferred draw runs has no effect.
            //
            // This push is deliberately outside setupRotations, i.e. in world space, so that the
            // name tag rides up with the head instead of ending up buried in the raised chest.
            poseStack.pushPose();
            poseStack.translate(0.0F, partial.hipRaise(), 0.0F);
            coi$hipRaisePushed = true;
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        coi$applyMythicalWalkAnimation(state, poseStack);

        String pathwayKey = form.getPathwayName();
        collector.order(0).submitCustomGeometry(
                poseStack,
                COI_MYTHICAL_RENDER_TYPE,
                (pose, consumer) -> MythicalFormManager.renderFormSubmit(pathwayKey, avatarState, pose, consumer)
        );
        coi$submitMythicalHeldItems(avatarState, poseStack, collector);

        poseStack.popPose();

        ((EntityRendererAccessor) this).callSubmitNameDisplay(avatarState, poseStack, collector, cameraState);

        ci.cancel();
    }

    /**
     * Moves the player — and, since they inherit this pose, every layer drawn on top of it: armor,
     * held items, cape, appearance traits — by the rigid motion of the baked model's carrier bone,
     * so the two halves read as one body rather than a torso balanced on an independently swaying
     * mount. {@link dev.ua.ikeepcalm.coi.client.mcf.PartialFormLayer} takes the transform back out
     * for the baked model itself, which animates that motion on its own.
     *
     * <p>It goes in here, immediately before the model is submitted, because that's the first point
     * in {@code submit()} where the pose stack is in model space: past {@code setupRotations}, so
     * the carrier's axes line up with the body's rather than the world's, and past the Y-flip and
     * the 1.501-block drop, so its units match the {@code ModelPart} coordinates it was measured in.
     * Pushing at {@code HEAD} the way {@code hipRaise} does would apply a body-relative roll as a
     * world-relative one, which only looks right when the player happens to face south.
     */
    @Inject(method = "submit*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;isBodyVisible(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z"))
    private void coi$rideFormCarrier(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        coi$carrierPushed = false;
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        PartialFormSpec partial = PartialForms.partial(avatarState);
        if (partial == null) {
            return;
        }
        Matrix4f carrier = PartialForms.carrierTransform(partial, avatarState);
        if (carrier == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(carrier);
        coi$carrierPushed = true;
    }

    /**
     * Ends the carrier pose at vanilla's own {@code popPose}, which is after the model and all its
     * layers but before the name tag — so the tag stays upright instead of swaying with the torso.
     */
    @Inject(method = "submit*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void coi$releaseFormCarrier(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        if (coi$carrierPushed) {
            coi$carrierPushed = false;
            poseStack.popPose();
        }
    }

    @Inject(method = "submit*", at = @At("RETURN"))
    private void coi$restoreAfterPartialForm(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        if (coi$hipRaisePushed) {
            coi$hipRaisePushed = false;
            poseStack.popPose();
        }
    }
}
