package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link MissionConfiguration}.</p>
 *
 * @author Mario Koehler
 */
class MissionConfigurationTest {

    /**
     * Verifies that a well-formed configuration keeps every field as
     * given.
     */
    @Test
    void storesEveryField() {
        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-std", "sputnik-1", 1);

        assertEquals("sputnik-1-std", configuration.id());
        assertEquals("sputnik-1", configuration.programId());
        assertEquals(1, configuration.durationInSeasons());
    }

    /**
     * Verifies that a duration below 1 is rejected.
     */
    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", 0));
    }

    /**
     * Verifies that a blank id or program id is rejected.
     */
    @Test
    void rejectsBlankIds() {
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration(" ", "sputnik-1", 1));
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration("sputnik-1-std", " ", 1));
    }
}
