package dev.ua.ikeepcalm.coi.client.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Direction-sequence gesture matcher. The stroke is filtered, normalized to a
 * unit box, resampled to evenly spaced points, converted into a collapsed
 * string of 8-way direction codes, and matched against each template variant
 * with Levenshtein distance. Simple, debuggable, language-free.
 */
public class GestureRecognizer {

    public record StrokePoint(float x, float y) {}

    /**
     * Raw cursor samples closer than this (GUI px) are dropped.
     */
    public static final float MIN_POINT_DISTANCE = 4f;
    private static final int RESAMPLE_COUNT = 32;

    /**
     * Strokes with a bounding-box diagonal below this fizzle silently.
     */
    private static final float MIN_STROKE_DIAGONAL = 24f;
    /**
     * A stroke this much flatter in one axis than the other is treated as 1D
     * and scaled uniformly, so normalizing a near-vertical line doesn't blow
     * horizontal jitter up to full box width.
     */
    private static final float ONE_DIMENSIONAL_RATIO = 0.2f;
    /**
     * A filtered stroke shorter than this has no shape left to read.
     */
    private static final int MIN_FILTERED_POINTS = 3;
    /**
     * Neighbor-averaging passes applied before resampling.
     */
    private static final int SMOOTHING_PASSES = 2;
    /**
     * A candidate is rejected outright once its edit distance exceeds the longer string's length
     * divided by this — a cheap cutoff that stops a wildly wrong template winning on score alone.
     */
    private static final float MAX_EDIT_DIVISOR = 3f;

    private GestureRecognizer() {
    }

    /**
     * Whether the stroke is big enough to be an intentional gesture at all.
     * Below this the screen closes silently instead of showing a fail.
     */
    public static boolean isMeaningful(List<StrokePoint> raw) {
        if (raw.size() < 4) return false;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (StrokePoint p : raw) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        float w = maxX - minX;
        float h = maxY - minY;
        return Math.sqrt(w * w + h * h) >= MIN_STROKE_DIAGONAL;
    }

    /**
     * Runs the three stages in order — resample, direction string, match — and returns the
     * best-matching gesture, or null if nothing matches well enough (the stroke fizzles).
     */
    public static GestureType recognize(List<StrokePoint> raw) {
        List<StrokePoint> stroke = filter(raw);
        if (stroke.size() < MIN_FILTERED_POINTS) return null;

        List<StrokePoint> resampled = resample(normalize(smooth(stroke)));
        String directions = DirectionCodes.of(resampled);
        if (directions.isEmpty()) return null;

        return bestMatch(directions);
    }

    /**
     * Third stage: the closest template by Levenshtein distance, normalized by length so a long
     * template isn't penalized for having more places to differ. Null when nothing clears
     * {@link #MAX_EDIT_DIVISOR}.
     */
    private static GestureType bestMatch(String directions) {
        GestureType best = null;
        float bestScore = Float.MAX_VALUE;
        for (GestureType type : GestureType.values()) {
            for (String variant : type.templateVariants()) {
                int maxLen = Math.max(variant.length(), directions.length());
                int distance = levenshtein(variant, directions);
                if (distance > Math.max(1, Math.round(maxLen / MAX_EDIT_DIVISOR))) continue;
                float score = distance / (float) maxLen;
                if (score < bestScore) {
                    bestScore = score;
                    best = type;
                }
            }
        }
        return best;
    }

    private static List<StrokePoint> filter(List<StrokePoint> raw) {
        List<StrokePoint> out = new ArrayList<>();
        for (StrokePoint p : raw) {
            if (out.isEmpty() || distance(out.getLast(), p) >= MIN_POINT_DISTANCE) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Two passes of neighbor averaging on the raw stroke. Kills pixel-scale
     * cursor jitter while corners stay sharp — the rounding radius is on the
     * order of the 4px sample spacing, tiny relative to the whole shape.
     * (Smoothing after resampling instead would round corners so far that
     * triangles start reading as circles.)
     */
    private static List<StrokePoint> smooth(List<StrokePoint> pts) {
        for (int pass = 0; pass < SMOOTHING_PASSES; pass++) {
            List<StrokePoint> out = new ArrayList<>(pts.size());
            out.add(pts.getFirst());
            for (int i = 1; i < pts.size() - 1; i++) {
                out.add(new StrokePoint(
                        (pts.get(i - 1).x() + pts.get(i).x() + pts.get(i + 1).x()) / 3f,
                        (pts.get(i - 1).y() + pts.get(i).y() + pts.get(i + 1).y()) / 3f));
            }
            out.add(pts.getLast());
            pts = out;
        }
        return pts;
    }

    private static List<StrokePoint> normalize(List<StrokePoint> pts) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (StrokePoint p : pts) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        float w = maxX - minX;
        float h = maxY - minY;

        float scaleX, scaleY;
        if (w < h * ONE_DIMENSIONAL_RATIO) {
            scaleX = scaleY = 1f / h;
        } else if (h < w * ONE_DIMENSIONAL_RATIO) {
            scaleX = scaleY = 1f / w;
        } else {
            scaleX = 1f / w;
            scaleY = 1f / h;
        }

        List<StrokePoint> out = new ArrayList<>(pts.size());
        for (StrokePoint p : pts) {
            out.add(new StrokePoint((p.x() - minX) * scaleX, (p.y() - minY) * scaleY));
        }
        return out;
    }

    /**
     * First stage: re-places the points at even arc-length intervals, so the direction codes that
     * follow measure the shape rather than how fast the hand happened to be moving.
     */
    private static List<StrokePoint> resample(List<StrokePoint> pts) {
        float pathLength = 0;
        for (int i = 1; i < pts.size(); i++) {
            pathLength += distance(pts.get(i - 1), pts.get(i));
        }
        float interval = pathLength / (RESAMPLE_COUNT - 1);
        if (interval <= 0) return pts;

        List<StrokePoint> out = new ArrayList<>(RESAMPLE_COUNT);
        out.add(pts.getFirst());
        float accumulated = 0;
        List<StrokePoint> work = new ArrayList<>(pts);
        for (int i = 1; i < work.size(); i++) {
            StrokePoint prev = work.get(i - 1);
            StrokePoint curr = work.get(i);
            float segment = distance(prev, curr);
            if (accumulated + segment >= interval && segment > 0) {
                float t = (interval - accumulated) / segment;
                StrokePoint inserted = new StrokePoint(
                        prev.x() + t * (curr.x() - prev.x()),
                        prev.y() + t * (curr.y() - prev.y()));
                out.add(inserted);
                work.add(i, inserted);
                accumulated = 0;
            } else {
                accumulated += segment;
            }
        }
        if (out.size() < RESAMPLE_COUNT) out.add(pts.getLast());
        return out;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private static float distance(StrokePoint a, StrokePoint b) {
        float dx = b.x() - a.x();
        float dy = b.y() - a.y();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
