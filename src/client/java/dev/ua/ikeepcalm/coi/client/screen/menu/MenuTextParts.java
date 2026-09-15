package dev.ua.ikeepcalm.coi.client.screen.menu;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.RAIL_TEXT_X;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.RAIL_X;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.SMALL_ICON;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The parts that are words: a section's caption, a paragraph, a callout, the
 * disclosure that hides a paragraph until it is asked for, a numbered rail, and
 * the three that are structure rather than text — a message, a rule, a gap.
 */
final class MenuTextParts {

    private MenuTextParts() {
    }

    /**
     * A section caption. A heading the server decorated carries an icon, a count
     * badge and — when it named {@code collapsed} — a chevron that folds the
     * whole section away.
     */
    static final class HeadingPart extends MenuPart {
        private final String title;
        private final MenuComponent.Heading heading;

        HeadingPart(MenuContext ctx, String title) {
            super(ctx);
            this.title = title;
            this.heading = null;
            this.height = 15;
        }

        HeadingPart(MenuContext ctx, MenuComponent.Heading heading) {
            super(ctx);
            this.title = heading.title();
            this.heading = heading;
            this.height = 15;
        }

        private boolean collapsible() {
            return heading != null && heading.collapsible();
        }

        private boolean open() {
            return !collapsible() || ctx.disclosed(heading.key(), !heading.collapsed());
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (heading == null) {
                MenuTheme.heading(g, font, title, x, top + 3, ctx.contentW(), ctx.accent());
                return;
            }
            int y = top + 3;
            int left = x;
            int right = x + ctx.contentW();
            boolean hovered = collapsible() && MenuMetrics.inBox(mouseX, mouseY, x, top, ctx.contentW(), height);
            int color = MenuTheme.lerpArgb(ctx.accent(), MenuTheme.shade(ctx.accent(), 0.3f), hover(hovered));

            if (collapsible()) {
                MenuTheme.disclosure(g, left, y, ctx.chevron(heading.key(), open()), color);
                left += 10;
            }
            if (MenuIcons.draw(g, font, heading.icon(), left, y - 2, SMALL_ICON, 1f)) {
                left += SMALL_ICON + 3;
            }
            int badgeW = heading.badge().isEmpty() ? 0
                    : MenuTheme.badge(g, font, heading.badge(), right, top + 1,
                    heading.badgeRgb(), ctx.accent()) + 5;
            int capW = MenuTheme.headingCaption(g, font, title, left, y,
                    Math.max(10, right - badgeW - left), color);
            MenuTheme.headingRule(g, left + capW + 5, y + 3, right - badgeW, ctx.accent());
            if (hovered) ctx.disclosureHint(open());
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (collapsible()) ctx.toggleDisclosure(heading.key(), !heading.collapsed());
            return true;
        }
    }

    static final class TextPart extends MenuPart {
        private final List<FormattedCharSequence> lines;
        private final int color;
        private final MenuComponent.Align align;

        TextPart(MenuContext ctx, MenuComponent.Text text) {
            super(ctx);
            this.lines = font.split(Component.literal(text.text()), ctx.contentW());
            this.color = MenuTheme.textColor(text.style(), ctx.accent());
            this.align = text.align();
            this.height = lines.size() * 9 + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int y = top + 1;
            for (FormattedCharSequence line : lines) {
                int lineX = switch (align) {
                    case LEFT -> x;
                    case CENTER -> x + (ctx.contentW() - font.width(line)) / 2;
                    case RIGHT -> x + ctx.contentW() - font.width(line);
                };
                g.text(font, line, lineX, y, color, false);
                y += 9;
            }
        }
    }

    static final class NotePart extends MenuPart {
        private final MenuComponent.Note note;
        private final List<FormattedCharSequence> lines;
        private final int color;
        private final int indent;

        NotePart(MenuContext ctx, MenuComponent.Note note) {
            super(ctx);
            this.note = note;
            this.color = MenuTheme.textColor(note.style(), ctx.accent());
            // An icon indents the whole block, not only the line beside it: a
            // paragraph that steps back under its own symbol reads as ragged
            this.indent = note.icon().present() ? 9 + SMALL_ICON + 3 : 9;
            this.lines = font.split(Component.literal(note.text()),
                    Math.max(20, ctx.contentW() - indent - 9));
            this.height = 6 + (note.title().isEmpty() ? 0 : 11) + lines.size() * 9 + 6 + 4;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int h = height - 4;
            int textW = Math.max(20, ctx.contentW() - indent - 9);
            g.fill(x, top, x + ctx.contentW(), top + h, MenuTheme.withAlpha(color, 0.10f));
            g.fill(x, top, x + 2, top + h, color);
            int y = top + 5;
            MenuIcons.draw(g, font, note.icon(), x + 9, y - 2, SMALL_ICON, 1f);
            if (!note.title().isEmpty()) {
                g.text(font,
                        font.plainSubstrByWidth(note.title(), textW), x + indent, y, color, true);
                y += 11;
            }
            for (FormattedCharSequence line : lines) {
                g.text(font, line, x + indent, y, CoiStyle.TEXT_BODY, false);
                y += 9;
            }
        }
    }

    /**
     * A disclosure row: one line of summary, and the paragraph behind it only
     * when the player asks. This is the component every wall of prose in the
     * system is supposed to move into.
     */
    static final class DetailsPart extends MenuPart {
        private static final int BODY_X = 10;

        private final MenuComponent.Details details;
        private final boolean open;
        private final int bodyColor;
        private final List<List<FormattedCharSequence>> blocks = new ArrayList<>();

