package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_DEFENSE;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetGlyphs.GLYPH_DIVINATION;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.GAP;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.HOVER_MS;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.NAV_CARD_H;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.THREE_COLUMN_MIN;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.TWO_COLUMN_MIN;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.accent;

import dev.ua.ikeepcalm.coi.client.network.payload.ActionPayload;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * The seven doorways, and the one row that is not a doorway.
 * <p>
 * Terrain damage is drawn as the same card as a destination on purpose — the
 * column reads as one list — but it carries a switch rather than a glyph and a
 * lock, so a preference cannot be mistaken for somewhere to go.
 * <p>
 * The only section that holds state: a card's hover eases, and an eased value
 * has to survive the frame it was measured in.
 */
final class SheetDestinations {

    /**
     * One destination card: the wire target — which is also its lang stem —
     * and the mark that tells it apart at a glance.
     */
    private record Nav(String target, String[] glyph) {
    }

    private static final Nav[] NAV = {
            new Nav("church", SheetGlyphs.CHURCH),
            new Nav("abilities", SheetGlyphs.SPARK),
            new Nav("mythical", SheetGlyphs.BEAST),
            new Nav("uniqueness", SheetGlyphs.GEM),
            new Nav("honorific", SheetGlyphs.CROWN),
            new Nav("map", SheetGlyphs.PIN),
            new Nav("seat", SheetGlyphs.THRONE)
    };

    private final SheetContext ctx;
    private final float[] navHover = new float[NAV.length];
    private float prefsHover;

    SheetDestinations(SheetContext ctx) {
        this.ctx = ctx;
    }

    int draw(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_nav", GLYPH_DIVINATION) + 4;
        // Seven destinations. At the sheet's old 440 these could only ever be two abreast, which
        // was four rows of card; with the shared width a third column fits and it becomes three.
        int cols = w >= THREE_COLUMN_MIN ? 3 : w >= TWO_COLUMN_MIN ? 2 : 1;
        int tileW = (w - GAP * (cols - 1)) / cols;
        boolean[] gates = gates();

        for (int i = 0; i < NAV.length; i++) {
            int cx = x + (i % cols) * (tileW + GAP);
            int cy = ry + (i / cols) * (NAV_CARD_H + GAP);
            drawNavCard(graphics, i, gates[i], cx, cy, tileW, mouseX, mouseY);
        }
        int rows = (NAV.length + cols - 1) / cols;
        return ry + rows * (NAV_CARD_H + GAP) - GAP;
    }

    private static boolean[] gates() {
        SheetState.Actions actions = SheetState.actions();
        boolean[] gates = new boolean[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            gates[i] = actions.unlocked(NAV[i].target());
        }
        return gates;
    }

    /**
     * A destination, not a button: the name is only half of it, the line under
     * it says what is on the other side. A locked card stays legible and
     * explains itself on hover instead of going dead grey with no reason given.
     * <p>
     * Drawn as a {@link MenuTheme#panel} over {@link MenuTheme#surface}, which
     * is the surface a menu's list rows and grid tiles sit on — and the hover
     * eases in rather than snapping, as everything in a document does.
     */
    private void drawNavCard(GuiGraphicsExtractor graphics, int index, boolean unlocked,
                             int x, int y, int w, int mouseX, int mouseY) {
        Nav nav = NAV[index];
        boolean inside = mouseY >= ctx.viewTop() && mouseY < ctx.viewBottom()
                && SheetMetrics.inBox(mouseX, mouseY, x, y, w, NAV_CARD_H);
        navHover[index] = ctx.approach(navHover[index], inside && unlocked ? 1f : 0f, HOVER_MS);
        float hoverT = navHover[index];

        int accent = unlocked ? accent() : CoiStyle.INACTIVE;
        MenuTheme.panel(graphics, x, y, w, NAV_CARD_H, MenuTheme.surface(hoverT),
                unlocked ? MenuTheme.lerpArgb(MenuTheme.withAlpha(accent, 0.30f), accent, hoverT)
                        : MenuTheme.BORDER_OFF);
        graphics.fill(x, y + 1, x + 2, y + NAV_CARD_H - 1, accent);

        SheetGlyphs.draw(graphics, nav.glyph(), x + 7, y + (NAV_CARD_H - 16) / 2, 16, accent);

        int textX = x + 27;
        int textW = x + w - 8 - textX - (unlocked ? 0 : 12);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_btn_" + nav.target()), textW),
                textX, y + 6, unlocked ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE, true);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_nav_" + nav.target() + "_desc"), textW),
                textX, y + 17, unlocked ? CoiStyle.TEXT_MUTED : CoiStyle.INACTIVE, false);

        if (!unlocked) {
            SheetGlyphs.draw(graphics, SheetGlyphs.LOCK, x + w - 16, y + (NAV_CARD_H - 10) / 2, 10,
                    CoiStyle.INACTIVE);
        }
        Component tip = unlocked ? null : Component.translatable("screen.coi.sheet_lock_" + nav.target());
        ctx.addHit(new SheetContext.Hit(x, y, w, NAV_CARD_H,
                unlocked ? () -> ctx.open(nav.target()) : null, tip));
        if (inside && tip != null) ctx.hover(tip);
    }

    /**
     * Terrain damage is a preference the player flips in place, so it gets a
     * switch and its own section rather than an eighth card that looks like a
     * doorway to somewhere.
     */
    int preferences(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        int ry = ctx.section(graphics, x, y, w, "screen.coi.sheet_sec_prefs", GLYPH_DEFENSE) + 4;
        boolean on = SheetState.actions().terrainDamage();
        boolean hovered = mouseY >= ctx.viewTop() && mouseY < ctx.viewBottom()
                && SheetMetrics.inBox(mouseX, mouseY, x, ry, w, NAV_CARD_H);
        prefsHover = ctx.approach(prefsHover, hovered ? 1f : 0f, HOVER_MS);

        int accent = accent();
        int state = on ? MenuTheme.SUCCESS : CoiStyle.INACTIVE;
        MenuTheme.panel(graphics, x, ry, w, NAV_CARD_H, MenuTheme.surface(prefsHover),
                MenuTheme.lerpArgb(MenuTheme.withAlpha(accent, 0.30f), accent, prefsHover));

        SheetGlyphs.draw(graphics, SheetGlyphs.BLOCKS, x + 7, ry + (NAV_CARD_H - 16) / 2, 16, state);

        String label = I18n.get(on ? "screen.coi.sheet_on" : "screen.coi.sheet_off");
        int labelW = ctx.font().width(label);
        int switchX = x + w - 8 - 20;
        int textX = x + 27;
        int textW = switchX - 6 - labelW - 4 - textX;

        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_terrain_title"), textW), textX, ry + 6,
                CoiStyle.TEXT_BODY, true);
        graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_terrain_desc"), textW), textX, ry + 17,
                CoiStyle.TEXT_MUTED, false);
        graphics.text(ctx.font(), label, switchX - 4 - labelW, ry + (NAV_CARD_H - 8) / 2, state, false);
        MenuTheme.toggle(graphics, switchX, ry + (NAV_CARD_H - 10) / 2, on, true, MenuTheme.SUCCESS);

        ctx.addHit(new SheetContext.Hit(x, ry, w, NAV_CARD_H,
                () -> CharacterSheetScreen.send(ActionPayload.of("toggle_terrain")), null));
        return ry + NAV_CARD_H;
    }
}
