package dev.ua.ikeepcalm.coi.client.screen.title;

import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The plate drawn in place of a vanilla button's stone sprite on the title
 * screen.
 * <p>
 * The 2px accent rail down the left edge is not decoration: it is the same mark
 * the character sheet's destination cards carry ({@code SheetDestinations}), and
 * it is what makes the menu and the rest of the mod read as one interface
 * rather than as a themed launcher in front of somebody else's game. A disabled
 * control loses it — the rail says "this leads somewhere".
 * <p>
 * Only the background is ours. The label is drawn afterwards by vanilla's own
 * {@code extractContents}, which is why nothing here touches text.
 */
public class TitleButtons {

    /**
     * Near-black and translucent, so the void reads through the plate.
     */
    private static final int PLATE = 0xC00A0910;

    private TitleButtons() {
    }

    public static void plate(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                             boolean hovered, boolean enabled) {
        if (w <= 0 || h <= 0) return;
        int accent = TitleTakeover.accent();
        boolean lifted = hovered && enabled;

        int border = !enabled ? MenuTheme.BORDER_OFF
                : lifted ? accent : MenuTheme.withAlpha(accent, 0.30f);
        MenuTheme.panel(graphics, x, y, w, h, PLATE, border);
        // The hover lift goes on as its own layer rather than as a blend of the
        // two: SURFACE_HOVER is a translucent white wash, and lerping toward it
        // would take the plate's own opacity away with it.
        if (lifted) {
            MenuTheme.panel(graphics, x, y, w, h, MenuTheme.SURFACE_HOVER, 0);
        }
        if (enabled) {
            graphics.fill(x, y + 1, x + 2, y + h - 1, accent);
        }
    }
}
