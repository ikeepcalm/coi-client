package dev.ua.ikeepcalm.coi.domain.glyph;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * What the player has drawn so far, one stroke list per layer, in canvas units
 * ({@code 0..}{@link #CANVAS}, y down). Strokes are thinned as they are
 * finished, so a whole eight-layer spell stays far below the submit limits.
 */
public final class GlyphDrawing {

    public static final int CANVAS = 1023;
    /**
     * A point is kept once it is this far from the last kept one.
     */
    private static final double MIN_SPACING = 12;
    /**
     * Enough to describe any glyph, and four times under the server's budget per layer.
     */
    private static final int MAX_STROKE_POINTS = 64;

    /**
     * {@code xy} is flat {@code [x,y,…]}; {@code t0}/{@code t1} are ms since the canvas opened.
     */
    public record Stroke(int[] xy, long t0, long t1) {
        public int points() {
            return xy.length / 2;
        }
    }

    private final List<List<Stroke>> layers = new ArrayList<>();
    private final GlyphSheet.Limits limits;

    public GlyphDrawing(int layerCount, GlyphSheet.Limits limits) {
        this.limits = limits;
        for (int i = 0; i < layerCount; i++) layers.add(new ArrayList<>());
    }

    public List<Stroke> layer(int index) {
        return layers.get(index);
    }

    public int layerCount() {
        return layers.size();
    }

    /**
     * Adds a finished stroke.
     *
     * @param raw the cursor samples in canvas units
     * @return null when added (or too small to matter), else the lang key of why not
     */
    public String add(int layer, List<double[]> raw, long t0, long t1) {
        int[] xy = thin(raw);
        if (xy.length < 4) return null;
        if (layers.get(layer).size() >= limits.strokes()) return "screen.coi.glyph_too_many_strokes";
        if (totalPoints() + xy.length / 2 > limits.total()) return "screen.coi.glyph_out_of_ink";
        layers.get(layer).add(new Stroke(xy, t0, Math.max(t0, t1)));
        return null;
    }

    public void undo(int layer) {
        List<Stroke> strokes = layers.get(layer);
        if (!strokes.isEmpty()) strokes.removeLast();
    }

    public void clear(int layer) {
        layers.get(layer).clear();
    }

    /**
     * The last layer with ink, or -1 when nothing is drawn.
     */
    public int lastInkedLayer() {
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (!layers.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /**
     * The {@code coi-client:glyph_submit} body. Layers run up to the last one
     * with ink; an empty one in between is sent as is and refused by the server.
     */
    public String toJson(String session, String dict, String name) {
        JsonArray layersJson = new JsonArray();
        for (int i = 0; i <= lastInkedLayer(); i++) {
            JsonArray strokes = new JsonArray();
            for (Stroke stroke : layers.get(i)) {
                JsonArray flat = new JsonArray();
                for (int v : stroke.xy()) flat.add(v);
                JsonObject json = new JsonObject();
                json.add("p", flat);
                json.addProperty("t0", stroke.t0());
                json.addProperty("t1", stroke.t1());
                strokes.add(json);
            }
            JsonObject layer = new JsonObject();
            layer.add("strokes", strokes);
            layersJson.add(layer);
        }
        JsonObject root = new JsonObject();
        root.addProperty("session", session);
        root.addProperty("dict", dict);
        root.addProperty("name", name);
        root.add("layers", layersJson);
        return root.toString();
    }

    private int totalPoints() {
        int total = 0;
        for (List<Stroke> layer : layers) {
            for (Stroke stroke : layer) total += stroke.points();
        }
        return total;
    }

    /**
     * Drops samples closer than {@link #MIN_SPACING} to the last kept one, then
     * evenly picks at most the per-stroke cap, keeping both ends.
     */
    private int[] thin(List<double[]> raw) {
        List<double[]> kept = new ArrayList<>();
        for (double[] p : raw) {
            if (kept.isEmpty() || Math.hypot(p[0] - kept.getLast()[0], p[1] - kept.getLast()[1]) >= MIN_SPACING) {
                kept.add(p);
            }
        }
        if (!raw.isEmpty() && kept.getLast() != raw.getLast()) kept.add(raw.getLast());
        int cap = Math.min(MAX_STROKE_POINTS, limits.points());
        int n = Math.min(kept.size(), cap);
        int[] xy = new int[n * 2];
        for (int i = 0; i < n; i++) {
            double[] p = kept.get(n == 1 ? 0 : (int) Math.round(i * (kept.size() - 1) / (double) (n - 1)));
            xy[i * 2] = (int) Math.clamp(Math.round(p[0]), 0, CANVAS);
            xy[i * 2 + 1] = (int) Math.clamp(Math.round(p[1]), 0, CANVAS);
        }
        return xy;
    }
}
