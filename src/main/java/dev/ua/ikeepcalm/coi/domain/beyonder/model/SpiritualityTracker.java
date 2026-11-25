package dev.ua.ikeepcalm.coi.domain.beyonder.model;

/**
 * Spirituality as the HUD needs it: the server's last figure, plus enough history to draw
 * between two packets.
 * <p>
 * The plugin raises spirituality in once-a-second steps, so the rate is measured off the last
 * two increases and the fill extrapolated from there. The measurement is thrown away on any
 * decrease — spending is not a rate — and capped both in speed ({@code max / 2} per second) and
 * in how far ahead it may run, so one odd packet cannot send the bar racing.
 */
final class SpiritualityTracker {

    /**
     * Cap on how far ahead of the last increase the prediction may run.
     */
    private static final long PREDICT_WINDOW_MS = 1500;

    /** No conditions packet for this long: drop back to the raw value. */
    private static final long STALE_PACKET_MS = 5000;

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
    private double lastRatePerMs;

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

    /** 1 → 0 over {@value #FLASH_MS} ms after spirituality drops. */
    float flashIntensity() {
        long elapsed = System.currentTimeMillis() - lastDecreaseMs;
        if (lastDecreaseMs == 0 || elapsed >= FLASH_MS) return 0.0f;
        return 1.0f - (elapsed / (float) FLASH_MS);
    }

    /** Milliseconds since spirituality last went <em>up</em>, or a large number if it never has. */
    long gainAgeMs() {
        if (lastIncreaseMs == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastIncreaseMs;
    }

    /**
     * Falls back to the raw value while not regenerating, before a rate is known, or once the
     * server has gone quiet, and never reads below {@link #current()} or above {@link #max()}.
     */
    double predicted() {
        if (!regen || regenPerMs <= 0 || lastIncreaseMs == 0) return current;
        long now = System.currentTimeMillis();
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

    /** Δvalue / Δtime across the last two increases, clamped to half the pool per second. */
    private void measureRate(int nextCurrent, int nextMax, long now) {
        if (lastIncreaseMs > 0 && now > lastIncreaseMs) {
            double rate = (nextCurrent - lastIncreaseValue) / (double) (now - lastIncreaseMs);
            rate = Math.clamp(rate, 0.0, Math.max(1, nextMax) / 2.0 / 1000.0);
            // A one-off burst (Spiritualism's 1% every 5 s) must not set the pace: running ahead on it
            // made the bar climb past the next packet and snap back. Keep the slower of the last two.
            regenPerMs = lastRatePerMs > 0 ? Math.min(rate, lastRatePerMs) : rate;
            lastRatePerMs = rate;
        }
        lastIncreaseMs = now;
        lastIncreaseValue = nextCurrent;
    }

    private void forgetRate() {
        regenPerMs = 0.0;
        lastRatePerMs = 0.0;
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
