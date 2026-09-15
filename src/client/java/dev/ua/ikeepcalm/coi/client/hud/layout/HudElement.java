package dev.ua.ikeepcalm.coi.client.hud.layout;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * One draggable thing on the HUD, as the layout editor sees it: where it is,
 * how to move it, and how to draw a representative copy of it.
 * <p>
 * Implementations translate between the editor's single notion of position — a
 * top-left corner in gui-scaled pixels — and whatever the overlay actually
 * stores (an anchor plus offsets, a distance from the bottom edge, a margin
 * from the right). {@link #bounds} and {@link #moveTo} are inverses: after
 * {@code moveTo(x, y, ...)}, {@code bounds(...)} reports {@code (x, y)} again,
 * give or take the on-screen clamp.
 */
public interface HudElement {

    /**
     * Stable id, used for the lang key and for preselecting from the settings
     * screen. Never localized.
     */
    String id();

    /**
     * Display name: {@code screen.coi.layout_el_<id>}.
     */
    default Component label() {
        return Component.translatable("screen.coi.layout_el_" + id());
    }

    /**
     * Id of the family this element belongs to, or {@code null} when it stands
     * alone. Members of a group move together while Ctrl is held, and an
     * <em>Align</em> button naming the group opens all of them at once.
     */
    default String group() {
        return null;
    }

    /**
     * Puts back whatever position the whole {@link #group()} shares - the
     * ability row's origin, say. Called once per group by the editor's Reset,
     * on top of each member's own {@link #resetPosition}.
     */
    default void resetGroup(HudConfig.HudSettings s) {
    }

    /**
     * The element's own show-toggle. A hidden element is still positionable —
     * the editor just ghosts it and tags it as hidden.
     */
    boolean visible(HudConfig.HudSettings s);

    /**
     * {@code {x, y, width, height}} in gui-scaled pixels, covering everything
     * the element draws, labels included.
     */
    int[] bounds(int w, int h, HudConfig.HudSettings s);

    /**
     * Writes whatever settings put the element's {@link #bounds} origin at
     * {@code (newX, newY)}, clamped so it stays on screen.
     */
    void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s);

    /**
     * Draws the element with sample data at full opacity. Must not touch the
     * {@code Client*State} classes — the editor runs while a real session is
     * live behind it.
     */
    void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs);

    /**
     * Puts this one element back where the Default preset has it, leaving
     * every other setting alone.
     */
    void resetPosition(HudConfig.HudSettings s);
}
