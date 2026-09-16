package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarRenderStateAccessor {

    @Unique
    private String coi$playerUuid;

    @Override
    public String coi$getPlayerUuid() {
        return coi$playerUuid;
    }

    @Override
    public void coi$setPlayerUuid(String uuid) {
        this.coi$playerUuid = uuid;
    }
}
