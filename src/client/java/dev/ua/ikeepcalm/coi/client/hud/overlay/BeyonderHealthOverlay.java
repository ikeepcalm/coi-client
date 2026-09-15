package dev.ua.ikeepcalm.coi.client.hud.overlay;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.HudAnchor;
import dev.ua.ikeepcalm.coi.client.hud.HudGate;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.render.CoiBar;
import dev.ua.ikeepcalm.coi.client.state.BeyonderState;

import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Beyonder's real HP pool, drawn where the vanilla hearts were.
 * <p>
 * <b>This is the one HUD element in the mod that replaces a vanilla element
 * rather than attaching beside one.</b> Everything else calls
 * {@code attachElementBefore} and coexists with the vanilla HUD; this one takes
 * {@link VanillaHudElements#HEALTH_BAR} over, because ten hearts and a 1750-HP
 * pool cannot both be the truth about the same number. The replacement keeps a
 * reference to the element it displaced and delegates to it whenever our bar is
 * not drawing, so a vanilla server — or a player who switched the bar off —
 * gets the hearts back byte for byte.
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
 */
public final class BeyonderHealthOverlay {

    /**
     * The bar runs from the hotbar's left edge ({@code screenW / 2 - 91}) to
     * {@code screenW / 2 + 9} — <b>the widest it can be on this row</b>. The
     * hearts only reached {@code screenW / 2 - 10}, so replacing them freed the
     * 20px gap in the middle; the food bar starts at {@code screenW / 2 + 10}
     * and is the hard stop. A four-digit pool needs the room the hearts' own 82
     * did not leave it.
     * <p>
     * Being <b>even</b> is what makes the centring exact: {@link
     * HudAnchor#resolve} centres with {@code (screenW - barW) / 2}, and that
     * floor lands a pixel away from vanilla's {@code screenW / 2 - 91} when the
     * screen width is odd <em>and</em> the bar width is odd too.
     */
    public static final int BAR_WIDTH = 95;
    /**
     * 9, because {@link CoiBar#frame} draws its border <em>outside</em> the box
     * — the drawn footprint is {@code BAR_HEIGHT + 2}, and the heart row has
     * only {@code h-40 .. h-30} before the experience bar starts at
     * {@code h-29}. At 11 the bar was 13 rows tall and the XP bar, which draws
     * after us, clipped its bottom.
     */
    public static final int BAR_HEIGHT = 9;

    /**
     * Defaults that keep the bar on the hearts' own line, starting where they
     * started. {@code HudAnchor.resolve} centres with {@code (w - BAR_WIDTH) / 2},
     * so landing on vanilla's {@code w / 2 - 91} needs an offset of
     * {@code BAR_WIDTH / 2 - 91} — <b>derived, never typed</b>. Hard-coding it
     * once already went wrong: widening the bar from 82 to 100 left the old
     * {@code -50} behind and pushed every existing config 9px to the left.
     * (The identity holds for any <em>even</em> {@code BAR_WIDTH}, which is the
     * other reason the width must stay even.)
     */
    public static final int DEFAULT_X_OFFSET = BAR_WIDTH / 2 - 91;
    public static final int DEFAULT_Y_OFFSET = 39;
    public static final String DEFAULT_ANCHOR = "BOTTOM_CENTER";

    private static final int BORDER_COLOR = 0xCC000000;
    /**
     * The fill is not one red but a blend between two, chosen by how much pool
     * is left. A bar that is bright scarlet at 225/225 spends its loudest colour
     * on the one state that needs no attention, and leaves nothing to escalate
     * to — so full health sits at a deep, calm crimson and only a draining pool
     * climbs to the alarm colour.
     */
    private static final int CALM_TOP = 0xFFA83A3A;
    private static final int CALM_BOTTOM = 0xFF4E1212;
    private static final int ALERT_TOP = 0xFFFF4444;
    private static final int ALERT_BOTTOM = 0xFF8E1414;

    /**
     * Where the fill starts warming up. It reaches the alarm colour exactly at
     * {@link #LOW_FRACTION}, so the colour shift and the pulse are two halves of
     * one cue rather than two thresholds the player has to learn separately.
     */
    private static final float ALERT_START_FRACTION = 0.60f;
    /**
     * Absorption is vanilla and soaks real damage, but replacing the health
     * element takes vanilla's absorption hearts with it — so it is drawn here,
     * in gold, continuing past the main fill.
     */
    private static final int ABSORB_TOP = 0xFFFFD75E;
    private static final int ABSORB_BOTTOM = 0xFFB07C0C;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /**
     * Below this fraction the bar pulses, standing in for the hearts' own
     * low-health shake.
     */
    private static final float LOW_FRACTION = 0.30f;
    private static final long FLASH_MS = 260;

    /**
     * Last derived pool value, only so a drop can be spotted. A pushed value
     * would need this too; deriving just makes it cheap.
     */
    private static double lastValue = -1;
    private static long lastDropMs = 0;

    private BeyonderHealthOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (ctx, tickCounter) -> {
            if (!render(ctx)) original.extractRenderState(ctx, tickCounter);
        });
    }

    /**
     * @return true when the pool bar drew, i.e. when the caller must
     * <em>not</em> fall through to the vanilla hearts
     */
    private static boolean render(GuiGraphicsExtractor ctx) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        HudConfig.HudSettings settings = HudConfig.getSettings();

        if (HudGate.blocked(client, settings) || !settings.showBeyonderHealth
                || !BeyonderState.hasHealthData()) {
            return false;
        }
        // Creative and spectator have no hearts to replace. Rather than
        // re-deriving vanilla's rule, hand the frame back and let the element
        // we displaced decide — it draws nothing, and it stays right if the
        // rule ever changes
        if (client.gameMode == null || !client.gameMode.canHurtPlayer()) return false;

        double poolMax = BeyonderState.maxHealth();
        float vanillaMax = player.getMaxHealth();
        if (poolMax <= 0 || vanillaMax <= 0) return false;

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
        drawBarAt(ctx, pos[0], pos[1], current, poolMax, absorption, flash, pulse);
        HudScale.pop(ctx);
        return true;
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
     * read as one cue.
     */
    private static float pulseAmount(float fraction, long now) {
        if (fraction >= LOW_FRACTION) return 0f;
        return (float) (0.5 + 0.5 * Math.sin(now * 0.009));
    }

    /**
     * Top-left corner of the fill — the point the element scales about.
     */
    public static int[] anchor(int screenW, int screenH, HudConfig.HudSettings s) {
        return HudAnchor.parse(s.beyonderHealthAnchor).resolve(
                screenW, screenH, HudScale.size(BAR_WIDTH, s.beyonderHealthScale),
                s.beyonderHealthXOffset, s.beyonderHealthYOffset, s.beyonderHealthYOffset);
    }

    /**
     * The bar from explicit values, so the layout editor's preview and the live
     * overlay can never draw two different things.
     *
     * @param absorption already converted into pool units by the caller
     * @param flash      1 → 0 white wash right after a drop
     * @param pulse      0 → 1 → 0 brightening while the pool is low
     */
    public static void drawBarAt(GuiGraphicsExtractor ctx, int x, int y,
                                 double current, double max, double absorption, float flash, float pulse) {
        Font font = Minecraft.getInstance().font;

        CoiBar.frame(ctx, x, y, BAR_WIDTH, BAR_HEIGHT, BORDER_COLOR);

        int fillW = CoiBar.lerpWidth(current, max, BAR_WIDTH);
        float urgency = urgency(current, max);
        CoiBar.fill(ctx, x, y, BAR_HEIGHT, fillW,
                lerpColor(CALM_TOP, ALERT_TOP, urgency),
                lerpColor(CALM_BOTTOM, ALERT_BOTTOM, urgency));
        if (pulse > 0 && fillW > 0) {
            ctx.fill(x, y, x + fillW, y + BAR_HEIGHT, CoiBar.withAlpha(0x38FFFFFF, pulse));
        }

        // Gold continues where red stops, so the two read as one pool rather
        // than as a second bar
        int absorbW = Math.min(BAR_WIDTH - fillW, CoiBar.lerpWidth(absorption, max, BAR_WIDTH));
        if (absorbW > 0) {
            CoiBar.fill(ctx, x + fillW, y, BAR_HEIGHT, absorbW, ABSORB_TOP, ABSORB_BOTTOM);
        }

        if (flash > 0) {
            ctx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, CoiBar.withAlpha(0xB0FFFFFF, flash));
        }

        // Inside the fill, not above it: the bar has the hearts' footprint and
        // nothing to spare. The shadow is what keeps it legible over both the
        // lit and the empty half
        Component readout = readout(font, current, max);
        int textX = x + (BAR_WIDTH - font.width(readout)) / 2;
        ctx.text(font, readout, textX, y + (BAR_HEIGHT - font.lineHeight) / 2 + 1, TEXT_COLOR, true);
    }

    /**
     * The widest readout that still fits, measured rather than assumed: grouped
     * digits first, then ungrouped, then the current value alone. Four-digit
     * pools plus a scale of 0.5 make "1,234 / 1,750" wider than 81px, and a
     * number spilling past the frame looks broken in a way a plainer number
     * does not.
     */
    private static Component readout(Font font, double current, double max) {
        int room = BAR_WIDTH - 4;
        Component grouped = Component.translatable("hud.coi.health_readout",
                format(current, true), format(max, true));
        if (font.width(grouped) <= room) return grouped;

        Component plain = Component.translatable("hud.coi.health_readout",
                format(current, false), format(max, false));
        if (font.width(plain) <= room) return plain;

        return Component.translatable("hud.coi.health_current", format(current, false));
    }

    /**
     * 0 while the pool is comfortable, 1 once it is low — the blend between the
     * calm and the alarm fill.
     */
    private static float urgency(double current, double max) {
        if (max <= 0) return 0f;
        float fraction = Mth.clamp((float) (current / max), 0f, 1f);
        return Mth.clamp((ALERT_START_FRACTION - fraction)
                / (ALERT_START_FRACTION - LOW_FRACTION), 0f, 1f);
    }

    /**
     * Per-channel blend. Both ends are opaque, so alpha is taken from {@code a}
     * rather than interpolated.
     */
    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = ar + Math.round((br - ar) * t);
        int g = ag + Math.round((bg - ag) * t);
        int bl = ab + Math.round((bb - ab) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /**
     * Pool units are whole numbers to the player; the fraction only matters to
     * the mirror.
     */
    private static String format(double value, boolean grouped) {
        return String.format(Locale.ROOT, grouped ? "%,d" : "%d", Math.max(0L, Math.round(value)));
    }

    /**
     * Sample geometry for the layout editor. Deliberately takes nothing from
     * {@link BeyonderState} or the player: the editor runs over a live
     * session, and a preview that borrowed the real numbers would flicker with
     * every hit taken behind it.
     */
    public static void renderPreview(GuiGraphicsExtractor ctx, int w, int h, HudConfig.HudSettings s) {
        int[] pos = anchor(w, h, s);
        HudScale.push(ctx, pos[0], pos[1], s.beyonderHealthScale);
        drawBarAt(ctx, pos[0], pos[1], 1234, 1750, 175, 0f, 0f);
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
