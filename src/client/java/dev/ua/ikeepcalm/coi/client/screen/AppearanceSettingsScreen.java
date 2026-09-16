package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraits;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.util.CoiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.NonNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Appearance controls using the same cards, tabs and input patterns as HUD settings. */
public final class AppearanceSettingsScreen extends Screen {
    private static final int ROW = 26;
    private enum Tab {VISIBILITY, FIT, UNIQUENESS}
    private record RowWidget(AbstractWidget widget, int y, int labelHeight) {}
    private record Heading(String key, int y, int color, int groupHeight) {}
    private final Screen parent;
    private final AppearanceConfig.Settings original = AppearanceConfig.copySettings();
    private final List<RowWidget> rows = new ArrayList<>();
    private final List<Heading> headings = new ArrayList<>();
    private final List<NumberSlider> numbers = new ArrayList<>();
    private Tab tab = Tab.FIT;
    private int panelX, panelW, cardTop, previewLeft, previewWidth, previewTop, previewBottom;
    private int contentX, contentW, viewportTop, viewportBottom, rowY, footerY;
    private double scroll;
    private float previewZoom = 1, previewYaw, previewPitch;
    private boolean draggingPreview, draggingScroll, committed;
    private Button done;
    private PreviewZoom zoom;

    public AppearanceSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.appearance_settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        draggingPreview = draggingScroll = false;
        numbers.forEach(NumberSlider::revertInvalid);
        clearWidgets(); rows.clear(); headings.clear(); numbers.clear(); rowY = 0; done = null;
        panelW = Math.min(600, width - 16);
        panelX = (width - panelW) / 2;
        int tabY = height < 260 ? 18 : 30;
        cardTop = tabY + 28;
        footerY = height - 28;
        previewLeft = panelX;
        previewWidth = Math.min(200, panelW / 3);
        contentX = previewLeft + previewWidth + 10;
        contentW = panelW - previewWidth - 10;
        viewportTop = cardTop + 8;
        viewportBottom = footerY - 14;
        previewTop = cardTop + (height < 260 ? 22 : 32);
        previewBottom = footerY - 66;
        int tabW = (panelW - 8) / 3;
        for (Tab value : Tab.values()) addTab(panelX + value.ordinal() * (tabW + 4), tabY, tabW, value);
        var settings = AppearanceConfig.get();
        switch (tab) {
            case VISIBILITY -> {
                toggle("traits_enabled", settings.enabled, v -> settings.enabled = v);
                toggle("show_self", settings.showSelf, v -> settings.showSelf = v);
                toggle("show_others", settings.showOthers, v -> settings.showOthers = v);
                toggle("show_body_changes", settings.showBodyChanges, v -> settings.showBodyChanges = v);
            }
            case FIT -> buildFit(settings);
            case UNIQUENESS -> {
                toggle("uniqueness_enabled", settings.enableUniquenessEffects, v -> settings.enableUniquenessEffects = v);
                toggle("uniqueness_self", settings.uniquenessShowSelf, v -> settings.uniquenessShowSelf = v);
                toggle("uniqueness_others", settings.uniquenessShowOthers, v -> settings.uniquenessShowOthers = v);
                number("uniqueness_intensity", settings.uniquenessParticleIntensity, .15f, 1, true, v -> settings.uniquenessParticleIntensity = v);
            }
        }
        addPreviewControls();
        int buttonW = (panelW - 8) / 3;
        Button reset = addRenderableWidget(Button.builder(text("reset_defaults"), b -> {
            AppearanceConfig.setSettings(new AppearanceConfig.Settings()); rebuildWidgets();
        }).bounds(panelX, footerY, buttonW, 20).build());
        reset.setTooltip(Tooltip.create(text("reset_hint")));
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(panelX + buttonW + 4, footerY, buttonW, 20).build());
        done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            AppearanceConfig.save(); committed = true; onClose();
        }).bounds(panelX + (buttonW + 4) * 2, footerY, buttonW, 20).build());
        done.setTooltip(Tooltip.create(text("save_hint")));
        applyScroll();
    }

    private void addTab(int x, int y, int w, Tab value) {
        Runnable select = () -> minecraft.execute(() -> { tab = value; scroll = 0; rebuildWidgets(); });
        CoiTabButton button = addRenderableWidget(new CoiTabButton(x, y, w, 22, text("tab." + value.name().toLowerCase(Locale.ROOT)),
                null, () -> tab == value, select) {
            @Override public boolean keyPressed(@NonNull KeyEvent event) {
                if (isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER || event.key() == GLFW.GLFW_KEY_SPACE)) {
                    playDownSound(Minecraft.getInstance().getSoundManager()); select.run(); return true;
                }
                return super.keyPressed(event);
            }
            @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
                super.extractWidgetRenderState(g, mx, my, delta);
                if (isFocused()) g.outline(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, CoiStyle.ACCENT);
            }
        });
        if (value == tab) setInitialFocus(button);
    }

    private void buildFit(AppearanceConfig.Settings s) {
        Set<String> families = new HashSet<>();
        if (minecraft.player != null) for (String id : ClientAppearanceState.getTraits(minecraft.player.getUUID().toString())) {
            var family = AppearanceTraits.familyOf(id);
            if (family != null) families.add(family.id());
        }
        if (families.contains("body")) {
            heading("section.chest");
            number("size", s.chestScale, .8f, 1.5f, true, v -> s.chestScale = v);
            number("separation", s.chestSeparationPixels, -.4f, 1, false, v -> s.chestSeparationPixels = v);
            number("vertical_position", s.chestYOffsetPixels, -1.5f, 1.5f, false, v -> s.chestYOffsetPixels = v);
            number("roundness", s.chestFullness, .75f, 1.35f, true, v -> s.chestFullness = v);
            toggle("project_jacket", s.projectJacket, v -> s.projectJacket = v);
        }
        if (families.contains("hair")) {
            heading("section.hair");
            number("length", s.hairLength, .5f, 1.6f, true, v -> s.hairLength = v);
            number("vertical_position", s.hairYOffsetPixels, -1.5f, 1.5f, false, v -> s.hairYOffsetPixels = v);
        }
        if (families.contains("claws")) {
            heading("section.claws");
            number("length", s.clawLength, .5f, 1.5f, true, v -> s.clawLength = v);
            number("spread", s.clawSpread, .7f, 1.3f, true, v -> s.clawSpread = v);
            number("vertical_position", s.clawYOffsetPixels, -1, 1, false, v -> s.clawYOffsetPixels = v);
            number("depth", s.clawZOffsetPixels, -1, 1, false, v -> s.clawZOffsetPixels = v);
        }
        if (families.contains("wings")) {
            heading("section.wings");
            number("scale", s.wingScale, .6f, 1.5f, true, v -> s.wingScale = v);
            number("flap_speed", s.wingFlapSpeed, .2f, 3, true, v -> s.wingFlapSpeed = v);
        }
        if (families.contains("skin") || families.contains("chained")) {
            heading("section.skin");
            number("opacity", s.overlayOpacity, .2f, 1, true, v -> s.overlayOpacity = v);
        }
        if (rowY == 0) heading("no_traits");
    }

    private static Component text(String key) { return Component.translatable("screen.coi.appearance." + key); }
    private void heading(String key) { headings.add(new Heading(key, rowY, CoiStyle.ACCENT, 0)); rowY += Math.max(18, font.split(text(key), contentW - 16).size() * 10 + 4); }
    private void row(AbstractWidget widget, int y) { row(widget, y, 0); }
    private void row(AbstractWidget widget, int y, int labelHeight) { rows.add(new RowWidget(widget, y, labelHeight)); addRenderableWidget(widget); }
    private void toggle(String key, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(text(key), font).pos(contentX + 8, 0).maxWidth(contentW - 40)
                .selected(selected).onValueChange((box, checked) -> setter.accept(checked)).build();
        row(checkbox, rowY);
        rowY += Math.max(ROW, checkbox.getHeight() + 6);
    }

    private void number(String key, float initial, float min, float max, boolean percent, Consumer<Float> setter) {
        int labelHeight = contentW < 230 ? font.split(text(key), contentW - 16).size() * 10 + 4 : 0;
        if (labelHeight > 0) { headings.add(new Heading(key, rowY, CoiStyle.TEXT_BODY, labelHeight + 20)); rowY += labelHeight; }
        int fieldW = contentW < 230 ? 42 : 52;
        int sliderW = contentW - 22 - fieldW;
        NumberSlider slider = new NumberSlider(contentX + 8, sliderW, key, initial, min, max, percent, setter);
        EditBox field = new EditBox(font, contentX + 12 + sliderW, 0, fieldW, 20, text(key)) {
            @Override public void setFocused(boolean focused) {
                if (focused && !isFocused()) slider.editStart = slider.current();
                if (!focused && isFocused()) slider.revertInvalid();
                super.setFocused(focused);
            }
        };
        field.setMaxLength(8);
        field.setValue(format(initial * (percent ? 100 : 1)));
        slider.field = field;
        Component hint = Component.translatable("screen.coi.appearance.number_hint", format(min * (percent ? 100 : 1)),
                format(max * (percent ? 100 : 1)), percent ? "%" : "px");
        if (key.equals("vertical_position") || key.equals("depth")) hint = hint.copy().append("\n").append(text(key + "_hint"));
        if (key.equals("uniqueness_intensity")) hint = hint.copy().append("\n").append(text("particles_hint"));
        slider.setTooltip(Tooltip.create(hint)); field.setTooltip(Tooltip.create(hint));
        field.setResponder(slider::typed);
        numbers.add(slider); row(slider, rowY, labelHeight); row(field, rowY, labelHeight); rowY += ROW;
    }

    private final class NumberSlider extends AbstractSliderButton {
        private final String key;
        private final float min, max;
        private final boolean percent;
        private final Consumer<Float> setter;
        private EditBox field;
        private boolean updating, valid = true;
        private float editStart;
        NumberSlider(int x, int w, String key, float initial, float min, float max, boolean percent, Consumer<Float> setter) {
            super(x, 0, w, 20, Component.empty(), (initial - min) / (max - min));
            this.key = key; this.min = min; this.max = max; this.percent = percent; this.setter = setter; this.editStart = initial;
            updateMessage();
        }
        private float current() { return min + (float) value * (max - min); }
        @Override protected void updateMessage() {
            String formatted = format(current() * (percent ? 100 : 1)) + (percent ? "%" : " px");
            setMessage(contentW < 230 ? Component.literal(formatted) : text(key).copy().append(": " + formatted));
        }
        @Override protected void applyValue() {
            float step = percent ? .01f : .05f;
            float v = Math.clamp(Math.round(current() / step) * step, min, max);
            setCurrent(v);
        }
        private void setCurrent(float v) {
            value = (v - min) / (max - min); setter.accept(v); valid = true; updateMessage();
            if (field != null) { updating = true; field.setValue(format(v * (percent ? 100 : 1))); field.setTextColor(CoiStyle.TEXT_BODY); updating = false; }
            validateFields();
        }
        // Leaving an invalid edit must not save a valid prefix of the rejected input.
        void revertInvalid() { if (!valid) setCurrent(editStart); }
        @Override public boolean keyPressed(@NonNull KeyEvent event) {
            if (isFocused() && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT)) {
                value = Math.clamp(value + (event.key() == GLFW.GLFW_KEY_RIGHT ? 1 : -1) * (percent ? .01 : .05) / (max - min), 0, 1);
                applyValue(); return true;
            }
            return super.keyPressed(event);
        }
        void typed(String input) {
            if (updating) return;
            try {
                float v = Float.parseFloat(input) / (percent ? 100 : 1);
                valid = Float.isFinite(v) && v >= min && v <= max;
                if (valid) { value = (v - min) / (max - min); setter.accept(v); updateMessage(); }
            } catch (NumberFormatException ignored) { valid = false; }
            field.setTextColor(valid ? CoiStyle.TEXT_BODY : 0xFFFF5555); validateFields();
        }
    }

    private void validateFields() { if (done != null) done.active = numbers.stream().allMatch(n -> n.valid); }
    private static String format(double value) { return String.format(Locale.ROOT, "%.2f", Math.abs(value) < .005 ? 0 : value).replaceAll("0+$", "").replaceAll("\\.$", ""); }

    private void addPreviewControls() {
        int y = footerY - 56;
        int labelW = Math.max(font.width(text("front")), font.width(text("back"))) + 8;
        int threeLabelW = Math.max(labelW, font.width(text("side")) + 8);
        String[] views = (previewWidth - 22) / 3 >= threeLabelW ? new String[]{"front", "side", "back"}
                : (previewWidth - 19) / 2 >= labelW ? new String[]{"front", "back"} : new String[]{"front"};
        int w = (previewWidth - 16 - (views.length - 1) * 3) / views.length;
        for (int i = 0; i < views.length; i++) {
            String view = views[i];
            addRenderableWidget(Button.builder(text(view), b -> { previewYaw = view.equals("front") ? 0 : view.equals("side") ? 90 : 180; previewPitch = 0; })
                    .bounds(previewLeft + 8 + i * (w + 3), y, w, 20).build());
        }
        zoom = addRenderableWidget(new PreviewZoom());
    }

    private final class PreviewZoom extends AbstractSliderButton {
        PreviewZoom() {
            super(previewLeft + 8, footerY - 30, previewWidth - 16, 20, Component.empty(), previewZoom - .6);
            updateMessage();
        }
        void sync() { value = previewZoom - .6; updateMessage(); }
        @Override protected void updateMessage() { setMessage(text("zoom").copy().append(": " + Math.round(previewZoom * 100) + "%")); }
        @Override protected void applyValue() { previewZoom = .6f + (float) value; updateMessage(); }
    }

    private double maxScroll() { return Math.max(0, rowY - (viewportBottom - viewportTop)); }
    private void applyScroll() {
        scroll = Math.clamp(scroll, 0, maxScroll());
        for (RowWidget row : rows) {
            int y = viewportTop + row.y - (int) scroll;
            row.widget.setY(y); row.widget.visible = y - row.labelHeight >= viewportTop && y + row.widget.getHeight() <= viewportBottom;
            if (!row.widget.visible && getFocused() == row.widget) setFocused(null);
        }
    }
    private void scrollTo(double y) {
        int h = viewportBottom - viewportTop;
        int thumb = Math.max(16, h * h / Math.max(1, rowY));
        scroll = (y - viewportTop - thumb / 2.0) / Math.max(1, h - thumb) * maxScroll(); applyScroll();
    }

    @Override public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, CoiStyle.BACKDROP);
        g.centeredText(font, title, width / 2, height < 260 ? 5 : 12, CoiStyle.ACCENT);
        CoiStyle.drawCard(g, previewLeft, cardTop, previewWidth, viewportBottom + 8 - cardTop);
        CoiStyle.drawCard(g, contentX, cardTop, contentW, viewportBottom + 8 - cardTop);
        if (height < 260) g.centeredText(font, text("rotate_short_hint"), previewLeft + previewWidth / 2, cardTop + 8, CoiStyle.TEXT_MUTED);
        if (height >= 260) {
            boolean hidden = !AppearanceConfig.get().enabled || !AppearanceConfig.get().showSelf;
            g.centeredText(font, text(hidden ? "hidden_hint" : "rotate_hint"), previewLeft + previewWidth / 2, cardTop + 8, hidden ? CoiStyle.ACCENT : CoiStyle.TEXT_MUTED);
            g.centeredText(font, text(hidden ? "visibility_hint" : "zoom_hint"), previewLeft + previewWidth / 2, cardTop + 18, CoiStyle.TEXT_MUTED);
        }
        if (minecraft.player != null) {
            int scale = Math.round(Math.min((previewBottom - previewTop - 8) / 2.5f, previewWidth / 2.5f) * previewZoom);
            extractPreviewEntity(g, previewLeft + 4, previewTop, previewLeft + previewWidth - 4, previewBottom,
                    Math.max(8, scale), previewYaw, previewPitch, minecraft.player);
        }
        for (Heading h : headings) {
            int y = viewportTop + h.y - (int) scroll;
            if (h.groupHeight > 0 && (y < viewportTop || y + h.groupHeight > viewportBottom)) continue;
            for (var line : font.split(text(h.key), contentW - 16)) {
                if (y >= viewportTop && y + 10 <= viewportBottom) g.text(font, line, contentX + 8, y + 2, h.color, false);
                y += 10;
            }
        }
        super.extractRenderState(g, mx, my, delta);
        if (done != null && !done.active) g.centeredText(font, text("invalid_hint"), contentX + contentW / 2, footerY - 10, CoiStyle.ACCENT);
        if (maxScroll() > 0) {
            int h = viewportBottom - viewportTop, x = contentX + contentW - 5;
            int thumb = Math.max(16, h * h / rowY);
            int y = viewportTop + (int) ((h - thumb) * scroll / maxScroll());
            g.fill(x, viewportTop, x + 3, viewportBottom, CoiStyle.SCROLL_TRACK);
            g.fill(x, y, x + 3, y + thumb, CoiStyle.ACCENT);
        }
    }

    private boolean insidePreview(double x, double y) { return x >= previewLeft && x < previewLeft + previewWidth && y >= previewTop && y < previewBottom; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
        if (maxScroll() > 0 && e.button() == 0 && e.x() >= contentX + contentW - 7 && e.x() < contentX + contentW && e.y() >= viewportTop && e.y() <= viewportBottom) {
            draggingScroll = true; scrollTo(e.y()); return true;
        }
        if (super.mouseClicked(e, twice)) return true;
        if (e.button() == 0 && insidePreview(e.x(), e.y())) { draggingPreview = true; return true; }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingScroll) { scrollTo(e.y()); return true; }
        if (draggingPreview && e.button() == 0) {
            previewYaw = net.minecraft.util.Mth.wrapDegrees(previewYaw + (float) dx * .65f);
            previewPitch = Math.clamp(previewPitch + (float) dy * .65f, -45, 45);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (draggingScroll || draggingPreview) { draggingScroll = false; draggingPreview = false; return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (insidePreview(x, y)) { previewZoom = Math.clamp(previewZoom + (float) vertical * .1f, .6f, 1.6f); zoom.sync(); return true; }
        if (x >= contentX && x < contentX + contentW && y >= viewportTop && y <= viewportBottom) { scroll -= vertical * ROW; applyScroll(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void removed() { if (!committed) AppearanceConfig.setSettings(original); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }

    private static void extractPreviewEntity(GuiGraphicsExtractor graphics,
                                             int x1, int y1, int x2, int y2, int scale,
                                             float yawDegrees, float pitchDegrees,
                                             LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderState state = minecraft.getEntityRenderDispatcher().extractEntity(entity, 1.0f);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + yawDegrees;
            // These are head-relative angles. Orbit the whole model without twisting its neck.
            living.yRot = 0;
            living.xRot = 0;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
        }
        Quaternionf flip = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf pitch = new Quaternionf().rotateX(pitchDegrees * ((float) Math.PI / 180.0f));
        flip.mul(pitch);
        Vector3f translation = new Vector3f(0.0f, state.boundingBoxHeight / 2.0f + 0.0625f, 0.0f);
        graphics.entity(state, scale, translation, flip, pitch, x1, y1, x2, y2);
    }

}
