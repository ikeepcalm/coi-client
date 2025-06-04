package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class AbilitySlotWidget {

    private final int slotIndex;
    private String abilityId;
    private String abilityName;
    private int cooldownTicks;
    private int maxCooldownTicks;
    private long lastUseTime;

    private static final int BORDER_COLOR = 0xFF444444;
    private static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int COOLDOWN_COLOR = 0x80000000;
    private static final int READY_GLOW_COLOR = 0xFF00FF00;
    private static final int KEYBIND_COLOR = 0xFFFFFF55;
    private static final int ABILITY_NAME_COLOR = 0xFFFFFFFF;

    public AbilitySlotWidget(int slotIndex) {
        this.slotIndex = slotIndex;
        this.cooldownTicks = 0;
        this.maxCooldownTicks = 0;
    }

    public void render(DrawContext context, int x, int y, int size, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        boolean hasAbility = abilityId != null;
        boolean onCooldown = cooldownTicks > 0;
        boolean isReady = hasAbility && !onCooldown;

        int borderColor = isReady ? READY_GLOW_COLOR : BORDER_COLOR;
        if (isReady && settings.showGlowEffect) {
            renderGlowEffect(context, x, y, size);
        }

        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);
        context.fill(x, y, x + size, y + size, BACKGROUND_COLOR);

        if (hasAbility) {
            renderAbilityIcon(context, x, y, size);

            if (onCooldown) {
                renderCooldownOverlay(context, x, y, size, tickDelta);
                renderCooldownText(context, textRenderer, x, y, size);
            }

            if (settings.showAbilityNames) {
                renderAbilityName(context, textRenderer, x, y, size);
            }
        }

        if (settings.showKeybinds) {
            renderKeybind(context, textRenderer, x, y, size);
        }

        if (onCooldown && cooldownTicks > 0) {
            updateCooldown();
        }
    }

    private void renderGlowEffect(DrawContext context, int x, int y, int size) {
        double time = Util.getMeasuringTimeMs() / 1000.0;
        float pulse = (float) (Math.sin(time * 3.0) * 0.3 + 0.7);
        int alpha = (int) (pulse * 255);
        int glowColor = (alpha << 24) | 0x00FF00;

        context.fill(x - 2, y - 2, x + size + 2, y + size + 2, glowColor);
    }

    private void renderAbilityIcon(DrawContext context, int x, int y, int size) {
        if (abilityName == null) return;

        int iconSize = size - 8;
        int iconX = x + 4;
        int iconY = y + 4;

        int pathwayColor = getPathwayColor(abilityId);
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, pathwayColor);

        if (!(cooldownTicks > 0)) {
            String initials = getAbilityInitials(abilityName);
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(initials);
            int textX = iconX + (iconSize - textWidth) / 2;
            int textY = iconY + (iconSize - 8) / 2;

            context.drawText(MinecraftClient.getInstance().textRenderer, initials,
                    textX, textY, 0xFFFFFF, true);
        }
    }

    private void renderCooldownOverlay(DrawContext context, int x, int y, int size, float tickDelta) {
        if (maxCooldownTicks <= 0) return;

        float progress = (cooldownTicks - tickDelta) / maxCooldownTicks;
        progress = MathHelper.clamp(progress, 0.0f, 1.0f);

        int overlayHeight = (int) (size * progress);
        if (overlayHeight > 0) {
            context.fill(x, y + size - overlayHeight, x + size, y + size, COOLDOWN_COLOR);
        }

        int sweepAngle = (int) (360 * (1 - progress));
        renderCooldownSweep(context, x, y, size, sweepAngle);
    }

    private void renderCooldownSweep(DrawContext context, int x, int y, int size, int sweepAngle) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 - 2;

        for (int angle = 0; angle < sweepAngle; angle += 2) {
            double radian = Math.toRadians(angle - 90);
            int endX = centerX + (int) (Math.cos(radian) * radius);
            int endY = centerY + (int) (Math.sin(radian) * radius);

            context.fill(endX, endY, endX + 2, endY + 2, 0x60FF0000);
        }
    }

    private void renderCooldownText(DrawContext context, TextRenderer textRenderer, int x, int y, int size) {
        if (cooldownTicks <= 0) return;

        float seconds = cooldownTicks / 20.0f;
        String cooldownText;

        if (seconds >= 10) {
            cooldownText = String.valueOf((int) seconds);
        } else if (seconds >= 1) {
            cooldownText = String.format("%.1f", seconds);
        } else {
            cooldownText = String.format("%.1f", seconds);
        }

        int textWidth = textRenderer.getWidth(cooldownText);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size / 2 - 4;

        context.drawText(textRenderer, cooldownText, textX, textY, 0xFFFFFF, true);
    }

    private void updateCooldown() {
        if (cooldownTicks > 0 && lastUseTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUseTime;
            int elapsedTicks = (int) (elapsedTime / 50);

            cooldownTicks = Math.max(0, maxCooldownTicks - elapsedTicks);

            if (cooldownTicks <= 0) {
                lastUseTime = 0;
            }
        }
    }

    private void renderAbilityName(DrawContext context, TextRenderer textRenderer, int x, int y, int size) {
        if (abilityName == null) return;

        String displayName = abilityName;
        if (displayName.length() > 12) {
            displayName = displayName.substring(0, 12) + "...";
        }

        int textWidth = textRenderer.getWidth(displayName);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size + 2;

        context.drawText(textRenderer, displayName, textX, textY, ABILITY_NAME_COLOR, true);
    }

    private void renderKeybind(DrawContext context, TextRenderer textRenderer, int x, int y, int size) {
        KeyBinding keyBinding = getKeyBinding();
        if (keyBinding == null) return;

        String keyText = KeyBindingHelper.getBoundKeyOf(keyBinding).getLocalizedText().getString();
        if (keyText.length() > 3) {
            keyText = keyText.substring(0, 3);
        }

        int textWidth = textRenderer.getWidth(keyText);
        int textX = x + size - textWidth - 2;
        int textY = y + 2;

        context.fill(textX - 1, textY - 1, textX + textWidth + 1, textY + 9, 0x80000000);
        context.drawText(textRenderer, keyText, textX, textY, KEYBIND_COLOR, true);
    }

    private KeyBinding getKeyBinding() {
        return switch (slotIndex) {
            case 0 -> CircleOfImaginationClient.ability1Key;
            case 1 -> CircleOfImaginationClient.ability2Key;
            case 2 -> CircleOfImaginationClient.ability3Key;
            default -> null;
        };
    }

    private int getPathwayColor(String abilityId) {
        if (abilityId == null) return 0xFF666666;

        String pathway = abilityId.split("-")[0];
        return switch (pathway.toLowerCase()) {
            case "fool" -> 0xFF9932CC;
            case "door" -> 0xFF4169E1;
            case "sun" -> 0xFFFFD700;
            case "tyrant" -> 0xFF1E90FF;
            case "demoness" -> 0xFF8B0000;
            case "priest" -> 0xFFFF4500;
            case "error" -> 0xFF808080;
            default -> 0xFF666666;
        };
    }

    private String getAbilityInitials(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) return "?";

        String[] words = abilityName.split(" ");
        if (words.length == 1) {
            return abilityName.length() >= 2 ? abilityName.substring(0, 2).toUpperCase() : abilityName.toUpperCase();
        } else {
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(words.length, 3); i++) {
                if (!words[i].isEmpty()) {
                    initials.append(words[i].charAt(0));
                }
            }
            return initials.toString().toUpperCase();
        }
    }

    public void setAbility(String abilityId) {
        this.abilityId = abilityId;
        if (abilityId != null && abilityId.contains(" - ")) {
            String[] parts = abilityId.split(" - ");
            this.abilityName = parts.length > 1 ? parts[1] : abilityId;
        } else {
            this.abilityName = null;
        }
    }

    public void setCooldown(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
        this.maxCooldownTicks = cooldownTicks;
        this.lastUseTime = System.currentTimeMillis();
    }

    public boolean hasAbility(String abilityId) {
        return this.abilityId != null && this.abilityId.contains(abilityId);
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }
}