package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.PlateCard;
import dev.ua.ikeepcalm.coi.client.hud.render.PlateSymbols;
import dev.ua.ikeepcalm.coi.client.state.ActingState;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;
import dev.ua.ikeepcalm.coi.client.state.ResourceState;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

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
 * <p>
 * This class reads the state and decides which rows exist; {@link PlateCard}
 * paints them.
 */
public final class CharacterPlateOverlay {

    private static final Identifier PLATE_LAYER = Identifier.fromNamespaceAndPath("coi-client", "character_plate");

    /**
     * The card's fixed width. Every interior column is measured off it, so the
     * plate has one number to tune.
     */
    public static final int CARD_W = PlateCard.CARD_W;
    /**
     * Where a TOP-anchored plate sits out of the box — the same line the
     * madness bar it replaces used.
     */
    public static final int DEFAULT_TOP_Y = 20;

    /**
     * Sanity's palette, indexed by {@link MadnessOverlay#stageOf}. Sharing
     * that function is the point: the plate can never disagree with the stage
     * vignette about how far gone the player is.
     */
    private static final int[] SANITY_RGB = {0x00FFCC, 0xFFAA00, 0xDD2222, 0xFF0055, 0x993399};

    /**
     * The acting gauge is the second row, and the one the {@code +N} popup
     * belongs to.
     */
    private static final int ACTING_ROW = 2;

