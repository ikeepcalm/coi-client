package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.layout.LayoutGeometry;
import dev.ua.ikeepcalm.coi.client.hud.overlay.ActionBarOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * COI's own action-bar lines, centred above the hotbar. Unlike the bars this
 * one has no anchor at all: x is a signed offset from the screen's centre line
 * and y a distance up from the bottom edge, so it tracks the hotbar the way
 * the vanilla action bar does.
 */
public class ActionBarElement extends AbstractElement {

    private static final int SAMPLE_W = 200;
    /**
     * Two 10px lines stacked upward from the base line, plus the descender
     * room the lower one needs.
     */
    private static final int SAMPLE_H = 20;
    private static final int ABOVE_BASE = 11;

    /**
     * Where the base line sits out of the box, clear of the hotbar.
     */
    private static final int DEFAULT_Y_OFFSET = 72;

    public ActionBarElement() {
        super(ElementIds.ACTION_BAR);
    }

    @Override
    public boolean visible(HudConfig.HudSettings s) {
        return s.showActionBar;
    }

    @Override
    public int[] bounds(int w, int h, HudConfig.HudSettings s) {
        float scale = s.actionBarScale;
        int sampleW = HudScale.size(SAMPLE_W, scale);
        return new int[]{w / 2 + s.actionBarXOffset - sampleW / 2,
                h - s.actionBarYOffset - HudScale.size(ABOVE_BASE, scale),
                sampleW, HudScale.size(SAMPLE_H, scale)};
    }

    @Override
    public void moveTo(int newX, int newY, int w, int h, HudConfig.HudSettings s) {
        float scale = s.actionBarScale;
        int xo = newX + HudScale.size(SAMPLE_W, scale) / 2 - w / 2;
        s.actionBarXOffset = Math.abs(xo) <= LayoutGeometry.CENTER_SNAP ? 0 : xo;
        s.actionBarYOffset = Mth.clamp(h - newY - HudScale.size(ABOVE_BASE, scale), 0, h);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s, long timeMs) {
        int centerX = w / 2 + s.actionBarXOffset;
        int baseY = h - s.actionBarYOffset;
        HudScale.push(ctx, centerX, baseY, s.actionBarScale);
        ActionBarOverlay.drawLineAt(ctx, Component.literal("Cooldown · Sword 1.8s"), "COOLDOWN", centerX, baseY);
        ActionBarOverlay.drawLineAt(ctx, Component.literal("Status · Active"), "STATUS",
                centerX, baseY - ActionBarOverlay.LINE_SPACING);
        HudScale.pop(ctx);
    }

    @Override
    public void resetPosition(HudConfig.HudSettings s) {
        s.actionBarXOffset = 0;
        s.actionBarYOffset = DEFAULT_Y_OFFSET;
    }
}
