package dev.ua.ikeepcalm.coi.client.screen.settings;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

/**
 * Builds the rows of {@link HudSettingsScreen}'s scrollable card, one after
 * another down a running cursor.
 * <p>
 * Every row is registered with the screen <em>and</em> remembered here with its
 * Y offset from the viewport top, so the screen can reposition and hide the lot
 * on a scroll without knowing what any of them are.
 */
final class SettingsRows {

    static final int ROW_STRIDE = 26;
    private static final int FIELD_WIDTH = 52;
    private static final int ALIGN_W = 60;
    /**
     * Left inset of a normal row, and of a row that belongs to the element
     * above it.
     */
    static final int INDENT = 10;
    static final int SUB_INDENT = 22;

    /** A tighter stride than a real row, so a hint reads as a caption. */
    private static final int HINT_STRIDE = 18;

    /**
     * A widget inside the scrollable card with its Y offset from the viewport top.
     */
    record ContentWidget(AbstractWidget widget, int baseY) {
    }

    private final Font font;
    private final int contentX;
    private final int contentW;
    private final Consumer<AbstractWidget> register;
    /** Re-runs the screen's {@code init()}, for rows that change which rows exist. */
    private final Runnable rebuild;
    private final Consumer<String> openLayout;

    private final List<ContentWidget> widgets = new ArrayList<>();
    private int cursor;

    SettingsRows(Font font, int contentX, int contentW,
                 Consumer<AbstractWidget> register, Runnable rebuild, Consumer<String> openLayout) {
        this.font = font;
        this.contentX = contentX;
        this.contentW = contentW;
        this.register = register;
        this.rebuild = rebuild;
        this.openLayout = openLayout;
    }

    List<ContentWidget> widgets() {
        return widgets;
    }

    /** Total height of everything built so far. */
    int cursor() {
        return cursor;
    }

    void contentRow(AbstractWidget widget) {
        widgets.add(new ContentWidget(widget, cursor));
        register.accept(widget);
        cursor += ROW_STRIDE;
    }

    void headerRow(Component label) {
        contentRow(new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 20, label, font));
    }

    /**
     * Section header with an Align button that opens the layout editor on this
     * element — the ability slots' only positional control.
     */
    void headerRow(Component label, String elementId) {
        StringWidget title = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2 - ALIGN_W - 4, 20, label, font);
        widgets.add(new ContentWidget(title, cursor));
        register.accept(title);
        contentRow(alignButton(elementId));
    }

    /**
     * One line of explanatory text above a tab's rows, on a tighter stride than
     * a real row so it reads as a caption rather than a setting.
     */
    void hintRow(Component label) {
        StringWidget hint = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 14,
                label.copy().withStyle(ChatFormatting.GRAY), font);
        widgets.add(new ContentWidget(hint, cursor));
        register.accept(hint);
        cursor += HINT_STRIDE;
    }

    /**
     * A bar/overlay's header line: the show/hide checkbox is the element's
     * name, so one row carries both what it is and whether it draws.
     * <p>
     * Toggling rebuilds the tab, since the rows below this one appear and
     * disappear with it.
     *
     * @return whether the element is on, i.e. whether its own rows follow
     */
    boolean elementRow(String elementId, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(Component.translatable("screen.coi.layout_el_" + elementId), font)
                .pos(contentX + INDENT, 0)
                .maxWidth(contentW - INDENT * 2 - ALIGN_W - 4)
                .onValueChange((box, checked) -> {
                    setter.accept(checked);
                    rebuild.run();
                })
                .selected(selected)
                .build();
        widgets.add(new ContentWidget(checkbox, cursor));
        register.accept(checkbox);
        contentRow(alignButton(elementId));
        return selected;
    }

    private Button alignButton(String elementId) {
        return Button.builder(Component.translatable("screen.coi.layout_align"), b -> openLayout.accept(elementId))
                .bounds(contentX + contentW - INDENT - ALIGN_W, 0, ALIGN_W, 20).build();
    }

    void scaleRow(float current, Consumer<Float> setter) {
        decimalRow(SUB_INDENT, Component.translatable("screen.coi.element_scale"),
                Component.translatable("screen.coi.element_scale_field"),
                HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE, current,
                value -> setter.accept((float) value));
    }

    void checkboxRow(int indent, Component label, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(label, font)
                .pos(contentX + indent, 0)
                .maxWidth(contentW - indent - INDENT)
                .onValueChange((box, checked) -> setter.accept(checked))
                .selected(selected)
                .build();
        contentRow(checkbox);
    }

    void intRow(int indent, Component label, Component fieldLabel, int min, int max, int initialValue, IntConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        int clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (double) (max - min);
        int sliderW = sliderWidth(indent);

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
                label.copy().append(": " + clampedInitial), sliderValue) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                this.setMessage(label.copy().append(": " + value));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.valueOf(value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.valueOf(clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Integer.parseInt(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        widgets.add(new ContentWidget(slider, cursor));
        register.accept(slider);
        contentRow(field); // advances the cursor for the pair
    }

    void decimalRow(int indent, Component label, Component fieldLabel, double min, double max,
                    double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);
        int sliderW = sliderWidth(indent);

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
                label.copy().append(": " + String.format("%.1f", clampedInitial)), sliderValue) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                value = Math.round(value * 10.0) / 10.0;
                setter.accept(value);
                this.setMessage(label.copy().append(": " + String.format("%.1f", value)));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.format("%.1f", value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.format("%.1f", clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Double.parseDouble(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        widgets.add(new ContentWidget(slider, cursor));
        register.accept(slider);
        contentRow(field);
    }

    /** A slider and its number field share a row; the field takes the right edge. */
    private int sliderWidth(int indent) {
        return contentW - indent - INDENT - FIELD_WIDTH - 6;
    }

    int contentX() {
        return contentX;
    }

    int contentW() {
        return contentW;
    }
}
