package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AbilityHudOverlay {

    private static final Identifier ABILITY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "abilities");
    private static AbilitySlotWidget[] abilitySlots;

    public static void initialize() {
        int maxAbilities = CircleOfImaginationClient.MAX_ABILITIES;
        abilitySlots = new AbilitySlotWidget[maxAbilities];

        for (int i = 0; i < maxAbilities; i++) {
            abilitySlots[i] = new AbilitySlotWidget(i);
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ABILITY_LAYER, AbilityHudOverlay::renderAbilities);
    }

    private static void renderAbilities(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.gui.hud.isHidden() || !settings.enabled) {
            return;
        }

        float tickDelta = 1.0f;

        int screenHeight = client.getWindow().getGuiScaledHeight();
        int startY = (int) ((screenHeight - settings.hudYOffset) / settings.hudScale);
        int hudX = (int) (settings.hudX / settings.hudScale);

        // Only render active slots that have abilities bound to them
        int activeSlots = CircleOfImaginationClient.getActiveAbilitySlots();
        List<AbilitySlotWidget> visible = new ArrayList<>();
        List<Integer> visibleIndices = new ArrayList<>();
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.getSlotIndex() < activeSlots && !abilitySlot.isEmpty()) {
                visible.add(abilitySlot);
                visibleIndices.add(abilitySlot.getSlotIndex());
            }
        }

        HudGaslight.update(visibleIndices);

        int renderedCount = 0;
        for (AbilitySlotWidget abilitySlot : visible) {
            // At high madness two slots may briefly trade places
            AbilitySlotWidget toDraw = abilitySlot;
            int swapped = HudGaslight.swappedWith(abilitySlot.getSlotIndex());
            if (swapped >= 0 && swapped < abilitySlots.length && !abilitySlots[swapped].isEmpty()) {
                toDraw = abilitySlots[swapped];
            }

            int x = hudX + (renderedCount * settings.slotSpacing);
            toDraw.render(context, x, startY, settings.slotSize, tickDelta);
            renderedCount++;
        }
    }

    public static void setCooldown(String abilityId, int cooldownTicks) {
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.hasAbility(abilityId)) {
                if (!abilitySlot.isOnCooldown()) {
                    abilitySlot.setCooldown(cooldownTicks);
                }
                break;
            }
        }
    }

    public static void onAbilityCast(String abilityId) {
        if (abilitySlots == null) return;
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (abilitySlot.hasAbility(abilityId)) {
                abilitySlot.triggerCastAnimation();
                break;
            }
        }
    }

    public static void updateAbilitySlot(int slot, String abilityId) {
        if (abilitySlots != null && slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot].setAbility(abilityId);
        }
    }
}