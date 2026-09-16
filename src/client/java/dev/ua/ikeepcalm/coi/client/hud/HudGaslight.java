package dev.ua.ikeepcalm.coi.client.hud;

import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;

import java.util.List;
import java.util.Random;

/**
 * At stage 3+ madness the HUD itself starts lying: a cooldown briefly shows
 * a wrong number, a keybind label glitches into nonsense glyphs, or two
 * slots quietly trade places for a heartbeat. Episodes are short (a few
 * hundred ms) and rare, so players doubt what they saw — that's the point.
 * <p>
 * Gated behind the same toggle as the rest of the hallucination family.
 */
public class HudGaslight {

    private enum Lie {NONE, COOLDOWN, KEYBIND, SWAP}

    private static final Random RANDOM = new Random();
    private static final String GLITCH_GLYPHS = "#%&@!?/\\";
    private static final double MIN_MADNESS = 75.0;

    private static Lie currentLie = Lie.NONE;
    private static long lieEndsAt = 0;
    private static long nextLieAt = 0;
    private static int targetSlot = -1;   // bind-slot index the lie applies to
    private static int swapWith = -1;     // second bind-slot index for SWAP
    private static String fakeCooldownText = "";
    private static int glitchCharIndex = 0;

    /**
     * Called once per HUD frame with the bind-slot indices currently visible.
     * Drives the episode state machine.
     */
    public static void update(List<Integer> visibleSlots) {
        long now = System.currentTimeMillis();

        double madness = ClientBeyonderState.getMadness();
        boolean enabled = HudConfig.getSettings().enableHallucinations
                && madness >= MIN_MADNESS
                && !visibleSlots.isEmpty();

        if (!enabled) {
            currentLie = Lie.NONE;
            nextLieAt = 0;
            return;
        }

        if (currentLie != Lie.NONE && now >= lieEndsAt) {
            currentLie = Lie.NONE;
        }

        if (nextLieAt == 0) {
            // Madness just crossed the threshold — schedule, don't fire instantly
            nextLieAt = now + nextIntervalMs(madness);
            return;
        }

        if (currentLie == Lie.NONE && now >= nextLieAt) {
            startLie(visibleSlots);
            nextLieAt = now + nextIntervalMs(madness);
        }
    }

    /** ~20s average at madness 75 down to ~8s at 100, ±50% jitter. */
    private static long nextIntervalMs(double madness) {
        double t = Math.min(1.0, (madness - MIN_MADNESS) / (100.0 - MIN_MADNESS));
        double base = 20_000 - t * 12_000;
        double jitter = 0.5 + RANDOM.nextDouble();
        return (long) (base * jitter);
    }

    private static void startLie(List<Integer> visibleSlots) {
        long now = System.currentTimeMillis();
        targetSlot = visibleSlots.get(RANDOM.nextInt(visibleSlots.size()));
        glitchCharIndex = RANDOM.nextInt(3);

        Lie lie = switch (RANDOM.nextInt(3)) {
            case 0 -> Lie.COOLDOWN;
            case 1 -> Lie.KEYBIND;
            default -> Lie.SWAP;
        };

        if (lie == Lie.SWAP) {
            if (visibleSlots.size() < 2) {
                lie = Lie.KEYBIND; // can't swap a lone slot
            } else {
                swapWith = targetSlot;
                while (swapWith == targetSlot) {
                    swapWith = visibleSlots.get(RANDOM.nextInt(visibleSlots.size()));
                }
            }
        }

        // A plausible-but-wrong cooldown: whole seconds or one decimal
        fakeCooldownText = RANDOM.nextBoolean()
                ? String.valueOf(7 + RANDOM.nextInt(80))
                : String.format("%.1f", RANDOM.nextDouble() * 9 + 0.5);

        currentLie = lie;
        lieEndsAt = now + 250 + RANDOM.nextInt(350);
    }

    /** Wrong cooldown text for this slot, or null to render the truth. */
    public static String cooldownOverride(int slotIndex) {
        if (currentLie == Lie.COOLDOWN && slotIndex == targetSlot) {
            return fakeCooldownText;
        }
        return null;
    }

    /** Corrupts one character of the keybind label while the lie lasts. */
    public static String corruptKeybind(int slotIndex, String keyText) {
        if (currentLie != Lie.KEYBIND || slotIndex != targetSlot || keyText.isEmpty()) {
            return keyText;
        }
        // Glyph flickers every ~80ms while the character position stays fixed
        char glyph = GLITCH_GLYPHS.charAt((int) ((System.currentTimeMillis() / 80) % GLITCH_GLYPHS.length()));
        int pos = Math.min(glitchCharIndex, keyText.length() - 1);
        return keyText.substring(0, pos) + glyph + keyText.substring(pos + 1);
    }

    /**
     * If this bind-slot should render another slot's content right now,
     * returns that other slot's index; otherwise -1.
     */
    public static int swappedWith(int slotIndex) {
        if (currentLie != Lie.SWAP) return -1;
        if (slotIndex == targetSlot) return swapWith;
        if (slotIndex == swapWith) return targetSlot;
        return -1;
    }
}
