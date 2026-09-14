package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientActingState;
import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.ClientResourceState;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;
import dev.ua.ikeepcalm.coi.util.CoiIcons;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One card for the whole character, in place of the four near-identical 182px
 * bars that used to stack down the top-left corner.
 * <p>
 * Madness, acting and the resource meters were visually interchangeable: same
 * width, same frame, same label position, distinguishable only by reading the
 * text. The plate puts the player's identity at the top and gives each meter a
 * row of its own, fronted by a symbol ({@link PlateSymbols}) rather than yet
 * another bar, so a glance is enough to tell them apart.
 * <p>
 * Rows with no data are <em>omitted</em> and the card shrinks to fit, so a
 * protocol-1 server shows a header and a sanity gauge instead of a column of
 * empty rails. Spirituality is deliberately <b>not</b> here — it keeps its own
 * sprite-built bar.
 */
public final class CharacterPlateOverlay {

    private static final Identifier PLATE_LAYER = Identifier.fromNamespaceAndPath("coi-client", "character_plate");

    /**
     * The card's fixed width. Every interior column below is measured off it,
     * so the plate has one number to tune.
     */
    public static final int CARD_W = 168;
    /**
     * Where a TOP-anchored plate sits out of the box — the same line the
     * madness bar it replaces used.
     */
    public static final int DEFAULT_TOP_Y = 20;

    private static final int PAD = 6;

    /**
     * The header block: a 16px head beside two 8px text lines (name, pathway),
     * which is what sets the 20 rather than the head's own height.
     */
    private static final int HEADER_H = 20;
    private static final int HEAD = 16;
    private static final int HEAD_GAP = 6;
    private static final int SKIN_SHEET = 64;
    private static final int CREST_GAP = 3;

    /**
     * A gauge row is exactly as tall as its symbol.
     */
    private static final int ROW_H = PlateSymbols.SIZE;
    private static final int ROW_GAP = 2;
    private static final int VALUE_W = 34;
    private static final int GAUGE_BAR_X = PAD + PlateSymbols.SIZE + 6;
    private static final int GAUGE_BAR_W = CARD_W - PAD - VALUE_W - GAUGE_BAR_X;
    private static final int GAUGE_BAR_H = 6;

    /**
     * Reserve rows are compact and symbol-less: the server names them, so the
     * label has to carry the identity instead.
     */
    private static final int DIVIDER_GAP = 4;
    private static final int RES_ROW_H = 11;
    private static final int RES_VALUE_W = 30;
    private static final int RES_BAR_W = 56;
    private static final int RES_BAR_H = 3;
    private static final int RES_BAR_X = CARD_W - PAD - RES_VALUE_W - 4 - RES_BAR_W;

    private static final int BORDER = 0xCC000000;
    private static final int DIVIDER = 0x40FFFFFF;
    private static final int VALUE_COLOR = 0xFFE0E0E0;

    /**
     * Sanity's palette, indexed by {@link MadnessHudOverlay#stageOf}. Sharing
     * that function is the point: the plate can never disagree with the stage
     * vignette about how far gone the player is.
     */
    private static final int[] SANITY_RGB = {0x00FFCC, 0xFFAA00, 0xDD2222, 0xFF0055, 0x993399};

