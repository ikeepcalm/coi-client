package dev.ua.ikeepcalm.coi.client.screen.title;

import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.screen.TitleScreenHaunt;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * The Lord of the Mysteries main menu: a void, a turning wheel of the 24
 * pathway emblems, the wordmark at its centre, and the vanilla buttons
 * re-chromed in the mod's dark/gold language.
 * <p>
 * This class is the shell — the gate, the time base, the geometry and the one
 * accent colour the whole scene shares. Nothing else decides any of those four,
 * which is the point: {@link #accent()} is a single value that corruption drags
 * from gold toward a sick crimson, so the void, the ring, the wordmark and the
 * button rails all sicken together rather than four classes each having their
 * own opinion about how red things have got.
 * <p>
 * Everything here is off behind one boolean. {@code coiTitleScreen} false means
 * every mixin below falls straight through to vanilla — panorama, logo, splash
 * placement, button sprites — and {@link TitleScreenHaunt}, which predates the
 * takeover, keeps drawing over the vanilla menu exactly as it did.
 */
public class TitleTakeover {

    /**
     * Where the wheel sits this frame. Computed once per draw and handed down,
     * rather than three collaborators each re-deriving it off {@code w}/{@code h}
     * and drifting the first time one of the constants is touched.
     *
     * @param cx     the ring's centre
     * @param cy     the ring's centre, above the middle so the button column
     *               falls inside the circle rather than under it
     * @param radius the circle the emblems ride
     */
    public record Geometry(int cx, int cy, int radius) {
    }

    /**
     * The colour the scene arrives at when corruption is total: crimson with
     * most of its saturation gone, which reads as something spoiled rather than
     * as an alarm. A fully saturated red would say "error".
     */
    private static final int SPOILED = 0xFFA85C63;

    /**
     * Below this the ring stops shrinking with the window and simply crops.
     */
    private static final int MIN_RADIUS = 90;

    /**
     * The longest gap {@link #approach} will honour. The title screen can sit
     * unrendered for minutes between visits, and a real gap of that size makes
     * every tween finish in one step — the wheel would be found already
     * rearranged around wherever the cursor happened to be left.
     */
    private static final float MAX_FRAME_MS = 100f;

    /**
     * The frame clock the scene's eased values step on. Render thread only.
     */
    private static long lastFrameAt;
    private static float frameDelta;

    private TitleTakeover() {
    }

    /**
     * The setting, and the whole feature's on/off switch.
     */
    public static boolean active() {
        return HudConfig.getSettings().coiTitleScreen;
    }

    /**
     * True only when the takeover is on <em>and</em> the title screen is the
     * screen being drawn. The logo, splash and button hooks are all shared with
     * the rest of the game, so both halves of this matter.
     */
    public static boolean onTitleScreen() {
        return active() && Minecraft.getInstance().gui.screen() instanceof TitleScreen;
    }

    /**
     * How corrupted the scene is, 0..1 — the same figure the haunting uses, so
     * the hallucinations toggle covers the takeover's horror for free and a
     * clean player never sees any of it.
     */
    public static float corruption() {
        return TitleScreenHaunt.intensity();
    }

    /**
     * The one accent the whole scene tints from.
     */
    public static int accent() {
        return MenuTheme.lerpArgb(CoiStyle.ACCENT, SPOILED, corruption());
    }

    /**
     * Wall-clock milliseconds. The mod animates on the clock rather than on
     * ticks everywhere else, and the title screen has no ticks worth the name.
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    public static boolean steady() {
        return HudConfig.getSettings().epilepsyMode;
    }

    /**
     * One step of a tween toward {@code target}, frame-rate independent, and the
     * only way anything in the scene reads the frame delta. Under
     * {@code epilepsyMode} it returns the target outright — the same contract as
     * {@code SheetContext.approach}, so the setting is honoured here the way it
     * is everywhere else in the mod, at one chokepoint rather than per caller.
     */
    public static float approach(float current, float target, float durationMs) {
        if (steady() || durationMs <= 0) return target;
        float step = frameDelta / durationMs;
        return step >= 1f ? target : current + (target - current) * step;
    }

    /**
     * The pathway whose emblem is lit, or empty when nothing is — which is what
     * a fresh install looks like, and is correct rather than a missing value.
     */
    public static String litPathway() {
        return ClientStateStore.getLastPathway();
    }

    public static Geometry geometry(int w, int h) {
        int radius = Math.max(MIN_RADIUS, Math.round(Math.min(w, h) * 0.46f) - 24);
        return new Geometry(w / 2, Math.round(h * 0.46f), radius);
    }

    /**
     * The whole backdrop, in the order it has to be painted: void, then wheel,
     * then the wordmark over both. Called in place of vanilla's panorama, so
     * the menu's widgets still land on top of all of it.
     * <p>
     * The cursor comes through because the wheel's emblems shy away from it;
     * it arrives already in gui-scaled pixels, which is the space the wheel
     * works in, so it is passed down untouched.
     * <p>
     * This is also where the frame delta is advanced, and the <em>only</em>
     * place it may be: {@link #drawSplash} runs once a frame too, and counting
     * the same gap twice would halve the duration of every tween in the scene.
     */
    public static void drawScene(GuiGraphicsExtractor graphics, int w, int h, int mouseX, int mouseY) {
        long now = now();
        frameDelta = lastFrameAt == 0 ? 0f : Math.min(MAX_FRAME_MS, now - lastFrameAt);
        lastFrameAt = now;

        Geometry geo = geometry(w, h);
        int accent = accent();
        float corruption = corruption();
        TitleScene.draw(graphics, w, h, geo, accent, corruption);
        PathwayWheel.draw(graphics, geo, accent, corruption, mouseX, mouseY);
        TitleLogo.draw(graphics, Minecraft.getInstance().font, w, h, geo, accent);
    }

    /**
     * The splash line, drawn where the vanilla one is drawn from so it keeps
     * vanilla's place in the layer order — only its position on screen moves,
     * off the wordmark's own top-right corner instead of the vanilla logo's.
     */
    public static void drawSplash(GuiGraphicsExtractor graphics, Font font, int w, int h) {
        TitleLogo.drawSplash(graphics, font, w, h, geometry(w, h), accent());
    }
}
