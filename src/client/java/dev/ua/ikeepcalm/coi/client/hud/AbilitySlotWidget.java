package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AbilitySlotWidget {

    private final int slotIndex;
    private String abilityId;
    private String abilityName;
    private String category;
    private int cooldownTicks;
    private int maxCooldownTicks;
    private long lastUseTime;

    private static final int BORDER_COLOR = 0xFF90EE90;
    private static final int BACKGROUND_GRADIENT_TOP = 0xFF98FB98;
    private static final int BACKGROUND_GRADIENT_BOTTOM = 0xFF0A0A0A;
    private static final int COOLDOWN_COLOR = 0xC0000000;
    private static final int READY_BORDER_COLOR = 0xFF228B22;
    private static final int KEYBIND_COLOR = 0xFFFFE135;
    private static final int KEYBIND_BACKGROUND = 0xC0000000;
    private static final int ABILITY_NAME_COLOR = 0xFFE0E0E0;
    private static final int SHADOW_COLOR = 0x60000000;

    public AbilitySlotWidget(int slotIndex) {
        this.slotIndex = slotIndex;
        this.cooldownTicks = 0;
        this.maxCooldownTicks = 0;
    }

    public void render(GuiGraphicsExtractor context, int x, int y, int size, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        boolean hasAbility = abilityId != null;
        boolean onCooldown = cooldownTicks > 0;
        boolean isReady = hasAbility && !onCooldown;


        renderDropShadow(context, x, y, size);

        if (isReady && settings.showGlowEffect) {
            renderGlowEffect(context, x, y, size);
        }

        int borderColor = isReady ? READY_BORDER_COLOR : BORDER_COLOR;
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);

        renderGradientBackground(context, x, y, size);

        if (isReady) {
            renderReadyOverlay(context, x, y, size);
        }

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


    private void renderDropShadow(GuiGraphicsExtractor context, int x, int y, int size) {
        context.fill(x + 2, y + 2, x + size + 3, y + size + 3, SHADOW_COLOR);
    }

    private void renderGradientBackground(GuiGraphicsExtractor context, int x, int y, int size) {
        context.fill(x, y, x + size, y + size / 2, BACKGROUND_GRADIENT_TOP);
        context.fill(x, y + size / 2, x + size, y + size, BACKGROUND_GRADIENT_BOTTOM);
    }

    private void renderReadyOverlay(GuiGraphicsExtractor context, int x, int y, int size) {
        int overlayColor = 0x20228B22;
        context.fill(x, y, x + size, y + size, overlayColor);
    }

    private void renderGlowEffect(GuiGraphicsExtractor context, int x, int y, int size) {
        int alpha = 60;
        int glowColor = (alpha << 24) | 0x32CD32;
        context.fill(x - 2, y - 2, x + size + 2, y + size + 2, glowColor);
    }

    private void renderAbilityIcon(GuiGraphicsExtractor context, int x, int y, int size) {
        if (abilityId == null) return;

        int iconX = x + 3;
        int iconY = y + 3;
        int iconSize = size - 6;

        // Pathway-colored background behind the icon
        int pathwayColor = getPathwayColor(abilityId);
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, pathwayColor);

        // Category texture
        String cat = (category != null && !category.isEmpty()) ? category.toLowerCase() : "uncategorized";
        String tier = getTierFromAbilityId(abilityId);
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client", "textures/icons/" + cat + "/" + tier + ".png");
        context.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }

    private String getTierFromAbilityId(String rawId) {
        if (rawId == null) return "low";
        String id = rawId.contains(" - ") ? rawId.split(" - ")[0] : rawId;
        String[] parts = id.split("-");
        if (parts.length < 2) return "low";
        try {
            int seq = Integer.parseInt(parts[1]);
            return switch (seq) {
                case 0 -> "divine";
                case 2, 1 -> "fair";
                case 3, 4 -> "high";
                case 5, 6, 7 -> "mid";
                default -> "low";
            };
        } catch (NumberFormatException e) {
            return "low";
        }
    }

    private void renderCooldownOverlay(GuiGraphicsExtractor context, int x, int y, int size, float tickDelta) {
        if (maxCooldownTicks <= 0) return;

        float progress = (cooldownTicks - tickDelta) / maxCooldownTicks;
        progress = Mth.clamp(progress, 0.0f, 1.0f);

        int overlayHeight = (int) (size * progress);
        if (overlayHeight > 0) {
            context.fill(x, y, x + size, y + overlayHeight, COOLDOWN_COLOR);
        }
    }

    private void renderCooldownText(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
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

        int textWidth = textRenderer.width(cooldownText);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size / 2 - 4;

        context.text(textRenderer, cooldownText, textX, textY, 0xFFFFFF, true);
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

    private void renderAbilityName(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        if (abilityId == null) return;

        AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(AbilityInfo.extractId(abilityId));
        String displayName = info != null ? info.englishName() : abilityName;
        if (displayName == null) return;

        if (displayName.length() > 12) {
            displayName = displayName.substring(0, 12) + "...";
        }

        int textWidth = textRenderer.width(displayName);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size + 3;

        context.text(textRenderer, displayName, textX + 1, textY + 1, 0x80000000, false);
        context.text(textRenderer, displayName, textX, textY, ABILITY_NAME_COLOR, true);
    }

    private void renderKeybind(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        KeyMapping keyBinding = getKeyBinding();
        if (keyBinding == null) return;

        String keyText = KeyMappingHelper.getBoundKeyOf(keyBinding).getName();
        if (keyText.length() > 3) {
            keyText = keyText.substring(0, 3);
        }

        int textWidth = textRenderer.width(keyText);
        int padding = 2;
        int bgWidth = textWidth + padding * 2;
        int bgHeight = 10;

        int bgX = x + size - bgWidth - 2;
        int bgY = y + 2;
        int textX = bgX + padding;
        int textY = bgY + 1;

        context.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, KEYBIND_BACKGROUND);
        context.fill(bgX, bgY, bgX + bgWidth, bgY + 1, 0xFF555555);
        context.text(textRenderer, keyText, textX, textY, KEYBIND_COLOR, true);
    }

    private KeyMapping getKeyBinding() {
        if (slotIndex >= 0 && slotIndex < CircleOfImaginationClient.abilityKeys.length) {
            return CircleOfImaginationClient.abilityKeys[slotIndex];
        }
        return null;
    }

    private int getPathwayColor(String abilityId) {
        if (abilityId == null) return 0xFF666666;

        String pathway = abilityId.split("-")[0];
        return switch (pathway.toLowerCase()) {
            case "fool" -> 0xFFB347CC;
            case "door" -> 0xFF5B7FE6;
            case "sun" -> 0xFFFFE55C;
            case "tyrant" -> 0xFF4AA3FF;
            case "demoness" -> 0xFFB22222;
            case "priest" -> 0xFFFF6B35;
            case "error" -> 0xFF999999;
            default -> 0xFF777777;
        };
    }


    public void setAbility(String abilityId) {
        this.abilityId = abilityId;
        if (abilityId != null && abilityId.contains(" - ")) {
            String[] parts = abilityId.split(" - ", 2);
            AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(parts[0]);
            this.abilityName = info != null ? info.englishName() : parts[1];
            this.category = info != null ? info.category() : "uncategorized";
        } else {
            this.abilityName = null;
            this.category = "uncategorized";
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

    public boolean isEmpty() {
        return this.abilityId == null;
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }
}