package dev.ua.ikeepcalm.coi.util;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Shared renderer for ability category/tier icons with a pathway-tinted backing.
 */
public final class AbilityIcons {

    private AbilityIcons() {
    }

    /**
     * Draws the ability's category/tier icon in a {@code size × size} box anchored at (x, y).
     */
    public static void draw(GuiGraphicsExtractor graphics, String abilityIdWithName, int x, int y, int size, int alpha) {
        String id = AbilityInfo.extractId(abilityIdWithName);
        AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(id);

        int pathwayColor = AbilityInfo.pathwayColor(id);
        graphics.fill(x, y, x + size, y + size, EffectPaint.argb(pathwayColor, alpha / 2));

        String category = info != null ? info.category() : "uncategorized";
        String tier = AbilityInfo.tierOf(id);
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client", "textures/icons/" + category.toLowerCase() + "/" + tier + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size, EffectPaint.argb(0xFFFFFF, alpha));
    }
}
