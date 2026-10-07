package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests a configuration's people against the agency's (play-test request, 2026-10-07): who is free
 * as assembly counts it, who is coming from training, and the seats' skills.
 *
 * @author Mario Koehler
 */
class StaffingTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static MissionControlOfficer controller(String id, AssignmentState state, TrainingKind kind, int done,
            int total) {
        MissionControlOfficer officer = new MissionControlOfficer(id, "C " + id, 30, 60, 50);
        officer.setAssignmentState(state);
        officer.setTrainingKind(kind);
        officer.setTrainingSeasonsCompleted(done);
        officer.setTrainingSeasonsTotal(total);
        return officer;
    }

    @Test
    void freeIsWhatAssemblyCountsAndBasicTrainingSaysWhenItEnds() {
        MissionConfiguration orbital = CATALOG.missionConfiguration("vostok-orbital").orElseThrow();
        PlayerSnapshot state = new PlayerSnapshot("A", Archetype.SOVIET, 0, 0, 0, 1);
        state.getAstronauts().add(new Astronaut("a1", "A", 30, 60, 50));
        state.setMissionControlOfficers(List.of(
            controller("c1", AssignmentState.IDLE, TrainingKind.NONE, 0, 0),
            controller("c2", AssignmentState.IN_TRAINING, TrainingKind.BASIC, 1, 2),
            controller("c3", AssignmentState.IN_TRAINING, TrainingKind.BASIC, 0, 2),
            controller("c4", AssignmentState.IN_TRAINING, TrainingKind.ADVANCED, 1, 3),
            controller("c5", AssignmentState.ASSIGNED, TrainingKind.NONE, 0, 0)));
        List<Staffing.Group> groups = Staffing.of(orbital, state);
        Staffing.Group controllers = groups.get(1);
        assertEquals(Missions.controllerCount(orbital), controllers.needed());
        assertEquals(1, controllers.free());
        assertEquals(2, controllers.inBasic());
        assertEquals(1, controllers.basicSoonest());
        assertEquals(1, controllers.inTraining());
        assertEquals(1, controllers.busy());
        assertTrue(controllers.isShort());
        assertTrue(Staffing.advice(controllers).contains("first done in 1 season"));
        assertTrue(Staffing.cardLine(orbital, state).startsWith("Free now: 1 of "
            + (Missions.crewCount(orbital) + Missions.capcomCount(orbital)) + " astronauts"));
    }

    @Test
    void enoughPeopleMeansNoAdviceAndEverySeatNamesASkill() {
        MissionConfiguration orbital = CATALOG.missionConfiguration("vostok-orbital").orElseThrow();
        PlayerSnapshot state = new PlayerSnapshot("A", Archetype.SOVIET, 0, 0, 0, 1);
        for (int i = 0; i < 12; i++) {
            state.getAstronauts().add(new Astronaut("a" + i, "A", 30, 60, 50));
            state.getMissionControlOfficers().add(controller("c" + i, AssignmentState.IDLE, TrainingKind.NONE, 0, 0));
        }
        assertFalse(Staffing.isShort(orbital, state));
        Staffing.of(orbital, state).forEach(group -> assertNull(Staffing.advice(group)));
        List<String[]> seats = Staffing.seats(orbital);
        assertEquals(Missions.crewCount(orbital) + Missions.controllerCount(orbital) + Missions.capcomCount(orbital),
            seats.size());
        seats.forEach(seat -> assertFalse(seat[1].isBlank(), seat[0]));
    }

    @Test
    void seatTitlesReadInNormalCaseButCallSignsStay() {
        assertEquals("Cosmonaut A", Staffing.title("COSMONAUT A"));
        assertEquals("Flight director", Staffing.title("FLIGHT DIRECTOR"));
        assertEquals("Spacecraft systems", Staffing.title("SPACECRAFT SYSTEMS"));
        assertEquals("RETRO", Staffing.title("RETRO"));
        assertEquals("CAPCOM", Staffing.title("CAPCOM"));
    }
}
