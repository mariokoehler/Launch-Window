package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link MissionControlSkill}. Deliberately five values, not
 * six - "Flight Director" is a computed aptitude
 * ({@link MissionControlOfficer#flightDirectorSkill()}), not a stored
 * skill.</p>
 *
 * @author Mario Koehler
 */
class MissionControlSkillTest {

    /**
     * Verifies the enum's value count (five, not six - see class Javadoc).
     */
    @Test
    void hasExactlyFiveStoredValues() {
        assertEquals(5, MissionControlSkill.values().length);
    }
}
