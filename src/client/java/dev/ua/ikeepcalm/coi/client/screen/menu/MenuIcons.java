package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.ui.AbilityIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The five places a document's icon can come from, and the one call that picks
 * between them.
 * <p>
 * This is the half of {@link MenuTheme} that does not paint: an icon is not a
 * colour decision but a lookup — a pathway emblem, an item model, an ability,
 * a player's head, or one of the glyphs that ship in this jar. Only the last
 * cannot be defeated by the player's resource pack, which is why a server names
 * it for anything conceptual.
 */
public final class MenuIcons {

    /**
     * How faint the document's pathway emblem sits behind the card. Any louder
     * and it competes with the text it is behind.
     */
    private static final float WATERMARK_ALPHA = 0.06f;

    /** The edge of the pathway-emblem bitmaps the watermark scales up from. */
    private static final int EMBLEM = 9;

    private static final int SKIN_SHEET = 64;

    private MenuIcons() {
    }

    /**
     * The document's pathway emblem, blown up and left almost invisible behind
     * the card's top-right corner. This is what makes a Fool screen and a Sun
     * screen feel like different places without either of them saying so.
     */
    public static void watermark(GuiGraphicsExtractor g, Font font, MenuIcon icon,
                                 int x, int y, int size) {
        if (icon.kind() != MenuIcon.Kind.PATHWAY || !icon.present()) return;
        int color = MenuTheme.withAlpha(0xFF000000 | Pathways.pathwayRgb(icon.value()), WATERMARK_ALPHA);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / (float) EMBLEM, size / (float) EMBLEM);
        CoiIcons.drawPathwayEmblem(g, font, icon.value(), 0, 0, color);
        pose.popMatrix();
    }

    /**
     * Draws whichever of the three icon sources the document named.
     *
     * @return false when nothing was drawn, so the caller can close the gap
     */
    public static boolean draw(GuiGraphicsExtractor g, Font font, MenuIcon icon, int x, int y, int size, float alpha) {
        if (!icon.present()) return false;
        return switch (icon.kind()) {
            case PATHWAY -> {
                int color = MenuTheme.withAlpha(0xFF000000 | Pathways.pathwayRgb(icon.value()), alpha);
                // Scaled to the slot. Drawing the font's own 9px inside a 32px hero
                // made the emblem read as a speck beside a 1.5x title.
                CoiIcons.drawPathwayEmblem(g, font, icon.value(), x, y, size, color);
                yield true;
            }
            case ITEM -> AbilityIcons.drawItemModel(g, icon.value(), x, y, size);
            // An ability drawn the way the mod always draws abilities: the pack's
            // per-ability item model, else the bundled category/tier art. ITEM
            // alone silently draws *nothing* when the pack does not define the
            // model, which is how ability tiles came out blank.
            case ABILITY -> {
                AbilityIcons.draw(g, icon.value(), x, y, size, Math.round(Math.clamp(alpha, 0f, 1f) * 255));
                yield true;
            }
            case HEAD -> drawHead(g, icon.value(), x, y, size);
            // The mod's own art, so this is the one kind that cannot be defeated
            // by the player's resource pack. It is authored at 16 and drawn at
            // whatever the slot asks for; below 16 it blurs, which is why the
            // callers that use small slots pass their drawn glyphs instead.
            case GLYPH -> {
                Identifier id = CoiIcons.glyph(icon.value());
                yield id != null && CoiIcons.draw(g, id, x, y, size, alpha);
            }
            case NONE -> false;
        };
    }

    /**
     * The player's face and hat layer off the 64×64 skin sheet, for anyone the
     * tab list still knows about. An unknown uuid draws nothing rather than the
     * default Steve, which would name the wrong person.
     */
    private static boolean drawHead(GuiGraphicsExtractor g, String uuid, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        PlayerInfo info;
        try {
            info = client.getConnection().getPlayerInfo(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (info == null) return false;
        Identifier skin = info.getSkin().body().texturePath();
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, size, size, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, size, size, 8, 8,
                SKIN_SHEET, SKIN_SHEET, 0xFFFFFFFF);
        return true;
    }
}
