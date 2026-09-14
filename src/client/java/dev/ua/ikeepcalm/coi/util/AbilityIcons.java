package dev.ua.ikeepcalm.coi.util;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Set;

/**
 * Shared ability icon renderer with a pathway-tinted backing.
 * <p>
 * Two sources, in order of preference:
 * <ol>
 *   <li>the per-ability item model the server names in {@code AbilityInfo.icon()},
 *       drawn as an item stack whose {@code minecraft:item_model} component points
 *       at it — that is exactly how the plugin skins its shortcut item, so the
 *       HUD and the server GUI show the same art;</li>
 *   <li>otherwise the bundled {@code textures/icons/<category>/<tier>.png}.</li>
 * </ol>
 */
public final class AbilityIcons {

    /**
     * The category folders that actually ship in this mod's assets. Anything
     * else (a category the server invented, or none at all) resolves to
     * {@code uncategorized} rather than blitting a missing texture per frame.
     */
    private static final Set<String> CATEGORIES = Set.of(
            "analytical", "attack", "buffs", "control", "craft", "debuffs", "defence",
            "exploratory", "mobility", "multitype", "summon", "support", "theft", "uncategorized"
    );
    private static final String FALLBACK_CATEGORY = "uncategorized";

    private AbilityIcons() {
    }

    /**
     * Draws the ability's icon in a {@code size × size} box anchored at (x, y).
     */
    public static void draw(GuiGraphicsExtractor graphics, String abilityIdWithName, int x, int y, int size, int alpha) {
        String id = AbilityInfo.extractId(abilityIdWithName);
        AbilityInfo info = CircleOfImaginationClient.getAbilityInfo(id);

        graphics.fill(x, y, x + size, y + size, EffectPaint.argb(AbilityInfo.pathwayColor(id), alpha / 2));

        if (info != null && drawItemModel(graphics, info.icon(), x, y, size)) return;
        drawCategoryIcon(graphics, info, id, x, y, size, alpha);
    }

    /**
     * Renders the resource pack's per-ability model, or reports false when the
     * pack that defines it isn't loaded.
     * <p>
     * Public because a server-authored menu can name an item model for a header
     * or a grid tile, and it must resolve it exactly the way an ability icon
     * does — one route, one answer about which packs are loaded.
     */
    public static boolean drawItemModel(GuiGraphicsExtractor graphics, String icon, int x, int y, int size) {
        if (icon == null || icon.isEmpty() || !IconModels.exists(icon)) return false;
        Identifier model = Identifier.tryParse(icon);
        if (model == null) return false;

        ItemStack stack = new ItemStack(Items.GLOWSTONE_DUST);
        stack.set(DataComponents.ITEM_MODEL, model);

        // Item rendering is fixed at 16×16, so the box size comes from the pose
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / 16f, size / 16f);
        graphics.fakeItem(stack, 0, 0);
        pose.popMatrix();
        return true;
    }

    private static void drawCategoryIcon(GuiGraphicsExtractor graphics, AbilityInfo info, String id,
                                         int x, int y, int size, int alpha) {
        Identifier texture = Identifier.fromNamespaceAndPath("coi-client",
                "textures/icons/" + category(info) + "/" + AbilityInfo.tierOf(id) + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, size, size,
                EffectPaint.argb(0xFFFFFF, alpha));
    }

    /**
     * The shipped category folder for this ability, or {@code uncategorized}.
     */
    public static String category(AbilityInfo info) {
        if (info == null || info.category() == null) return FALLBACK_CATEGORY;
        String category = info.category().toLowerCase(Locale.ROOT);
        return CATEGORIES.contains(category) ? category : FALLBACK_CATEGORY;
    }
}
