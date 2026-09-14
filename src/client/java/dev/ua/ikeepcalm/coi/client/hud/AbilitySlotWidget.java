package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import dev.ua.ikeepcalm.coi.util.AbilityIcons;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;

public class AbilitySlotWidget {

    private static final long CAST_POP_MS = 320;
    private static final long CAST_RING_MS = 420;
    private static final long READY_FLASH_MS = 650;
    private static final int BORDER_COLOR = 0xFF90EE90;
    private static final int BACKGROUND_GRADIENT_TOP = 0xFF98FB98;
    private static final int BACKGROUND_GRADIENT_BOTTOM = 0xFF0A0A0A;
    private static final int COOLDOWN_COLOR = 0xC0000000;
    private static final int READY_BORDER_COLOR = 0xFF228B22;
    private static final int KEYBIND_COLOR = 0xFFFFE135;
    private static final int KEYBIND_BACKGROUND = 0xC0000000;
    private static final int ABILITY_NAME_COLOR = 0xFFE0E0E0;
    private static final int SHADOW_COLOR = 0x60000000;
    private static final int TOGGLE_COLOR = 0xFF55FFFF;
    private static final int LOCKED_TINT = 0x80FF3030;
    private static final int LOCKED_NAME_COLOR = 0xFFFF5555;
    private final int slotIndex;
    private String abilityId;
    private String abilityName;
    /**
     * Server-known metadata for the bound ability; null on protocol-1 servers
     * and for ids the server has never described.
     */
    private AbilityInfo info;
    /**
     * Toggle state pushed by {@code coi-client:state} (or the v2 ability list).
     */
    private boolean toggled;
    /**
     * Localized name of the ability's current category, for the abilities that
     * switch between several.
     */
    private String categoryLabel;
    private int cooldownTicks;
    private int maxCooldownTicks;
    private long lastUseTime;
    private long castTime;
    private long readyFlashTime;

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

        long now = System.currentTimeMillis();

        // Cast punch: the whole slot pops briefly around its center
        boolean popped = false;
        if (castTime > 0 && now - castTime < CAST_POP_MS) {
            float castP = (now - castTime) / (float) CAST_POP_MS;
            float scale = 1f + 0.13f * (float) Math.sin(Math.PI * castP);
            var pose = context.pose();
            pose.pushMatrix();
            pose.translate(x + size / 2f, y + size / 2f);
            pose.scale(scale, scale);
            pose.translate(-(x + size / 2f), -(y + size / 2f));
            popped = true;
        }

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

        if (toggled) {
            renderToggleState(context, textRenderer, x, y, size, now, settings.epilepsyMode);
        }

        renderReadyFlash(context, x, y, size, now);
        renderCastRing(context, x, y, size, now);

        if (popped) {
            context.pose().popMatrix();
        }

