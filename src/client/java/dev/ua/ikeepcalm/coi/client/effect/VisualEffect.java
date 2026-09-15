package dev.ua.ikeepcalm.coi.client.effect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One screen-space effect the server can trigger over {@code coi-client:effect}.
 * Implementations are registered by id in {@link EffectManager}, instantiated
 * fresh per trigger, and rendered every frame until they report themselves
 * finished or are stopped.
 */
public interface VisualEffect {

    /**
     * Unique identifier used for registration and network protocol.
     */
    String getId();

    /**
     * Human-readable name shown in the debug screen.
     */
    String getDisplayName();

    /**
     * Default param string shown in the debug screen input field.
     */
    String getDefaultParams();

    /**
     * Called once when the effect is triggered. Parse params here.
     */
    void start(String params);

    /**
     * Called every render frame while the effect is active.
     */
    void render(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, float tickDelta);

    /**
     * Return true once the effect has naturally finished and should be removed.
     */
    boolean isFinished();

    /**
     * Called when the effect is forcibly removed (e.g. "stop" command).
     */
    default void stop() {
    }
}
