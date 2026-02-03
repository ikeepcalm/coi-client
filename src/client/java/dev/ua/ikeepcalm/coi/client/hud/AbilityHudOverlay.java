package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public class AbilityHudOverlay {

    private static final Identifier ABILITY_LAYER = Identifier.of("coi-client", "abilities");
    private static AbilitySlotWidget[] abilitySlots;

    public static void initialize() {
        HudConfig.load();

        int maxAbilities = CircleOfImaginationClient.MAX_ABILITIES;
        abilitySlots = new AbilitySlotWidget[maxAbilities];

        for (int i = 0; i < maxAbilities; i++) {
            abilitySlots[i] = new AbilitySlotWidget(i);
        }

        HudRenderCallback.EVENT.register(AbilityHudOverlay::renderAbilities);
    }

    private static void renderAbilities(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (client.player == null || client.options.hudHidden || !settings.enabled) {
            return;
        }

        float tickDelta = 1.0f;

        int screenHeight = client.getWindow().getScaledHeight();
        int startY = (int) ((screenHeight - settings.hudYOffset) / settings.hudScale);
        int hudX = (int) (settings.hudX / settings.hudScale);

        // Only render slots that have abilities bound to them
        int renderedCount = 0;
        for (int i = 0; i < abilitySlots.length; i++) {
            if (!abilitySlots[i].isEmpty()) {
                int x = hudX + (renderedCount * settings.slotSpacing);
                abilitySlots[i].render(context, x, startY, settings.slotSize, tickDelta);
                renderedCount++;
            }
        }
    }

    public static void setCooldown(String abilityId, int cooldownTicks) {
        for (int i = 0; i < abilitySlots.length; i++) {
            if (abilitySlots[i].hasAbility(abilityId)) {
                if (!abilitySlots[i].isOnCooldown()) {
                    abilitySlots[i].setCooldown(cooldownTicks);
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