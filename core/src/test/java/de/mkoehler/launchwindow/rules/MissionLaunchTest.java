package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link MissionLaunch}.</p>
 *
 * @author Mario Koehler
 */
class MissionLaunchTest {

    private static final List<Double> RD_SLOT_BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    private final MissionComponent capsule =
        new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT, 3, 90, 82, RD_SLOT_BANDS, 0, List.of());
    private final Program program =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private final ProgramCategory category = new ProgramCategory("early-space", "Early Space");

    private final MissionStep crewedStep = new MissionStep("launch",
        List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
        List.of(new MissionStep.ControllerInvolvement(0, 100)),
        List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true);
    private final MissionConfiguration crewedConfiguration = new MissionConfiguration("crewed-std",
        "mercury-program", List.of(new MissionSeason(List.of(crewedStep))), 1, 1, 500);

    private final MissionStep uncrewedStep = new MissionStep("ops",
        List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
    private final MissionConfiguration uncrewedConfiguration = new MissionConfiguration("uncrewed-std",
        "mercury-program", List.of(new MissionSeason(List.of(uncrewedStep))), 0, 0, 0);

    private final ContentCatalog catalog = new ContentCatalog(List.of(category), List.of(program),
        List.of(crewedConfiguration, uncrewedConfiguration), List.of(capsule));

    /**
     * Builds a snapshot with the mercury program already open, two idle
     * astronauts, and one idle Mission Control officer.
     *
     * @return the built snapshot
     */
    private static PlayerSnapshot snapshotWithProgramOpenAndIdleCrew() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setAstronauts(List.of(new Astronaut("astro-1", "Jane Reyes", 34, 80, 60),
            new Astronaut("astro-2", "Kim Lee", 29, 90, 65)));
        snapshot.setMissionControlOfficers(List.of(new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40)));
        return snapshot;
    }

    /**
     * Verifies a well-formed crewed launch succeeds, creates the
     * in-progress mission, and assigns every supplied person.
     */
    @Test
    void launchesACrewedMissionAndAssignsEveryone() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of("mco-1"), catalog);

        assertEquals(MissionLaunchOutcome.LAUNCHED, outcome);
        assertEquals(1, snapshot.getInProgressMissions().size());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getAstronauts().get(0).getAssignmentState());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getMissionControlOfficers().get(0).getAssignmentState());
        String missionId = snapshot.getInProgressMissions().keySet().iterator().next();
        assertEquals(missionId, snapshot.getAstronauts().get(0).getAssignedMissionId());
        assertEquals(missionId, snapshot.getMissionControlOfficers().get(0).getAssignedMissionId());
    }

    /**
     * Verifies a well-formed uncrewed launch succeeds with empty id lists.
     */
    @Test
    void launchesAnUncrewedMissionWithNoPersonnel() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of(), List.of(), catalog);

        assertEquals(MissionLaunchOutcome.LAUNCHED, outcome);
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Verifies a successful launch deducts the configuration's own cost
     * from the player's funds.
     */
    @Test
    void launchDeductsTheConfigurationsCost() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of("mco-1"), catalog);

        assertEquals(10_000 - 500, snapshot.getFunds());
    }

    /**
     * Verifies a launch whose cost exceeds the player's current funds is
     * rejected, and nothing is mutated - the same "fail before any
     * mutation" contract every other rejection here already upholds.
     */
    @Test
    void rejectsLaunchWhenFundsAreInsufficient() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();
        snapshot.setFunds(499);

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of("mco-1"), catalog);

        assertEquals(MissionLaunchOutcome.INSUFFICIENT_FUNDS, outcome);
        assertEquals(499, snapshot.getFunds());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Verifies a launch whose cost exactly equals the player's current
     * funds is accepted, not rejected - the funds check must not be an
     * off-by-one strict inequality.
     */
    @Test
    void acceptsLaunchWhenFundsExactlyMatchCost() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();
        snapshot.setFunds(500);

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of("mco-1"), catalog);

        assertEquals(MissionLaunchOutcome.LAUNCHED, outcome);
        assertEquals(0, snapshot.getFunds());
    }

    /**
     * Verifies an unknown mission configuration id is rejected.
     */
    @Test
    void rejectsUnknownMissionConfiguration() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.UNKNOWN_MISSION_CONFIGURATION,
            MissionLaunch.launch(snapshot, "unknown", List.of(), List.of(), catalog));
    }

    /**
     * Verifies a configuration whose program has not been opened is
     * rejected.
     */
    @Test
    void rejectsLaunchWhenProgramNotOpen() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);

        assertEquals(MissionLaunchOutcome.PROGRAM_NOT_OPEN,
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of(), List.of(), catalog));
    }

    /**
     * Verifies supplying crew for an uncrewed mission (which requires
     * exactly 0) is rejected - the departure-protection exploit this
     * method's own Javadoc describes.
     */
    @Test
    void rejectsCrewOnAnUncrewedMission() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT,
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of("astro-1"), List.of(), catalog));
    }

    /**
     * Verifies launching a crewed mission with no crew supplied at all
     * (requires exactly 1) is rejected.
     */
    @Test
    void rejectsACrewedMissionWithNoCrewSupplied() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT,
            MissionLaunch.launch(snapshot, "crewed-std", List.of(), List.of("mco-1"), catalog));
    }

    /**
     * Verifies supplying one astronaut too many is rejected, even though
     * every supplied id is itself valid and idle.
     */
    @Test
    void rejectsTooManyAstronauts() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1", "astro-2"), List.of("mco-1"), catalog));
    }

    /**
     * Verifies supplying controllers for a configuration requiring exactly
     * 0 is rejected.
     */
    @Test
    void rejectsControllersWhenNoneRequired() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.WRONG_MISSION_CONTROL_OFFICER_COUNT,
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of(), List.of("mco-1"), catalog));
    }

    /**
     * Verifies launching a configuration that needs controllers with none
     * supplied is rejected.
     */
    @Test
    void rejectsACrewedMissionWithNoControllersSupplied() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.WRONG_MISSION_CONTROL_OFFICER_COUNT,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of(), catalog));
    }

    /**
     * Verifies an unknown astronaut id is rejected.
     */
    @Test
    void rejectsUnknownAstronautId() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.UNKNOWN_ASTRONAUT,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("unknown"), List.of("mco-1"), catalog));
    }

    /**
     * Verifies an astronaut who is not currently {@code IDLE} is rejected.
     */
    @Test
    void rejectsAnAstronautWhoIsNotIdle() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();
        snapshot.getAstronauts().get(0).setAssignmentState(AssignmentState.IN_TRAINING);

        assertEquals(MissionLaunchOutcome.ASTRONAUT_NOT_IDLE,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1"), List.of("mco-1"), catalog));
    }

    /**
     * Verifies a duplicate astronaut id is rejected as a duplicate, not
     * reported as a wrong-count problem (the list's size, 2, is also
     * wrong for a configuration requiring exactly 1 - the duplicate check
     * runs first).
     */
    @Test
    void rejectsDuplicateAstronautId() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        assertEquals(MissionLaunchOutcome.DUPLICATE_ASTRONAUT,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("astro-1", "astro-1"), List.of("mco-1"), catalog));
    }

    /**
     * Verifies nothing is mutated when a launch call ultimately fails -
     * the astronaut stays {@code IDLE}, no in-progress mission is created,
     * and no cost is deducted, even though the controller was valid and
     * funds were sufficient.
     */
    @Test
    void failedLaunchMutatesNothing() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, "crewed-std", List.of("unknown"), List.of("mco-1"), catalog);

        assertEquals(MissionLaunchOutcome.UNKNOWN_ASTRONAUT, outcome);
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(AssignmentState.IDLE, snapshot.getMissionControlOfficers().get(0).getAssignmentState());
        assertEquals(10_000, snapshot.getFunds());
    }

    /**
     * The copy that flies is built at launch (user decision 2026-10-07): later changes to the
     * component on the ground, from R&amp;D or another flight's failure, don't reach it, but do
     * reach the next mission.
     */
    @Test
    void aFlightKeepsTheReliabilityItWasLaunchedWith() {
        PlayerSnapshot snapshot = snapshotWithProgramOpenAndIdleCrew();
        snapshot.getMissionComponents().put("capsule-mercury",
            new de.mkoehler.launchwindow.model.OwnedMissionComponent(70.0, 3.0));
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of(), List.of(), catalog));
        de.mkoehler.launchwindow.model.InProgressMission flying =
            snapshot.getInProgressMissions().values().iterator().next();
        assertEquals(java.util.Map.of("capsule-mercury", 70.0), flying.getLaunchReliability());

        // Ground R&D raises the component; the flying copy stays as built.
        snapshot.getMissionComponents().get("capsule-mercury").setCurrentReliability(80.0);
        assertEquals(70.0, MissionProgress.flyingReliability(snapshot, flying, "capsule-mercury"), 1e-9);
        assertEquals(70.0, MissionProgress.componentContributionSum(snapshot, catalog, flying, uncrewedStep,
            goal -> true), 1e-9);
        assertEquals(MissionAssessment.assess(snapshot, catalog, flying, uncrewedConfiguration).steps().get(0)
            .success(), withReliability(70.0).steps().get(0).success(), 1e-9);

        // Another flight's failure lowers the ground template only: the next launch takes it.
        snapshot.getMissionComponents().get("capsule-mercury").setCurrentReliability(55.0);
        assertEquals(70.0, MissionProgress.flyingReliability(snapshot, flying, "capsule-mercury"), 1e-9);
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, "uncrewed-std", List.of(), List.of(), catalog));
        de.mkoehler.launchwindow.model.InProgressMission next = snapshot.getInProgressMissions().values().stream()
            .filter(mission -> mission != flying).findFirst().orElseThrow();
        assertEquals(55.0, MissionProgress.flyingReliability(snapshot, next, "capsule-mercury"), 1e-9);

        // A mission saved before launch values were kept reads the component as it is now.
        flying.setLaunchReliability(null);
        assertEquals(55.0, MissionProgress.flyingReliability(snapshot, flying, "capsule-mercury"), 1e-9);
    }

    private MissionAssessment.Assessment withReliability(double reliability) {
        PlayerSnapshot plain = snapshotWithProgramOpenAndIdleCrew();
        plain.getMissionComponents().put("capsule-mercury",
            new de.mkoehler.launchwindow.model.OwnedMissionComponent(reliability, 3.0));
        return MissionAssessment.assess(plain, catalog, uncrewedConfiguration, List.of(), List.of(), List.of());
    }
}
