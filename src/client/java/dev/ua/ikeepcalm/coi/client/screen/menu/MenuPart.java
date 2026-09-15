package dev.ua.ikeepcalm.coi.client.screen.menu;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.GAUGE_MS;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.HOVER_MS;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One laid-out piece of the document, positioned in content space: the
 * render loop adds {@code viewTop - scrollY}. Heights are decided once,
 * here, so the scrollbar and the hit test can never disagree with what was
 * drawn.
 */
abstract class MenuPart {

    protected final MenuContext ctx;
    protected final Font font;

    int y;
    int height;

    /**
     * Eased hover, and the eased gauge fill — one of each is all any part
     * has needed so far; the parts that hold several keep their own arrays.
     */
    float hoverT;
    float shownT;

    MenuPart(MenuContext ctx) {
        this.ctx = ctx;
        this.font = ctx.font();
    }

    abstract void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY);

    boolean click(double mx, double my, int x, int top) {
        return false;
    }

    float hover(boolean hovered) {
        hoverT = ctx.approach(hoverT, hovered ? 1f : 0f, HOVER_MS);
        return hoverT;
    }

    double shown(double target) {
        shownT = ctx.approach(shownT, (float) target, GAUGE_MS);
        return shownT;
    }
}
