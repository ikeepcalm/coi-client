package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MenuManualControlsTest {
    @Test
    void onlyRepresentedAbilityRowsMoveIntoTheManual() {
        var source = MenuParser.parse("""
                {"session":"live","version":14,"screen":"catalogue",
                 "presentation":{"template":"ability_manual"},"sections":[
                 {"title":"Abilities","components":[
                   {"type":"grid","columns":3,"cells":[
                     {"id":"known","title":"Known","action":"take"},
                     {"id":"borrowed","title":"Borrowed","action":"borrow","enabled":false,"disabledReason":"Locked"}]},
                   {"type":"list","id":"passives","rows":[{"id":"known","action":"toggle"}]},
                   {"type":"button","id":"configure","label":"Configure"}]},
                 {"title":"Cogitation","components":[{"type":"button","id":"cogitate","label":"Begin"}]}],
                 "footer":[{"id":"leave","label":"Leave","confirm":{"title":"Leave?","body":"Leave the cohort?"}}]}
                """);
        var projected = MenuManualControls.project(source, "known"::equals);
        assertEquals("live", projected.session());
        assertEquals(14, projected.version());
        assertEquals(2, projected.sections().size());
        assertEquals(2, projected.sections().getFirst().components().size());
        var borrowed = ((MenuComponent.Grid) projected.sections().getFirst().components().getFirst()).cells().getFirst();
        assertEquals("borrow", borrowed.action());
        assertFalse(borrowed.enabled());
        assertEquals("Locked", borrowed.disabledReason());
        assertEquals(source.footer(), projected.footer());
        assertEquals(3, source.sections().getFirst().components().size());
    }

    @Test
    void missingMetadataKeepsTheCompleteServerDocument() {
        var source = MenuParser.parse("""
                {"presentation":{"template":"ability_manual"},"sections":[{"components":[
                  {"type":"grid","cells":[{"id":"unknown","action":"still-available"}]}]}]}
                """);
        assertEquals(source, MenuManualControls.project(source, id -> false));
    }
}
