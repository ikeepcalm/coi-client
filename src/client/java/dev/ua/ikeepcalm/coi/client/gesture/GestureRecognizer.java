package dev.ua.ikeepcalm.coi.client.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Direction-sequence gesture matcher. The stroke is filtered, normalized to a
 * unit box, resampled to evenly spaced points, converted into a collapsed
 * string of 8-way direction codes, and matched against each template variant
 * with Levenshtein distance. Simple, debuggable, language-free.
 */
public final class GestureRecognizer {

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
     * Returns the best-matching gesture, or null if nothing matches well
     * enough (the stroke fizzles).
     */
    public static GestureType recognize(List<StrokePoint> raw) {
        List<StrokePoint> pts = filter(raw);
        if (pts.size() < 3) return null;
        pts = resample(normalize(smooth(pts)));

        String input = directionString(pts);
        if (input.isEmpty()) return null;

        GestureType best = null;
        float bestScore = Float.MAX_VALUE;
        for (GestureType type : GestureType.values()) {
            for (String variant : type.templateVariants()) {
                int maxLen = Math.max(variant.length(), input.length());
                int dist = levenshtein(variant, input);
                if (dist > Math.max(1, Math.round(maxLen / 3f))) continue;
                float score = dist / (float) maxLen;
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
        for (int pass = 0; pass < 2; pass++) {
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

    /**
     * Quantizes each segment to an 8-way code, then collapses runs. Runs of a
     * single sample are treated as corner noise and dropped (unless that would
     * leave nothing).
     */
    private static String directionString(List<StrokePoint> pts) {
        List<int[]> runs = new ArrayList<>(); // [direction, count]
        for (int i = 1; i < pts.size(); i++) {
            float dx = pts.get(i).x() - pts.get(i - 1).x();
            float dy = pts.get(i).y() - pts.get(i - 1).y();
            if (dx == 0 && dy == 0) continue;
            int dir = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dy, dx)) / 45.0), 8);
            if (!runs.isEmpty() && runs.getLast()[0] == dir) {
                runs.getLast()[1]++;
            } else {
                runs.add(new int[]{dir, 1});
            }
        }

        runs = mergeOscillations(runs);

        // A stroke overwhelmingly in one direction is a line — ignore the
        // small entry/exit hooks a hand leaves at the ends
        int total = 0;
        int[] dominant = null;
        for (int[] run : runs) {
            total += run[1];
            if (dominant == null || run[1] > dominant[1]) dominant = run;
        }
        if (dominant != null && dominant[1] >= total * 0.7f) {
            return String.valueOf((char) ('0' + dominant[0]));
        }

        String denoised = collapseRuns(runs, 2);
        return denoised.isEmpty() ? collapseRuns(runs, 1) : denoised;
    }

    /**
     * A stroke along an 8-way sector boundary flickers between the two
     * adjacent directions (e.g. 7,6,7,6). Blocks of 3+ runs alternating
     * between two adjacent directions collapse into the dominant one.
     * (Two long adjacent runs — as around a circle — are NOT oscillation
     * and pass through untouched.)
     */
    private static List<int[]> mergeOscillations(List<int[]> runs) {
        List<int[]> out = new ArrayList<>(runs.size());
        int i = 0;
        while (i < runs.size()) {
            int a = runs.get(i)[0];
            int b = -1;
            int j = i + 1;
            while (j < runs.size()) {
                int d = runs.get(j)[0];
                if (b == -1) {
                    if (d != a && adjacent(d, a)) b = d;
                    else break;
                } else if (d != a && d != b) {
                    break;
                }
                j++;
            }
            if (b != -1 && j - i >= 3) {
                int countA = 0, countB = 0;
                for (int k = i; k < j; k++) {
                    int[] run = runs.get(k);
                    if (run[0] == a) countA += run[1];
                    else countB += run[1];
                }
                out.add(new int[]{countA >= countB ? a : b, countA + countB});
                i = j;
            } else {
                out.add(runs.get(i));
                i++;
            }
        }
        return out;
    }

    private static boolean adjacent(int a, int b) {
        int diff = Math.floorMod(a - b, 8);
        return diff == 1 || diff == 7;
    }

    private static String collapseRuns(List<int[]> runs, int minRun) {
        StringBuilder sb = new StringBuilder();
        for (int[] run : runs) {
            if (run[1] < minRun) continue;
            char c = (char) ('0' + run[0]);
            if (sb.isEmpty() || sb.charAt(sb.length() - 1) != c) {
                sb.append(c);
            }
        }
        return sb.toString();
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
