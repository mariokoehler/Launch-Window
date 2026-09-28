package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <p>Tests for {@link Astronaut}'s per-skill accessors.</p>
 *
 * @author Mario Koehler
 */
class AstronautTest {

    /**
     * Verifies that a skill never explicitly set reads back as zero.
     */
    @Test
    void unsetSkillDefaultsToZero() {
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);

        assertEquals(0, astronaut.getSkill(AstronautSkill.EVA));
    }

    /**
     * Verifies that a value written via {@code setSkill} is read back
     * unchanged via {@code getSkill}.
     */
    @Test
    void setSkillIsReflectedByGetSkill() {
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);

        astronaut.setSkill(AstronautSkill.LEADERSHIP, 42);

        assertEquals(42, astronaut.getSkill(AstronautSkill.LEADERSHIP));
    }

    /**
     * Verifies that {@code setSkills} defensively copies its argument, so
     * passing an immutable map (e.g. {@code Map.of(...)}) does not leave
     * the astronaut's own skill map immutable.
     */
    @Test
    void setSkillsCopiesAnImmutableMapIntoAMutableOne() {
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);

        astronaut.setSkills(Map.of(AstronautSkill.PILOTING, 55));

        assertEquals(55, astronaut.getSkill(AstronautSkill.PILOTING));
        assertDoesNotThrow(() -> astronaut.setSkill(AstronautSkill.SCIENCE, 30));
    }
}