        if (onCooldown && cooldownTicks > 0) {
            updateCooldown();
        }
    }

    /**
     * Pathway-colored ring bursting outward from the slot on cast.
     */
    private void renderCastRing(GuiGraphicsExtractor context, int x, int y, int size, long now) {
        if (castTime <= 0 || abilityId == null) return;
        float p = (now - castTime) / (float) CAST_RING_MS;
        if (p >= 1f) return;

        float ease = EffectPaint.easeOutCubic(p);
        int pathway = AbilityInfo.pathwayColor(AbilityInfo.extractId(abilityId)) & 0xFFFFFF;

        int expand = 2 + (int) (13 * ease);
        int a = (int) (210 * (1f - p));
        context.outline(x - expand, y - expand, size + expand * 2, size + expand * 2, EffectPaint.argb(pathway, a));

        int inner = 1 + (int) (7 * ease);
        context.outline(x - inner, y - inner, size + inner * 2, size + inner * 2, EffectPaint.argb(0xFFFFFF, a / 2));
    }

    /**
     * "Ability ready" moment: a glint sweeps across the icon while the
     * border flares — the payoff players watch cooldowns for.
     */
    private void renderReadyFlash(GuiGraphicsExtractor context, int x, int y, int size, long now) {
        if (readyFlashTime <= 0) return;
        float p = (now - readyFlashTime) / (float) READY_FLASH_MS;
        if (p >= 1f) {
            readyFlashTime = 0;
            return;
        }

        // Border flare + soft inner bloom, both decaying
        int fade = (int) (200 * (1f - p));
        context.outline(x - 2, y - 2, size + 4, size + 4, EffectPaint.argb(0x88FF88, fade));
        context.fill(x, y, x + size, y + size, EffectPaint.argb(0xAAFFAA, fade / 4));

        // Diagonal glint sweeping left to right, clipped to the slot
        float sweep = EffectPaint.smoothstep(p);
        int bandCx = (int) (x - size * 0.6f + size * 2.2f * sweep);
        context.enableScissor(x, y, x + size, y + size);
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(bandCx, y + size / 2f);
        pose.rotate(-0.45f);
        context.fill(-2, -size, 2, size, 0xB0FFFFFF);
        context.fill(-6, -size, -2, size, 0x50FFFFFF);
        context.fill(2, -size, 6, size, 0x50FFFFFF);
        pose.popMatrix();
        context.disableScissor();
    }

    public void triggerCastAnimation() {
        castTime = System.currentTimeMillis();
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

        AbilityIcons.draw(context, abilityId, iconX, iconY, iconSize, 255);

        // An ability the server says we may not cast reads as bloodied over
        if (isUnavailable()) {
            context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, LOCKED_TINT);
        }
    }

    /**
     * Cyan outline plus an "ON" tag for an ability the server reports as
     * currently active - the HUD's answer to 405 STATUS action-bar messages.
     */
    private void renderToggleState(GuiGraphicsExtractor context, Font font, int x, int y, int size,
                                   long now, boolean epilepsyMode) {
        float pulse = epilepsyMode ? 1f : 0.72f + 0.28f * (float) Math.sin(now * 0.003);
        int color = EffectPaint.argb(TOGGLE_COLOR, (int) (255 * pulse));
        context.outline(x - 1, y - 1, size + 2, size + 2, color);
        context.outline(x - 2, y - 2, size + 4, size + 4, EffectPaint.argb(TOGGLE_COLOR, (int) (90 * pulse)));

        String tag = I18n.get("hud.coi.toggle_on");
        int tagW = font.width(tag) + 4;
        context.fill(x + 1, y + 1, x + 1 + tagW, y + 11, KEYBIND_BACKGROUND);
        context.text(font, tag, x + 3, y + 2, color, true);
    }

    private boolean isUnavailable() {
        return info != null && info.isUnavailable();
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

        String fake = HudGaslight.cooldownOverride(slotIndex);
        if (fake != null) {
            cooldownText = fake;
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
                readyFlashTime = System.currentTimeMillis();
            }
        }
    }

    private void renderAbilityName(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        if (abilityId == null) return;

        String displayName = AbilityInfo.extractDisplayName(abilityId);
        if (displayName == null) return;

        if (categoryLabel != null && !categoryLabel.isEmpty()) {
            displayName = displayName + " \u00B7 " + categoryLabel;
        }
        if (displayName.length() > 12) {
            displayName = displayName.substring(0, 12) + "...";
        }

        int textWidth = textRenderer.width(displayName);
        int textX = x + (size - textWidth) / 2;
        int textY = y + size + 3;

        context.text(textRenderer, displayName, textX + 1, textY + 1, 0x80000000, false);
        if (isUnavailable()) {
            Component struck = Component.literal(displayName).withStyle(ChatFormatting.STRIKETHROUGH);
            context.text(textRenderer, struck, textX, textY, LOCKED_NAME_COLOR, true);
        } else {
            context.text(textRenderer, displayName, textX, textY, ABILITY_NAME_COLOR, true);
        }
    }

    private void renderKeybind(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int size) {
        KeyMapping keyBinding = getKeyBinding();
        if (keyBinding == null) return;

        String keyText = KeyMappingHelper.getBoundKeyOf(keyBinding).getName();
        if (keyText.length() > 3) {
            keyText = keyText.substring(0, 3);
        }
        keyText = HudGaslight.corruptKeybind(slotIndex, keyText);

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


    public void setAbility(String abilityId) {
        this.abilityId = abilityId;
        this.categoryLabel = null;
        if (abilityId != null && abilityId.contains(" - ")) {
            this.info = CircleOfImaginationClient.getAbilityInfo(AbilityInfo.extractId(abilityId));
            this.abilityName = AbilityInfo.extractDisplayName(abilityId);
            this.toggled = info != null && info.active();
        } else {
            this.info = null;
            this.abilityName = null;
            this.toggled = false;
        }
    }

    public void setCooldown(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
        this.maxCooldownTicks = cooldownTicks;
        this.lastUseTime = System.currentTimeMillis();
    }

    /**
     * Server-authoritative cooldown: back-dates {@code lastUseTime} so the
     * local countdown picks up mid-cooldown instead of restarting it.
     */
    public void setCooldown(int remainingTicks, int maxTicks) {
        this.cooldownTicks = Math.max(0, remainingTicks);
        this.maxCooldownTicks = Math.max(this.cooldownTicks, maxTicks);
        this.lastUseTime = this.cooldownTicks > 0
                ? System.currentTimeMillis() - (long) (this.maxCooldownTicks - this.cooldownTicks) * 50L
                : 0;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    /**
     * Exact-id match - {@code contains} used to let {@code sun-9-0} light up
     * {@code sun-9-01} as well.
     */
    public boolean hasAbility(String abilityId) {
        return Objects.equals(AbilityInfo.extractId(this.abilityId), abilityId);
    }

    public boolean isEmpty() {
        return this.abilityId == null;
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
