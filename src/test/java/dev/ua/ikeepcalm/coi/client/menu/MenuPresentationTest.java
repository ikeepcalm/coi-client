package dev.ua.ikeepcalm.coi.client.menu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MenuPresentationTest {
    @Test
    void archiveFamiliesKeepAuthorityAndUnknownTemplatesFallBack() {
        for (String template : new String[]{"ledger", "relic", "inscription", "atlas", "challenge"}) {
            MenuDocument doc = document("{\"template\":\"" + template + "\"}");
            assertTrue(doc.presentation().archive(), template);
            assertEquals(template, doc.presentation().template());
            assertEquals("token", doc.session());
            assertEquals(12, doc.version());
            MenuComponent.Button button = (MenuComponent.Button) doc.sections().getFirst().components().getFirst();
            assertEquals("server-action", button.id());
            assertFalse(button.enabled());
            assertEquals("Cooldown", button.disabledReason());
        }
        assertFalse(document("{\"template\":\"future\"}").presentation().archive());
    }
    @Test void manualPresentationRetainsServerControls() {
        MenuDocument doc = document("{\"template\":\"ability_manual\"}");
        assertTrue(doc.presentation().abilityManual());
        assertFalse(doc.presentation().specimen());
        MenuComponent.Button button = (MenuComponent.Button) doc.sections().getFirst().components().getFirst();
        assertEquals("server-action", button.id());
        assertFalse(button.enabled());
    }
    private MenuDocument document(String presentation) {
        return MenuParser.parse("""
                {"session":"token","version":12,"screen":"any.server.screen",
                 "presentation":%s,
                 "sections":[{"components":[{"type":"button","id":"server-action",
                 "label":"Transform","enabled":false,"disabledReason":"Cooldown"}]}]}
                """.formatted(presentation));
    }

    @Test void presentationNeverChangesServerActionsOrGates() {
        MenuDocument doc = document("""
                {"template":"specimen","subject":"giant","caption":"Form study"}
                """);
        assertTrue(doc.presentation().specimen());
        assertEquals("giant", doc.presentation().subject());
        assertEquals("token", doc.session());
        assertEquals(12, doc.version());
        MenuComponent.Button button = (MenuComponent.Button) doc.sections().getFirst().components().getFirst();
        assertEquals("server-action", button.id());
        assertFalse(button.enabled());
        assertEquals("Cooldown", button.disabledReason());
    }

    @Test void unknownAndMalformedTemplatesRetainAllContent() {
        for (String hint : new String[]{"null", "[]", "42", "{}",
                "{\"template\":\"future\",\"subject\":\"giant\"}",
                "{\"template\":\"specimen\",\"subject\":{}}"}) {
            MenuDocument doc = document(hint);
            assertNotNull(doc, hint);
            assertFalse(doc.presentation().specimen(), hint);
            assertEquals(1, doc.sections().getFirst().components().size());
        }
    }

    @Test void oldServersAndServerClosureNeedNoPresentation() {
        MenuDocument legacy = MenuParser.parse("{\"session\":\"old\",\"sections\":[]}");
        assertNotNull(legacy);
        assertFalse(legacy.presentation().specimen());
        MenuDocument closed = MenuParser.parse("{\"session\":\"old\",\"closed\":true}");
        assertTrue(closed.closed());
        assertFalse(closed.presentation().specimen());
    }
}
