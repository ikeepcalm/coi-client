package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * The repeated structures inside a menu component — buttons, rows, chips,
 * steps, panel cells and the rest.
 * <p>
 * Each of these appears in more than one component ({@code button} both alone
 * and inside {@code buttons} and a document's footer; {@code chips} both alone
 * and inside a {@code hero}; a {@code row} both in a list and in a grid), which
 * is what earns them a home of their own rather than a branch of
 * {@link MenuParser}'s switch.
 * <p>
 * Same contract as everything else on this channel: a malformed entry is
 * skipped, an absent list is empty, and nothing throws.
 */
final class MenuParts {

    private MenuParts() {
    }

    static MenuComponent.Button button(JsonObject node) {
        return new MenuComponent.Button(
                MenuJson.string(node, "id", MenuLimits.MAX_ID),
                MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                MenuJson.string(node, "desc", MenuLimits.MAX_TEXT),
                MenuStyles.button(MenuStyles.word(node, "style")),
                MenuJson.enabled(node),
                MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT),
                confirm(node.get("confirm")),
                MenuIcon.parse(node.get("icon")));
    }

    static List<MenuComponent.Button> buttons(JsonElement element, int cap) {
        List<MenuComponent.Button> buttons = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, cap)) {
            buttons.add(button(node));
        }
        return List.copyOf(buttons);
    }

    /**
     * @return null when the button names no confirmation, which is the signal
     * to send the click straight through
     */
    private static MenuComponent.Confirm confirm(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject node = element.getAsJsonObject();
        return new MenuComponent.Confirm(MenuJson.string(node, "title", MenuLimits.MAX_TITLE),
                MenuJson.string(node, "body", MenuLimits.MAX_TEXT),
                MenuJson.string(node, "confirmLabel", MenuLimits.MAX_LABEL));
    }

    static List<MenuComponent.KvRow> kvRows(JsonElement element) {
        List<MenuComponent.KvRow> rows = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_KV_ROWS)) {
            rows.add(new MenuComponent.KvRow(MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL), MenuJson.color(node, "color"),
                    MenuJson.string(node, "hint", MenuLimits.MAX_TEXT), MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(rows);
    }

    static List<MenuComponent.Check> checks(JsonElement element) {
        List<MenuComponent.Check> checks = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_CHECKS)) {
            checks.add(new MenuComponent.Check(MenuStyles.check(node),
                    MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "detail", MenuLimits.MAX_TEXT),
                    MenuIcon.parse(node.get("icon")), MenuJson.color(node, "color")));
        }
        return List.copyOf(checks);
    }

    /**
     * A list row or a grid cell — the same record either way, which is why a
     * grid can carry a badge and a fraction it happens not to draw.
     */
    static List<MenuComponent.Row> rows(JsonElement element) {
        List<MenuComponent.Row> rows = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_ROWS)) {
            rows.add(new MenuComponent.Row(
                    MenuJson.string(node, "id", MenuLimits.MAX_ID),
                    MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "subtitle", MenuLimits.MAX_LABEL),
                    MenuIcon.parse(node.get("icon")),
                    MenuJson.string(node, "badge", MenuLimits.MAX_BADGE),
                    MenuJson.color(node, "badgeColor"),
                    tooltip(node.get("tooltip")),
                    MenuJson.string(node, "action", MenuLimits.MAX_ID),
                    MenuJson.enabled(node),
                    MenuJson.color(node, "color"),
                    MenuJson.fraction(node, "fraction"), node.has("fraction"),
                    MenuJson.string(node, "meta", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(rows);
    }

    static List<MenuComponent.Chip> chips(JsonElement element, int cap) {
        List<MenuComponent.Chip> chips = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, cap)) {
            chips.add(new MenuComponent.Chip(MenuJson.string(node, "label", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL), MenuJson.color(node, "color"),
                    MenuIcon.parse(node.get("icon")), MenuJson.string(node, "hint", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(chips);
    }

    static List<MenuComponent.Step> steps(JsonElement element) {
        List<MenuComponent.Step> steps = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_STEPS)) {
            // Boxed on purpose: an absent "done" is "not yet", which is neither
            // of the two things a boolean could say
            Boolean done = node.has("done") && node.get("done").isJsonPrimitive()
                    ? MenuJson.bool(node, "done") : null;
            steps.add(new MenuComponent.Step(MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "text", MenuLimits.MAX_TEXT), done, MenuIcon.parse(node.get("icon"))));
        }
        return List.copyOf(steps);
    }

    static List<MenuComponent.PanelCell> panelCells(JsonElement element) {
        List<MenuComponent.PanelCell> cells = new ArrayList<>();
        for (JsonObject node : MenuJson.objects(element, MenuLimits.MAX_PANEL_CELLS)) {
            cells.add(new MenuComponent.PanelCell(
                    MenuJson.string(node, "id", MenuLimits.MAX_ID),
                    MenuIcon.parse(node.get("icon")),
                    MenuJson.string(node, "title", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "value", MenuLimits.MAX_LABEL),
                    MenuJson.string(node, "subtitle", MenuLimits.MAX_TEXT),
                    MenuJson.color(node, "color"),
                    MenuJson.fraction(node, "fraction"), node.has("fraction"),
                    MenuJson.string(node, "badge", MenuLimits.MAX_BADGE),
                    MenuJson.color(node, "badgeColor"),
                    tooltip(node.get("tooltip")),
                    MenuJson.string(node, "action", MenuLimits.MAX_ID),
                    MenuJson.enabled(node),
                    MenuJson.string(node, "disabledReason", MenuLimits.MAX_TEXT)));
        }
        return List.copyOf(cells);
    }

    static List<String> tooltip(JsonElement element) {
        return MenuJson.strings(element, MenuLimits.MAX_TOOLTIP_LINES);
    }
}
