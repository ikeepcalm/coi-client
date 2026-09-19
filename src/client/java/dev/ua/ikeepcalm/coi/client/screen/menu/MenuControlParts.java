package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.*;

/**
 * The parts the player operates: buttons, a switch, the live search box a
 * searchable list grows, and a text field with its own submit.
 */
public class MenuControlParts {

    private MenuControlParts() {
    }

    public static class ButtonsPart extends MenuPart {
        private final List<MenuComponent.Button> buttons;
        private final int columns;
        private final int rowH;
        private final int rows;

        ButtonsPart(MenuContext ctx, List<MenuComponent.Button> buttons, int columns) {
            super(ctx);
            this.buttons = buttons;
            this.columns = Math.max(1, columns);
            this.rowH = ctx.rowHeightFor(buttons);
            this.rows = (buttons.size() + this.columns - 1) / this.columns;
            this.height = rows * (rowH + GAP) + 2;
        }

        @Override
        int growthCapacity() {
            return rows * 12;
        }

        private int expandedRowH() {
            return rows == 0 ? rowH : (height - 2) / rows - GAP;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            ctx.drawButtons(g, buttons, x, top + 1, ctx.contentW(), columns, expandedRowH(), mouseX, mouseY);
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            int index = ctx.hitButton(mx, my, buttons.size(), x, top + 1, ctx.contentW(), columns, expandedRowH());
            if (index >= 0) ctx.activate(buttons.get(index), null);
            return true;
        }
    }

    public static class TogglePart extends MenuPart {
        private final MenuComponent.Toggle toggle;

        @Override
        int growthCapacity() {
            return 16;
        }

        TogglePart(MenuContext ctx, MenuComponent.Toggle toggle) {
            super(ctx);
            this.toggle = toggle;
            this.height = (toggle.desc().isEmpty() ? 18 : 28) + 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            int w = ctx.contentW();
            int h = height - 3;
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, x, top, w, h);
            MenuTheme.panel(g, x, top, w, h, MenuTheme.surface(hover(hovered && toggle.enabled())), 0);

            int switchX = x + w - 26;
            MenuTheme.toggle(g, switchX, top + (h - 10) / 2, toggle.on(), toggle.enabled(), ctx.accent());

            String state = stateText();
            int stateW = font.width(state);
            int stateColor = !toggle.enabled() ? CoiStyle.INACTIVE
                    : toggle.on() ? ctx.accent() : CoiStyle.TEXT_MUTED;
            g.text(font, state, switchX - stateW - 6, top + (h - 8) / 2, stateColor, false);

            int labelX = x + 6;
            int labelY = top + (h - (toggle.desc().isEmpty() ? 8 : 19)) / 2;
            if (MenuIcons.draw(g, font, toggle.icon(), labelX, labelY - 2,
                    SMALL_ICON, toggle.enabled() ? 1f : 0.4f)) {
                labelX += SMALL_ICON + 3;
            }
            int labelW = Math.max(10, w - 38 - stateW - (labelX - x - 6));
            int labelColor = toggle.enabled() ? CoiStyle.TEXT_BODY : CoiStyle.INACTIVE;
            g.text(font, font.plainSubstrByWidth(toggle.label(), labelW),
                    labelX, labelY, labelColor, false);
            if (!toggle.desc().isEmpty()) {
                g.text(font, font.plainSubstrByWidth(toggle.desc(), labelW),
                        labelX, labelY + 11, CoiStyle.TEXT_MUTED, false);
            }
            if (hovered && !toggle.enabled()) ctx.reason(toggle.disabledReason());
        }

