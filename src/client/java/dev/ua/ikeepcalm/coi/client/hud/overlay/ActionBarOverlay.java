package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.state.ActionBarState;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * COI's own action bar. The plugin funnels 743 publish sites through eight
 * prioritised channels and used to squeeze the top two into the single vanilla
 * action-bar line; capable clients get the whole sorted list instead and stack
 * it here, one line per entry with a channel-coloured tick.
 * <p>
 * Only COI's own entries move — vanilla action bars from other plugins are
 * untouched.
 */
public class ActionBarOverlay {

    private static final Identifier ACTION_BAR_LAYER = Identifier.fromNamespaceAndPath("coi-client", "actionbar");

    public static final int LINE_SPACING = 10;
    private static final int TICK_WIDTH = 2;
    private static final int TICK_GAP = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_TICK = 0xFF999999;

    private ActionBarOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ACTION_BAR_LAYER, ActionBarOverlay::render);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showActionBar) {
            return;
        }

        // 0 = follow whatever the server thinks fits
        int serverMax = ActionBarState.getMaxVisible();
        int limit = settings.actionBarLines > 0 ? Math.min(serverMax, settings.actionBarLines) : serverMax;

        List<ActionBarState.Entry> entries = ActionBarState.visibleEntries(System.currentTimeMillis(), limit);
        if (entries.isEmpty()) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int baseY = h - settings.actionBarYOffset;
        int centerX = w / 2 + settings.actionBarXOffset;

        // Stacked upward so the highest-priority entry sits closest to the
        // hotbar; the stack scales about its centre/base point
        HudScale.push(ctx, centerX, baseY, settings.actionBarScale);
        for (int i = 0; i < entries.size(); i++) {
            ActionBarState.Entry entry = entries.get(i);
            drawLineAt(ctx, entry.text(), entry.channel(), centerX, baseY - i * LINE_SPACING);
        }
        HudScale.pop(ctx);
    }

    /**
     * One line centred on {@code centerX}, with its channel tick. Takes plain
     * values so previews don't have to fabricate entries.
     */
    public static void drawLineAt(GuiGraphicsExtractor ctx, Component text, String channel, int centerX, int y) {
        Minecraft client = Minecraft.getInstance();
        int x = centerX - client.font.width(text) / 2;
        ctx.fill(x - TICK_GAP - TICK_WIDTH, y - 1, x - TICK_GAP, y + client.font.lineHeight - 1, tickColor(channel));
        ctx.text(client.font, text, x, y, TEXT_COLOR, true);
    }

    /**
     * One colour per plugin channel, so a glance is enough to tell a cooldown
     * line from a madness warning.
     */
    private static int tickColor(String channel) {
        if (channel == null) return DEFAULT_TICK;
        return switch (channel.toUpperCase()) {
            case "RESERVE" -> 0xFF7FB2FF;
            case "SENSORY" -> 0xFFB57FFF;
            case "COOLDOWN" -> 0xFFFFD870;
            case "STATUS" -> 0xFF7FFF9F;
            case "CATEGORY" -> 0xFFFFA64D;
            case "NOTIFICATION" -> 0xFFFF5555;
            case "MINIGAME" -> 0xFF55FFFF;
            case "SYSTEM" -> 0xFFCCCCCC;
            default -> DEFAULT_TICK;
        };
    }
}
