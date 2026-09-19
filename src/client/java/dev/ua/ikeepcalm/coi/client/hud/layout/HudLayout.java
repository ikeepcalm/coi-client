package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.screen.settings.HudLayoutScreen;

/**
 * Shared switch for the HUD layout editor.
 * <p>
 * While the editor screen is open every overlay skips its own render and the
 * screen draws sample-data previews in their place, so the player drags one
 * picture of each element instead of two copies fighting over the same pixels.
 */
public class HudLayout {

    private static boolean editing = false;

    private HudLayout() {
    }

    /**
     * True while {@code HudLayoutScreen} is open. Overlays check this in their
     * render gate right after the hud-hidden check.
     */
    public static boolean editing() {
        return editing;
    }

    public static void setEditing(boolean value) {
        editing = value;
    }
}
