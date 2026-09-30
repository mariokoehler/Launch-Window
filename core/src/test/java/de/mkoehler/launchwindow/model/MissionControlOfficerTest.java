package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link MissionControlOfficer}, including the computed Flight
 * Director aptitude.</p>
 *
 * @author Mario Koehler
 */
class MissionControlOfficerTest {

    /**
     * Verifies that a skill never explicitly set reads back as zero.
     */
    @Test
    void unsetSkillDefaultsToZero() {
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);

        assertEquals(0.0, officer.getSkill(MissionControlSkill.PROPULSION));
    }

    /**
     * Verifies that a fractional skill value - the shape SET's flat R&D
     * growth needs, {@code design.md} &sect;2.2(c) - is read back exactly,
     * not rounded to a whole number.
     */
    @Test
    void fractionalSkillValueIsReadBackExactly() {
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);

        officer.setSkill(MissionControlSkill.PROPULSION, 50.3);

        assertEquals(50.3, officer.getSkill(MissionControlSkill.PROPULSION));
    }

    /**
     * Verifies that {@code flightDirectorSkill} returns the plain average
     * of all five stored skill values.
     */
    @Test
    void flightDirectorSkillIsAverageOfAllFiveStoredSkills() {
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, 50);
        officer.setSkill(MissionControlSkill.TRAJECTORY_AND_GNC, 60);
        officer.setSkill(MissionControlSkill.SPACECRAFT_SYSTEMS, 70);
        officer.setSkill(MissionControlSkill.CREW_SYSTEMS_AND_PAYLOADS, 80);
        officer.setSkill(MissionControlSkill.MISSION_OPERATIONS, 90);

        assertEquals(70.0, officer.flightDirectorSkill());
    }

    /**
     * Verifies that a skill never explicitly set counts as zero in the
     * {@code flightDirectorSkill} average, rather than being excluded from
     * it.
     */
    @Test
    void flightDirectorSkillTreatsUnsetSkillsAsZero() {
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, 50);

        assertEquals(10.0, officer.flightDirectorSkill());
    }

    /**
     * Verifies that {@code setSkills} defensively copies its argument, so
     * passing an immutable map (e.g. {@code Map.of(...)}) does not leave
     * the officer's own skill map immutable.
     */
    @Test
    void setSkillsCopiesAnImmutableMapIntoAMutableOne() {
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);

        officer.setSkills(Map.of(MissionControlSkill.MISSION_OPERATIONS, 66.0));

        assertEquals(66.0, officer.getSkill(MissionControlSkill.MISSION_OPERATIONS));
        assertDoesNotThrow(() -> officer.setSkill(MissionControlSkill.PROPULSION, 10));
    }
}
