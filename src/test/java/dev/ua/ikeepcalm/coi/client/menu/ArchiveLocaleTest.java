package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveLocaleTest {
    @Test void dossierAndSpecimenLabelsHaveTranslationsWithMatchingArguments() throws Exception {
        Path directory = Path.of("src/client/resources/assets/coi-client/lang");
        var english = JsonParser.parseString(Files.readString(directory.resolve("en_us.json"))).getAsJsonObject();
        var ukrainian = JsonParser.parseString(Files.readString(directory.resolve("uk_ua.json"))).getAsJsonObject();
        Pattern argument = Pattern.compile("%(?:\\d+\\$)?[sd]");
        for (String key : english.keySet()) {
            if (!key.startsWith("screen.coi.sheet_") && !key.startsWith("screen.coi.dossier_")
                    && !key.startsWith("screen.coi.specimen_") && !key.startsWith("screen.coi.manual_")
                    && !key.startsWith("screen.coi.picker_") && !key.equals("screen.coi.category_unavailable")) continue;
            assertTrue(ukrainian.has(key), key);
            String translation = ukrainian.get(key).getAsString();
            assertFalse(translation.isBlank(), key);
            var expected = argument.matcher(english.get(key).getAsString()).results().map(m -> m.group()).sorted().toList();
            var actual = argument.matcher(translation).results().map(m -> m.group()).sorted().toList();
            assertEquals(expected, actual, key);
        }
    }
}
