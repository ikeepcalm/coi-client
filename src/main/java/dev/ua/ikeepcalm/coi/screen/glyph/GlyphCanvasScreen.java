package dev.ua.ikeepcalm.coi.screen.glyph;

import dev.ua.ikeepcalm.coi.domain.glyph.GlyphDrawing;
import dev.ua.ikeepcalm.coi.domain.glyph.GlyphSheet;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.GlyphSubmitPayload;
import dev.ua.ikeepcalm.coi.screen.CoiTabButton;
import dev.ua.ikeepcalm.coi.ui.CoiStyle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/**
 * Drawing magic: the player draws a ring, a sigil inside it and signs around
 * it on each layer, names the spell and submits. The server reads the strokes
 * and answers with a result; this screen only captures ink and shows replies.
 * <p>
 * Opened by a {@code coi-client:glyph} {@code open}; the pixels live in
 * {@link GlyphCanvasPainter}, the strokes in {@link GlyphDrawing}.
 */
public class GlyphCanvasScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int TOP = 42;
    private static final int CONTROLS_H = 20;
    /**
     * The server accepts one submit per 2 s; a reply that never comes frees the button after 5 s.
     */
    private static final long RESUBMIT_MS = 2_000;
    private static final long REPLY_TIMEOUT_MS = 5_000;
    private static final long CLOSE_AFTER_SUCCESS_MS = 1_600;
    private static final int MAX_RAW_POINTS = 2_000;
    private static final int OK_COLOR = 0xFF8FE39A;
    private static final int ERROR_COLOR = 0xFFFF7A7A;

    private final GlyphSheet sheet;
    private final GlyphDrawing drawing;
    private final long openedAt = System.currentTimeMillis();
    private final List<CoiTabButton> tabs = new ArrayList<>();

    private int layer;
    private List<double[]> active;
    private long activeStart;
    private String name = "";
    private EditBox nameBox;
    private Component status;
    private int statusColor = CoiStyle.TEXT_BODY;
    private boolean waiting;
    private long sentAt;
    private long closeAt;

    private int canvasX;
    private int canvasY;
    private int canvasSize;
    private int panelX;
    private int panelW;

    public GlyphCanvasScreen(GlyphSheet sheet) {
        super(Component.translatable("screen.coi.glyph_title"));
        this.sheet = sheet;
        this.drawing = new GlyphDrawing(sheet.maxLayers(), sheet.limits());
    }

    public String session() {
        return sheet.session();
    }

    @Override
    protected void init() {
        clearWidgets();
        tabs.clear();
        canvasSize = Math.max(80, Math.min(height - TOP - CONTROLS_H - 16, (int) ((width - 3 * MARGIN) * 0.6f)));
        canvasX = MARGIN;
        canvasY = TOP;
        panelX = canvasX + canvasSize + MARGIN;
        panelW = width - panelX - MARGIN;

        int tabW = Math.min(28, canvasSize / Math.max(1, drawing.layerCount()));
        for (int i = 0; i < drawing.layerCount(); i++) {
            final int index = i;
            tabs.add(addRenderableWidget(new CoiTabButton(canvasX + i * tabW, TOP - 18, tabW - 2, 16,
                    Component.literal(String.valueOf(i + 1)), null, () -> layer == index, () -> layer = index)));
        }

        int y = height - CONTROLS_H - 6;
        int right = width - MARGIN;
        addRenderableWidget(Button.builder(Component.translatable("screen.coi.glyph_undo"), b -> drawing.undo(layer))
                .bounds(MARGIN, y, 50, CONTROLS_H).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.coi.glyph_clear"), b -> drawing.clear(layer))
                .bounds(MARGIN + 54, y, 50, CONTROLS_H).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.coi.glyph_submit"), b -> submit())
                .bounds(right - 80, y, 80, CONTROLS_H).build());
        nameBox = new EditBox(font, MARGIN + 110, y, Math.max(40, right - 86 - MARGIN - 110), CONTROLS_H,
                Component.translatable("screen.coi.glyph_name"));
        nameBox.setMaxLength(sheet.limits().name());
        nameBox.setHint(Component.translatable("screen.coi.glyph_name"));
        nameBox.setValue(name);
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);
    }

    // ── input ───────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !insideCanvas(event.x(), event.y())) return false;
        setFocused(null);
        active = new ArrayList<>();
        activeStart = System.currentTimeMillis() - openedAt;
        active.add(toCanvas(event.x(), event.y()));
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (active == null) return super.mouseDragged(event, dx, dy);
        double[] p = toCanvas(event.x(), event.y());
        double[] last = active.getLast();
        if (active.size() < MAX_RAW_POINTS && Math.hypot(p[0] - last[0], p[1] - last[1]) >= 2) active.add(p);
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (active == null) return super.mouseReleased(event);
        String refusal = drawing.add(layer, active, activeStart, System.currentTimeMillis() - openedAt);
        if (refusal != null) setStatus(Component.translatable(refusal), ERROR_COLOR);
        active = null;
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        boolean typing = nameBox != null && nameBox.isFocused();
        if (!typing && event.hasControlDown() && event.key() == GLFW.GLFW_KEY_Z) {
            drawing.undo(layer);
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean insideCanvas(double x, double y) {
        return x >= canvasX && x < canvasX + canvasSize && y >= canvasY && y < canvasY + canvasSize;
    }

    private double[] toCanvas(double x, double y) {
        double scale = GlyphDrawing.CANVAS / (double) canvasSize;
        return new double[]{
                Math.clamp((x - canvasX) * scale, 0, GlyphDrawing.CANVAS),
                Math.clamp((y - canvasY) * scale, 0, GlyphDrawing.CANVAS)};
    }

    // ── submit and reply ────────────────────────────────────────────────────

    private void submit() {
        long now = System.currentTimeMillis();
        if (closeAt > 0 || now - sentAt < (waiting ? REPLY_TIMEOUT_MS : RESUBMIT_MS)) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            setStatus(Component.translatable("screen.coi.glyph_need_name"), ERROR_COLOR);
            return;
        }
        if (drawing.lastInkedLayer() < 0) {
            setStatus(Component.translatable("screen.coi.glyph_empty"), ERROR_COLOR);
            return;
        }
        String json = drawing.toJson(sheet.session(), sheet.dict(), trimmed);
        if (json.getBytes(StandardCharsets.UTF_8).length > sheet.limits().bytes()) {
            setStatus(Component.translatable("screen.coi.glyph_too_large"), ERROR_COLOR);
            return;
        }
        if (!ClientPlayNetworking.canSend(GlyphSubmitPayload.ID)) {
            setStatus(Component.translatable("screen.coi.glyph_unavailable"), ERROR_COLOR);
            return;
        }
        ClientPlayNetworking.send(new GlyphSubmitPayload(json));
        waiting = true;
        sentAt = now;
        setStatus(Component.translatable("screen.coi.glyph_sending"), CoiStyle.TEXT_MUTED);
    }

    /**
     * The server's verdict. A success closes the canvas shortly after; a
     * failure jumps to the layer it is about.
     */
    public void showResult(boolean ok, String message, int failedLayer) {
        waiting = false;
        setStatus(Component.literal(message), ok ? OK_COLOR : ERROR_COLOR);
        if (ok) {
            closeAt = System.currentTimeMillis() + CLOSE_AFTER_SUCCESS_MS;
        } else if (failedLayer >= 0 && failedLayer < drawing.layerCount()) {
            layer = failedLayer;
        }
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    @Override
    public void tick() {
        if (closeAt > 0 && System.currentTimeMillis() >= closeAt) onClose();
    }

    // ── drawing ─────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, CoiStyle.BACKDROP);
        g.text(font, title, MARGIN, 8, CoiStyle.ACCENT, false);
        g.text(font, font.plainSubstrByWidth(Component.translatable("screen.coi.glyph_hint").getString(), width - 2 * MARGIN),
                MARGIN, 20, CoiStyle.TEXT_MUTED, false);
        for (int i = 0; i < tabs.size(); i++) {
            boolean inked = !drawing.layer(i).isEmpty();
            tabs.get(i).setMessage(Component.literal((i + 1) + (inked ? "*" : "")));
        }
        GlyphCanvasPainter.canvas(g, canvasX, canvasY, canvasSize, layer % 2 == 1, drawing.layer(layer),
                active == null ? List.of() : active);
        if (status != null) {
            String text = font.plainSubstrByWidth(status.getString(), canvasSize - 8);
            g.text(font, text, canvasX + 4, canvasY + canvasSize - 12, statusColor, true);
        }
        GlyphSheet.Glyph hovered = panelW >= 60
                ? GlyphCanvasPainter.reference(g, font, panelX, canvasY, panelW, canvasSize, sheet.glyphs(), mouseX, mouseY)
                : null;
        super.extractRenderState(g, mouseX, mouseY, partial);
        if (hovered != null) g.setTooltipForNextFrame(tooltip(hovered), mouseX, mouseY);
    }

    private Component tooltip(GlyphSheet.Glyph glyph) {
        return switch (glyph.flow()) {
            case "out" -> Component.translatable("screen.coi.glyph_flow_out", glyph.name());
            case "in" -> Component.translatable("screen.coi.glyph_flow_in", glyph.name());
            default -> Component.literal(glyph.name());
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
