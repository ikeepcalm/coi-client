package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.form.PartialForms;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the player's own legs when a partial form's baked model is standing in for them.
 *
 * <p>This has to run in {@code setupAnim}, not around the render call: geometry submission is
 * deferred, so toggling shared model state at render time and restoring it before the deferred draw
 * actually runs has no effect at all.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void coi$hideLegsForPartialForm(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        boolean hidden = PartialForms.partial(state) != null;
        self.leftLeg.visible = !hidden;
        self.rightLeg.visible = !hidden;

        // Overlay layers are re-derived rather than simply un-hidden: vanilla decides these from the
        // viewer's skin-customisation flags just above us, and blanket-restoring them to visible
        // would force pants back on for players who have that layer switched off.
        self.leftPants.visible = !hidden && state.showLeftPants;
        self.rightPants.visible = !hidden && state.showRightPants;
    }
}
