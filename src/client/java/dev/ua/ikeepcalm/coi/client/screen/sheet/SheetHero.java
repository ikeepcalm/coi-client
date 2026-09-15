package dev.ua.ikeepcalm.coi.client.screen.sheet;

import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.HEAD;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.SEQ_BADGE_W;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetMetrics.SKIN_SHEET;
import static dev.ua.ikeepcalm.coi.client.screen.sheet.SheetPalette.accent;

import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * The subject of the page: face, name, pathway, sequence.
 * <p>
 * It is the one section that draws before the first packet lands, because the
 * client already knows who the player is — which is what lets the empty state
 * read as "waiting" rather than as "nothing here".
 */
final class SheetHero {

    private SheetHero() {
    }

    /**
     * The subject of the page, in the same translucent accent panel a document's
     * {@code hero} gets: face, name at 1.5x, pathway and sequence beneath, and
     * the sequence again as a numeral in a badge — the one number other players
     * ask for.
     */
    static int draw(SheetContext ctx, GuiGraphicsExtractor graphics, int x, int y, int w) {
        int accent = accent();
        boolean loaded = SheetState.hasData();
        boolean badge = loaded && SheetState.sequence() >= 0;
        int textBlock = 14 + 11 + (loaded ? 11 : 0);
        int core = Math.max(HEAD, textBlock);
        int h = 10 + core + 8;

        MenuTheme.panel(graphics, x, y, w, h, MenuTheme.withAlpha(accent, 0.08f),
                MenuTheme.withAlpha(accent, 0.30f));

        int coreTop = y + 10;
        drawHead(graphics, x + 8, coreTop + (core - HEAD) / 2);

        int right = x + w - 8;
        if (badge) {
            drawSequenceBadge(ctx, graphics, right - SEQ_BADGE_W, coreTop + (core - HEAD) / 2, accent);
            right -= SEQ_BADGE_W + 8;
        }

        int textX = x + 8 + HEAD + 8;
        int textW = Math.max(20, right - textX);
        int textY = coreTop + (core - textBlock) / 2;

        AbstractClientPlayer player = SheetContext.player();
        String name = player == null ? "" : player.getName().getString();
        // plainSubstrByWidth measures at 1x, so the budget is divided by the
        // scale before clipping rather than after
        MenuTheme.scaledText(graphics, ctx.font(),
                ctx.font().plainSubstrByWidth(name, (int) (textW / 1.5f)), textX, textY, 1.5f, accent, true);

        if (!loaded) {
            graphics.text(ctx.font(), ctx.trim(I18n.get("screen.coi.sheet_loading"), textW),
                    textX, textY + 15, CoiStyle.TEXT_MUTED, false);
            return y + h;
        }

        int emblemW = CoiIcons.drawPathwayEmblem(graphics, ctx.font(), SheetState.pathway(),
                textX, textY + 15, accent);
        graphics.text(ctx.font(), ctx.trim(pathwayCaption(), textW - emblemW - 4),
                textX + emblemW + 4, textY + 15, accent, false);
        graphics.text(ctx.font(), ctx.trim(sequenceCaption(), textW), textX, textY + 26,
                CoiStyle.TEXT_MUTED, false);
        return y + h;
    }

    /**
     * The player's face and its hat layer, scaled up from the 8×8 patches of
     * the 64×64 skin sheet — the same two blits the character plate makes, four
     * times the size.
     */
    private static void drawHead(GuiGraphicsExtractor graphics, int x, int y) {
        AbstractClientPlayer player = SheetContext.player();
        graphics.outline(x - 1, y - 1, HEAD + 2, HEAD + 2, MenuTheme.withAlpha(accent(), 0.35f));
        if (player == null) {
            graphics.fill(x, y, x + HEAD, y + HEAD, 0xFF2A2A32);
            return;
        }
        Identifier skin = player.getSkin().body().texturePath();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
    }

    /**
     * The sequence as a numeral rather than a word in a sentence.
     */
    private static void drawSequenceBadge(SheetContext ctx, GuiGraphicsExtractor graphics,
                                          int x, int y, int accent) {
        MenuTheme.panel(graphics, x, y, SEQ_BADGE_W, HEAD, MenuTheme.withAlpha(accent, 0.14f),
                MenuTheme.withAlpha(accent, 0.45f));

        String tag = I18n.get("screen.coi.sheet_seq_badge");
        graphics.text(ctx.font(), tag, x + (SEQ_BADGE_W - ctx.font().width(tag)) / 2, y + 4,
                CoiStyle.TEXT_MUTED, false);

        String number = String.valueOf(SheetState.sequence());
        MenuTheme.scaledText(graphics, ctx.font(), number,
                x + (SEQ_BADGE_W - ctx.font().width(number) * 2) / 2, y + 13, 2f, accent, true);
    }

    private static String pathwayCaption() {
        String name = SheetState.pathwayName();
        return name.isEmpty() ? I18n.get("screen.coi.sheet_no_pathway") : name.toUpperCase(Locale.ROOT);
    }

    private static String sequenceCaption() {
        int sequence = SheetState.sequence();
        if (sequence < 0) return "";
        return SheetState.sequenceName().isEmpty()
                ? I18n.get("screen.coi.sheet_sequence_only", sequence)
                : I18n.get("screen.coi.sheet_sequence", sequence, SheetState.sequenceName());
    }
}
