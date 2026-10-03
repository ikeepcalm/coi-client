package dev.ua.ikeepcalm.coi.hud.overlay;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.ActingPercent;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.ActingState;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.BeyonderState;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.ResourceState;
import dev.ua.ikeepcalm.coi.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.hud.HudGate;
import dev.ua.ikeepcalm.coi.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.hud.HudScale;
import dev.ua.ikeepcalm.coi.hud.render.PlateCard;
import dev.ua.ikeepcalm.coi.hud.render.PlateSymbols;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One card for madness, acting and the resource meters; this class reads the state and decides
 * which rows exist, {@link PlateCard} paints them.
 * <p>
 * Rows with no data are <em>omitted</em>, not blanked, so the card's height follows the server.
 * <b>Every colour the plate draws must go through {@link HudOpacity#apply}</b> — one that skips
 * it stays solid while the rest fades, which reads as a bug rather than a setting.
 */
public class CharacterPlateOverlay {

    private static final Identifier PLATE_LAYER = Identifier.fromNamespaceAndPath("coi-client", "character_plate");

    public static final int CARD_W = PlateCard.CARD_W;
    public static final int DEFAULT_TOP_Y = 20;

    /**
     * Indexed by {@link MadnessOverlay#stageOf}, so the plate cannot disagree with the vignette.
     */
    private static final int[] SANITY_RGB = {0x00FFCC, 0xFFAA00, 0xDD2222, 0xFF0055, 0x993399};

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
        HudOpacity.push(settings.characterPlateOpacity);
        PlateCard.draw(ctx, client.font, pos[0], pos[1], client.player,
                client.player.getName().getString(),
                BeyonderState.getPathway(), BeyonderState.getSequence(), gauges, reserves);
        drawGrantPopup(ctx, client.font, pos[0], pos[1], gauges);
        HudOpacity.pop();
        HudScale.pop(ctx);
    }

    /** A card holding only the player's own head is noise, so a vanilla server gets none. */
    private static boolean hasAnything() {
        return BeyonderState.hasIdentity()
                || BeyonderState.getMadness() > 0
                || BeyonderState.getPermanentMadness() > 0
                || (ActingState.hasData() && !ActingState.isOuter())
                || ResourceState.hasData();
    }

    /** The plate's drawn bounds <em>are</em> its fill origin: no label hangs above it. */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.characterPlateAnchor).resolve(
                screenW, screenH, HudScale.size(CARD_W, s.characterPlateScale),
                s.characterPlateXOffset, s.characterPlateYOffset, s.characterPlateYOffset);
    }

    /** The <em>preview's</em> height, not the live card's: that is what the editor draws. */
    public static int previewHeight() {
        return PlateCard.height(2, 1);
    }

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
     * Madness the other way up: a full brain is a clear head. Permanent madness never comes
     * back, so it is a <em>ceiling</em> on the gauge rather than a second fill.
     */
    private static PlateCard.Gauge sanityGauge(double madness, double permanentMadness) {
        double sanity = Math.clamp(100.0 - madness, 0, 100);
        float cap = (float) (Math.clamp(100.0 - permanentMadness, 0, 100) / 100.0);
        int stage = Mth.clamp(MadnessOverlay.stageOf(madness), 0, SANITY_RGB.length - 1);
        int rgb = SANITY_RGB[stage];
        // Below stage 2 the brain keeps its own colour, so the red means something when it comes
        int wash = stage < 2 ? PlateSymbols.NO_WASH : rgb;
        return new PlateCard.Gauge(PlateSymbols.BRAIN, (float) (sanity / 100.0), cap, rgb, wash,
                Component.translatable("hud.coi.plate_sanity_value", Math.round(sanity)));
    }

    private static PlateCard.Gauge actingGauge(double percent, String pathway, String cooldownClock) {
        double clamped = Math.clamp(percent, 0, 100);
        // The mask is already vivid; the pathway colour rides the bar instead of washing the art
        return new PlateCard.Gauge(PlateSymbols.MASK, (float) (clamped / 100.0), 1f,
                Pathways.pathwayRgb(pathway), PlateSymbols.NO_WASH,
                Component.translatable("hud.coi.plate_percent", String.format(Locale.ROOT, "%.0f", ActingPercent.floor(clamped, 0))),
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

    /** Matching decimals on both sides, so {@code 2.5 / 5.0} never reads as {@code 2.5 / 5}. */
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

    private static void drawGrantPopup(GuiGraphicsExtractor ctx, Font font,
                                       int x, int y, List<PlateCard.Gauge> gauges) {
        int granted = ActingState.activeGrant();
        if (granted <= 0 || gauges.size() < ACTING_ROW) return;
        PlateCard.drawGrantPopup(ctx, font, x, y, ACTING_ROW,
                gauges.get(ACTING_ROW - 1).rgb(), granted, ActingState.grantProgress());
    }

    /** Sample data only: the real state classes are live behind the editor. */
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

        // Faded too: the editor is where the player judges the opacity setting
        HudScale.push(ctx, pos[0], pos[1], s.characterPlateScale);
        HudOpacity.push(s.characterPlateOpacity);
        PlateCard.drawChrome(ctx, pos[0], pos[1], CARD_W, previewHeight());
        drawPreviewHeader(ctx, client.font, pos[0], pos[1], client.player, fool);

        int rowY = PlateCard.drawGauges(ctx, client.font, pos[0], pos[1] + PlateCard.PAD + PlateCard.HEADER_H, gauges);
        rowY = PlateCard.drawDivider(ctx, pos[0], rowY);
        PlateCard.drawReserve(ctx, client.font, pos[0], rowY, reserve);
        HudOpacity.pop();
        HudScale.pop(ctx);
    }

    /** The head stays the player's own: it is a texture, not session state. */
    private static void drawPreviewHeader(GuiGraphicsExtractor ctx, Font font,
                                          int x, int y, AbstractClientPlayer player, int rgb) {
        PlateCard.drawHeader(ctx, font, x, y, player,
                Component.translatable("screen.coi.plate_sample_name").getString(), "fool", 5);
    }
}