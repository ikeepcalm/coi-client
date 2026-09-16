package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraits;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Local preview for the appearance traits: toggles any of the registered traits on the
 * local player without a server. Traits are split across pages (the registry holds 38),
 * cycled with a single page button; the preview entity updates live.
 */
public final class AppearanceDebugScreen extends Screen {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 330;
    private static final int BUTTON_W = 140;
    private static final int BUTTON_H = 22;
    private static final int ROWS_PER_PAGE = 5;
    private static final int COLUMNS = 2;
    private static final int PER_PAGE = ROWS_PER_PAGE * COLUMNS;

    private final Screen parent;
    private final List<TraitButton> traitButtons = new ArrayList<>();
    private int page;
    private int panelX;
    private int panelY;

    public AppearanceDebugScreen(Screen parent) {
        super(Component.literal("Appearance Traits — Debug"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        traitButtons.clear();
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(16, (height - PANEL_H) / 2);

        String playerUuid = localPlayerUuid();
        int controlsX = panelX + 246;
        int controlsY = panelY + 54;

        int pageCount = pageCount();
        page = Math.floorMod(page, pageCount);
        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, AppearanceTraits.TRAITS.size());

        for (int index = start; index < end; index++) {
            AppearanceTraits.TraitInfo trait = AppearanceTraits.TRAITS.get(index);
            int localIndex = index - start;
            int column = localIndex % COLUMNS;
            int row = localIndex / COLUMNS;
            int x = controlsX + column * (BUTTON_W + 12);
            int y = controlsY + row * 44;
            boolean enabled = ClientAppearanceState.hasDebugTrait(playerUuid, trait.id());

            Button button = Button.builder(buttonLabel(trait, enabled), clicked -> {
                String uuid = localPlayerUuid();
                boolean nowEnabled = ClientAppearanceState.toggleDebugTrait(uuid, trait.id());
                clicked.setMessage(buttonLabel(trait, nowEnabled));
            }).bounds(x, y, BUTTON_W, BUTTON_H).build();

            addRenderableWidget(button);
            traitButtons.add(new TraitButton(trait, x, y));
        }

        int actionY = controlsY + ROWS_PER_PAGE * 44 + 12;
        addRenderableWidget(Button.builder(
                        Component.literal("Page " + (page + 1) + "/" + pageCount + " ▶").withStyle(ChatFormatting.GOLD),
                        clicked -> {
                            page = (page + 1) % pageCount();
                            rebuildWidgets();
                        }
                ).bounds(controlsX, actionY, BUTTON_W, BUTTON_H).build());

        addRenderableWidget(Button.builder(
                Component.literal("Clear All Traits").withStyle(ChatFormatting.RED),
                clicked -> {
                    ClientAppearanceState.clearDebugTraits(localPlayerUuid());
                    rebuildWidgets();
                }
        ).bounds(controlsX + BUTTON_W + 12, actionY, BUTTON_W, BUTTON_H).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                clicked -> onClose()
        ).bounds(controlsX, actionY + 32, BUTTON_W * 2 + 12, BUTTON_H).build());
    }

    private static int pageCount() {
        return (AppearanceTraits.TRAITS.size() + PER_PAGE - 1) / PER_PAGE;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xE6101014);
        graphics.fill(panelX + 12, panelY + 42, panelX + 226, panelY + PANEL_H - 18, 0xAA050507);

        if (minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    panelX + 20,
                    panelY + 48,
                    panelX + 218,
                    panelY + PANEL_H - 26,
                    72,
                    0.0625f,
                    mouseX,
                    mouseY,
                    minecraft.player
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(
                font,
                Component.literal("Appearance Traits — Local Preview")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                panelX + PANEL_W / 2,
                panelY + 16,
                0xFFFFFFFF
        );
        graphics.text(
                font,
                Component.literal("Toggle traits independently; drag your mouse around the preview.")
                        .withStyle(ChatFormatting.GRAY),
                panelX + 246,
                panelY + 36,
                0xFFFFFFFF
        );

        for (TraitButton entry : traitButtons) {
            String family = entry.trait().family() != null ? "  ·" + entry.trait().family().id() : "";
            graphics.text(
                    font,
                    Component.literal(entry.trait().id() + family).withStyle(ChatFormatting.DARK_GRAY),
                    entry.x() + 3,
                    entry.y() + BUTTON_H + 2,
                    0xFF777777
            );
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String localPlayerUuid() {
        return minecraft.player != null
                ? minecraft.player.getUUID().toString()
                : null;
    }

    private static Component buttonLabel(AppearanceTraits.TraitInfo trait, boolean enabled) {
        return Component.literal((enabled ? "ON  " : "OFF  ") + trait.displayName())
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private record TraitButton(AppearanceTraits.TraitInfo trait, int x, int y) {
    }
}
