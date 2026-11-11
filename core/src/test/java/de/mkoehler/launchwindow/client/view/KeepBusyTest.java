package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Keep Busy trains each idle person's highest skill below 90, the next one down once it's there,
 * and leaves alone anyone busy, unavailable, or at 90 in everything.
 */
class KeepBusyTest {

    private static Astronaut astronaut(String id, double leadership, double piloting, double eva, double science,
            double fitness) {
        Astronaut astronaut = new Astronaut(id, "Test " + id, 30, 70, 60);
        astronaut.setSkill(AstronautSkill.LEADERSHIP, leadership);
        astronaut.setSkill(AstronautSkill.PILOTING, piloting);
        astronaut.setSkill(AstronautSkill.EVA, eva);
        astronaut.setSkill(AstronautSkill.SCIENCE, science);
        astronaut.setSkill(AstronautSkill.FITNESS, fitness);
        astronaut.setAssignmentState(AssignmentState.IDLE);
        return astronaut;
    }

    @Test
    void picksTheHighestSkillStillBelowTheTarget() {
        Astronaut highest = astronaut("a1", 40, 75, 60, 30, 50);
        Astronaut pastTarget = astronaut("a2", 40, 95, 88.5, 30, 50);
        Astronaut tie = astronaut("a3", 70, 70, 10, 10, 10);
        Astronaut done = astronaut("a4", 90, 92, 90, 99, 90);
        Astronaut busy = astronaut("a5", 40, 75, 60, 30, 50);
        busy.setAssignmentState(AssignmentState.IN_TRAINING);
        Astronaut away = astronaut("a6", 40, 75, 60, 30, 50);
        away.setUnavailableSeasons(1);

        List<KeepBusy.Assignment> plan = KeepBusy.plan(Department.ASTRONAUT,
            List.of(highest, pastTarget, tie, done, busy, away));

        assertEquals(List.of(new KeepBusy.Assignment(highest, "PILOTING"), new KeepBusy.Assignment(pastTarget, "EVA"),
            new KeepBusy.Assignment(tie, "LEADERSHIP")), plan);
    }
}
