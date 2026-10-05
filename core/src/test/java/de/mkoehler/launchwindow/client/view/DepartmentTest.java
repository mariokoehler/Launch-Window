package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The astronaut department reads astronauts, not controllers, and offers Expert training only in
 * skills at the threshold that aren't Expert yet.
 */
class DepartmentTest {

    @Test
    void theAstronautDepartmentReadsAstronauts() {
        PlayerSnapshot state = new PlayerSnapshot("Agency", Archetype.SOVIET, 0, 0, 0, 1);
        Astronaut astronaut = new Astronaut("a1", "Viktor Dorofeev", 30, 70, 60);
        astronaut.setSkill(AstronautSkill.FITNESS, 81.0);
        astronaut.setSkill(AstronautSkill.LEADERSHIP, 90.0);
        astronaut.getExpertSkills().add(AstronautSkill.LEADERSHIP);
        state.setAstronauts(List.of(astronaut));

        Department department = Department.ASTRONAUT;
        assertEquals(PersonnelGroup.ASTRONAUT, department.group());
        assertSame(state.getAstronauts(), department.roster(state));
        assertSame(state.getAstronautCandidates(), department.candidates(state));
        assertEquals(List.of("LEADERSHIP", "PILOTING", "EVA", "SCIENCE", "FITNESS"), department.skills());
        assertEquals(81.0, department.skill(astronaut, "FITNESS"));
        assertTrue(department.isExpert(astronaut, "LEADERSHIP"));
        assertEquals(List.of("FITNESS"), department.expertEligible(astronaut));
        assertEquals(50, department.retirementAge());
        assertEquals("AGE 30 · RETIRES IN 20 YRS", department.ageLine(astronaut));
        astronaut.setAge(49);
        assertEquals("AGE 49 · RETIRES THIS YEAR", department.ageLine(astronaut));
        assertFalse(Department.SET.hasExperts());
    }
}
