package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitLayer;
import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import dev.ua.ikeepcalm.coi.client.mcf.PartialFormLayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void coi$addAppearanceTraitLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;
        ((LivingEntityRendererAccessor) this).coi$addLayer(new AppearanceTraitLayer(parent));
        ((LivingEntityRendererAccessor) this).coi$addLayer(new PartialFormLayer(parent, context));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void coi$extractPlayerUuid(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        ((AvatarRenderStateAccessor) state).coi$setPlayerUuid(entity.getUUID().toString());
    }

}
