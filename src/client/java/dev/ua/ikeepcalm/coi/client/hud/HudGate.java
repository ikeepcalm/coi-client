package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;

import net.minecraft.client.Minecraft;

/**
 * The opening of every COI overlay's render gate, in one place.
 * <p>
 * All eleven overlays begin by refusing to draw for the same four reasons: no
 * player yet, F1 pressed, the layout editor open, or the mod's master switch
 * off. The {@link HudLayout#editing()} term is the one that matters most and is
 * the easiest to leave out — an overlay that forgets it draws its live self
 * underneath the editor's preview, which is the one way to break the editor —
 * so a new overlay gets it by calling this instead of retyping the chain.
 * <p>
 * Whatever else an overlay needs (its own {@code show*} toggle, whether the
 * server has sent any data, the character plate superseding it) stays in the
 * overlay, because no two of those agree.
 */
public final class HudGate {

    private HudGate() {
    }

    /**
     * @return true when nothing COI draws may be drawn at all this frame
     */
    public static boolean blocked(Minecraft client, HudConfig.HudSettings settings) {
        return client.player == null
                || client.gui.hud.isHidden()
                || HudLayout.editing()
                || !settings.enabled;
    }
}
