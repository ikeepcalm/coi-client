package dev.ua.ikeepcalm.coi.domain.glyph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.network.payload.CoiPayloads;
import dev.ua.ikeepcalm.coi.util.JsonRead;

import java.util.ArrayList;
import java.util.List;

/**
 * The canvas the server opened on {@code coi-client:glyph}: the session to
 * answer, the dictionary key to echo, how many layers may be drawn, the limits
 * a submit must stay inside, and the reference glyphs. The client keeps no
 * glyph data of its own, so it can never drift from the server's dictionary.
 */
public record GlyphSheet(String session, String dict, int maxLayers, Limits limits, List<Glyph> glyphs) {

    private static final int MAX_LAYERS = 8;
    private static final int MAX_GLYPHS = 64;
    private static final int MAX_GLYPH_STROKES = 8;
    private static final int MAX_GLYPH_NUMBERS = 512;

    /**
     * Per layer strokes, per stroke points, points in total, submit bytes, name length.
     */
    public record Limits(int strokes, int points, int total, int bytes, int name) {
    }

    /**
     * One reference entry. {@code strokes} are flat {@code [x,y,…]} in
     * hundredths, y down; a sign is shown as it sits at the top slot.
     * {@code flow} is {@code out}, {@code in} or {@code any}.
     */
    public record Glyph(String id, boolean sign, String name, String flow, List<int[]> strokes) {
    }

    /**
     * Defensive like every other channel: anything missing or out of range is
     * clamped, and null comes back only when there is no session to answer.
     */
    public static GlyphSheet parse(JsonObject root) {
        String session = JsonRead.string(root, "session", "");
        if (session.isEmpty()) return null;
        JsonObject limits = JsonRead.object(root, "limits");
        if (limits == null) limits = new JsonObject();
        Limits parsed = new Limits(
                Math.clamp(JsonRead.intOf(limits, "strokes", 24), 1, 64),
                Math.clamp(JsonRead.intOf(limits, "points", 128), 2, 256),
                Math.clamp(JsonRead.intOf(limits, "total", 2048), 16, 4096),
                Math.clamp(JsonRead.intOf(limits, "bytes", CoiPayloads.MAX_ACTION), 1024, CoiPayloads.MAX_ACTION),
                Math.clamp(JsonRead.intOf(limits, "name", 64), 1, 64));
        return new GlyphSheet(session, JsonRead.string(root, "dict", ""),
                Math.clamp(JsonRead.intOf(root, "maxLayers", 1), 1, MAX_LAYERS), parsed, glyphs(root));
    }

    private static List<Glyph> glyphs(JsonObject root) {
        List<Glyph> out = new ArrayList<>();
        if (!root.has("glyphs") || !root.get("glyphs").isJsonArray()) return out;
        for (JsonElement element : root.getAsJsonArray("glyphs")) {
            if (out.size() >= MAX_GLYPHS) break;
            if (!element.isJsonObject()) continue;
            JsonObject json = element.getAsJsonObject();
            out.add(new Glyph(JsonRead.string(json, "id", "?"), "sign".equals(JsonRead.string(json, "kind", "")),
                    JsonRead.string(json, "name", "?"), JsonRead.string(json, "flow", "any"), strokes(json)));
        }
        return out;
    }

    private static List<int[]> strokes(JsonObject json) {
        List<int[]> out = new ArrayList<>();
        if (!json.has("strokes") || !json.get("strokes").isJsonArray()) return out;
        for (JsonElement element : json.getAsJsonArray("strokes")) {
            if (out.size() >= MAX_GLYPH_STROKES || !element.isJsonArray()) continue;
            JsonArray flat = element.getAsJsonArray();
            int n = Math.min(flat.size(), MAX_GLYPH_NUMBERS) & ~1;
            int[] xy = new int[n];
            for (int i = 0; i < n; i++) xy[i] = Math.clamp(flat.get(i).getAsInt(), -200, 200);
            out.add(xy);
        }
        return out;
    }
}
