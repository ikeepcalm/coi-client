package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ua.ikeepcalm.coi.client.mcf.PartialForms;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides leggings/boots geometry for a player transformed into a partial form (see
 * {@link dev.ua.ikeepcalm.coi.client.mcf.PartialFormSpec}), since armor pieces render from their
 * own model set rather than from the player parts {@link PlayerModelMixin} hides.
 *
 * <p>Chest and head armor need no help here: {@link PlayerRendererMixin} moves the pose every layer
 * inherits, so they ride the baked model along with the body they're worn on.
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void coi$hidePartialFormLegArmor(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int light, HumanoidRenderState state, CallbackInfo ci) {
        if (slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
            return;
        }
        if (PartialForms.partial(state) != null) {
            ci.cancel();
        }
    }
}
