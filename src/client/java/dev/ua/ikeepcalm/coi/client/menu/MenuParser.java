package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code coi-client:menu} JSON to a {@link MenuDocument}.
 * <p>
 * Nothing in here throws. The document comes from a plugin that may be newer
 * than this client and is rendered as a whole screen, so the failure modes are
 * asymmetric: dropping one unreadable component costs a row, while letting an
 * exception out of a network receiver costs the screen. Unknown {@code type}s,
 * wrong JSON types and missing keys are therefore all "skip it and carry on",
 * and only a body that is not a JSON object at all gives up entirely.
 * <p>
 * The caps below are not a security boundary — the payload is already capped at
 * 1 MiB — they keep a runaway server-side loop from laying out a card a hundred
 * thousand pixels tall.
 */
public final class MenuParser {

    private static final int MAX_SECTIONS = 32;
    private static final int MAX_COMPONENTS = 96;
    private static final int MAX_ROWS = 400;
    private static final int MAX_KV_ROWS = 64;
    private static final int MAX_CHECKS = 64;
    private static final int MAX_BUTTONS = 24;
    private static final int MAX_TOOLTIP_LINES = 10;

    /**
     * Vocabulary v2 caps. A chip row and a step rail are read top to bottom, so
     * the ceilings are low on purpose: past a dozen the component is the wrong
     * one and the server should have sent a list.
     */
    private static final int MAX_CHIPS = 12;
    private static final int MAX_STEPS = 12;
    private static final int MAX_PANEL_CELLS = 12;
    private static final int MAX_DETAILS_BLOCKS = 8;
    private static final int MAX_HERO_CHIPS = 4;

    private static final int MAX_TITLE = 128;
    private static final int MAX_LABEL = 160;
    private static final int MAX_TEXT = 2000;
    private static final int MAX_ID = 96;

    private MenuParser() {
    }

    /**
     * @return the document, or null when the body is not usable at all
     */
    public static MenuDocument parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject node = root.getAsJsonObject();
            String session = string(node, "session", MAX_ID);
            if (bool(node, "closed")) return MenuDocument.closed(session);

