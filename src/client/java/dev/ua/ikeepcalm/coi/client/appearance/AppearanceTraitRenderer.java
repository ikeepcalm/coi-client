package dev.ua.ikeepcalm.coi.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public interface AppearanceTraitRenderer {

    String traitId();

    void submit(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model);
}
