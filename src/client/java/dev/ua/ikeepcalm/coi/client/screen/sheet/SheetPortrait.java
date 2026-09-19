package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuIcons;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import dev.ua.ikeepcalm.coi.client.ui.EntityStudy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

/** The fixed identity leaf; gameplay data remains in the server-fed record beside it. */
final class SheetPortrait {
    private SheetPortrait() {}

    static void draw(SheetContext ctx, GuiGraphicsExtractor g, int x, int y, int w, int h) {
        ArchivePaint.paper(g, x, y, w, h);
        boolean horizontal = h < 190;
        int portraitW = horizontal ? Math.min(100, w / 3) : w;
        int portraitH = horizontal ? h - 10 : Math.max(40, h - 90);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        ArchivePaint.seal(g, x + portraitW / 2, y + portraitH / 2,
                Math.max(10, Math.min(portraitW, portraitH) / 2 - 15), 0x38797363);
        EntityStudy.draw(g, x + 8, y + 5, portraitW - 16, portraitH, "", -12, 1);
        int tx = horizontal ? x + portraitW + 6 : x + 14;
        int ty = horizontal ? y + 15 : y + h - 78;
        int tw = x + w - tx - 12;
        String name = SheetContext.player() == null ? I18n.get("screen.coi.sheet_title")
                : SheetContext.player().getName().getString();
        g.text(ctx.font(), ctx.trim(name, tw), tx, ty, ArchivePaint.INK, false);
        g.fill(tx, ty + 13, tx + tw, ty + 14, 0x60797363);
        String pathway = !SheetState.hasData() ? I18n.get("screen.coi.sheet_loading")
                : SheetState.pathwayName().isEmpty() ? I18n.get("screen.coi.sheet_no_pathway") : SheetState.pathwayName();
        g.text(ctx.font(), ctx.trim(pathway, tw), tx, ty + 21, ArchivePaint.INK, false);
        if (SheetState.hasData() && SheetState.sequence() >= 0) {
            String sequence = I18n.get("screen.coi.sheet_sequence_only", SheetState.sequence());
            g.text(ctx.font(), ctx.trim(sequence, tw), tx, ty + 35, ArchivePaint.INK, false);
            g.text(ctx.font(), ctx.trim(SheetState.sequenceName(), tw), tx, ty + 48, ArchivePaint.FAINT_INK, false);
        }
        if (!horizontal && portraitH > 95) {
            int sealY = y + portraitH - 18;
            ArchivePaint.seal(g, x + w - 31, sealY, 20, 0x997B453C);
            MenuIcons.draw(g, ctx.font(), new MenuIcon(MenuIcon.Kind.PATHWAY, SheetState.pathway()),
                    x + w - 43, sealY - 12, 24, 1f);
        }
        g.disableScissor();
    }
}
