package dev.ua.ikeepcalm.coi.client.config;

import dev.ua.ikeepcalm.coi.client.config.HudConfig.HudSettings;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.overlay.BeyonderHealthOverlay;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle;

/**
 * Brings an older {@code coi_hud.json} up to {@link HudConfig#LAYOUT_VERSION}.
 * <p>
 * Each step is guarded by the version it was introduced in and they run in
 * ascending order, so a file at version 0 gets <em>all</em> of them rather than
 * only the newest. Collapsing two steps into one guard, or reordering them,
 * breaks that: the point is that a player who skipped three releases lands in
 * the same place as one who upgraded each time.
 */
final class HudConfigMigrations {

    private HudConfigMigrations() {
    }

    /**
     * v2 — before it, a TOP-anchored madness bar ignored {@code madnessYOffset}
     * and was pinned at {@link HudConfig#DEFAULT_MADNESS_Y}; the value in the
     * file was whatever the BOTTOM anchor would have used. Keeping it would
     * teleport every existing player's bar the moment the offset started
     * mattering, so top-anchored bars are reset to where they were actually
     * drawn.
     * <p>
     * v3 — retired {@code hudScale} and {@code slotSpacing}. {@code hudScale}
     * used to <em>divide</em> the slot row's {@code hudX}/{@code hudYOffset}
     * rather than scale anything, and {@code slotSpacing} was a second size
     * knob that could disagree with {@code slotSize}; both are now folded into
     * {@code slotSize}. Neither has a position-preserving conversion (the old
     * scale formula depended on the screen height), so the offsets are kept
     * exactly as written and the two keys are simply dropped on the next save
     * — there is nothing for this method to do, which is why v3 has no step.
     * Slots 2..N in the shared row therefore shift to the derived step; the
     * layout editor makes any nudge from there trivial.
     * <p>
     * v4 — re-places the Beyonder health bar. {@code HudAnchor.resolve} centres
     * on the element's width, so the stored X offset is only meaningful against
     * the width it was calibrated for — widening the bar from 82 to 100 left
     * every existing config pointing 9px too far left. The offset is now
     * derived from {@code BAR_WIDTH} so it cannot drift again, and a pre-v4
     * file has the bar's placement reset to those defaults. This only discards
     * a deliberate position if the player had already dragged this one bar,
     * which is a far smaller cost than leaving it visibly off-centre for
     * everyone else.
     * <p>
     * v5 — the health element gained four {@link HealthStyle}s and now defaults
     * to {@link HealthStyle#HEARTS}, which keeps the vanilla hearts and draws
     * only the readout. A pre-v5 file predates the setting entirely, so two
     * things are true of it at once: its stored placement was calibrated for a
     * bar sitting <em>on</em> the hearts' row, where the new default's readout
     * would land on top of the hearts it has just handed back; and
     * {@code BAR_WIDTH} changed again in the same release, which the v4 note
     * above already explains makes a stored X offset meaningless. So the style
     * is set and the whole placement goes back to that style's defaults.
     */
    static void migrate(HudSettings s) {
        if (s.layoutVersion < 2 && HudAnchor.parse(s.madnessAnchor).isTop()) {
            s.madnessYOffset = HudConfig.DEFAULT_MADNESS_Y;
        }
        if (s.layoutVersion < 4) {
            s.beyonderHealthAnchor = BeyonderHealthOverlay.DEFAULT_ANCHOR;
            s.beyonderHealthXOffset = BeyonderHealthOverlay.DEFAULT_X_OFFSET;
            s.beyonderHealthYOffset = HealthStyle.BAR.defaultYOffset();
        }
        if (s.layoutVersion < 5) {
            s.beyonderHealthStyle = HealthStyle.HEARTS.name();
            BeyonderHealthOverlay.resetPosition(s, HealthStyle.HEARTS);
        }
        s.layoutVersion = HudConfig.LAYOUT_VERSION;
    }
}
