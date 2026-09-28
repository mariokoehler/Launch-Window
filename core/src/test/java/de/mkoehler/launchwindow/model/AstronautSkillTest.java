package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link AstronautSkill}.</p>
 *
 * @author Mario Koehler
 */
class AstronautSkillTest {

    /**
     * Verifies the enum's value count.
     */
    @Test
    void hasExactlyFiveValues() {
        assertEquals(5, AstronautSkill.values().length);
    }
}
