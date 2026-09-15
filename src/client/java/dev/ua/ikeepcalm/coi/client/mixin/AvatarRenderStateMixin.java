package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.duck.AvatarRenderStateAccessor;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a player-UUID field to the vanilla render state, which carries no identity of its own.
 * Written by {@link AvatarRendererMixin}, read through {@link AvatarRenderStateAccessor} by
 * anything that has to ask whose render state it is — appearance traits and creature forms.
 */
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
