package dev.ua.ikeepcalm.coi.client.gesture;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.config.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;
import dev.ua.ikeepcalm.coi.client.gesture.GestureRecognizer.StrokePoint;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Ritual casting: opened while the gesture key is held (same pattern as the
 * ability wheel — mouse freed from the camera, movement still allowed).
 * Cursor movement is sampled into an ink trail; releasing the key evaluates
 * the stroke. Success casts through the normal ability pipeline; failure
 * shakes the trail red and fades. Deliberately slower than keybinds.
 */
public class GestureScreen extends Screen {

    private static final long OPEN_MS = 150;
    private static final long SUCCESS_MS = 450;
    private static final long FAIL_MS = 650;

    /**
     * Where the cursor starts, as a fraction of window height. Above the
     * crosshair, so shapes (which are mostly drawn downward) end up centered
     * instead of crammed into the bottom half.
     */
    private static final float START_Y_FRACTION = 0.30f;

    private enum Phase {DRAWING, SUCCESS, FAIL}

    private Phase phase = Phase.DRAWING;

    private final long openTime = System.currentTimeMillis();
    private final List<StrokePoint> points = new ArrayList<>();
    private long resultTime;
    private Component resultText;
    private int resultColor = 0xFFFFFF;
    private int ticksOpen = 0;
    private boolean cursorPositioned = false;

