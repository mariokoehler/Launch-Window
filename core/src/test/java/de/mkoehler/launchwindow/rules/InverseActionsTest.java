package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the M4 personnel actions and mission cancellation - the validated
 * actions and inverses dispatch needs ({@code design.md} &sect;5, M4 decisions 1 and 5).
 *
 * @author Mario Koehler
 */
class InverseActionsTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static PlayerSnapshot soviet() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.SOVIET, 50_000, 1_000, 3_000, 1);
        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(snapshot, "sputnik-1", CATALOG));
        assertEquals(RocketProgramOutcome.OPENED, RocketPrograms.open(snapshot, "r7-sputnik", CATALOG));
        List<MissionControlOfficer> controllers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            controllers.add(new MissionControlOfficer("mc" + i, "mc" + i, 30, 70, 50));
        }
        snapshot.setMissionControlOfficers(controllers);
        return snapshot;
    }

    private static LaunchRequest sputnik(String configurationId) {
        // Sputnik's steps use the BOOSTER and SPACECRAFT SYSTEMS slots; the third slot stays empty.
        return new LaunchRequest(configurationId, List.of(), List.of("mc0", "mc1"), List.of(),
            List.of("r7-sputnik"));
    }

    @Test
    void dismissIsFreeButNotForAnAssignedController() {
        PlayerSnapshot snapshot = soviet();
        long funds = snapshot.getFunds();
        MissionLaunch.launch(snapshot, sputnik("sputnik-1-orbit"), CATALOG);
        assertEquals(PersonnelActions.DismissOutcome.ASSIGNED_TO_MISSION,
            PersonnelActions.dismiss(snapshot, PersonnelGroup.MISSION_CONTROL, "mc0"));

        snapshot.getMissionControlOfficers().add(new MissionControlOfficer("spare", "spare", 30, 70, 50));
        long before = snapshot.getFunds();
        assertEquals(PersonnelActions.DismissOutcome.DISMISSED,
            PersonnelActions.dismiss(snapshot, PersonnelGroup.MISSION_CONTROL, "spare"));
        assertEquals(before, snapshot.getFunds());
        assertEquals(PersonnelActions.DismissOutcome.UNKNOWN_EMPLOYEE,
            PersonnelActions.dismiss(snapshot, PersonnelGroup.MISSION_CONTROL, "spare"));
        assertTrue(funds > snapshot.getFunds());
    }

    @Test
    void scrubBeforeFlightRefundsTheWholeLaunchCostAndFreesTheTeam() {
        PlayerSnapshot snapshot = soviet();
        long before = snapshot.getFunds();
        MissionLaunch.launch(snapshot, sputnik("sputnik-1-orbit"), CATALOG);
        InProgressMission mission = snapshot.getInProgressMissions().values().iterator().next();
        assertEquals(488 + 1_625, mission.getLaunchCost(), "Sputnik payload plus the R-7's unit cost");

        assertEquals(MissionCancellation.Outcome.SCRUBBED,
            MissionCancellation.cancel(snapshot, mission.getId(), CATALOG));

        assertEquals(before, snapshot.getFunds());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(0, snapshot.getLaunchesThisWindow());
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            assertEquals(AssignmentState.IDLE, officer.getAssignmentState());
            assertNull(officer.getAssignedMissionId());
        }
    }

    @Test
    void uncrewedMissionInFlightCancelsWithBaspmsTerms() {
        PlayerSnapshot snapshot = soviet();
        MissionLaunch.launch(snapshot, sputnik("sputnik-1-extended"), CATALOG);
        InProgressMission mission = snapshot.getInProgressMissions().values().iterator().next();
        mission.setCurrentSeasonIndex(1);
        long funds = snapshot.getFunds();
        long prestige = snapshot.getPrestige();

        assertEquals(MissionCancellation.Outcome.CANCELLED,
            MissionCancellation.cancel(snapshot, mission.getId(), CATALOG));

        assertEquals(funds + mission.getLaunchCost() * 10 / 100, snapshot.getFunds());
        assertEquals(prestige - 375, snapshot.getPrestige());
    }

    @Test
    void crewedMissionInFlightCannotBeAbandoned() {
        PlayerSnapshot snapshot = soviet();
        InProgressMission crewed = new InProgressMission("m9", "vostok-orbital", List.of("a1"), List.of());
        crewed.setCurrentSeasonIndex(1);
        snapshot.getInProgressMissions().put("m9", crewed);
        snapshot.getAstronauts().add(new Astronaut("a1", "a1", 30, 70, 50));

        assertEquals(MissionCancellation.Outcome.CREWED_MISSION_IN_FLIGHT,
            MissionCancellation.cancel(snapshot, "m9", CATALOG));
        assertTrue(snapshot.getInProgressMissions().containsKey("m9"));
    }

    @Test
    void trainingAndResearchStartOnlyFromIdleAndValidTargets() {
        PlayerSnapshot snapshot = soviet();
        SetSpecialist specialist = new SetSpecialist("s1", "s1", 30, 70, 50);
        specialist.setSkill(SetSkill.ROCKETS, 40);
        snapshot.setSetSpecialists(new ArrayList<>(List.of(specialist)));

        assertEquals(PersonnelActions.StartTrainingOutcome.UNKNOWN_SKILL, PersonnelActions.startTraining(snapshot,
            PersonnelGroup.SET, "s1", TrainingKind.ADVANCED, "PILOTING", 0));
        // Basic training only follows a hire; a player can't choose it.
        assertEquals(PersonnelActions.StartTrainingOutcome.UNSUPPORTED_KIND, PersonnelActions.startTraining(snapshot,
            PersonnelGroup.SET, "s1", TrainingKind.BASIC, "ROCKETS", 0));
        assertEquals(PersonnelActions.StartTrainingOutcome.TARGET_NOT_ABOVE_SKILL, PersonnelActions.startTraining(
            snapshot, PersonnelGroup.SET, "s1", TrainingKind.PERMANENT, "ROCKETS", 30));
        assertEquals(PersonnelActions.ResearchOutcome.COMPONENT_NOT_AVAILABLE,
            PersonnelActions.assignResearch(snapshot, "s1", "atlas", CATALOG));
        assertEquals(PersonnelActions.ResearchOutcome.ASSIGNED,
            PersonnelActions.assignResearch(snapshot, "s1", "r7-sputnik", CATALOG));
        assertEquals(0, specialist.getRdSlot());
        assertEquals(PersonnelActions.StartTrainingOutcome.NOT_IDLE, PersonnelActions.startTraining(snapshot,
            PersonnelGroup.SET, "s1", TrainingKind.ADVANCED, "ROCKETS", 0));

        assertEquals(PersonnelActions.ResearchOutcome.RELEASED, PersonnelActions.releaseResearch(snapshot, "s1"));
        assertEquals(PersonnelActions.StartTrainingOutcome.STARTED, PersonnelActions.startTraining(snapshot,
            PersonnelGroup.SET, "s1", TrainingKind.PERMANENT, "ROCKETS", 60));
        assertEquals(AssignmentState.IN_TRAINING, specialist.getAssignmentState());
        assertEquals(TrainingProgress.ADVANCED_TRAINING_SEASONS, specialist.getTrainingSeasonsTotal());
        assertEquals(60, specialist.getTrainingTargetLevel());
    }
}
