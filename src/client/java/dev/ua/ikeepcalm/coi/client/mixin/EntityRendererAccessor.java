package dev.ua.ikeepcalm.coi.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's protected name-tag submission, so a full creature form — which cancels the
 * vanilla render entirely — can still draw the player's name tag itself.
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {

    @Invoker("submitNameDisplay")
    void callSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState);

}
