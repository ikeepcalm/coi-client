package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.input.CoiKeyBindings;
import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/**
 * The strip that explains the empty hotbar slot.
 * <p>
 * The plugin's {@code Beyonder.setShortcutItem()} deletes the "Mystery Arts"
 * item for any client advertising {@code menu_action} — this client renders the
 * menus itself, so the item would only be a second, worse doorway. What the
 * player sees, though, is a slot that used to hold something and now does not,
 * with nothing anywhere saying why. This says why, once.
 * <p>
 * It attaches through {@code fabric-screen-api-v1} rather than a mixin: the
 * mod's existing mixins are all renderer-side and there is no screen mixin to
 * extend, and a hint that lives entirely in an event registration is one that
 * can never break the inventory when it goes wrong.
 * <p>
 * <b>Placement is measured, not guessed.</b> The strip goes above the inventory
 * panel, or below it when the top is the cramped edge, and draws nothing at all
 * rather than overlap the panel — see {@link #layout(Screen, Font)}.
 * <p>
 * Dismissal is state, not a preference, so it is persisted in
 * {@code coi_client_state.json} beside the tour flag: a player who has read this
 * once has read it, and resetting the HUD config must not bring it back.
 */
public class InventoryHint {

    /**
     * {@code AbstractContainerScreen}'s default panel, and its centring rule
     * ({@code leftPos = (width - imageWidth) / 2}, likewise {@code topPos}).
     * Both fields are protected and there is no accessor, but the survival
     * inventory takes the defaults and the centring is the base class's own
     * {@code init} — so recomputing it here agrees with the screen by
     * construction. The recipe book shifts {@code leftPos} only, which is why
     * nothing below reads the panel's horizontal position.
     */
    private static final int PANEL_H = 166;

    private static final int PAD = 5;
    private static final int ICON_GAP = 5;
    private static final int BTN_GAP = 7;
    private static final int LINE = 10;
    private static final int FONT_H = 9;
    private static final int BTN_H = 13;
    private static final int BTN_MIN_W = 38;

    /** Between the strip and the panel it is attached to. */
    private static final int GAP = 4;
    /** Between the strip and the edge of the screen. */
    private static final int MARGIN = 2;

    /** Below this the text column is too narrow to read; the strip stands down. */
    private static final int MIN_TEXT_W = 48;

