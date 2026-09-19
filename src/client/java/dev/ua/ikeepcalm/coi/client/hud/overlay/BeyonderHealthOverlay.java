package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthBarPaint;
import dev.ua.ikeepcalm.coi.client.hud.render.HealthStyle;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * The Beyonder's real HP pool, on the vanilla hearts' row.
 * <p>
 * <b>This is the one HUD element in the mod that replaces a vanilla element
 * rather than attaching beside one.</b> Everything else calls
 * {@code attachElementBefore} and coexists with the vanilla HUD; this one takes
 * {@link VanillaHudElements#HEALTH_BAR} over, because ten hearts and a 1750-HP
 * pool cannot both be the truth about the same number. The replacement keeps a
 * reference to the element it displaced and calls it whenever the hearts are
 * still wanted, so a vanilla server — or a player who switched this off — gets
 * them back byte for byte.
 * <p>
 * <b>The current value is derived, not received.</b> The pool is a proportional
 * mirror of vanilla health and the server recomputes it <em>from</em> vanilla
 * health after every hit, so vanilla health is the authority:
 * <pre>poolCurrent = poolMax * (getHealth() / getMaxHealth())</pre>
 * Only {@code poolMax} comes over the wire ({@code maxHealth} on
 * {@code coi-client:conditions}). Deriving beats a pushed value on both counts
 * that matter: it moves the same frame the damage lands, and it cannot go stale
 * the way the server's own copy does between its periodic syncs.
 * <p>
 * The denominator is {@code getMaxHealth()}, never a hardcoded 20 — some
 * abilities apply a negative max-health modifier, and the mirror is a
 * percentage either way.
 * <p>
 * <b>This class is the shell.</b> It owns the render gate, the vanilla
 * replacement, the derivation, the flash/pulse state and the anchor;
 * {@link HealthStyle} owns which shape is drawn and {@link HealthBarPaint} owns
 * the pixels of all four.
 */
public class BeyonderHealthOverlay {

    /**
     * The box every style occupies. It runs from the hotbar's left edge
     * ({@code screenW / 2 - 91}) to {@code screenW / 2 + 10} — <b>the widest it
     * can be on this row</b>. The hearts only reached {@code screenW / 2 - 10},
     * so replacing them freed the 20px gap in the middle; the food bar starts
     * at {@code screenW / 2 + 10} and is the hard stop. A four-digit pool needs
     * the room the hearts' own 82 did not leave it.
     * <p>
     * Being <b>even</b> is what makes the centring exact: {@link
     * HudAnchor#resolve} centres with {@code (screenW - barW) / 2}, and that
     * floor lands a pixel away from vanilla's {@code screenW / 2 - 91} when the
     * screen width is odd <em>and</em> the bar width is odd too. 95 broke that
     * rule the class states two paragraphs down, so it is 96.
     */
    public static final int BAR_WIDTH = 96;
    /**
     * 9, because {@link CoiBar#frame} draws its border <em>outside</em> the box
     * — the drawn footprint is {@code BAR_HEIGHT + 2}, and the heart row has
     * only {@code h-40 .. h-30} before the experience bar starts at
     * {@code h-29}. At 11 the bar was 13 rows tall and the XP bar, which draws
     * after us, clipped its bottom.
     */
    public static final int BAR_HEIGHT = 9;

    /**
     * Defaults that put the box where the hearts start. {@code
     * HudAnchor.resolve} centres with {@code (w - BAR_WIDTH) / 2}, so landing
     * on vanilla's {@code w / 2 - 91} needs an offset of
     * {@code BAR_WIDTH / 2 - 91} — <b>derived, never typed</b>. Hard-coding it
     * once already went wrong: widening the bar from 82 to 100 left the old
     * {@code -50} behind and pushed every existing config 9px to the left.
     * (The identity holds for any <em>even</em> {@code BAR_WIDTH}, which is the
     * other reason the width must stay even.)
     * <p>
     * The vertical default is the style's, not the element's — see
     * {@link HealthStyle#defaultYOffset()}.
     */
    public static final int DEFAULT_X_OFFSET = BAR_WIDTH / 2 - 91;
    public static final String DEFAULT_ANCHOR = "BOTTOM_CENTER";

    private static final long FLASH_MS = 260;

    /**
     * Last derived pool value, only so a drop can be spotted. A pushed value
     * would need this too; deriving just makes it cheap.
     */
    private static double lastValue = -1;
    private static long lastDropMs = 0;

    private BeyonderHealthOverlay() {
    }

    /**
     * <b>The replacement has three answers, not two.</b> It used to be
     * {@code if (!render(ctx)) original.extractRenderState(...)}: ours drew, or
     * vanilla's did. {@link HealthStyle#HEARTS} needs <em>both</em> — the whole
     * point of that style is that vanilla keeps its hearts, absorption hearts
     * included, and the mod adds only the numbers they cannot carry — and the
     * hearts have to go down <em>first</em>, or they land on top of the
     * readout. A boolean return could express neither the third case nor the
     * ordering, so the displaced element is handed in as something this frame
     * may call, at the moment it decides.
     */
    public static void initialize() {
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (ctx, tickCounter) ->
                render(ctx, () -> original.extractRenderState(ctx, tickCounter)));
    }

    /**
     * @param vanillaHearts draws the element we displaced: called instead of us
     *                      when nothing of ours may draw, and before us when
     *                      the selected style keeps the hearts underneath
     */
    private static void render(GuiGraphicsExtractor ctx, Runnable vanillaHearts) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showBeyonderHealth
                || !BeyonderState.hasHealthData()) {
            vanillaHearts.run();
            return;
        }
        // Creative and spectator have no hearts to replace. Rather than
        // re-deriving vanilla's rule, hand the frame back and let the element
        // we displaced decide — it draws nothing, and it stays right if the
        // rule ever changes
        if (client.gameMode == null || !client.gameMode.canHurtPlayer()) {
            vanillaHearts.run();
            return;
        }

        double poolMax = BeyonderState.maxHealth();
        float vanillaMax = player.getMaxHealth();
        if (poolMax <= 0 || vanillaMax <= 0) {
            vanillaHearts.run();
            return;
        }

        HealthStyle style = HealthStyle.parse(settings.beyonderHealthStyle);
        if (style.drawsOverVanillaHearts()) vanillaHearts.run();

        // Read poolMax fresh every frame: True Form doubles it mid-fight, and
        // only the fill is allowed to animate — a lerped maximum would make the
        // readout disagree with the sheet
        float fraction = Mth.clamp(player.getHealth() / vanillaMax, 0f, 1f);
        double current = poolMax * fraction;
        double absorption = player.getAbsorptionAmount() * poolMax / vanillaMax;

        long now = System.currentTimeMillis();
        noteDrop(current, now);

        // Both cues are suppressed together: they are the two things on this
        // element that move on their own
        boolean calm = settings.epilepsyMode;
        float flash = calm ? 0f : flashAmount(now);
        float pulse = calm ? 0f : pulseAmount(fraction, now);

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int[] pos = anchor(w, h, settings);

        HudScale.push(ctx, pos[0], pos[1], settings.beyonderHealthScale);
        HealthBarPaint.draw(ctx, style, pos[0], pos[1], BAR_WIDTH, BAR_HEIGHT,
                current, poolMax, absorption, flash, pulse);
        HudScale.pop(ctx);
    }

    /**
     * Remembers when the pool last went down, which is what the damage flash
     * is timed from. Deriving the pool makes this cheap; a pushed value would
     * still need it.
     */
    private static void noteDrop(double current, long now) {
        if (lastValue >= 0 && current < lastValue - 0.001) lastDropMs = now;
        lastValue = current;
    }

    /**
     * 1 the instant damage lands, decaying to 0 over {@value #FLASH_MS}ms.
     */
    private static float flashAmount(long now) {
        long sinceDrop = now - lastDropMs;
        if (lastDropMs <= 0 || sinceDrop >= FLASH_MS) return 0f;
        return 1f - sinceDrop / (float) FLASH_MS;
    }

    /**
     * The low-health throb, standing in for the hearts' own shake. It starts
     * exactly where the fill finishes blending to the alarm colour, so the two
     * read as one cue — which is why the threshold is
     * {@link HealthBarPaint#LOW_FRACTION} rather than a second copy of it.
     */
    private static float pulseAmount(float fraction, long now) {
        if (fraction >= HealthBarPaint.LOW_FRACTION) return 0f;
        return (float) (0.5 + 0.5 * Math.sin(now * 0.009));
    }

    /**
     * Top-left corner of the box — the point the element scales about. The box
     * is {@link #BAR_WIDTH} wide whichever style is on, so switching style
     * never moves the element and the layout editor never has to ask which one
     * it is looking at.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.beyonderHealthAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.beyonderHealthScale),
                s.beyonderHealthXOffset, s.beyonderHealthYOffset, s.beyonderHealthYOffset);
    }

    /**
     * Switches style, <b>carrying the placement across only if the player never
     * moved it</b>.
     * <p>
     * The two sets of defaults sit twenty pixels apart, because the replacing
     * styles take the hearts' row and {@link HealthStyle#HEARTS} has to clear
     * both the hearts and vanilla's armour bar above them. So a player who never dragged this element must be moved with the
     * style, or picking Hearts drops the readout on top of the hearts it just
     * restored; and a player who <em>did</em> drag it must not be, or the
     * position they chose is snatched back the first time they look at the
     * other styles. "Never moved it" is the only signal there is for telling
     * the two apart, and it is exact: the defaults are three known values.
     */
    public static void applyStyle(HudConfig.HudSettings s, HealthStyle next) {
        boolean untouched = atDefaults(s, HealthStyle.parse(s.beyonderHealthStyle));
        s.beyonderHealthStyle = next.name();
        if (untouched) resetPosition(s, next);
    }

    /**
     * Whether the placement is still exactly what {@code style} ships with.
     */
    public static boolean atDefaults(HudConfig.HudSettings s, HealthStyle style) {
        return DEFAULT_ANCHOR.equals(s.beyonderHealthAnchor)
                && s.beyonderHealthXOffset == DEFAULT_X_OFFSET
                && s.beyonderHealthYOffset == style.defaultYOffset();
    }

    /**
     * Puts the element back where {@code style} ships it — the layout editor's
     * per-element reset, and half of {@link #applyStyle}.
     */
    public static void resetPosition(HudConfig.HudSettings s, HealthStyle style) {
        s.beyonderHealthAnchor = DEFAULT_ANCHOR;
        s.beyonderHealthXOffset = DEFAULT_X_OFFSET;
        s.beyonderHealthYOffset = style.defaultYOffset();
    }

    /**
     * Sample geometry for the layout editor, in whichever style is selected —
     * the editor is where a player compares them, so a preview that always drew
     * the bar would be showing them the wrong element. Deliberately takes
     * nothing from {@link BeyonderState} or the player: the editor runs over a
     * live session, and a preview that borrowed the real numbers would flicker
     * with every hit taken behind it.
     */
    public static void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s) {
        int[] pos = anchor(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.beyonderHealthScale);
        HealthBarPaint.draw(ctx, HealthStyle.parse(s.beyonderHealthStyle),
                pos[0], pos[1], BAR_WIDTH, BAR_HEIGHT, 1234, 1750, 175, 0f, 0f);
        HudScale.pop(ctx);
    }

    /**
     * Dropped on disconnect along with the rest of the client state, so the
     * next server's first drop is not measured against the last server's pool.
     */
    public static void reset() {
        lastValue = -1;
        lastDropMs = 0;
    }
}