        DetailsPart(MenuContext ctx, MenuComponent.Details details) {
            super(ctx);
            this.details = details;
            this.open = ctx.disclosed(details.id(), details.open());
            this.bodyColor = MenuTheme.textColor(details.style(), ctx.accent());
            int h = 16;
            if (open) {
                int bodyW = Math.max(20, ctx.contentW() - BODY_X);
                for (String block : details.text()) {
                    List<FormattedCharSequence> lines =
                            font.split(Component.literal(block), bodyW);
                    blocks.add(lines);
                    h += lines.size() * 9 + 4;
                }
            }
            this.height = h + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, x, top, w, 16);
            int color = MenuTheme.lerpArgb(CoiStyle.TEXT_BODY, ctx.accent(), hover(hovered));

            MenuTheme.disclosure(g, x, top + 4, ctx.chevron(details.id(), open), color);
            int left = x + BODY_X;
            if (MenuIcons.draw(g, font, details.icon(), left, top + 1, SMALL_ICON, 1f)) {
                left += SMALL_ICON + 3;
            }
            String summary = font.plainSubstrByWidth(details.summary(),
                    Math.max(10, x + w - left - 10));
            g.text(font, summary, left, top + 3, color, false);
            // The hairline runs from the end of the summary to the far edge, so
            // a collapsed row still reads as a full-width control
            MenuTheme.hairline(g, left + font.width(summary) + 5, top + 7,
                    Math.max(0, x + w - left - font.width(summary) - 5));

            if (hovered) ctx.disclosureHint(open);

            int y = top + 16;
            for (List<FormattedCharSequence> block : blocks) {
                for (FormattedCharSequence line : block) {
                    g.text(font, line, x + BODY_X, y, bodyColor, false);
                    y += 9;
                }
                y += 4;
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            // Only the summary row toggles; a click in the open body is not a
            // request to close what the player is reading
            if (my < top + 16) ctx.toggleDisclosure(details.id(), details.open());
            return true;
        }
    }

    /**
     * A numbered rail. Six consecutive {@code text} components explaining a
     * sequence become one of these.
     */
    static final class StepsPart extends MenuPart {
        private final MenuComponent.Steps steps;
        private final List<List<FormattedCharSequence>> texts = new ArrayList<>();
        private final int[] itemH;

        StepsPart(MenuContext ctx, MenuComponent.Steps steps) {
            super(ctx);
            this.steps = steps;
            this.itemH = new int[steps.items().size()];
            int h = 2;
            for (int i = 0; i < steps.items().size(); i++) {
                MenuComponent.Step item = steps.items().get(i);
                List<FormattedCharSequence> lines = item.text().isEmpty() ? List.of()
                        : font.split(Component.literal(item.text()),
                        Math.max(20, ctx.contentW() - RAIL_TEXT_X));
                texts.add(lines);
                itemH[i] = 13 + lines.size() * 9 + 4;
                h += itemH[i];
            }
            this.height = h;
        }

        private int itemTop(int top, int index) {
            int y = top + 1;
            for (int i = 0; i < index; i++) y += itemH[i];
            return y;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int count = steps.items().size();
            if (count > 1) {
                MenuTheme.rail(g, x + RAIL_X, itemTop(top, 0) + 6,
                        itemTop(top, count - 1) + 6, ctx.accent());
            }
            for (int i = 0; i < count; i++) {
                MenuComponent.Step item = steps.items().get(i);
                int y = itemTop(top, i);
                // An icon replaces the marker outright — a numbered disc beside a
                // symbol is two answers to the same question
                if (!MenuIcons.draw(g, font, item.icon(), x, y, SMALL_ICON, 1f)) {
                    String number = steps.style() == MenuComponent.StepStyle.NUMBERED
                            ? String.valueOf(i + 1) : "";
                    MenuTheme.stepMarker(g, font, x + RAIL_X, y + 6, number,
                            item.done(), ctx.accent());
                }
                g.text(font,
                        font.plainSubstrByWidth(item.title(), ctx.contentW() - RAIL_TEXT_X),
                        x + RAIL_TEXT_X, y + 2, CoiStyle.TEXT_BODY, false);
                int lineY = y + 13;
                for (FormattedCharSequence line : texts.get(i)) {
                    g.text(font, line, x + RAIL_TEXT_X, lineY, CoiStyle.TEXT_MUTED, false);
                    lineY += 9;
                }
            }
        }
    }

    static final class MessagePart extends MenuPart {
        private final String text;

        MessagePart(MenuContext ctx, String text) {
            super(ctx);
            this.text = text;
            this.height = 20;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            String line = font.plainSubstrByWidth(text, ctx.contentW());
            g.text(font, line, x + (ctx.contentW() - font.width(line)) / 2,
                    top + 6, CoiStyle.TEXT_MUTED, false);
        }
    }

    static final class DividerPart extends MenuPart {
        private final String label;

        DividerPart(MenuContext ctx, String label) {
            super(ctx);
            this.label = label;
            this.height = label.isEmpty() ? 9 : 13;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (label.isEmpty()) {
                MenuTheme.hairline(g, x, top + 4, ctx.contentW());
            } else {
                MenuTheme.labelledRule(g, font, label, x, top + 2, ctx.contentW());
            }
        }
    }

    static final class SpacerPart extends MenuPart {
        SpacerPart(MenuContext ctx, int size) {
            super(ctx);
            this.height = size;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
        }
    }

}
