package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.AgencyNames;
import de.mkoehler.launchwindow.model.Archetype;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link NameSuggestions}.
 *
 * @author Mario Koehler
 */
class NameSuggestionsTest {

    private static final AgencyNames NAMES = new AgencyNames(List.of("A", "B", "C", "D"), List.of("X", "Y", "Z"));

    /** A deck deals every name once before any repeats, and a new shuffle never repeats the last. */
    @Test
    void dealsEveryNameOnceBeforeRepeating() {
        for (long seed = 0; seed < 200; seed++) {
            NameSuggestions suggestions = new NameSuggestions(NAMES, new Random(seed));
            String previous = null;
            for (int round = 0; round < 3; round++) {
                Set<String> dealt = new HashSet<>();
                for (int i = 0; i < 4; i++) {
                    String name = suggestions.next(Archetype.NASA).orElseThrow();
                    assertNotEquals(previous, name, "seed " + seed + " repeated a name");
                    assertTrue(dealt.add(name), "seed " + seed + " dealt " + name + " twice in one deck");
                    previous = name;
                }
                assertEquals(Set.of("A", "B", "C", "D"), dealt);
            }
        }
    }

    /** Each style has its own deck; no style picked deals from both. */
    @Test
    void keepsADeckPerStyle() {
        NameSuggestions suggestions = new NameSuggestions(NAMES, new Random(1));
        assertTrue(List.of("X", "Y", "Z").contains(suggestions.next(Archetype.SOVIET).orElseThrow()));
        assertTrue(List.of("A", "B", "C", "D").contains(suggestions.next(Archetype.NASA).orElseThrow()));
        Set<String> both = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            both.add(suggestions.next(null).orElseThrow());
        }
        assertEquals(7, both.size());
        assertEquals(7, suggestions.size(null));
    }

    /** An empty pool suggests nothing. */
    @Test
    void anEmptyPoolSuggestsNothing() {
        assertEquals(Optional.empty(), new NameSuggestions(AgencyNames.EMPTY, new Random()).next(Archetype.NASA));
    }
}