    private CharacterPlateOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, PLATE_LAYER, CharacterPlateOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showCharacterPlate || !hasAnything()) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        List<PlateCard.Gauge> gauges = liveGauges();
        List<PlateCard.Reserve> reserves = liveReserves(settings);
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.characterPlateScale);
        PlateCard.draw(ctx, client.font, pos[0], pos[1], client.player,
                client.player.getName().getString(),
                BeyonderState.getPathway(), BeyonderState.getSequence(), gauges, reserves);
        drawGrantPopup(ctx, client.font, pos[0], pos[1], gauges);
        HudScale.pop(ctx);
    }

    /**
     * Whether the plate has anything worth a card. A vanilla server answers
     * nothing at all, and a card holding only the player's own head would be
     * noise rather than a HUD.
     */
    private static boolean hasAnything() {
        return BeyonderState.hasIdentity()
                || BeyonderState.getMadness() > 0
                || BeyonderState.getPermanentMadness() > 0
                || (ActingState.hasData() && !ActingState.isOuter())
                || ResourceState.hasData();
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
     * The height the layout editor works against. It is the preview's, not the
     * live card's: the editor draws the preview, so the grab box has to match
     * that and not whatever the server happens to be sending behind the screen.
     */
    public static int previewHeight() {
        return PlateCard.height(2, 1);
    }

    // --- Live data ---

    private static List<PlateCard.Gauge> liveGauges() {
        List<PlateCard.Gauge> gauges = new ArrayList<>(2);
        gauges.add(sanityGauge(BeyonderState.getMadness(), BeyonderState.getPermanentMadness()));
        if (ActingState.hasData() && !ActingState.isOuter()) {
            gauges.add(actingGauge(ActingState.getPercent(), ActingState.getPathway(),
                    ActingState.cooldownRemainingNow() > 0 ? ActingState.cooldownClock() : null));
        }
        return gauges;
    }

    /**
     * Madness read the other way up: a full brain is a clear head, a draining
     * one is a player losing it. Permanent madness is headroom that never comes
     * back, so it is a ceiling on the gauge rather than a second fill.
     */
    private static PlateCard.Gauge sanityGauge(double madness, double permanentMadness) {
        double sanity = Math.clamp(100.0 - madness, 0, 100);
        float cap = (float) (Math.clamp(100.0 - permanentMadness, 0, 100) / 100.0);
        int stage = Mth.clamp(MadnessOverlay.stageOf(madness), 0, SANITY_RGB.length - 1);
        int rgb = SANITY_RGB[stage];
        // Below stage 2 the brain keeps its own colour and only the bar carries
        // the state; from there the wash bleeds the artwork itself towards the
        // stage colour, so a player losing it can see it without reading a number
        int wash = stage < 2 ? PlateSymbols.NO_WASH : rgb;
        return new PlateCard.Gauge(PlateSymbols.BRAIN, (float) (sanity / 100.0), cap, rgb, wash,
                Component.translatable("hud.coi.plate_sanity_value", Math.round(sanity)));
    }

    private static PlateCard.Gauge actingGauge(double percent, String pathway, String cooldownClock) {
        double clamped = Math.clamp(percent, 0, 100);
        // The mask is already vivid; the pathway colour rides the bar instead, or
        // the wash would just muddy the artwork
        return new PlateCard.Gauge(PlateSymbols.MASK, (float) (clamped / 100.0), 1f,
                Pathways.pathwayRgb(pathway), PlateSymbols.NO_WASH,
                Component.translatable("hud.coi.plate_percent", String.format(Locale.ROOT, "%.0f", clamped)),
                cooldownClock == null ? null : Component.translatable("hud.coi.plate_cooldown", cooldownClock));
    }

    private static List<PlateCard.Reserve> liveReserves(HudConfig.HudSettings s) {
        List<ResourceState.Entry> entries = ResourceState.visible();
        int count = Math.min(entries.size(), Math.max(0, s.resourceMaxBars));
        List<PlateCard.Reserve> reserves = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceState.Entry entry = entries.get(i);
            double max = entry.max() > 0 ? entry.max() : 1;
            reserves.add(new PlateCard.Reserve(entry.label(), (float) (entry.current() / max), entry.rgb(),
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

    /**
     * The acting row's {@code +N}, only while the server says a grant just
     * landed and only when the acting row is actually on the card.
     */
    private static void drawGrantPopup(GuiGraphicsExtractor ctx, Font font,
                                       int x, int y, List<PlateCard.Gauge> gauges) {
        int granted = ActingState.activeGrant();
        if (granted <= 0 || gauges.size() < ACTING_ROW) return;
        PlateCard.drawGrantPopup(ctx, font, x, y, ACTING_ROW,
                gauges.get(ACTING_ROW - 1).rgb(), granted, ActingState.grantProgress());
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
        int fool = Pathways.pathwayRgb("fool");

        List<PlateCard.Gauge> gauges = List.of(
                new PlateCard.Gauge(PlateSymbols.BRAIN, 0.62f, 0.88f, SANITY_RGB[1], PlateSymbols.NO_WASH,
                        Component.translatable("hud.coi.plate_sanity_value", 62)),
                new PlateCard.Gauge(PlateSymbols.MASK, 0.63f, 1f, fool, PlateSymbols.NO_WASH,
                        Component.translatable("hud.coi.plate_percent", "63"),
                        Component.translatable("hud.coi.plate_cooldown", "04:12")));
        PlateCard.Reserve reserve = new PlateCard.Reserve("Rage Meter", 0.62f, 0xFF5555,
                Component.translatable("hud.coi.plate_percent", "62"));

        HudScale.push(ctx, pos[0], pos[1], s.characterPlateScale);
        CoiStyle.drawCard(ctx, pos[0], pos[1], CARD_W, previewHeight());
        drawPreviewHeader(ctx, client.font, pos[0], pos[1], client.player, fool);

        int rowY = PlateCard.drawGauges(ctx, client.font, pos[0], pos[1] + PlateCard.PAD + PlateCard.HEADER_H, gauges);
        rowY = PlateCard.drawDivider(ctx, pos[0], rowY);
        PlateCard.drawReserve(ctx, client.font, pos[0], rowY, reserve);
        HudScale.pop(ctx);
    }

    /**
     * The header with an invented identity. The head is still the player's own
     * — it is a texture, not session state, and a blank square would make the
     * preview harder to judge than the thing it stands for.
     */
    private static void drawPreviewHeader(GuiGraphicsExtractor ctx, Font font,
                                          int x, int y, AbstractClientPlayer player, int rgb) {
        PlateCard.drawHead(ctx, player, x + PlateCard.PAD,
                y + PlateCard.PAD + (PlateCard.HEADER_H - PlateCard.HEAD) / 2);
        int textX = x + PlateCard.PAD + PlateCard.HEAD + PlateCard.HEAD_GAP;
        ctx.text(font, Component.translatable("screen.coi.plate_sample_name"), textX, y + PlateCard.PAD,
                CoiStyle.TEXT_BODY, true);
        PlateCard.drawPathwayLine(ctx, font, textX, y + PlateCard.PAD + PlateCard.HEADER_LINE_2, "FOOL", 5, rgb);
    }
}
