package dev.ua.ikeepcalm.coi.client.screen.debug;

import dev.ua.ikeepcalm.coi.client.state.AppearanceState;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/**
 * Developer-only preview of the appearance traits, opened from
 * {@link EffectDebugScreen}. Each trait is toggled on the local player alone
 * (through {@code AppearanceState}'s debug set, never the wire) beside a live
 * inventory-style render, so a trait's geometry can be judged without a server
 * to send the packet.
 */
public class AppearanceDebugScreen extends Screen {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 330;
    private static final int BUTTON_W = 140;
    private static final int BUTTON_H = 22;

    private static final List<DebugTrait> TRAITS = List.of(
            new DebugTrait("female_traits", "Female Traits"),
            new DebugTrait("horns", "Abyss Horns"),
            new DebugTrait("mushroom", "Head Mushroom")
    );

    private final Screen parent;
    private final List<TraitButton> traitButtons = new ArrayList<>();
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

        for (int index = 0; index < TRAITS.size(); index++) {
            DebugTrait trait = TRAITS.get(index);
            int column = index % 2;
            int row = index / 2;
            int x = controlsX + column * (BUTTON_W + 12);
            int y = controlsY + row * 44;
            boolean enabled = AppearanceState.hasDebugTrait(playerUuid, trait.id());

            Button button = Button.builder(buttonLabel(trait, enabled), clicked -> {
                String uuid = localPlayerUuid();
                boolean nowEnabled = AppearanceState.toggleDebugTrait(uuid, trait.id());
                clicked.setMessage(buttonLabel(trait, nowEnabled));
            }).bounds(x, y, BUTTON_W, BUTTON_H).build();

            addRenderableWidget(button);
            traitButtons.add(new TraitButton(trait, x, y));
        }

        int traitRows = (TRAITS.size() + 1) / 2;
        int actionY = controlsY + traitRows * 44 + 12;
        addRenderableWidget(Button.builder(
                Component.literal("Clear All Traits").withStyle(ChatFormatting.RED),
                clicked -> {
                    AppearanceState.clearDebugTraits(localPlayerUuid());
                    rebuildWidgets();
                }
        ).bounds(controlsX, actionY, BUTTON_W, BUTTON_H).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                clicked -> onClose()
        ).bounds(controlsX + BUTTON_W + 12, actionY, BUTTON_W, BUTTON_H).build());
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
            graphics.text(
                    font,
                    Component.literal(entry.trait().id()).withStyle(ChatFormatting.DARK_GRAY),
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

    private static Component buttonLabel(DebugTrait trait, boolean enabled) {
        return Component.literal((enabled ? "ON  " : "OFF  ") + trait.displayName())
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private record DebugTrait(String id, String displayName) {
    }

    private record TraitButton(DebugTrait trait, int x, int y) {
    }
}
