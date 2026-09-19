package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.HEADING_H;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.SMALL_ICON;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.accent;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuIcons;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * What a section of the sheet is given to draw itself with.
 * <p>
 * The sheet's draw <em>is</em> its layout — every section returns the y it
 * reached — so a section is handed this rather than the screen: the font it
 * measures with, the easing chokepoint, the caption every section opens with,
 * and the two things a frame collects on its way down the page, the hit boxes
 * and whatever the cursor is over. Everything here is per-frame or per-screen,
 * which is why the sections themselves hold no state and are static.
 */
final class SheetContext {

    /**
     * A box the mouse can reach inside the scrolling column: a destination card
     * or the terrain toggle. Rebuilt every frame during the draw, in absolute
     * screen coordinates.
     */
    record Hit(int x, int y, int w, int h, Runnable action, Component tip) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Font font;
    private final Consumer<String> opener;
    private final List<Hit> hits = new ArrayList<>();

    private Component hoveredTip;
    private int viewTop;
    private int viewBottom;
    private float frameDelta;
    private boolean compact;
    private int spareHeight;
    private int spareGaps;

    SheetContext(Font font, Consumer<String> opener) {
        this.font = font;
        this.opener = opener;
    }

    /**
     * Starts a frame. The hit boxes and the hover are rebuilt by the draw, so
     * they are dropped here rather than carried over from the last one.
     */
    void beginFrame(float delta, int top, int bottom, boolean compactLayout) {
        this.frameDelta = delta;
        this.viewTop = top;
        this.viewBottom = bottom;
        this.compact = compactLayout;
        hits.clear();
        hoveredTip = null;
    }

    Font font() {
        return font;
    }

    int viewTop() {
        return viewTop;
    }

    int viewBottom() {
        return viewBottom;
    }

    List<Hit> hits() {
        return hits;
    }

    void addHit(Hit hit) {
        hits.add(hit);
    }

    void hover(Component tip) {
        hoveredTip = tip;
    }

    Component hoveredTip() {
        return hoveredTip;
    }

    /** Hands a destination to the screen, which hands it to the server. */
    void open(String target) {
        opener.accept(target);
    }

    /** The local player, or null before there is one. */
    static AbstractClientPlayer player() {
        Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.player;
    }

    /** The gap between two sections, which a short window tightens. */
    int sectionGap() {
        return compact ? 4 : 8;
    }

    void distributeSpace(int pixels, int gaps) {
        spareHeight = Math.max(0, pixels);
        spareGaps = gaps;
    }

    int expandedGap(int minimum) {
        if (spareGaps <= 0) return minimum;
        int extra = spareHeight / spareGaps--;
        spareHeight -= extra;
        return minimum + extra;
    }

    /**
     * One step of a tween toward {@code target}, frame-rate independent. Under
     * {@code epilepsyMode} it returns the target outright, which is what makes
     * the chrome snap rather than move — one place to honour the setting, as on
     * the menu screen.
     */
    float approach(float current, float target, float durationMs) {
        if (HudConfig.getSettings().epilepsyMode || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    /**
     * A plain caption and a pathway-colored rule. Letter spacing is kept natural for reading.
     */
    int section(GuiGraphicsExtractor graphics, int x, int y, int w, String key, String glyph) {
        if (graphics == null) return y + HEADING_H;
        int accent = accent();
        int left = x;
        if (MenuIcons.draw(graphics, font(), new MenuIcon(MenuIcon.Kind.GLYPH, glyph),
                left, y + 1, SMALL_ICON, 1f)) {
            left += SMALL_ICON + 3;
        }
        String caption = trim(I18n.get(key), x + w - left);
        graphics.text(font(), caption, left, y + 3, 0xFFE4D9BF, false);
        int used = font().width(caption);
        MenuTheme.headingRule(graphics, left + used + 5, y + 6, x + w, accent);
        return y + HEADING_H;
    }

    String trim(String text, int maxW) {
        if (text == null) return "";
        if (maxW <= 0) return "";
        if (this.font.width(text) <= maxW) return text;
        return this.font.plainSubstrByWidth(text, Math.max(0, maxW - this.font.width("…"))) + "…";
    }

    static String num(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    /**
     * A percentage as a bare whole number — the {@code %} sign belongs to the
     * lang string, which is the only place that knows where it goes.
     */
    static String round0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
