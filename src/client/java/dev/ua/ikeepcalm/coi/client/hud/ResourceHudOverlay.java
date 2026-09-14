package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientResourceState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.layout.HudLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Ability resource meters pushed by the server on {@code coi-client:resource}.
 * <p>
 * A stack of thin bars in the colour the server picked, one per live resource,
 * growing away from the anchor (down from the TOP_* corners, up from the
 * BOTTOM_* ones) so it never overruns the edge it is pinned to. Bars vanish on
 * their own once their TTL runs out, so a passive that stops refreshing takes
 * its bar with it.
 */
public class ResourceHudOverlay {

    private static final Identifier RESOURCE_LAYER = Identifier.fromNamespaceAndPath("coi-client", "resources");

    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 4;
    /**
     * Bar plus the label sitting 10px above the next one down.
     */
    public static final int STRIDE = 18;

    private static final int BORDER_COLOR = 0xCC000000;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, RESOURCE_LAYER, ResourceHudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        // The character plate lists the same meters as its reserve rows, so the
        // stack stands down for it
        if (client.player == null || client.gui.hud.isHidden() || HudLayout.editing() || !settings.enabled
                || settings.showCharacterPlate
                || !settings.showResourceBars || !ClientResourceState.hasData()) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        HudAnchor anchor = HudAnchor.parse(settings.resourceAnchor);
        int[] pos = anchor(w, h, settings);

        // TOP_* stacks downwards, BOTTOM_* upwards — either way, away from the edge
        int step = anchor.isTop() ? STRIDE : -STRIDE;
        List<ClientResourceState.Entry> bars = ClientResourceState.visible();
        int count = Math.min(bars.size(), Math.max(1, settings.resourceMaxBars));

        HudScale.push(ctx, pos[0], pos[1], settings.resourceScale);
        for (int i = 0; i < count; i++) {
            drawBar(ctx, client, bars.get(i), pos[0], pos[1] + step * i);
        }
        HudScale.pop(ctx);
    }

    /**
     * Top-left corner of the first bar in the stack.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.resourceAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.resourceScale),
                s.resourceXOffset, s.resourceYOffset, s.resourceYOffset);
    }

    /**
     * One bar from explicit values, for previews that must not invent entries
     * in {@code ClientResourceState}.
     */
    public static void drawBarAt(GuiGraphicsExtractor ctx, int barX, int barY, String label,
                                 double current, double max, int rgb, boolean percent) {
        Minecraft client = Minecraft.getInstance();
        int fillWidth = CoiBar.lerpWidth(current, max, BAR_WIDTH);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, 0xFF000000 | rgb, darken(rgb));
        CoiBar.label(ctx, client.font, labelText(label, current, max, percent), barX, barY, BAR_WIDTH, LABEL_COLOR);
    }

    private static void drawBar(GuiGraphicsExtractor ctx, Minecraft client, ClientResourceState.Entry entry, int barX, int barY) {
        int rgb = entry.rgb();
        int fillWidth = CoiBar.lerpWidth(entry.current(), entry.max(), BAR_WIDTH);

        CoiBar.frame(ctx, barX, barY, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);
        CoiBar.fill(ctx, barX, barY, BAR_HEIGHT, fillWidth, 0xFF000000 | rgb, darken(rgb));
        CoiBar.label(ctx, client.font, labelText(entry), barX, barY, BAR_WIDTH, LABEL_COLOR);
    }

    private static String labelText(ClientResourceState.Entry entry) {
        return labelText(entry.label(), entry.current(), entry.max(), entry.percent());
    }

    private static String labelText(String label, double current, double max, boolean asPercent) {
        if (asPercent) {
            double percent = max > 0 ? current * 100.0 / max : 0;
            return I18n.get("hud.coi.resource_percent", label, String.format(Locale.ROOT, "%.1f", percent));
        }
        boolean whole = current == Math.rint(current) && max == Math.rint(max);
        return I18n.get("hud.coi.resource_value", label, number(current, whole), number(max, whole));
    }

    /**
     * Two whole numbers read as integers; a fractional pair keeps one decimal
     * on both sides, so {@code 2.5 / 5.0} never reads as {@code 2.5 / 5}.
     */
    private static String number(double value, boolean whole) {
        return whole ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
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
}
