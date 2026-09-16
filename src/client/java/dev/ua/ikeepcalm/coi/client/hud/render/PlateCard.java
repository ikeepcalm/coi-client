package dev.ua.ikeepcalm.coi.client.hud.render;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The character plate's paint: the card, its header and one row per gauge or
 * reserve. Stateless, like the rest of {@code hud.render} — every value it
 * draws is handed to it, so the same methods serve the live overlay and the
 * layout editor's sample-data preview and the two can never draw differently.
 * <p>
 * The interesting decision here is {@link #height}: the card's height is a
 * function of the row <em>counts</em>, so a row with no data is omitted and the
 * card shrinks to fit rather than showing an empty rail.
 * <p>
 * <b>Every colour drawn here goes through {@link HudOpacity#apply}</b> — fills,
 * outlines, text colours and the tints handed to {@code blit} alike. The plate
 * is the one element with a transparency setting, and one colour that skipped
 * it would leave a piece of the card solid while the rest faded, which reads as
 * a bug rather than as a setting. Outside the overlay's push {@code apply} is
 * the identity, so this costs the preview and the live card nothing.
 */
public final class PlateCard {

    /**
     * The card's fixed width. Every interior column below is measured off it,
     * so the plate has one number to tune.
     */
    public static final int CARD_W = 168;

    public static final int PAD = 6;

    /**
     * The header block: a 16px head beside two 8px text lines (name, pathway),
     * which is what sets the 20 rather than the head's own height.
     */
    public static final int HEADER_H = 20;
    public static final int HEAD = 16;
    public static final int HEAD_GAP = 6;
    /**
     * The second header line (the pathway) sits under the first.
     */
    public static final int HEADER_LINE_2 = 11;
    private static final int SKIN_SHEET = 64;
    private static final int CREST_GAP = 3;

    /**
     * A gauge row is exactly as tall as its symbol.
     */
    public static final int ROW_H = PlateSymbols.SIZE;
    public static final int ROW_GAP = 2;
    private static final int VALUE_W = 34;
    private static final int GAUGE_BAR_X = PAD + PlateSymbols.SIZE + 6;
    private static final int GAUGE_BAR_W = CARD_W - PAD - VALUE_W - GAUGE_BAR_X;
    private static final int GAUGE_BAR_H = 6;

    /**
     * Reserve rows are compact and symbol-less: the server names them, so the
     * label has to carry the identity instead.
     */
    public static final int DIVIDER_GAP = 4;
    public static final int RES_ROW_H = 11;
    private static final int RES_VALUE_W = 30;
    private static final int RES_BAR_W = 56;
    private static final int RES_BAR_H = 3;
    private static final int RES_BAR_X = CARD_W - PAD - RES_VALUE_W - 4 - RES_BAR_W;

    private static final int BORDER = 0xCC000000;
    private static final int DIVIDER = 0x40FFFFFF;
    private static final int VALUE_COLOR = 0xFFE0E0E0;

    /**
     * How far the {@code +N} popup rises over its fade.
     */
    private static final int GRANT_RISE = 5;

    private PlateCard() {
    }

    /**
     * One gauge row: symbol, bar, a right-aligned value and an optional muted
     * second line under it (the acting method's cooldown).
     *
     * @param rgb  the row's state colour, used by the bar
     * @param wash multiplied over the symbol's filled part; {@link PlateSymbols#NO_WASH}
     *             leaves the artwork alone, which is what a gauge wants until it has
     *             something urgent to say
     */
    public record Gauge(Identifier symbol, float fill, float cap, int rgb, int wash,
                        Component value, Component sub) {
        public Gauge(Identifier symbol, float fill, float cap, int rgb, int wash, Component value) {
            this(symbol, fill, cap, rgb, wash, value, null);
        }
    }

    /**
     * One reserve row, already reduced to what the card draws.
     */
    public record Reserve(String label, float fill, int rgb, Component value) {
    }

    /**
     * Card height for a given row count — the one place the stacking is
     * defined, so {@link #draw} and the layout editor cannot drift.
     */
    public static int height(int gaugeCount, int reserveCount) {
        int h = PAD + HEADER_H;
        h += gaugeCount * (ROW_GAP + ROW_H);
        if (reserveCount > 0) {
            h += DIVIDER_GAP + 1 + DIVIDER_GAP + reserveCount * RES_ROW_H;
        }
        return h + PAD;
    }

    /**
     * The whole live card: chrome, header, gauge rows and — under a divider —
     * the reserve rows.
     */
    public static void draw(GuiGraphicsExtractor ctx, Font font, int x, int y,
                            AbstractClientPlayer player, String name, String pathway, int sequence,
                            List<Gauge> gauges, List<Reserve> reserves) {
        drawChrome(ctx, x, y, CARD_W, height(gauges.size(), reserves.size()));
        drawHeader(ctx, font, x, y, player, name, pathway, sequence);

        int rowY = drawGauges(ctx, font, x, y + PAD + HEADER_H, gauges);
        if (reserves.isEmpty()) return;

        rowY = drawDivider(ctx, x, rowY);
        for (Reserve reserve : reserves) {
            drawReserve(ctx, font, x, rowY, reserve);
            rowY += RES_ROW_H;
        }
    }

    /**
     * {@link CoiStyle#drawCard}'s recipe, in {@link CoiStyle}'s own three
     * colours, with each one put through {@link HudOpacity}.
     * <p>
     * It is restated here rather than added to {@code CoiStyle} because that
     * class is the chrome of the mod's <em>screens</em>, which have no ambient
     * alpha and would have to import {@code hud} to get one. Only the three
     * draw calls are duplicated — the colours are still read from
     * {@code CoiStyle}, so the plate cannot drift away from the card language
     * the sheet and the menus use.
     */
    public static void drawChrome(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, HudOpacity.apply(CoiStyle.CARD_BG));
        ctx.outline(x, y, w, h, HudOpacity.apply(CoiStyle.BORDER));
        ctx.fill(x, y, x + w, y + 1, HudOpacity.apply(CoiStyle.ACCENT));
    }

    /**
     * Draws every gauge row from {@code rowY} down.
     *
     * @return the y the rows reached
     */
    public static int drawGauges(GuiGraphicsExtractor ctx, Font font, int x, int rowY, List<Gauge> gauges) {
        for (Gauge gauge : gauges) {
            rowY += ROW_GAP;
            drawGauge(ctx, font, x, rowY, gauge);
            rowY += ROW_H;
        }
        return rowY;
    }

    /**
     * The hairline that separates the reserve rows from the gauges.
     *
     * @return the y the first reserve row starts at
     */
    public static int drawDivider(GuiGraphicsExtractor ctx, int x, int rowY) {
        rowY += DIVIDER_GAP;
        ctx.fill(x + PAD, rowY, x + CARD_W - PAD, rowY + 1, HudOpacity.apply(DIVIDER));
        return rowY + 1 + DIVIDER_GAP;
    }

    /**
     * Head, name, and the pathway line the crest colours.
     */
    private static void drawHeader(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                   AbstractClientPlayer player, String name, String pathway, int sequence) {
        int headX = x + PAD;
        // Centred against the two text lines, not against the padding
        int headY = y + PAD + (HEADER_H - HEAD) / 2;
        drawHead(ctx, player, headX, headY);

        int textX = headX + HEAD + HEAD_GAP;
        int textW = CARD_W - PAD - (textX - x);
        ctx.text(font, trim(font, name, textW), textX, y + PAD, HudOpacity.apply(CoiStyle.TEXT_BODY), true);

        if (pathway == null || pathway.isEmpty()) return;
        drawPathwayLine(ctx, font, textX, y + PAD + HEADER_LINE_2, pathway, sequence,
                Pathways.pathwayRgb(pathway));
    }

    /**
     * Emblem plus caption. The mod ships a real 9px emblem for every pathway
     * behind the {@code pathway_icons} font, which is what the character sheet
     * has always drawn — the plate uses the same one so the two screens can
     * never show a player two different symbols for the same pathway. Names
     * stay English and upper-cased, as everywhere else in the mod.
     */
    public static void drawPathwayLine(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                       String pathway, int sequence, int rgb) {
        int argb = HudOpacity.apply(EffectPaint.argb(rgb, 255));
        int emblemW = CoiIcons.drawPathwayEmblem(ctx, font, pathway, x, y, argb);
        int textX = x + emblemW + CREST_GAP;
        String upper = pathway.toUpperCase(Locale.ROOT);
        Component caption = sequence >= 0
                ? Component.translatable("hud.coi.plate_pathway", upper, sequence)
                : Component.translatable("hud.coi.plate_pathway_only", upper);
        ctx.text(font, caption, textX, y, argb, true);
    }

    /**
     * The player's face and its hat layer, scaled up from the 8×8 patches of
     * the 64×64 skin sheet.
     */
    public static void drawHead(GuiGraphicsExtractor ctx, AbstractClientPlayer player, int x, int y) {
        if (player == null) {
            ctx.fill(x, y, x + HEAD, y + HEAD, HudOpacity.apply(EffectPaint.argb(0x2A2A32, 255)));
            return;
        }
        Identifier skin = player.getSkin().body().texturePath();
        // The head is the one blit whose tint is otherwise plain white, which
        // is exactly how a face ends up floating solid over a faded card
        int tint = HudOpacity.apply(0xFFFFFFFF);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, tint);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, tint);
    }

    public static void drawGauge(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Gauge gauge) {
        PlateSymbols.draw(ctx, gauge.symbol(), x + PAD, rowY, gauge.fill(), gauge.cap(), gauge.wash());

        int barX = x + GAUGE_BAR_X;
        int barY = rowY + (ROW_H - GAUGE_BAR_H) / 2;
        // CoiBar owns the frame's background and the fill's bevel, so the fade
        // has to be handed to it as a factor rather than applied to the colours
        float fade = HudOpacity.current();
        CoiBar.frame(ctx, barX, barY, GAUGE_BAR_W, GAUGE_BAR_H, BORDER, fade);
        CoiBar.fill(ctx, barX, barY, GAUGE_BAR_H, CoiBar.lerpWidth(gauge.fill(), 1.0, GAUGE_BAR_W),
                EffectPaint.argb(gauge.rgb(), 255), darken(gauge.rgb()), fade);
        drawCeiling(ctx, barX, barY, gauge.cap());

        int right = x + CARD_W - PAD;
        int valueArgb = HudOpacity.apply(EffectPaint.argb(gauge.rgb(), 255));
        if (gauge.sub() == null) {
            // One line: centre it on the symbol rather than on the bar
            ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + 6, valueArgb, true);
            return;
        }
        ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + 1, valueArgb, true);
        ctx.text(font, gauge.sub(), right - font.width(gauge.sub()), rowY + 11,
                HudOpacity.apply(CoiStyle.TEXT_MUTED), true);
    }

    /**
     * The ceiling on the bar, mirroring the symbol's dead band: a bright tick
     * at the limit and a dimmed remainder past it.
     */
    private static void drawCeiling(GuiGraphicsExtractor ctx, int barX, int barY, float cap) {
        if (cap >= 1f) return;
        int capX = barX + CoiBar.lerpWidth(cap, 1.0, GAUGE_BAR_W);
        ctx.fill(capX, barY, barX + GAUGE_BAR_W, barY + GAUGE_BAR_H, HudOpacity.apply(0x90000000));
        ctx.fill(capX, barY - 1, capX + 1, barY + GAUGE_BAR_H + 1, HudOpacity.apply(0xDDFFFFFF));
    }

    public static void drawReserve(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Reserve reserve) {
        ctx.text(font, trim(font, reserve.label(), RES_BAR_X - PAD - 4), x + PAD, rowY + 1,
                HudOpacity.apply(CoiStyle.TEXT_BODY), true);

        int barX = x + RES_BAR_X;
        int barY = rowY + 4;
        float fade = HudOpacity.current();
        CoiBar.frame(ctx, barX, barY, RES_BAR_W, RES_BAR_H, BORDER, fade);
        CoiBar.fill(ctx, barX, barY, RES_BAR_H, CoiBar.lerpWidth(reserve.fill(), 1.0, RES_BAR_W),
                EffectPaint.argb(reserve.rgb(), 255), darken(reserve.rgb()), fade);

        int right = x + CARD_W - PAD;
        ctx.text(font, reserve.value(), right - font.width(reserve.value()), rowY + 1,
                HudOpacity.apply(VALUE_COLOR), true);
    }

    /**
     * {@code +N} rising out of the acting row's right edge and fading, so a
     * grant is noticeable without another action-bar line. Drawn outside
     * {@link #draw} because it leaves the card.
     *
     * @param rowIndex which gauge row the popup belongs to, counting from 1
     */
    public static void drawGrantPopup(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                      int rowIndex, int rgb, int granted, float progress) {
        Component text = Component.translatable("hud.coi.acting_gain", granted);
        // The gauge row's own top edge is the baseline
        int rowY = y + PAD + HEADER_H + rowIndex * (ROW_GAP + ROW_H) - ROW_H;
        int textY = rowY - 2 - (int) (GRANT_RISE * progress);
        ctx.text(font, text, x + CARD_W - PAD - font.width(text), textY,
                HudOpacity.apply(EffectPaint.argb(rgb, (int) (255 * (1f - progress)))), true);
    }

    /**
     * Bottom edge of the fill gradient: the same hue at ~55% brightness.
     */
    private static int darken(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.55f);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.55f);
        int b = (int) ((rgb & 0xFF) * 0.55f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String trim(Font font, String text, int maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxW - font.width("…"))) + "…";
    }
}
