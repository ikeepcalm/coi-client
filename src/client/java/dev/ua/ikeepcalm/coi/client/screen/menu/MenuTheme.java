package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The paint box for server-authored menus: every colour decision and every
 * hand-drawn primitive lives here, so {@link MenuScreen} is only layout and
 * input. Its two specialist halves are beside it — {@link MenuGauges} for the
 * meters and {@link MenuIcons} for the icon sources.
 * <p>
 * Nothing here is a vanilla widget sprite. The mod's screens are drawn in
 * {@link CoiStyle}'s dark/gold card chrome, and a stone-grey vanilla button in
 * the middle of one reads as a different program — which is precisely how the
 * InvUI chest menus these documents replace used to read.
 */
public class MenuTheme {

    public static final int INFO = 0xFF7FC8FF;
    public static final int SUCCESS = 0xFF5FD35F;
    public static final int WARN = 0xFFE0A83C;
    public static final int DANGER = 0xFFE04C4C;

    /**
     * Fill for anything that sits on the card as its own surface — field
     * backgrounds, list rows, grid tiles. Light enough to separate from the
     * card, dark enough that text keeps its contrast.
     */
    public static final int SURFACE = 0x18FFFFFF;
    public static final int SURFACE_HOVER = 0x30FFFFFF;
    public static final int RULE = 0x28FFFFFF;

    /**
     * The outline a disabled surface gets — dark enough to read as off without
     * disappearing into the card.
     */
    public static final int BORDER_OFF = 0xFF2A2A32;

    /**
     * Text on a filled accent button. Buttons carry the accent; their label has
     * to be the dark half of that pair or the whole thing turns to mush.
     */
    private static final int ON_ACCENT = 0xFF14141A;

    /**
     * Chip and badge height. 16 rather than the font's bare 8+2: a pill sized to its text alone
     * reads as a cramped label rather than a chip, and these carry real values — a state word, a
     * stability verdict — that deserve the same presence as the number beside them.
     */
    public static final int CHIP_H = 16;

    /**
     * The y a line of text starts at to sit centred in a {@link #CHIP_H} pill.
     * <p>
     * One helper because there are four of these boxes — chips, badges, grid-cell badges, hero
     * badges — and they drifted apart the moment the height changed: the pill grew and the text
     * stayed where it was, which reads as text jammed against the top edge rather than as a taller
     * chip. Minecraft glyphs carry their ink in the top 7 rows of an 8-row cell, so centring on 8
     * lands the ink a touch high on purpose.
     */
    public static int chipTextY(int y) {
        return y + (CHIP_H - 8) / 2;
    }

    private MenuTheme() {
    }

    // --- Colours ---

    public static int textColor(MenuComponent.TextStyle style, int accentArgb) {
        return switch (style) {
            case BODY -> CoiStyle.TEXT_BODY;
            case MUTED -> CoiStyle.TEXT_MUTED;
            case HEADING -> accentArgb;
            case WARN -> WARN;
            case DANGER -> DANGER;
            case SUCCESS -> SUCCESS;
        };
    }

    /**
     * The document's {@code toast.style} words, which are the plugin's own
     * severity vocabulary rather than the component styles.
     */
    public static int toastColor(String style, int accentArgb) {
        return switch (style == null ? "" : style.toLowerCase(Locale.ROOT)) {
            case "success" -> SUCCESS;
            case "warn", "warning" -> WARN;
            case "error", "danger" -> DANGER;
            case "info" -> INFO;
            default -> accentArgb;
        };
    }

    public static int argb(int rgb, int fallbackArgb) {
        return rgb != 0 ? 0xFF000000 | rgb : fallbackArgb;
    }

