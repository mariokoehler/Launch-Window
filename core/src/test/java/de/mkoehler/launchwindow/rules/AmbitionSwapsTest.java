package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AmbitionSwaps} (the user's request, 2026-11-14): the plan's best fits pass someone ambitious
 * over, and they're offered the slot where they cost least.
 *
 * @author Mario Koehler
 */
class AmbitionSwapsTest {

    private static final MissionConfiguration CREWED = new MissionConfiguration("crewed-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("03-launch",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true)))),
        1, 0, 0, 0, List.of(), List.of(), List.of(new MissionConfiguration.Launch(
            de.mkoehler.launchwindow.content.LaunchType.LEO_HR, List.of("capsule-mercury"))));

    private static Astronaut astronaut(PlayerSnapshot snapshot, String id, double piloting, Ambition.Kind wants) {
        Astronaut astronaut = new Astronaut(id, "Astronaut " + id, 34, 70, 50);
        astronaut.setSkill(AstronautSkill.PILOTING, piloting);
        if (wants != null) {
            astronaut.setAmbition(new Ambition(wants, "", 10, 10, 1));
        }
        List<Astronaut> roster = new ArrayList<>(snapshot.getAstronauts());
        roster.add(astronaut);
        snapshot.setAstronauts(roster);
        return astronaut;
    }

    private static PlayerSnapshot player() {
        return new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
    }

    @Test
    void someoneAmbitiousPassedOverIsOfferedTheSeat() {
        PlayerSnapshot snapshot = player();
        astronaut(snapshot, "a1", 80, null);
        astronaut(snapshot, "a2", 50, Ambition.Kind.FLY_IN_SPACE);

        AmbitionSwaps.Swap swap = AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), false, Set.of(),
            (crew, slot) -> true).orElseThrow();

        assertEquals("a2", swap.personId());
        assertEquals("a1", swap.holderId());
        assertTrue(swap.crew());
        assertEquals(80, swap.holderValue(), 1e-9);
        assertEquals(50, swap.personValue(), 1e-9);
        assertFalse(swap.holderWants());
        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), false, Set.of("a2"),
            (crew, slot) -> true).isEmpty(), "asked once");
        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a2"), List.of(), false, Set.of(),
            (crew, slot) -> true).isEmpty(), "aboard already");
        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), false, Set.of(),
            (crew, slot) -> false).isEmpty(), "only the slots offered");
        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of("a2"), false, Set.of(),
            (crew, slot) -> true).isEmpty(), "already in the plan, at a console");
    }

    @Test
    void theHolderSaysWhenTheyWantItToo() {
        PlayerSnapshot snapshot = player();
        astronaut(snapshot, "a1", 80, Ambition.Kind.FLY_IN_SPACE);
        astronaut(snapshot, "a2", 50, Ambition.Kind.FLY_IN_SPACE);

        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), false, Set.of(),
            (crew, slot) -> true).orElseThrow().holderWants());
    }

    @Test
    void someoneInTrainingIsOfferedOnlyWhenTrainingMayBeCutShort() {
        PlayerSnapshot snapshot = player();
        astronaut(snapshot, "a1", 80, null);
        Astronaut trainee = astronaut(snapshot, "a2", 50, Ambition.Kind.FLY_IN_SPACE);
        trainee.setAssignmentState(AssignmentState.IN_TRAINING);
        trainee.setTrainingKind(TrainingKind.ADVANCED);
        trainee.setTrainingSeasonsTotal(3);

        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), false, Set.of(),
            (crew, slot) -> true).isEmpty(), "Auto-assign offers only the free");
        assertTrue(AmbitionSwaps.next(snapshot, CREWED, List.of("a1"), List.of(), true, Set.of(),
            (crew, slot) -> true).orElseThrow().fromTraining());
    }
}
