package dev.ua.ikeepcalm.coi.client.data;

import net.minecraft.ChatFormatting;

public record IngredientInfo(String pathway, int sequence, boolean isMain, ChatFormatting color) {
}
