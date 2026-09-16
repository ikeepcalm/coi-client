package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.client.mcf.PartialForms;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
