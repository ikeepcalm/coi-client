package dev.ua.ikeepcalm.coi.client.screen.debug;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudOpacity;
import dev.ua.ikeepcalm.coi.client.hud.overlay.MadnessOverlay;
import dev.ua.ikeepcalm.coi.client.hud.overlay.SpiritualityOverlay;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthBarPaint;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle;
import dev.ua.ikeepcalm.coi.client.hud.render.PlateCard;
import dev.ua.ikeepcalm.coi.client.hud.render.PlateSymbols;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Sample values only; opened by the opt-in development capture runner.
 */
public class HudPreviewScreen extends Screen {

    private final boolean bars;

    HudPreviewScreen(boolean bars) {
        super(Component.literal("HUD instruments"));
        this.bars = bars;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x50101A20);
        int left = Math.max(12, (width - 440) / 2);
        int top = 16;
        var client = Minecraft.getInstance();
        var gauges = List.of(
                new PlateCard.Gauge(PlateSymbols.BRAIN, .62f, .88f, 0xE5AD57, PlateSymbols.NO_WASH, Component.literal("62%")),
                new PlateCard.Gauge(PlateSymbols.MASK, .63f, 1, 0xB893D5, PlateSymbols.NO_WASH,
                        Component.literal("63%"), Component.literal("04:12")));
        var reserves = List.of(new PlateCard.Reserve("Rage", .62f, 0xC7655D, Component.literal("62%")));
        if (!bars) {
            PlateCard.draw(g, font, left, top, client.player, "ikeepcalm", "fool", 5, gauges, reserves);
            HudOpacity.push(.45f);
            PlateCard.draw(g, font, left, top + PlateCard.height(2, 1) + 10, client.player,
                    "A very long character name", "visionary", 0, gauges, List.of());
            HudOpacity.pop();
        }
        int right = left + 220;
        if (!bars) {
            for (int i = 0; i < 5; i++)
                MadnessOverlay.drawBarAt(g, right, top + 10 + i * 31, Math.max(12, i * 25), 12, 0);
        } else {
            int y = top + 20;
            for (HealthStyle style : HealthStyle.values()) {
                g.text(font, style.name(), left, y - 11, 0xFFE4D9BF, true);
                HealthBarPaint.draw(g, style, left, y, 96, 9, 1234, 1750, 0, 0, 0);
                HealthBarPaint.draw(g, style, left + 116, y, 96, 9, 260, 1750, 0, 0, 0);
                HealthBarPaint.draw(g, style, left + 232, y, 96, 9, 1750, 1750, 450, 0, 0);
                y += 27;
            }
        }
        var settings = new HudConfig.HudSettings();
        settings.spiritualityAnchor = "TOP_LEFT";
        settings.spiritualityXOffset = right;
        settings.spiritualityYOffset = top + 172;
        settings.spiritualityScale = 1f;
        settings.epilepsyMode = true;
        SpiritualityOverlay.renderPreview(g, width, height, settings, 0, .72f);
        settings.spiritualityXOffset = right;
        settings.spiritualityYOffset = top + 218;
        SpiritualityOverlay.renderPreview(g, width, height, settings, 0, .18f);
    }
}
