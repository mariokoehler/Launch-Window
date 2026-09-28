package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link SetSkill}.</p>
 *
 * @author Mario Koehler
 */
class SetSkillTest {

    /**
     * Verifies the enum's value count.
     */
    @Test
    void hasExactlyFiveValues() {
        assertEquals(5, SetSkill.values().length);
    }
}