    /**
     * Blends toward white ({@code amount > 0}) or black ({@code amount < 0}),
     * keeping the alpha — the one operation hover and press states need.
     */
    public static int shade(int argb, float amount) {
        int target = amount >= 0 ? 0xFF : 0x00;
        float f = Math.abs(amount);
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - f) + target * f);
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - f) + target * f);
        int b = Math.round((argb & 0xFF) * (1 - f) + target * f);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, float alpha) {
        return EffectPaint.argb(argb & 0xFFFFFF, Math.round(Math.clamp(alpha, 0f, 1f) * 255));
    }

    /**
     * Straight component-wise blend, alpha included — the one operation an
     * eased hover tint needs, since {@link #SURFACE} and {@link #SURFACE_HOVER}
     * differ in alpha and not in colour.
     */
    public static int lerpArgb(int from, int to, float t) {
        float f = Math.clamp(t, 0f, 1f);
        int a = Math.round(((from >>> 24) & 0xFF) * (1 - f) + ((to >>> 24) & 0xFF) * f);
        int r = Math.round(((from >> 16) & 0xFF) * (1 - f) + ((to >> 16) & 0xFF) * f);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - f) + ((to >> 8) & 0xFF) * f);
        int b = Math.round((from & 0xFF) * (1 - f) + (to & 0xFF) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * The fill for a surface that is {@code t} of the way into its hover, where
     * the caller has already decided whether hovering means anything here.
     */
    public static int surface(float hoverT) {
        return lerpArgb(SURFACE, SURFACE_HOVER, hoverT);
    }

    /**
     * A trend token's colour, read off its own sign: the server writes
     * {@code "+12"} or {@code "-3"} and never has to name a colour for it.
     */
    public static int deltaColor(String delta) {
        if (delta.startsWith("+")) return SUCCESS;
        if (delta.startsWith("-") || delta.startsWith("−")) return DANGER;
        return CoiStyle.TEXT_MUTED;
    }

    // --- Primitives ---

    /**
     * A section heading: the caption in the accent, upper-cased and spaced out
     * so it reads as a heading at 8px where bold would only read as noise, then
     * a hairline running to the far edge.
     */
    public static void heading(GuiGraphicsExtractor g, Font font, String title, int x, int y, int w, int accentArgb) {
        int used = headingCaption(g, font, title, x, y, w, accentArgb);
        headingRule(g, x + used + 5, y + 3, x + w, accentArgb);
    }

    /**
     * The caption half of a heading on its own, for the decorated headings that
     * have to fit an icon, a count badge and a disclosure chevron around it.
     *
     * @return the width the caption took
     */
    public static int headingCaption(GuiGraphicsExtractor g, Font font, String title,
                                     int x, int y, int w, int accentArgb) {
        if (title.isEmpty() || w <= 0) return 0;
        String caption = spaced(title.toUpperCase(Locale.ROOT));
        int textW = Math.min(font.width(caption), w);
        g.text(font, font.plainSubstrByWidth(caption, w), x, y, accentArgb, false);
        return textW;
    }

    public static void headingRule(GuiGraphicsExtractor g, int x, int y, int rightX, int accentArgb) {
        if (x < rightX) g.fill(x, y, rightX, y + 1, withAlpha(accentArgb, 0.35f));
    }

    /**
     * One space between letters — the small-caps trick, since the vanilla font
     * has no small caps and no letter-spacing.
     */
    private static String spaced(String text) {
        StringBuilder out = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) out.append(' ');
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    public static void hairline(GuiGraphicsExtractor g, int x, int y, int w) {
        if (w <= 0) return;
        g.fill(x, y, x + w, y + 1, RULE);
    }

    /**
     * A titled separator. Quieter than {@link #heading} on purpose: a divider
     * names a break inside a section, and a second small-caps accent caption
     * would read as a second section.
     */
    public static void labelledRule(GuiGraphicsExtractor g, Font font, String label, int x, int y, int w) {
        String text = font.plainSubstrByWidth(label, w);
        g.text(font, text, x, y, CoiStyle.TEXT_MUTED, false);
        int ruleX = x + font.width(text) + 5;
        hairline(g, ruleX, y + 4, x + w - ruleX);
    }

    /**
     * A rounded-ish panel: a filled box with its four corner pixels knocked out,
     * which is as much rounding as reads honestly at this scale.
     */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        if (w <= 0 || h <= 0) return;
        g.fill(x + 1, y, x + w - 1, y + h, fill);
        g.fill(x, y + 1, x + 1, y + h - 1, fill);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, fill);
        if (border != 0) {
            g.fill(x + 1, y, x + w - 1, y + 1, border);
            g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
            g.fill(x, y + 1, x + 1, y + h - 1, border);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        }
    }

    /**
     * Draws a button and answers nothing — the caller already knows where it
     * put it. A disabled button is drawn, not hidden: the reason it is disabled
     * is the information the player came for, and the caller shows it as a
     * tooltip.
     */
    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                              MenuComponent.Button button, int accentArgb, boolean hovered, boolean pressed) {
        boolean enabled = button.enabled();
        int fill;
        int border;
        int label;
        switch (button.style()) {
            case PRIMARY -> {
                fill = withAlpha(accentArgb, 0.85f);
                border = accentArgb;
                label = ON_ACCENT;
            }
            case DANGER -> {
                fill = 0xCC7A2626;
                border = DANGER;
                label = 0xFFFFE4E4;
            }
            case SUCCESS -> {
                fill = 0xCC1F5C33;
                border = SUCCESS;
                label = 0xFFE4FFEA;
            }
            case GHOST -> {
                fill = hovered ? SURFACE : 0;
                border = hovered ? CoiStyle.BORDER : 0;
                label = hovered ? CoiStyle.TEXT_BODY : CoiStyle.TEXT_MUTED;
            }
            case SECONDARY -> {
                fill = SURFACE;
                border = CoiStyle.BORDER;
                label = CoiStyle.TEXT_BODY;
            }
            default -> {
                fill = SURFACE;
                border = CoiStyle.BORDER;
                label = CoiStyle.TEXT_BODY;
            }
        }

        if (!enabled) {
            fill = 0x14FFFFFF;
            border = BORDER_OFF;
            label = CoiStyle.INACTIVE;
        } else if (pressed) {
            fill = shade(fill, -0.25f);
        } else if (hovered) {
            fill = shade(fill, 0.14f);
            border = shade(border, 0.2f);
        }

        panel(g, x, y, w, h, fill, border);

        int textY = y + (button.desc().isEmpty() ? (h - 8) / 2 : 4) + (enabled && pressed ? 1 : 0);
        int inset = 6;
        int iconW = 0;
        if (button.icon().present()) {
            MenuIcons.draw(g, font, button.icon(), x + inset, y + (h - 12) / 2, 12, enabled ? 1f : 0.4f);
            iconW = 15;
        }

        int textX = x + inset + iconW;
        int textW = w - inset * 2 - iconW;
        if (button.desc().isEmpty() && iconW == 0) {
            String text = font.plainSubstrByWidth(button.label(), textW);
            g.text(font, text, x + (w - font.width(text)) / 2, textY, label, false);
        } else {
            g.text(font, font.plainSubstrByWidth(button.label(), textW), textX, textY, label, false);
            if (!button.desc().isEmpty()) {
                int descColor = enabled ? CoiStyle.TEXT_MUTED : shade(CoiStyle.INACTIVE, -0.2f);
                g.text(font, font.plainSubstrByWidth(button.desc(), textW), textX, textY + 10, descColor, false);
            }
        }
    }

    /**
     * A pill-shaped tag, right-aligned on {@code rightX}.
     *
     * @return the width it took, so a caller can reserve the space
     */
    public static int badge(GuiGraphicsExtractor g, Font font, String text, int rightX, int y, int rgb, int accentArgb) {
        if (text.isEmpty()) return 0;
        int color = argb(rgb, accentArgb);
        int textW = font.width(text);
        int w = textW + 8;
        int x = rightX - w;
        panel(g, x, y, w, CHIP_H, withAlpha(color, 0.18f), withAlpha(color, 0.55f));
        g.text(font, text, x + 4, chipTextY(y), color, false);
        return w;
    }

    /**
     * The switch in a {@code toggle} row: a track with a knob at one end. The
     * accent-filled side is the one that is on, so the state is readable
     * without the word beside it.
     */
    public static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on, boolean enabled, int accentArgb) {
        int trackW = 20;
        int trackH = 10;
        int track = !enabled ? 0x18FFFFFF : on ? withAlpha(accentArgb, 0.45f) : 0x22FFFFFF;
        int border = !enabled ? BORDER_OFF : on ? accentArgb : CoiStyle.BORDER;
        panel(g, x, y, trackW, trackH, track, border);
        int knobX = on ? x + trackW - 9 : x + 1;
        int knob = !enabled ? CoiStyle.INACTIVE : on ? accentArgb : CoiStyle.TEXT_MUTED;
        g.fill(knobX, y + 1, knobX + 8, y + trackH - 1, knob);
    }

    /**
     * A drawn tick, cross or hollow ring — unicode ✔/✘ are unreliable in the
     * vanilla font, and these stay sharp at any size. {@code PENDING} is a ring
     * rather than a dimmed cross: "not yet" must not look like "failed".
     *
     * @param rgb the server's override, or 0 for the state's own colour
     */
    public static void check(GuiGraphicsExtractor g, int x, int y, MenuComponent.CheckState state, int rgb) {
        int color = argb(rgb, checkColor(state));
        switch (state) {
            case OK -> {
                EffectPaint.line(g, x + 1, y + 4, x + 3, y + 6, color, 1);
                EffectPaint.line(g, x + 3, y + 6, x + 7, y + 1, color, 1);
            }
            case NO -> {
                EffectPaint.line(g, x + 1, y + 1, x + 7, y + 6, color, 1);
                EffectPaint.line(g, x + 1, y + 6, x + 7, y + 1, color, 1);
            }
            case PENDING -> panel(g, x + 1, y, 7, 7, 0, color);
        }
    }

    /**
     * What a checklist item's label is coloured when the server named nothing.
     */
    public static int checkColor(MenuComponent.CheckState state) {
        return switch (state) {
            case OK -> SUCCESS;
            case NO -> DANGER;
            case PENDING -> CoiStyle.TEXT_MUTED;
        };
    }

    /**
     * A met requirement's label is ordinary body text; only the ones that still
     * want something are coloured, so a long checklist is not a wall of green.
     */
    public static int checkLabelColor(MenuComponent.CheckState state) {
        return state == MenuComponent.CheckState.OK ? CoiStyle.TEXT_BODY : checkColor(state);
    }

    /**
     * A left-pointing chevron for the back control, drawn rather than typed for
     * the same reason as {@link #check}.
     */
    public static void chevron(GuiGraphicsExtractor g, int x, int y, int color) {
        EffectPaint.line(g, x + 4, y, x, y + 4, color, 1);
        EffectPaint.line(g, x, y + 4, x + 4, y + 8, color, 1);
    }

    public static void cross(GuiGraphicsExtractor g, int x, int y, int color) {
        EffectPaint.line(g, x, y, x + 7, y + 7, color, 1);
        EffectPaint.line(g, x, y + 7, x + 7, y, color, 1);
    }

    /**
     * The disclosure marker of a {@code details} row or a collapsible heading.
     * {@code openT} rotates it from pointing right (closed) to down (open), so
     * the state is legible mid-animation as well as at rest.
     */
    public static void disclosure(GuiGraphicsExtractor g, int x, int y, float openT, int color) {
        double angle = Math.clamp(openT, 0f, 1f) * (Math.PI / 2);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        float cx = x + 3f;
        float cy = y + 3.5f;
        // Vertex and both arms rotate together about the glyph's centre;
        // rotating only the arms would swing the caret open around the wrong point
        float vx = (float) (cx + 2.5 * cos);
        float vy = (float) (cy + 2.5 * sin);
        for (int arm = -1; arm <= 1; arm += 2) {
            double ax = -2.5;
            double ay = arm * 3.0;
            EffectPaint.line(g, vx, vy,
                    (float) (cx + ax * cos - ay * sin), (float) (cy + ax * sin + ay * cos), color, 1);
        }
    }

    /**
     * A step rail's marker: a numbered disc, or a dot when the caller passes no
     * number. Filled means done, hollow means not, and a muted hollow means the
     * server said nothing — which is "not yet", not "failed".
     */
    public static void stepMarker(GuiGraphicsExtractor g, Font font, int centreX, int centreY,
                                  String number, Boolean done, int accentArgb) {
        boolean filled = Boolean.TRUE.equals(done);
        int color = done == null ? CoiStyle.TEXT_MUTED
                : filled ? accentArgb : withAlpha(accentArgb, 0.55f);
        if (number.isEmpty()) {
            if (filled) {
                g.fill(centreX - 2, centreY - 2, centreX + 3, centreY + 3, color);
            } else {
                panel(g, centreX - 3, centreY - 3, 7, 7, CoiStyle.CARD_BG, color);
            }
            return;
        }
        panel(g, centreX - 5, centreY - 5, 11, 11,
                filled ? withAlpha(color, 0.85f) : CoiStyle.CARD_BG, color);
        int textW = font.width(number);
        g.text(font, number, centreX - textW / 2, centreY - 3, filled ? ON_ACCENT : color, false);
    }

    /**
     * The vertical hairline a step rail draws behind its markers.
     */
    public static void rail(GuiGraphicsExtractor g, int x, int top, int bottom, int accentArgb) {
        if (bottom <= top) return;
        g.fill(x, top, x + 1, bottom, withAlpha(accentArgb, 0.25f));
    }

    /**
     * How wide {@link #chip} will draw — measured rather than guessed, because
     * a wrapping row has to know before it commits to a line.
     */
    public static int chipWidth(Font font, MenuComponent.Chip chip) {
        int w = 8 + font.width(chip.label());
        if (chip.icon().present()) w += 12;
        if (!chip.value().isEmpty()) w += 4 + font.width(chip.value());
        return w;
    }

    /**
     * A small fact as a pill: the label muted, the value in its own colour. The
     * pair is what makes a chip say more than a badge.
     */
    public static void chip(GuiGraphicsExtractor g, Font font, MenuComponent.Chip chip,
                            int x, int y, int accentArgb, float hoverT) {
        int color = argb(chip.rgb(), accentArgb);
        int w = chipWidth(font, chip);
        panel(g, x, y, w, CHIP_H, surface(hoverT), withAlpha(color, 0.35f));
        int textX = x + 4;
        int textY = chipTextY(y);
        if (chip.icon().present()) {
            MenuIcons.draw(g, font, chip.icon(), textX, y + (CHIP_H - 9) / 2, 9, 1f);
            textX += 12;
        }
        g.text(font, chip.label(), textX, textY, CoiStyle.TEXT_MUTED, false);
        textX += font.width(chip.label());
        if (!chip.value().isEmpty()) {
            g.text(font, chip.value(), textX + 4, textY, color, false);
        }
    }

    /**
     * Text at a scale the vanilla font does not have. Everything inside keeps
     * its own coordinate space, so the caller only has to know where the text
     * starts — the same bargain {@code HudScale} makes for the HUD.
     */
    public static void scaledText(GuiGraphicsExtractor g, Font font, String text,
                                  int x, int y, float scale, int color, boolean shadow) {
        if (text.isEmpty()) return;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(font, text, 0, 0, color, shadow);
        pose.popMatrix();
    }

    /**
     * The scroll bar's two layers: the track, and the handle in the document's
     * accent, a touch brighter while it is grabbed. The menu card and the
     * character sheet draw the same bar, so the recipe lives here and each
     * screen keeps its own geometry.
     */
    public static void scrollbar(GuiGraphicsExtractor g, int x, int top, int trackH,
                                 int handleY, int handleH, int width, int accentArgb, boolean grabbed) {
        g.fill(x, top, x + width, top + trackH, CoiStyle.SCROLL_TRACK);
        g.fill(x, handleY, x + width, handleY + handleH, withAlpha(accentArgb, grabbed ? 0.85f : 0.55f));
    }

    /**
     * The chrome behind a text field. The vanilla {@code EditBox} draws a black
     * fill inside a white border, which on this card reads as a hole punched
     * through it — so the box is rendered borderless on top of this instead.
     */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h,
                             boolean focused, int accentArgb) {
        panel(g, x, y, w, h, focused ? SURFACE_HOVER : SURFACE,
                focused ? accentArgb : CoiStyle.BORDER);
    }

}
