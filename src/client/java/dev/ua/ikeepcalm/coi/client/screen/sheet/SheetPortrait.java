package dev.ua.ikeepcalm.coi.client.screen.sheet;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuIcons;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import dev.ua.ikeepcalm.coi.client.ui.EntityStudy;
import dev.ua.ikeepcalm.coi.client.ui.PortraitPose;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

/** The fixed identity leaf; gameplay data remains in the server-fed record beside it. */
public class SheetPortrait {
    private float lookYaw;
    private float lookPitch;

    void draw(SheetContext ctx, GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, 0xFF171B21);
        g.outline(x, y, w, h, 0xFF565344);
        boolean horizontal = h < 190;
        int portraitW = horizontal ? Math.min(100, w / 3) : w;
        int portraitH = horizontal ? h - 10 : Math.max(40, h - 90);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        PathwayPortraitScene.draw(g, x + 1, y + 1, portraitW - 2, portraitH,
                SheetState.pathway(), SheetState.sequence());
        if (HudConfig.getSettings().epilepsyMode) {
            lookYaw = lookPitch = 0;
        } else {
            float cursorX = Math.clamp((mouseX - (x + portraitW * .5f)) / Math.max(180f, portraitW), -1f, 1f);
            float vertical = Math.clamp((mouseY - (y + portraitH * .25f)) / Math.max(140f, portraitH), -1f, 1f);
            lookYaw = ctx.approach(lookYaw, -cursorX * .105f, 150f);
            lookPitch = ctx.approach(lookPitch, vertical * .07f, 150f);
        }
        PortraitPose pose = SheetState.hasData() && SheetState.sequence() >= 0
                ? new PortraitPose(SheetState.pathway(), SheetState.sequence(), lookYaw, lookPitch) : null;
        EntityStudy.draw(g, x + 3, y + 5, portraitW - 6, portraitH - 4, "", -12, 1, pose);
        if (SheetState.pathway().equals("chained") && pose != null) {
            // Foreground restraints bind the portrait, never the live entity.
            g.nextStratum();
            double left = x + portraitW * .11, right = x + portraitW * .89;
            double wrists = y + portraitH * .36;
            PortraitElements.chain(g, x + 4, y + 5, left, wrists, 0xD5B4B7C7);
            PortraitElements.chain(g, x + portraitW - 4, y + 5, right, wrists, 0xD5B4B7C7);
            if (SheetState.sequence() <= 4) {
                PortraitElements.chain(g, left, wrists, x + portraitW * .63, y + portraitH * .65, 0xABB4B7C7);
                PortraitElements.chain(g, right, wrists, x + portraitW * .37, y + portraitH * .65, 0xABB4B7C7);
            }
        }
        int tx = horizontal ? x + portraitW + 6 : x + 14;
        int ty = horizontal ? y + 15 : y + h - 78;
        int tw = x + w - tx - 12;
        String name = SheetContext.player() == null ? I18n.get("screen.coi.sheet_title")
                : SheetContext.player().getName().getString();
        g.text(ctx.font(), ctx.trim(name, tw), tx, ty, ArchivePaint.LABEL, false);
        g.fill(tx, ty + 13, tx + tw, ty + 14, 0x60797363);
        String pathway = !SheetState.hasData() ? I18n.get("screen.coi.sheet_loading")
                : SheetState.pathwayName().isEmpty() ? I18n.get("screen.coi.sheet_no_pathway") : SheetState.pathwayName();
        g.text(ctx.font(), ctx.trim(pathway, tw), tx, ty + 21, ArchivePaint.LABEL, false);
        if (SheetState.hasData() && SheetState.sequence() >= 0) {
            String sequence = I18n.get("screen.coi.sheet_sequence_only", SheetState.sequence());
            g.text(ctx.font(), ctx.trim(sequence, tw), tx, ty + 35, ArchivePaint.LABEL, false);
            g.text(ctx.font(), ctx.trim(SheetState.sequenceName(), tw), tx, ty + 48, 0xFFAFA994, false);
        }
        if (!horizontal && portraitH > 95) {
            int sealY = y + portraitH - 18;
            MenuIcons.draw(g, ctx.font(), new MenuIcon(MenuIcon.Kind.PATHWAY, SheetState.pathway()),
                    x + w - 43, sealY - 12, 24, 1f);
        }
        g.disableScissor();
    }
}
