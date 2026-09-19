package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Removes only rows whose actions and lore are available in the field manual.
 */
public class MenuManualControls {
    private MenuManualControls() {
    }

    public static MenuDocument project(MenuDocument doc) {
        return project(doc, id -> AbilityRegistry.getAbilityInfo(id) != null);
    }

    static MenuDocument project(MenuDocument doc, Predicate<String> represented) {
        if (!doc.presentation().abilityManual()) return doc;
        List<MenuDocument.Section> sections = new ArrayList<>();
        for (var section : doc.sections()) {
            List<MenuComponent> components = new ArrayList<>();
            for (var component : section.components()) {
                if (component instanceof MenuComponent.Grid(
                        List<MenuComponent.Row> cells, int columns, MenuComponent.TileSize size
                )) {
                    var rows = cells.stream().filter(row -> !represented.test(row.id())).toList();
                    if (!rows.isEmpty()) components.add(new MenuComponent.Grid(rows, columns, size));
                } else if (component instanceof MenuComponent.ListView(
                        String id, List<MenuComponent.Row> rows1, boolean searchable, int maxVisible, String empty
                )) {
                    var rows = rows1.stream().filter(row -> !represented.test(row.id())).toList();
                    if (!rows.isEmpty()) components.add(new MenuComponent.ListView(id, rows,
                            searchable, maxVisible, empty));
                } else components.add(component);
            }
            if (components.stream().anyMatch(c -> !(c instanceof MenuComponent.Heading)
                    && !(c instanceof MenuComponent.Divider) && !(c instanceof MenuComponent.Spacer))) {
                sections.add(new MenuDocument.Section(section.title(), List.copyOf(components)));
            }
        }
        return new MenuDocument(doc.session(), doc.version(), doc.screen(), doc.title(), doc.subtitle(),
                doc.accentRgb(), doc.icon(), doc.back(), doc.closable(), doc.toast(), List.copyOf(sections),
                doc.footer(), doc.closed(), doc.presentation());
    }
}
