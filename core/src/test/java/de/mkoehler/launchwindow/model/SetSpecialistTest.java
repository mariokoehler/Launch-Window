package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link SetSpecialist}'s per-skill accessors.</p>
 *
 * @author Mario Koehler
 */
class SetSpecialistTest {

    /**
     * Verifies that a skill never explicitly set reads back as zero.
     */
    @Test
    void unsetSkillDefaultsToZero() {
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);

        assertEquals(0.0, specialist.getSkill(SetSkill.ROCKETS));
    }

    /**
     * Verifies that a value written via {@code setSkill} is read back
     * unchanged via {@code getSkill}, including a fractional value - the
     * shape SET's own flat per-season R&D growth needs, {@code design.md}
     * &sect;2.2(c).
     */
    @Test
    void setSkillIsReflectedByGetSkill() {
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);

        specialist.setSkill(SetSkill.CREWED_SPACECRAFT, 63.4);

        assertEquals(63.4, specialist.getSkill(SetSkill.CREWED_SPACECRAFT));
    }

    /**
     * Verifies that {@code setSkills} defensively copies its argument, so
     * passing an immutable map (e.g. {@code Map.of(...)}) does not leave
     * the specialist's own skill map immutable.
     */
    @Test
    void setSkillsCopiesAnImmutableMapIntoAMutableOne() {
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);

        specialist.setSkills(Map.of(SetSkill.SPACE_PROBES, 48.0));

        assertEquals(48.0, specialist.getSkill(SetSkill.SPACE_PROBES));
        assertDoesNotThrow(() -> specialist.setSkill(SetSkill.EVA_SUITS, 20));
    }
}
