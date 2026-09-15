package dev.ua.ikeepcalm.coi.client.ui;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The mod's small first-party GUI icons, and the one way to draw them.
 * <p>
 * Each icon ships at exactly the size it is drawn at, so every blit is 1:1 and
 * the artwork never resamples — that is why the constants below are part of the
 * contract rather than a caller's choice. Anything that wants a different size
 * gets a second file, not a scaled blit.
 * <p>
 * A missing icon is a cosmetic problem, not a functional one, so callers get
 * {@code false} back and fall through to whatever they drew before. Presence is
 * cached per identifier and cleared on resource reload, exactly as
 * {@code IconModels} does for item models.
 */
public final class CoiIcons {

    /**
     * Settings cog, beside the HUD settings title.
     * <p>
     * <b>16 is the floor for this artwork.</b> The sources are detailed 64px
     * images, not pixel art upscaled from a small grid — barely a third of their
     * 4×4 blocks are a flat colour — so a reduction below 16px averages away the
     * detail and reads as a blur. Slots narrower than that (tab marks, button
     * gutters, meta-line badges) keep their drawn glyphs, which stay sharp at
     * any size.
     */
    public static final Identifier COG = icon("cog");
    public static final int COG_SIZE = 16;

    private static final int WHITE = 0xFFFFFFFF;

    /**
     * Edge of the fallback crest, when a pathway has no emblem of its own.
     */
    public static final int CREST = 7;

    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();

    private CoiIcons() {
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("coi-client", "textures/gui/icons/" + name + ".png");
    }

    /**
     * Resolves a <em>server-named</em> glyph — the {@code glyph} menu icon kind.
     * <p>
     * The name is a bare word, never a path: everything outside {@code [a-z0-9_]}
     * is dropped, so a document cannot walk out of the icon folder or name a
     * texture in another namespace. A name that survives sanitising but matches
     * no shipped file simply fails {@link #draw}, like any other absent icon.
     *
     * @return null when nothing legible is left of the name
     */
    public static Identifier glyph(String name) {
        if (name == null || name.isEmpty()) return null;
        StringBuilder clean = new StringBuilder(name.length());
        for (int i = 0; i < name.length() && clean.length() < 48; i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') clean.append(c);
        }
        return clean.isEmpty() ? null : icon(clean.toString());
    }

    /** Every glyph this build ships, so the debug screen can show what a server may name. */
    public static final String[] GLYPHS = {
            "alliance", "authority", "cooldown", "cost", "damage", "defense", "divination",
            "flame", "growth", "health", "magic", "power", "regen", "resist", "restore",
            "rites", "saturation", "sequence", "slot_empty", "slot_filled", "soul",
            "spirit", "spirituality", "uniqueness", "ward"
    };

    /**
     * Draws the icon at its authored size in its own colours.
     *
     * @return false when no loaded pack defines it, so the caller can fall back
     */
    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size) {
        return draw(ctx, icon, x, y, size, WHITE);
    }

    /**
     * Draws the icon tinted. The tint is a multiply, so it can dim or colour an
     * icon but never brighten it.
     */
    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size, int argb) {
        if (!present(icon)) return false;
        ctx.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, size, size, size, size, argb);
        return true;
    }

    /**
     * Draws the icon at {@code alpha}, for callers that fade with a surface.
     */
    public static boolean draw(GuiGraphicsExtractor ctx, Identifier icon, int x, int y, int size, float alpha) {
        return draw(ctx, icon, x, y, size, EffectPaint.argb(0xFFFFFF, Math.round(Math.clamp(alpha, 0f, 1f) * 255)));
    }

    /**
     * Draws a pathway's emblem and answers how wide it came out.
     * <p>
     * The mod ships real art for all 25 pathways — 9px bitmaps behind the
     * {@code coi-client:pathway_icons} font — so nothing here should ever need
     * to invent a symbol. It still can: a pathway the map does not know (a
     * server invents one, or the player has none yet) falls back to a diamond
     * crest in the pathway's colour, because a missing glyph would otherwise
     * leave the caption with a ragged gap in front of it.
     *
     * @return the width drawn, so the caller can place the caption after it
     */
    public static int drawPathwayEmblem(GuiGraphicsExtractor ctx, Font font, String pathway,
                                        int x, int y, int argb) {
        Component emblem = Pathways.pathwayEmblem(pathway);
        if (emblem != null) {
            ctx.text(font, emblem, x, y, argb, false);
            return font.width(emblem);
        }
        drawCrest(ctx, x, y + 1, argb);
        return CREST;
    }

    /** The emblem's authored height — the bitmaps behind the pathway font are 9px. */
    public static final int EMBLEM = 9;

    /**
     * The same emblem, scaled to fill a {@code size}-tall slot.
     * <p>
     * The plain overload draws at the font's own 9px whatever the caller asked for, which is right
     * beside a line of text and wrong inside a 32px hero block — there it reads as a speck. The art
     * is a bitmap, so this scales the pose rather than resampling, and whole-number scales stay
     * crisp; 9→18 and 9→27 are exact, anything else is a bilinear blur the same way the item route
     * already is.
     *
     * @return the width drawn, so the caller can place a caption after it
     */
    public static int drawPathwayEmblem(GuiGraphicsExtractor ctx, Font font, String pathway,
                                        int x, int y, int size, int argb) {
        if (size <= EMBLEM) return drawPathwayEmblem(ctx, font, pathway, x, y, argb);
        float scale = size / (float) EMBLEM;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        int drawn = drawPathwayEmblem(ctx, font, pathway, 0, 0, argb);
        pose.popMatrix();
        return Math.round(drawn * scale);
    }

    /**
     * The stand-in emblem: a solid diamond, drawn row by row so it stays a
     * diamond at this size instead of the rounded blob a circle would become.
     */
    private static void drawCrest(GuiGraphicsExtractor ctx, int x, int y, int argb) {
        int mid = CREST / 2;
        for (int row = 0; row < CREST; row++) {
            int spread = mid - Math.abs(row - mid);
            ctx.fill(x + mid - spread, y + row, x + mid + spread + 1, y + row + 1, argb);
        }
        ctx.fill(x + mid, y + mid, x + mid + 1, y + mid + 1, 0xC0000000);
    }

    private static boolean present(Identifier icon) {
        Boolean cached = PRESENT.get(icon);
        if (cached != null) return cached;
        boolean found;
        try {
            Minecraft client = Minecraft.getInstance();
            found = client != null && client.getResourceManager() != null
                    && client.getResourceManager().getResource(icon).isPresent();
        } catch (Exception e) {
            found = false;
        }
        PRESENT.put(icon, found);
        return found;
    }

    /**
     * Forgets which icons were found, because a different pack may define (or
     * stop defining) them.
     */
    public static void clearCache() {
        PRESENT.clear();
    }
}