    private InventoryHint() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;
            // Per-screen events are rebuilt on every init and resize, so
            // registering here re-registers rather than stacking up.
            ScreenEvents.afterExtract(screen).register(InventoryHint::render);
            ScreenMouseEvents.allowMouseClick(screen).register(InventoryHint::click);
        });
    }

    private static boolean shouldShow() {
        // A vanilla server never took the item away, so it never owes an
        // explanation for it being gone.
        return ServerCapabilities.has("menu_action")
                && !ClientStateStore.isInventoryHintDismissed();
    }

    // --- Geometry ---

    /**
     * One laid-out strip, in absolute screen pixels. The same method answers
     * both the draw and the click, so the dismiss box can never drift from the
     * one that was painted.
     */
    private record Strip(int x, int y, int w, int h,
                         int iconX, int iconY,
                         int textX, int textY,
                         int btnX, int btnY, int btnW,
                         List<FormattedCharSequence> head,
                         List<FormattedCharSequence> tail) {

        boolean inButton(double mx, double my) {
            return mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + BTN_H;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /**
     * Measures the strip, or answers null when there is nowhere it fits.
     * <p>
     * The order is: fit the width to the screen and wrap the text into it, then
     * ask whether the resulting height clears the panel — above first, below
     * second, nowhere third. Minecraft never scales a window below 320×240
     * scaled pixels, which puts the panel's top edge at {@code (240 - 166) / 2
     * = 37} at worst; a two-line strip is 29 tall and needs 35 with its gap and
     * margin, so the "nowhere" branch is a guard, not the common case.
     */
    private static Strip layout(Screen screen, Font font) {
        Component headText = Component.translatable("screen.coi.inv_hint_line1");
        Component tailText = keyLine();
        Component dismiss = Component.translatable("screen.coi.inv_hint_dismiss");

        int btnW = Math.max(BTN_MIN_W, font.width(dismiss) + 12);
        int fixed = PAD + CoiIcons.COG_SIZE + ICON_GAP + BTN_GAP + btnW + PAD;
        int wanted = Math.max(font.width(headText), font.width(tailText));

        int maxW = screen.width - MARGIN * 2;
        int cardW = Math.min(fixed + wanted, maxW);
        int textW = cardW - fixed;
        if (textW < MIN_TEXT_W) return null;

        List<FormattedCharSequence> head = new ArrayList<>(font.split(headText, textW));
        List<FormattedCharSequence> tail = new ArrayList<>(font.split(tailText, textW));
        if (head.isEmpty() && tail.isEmpty()) return null;

        int textH = (head.size() + tail.size()) * LINE - (LINE - FONT_H);
        int contentH = Math.max(textH, Math.max(CoiIcons.COG_SIZE, BTN_H));
        int cardH = contentH + PAD * 2;

        int panelTop = (screen.height - PANEL_H) / 2;
        int panelBottom = panelTop + PANEL_H;
        int cardY;
        if (panelTop - GAP - cardH >= MARGIN) {
            cardY = panelTop - GAP - cardH;
        } else if (panelBottom + GAP + cardH <= screen.height - MARGIN) {
            cardY = panelBottom + GAP;
        } else {
            // Overlapping the inventory would be worse than staying quiet.
            return null;
        }

        int cardX = (screen.width - cardW) / 2;
        int contentY = cardY + PAD;
        return new Strip(cardX, cardY, cardW, cardH,
                cardX + PAD, contentY + (contentH - CoiIcons.COG_SIZE) / 2,
                cardX + PAD + CoiIcons.COG_SIZE + ICON_GAP, contentY + (contentH - textH) / 2,
                cardX + cardW - PAD - btnW, contentY + (contentH - BTN_H) / 2, btnW,
                head, tail);
    }

    /**
     * The second line, naming whatever key actually opens the menu right now —
     * read off the mapping rather than spelled out, so a rebound key says the
     * new key and an unbound one says so instead of lying about M.
     */
    private static Component keyLine() {
        KeyMapping mapping = CoiKeyBindings.openMenu;
        if (mapping == null || mapping.isUnbound()) {
            return Component.translatable("screen.coi.inv_hint_line2_unbound");
        }
        Component key = KeyMappingHelper.getBoundKeyOf(mapping).getDisplayName();
        return Component.translatable("screen.coi.inv_hint_line2",
                key.copy().withStyle(ChatFormatting.GOLD));
    }

    // --- Draw ---

    private static void render(Screen screen, GuiGraphicsExtractor graphics,
                               int mouseX, int mouseY, float tickProgress) {
        if (!shouldShow()) return;
        Font font = Minecraft.getInstance().font;
        Strip strip = layout(screen, font);
        if (strip == null) return;

        CoiStyle.drawCard(graphics, strip.x(), strip.y(), strip.w(), strip.h());

        if (!CoiIcons.draw(graphics, CoiIcons.COG, strip.iconX(), strip.iconY(), CoiIcons.COG_SIZE)) {
            // No pack defines the cog: keep the column, mark it with the accent
            // so the line still starts on something.
            int cx = strip.iconX() + CoiIcons.COG_SIZE / 2;
            int cy = strip.iconY() + CoiIcons.COG_SIZE / 2;
            graphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, CoiStyle.ACCENT);
            graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, CoiStyle.CARD_BG);
        }

        int y = strip.textY();
        for (FormattedCharSequence line : strip.head()) {
            graphics.text(font, line, strip.textX(), y, CoiStyle.TEXT_BODY, false);
            y += LINE;
        }
        for (FormattedCharSequence line : strip.tail()) {
            graphics.text(font, line, strip.textX(), y, CoiStyle.TEXT_MUTED, false);
            y += LINE;
        }

        boolean hovered = strip.inButton(mouseX, mouseY);
        graphics.fill(strip.btnX(), strip.btnY(), strip.btnX() + strip.btnW(), strip.btnY() + BTN_H,
                hovered ? CoiStyle.ROW_HOVER : CoiStyle.TAB_BG_UNSELECTED);
        graphics.outline(strip.btnX(), strip.btnY(), strip.btnW(), BTN_H,
                hovered ? CoiStyle.ACCENT : CoiStyle.BORDER);
        graphics.centeredText(font, Component.translatable("screen.coi.inv_hint_dismiss"),
                strip.btnX() + strip.btnW() / 2, strip.btnY() + (BTN_H - FONT_H) / 2 + 1,
                hovered ? CoiStyle.ACCENT : CoiStyle.TEXT_BODY);
    }

    // --- Input ---

    /**
     * Swallows left-clicks that land on the strip, and dismisses on the one that lands on
     * "Got it".
     * <p>
     * <b>The whole strip is consumed, not just the button.</b> The strip sits outside the
     * inventory panel, and a click outside the panel is how {@code AbstractContainerScreen} drops
     * whatever is on the cursor — so a player mid-drag who clicked this card, which looks like
     * ordinary UI, would have thrown their item on the floor. Nothing underneath the strip is
     * worth reaching, so nothing under it gets the click.
     */
    private static boolean click(Screen screen, MouseButtonEvent event) {
        if (!shouldShow() || event.button() != 0) return true;
        Strip strip = layout(screen, Minecraft.getInstance().font);
        if (strip == null || !strip.contains(event.x(), event.y())) return true;

        if (strip.inButton(event.x(), event.y())) {
            ClientStateStore.setInventoryHintDismissed(true);
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return false;
    }
}