        private String stateText() {
            if (toggle.on()) {
                return toggle.onText().isEmpty()
                        ? Component.translatable("screen.coi.menu_on").getString() : toggle.onText();
            }
            return toggle.offText().isEmpty()
                    ? Component.translatable("screen.coi.menu_off").getString() : toggle.offText();
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            if (toggle.enabled()) ctx.fire(toggle.id(), null);
            return true;
        }
    }

    public static class SearchPart extends MenuPart {
        private final EditBox box;

        SearchPart(MenuContext ctx, MenuComponent.ListView list) {
            super(ctx);
            this.box = ctx.field("search:" + list.id(), () -> {
                EditBox created = new EditBox(font, 0, 0, 100, FIELD_H,
                        Component.translatable("screen.coi.menu_search"));
                created.setMaxLength(48);
                created.setBordered(false);
                created.setHint(Component.translatable("screen.coi.menu_search")
                        .withStyle(ChatFormatting.DARK_GRAY));
                // Relaying out on every keystroke is what makes the filter live.
                // The box itself outlives the layout, so the caret survives it.
                created.setResponder(ignored -> ctx.relayout());
                return created;
            });
            this.height = 22;
        }

        /** The live filter: whatever the player has typed, normalised for matching. */
        String query() {
            return box.getValue().trim().toLowerCase(Locale.ROOT);
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            ctx.drawField(g, box, x, top, ctx.contentW(), mouseX, mouseY);
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            ctx.focusField(box);
            return true;
        }
    }

    public static class InputPart extends MenuPart {
        private final MenuComponent.Input input;
        private final EditBox box;
        private final MenuComponent.Button submit;
        private final int labelH;
        private final int submitW;

        InputPart(MenuContext ctx, MenuComponent.Input input) {
            super(ctx);
            this.input = input;
            this.labelH = input.label().isEmpty() ? 0 : 11;
            String label = input.submitLabel().isEmpty()
                    ? Component.translatable("screen.coi.menu_submit").getString() : input.submitLabel();
            this.submit = new MenuComponent.Button(input.submit(), label, "",
                    MenuComponent.ButtonStyle.PRIMARY, !input.submit().isEmpty(), "", null, MenuIcon.NONE);
            this.submitW = Math.min(ctx.contentW() / 2, font.width(label) + 16);

            this.box = ctx.field("input:" + input.id(), () -> {
                EditBox created = new EditBox(font, 0, 0, 100, FIELD_H,
                        Component.literal(input.label()));
                created.setMaxLength(input.maxLength());
                created.setBordered(false);
                if (!input.placeholder().isEmpty()) {
                    created.setHint(Component.literal(input.placeholder()).withStyle(ChatFormatting.DARK_GRAY));
                }
                created.setValue(input.value());
                return created;
            });

            this.height = labelH + FIELD_H + (input.hint().isEmpty() ? 0 : 10) + 5;
        }

        @Override
        void render(GuiGraphicsExtractor g, int x, int top, int mouseX, int mouseY) {
            if (labelH > 0) {
                g.text(font,
                        font.plainSubstrByWidth(input.label(), ctx.contentW()),
                        x, top + 1, CoiStyle.TEXT_BODY, false);
            }
            int fieldY = top + labelH;
            ctx.drawField(g, box, x, fieldY, ctx.contentW() - submitW - GAP, mouseX, mouseY);

            int bx = x + ctx.contentW() - submitW;
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, bx, fieldY, submitW, FIELD_H);
            MenuTheme.button(g, font, bx, fieldY, submitW, FIELD_H, submit, ctx.accent(), hovered, false);

            if (!input.hint().isEmpty()) {
                g.text(font,
                        font.plainSubstrByWidth(input.hint(), ctx.contentW()),
                        x, fieldY + FIELD_H + 2, CoiStyle.TEXT_MUTED, false);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int top) {
            int fieldY = top + labelH;
            if (MenuMetrics.inBox(mx, my, x + ctx.contentW() - submitW, fieldY, submitW, FIELD_H)) {
                if (submit.enabled()) ctx.fire(input.submit(), box.getValue());
                return true;
            }
            if (MenuMetrics.inBox(mx, my, x, fieldY, ctx.contentW() - submitW - GAP, FIELD_H)) {
                ctx.focusField(box);
            }
            return true;
        }
    }

}
