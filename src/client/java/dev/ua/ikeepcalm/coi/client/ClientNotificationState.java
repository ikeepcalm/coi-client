package dev.ua.ikeepcalm.coi.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.client.effects.impl.EffectPaint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Toast queue for {@code coi-client:notify} — sequence advancements, acting
 * method successes, bounty completions, madness stage crossings.
 * <p>
 * At most {@link #MAX_VISIBLE} toasts are on screen; the rest wait in a queue
 * and are promoted (with a fresh {@code shownAt}) as slots free up, so a burst
 * of events reads as a sequence rather than a pile.
 */
public final class ClientNotificationState {

    public static final int MAX_VISIBLE = 3;
    public static final long SLIDE_MS = 250;
    public static final long FADE_MS = 300;

    private static final int DEFAULT_ARGB = 0xFFFFD870;
    private static final long DEFAULT_DURATION_MS = 4000;

    /**
     * One toast. {@code shownAt} is 0 until the toast is promoted out of the
     * queue, which is when its timeline starts.
     */
    public static final class Toast {
        private final String kind;
        private final String title;
        private final String body;
        private final int argb;
        private final long durationMs;
        private long shownAt;

        Toast(String kind, String title, String body, int argb, long durationMs) {
            this.kind = kind;
            this.title = title;
            this.body = body;
            this.argb = argb;
            this.durationMs = durationMs;
        }

        public String kind() {
            return kind;
        }

        public String title() {
            return title;
        }

        public String body() {
            return body;
        }

        public int argb() {
            return argb;
        }

        long endsAt() {
            return shownAt + SLIDE_MS + durationMs + FADE_MS;
        }

        /**
         * Opacity across slide-in (or fade-in under epilepsy mode), hold and
         * fade-out.
         */
        public float alpha(long now, boolean epilepsyMode) {
            long elapsed = now - shownAt;
            if (elapsed < SLIDE_MS) return epilepsyMode ? elapsed / (float) SLIDE_MS : 1f;
            long fadeStart = SLIDE_MS + durationMs;
            if (elapsed < fadeStart) return 1f;
            return Math.clamp(1f - (elapsed - fadeStart) / (float) FADE_MS, 0f, 1f);
        }

        /**
         * How far off the right edge the card still is, in pixels. Always 0
         * under epilepsy mode, which fades instead of sliding.
         */
        public int slideOffset(long now, int width, boolean epilepsyMode) {
            long elapsed = now - shownAt;
            if (epilepsyMode || elapsed >= SLIDE_MS) return 0;
            float t = EffectPaint.easeOutCubic(elapsed / (float) SLIDE_MS);
            return Math.round(width * (1f - t));
        }
    }

    private static final Deque<Toast> pending = new ArrayDeque<>();
    private static final List<Toast> visible = new ArrayList<>();

    private ClientNotificationState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            push(new Toast(
                    string(root, "kind", "info"),
                    string(root, "title", ""),
                    string(root, "body", ""),
                    parseColor(string(root, "color", "")),
                    root.has("durationMs") ? root.get("durationMs").getAsLong() : DEFAULT_DURATION_MS));
        } catch (Exception e) {
            System.err.println("COI Client: malformed notify payload: " + json);
        }
    }

    private static String string(JsonObject root, String key, String fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback;
    }

    /**
     * {@code "RRGGBB"} (with or without a leading {@code #}) → opaque ARGB.
     */
    private static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT_ARGB;
        try {
            return 0xFF000000 | (Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return DEFAULT_ARGB;
        }
    }

    private static void push(Toast toast) {
        synchronized (pending) {
            pending.addLast(toast);
        }
    }

    /**
     * Retires finished toasts, promotes queued ones into the free slots and
     * returns what should be drawn this frame.
     */
    public static List<Toast> visibleToasts(long now) {
        synchronized (pending) {
            visible.removeIf(toast -> now >= toast.endsAt());
            while (visible.size() < MAX_VISIBLE && !pending.isEmpty()) {
                Toast toast = pending.removeFirst();
                toast.shownAt = now;
                visible.add(toast);
            }
            return List.copyOf(visible);
        }
    }

    /**
     * Debug-screen entry point: a gold advancement toast.
     */
    public static void debugToast() {
        push(new Toast("advancement", "Sequence 8 Reached",
                "You have digested the potion and become a Sun pathway Beyonder.",
                0xFFFFD870, DEFAULT_DURATION_MS));
    }

    public static void reset() {
        synchronized (pending) {
            pending.clear();
            visible.clear();
        }
    }
}