    public GestureScreen() {
        super(Component.translatable("screen.coi.gesture_draw_hint"));
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            this.minecraft.mouseHandler.releaseMouse();
            // init() also runs on window resize — only teleport the cursor once
            if (!cursorPositioned) {
                cursorPositioned = true;
                positionCursorAboveCrosshair();
            }
        }
    }

    /**
     * releaseMouse() parks the cursor at screen center; nudge it up so there
     * is room to draw. glfwGetWindowSize and glfwSetCursorPos use the same
     * units, so a fractional position needs no GUI-scale conversion.
     */
    private void positionCursorAboveCrosshair() {
        long handle = this.minecraft.getWindow().handle();
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(handle, w, h);
        GLFW.glfwSetCursorPos(handle, w[0] / 2.0, h[0] * START_Y_FRACTION);
    }

    @Override
    public void tick() {
        ticksOpen++;
        keepMovementKeysAlive();

        if (phase == Phase.DRAWING) {
            if (ticksOpen > 2 && !CircleOfImaginationClient.isKeyDown(CircleOfImaginationClient.gestureCast)) {
                evaluate();
            }
        } else if (System.currentTimeMillis() - resultTime >= (phase == Phase.SUCCESS ? SUCCESS_MS : FAIL_MS)) {
            this.onClose();
        }
    }

    private void evaluate() {
        if (!GestureRecognizer.isMeaningful(points)) {
            this.onClose();
            return;
        }

        GestureType type = GestureRecognizer.recognize(points);
        if (type == null) {
            fail(Component.translatable("screen.coi.gesture_failed"));
            return;
        }

        String ability = CircleOfImaginationClient.getGestureAbility(type);
        if (ability == null) {
            fail(Component.translatable("screen.coi.gesture_unbound", type.displayName()));
            return;
        }

        CircleOfImaginationClient.useAbilityById(ability);
        phase = Phase.SUCCESS;
        resultTime = System.currentTimeMillis();
        resultColor = AbilityInfo.pathwayColor(AbilityInfo.extractId(ability)) & 0xFFFFFF;
        resultText = Component.literal(AbilityInfo.extractDisplayName(ability));
    }

    private void fail(Component message) {
        phase = Phase.FAIL;
        resultTime = System.currentTimeMillis();
        resultColor = 0xFF4444;
        resultText = message;
    }

    /**
     * Screens normally swallow keyboard input, freezing the player. Feed the
     * raw key state back into the movement bindings so the player can keep
     * moving while drawing.
     */
    private void keepMovementKeysAlive() {
        if (this.minecraft.player == null) return;
        var options = this.minecraft.options;
        KeyMapping[] movementKeys = {
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint
        };
        for (KeyMapping key : movementKeys) {
            key.setDown(CircleOfImaginationClient.isKeyDown(key));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        long now = System.currentTimeMillis();
        float openP = EffectPaint.clamp((now - openTime) / (float) OPEN_MS, 0f, 1f);

        // Result phases fade everything back out before the screen closes
        float fade = 1f;
        if (phase != Phase.DRAWING) {
            long resultLength = phase == Phase.SUCCESS ? SUCCESS_MS : FAIL_MS;
            fade = 1f - EffectPaint.clamp((now - resultTime) / (float) resultLength, 0f, 1f);
        }

        graphics.fill(0, 0, this.width, this.height, (int) (0x70 * openP * fade) << 24);

        if (phase == Phase.DRAWING) {
            capturePoint(mouseX, mouseY);
        }

        renderTrail(graphics, fade, now);
        renderTexts(graphics, openP, fade);

        if (phase == Phase.DRAWING) {
            renderLegend(graphics, openP);
            graphics.fill(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xFFFFFFFF);
        }
    }

    private void capturePoint(int mouseX, int mouseY) {
        StrokePoint p = new StrokePoint(mouseX, mouseY);
        if (points.isEmpty()) {
            if (Math.abs(mouseX - this.width / 2) <= 2 && Math.abs(mouseY - this.height / 2) <= 2) return;
            points.add(p);
            return;
        }
        StrokePoint last = points.getLast();
        float dx = p.x() - last.x();
        float dy = p.y() - last.y();
        float distSq = dx * dx + dy * dy;

        float teleport = Math.max(40f, this.height * 0.12f);
        if (points.size() == 1 && distSq > teleport * teleport) {
            points.set(0, p);
            return;
        }

        if (distSq >= GestureRecognizer.MIN_POINT_DISTANCE * GestureRecognizer.MIN_POINT_DISTANCE) {
            points.add(p);
        }
    }

    private void renderTrail(GuiGraphicsExtractor graphics, float fade, long now) {
        int n = points.size();
        if (n < 2) return;

        int rgb = phase == Phase.DRAWING ? 0xE8E8FF : resultColor;
        // Failed strokes shake, hardest right after the fizzle
        float shake = phase == Phase.FAIL ? 3f * fade : 0f;
        boolean glow = phase == Phase.SUCCESS;

        for (int i = 1; i < n; i++) {
            StrokePoint p1 = points.get(i - 1);
            StrokePoint p2 = points.get(i);
            float x1 = p1.x() + jitter(i - 1, now, shake);
            float y1 = p1.y() + jitter((i - 1) * 7 + 3, now, shake);
            float x2 = p2.x() + jitter(i, now, shake);
            float y2 = p2.y() + jitter(i * 7 + 3, now, shake);

            // Ink: older segments dimmer, the fresh end at full strength
            int alpha = (int) ((90 + 165f * i / n) * fade);
            if (glow) {
                EffectPaint.line(graphics, x1, y1, x2, y2, EffectPaint.argb(rgb, alpha / 4), 6);
            }
            EffectPaint.line(graphics, x1, y1, x2, y2, EffectPaint.argb(rgb, alpha), 2);
        }
    }

    private static float jitter(int index, long now, float amplitude) {
        if (amplitude <= 0f) return 0f;
        return (float) Math.sin(index * 12.9898 + now * 0.045) * amplitude;
    }

    private void renderTexts(GuiGraphicsExtractor graphics, float openP, float fade) {
        if (phase == Phase.DRAWING) {
            int alpha = (int) (200 * openP);
            if (alpha < 8) return;
            graphics.centeredText(this.font, Component.translatable("screen.coi.gesture_draw_hint"),
                    this.width / 2, 40, EffectPaint.argb(0xFFFFFF, alpha));
        } else if (resultText != null) {
            int alpha = (int) (255 * fade);
            if (alpha < 8) return;
            graphics.centeredText(this.font, resultText, this.width / 2, 40, EffectPaint.argb(resultColor, alpha));
        }
    }

    /**
     * Bottom row reminding which shapes are bound to what.
     */
    private void renderLegend(GuiGraphicsExtractor graphics, float openP) {
        int previewSize = 12;
        int gap = 18;

        List<GestureType> bound = new ArrayList<>();
        int totalWidth = 0;
        for (GestureType type : GestureType.values()) {
            String ability = CircleOfImaginationClient.getGestureAbility(type);
            if (ability == null) continue;
            bound.add(type);
            totalWidth += previewSize + 5 + this.font.width(AbilityInfo.extractDisplayName(ability)) + gap;
        }
        if (bound.isEmpty()) return;
        totalWidth -= gap;

        int shapeAlpha = (int) (140 * openP);
        int textAlpha = (int) (170 * openP);
        if (textAlpha < 8) return;

        int x = this.width / 2 - totalWidth / 2;
        int y = this.height - 36;
        for (GestureType type : bound) {
            String name = AbilityInfo.extractDisplayName(CircleOfImaginationClient.getGestureAbility(type));
            type.drawPreview(graphics, x, y, previewSize, EffectPaint.argb(0xFFFFFF, shapeAlpha));
            graphics.text(this.font, name, x + previewSize + 5, y + 2, EffectPaint.argb(0xAAAAAA, textAlpha));
            x += previewSize + 5 + this.font.width(name) + gap;
        }
    }
}