    private CharacterPlateOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, PLATE_LAYER, CharacterPlateOverlay::render);
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
    private record Gauge(Identifier symbol, float fill, float cap, int rgb, int wash,
                         Component value, Component sub) {
        Gauge(Identifier symbol, float fill, float cap, int rgb, int wash, Component value) {
            this(symbol, fill, cap, rgb, wash, value, null);
        }
    }

    /**
     * One reserve row, already reduced to what the card draws.
     */
    private record Reserve(String label, float fill, int rgb, Component value) {
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.gui.hud.isHidden() || HudLayout.editing()
                || !settings.enabled || !settings.showCharacterPlate || !hasAnything()) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        List<Gauge> gauges = liveGauges();
        List<Reserve> reserves = liveReserves(settings);
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.characterPlateScale);
        drawPlate(ctx, client.font, pos[0], pos[1], client.player, client.player.getName().getString(),
                gauges, reserves);
        drawGrantPopup(ctx, client.font, pos[0], pos[1], gauges);
        HudScale.pop(ctx);
    }

    /**
     * Whether the plate has anything worth a card. A vanilla server answers
     * nothing at all, and a card holding only the player's own head would be
     * noise rather than a HUD.
     */
    private static boolean hasAnything() {
        return ClientBeyonderState.hasIdentity()
                || ClientBeyonderState.getMadness() > 0
                || ClientBeyonderState.getPermanentMadness() > 0
                || (ClientActingState.hasData() && !ClientActingState.isOuter())
                || ClientResourceState.hasData();
    }

    // --- Geometry ---

    /**
     * Top-left corner of the card. Unlike the bars, the plate's drawn bounds
     * <em>are</em> its fill origin — there is no label hanging above it — which
     * is what keeps the layout editor's bounds/moveTo pair exact at any scale
     * without any padding arithmetic.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.characterPlateAnchor).resolve(
                screenW, screenH, HudScale.size(CARD_W, s.characterPlateScale),
                s.characterPlateXOffset, s.characterPlateYOffset, s.characterPlateYOffset);
    }

    /**
     * Card height for a given row count — the one place the stacking is
     * defined, so {@link #drawPlate} and the layout editor cannot drift.
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
     * The height the layout editor works against. It is the preview's, not the
     * live card's: the editor draws the preview, so the grab box has to match
     * that and not whatever the server happens to be sending behind the screen.
     */
    public static int previewHeight() {
        return height(2, 1);
    }

    // --- Live data ---

    private static List<Gauge> liveGauges() {
        List<Gauge> gauges = new ArrayList<>(2);
        gauges.add(sanityGauge(ClientBeyonderState.getMadness(), ClientBeyonderState.getPermanentMadness()));
        if (ClientActingState.hasData() && !ClientActingState.isOuter()) {
            gauges.add(actingGauge(ClientActingState.getPercent(), ClientActingState.getPathway(),
                    ClientActingState.cooldownRemainingNow() > 0 ? ClientActingState.cooldownClock() : null));
        }
        return gauges;
    }

    /**
     * Madness read the other way up: a full brain is a clear head, a draining
     * one is a player losing it. Permanent madness is headroom that never comes
     * back, so it is a ceiling on the gauge rather than a second fill.
     */
    private static Gauge sanityGauge(double madness, double permanentMadness) {
        double sanity = Math.clamp(100.0 - madness, 0, 100);
        float cap = (float) (Math.clamp(100.0 - permanentMadness, 0, 100) / 100.0);
        int stage = Mth.clamp(MadnessHudOverlay.stageOf(madness), 0, SANITY_RGB.length - 1);
        int rgb = SANITY_RGB[stage];
        // Below stage 2 the brain keeps its own colour and only the bar carries
        // the state; from there the wash bleeds the artwork itself towards the
        // stage colour, so a player losing it can see it without reading a number
        int wash = stage < 2 ? PlateSymbols.NO_WASH : rgb;
        return new Gauge(PlateSymbols.BRAIN, (float) (sanity / 100.0), cap, rgb, wash,
                Component.translatable("hud.coi.plate_sanity_value", Math.round(sanity)));
    }

    private static Gauge actingGauge(double percent, String pathway, String cooldownClock) {
        double clamped = Math.clamp(percent, 0, 100);
        // The mask is already vivid; the pathway colour rides the bar instead, or
        // the wash would just muddy the artwork
        return new Gauge(PlateSymbols.MASK, (float) (clamped / 100.0), 1f, AbilityInfo.pathwayRgb(pathway),
                PlateSymbols.NO_WASH,
                Component.translatable("hud.coi.plate_percent", String.format(Locale.ROOT, "%.0f", clamped)),
                cooldownClock == null ? null : Component.translatable("hud.coi.plate_cooldown", cooldownClock));
    }

    private static List<Reserve> liveReserves(HudConfig.HudSettings s) {
        List<ClientResourceState.Entry> entries = ClientResourceState.visible();
        int count = Math.min(entries.size(), Math.max(0, s.resourceMaxBars));
        List<Reserve> reserves = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ClientResourceState.Entry entry = entries.get(i);
            double max = entry.max() > 0 ? entry.max() : 1;
            reserves.add(new Reserve(entry.label(), (float) (entry.current() / max), entry.rgb(),
                    reserveValue(entry.current(), max, entry.percent())));
        }
        return reserves;
    }

    /**
     * A percent meter reads as one number; a counted one keeps both sides, and
     * matching decimals so {@code 2.5 / 5.0} never reads as {@code 2.5 / 5}.
     */
    private static Component reserveValue(double current, double max, boolean asPercent) {
        if (asPercent) {
            double percent = max > 0 ? current * 100.0 / max : 0;
            return Component.translatable("hud.coi.plate_percent", String.format(Locale.ROOT, "%.0f", percent));
        }
        boolean whole = current == Math.rint(current) && max == Math.rint(max);
        return Component.translatable("hud.coi.plate_amount", number(current, whole), number(max, whole));
    }

    private static String number(double value, boolean whole) {
        return whole ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    // --- Drawing ---

    private static void drawPlate(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                  AbstractClientPlayer player, String name,
                                  List<Gauge> gauges, List<Reserve> reserves) {
        CoiStyle.drawCard(ctx, x, y, CARD_W, height(gauges.size(), reserves.size()));
        drawHeader(ctx, font, x, y, player, name);

        int rowY = y + PAD + HEADER_H;
        for (Gauge gauge : gauges) {
            rowY += ROW_GAP;
            drawGauge(ctx, font, x, rowY, gauge);
            rowY += ROW_H;
        }
        if (reserves.isEmpty()) return;

        rowY += DIVIDER_GAP;
        ctx.fill(x + PAD, rowY, x + CARD_W - PAD, rowY + 1, DIVIDER);
        rowY += 1 + DIVIDER_GAP;
        for (Reserve reserve : reserves) {
            drawReserve(ctx, font, x, rowY, reserve);
            rowY += RES_ROW_H;
        }
    }

    /**
     * Head, name, and the pathway line the crest colours.
     */
    private static void drawHeader(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                   AbstractClientPlayer player, String name) {
        int headX = x + PAD;
        // Centred against the two text lines, not against the padding
        int headY = y + PAD + (HEADER_H - HEAD) / 2;
        drawHead(ctx, player, headX, headY);

        int textX = headX + HEAD + HEAD_GAP;
        int textW = CARD_W - PAD - (textX - x);
        ctx.text(font, trim(font, name, textW), textX, y + PAD, CoiStyle.TEXT_BODY, true);

        String pathway = ClientBeyonderState.getPathway();
        if (pathway == null || pathway.isEmpty()) return;
        drawPathwayLine(ctx, font, textX, y + PAD + 11, textW,
                pathway, ClientBeyonderState.getSequence(), AbilityInfo.pathwayRgb(pathway));
    }

    /**
     * Emblem plus caption. The mod ships a real 9px emblem for every pathway
     * behind the {@code pathway_icons} font, which is what the character sheet
     * has always drawn — the plate uses the same one so the two screens can
     * never show a player two different symbols for the same pathway. Names
     * stay English and upper-cased, as everywhere else in the mod.
     */
    private static void drawPathwayLine(GuiGraphicsExtractor ctx, Font font, int x, int y, int maxW,
                                        String pathway, int sequence, int rgb) {
        int emblemW = CoiIcons.drawPathwayEmblem(ctx, font, pathway, x, y, EffectPaint.argb(rgb, 255));
        int textX = x + emblemW + CREST_GAP;
        String upper = pathway.toUpperCase(Locale.ROOT);
        Component caption = sequence >= 0
                ? Component.translatable("hud.coi.plate_pathway", upper, sequence)
                : Component.translatable("hud.coi.plate_pathway_only", upper);
        ctx.text(font, caption, textX, y, EffectPaint.argb(rgb, 255), true);
    }

    /**
     * The player's face and its hat layer, scaled up from the 8×8 patches of
     * the 64×64 skin sheet.
     */
    private static void drawHead(GuiGraphicsExtractor ctx, AbstractClientPlayer player, int x, int y) {
        if (player == null) {
            ctx.fill(x, y, x + HEAD, y + HEAD, EffectPaint.argb(0x2A2A32, 255));
            return;
        }
        Identifier skin = player.getSkin().body().texturePath();
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        ctx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD, HEAD, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
    }

    private static void drawGauge(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Gauge gauge) {
        PlateSymbols.draw(ctx, gauge.symbol(), x + PAD, rowY, gauge.fill(), gauge.cap(), gauge.wash());

        int barX = x + GAUGE_BAR_X;
        int barY = rowY + (ROW_H - GAUGE_BAR_H) / 2;
        CoiBar.frame(ctx, barX, barY, GAUGE_BAR_W, GAUGE_BAR_H, BORDER);
        CoiBar.fill(ctx, barX, barY, GAUGE_BAR_H, CoiBar.lerpWidth(gauge.fill(), 1.0, GAUGE_BAR_W),
                EffectPaint.argb(gauge.rgb(), 255), darken(gauge.rgb()));
        drawCeiling(ctx, barX, barY, gauge.cap());

        int right = x + CARD_W - PAD;
        if (gauge.sub() == null) {
            // One line: centre it on the symbol rather than on the bar
            ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + 6,
                    EffectPaint.argb(gauge.rgb(), 255), true);
            return;
        }
        ctx.text(font, gauge.value(), right - font.width(gauge.value()), rowY + 1,
                EffectPaint.argb(gauge.rgb(), 255), true);
        ctx.text(font, gauge.sub(), right - font.width(gauge.sub()), rowY + 11, CoiStyle.TEXT_MUTED, true);
    }

    /**
     * The ceiling on the bar, mirroring the symbol's dead band: a bright tick
     * at the limit and a dimmed remainder past it.
     */
    private static void drawCeiling(GuiGraphicsExtractor ctx, int barX, int barY, float cap) {
        if (cap >= 1f) return;
        int capX = barX + CoiBar.lerpWidth(cap, 1.0, GAUGE_BAR_W);
        ctx.fill(capX, barY, barX + GAUGE_BAR_W, barY + GAUGE_BAR_H, 0x90000000);
        ctx.fill(capX, barY - 1, capX + 1, barY + GAUGE_BAR_H + 1, 0xDDFFFFFF);
    }

    private static void drawReserve(GuiGraphicsExtractor ctx, Font font, int x, int rowY, Reserve reserve) {
        ctx.text(font, trim(font, reserve.label(), RES_BAR_X - PAD - 4), x + PAD, rowY + 1,
                CoiStyle.TEXT_BODY, true);

        int barX = x + RES_BAR_X;
        int barY = rowY + 4;
        CoiBar.frame(ctx, barX, barY, RES_BAR_W, RES_BAR_H, BORDER);
        CoiBar.fill(ctx, barX, barY, RES_BAR_H, CoiBar.lerpWidth(reserve.fill(), 1.0, RES_BAR_W),
                EffectPaint.argb(reserve.rgb(), 255), darken(reserve.rgb()));

        int right = x + CARD_W - PAD;
        ctx.text(font, reserve.value(), right - font.width(reserve.value()), rowY + 1, VALUE_COLOR, true);
    }

    /**
     * {@code +N} rising out of the acting row's right edge and fading, so a
     * grant is noticeable without another action-bar line. Drawn outside
     * {@link #drawPlate} because it leaves the card.
     */
    private static void drawGrantPopup(GuiGraphicsExtractor ctx, Font font, int x, int y, List<Gauge> gauges) {
        int granted = ClientActingState.activeGrant();
        if (granted <= 0 || gauges.size() < 2) return;

        float progress = ClientActingState.grantProgress();
        Component text = Component.translatable("hud.coi.acting_gain", granted);
        // The acting gauge is the second row; its own top edge is the baseline
        int rowY = y + PAD + HEADER_H + 2 * (ROW_GAP + ROW_H) - ROW_H;
        int textY = rowY - 2 - (int) (5 * progress);
        ctx.text(font, text, x + CARD_W - PAD - font.width(text), textY,
                EffectPaint.argb(gauges.get(1).rgb(), (int) (255 * (1f - progress))), true);
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

    // --- Layout editor preview ---

    /**
     * A representative plate: a fake name, a Fool sequence 5 identity, plausible
     * gauges and one reserve row. Reads nothing from the {@code Client*State}
     * classes, which are live behind the editor.
     */
    public static void renderPreview(GuiGraphicsExtractor ctx, int screenW, int screenH,
                                     HudConfig.HudSettings s, long timeMs) {
        Minecraft client = Minecraft.getInstance();
        int[] pos = anchor(screenW, screenH, s);
        int fool = AbilityInfo.pathwayRgb("fool");

        List<Gauge> gauges = List.of(
                new Gauge(PlateSymbols.BRAIN, 0.62f, 0.88f, SANITY_RGB[1], PlateSymbols.NO_WASH,
                        Component.translatable("hud.coi.plate_sanity_value", 62)),
                new Gauge(PlateSymbols.MASK, 0.63f, 1f, fool, PlateSymbols.NO_WASH,
                        Component.translatable("hud.coi.plate_percent", "63"),
                        Component.translatable("hud.coi.plate_cooldown", "04:12")));
        List<Reserve> reserves = List.of(
                new Reserve("Rage Meter", 0.62f, 0xFF5555,
                        Component.translatable("hud.coi.plate_percent", "62")));

        HudScale.push(ctx, pos[0], pos[1], s.characterPlateScale);
        CoiStyle.drawCard(ctx, pos[0], pos[1], CARD_W, previewHeight());
        drawPreviewHeader(ctx, client.font, pos[0], pos[1], client.player, fool);

        int rowY = pos[1] + PAD + HEADER_H;
        for (Gauge gauge : gauges) {
            rowY += ROW_GAP;
            drawGauge(ctx, client.font, pos[0], rowY, gauge);
            rowY += ROW_H;
        }
        rowY += DIVIDER_GAP;
        ctx.fill(pos[0] + PAD, rowY, pos[0] + CARD_W - PAD, rowY + 1, DIVIDER);
        rowY += 1 + DIVIDER_GAP;
        drawReserve(ctx, client.font, pos[0], rowY, reserves.getFirst());
        HudScale.pop(ctx);
    }

    /**
     * The header with an invented identity. The head is still the player's own
     * — it is a texture, not session state, and a blank square would make the
     * preview harder to judge than the thing it stands for.
     */
    private static void drawPreviewHeader(GuiGraphicsExtractor ctx, Font font, int x, int y,
                                          AbstractClientPlayer player, int rgb) {
        drawHead(ctx, player, x + PAD, y + PAD + (HEADER_H - HEAD) / 2);
        int textX = x + PAD + HEAD + HEAD_GAP;
        ctx.text(font, Component.translatable("screen.coi.plate_sample_name"), textX, y + PAD,
                CoiStyle.TEXT_BODY, true);
        drawPathwayLine(ctx, font, textX, y + PAD + 11, CARD_W - PAD - (textX - x), "FOOL", 5, rgb);
    }
}
