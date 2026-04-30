package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class AbilityWheelScreen extends Screen {

    private static final int WHEEL_RADIUS = 80;
    private static final int SLOT_RADIUS = 25;
    private static final int INNER_RADIUS = 30;

    private int selectedSlot = -1;
    private int ticksOpen = 0;

    public AbilityWheelScreen() {
        super(Component.literal("Ability Wheel"));
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            // Force release mouse
            this.minecraft.mouseHandler.releaseMouse();
        }
    }

    @Override
    public void tick() {
        ticksOpen++;
        
        // Increase delay slightly and ensure robust key check
        if (ticksOpen > 2 && !CircleOfImaginationClient.isKeyDown(CircleOfImaginationClient.abilityWheel)) {
            if (selectedSlot != -1) {
                String ability = CircleOfImaginationClient.getWheelAbility(selectedSlot);
                if (ability != null) {
                    CircleOfImaginationClient.useAbilityById(ability);
                }
            }
            this.onClose();
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int wheelSize = CircleOfImaginationClient.getWheelSize();

        // Render darkened background
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > INNER_RADIUS) {
            double rawAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (rawAngle < 0) rawAngle += 360;
            selectedSlot = (int) ((rawAngle + (360.0 / wheelSize / 2)) % 360) / (360 / wheelSize);
            if (selectedSlot >= wheelSize) selectedSlot = wheelSize - 1;
        } else {
            selectedSlot = -1;
        }

        renderSlots(graphics, centerX, centerY, wheelSize);
        renderSelectedAbilityName(graphics, centerX, centerY);
        
        // Render a small cursor dot at the mouse position
        graphics.fill(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xFFFFFFFF);
    }

    private void renderSlots(GuiGraphicsExtractor graphics, int centerX, int centerY, int wheelSize) {
        for (int i = 0; i < wheelSize; i++) {
            double angle = Math.toRadians(i * (360.0 / wheelSize) - 90);
            int x = centerX + (int) (Math.cos(angle) * WHEEL_RADIUS);
            int y = centerY + (int) (Math.sin(angle) * WHEEL_RADIUS);

            boolean isSelected = i == selectedSlot;
            int color = isSelected ? 0xFF55FF55 : 0xFFFFFFFF;
            int bgColor = isSelected ? 0x8055FF55 : 0x80000000;

            // Slot background
            graphics.fill(x - SLOT_RADIUS, y - SLOT_RADIUS, x + SLOT_RADIUS, y + SLOT_RADIUS, bgColor);
            // Border
            graphics.fill(x - SLOT_RADIUS - 1, y - SLOT_RADIUS - 1, x + SLOT_RADIUS + 1, y - SLOT_RADIUS, color);
            graphics.fill(x - SLOT_RADIUS - 1, y + SLOT_RADIUS, x + SLOT_RADIUS + 1, y + SLOT_RADIUS + 1, color);
            graphics.fill(x - SLOT_RADIUS - 1, y - SLOT_RADIUS, x - SLOT_RADIUS, y + SLOT_RADIUS, color);
            graphics.fill(x + SLOT_RADIUS, y - SLOT_RADIUS, x + SLOT_RADIUS + 1, y + SLOT_RADIUS, color);

            String abilityIdWithName = CircleOfImaginationClient.getWheelAbility(i);
            if (abilityIdWithName != null) {
                renderAbilityIcon(graphics, abilityIdWithName, x, y, SLOT_RADIUS * 2 - 10);
            } else {
                graphics.centeredText(this.font, Component.literal("+"), x, y - 4, 0xFF555555);
            }
        }
    }

    private void renderSelectedAbilityName(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        if (selectedSlot != -1) {
            String abilityIdWithName = CircleOfImaginationClient.getWheelAbility(selectedSlot);
            if (abilityIdWithName != null) {
                String id = AbilityInfo.extractId(abilityIdWithName);
                AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(id);
                String name = info != null ? info.englishName() : AbilityInfo.extractDisplayName(abilityIdWithName);
                graphics.centeredText(this.font, Component.literal(name), centerX, centerY - 10, 0xFFFFFFFF);
            } else {
                graphics.centeredText(this.font, Component.translatable("screen.coi.empty_slot"), centerX, centerY - 10, 0xFFAAAAAA);
            }
        } else {
            graphics.centeredText(this.font, Component.translatable("screen.coi.select_ability"), centerX, centerY - 10, 0xFFFFFFFF);
        }
    }

    private void renderAbilityIcon(GuiGraphicsExtractor context, String abilityIdWithName, int x, int y, int size) {
        String id = AbilityInfo.extractId(abilityIdWithName);
        AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(id);
        
        int iconX = x - size / 2;
        int iconY = y - size / 2;

        int pathwayColor = getPathwayColor(id);
        context.fill(iconX, iconY, iconX + size, iconY + size, (0x80 << 24) | (pathwayColor & 0xFFFFFF));

        String category = info != null ? info.category() : "uncategorized";
        String tier = getTierFromAbilityId(id);
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client", "textures/icons/" + category.toLowerCase() + "/" + tier + ".png");
        context.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0, 0, size, size, size, size);
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

    private String getTierFromAbilityId(String rawId) {
        if (rawId == null) return "low";
        String[] parts = rawId.split("-");
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

    @Override
    public void onClose() {
        super.onClose();
    }
}