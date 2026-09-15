package dev.ua.ikeepcalm.coi.client.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Second stage of gesture recognition: turns a resampled stroke into a short string of 8-way
 * direction codes ({@code '0'}–{@code '7'}), which is what the templates are written in and what
 * the Levenshtein match runs over.
 *
 * <p>Almost all of this class is denoising, and that is the point — a quantized hand-drawn stroke is
 * far noisier than the shape the player thinks they drew. Three separate corrections apply, each
 * fixing a different way a real stroke lies:
 *
 * <ul>
 *   <li><b>Oscillation</b> — a stroke running along a sector boundary flickers between the two
 *       directions either side of it;
 *   <li><b>Dominance</b> — a straight line picks up small entry and exit hooks from the wrist;
 *   <li><b>Short runs</b> — corners produce a sample or two of a direction nobody intended.
 * </ul>
 */
public class DirectionCodes {

    /**
     * A stroke with this share of its samples in a single direction is read as a straight line.
     */
    private static final float DOMINANT_RUN_FRACTION = 0.7f;
    /**
     * Runs shorter than this are dropped as corner noise, unless that would leave nothing at all.
     */
    private static final int MIN_SIGNIFICANT_RUN = 2;
    /**
     * Alternating runs must span at least this many before the block counts as oscillation rather
     * than a genuine change of direction.
     */
    private static final int MIN_OSCILLATION_RUNS = 3;

    private DirectionCodes() {
    }

    /**
     * Quantizes each segment to an 8-way code, collapses runs, and applies the three denoising
     * corrections described on this class. Empty when the stroke had no movement in it at all.
     */
    static String of(List<GestureRecognizer.StrokePoint> pts) {
        List<int[]> runs = mergeOscillations(quantize(pts));

        String line = asStraightLine(runs);
        if (line != null) return line;

        String denoised = collapseRuns(runs, MIN_SIGNIFICANT_RUN);
        return denoised.isEmpty() ? collapseRuns(runs, 1) : denoised;
    }

    /**
     * Each segment's heading as an 8-way code, run-length encoded into {@code [direction, count]}
     * pairs. Zero-length segments contribute nothing rather than an arbitrary angle.
     */
    private static List<int[]> quantize(List<GestureRecognizer.StrokePoint> pts) {
        List<int[]> runs = new ArrayList<>();
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
        return runs;
    }

    /**
     * The single dominant direction when a stroke is overwhelmingly one way — which ignores the
     * small entry/exit hooks a hand leaves at the ends of a line. Null when no direction dominates.
     */
    private static String asStraightLine(List<int[]> runs) {
        int total = 0;
        int[] dominant = null;
        for (int[] run : runs) {
            total += run[1];
            if (dominant == null || run[1] > dominant[1]) dominant = run;
        }
        if (dominant != null && dominant[1] >= total * DOMINANT_RUN_FRACTION) {
            return String.valueOf((char) ('0' + dominant[0]));
        }
        return null;
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
            if (b != -1 && j - i >= MIN_OSCILLATION_RUNS) {
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

    /**
     * Whether two direction codes are neighbours on the 8-way compass.
     */
    private static boolean adjacent(int a, int b) {
        int diff = Math.floorMod(a - b, 8);
        return diff == 1 || diff == 7;
    }

    /**
     * Runs of at least {@code minRun} samples, as a string with consecutive duplicates removed.
     */
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
}
