package dev.ua.ikeepcalm.coi.client.ability;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityCategoryBindingTest {
    @AfterEach void clear() { AbilityCategories.clear(); }

    @Test void legacyBindingsKeepTheirMeaning() {
        String normal = AbilityInfo.formatStored("door-5-1", "Door", "execute");
        String secondary = AbilityInfo.formatStored("door-5-1", "Door", "left_click");
        assertEquals("door-5-1", AbilityInfo.extractId(normal));
        assertEquals("door-5-1", AbilityInfo.extractId(secondary));
        assertEquals("left_click", AbilityInfo.extractAction(secondary));
        assertEquals("", AbilityInfo.extractCategory(secondary));
        assertEquals("", AbilityInfo.extractCategory(normal));
    }

    @Test void categoryIdentitySurvivesLocalizationAndDelimiterCharacters() {
        String key = "полум’я - #left_click/return";
        String stored = AbilityInfo.formatCategory("door-5-1", "Двері · Повернення", key);
        assertEquals("door-5-1", AbilityInfo.extractId(stored));
        assertEquals(key, AbilityInfo.extractCategory(stored));
        assertEquals("execute", AbilityInfo.extractAction(stored));
        assertEquals("Двері · Повернення", AbilityInfo.extractDisplayName(stored));
        assertNotEquals(stored, AbilityInfo.formatCategory("door-5-1", "Двері · Повернення", "different"));
    }

    @Test void cooldownSnapshotsKeepModesIndependentAndPermitReset() {
        AbilityCategories.read("door-5-1", JsonParser.parseString("""
                {"selectedCategory":"travel","castCategories":[
                 {"id":"travel","name":"Travel","cooldownSeconds":20,"cooldownRemainingTicks":200},
                 {"id":"return","name":"Return","cooldownSeconds":5,"cooldownRemainingTicks":0}]}
                """).getAsJsonObject());
        assertTrue(AbilityCategories.find("door-5-1", "travel").remainingTicks() > 190);
        assertEquals(0, AbilityCategories.find("door-5-1", "return").remainingTicks());
        AbilityCategories.read("door-5-1", JsonParser.parseString("""
                {"selectedCategory":"return","castCategories":[{"id":"travel","cooldownRemainingTicks":0}]}
                """).getAsJsonObject());
        assertEquals(0, AbilityCategories.find("door-5-1", "travel").remainingTicks());
        assertNull(AbilityCategories.find("door-5-1", "return"));
        assertEquals("return", AbilityCategories.selected("door-5-1"));
    }

    @Test void malformedAndDuplicateCategoriesDoNotCreateBindings() {
        AbilityCategories.read("x", JsonParser.parseString("""
                {"castCategories":[null,7,{}, {"id":""}, {"id":"ok","cooldownSeconds":-2}, {"id":"ok"}]}
                """).getAsJsonObject());
        assertEquals(1, AbilityCategories.get("x").size());
        assertEquals(0, AbilityCategories.find("x", "ok").cooldownSeconds());
    }

    @Test void abilityLockSurvivesCategoryOnlyUpdatesAndCanBeCleared() {
        AbilityCategories.read("x", JsonParser.parseString("{\"abilityLockTicks\":200}").getAsJsonObject());
        AbilityCategories.read("x", JsonParser.parseString("{\"selectedCategory\":\"travel\"}").getAsJsonObject());
        assertTrue(AbilityCategories.lockTicks("x") > 190);
        AbilityCategories.read("x", JsonParser.parseString("{\"abilityLockTicks\":0}").getAsJsonObject());
        assertEquals(0, AbilityCategories.lockTicks("x"));
        AbilityCategories.clear();
        assertEquals(0, AbilityCategories.lockTicks("x"));
    }
}