            return new MenuDocument(
                    session,
                    intOf(node, "version", 0),
                    string(node, "screen", MAX_ID),
                    string(node, "title", MAX_TITLE),
                    string(node, "subtitle", MAX_TITLE),
                    hex(string(node, "accent", 8)),
                    MenuIcon.parse(node.get("icon")),
                    bool(node, "back"),
                    !node.has("closable") || bool(node, "closable"),
                    toast(node.get("toast")),
                    sections(node.get("sections")),
                    buttons(node.get("footer"), MAX_BUTTONS),
                    false);
        } catch (Exception e) {
            System.err.println("COI Client: malformed menu payload: " + e);
            return null;
        }
    }

    private static MenuDocument.Toast toast(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject node = element.getAsJsonObject();
        String text = string(node, "text", MAX_TEXT);
        if (text.isEmpty()) return null;
        return new MenuDocument.Toast(string(node, "style", 16).toLowerCase(Locale.ROOT), text);
    }

    /**
     * A section whose heading carries v2's {@code icon} / {@code badge} /
     * {@code collapsed} is rewritten: the decoration becomes a
     * {@link MenuComponent.Heading} at the head of the component list and the
     * section's own title is emptied, so the renderer draws exactly one
     * heading. A plain titled section still travels the v1 way.
     */
    private static List<MenuDocument.Section> sections(JsonElement element) {
        List<MenuDocument.Section> sections = new ArrayList<>();
        int index = 0;
        for (JsonObject node : objects(element, MAX_SECTIONS)) {
            String title = string(node, "title", MAX_TITLE);
            List<MenuComponent> components = components(node.get("components"));
            MenuComponent.Heading heading = heading(node, title, index);
            if (heading != null) {
                List<MenuComponent> decorated = new ArrayList<>(components.size() + 1);
                decorated.add(heading);
                decorated.addAll(components);
                sections.add(new MenuDocument.Section("", List.copyOf(decorated)));
            } else {
                sections.add(new MenuDocument.Section(title, components));
            }
            index++;
        }
        return List.copyOf(sections);
    }

    /**
     * @return null when the section named none of v2's heading fields, which is
     * the signal to leave it as a plain caption
     */
    private static MenuComponent.Heading heading(JsonObject node, String title, int index) {
        boolean collapsible = node.has("collapsed");
        MenuIcon icon = MenuIcon.parse(node.get("icon"));
        String badge = string(node, "badge", 32);
        if (!collapsible && !icon.present() && badge.isEmpty()) return null;
        String id = string(node, "id", MAX_ID);
        return new MenuComponent.Heading(id.isEmpty() ? "#section" + index : id, title, icon, badge,
                hex(string(node, "badgeColor", 8)), collapsible, bool(node, "collapsed"));
    }

    private static List<MenuComponent> components(JsonElement element) {
        List<MenuComponent> components = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_COMPONENTS)) {
            MenuComponent component = component(node);
            if (component != null) components.add(component);
        }
        return List.copyOf(components);
    }

    private static MenuComponent component(JsonObject node) {
        return switch (string(node, "type", 24).toLowerCase(Locale.ROOT)) {
            case "text" -> new MenuComponent.Text(string(node, "text", MAX_TEXT),
                    textStyle(string(node, "style", 16)), align(string(node, "align", 16)));
            case "note" -> new MenuComponent.Note(textStyle(string(node, "style", 16)),
                    string(node, "title", MAX_TITLE), string(node, "text", MAX_TEXT),
                    MenuIcon.parse(node.get("icon")));
            case "stat" -> new MenuComponent.Stat(string(node, "label", MAX_LABEL),
                    string(node, "value", MAX_LABEL),
                    Math.clamp(dbl(node, "fraction"), 0.0, 1.0), node.has("fraction"),
                    hex(string(node, "color", 8)), string(node, "hint", MAX_TEXT),
                    MenuIcon.parse(node.get("icon")), gaugeStyle(string(node, "style", 16)),
                    Math.clamp(dbl(node, "cap"), 0.0, 1.0), node.has("cap"),
                    string(node, "delta", 32));
            case "kv" -> new MenuComponent.Kv(kvRows(node.get("rows")));
            case "checklist" -> new MenuComponent.Checklist(checks(node.get("items")));
            case "button" -> button(node);
            case "buttons" -> new MenuComponent.Buttons(buttons(node.get("buttons"), MAX_BUTTONS),
                    Math.clamp(intOf(node, "columns", 1), 1, 4));
            case "toggle" -> new MenuComponent.Toggle(string(node, "id", MAX_ID),
                    string(node, "label", MAX_LABEL), bool(node, "on"), enabled(node),
                    string(node, "desc", MAX_TEXT), string(node, "onText", 32), string(node, "offText", 32),
                    string(node, "disabledReason", MAX_TEXT), MenuIcon.parse(node.get("icon")));
            case "list" -> new MenuComponent.ListView(string(node, "id", MAX_ID), rows(node.get("rows")),
                    bool(node, "searchable"), intOf(node, "maxVisible", 0), string(node, "empty", MAX_TEXT));
            case "grid" -> new MenuComponent.Grid(rows(node.get("cells")),
                    Math.clamp(intOf(node, "columns", 6), 1, 12), tileSize(string(node, "size", 16)));
            case "input" -> new MenuComponent.Input(string(node, "id", MAX_ID),
                    string(node, "label", MAX_LABEL), string(node, "placeholder", MAX_LABEL),
                    string(node, "value", MAX_LABEL), Math.clamp(intOf(node, "maxLength", 64), 1, 256),
                    string(node, "submit", MAX_ID), string(node, "submitLabel", MAX_LABEL),
                    string(node, "hint", MAX_TEXT));
            case "divider" -> new MenuComponent.Divider(string(node, "label", MAX_LABEL));
            case "spacer" -> new MenuComponent.Spacer(Math.clamp(intOf(node, "size", 6), 1, 48));
            case "hero" -> hero(node);
            case "details" -> details(node);
            case "steps" -> new MenuComponent.Steps(stepStyle(string(node, "style", 16)),
                    steps(node.get("items")));
            case "chips" -> new MenuComponent.Chips(chips(node.get("items"), MAX_CHIPS));
            case "panels" -> new MenuComponent.Panels(Math.clamp(intOf(node, "columns", 2), 1, 3),
                    panelCells(node.get("cells")));
            // A component this client has never heard of: a newer plugin's
            // problem to notice, not a reason to lose the screen
            default -> null;
        };
    }

    private static MenuComponent.Hero hero(JsonObject node) {
        return new MenuComponent.Hero(
                MenuIcon.parse(node.get("icon")),
                string(node, "title", MAX_TITLE),
                string(node, "subtitle", MAX_TITLE),
                string(node, "badge", 32),
                hex(string(node, "badgeColor", 8)),
                heroStyle(string(node, "style", 16)),
                Math.clamp(dbl(node, "fraction"), 0.0, 1.0), node.has("fraction"),
                string(node, "fractionLabel", 32),
                hex(string(node, "color", 8)),
                chips(node.get("chips"), MAX_HERO_CHIPS));
    }

    private static MenuComponent.Details details(JsonObject node) {
        return new MenuComponent.Details(
                string(node, "id", MAX_ID),
                string(node, "summary", MAX_LABEL),
                strings(node.get("text"), MAX_DETAILS_BLOCKS),
                node.has("style") ? textStyle(string(node, "style", 16)) : MenuComponent.TextStyle.MUTED,
                MenuIcon.parse(node.get("icon")),
                bool(node, "open"));
    }

    private static List<MenuComponent.Chip> chips(JsonElement element, int cap) {
        List<MenuComponent.Chip> chips = new ArrayList<>();
        for (JsonObject node : objects(element, cap)) {
            chips.add(new MenuComponent.Chip(string(node, "label", MAX_LABEL),
                    string(node, "value", MAX_LABEL), hex(string(node, "color", 8)),
                    MenuIcon.parse(node.get("icon")), string(node, "hint", MAX_TEXT)));
        }
        return List.copyOf(chips);
    }

    private static List<MenuComponent.Step> steps(JsonElement element) {
        List<MenuComponent.Step> steps = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_STEPS)) {
            // Boxed on purpose: an absent "done" is "not yet", which is neither
            // of the two things a boolean could say
            Boolean done = node.has("done") && node.get("done").isJsonPrimitive()
                    ? bool(node, "done") : null;
            steps.add(new MenuComponent.Step(string(node, "title", MAX_LABEL),
                    string(node, "text", MAX_TEXT), done, MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(steps);
    }

    private static List<MenuComponent.PanelCell> panelCells(JsonElement element) {
        List<MenuComponent.PanelCell> cells = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_PANEL_CELLS)) {
            cells.add(new MenuComponent.PanelCell(
                    string(node, "id", MAX_ID),
                    MenuIcon.parse(node.get("icon")),
                    string(node, "title", MAX_LABEL),
                    string(node, "value", MAX_LABEL),
                    string(node, "subtitle", MAX_TEXT),
                    hex(string(node, "color", 8)),
                    Math.clamp(dbl(node, "fraction"), 0.0, 1.0), node.has("fraction"),
                    string(node, "badge", 32),
                    hex(string(node, "badgeColor", 8)),
                    tooltip(node.get("tooltip")),
                    string(node, "action", MAX_ID),
                    enabled(node),
                    string(node, "disabledReason", MAX_TEXT)));
        }
        return List.copyOf(cells);
    }

    private static MenuComponent.Button button(JsonObject node) {
        return new MenuComponent.Button(
                string(node, "id", MAX_ID),
                string(node, "label", MAX_LABEL),
                string(node, "desc", MAX_TEXT),
                buttonStyle(string(node, "style", 16)),
                enabled(node),
                string(node, "disabledReason", MAX_TEXT),
                confirm(node.get("confirm")),
                MenuIcon.parse(node.get("icon")));
    }

    private static MenuComponent.Confirm confirm(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject node = element.getAsJsonObject();
        return new MenuComponent.Confirm(string(node, "title", MAX_TITLE),
                string(node, "body", MAX_TEXT), string(node, "confirmLabel", MAX_LABEL));
    }

    private static List<MenuComponent.Button> buttons(JsonElement element, int cap) {
        List<MenuComponent.Button> buttons = new ArrayList<>();
        for (JsonObject node : objects(element, cap)) {
            buttons.add(button(node));
        }
        return List.copyOf(buttons);
    }

    private static List<MenuComponent.KvRow> kvRows(JsonElement element) {
        List<MenuComponent.KvRow> rows = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_KV_ROWS)) {
            rows.add(new MenuComponent.KvRow(string(node, "label", MAX_LABEL),
                    string(node, "value", MAX_LABEL), hex(string(node, "color", 8)),
                    string(node, "hint", MAX_TEXT), MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(rows);
    }

    private static List<MenuComponent.Check> checks(JsonElement element) {
        List<MenuComponent.Check> checks = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_CHECKS)) {
            checks.add(new MenuComponent.Check(checkState(node), string(node, "label", MAX_LABEL),
                    string(node, "detail", MAX_TEXT), MenuIcon.parse(node.get("icon")),
                    hex(string(node, "color", 8))));
        }
        return List.copyOf(checks);
    }

    private static List<MenuComponent.Row> rows(JsonElement element) {
        List<MenuComponent.Row> rows = new ArrayList<>();
        for (JsonObject node : objects(element, MAX_ROWS)) {
            rows.add(new MenuComponent.Row(
                    string(node, "id", MAX_ID),
                    string(node, "title", MAX_LABEL),
                    string(node, "subtitle", MAX_LABEL),
                    MenuIcon.parse(node.get("icon")),
                    string(node, "badge", 32),
                    hex(string(node, "badgeColor", 8)),
                    tooltip(node.get("tooltip")),
                    string(node, "action", MAX_ID),
                    enabled(node),
                    hex(string(node, "color", 8)),
                    Math.clamp(dbl(node, "fraction"), 0.0, 1.0), node.has("fraction"),
                    string(node, "meta", MAX_LABEL),
                    string(node, "disabledReason", MAX_TEXT)));
        }
        return List.copyOf(rows);
    }

    private static List<String> tooltip(JsonElement element) {
        return strings(element, MAX_TOOLTIP_LINES);
    }

    private static List<String> strings(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<String> lines = new ArrayList<>();
        for (JsonElement line : element.getAsJsonArray()) {
            if (lines.size() >= cap) break;
            if (line.isJsonPrimitive()) lines.add(trim(line.getAsString(), MAX_TEXT));
        }
        return List.copyOf(lines);
    }

    // --- Enums (an unknown word is the default, never a failure) ---

    private static MenuComponent.TextStyle textStyle(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "muted" -> MenuComponent.TextStyle.MUTED;
            case "heading" -> MenuComponent.TextStyle.HEADING;
            case "warn", "warning" -> MenuComponent.TextStyle.WARN;
            case "danger", "error" -> MenuComponent.TextStyle.DANGER;
            case "success" -> MenuComponent.TextStyle.SUCCESS;
            default -> MenuComponent.TextStyle.BODY;
        };
    }

    private static MenuComponent.ButtonStyle buttonStyle(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "primary" -> MenuComponent.ButtonStyle.PRIMARY;
            case "danger" -> MenuComponent.ButtonStyle.DANGER;
            case "success" -> MenuComponent.ButtonStyle.SUCCESS;
            case "ghost" -> MenuComponent.ButtonStyle.GHOST;
            default -> MenuComponent.ButtonStyle.SECONDARY;
        };
    }

    private static MenuComponent.Align align(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "center", "centre" -> MenuComponent.Align.CENTER;
            case "right" -> MenuComponent.Align.RIGHT;
            default -> MenuComponent.Align.LEFT;
        };
    }

    private static MenuComponent.GaugeStyle gaugeStyle(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "ring" -> MenuComponent.GaugeStyle.RING;
            case "segments", "segmented" -> MenuComponent.GaugeStyle.SEGMENTS;
            default -> MenuComponent.GaugeStyle.BAR;
        };
    }

    private static MenuComponent.HeroStyle heroStyle(String value) {
        return "ring".equals(value.toLowerCase(Locale.ROOT))
                ? MenuComponent.HeroStyle.RING : MenuComponent.HeroStyle.PLAIN;
    }

    private static MenuComponent.StepStyle stepStyle(String value) {
        return "timeline".equals(value.toLowerCase(Locale.ROOT))
                ? MenuComponent.StepStyle.TIMELINE : MenuComponent.StepStyle.NUMBERED;
    }

    private static MenuComponent.TileSize tileSize(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "small" -> MenuComponent.TileSize.SMALL;
            case "large" -> MenuComponent.TileSize.LARGE;
            default -> MenuComponent.TileSize.MEDIUM;
        };
    }

    /**
     * The tri-state, folded down once here. An unknown word is not a third
     * answer: it falls back to the {@code ok} boolean the wire has always
     * carried, which is also what an absent {@code state} does.
     */
    private static MenuComponent.CheckState checkState(JsonObject node) {
        return switch (string(node, "state", 16).toLowerCase(Locale.ROOT)) {
            case "ok", "yes", "done" -> MenuComponent.CheckState.OK;
            case "no", "fail", "failed" -> MenuComponent.CheckState.NO;
            case "pending", "wait", "waiting" -> MenuComponent.CheckState.PENDING;
            default -> bool(node, "ok") ? MenuComponent.CheckState.OK : MenuComponent.CheckState.NO;
        };
    }

    // --- Scalars (every read tolerates a missing key or the wrong type) ---

    private static List<JsonObject> objects(JsonElement element, int cap) {
        if (element == null || !element.isJsonArray()) return List.of();
        JsonArray array = element.getAsJsonArray();
        List<JsonObject> objects = new ArrayList<>();
        for (JsonElement item : array) {
            if (objects.size() >= cap) break;
            if (item.isJsonObject()) objects.add(item.getAsJsonObject());
        }
        return objects;
    }

    /**
     * Absent means enabled: most components are, and a server that has to spell
     * out {@code "enabled":true} on every button will eventually forget one.
     */
    private static boolean enabled(JsonObject node) {
        return !node.has("enabled") || bool(node, "enabled");
    }

    static String string(JsonObject node, String key, int max) {
        if (node == null || !node.has(key)) return "";
        JsonElement element = node.get(key);
        if (!element.isJsonPrimitive()) return "";
        return trim(element.getAsString(), max);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static int intOf(JsonObject node, String key, int fallback) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsInt() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double dbl(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() ? node.get(key).getAsDouble() : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean bool(JsonObject node, String key) {
        try {
            return node.has(key) && node.get(key).isJsonPrimitive() && node.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Six hex digits with no {@code #} to 0xRRGGBB; 0 means "unspecified", which
     * every caller reads as "use the document's accent or the default".
     */
    private static int hex(String value) {
        String clean = value.startsWith("#") ? value.substring(1) : value;
        if (clean.length() != 6) return 0;
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
