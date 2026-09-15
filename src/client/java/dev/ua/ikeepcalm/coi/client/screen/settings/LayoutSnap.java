package dev.ua.ikeepcalm.coi.client.screen.settings;

import net.minecraft.util.Mth;

/**
 * Where a dragged HUD element actually lands: the grid, centre-line and margin
 * snapping of {@link HudLayoutScreen}, and the clamp that keeps the box on
 * screen whether it snapped or not.
 */
final class LayoutSnap {

    /**
     * Free placement is Shift; everything else lands on this grid.
     */
    static final int GRID = 4;
    /**
     * How close an edge or centre line has to be before it snaps.
     */
    static final int SNAP = 6;
    static final int SCREEN_MARGIN = 10;

    /**
     * The landing position, plus which centre lines it locked onto — the
     * editor's guides light up for exactly those.
     */
    record Landing(int x, int y, boolean snappedX, boolean snappedY) {
    }

    private LayoutSnap() {
    }

    static Landing apply(int x, int y, int boxW, int boxH, int screenW, int screenH, boolean snap) {
        boolean snappedX = false;
        boolean snappedY = false;

        if (snap) {
            x = Math.round(x / (float) GRID) * GRID;
            y = Math.round(y / (float) GRID) * GRID;

            if (Math.abs(x + boxW / 2 - screenW / 2) <= SNAP) {
                x = (screenW - boxW) / 2;
                snappedX = true;
            } else if (Math.abs(x - SCREEN_MARGIN) <= SNAP) {
                x = SCREEN_MARGIN;
            } else if (Math.abs(screenW - SCREEN_MARGIN - (x + boxW)) <= SNAP) {
                x = screenW - SCREEN_MARGIN - boxW;
            }

            if (Math.abs(y + boxH / 2 - screenH / 2) <= SNAP) {
                y = (screenH - boxH) / 2;
                snappedY = true;
            } else if (Math.abs(y - SCREEN_MARGIN) <= SNAP) {
                y = SCREEN_MARGIN;
            } else if (Math.abs(screenH - SCREEN_MARGIN - (y + boxH)) <= SNAP) {
                y = screenH - SCREEN_MARGIN - boxH;
            }
        }

        return new Landing(
                Mth.clamp(x, 0, Math.max(0, screenW - boxW)),
                Mth.clamp(y, 0, Math.max(0, screenH - boxH)),
                snappedX, snappedY);
    }
}
