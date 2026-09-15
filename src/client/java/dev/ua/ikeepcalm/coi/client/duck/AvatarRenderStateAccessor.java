package dev.ua.ikeepcalm.coi.client.duck;

/**
 * Duck interface for the player-UUID field {@code AvatarRenderStateMixin} adds to the vanilla
 * render state. Cast a render state to this to find out which player it belongs to.
 */
public interface AvatarRenderStateAccessor {

    String coi$getPlayerUuid();

    void coi$setPlayerUuid(String uuid);

}
