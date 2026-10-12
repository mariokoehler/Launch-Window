package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AgencyNames} and its bundled names.
 *
 * @author Mario Koehler
 */
class AgencyNamesTest {

    private static final AgencyNames NAMES = AgencyNames.load(Path.of("..", "assets", "content", AgencyNames.FILE));

    /**
     * Twenty names a style, each one a name the server accepts, unique across both styles (ignoring
     * case, as the server compares), in letters the fonts can draw.
     */
    @Test
    void theBundledNamesAreWellFormed() {
        assertEquals(20, NAMES.nasa().size());
        assertEquals(20, NAMES.soviet().size());
        Set<String> seen = new HashSet<>();
        for (String name : NAMES.forArchetype(null)) {
            assertFalse(name.isBlank());
            assertEquals(name.strip(), name, name);
            assertTrue(name.length() <= ClaimSlotRequest.MAX_NAME_LENGTH, name + " is too long");
            assertTrue(seen.add(name.toLowerCase(Locale.ROOT)), "duplicate " + name);
            name.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                name + " uses '" + (char) c + "', which the fonts lack"));
        }
    }

    /** Each style draws its own pool; no style picked draws both. */
    @Test
    void picksThePoolByArchetype() {
        assertEquals(NAMES.nasa(), NAMES.forArchetype(Archetype.NASA));
        assertEquals(NAMES.soviet(), NAMES.forArchetype(Archetype.SOVIET));
        assertEquals(40, NAMES.forArchetype(null).size());
    }
}
