package de.mkoehler.launchwindow.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PlayerSnapshot}, including the Jackson round-trip that
 * every on-disk snapshot (see {@code design.md} &sect;3.7) depends on.</p>
 *
 * @author Mario Koehler
 */
class PlayerSnapshotTest {

    /**
     * Verifies that a freshly-constructed snapshot has empty personnel
     * rosters rather than {@code null} lists.
     */
    @Test
    void newSnapshotStartsWithEmptyRosters() {
        PlayerSnapshot snapshot =
            new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 25_000, 0, 5_000, 1);

        assertTrue(snapshot.getAstronauts().isEmpty());
        assertTrue(snapshot.getSetSpecialists().isEmpty());
        assertTrue(snapshot.getMissionControlOfficers().isEmpty());
    }

    /**
     * Verifies that a snapshot's identity/economy fields and all three
     * personnel rosters, including per-employee skill values, survive a
     * full serialize/deserialize round trip unchanged.
     */
    @Test
    void roundTripsThroughJsonIncludingAllPersonnelRosters() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PlayerSnapshot original =
            new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 25_000, 100, 5_000, 3);

        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);
        astronaut.setSkill(AstronautSkill.PILOTING, 70.5);
        astronaut.setAssignmentState(AssignmentState.ASSIGNED);
        astronaut.setAgingProgressSeasons(2);
        original.setAstronauts(List.of(astronaut));

        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);
        specialist.setSkill(SetSkill.ROCKETS, 63);
        specialist.setAssignmentState(AssignmentState.IN_TRAINING);
        specialist.setTrainingKind(TrainingKind.BASIC);
        specialist.setTrainingSeasonsCompleted(1);
        specialist.setTrainingSeasonsTotal(2);
        specialist.setTrainingTargetSkill(SetSkill.ROCKETS);
        original.setSetSpecialists(List.of(specialist));

        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, 50);
        original.setMissionControlOfficers(List.of(officer));

        String json = mapper.writeValueAsString(original);
        PlayerSnapshot restored = mapper.readValue(json, PlayerSnapshot.class);

        assertEquals(original.getAgencyName(), restored.getAgencyName());
        assertEquals(original.getArchetype(), restored.getArchetype());
        assertEquals(original.getFunds(), restored.getFunds());
        assertEquals(original.getPrestige(), restored.getPrestige());
        assertEquals(original.getFundingRatePerSeason(), restored.getFundingRatePerSeason());
        assertEquals(original.getSeason(), restored.getSeason());

        assertEquals(1, restored.getAstronauts().size());
        Astronaut restoredAstronaut = restored.getAstronauts().get(0);
        assertEquals("astro-1", restoredAstronaut.getId());
        assertEquals("Jane Reyes", restoredAstronaut.getName());
        assertEquals(34, restoredAstronaut.getAge());
        assertEquals(80, restoredAstronaut.getMorale());
        assertEquals(60, restoredAstronaut.getLearningPotential());
        assertEquals(AssignmentState.ASSIGNED, restoredAstronaut.getAssignmentState());
        assertEquals(70.5, restoredAstronaut.getSkill(AstronautSkill.PILOTING));
        assertEquals(2, restoredAstronaut.getAgingProgressSeasons());

        assertEquals(1, restored.getSetSpecialists().size());
        SetSpecialist restoredSpecialist = restored.getSetSpecialists().get(0);
        assertEquals("set-1", restoredSpecialist.getId());
        assertEquals(AssignmentState.IN_TRAINING, restoredSpecialist.getAssignmentState());
        assertEquals(63, restoredSpecialist.getSkill(SetSkill.ROCKETS));
        assertEquals(TrainingKind.BASIC, restoredSpecialist.getTrainingKind());
        assertEquals(1, restoredSpecialist.getTrainingSeasonsCompleted());
        assertEquals(2, restoredSpecialist.getTrainingSeasonsTotal());
        assertEquals(SetSkill.ROCKETS, restoredSpecialist.getTrainingTargetSkill());

        assertEquals(1, restored.getMissionControlOfficers().size());
        MissionControlOfficer restoredOfficer = restored.getMissionControlOfficers().get(0);
        assertEquals("mco-1", restoredOfficer.getId());
        assertEquals(50, restoredOfficer.getSkill(MissionControlSkill.PROPULSION));
    }

    /**
     * Verifies that the roster setters defensively copy their argument, so
     * passing an immutable list (e.g. {@code List.of(...)}) does not leave
     * the snapshot's own roster immutable.
     */
    @Test
    void rosterSettersCopyAnImmutableListIntoAMutableOne() {
        PlayerSnapshot snapshot =
            new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 25_000, 0, 5_000, 1);
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);

        snapshot.setAstronauts(List.of(astronaut));

        assertDoesNotThrow(() -> snapshot.getAstronauts().add(new Astronaut("astro-2", "Kim Lee", 29, 90, 65)));
    }
}
