package dev.ua.ikeepcalm.coi.client.state;

/**
 * Spirituality as the HUD needs it: the server's last figure, plus enough
 * history to draw between two packets.
 * <p>
 * The plugin raises spirituality in once-a-second steps. Drawn raw that reads
 * as a bar that jerks forward and then sits still, so the rate is measured off
 * the last two increases and the fill is extrapolated from there. The
 * measurement is thrown away on any decrease — spending is not a rate — and
 * capped both in speed ({@code max / 2} per second) and in how far ahead of the
 * last increase it may run, so one odd packet cannot send the bar racing.
 * <p>
 * Nothing here is known until a protocol-2 server actually sends the keys:
 * {@link #hasData()} stays false, and the bar draws nothing rather than 0/0.
 */
final class SpiritualityTracker {

    /**
     * Cap on how far ahead of the last increase the prediction may run.
     */
    private static final long PREDICT_WINDOW_MS = 1500;

    /**
     * No conditions packet for this long: drop back to the raw value.
     */
    private static final long STALE_PACKET_MS = 5000;

    /**
     * How long the drop flash takes to fade, mirroring the madness flash.
     */
    private static final long FLASH_MS = 500;

    private int current;
    private int max;
    private boolean regen;
    private boolean hasData;
    private long lastDecreaseMs;

    private long lastPacketMs;
    private long lastIncreaseMs;
    private int lastIncreaseValue;
    private double regenPerMs;

    int current() {
        return current;
    }

    int max() {
        return max;
    }

    boolean regenerating() {
        return regen;
    }

    boolean hasData() {
        return hasData;
    }

    /**
     * 1 → 0 over {@value #FLASH_MS} ms after spirituality drops.
     */
    float flashIntensity() {
        long elapsed = System.currentTimeMillis() - lastDecreaseMs;
        if (lastDecreaseMs == 0 || elapsed >= FLASH_MS) return 0.0f;
        return 1.0f - (elapsed / (float) FLASH_MS);
    }

    /**
     * Milliseconds since spirituality last went <em>up</em>, or a large number
     * if it never has. Tying the HUD's edge flare to gains (not to every
     * {@code conditions} packet) keeps it a pulse rather than a constant glow.
     */
    long gainAgeMs() {
        if (lastIncreaseMs == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastIncreaseMs;
    }

    /**
     * Spirituality extrapolated forward from the last increase at the measured
     * regen rate. Falls back to the raw value while not regenerating, before a
     * rate is known, or once the server has gone quiet, and never reads below
     * {@link #current()} or above {@link #max()}.
     */
    double predicted() {
        if (!regen || regenPerMs <= 0 || lastIncreaseMs == 0) return current;
        long now = System.currentTimeMillis();
        // Gone quiet: stop guessing rather than drift away from the server
        if (now - lastPacketMs > STALE_PACKET_MS) return current;
        long dt = Math.min(PREDICT_WINDOW_MS, now - lastIncreaseMs);
        if (dt <= 0) return current;
        double predicted = lastIncreaseValue + regenPerMs * dt;
        return Math.max(current, Math.min(max, predicted));
    }

    void update(int nextCurrent, int nextMax, boolean nextRegen) {
        long now = System.currentTimeMillis();
        nextCurrent = Math.max(0, nextCurrent);
        nextMax = Math.max(0, nextMax);
        if (hasData && nextCurrent < current) {
            lastDecreaseMs = now;
            forgetRate();
        } else if (hasData && nextCurrent > current && nextRegen) {
            measureRate(nextCurrent, nextMax, now);
        }
        current = nextCurrent;
        max = nextMax;
        regen = nextRegen;
        hasData = true;
        lastPacketMs = now;
    }

    /**
     * Δvalue / Δtime across the last two increases, clamped to at most half the
     * pool per second.
     */
    private void measureRate(int nextCurrent, int nextMax, long now) {
        if (lastIncreaseMs > 0 && now > lastIncreaseMs) {
            double rate = (nextCurrent - lastIncreaseValue) / (double) (now - lastIncreaseMs);
            regenPerMs = Math.clamp(rate, 0.0, Math.max(1, nextMax) / 2.0 / 1000.0);
        }
        lastIncreaseMs = now;
        lastIncreaseValue = nextCurrent;
    }

    private void forgetRate() {
        regenPerMs = 0.0;
        lastIncreaseMs = 0;
        lastIncreaseValue = 0;
    }

    void reset() {
        current = 0;
        max = 0;
        regen = false;
        hasData = false;
        lastDecreaseMs = 0;
        lastPacketMs = 0;
        forgetRate();
    }
}
