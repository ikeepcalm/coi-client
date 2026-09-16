package dev.ua.ikeepcalm.coi.client.gesture;

import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The five recognizable gesture shapes. Direction templates use 8-way codes
 * '0'..'7' starting at east and rotating clockwise in screen space (y down):
 * 0=E, 1=SE, 2=S, 3=SW, 4=W, 5=NW, 6=N, 7=NE.
 *
 * Matching variants (reversed stroke order; every starting corner for closed
 * shapes) are derived from the canonical template, so a circle drawn
 * counter-clockwise from the left matches just as well as the canonical one.
 */
public enum GestureType {
    CIRCLE("circle", "01234567", true, new float[][]{
            {0.5f, 0f}, {0.75f, 0.067f}, {0.933f, 0.25f}, {1f, 0.5f},
            {0.933f, 0.75f}, {0.75f, 0.933f}, {0.5f, 1f}, {0.25f, 0.933f},
            {0.067f, 0.75f}, {0f, 0.5f}, {0.067f, 0.25f}, {0.25f, 0.067f}, {0.5f, 0f}
    }),
    V("v", "17", false, new float[][]{{0f, 0f}, {0.5f, 1f}, {1f, 0f}}),
    Z("z", "030", false, new float[][]{{0f, 0f}, {1f, 0f}, {0f, 1f}, {1f, 1f}}),
    LINE_DOWN("line_down", "2", false, new float[][]{{0.5f, 0f}, {0.5f, 1f}}),
    TRIANGLE("triangle", "305", true, new float[][]{{0.5f, 0f}, {0f, 1f}, {1f, 1f}, {0.5f, 0f}});

    private final String id;
    private final float[][] preview;
    private final List<String> variants;

    GestureType(String id, String template, boolean cyclic, float[][] preview) {
        this.id = id;
        this.preview = preview;
        this.variants = buildVariants(template, cyclic);
    }

    private static List<String> buildVariants(String template, boolean cyclic) {
        Set<String> out = new LinkedHashSet<>();
        addRotations(out, template, cyclic);
        addRotations(out, reversed(template), cyclic);
        return List.copyOf(out);
    }

    private static void addRotations(Set<String> out, String template, boolean cyclic) {
        int rotations = cyclic ? template.length() : 1;
        for (int k = 0; k < rotations; k++) {
            out.add(template.substring(k) + template.substring(0, k));
        }
    }

    /**
     * The same stroke traced backwards: direction codes flipped 180° (+4 mod 8)
     * and read in reverse order.
     */
    private static String reversed(String template) {
        StringBuilder sb = new StringBuilder(template.length());
        for (int i = template.length() - 1; i >= 0; i--) {
            sb.append((char) ('0' + ((template.charAt(i) - '0' + 4) % 8)));
        }
        return sb.toString();
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("screen.coi.gesture." + id);
    }

    public List<String> templateVariants() {
        return variants;
    }

    /**
     * Tiny polyline glyph of the shape, scaled into a size×size box.
     */
    public void drawPreview(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        for (int i = 1; i < preview.length; i++) {
            EffectPaint.line(graphics,
                    x + preview[i - 1][0] * size, y + preview[i - 1][1] * size,
                    x + preview[i][0] * size, y + preview[i][1] * size,
                    color, 1);
        }
    }
}
