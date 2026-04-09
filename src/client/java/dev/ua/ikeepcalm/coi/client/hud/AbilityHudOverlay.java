package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class AbilityHudOverlay {

    private static final Identifier ABILITY_LAYER = Identifier.fromNamespaceAndPath("coi-client", "abilities");
    private static AbilitySlotWidget[] abilitySlots;

    public static void initialize() {
        HudConfig.load();

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

        if (client.player == null || client.options.hideGui || !settings.enabled) {
            return;
        }

        float tickDelta = 1.0f;

        int screenHeight = client.getWindow().getGuiScaledHeight();
        int startY = (int) ((screenHeight - settings.hudYOffset) / settings.hudScale);
        int hudX = (int) (settings.hudX / settings.hudScale);

        // Only render slots that have abilities bound to them
        int renderedCount = 0;
        for (AbilitySlotWidget abilitySlot : abilitySlots) {
            if (!abilitySlot.isEmpty()) {
                int x = hudX + (renderedCount * settings.slotSpacing);
                abilitySlot.render(context, x, startY, settings.slotSize, tickDelta);
                renderedCount++;
            }
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

    public static void updateAbilitySlot(int slot, String abilityId) {
        if (abilitySlots != null && slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot].setAbility(abilityId);
        }
    }
}