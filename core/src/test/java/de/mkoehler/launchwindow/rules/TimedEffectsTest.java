package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for the timed effects sub-slice (b) adds: the tick that ages every timed
 * effect once per season, the launch cap and the component and employee
 * unavailability that {@link MissionLaunch} enforces, and the running-cost
 * multiplier. See {@code design.md} &sect;2.7.</p>
 *
 * @author Mario Koehler
 */
class TimedEffectsTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of(), 0);
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final MissionConfiguration CONFIG = new MissionConfiguration("crewless-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("s1",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false)))),
        0, 0, 0, 0);
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(List.of(CATEGORY), List.of(PROGRAM),
        List.of(CONFIG), List.of(CAPSULE));
    private static final RandomEventRoster QUIET = new RandomEventRoster(0, 0,
        RandomEventRoster.baspmClose().events());

    private static PlayerSnapshot player() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        return snapshot;
    }

    /**
     * A multiplier set for N seasons is in force for the next N seasons, then resets to 1.0.
     */
    @Test
    void multiplierLastsItsSeasonsThenResets() {
        PlayerSnapshot snapshot = player();
        snapshot.setRdResults(0.5, 2);

        RandomEvents.advanceSeason(snapshot, QUIET, new Random(1), 1, 1);
        assertEquals(0.5, snapshot.getRdResultsMultiplier());
        assertEquals(1, snapshot.getRdResultsSeasonsRemaining());

        RandomEvents.advanceSeason(snapshot, QUIET, new Random(1), 2, 2);
        assertEquals(1.0, snapshot.getRdResultsMultiplier());
        assertEquals(0, snapshot.getRdResultsSeasonsRemaining());
    }

    /**
     * The running-cost multiplier scales salaries and maintenance alike.
     */
    @Test
    void costMultiplierScalesRunningCosts() {
        PlayerSnapshot snapshot = player();
        Astronaut astronaut = new Astronaut("a1", "a1", 30, 70, 60);
        astronaut.setSalary(100);
        snapshot.setAstronauts(List.of(astronaut));
        assertEquals(100, BudgetProgress.runningCosts(snapshot, CATALOG));

        snapshot.setCostMultiplier(0.75, 3);
        assertEquals(75, BudgetProgress.runningCosts(snapshot, CATALOG));
    }

    /**
     * A launch cap of zero refuses the launch, and a cap of one allows exactly one per window.
     */
    @Test
    void launchCapLimitsLaunchesPerWindow() {
        PlayerSnapshot snapshot = player();
        snapshot.setLaunchCap(0);
        assertEquals(MissionLaunchOutcome.LAUNCH_CAP_REACHED,
            MissionLaunch.launch(snapshot, "crewless-std", List.of(), List.of(), CATALOG));

        snapshot.setLaunchCap(1);
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, "crewless-std", List.of(), List.of(), CATALOG));
        assertEquals(MissionLaunchOutcome.LAUNCH_CAP_REACHED,
            MissionLaunch.launch(snapshot, "crewless-std", List.of(), List.of(), CATALOG));
        assertEquals(1, snapshot.getLaunchesThisWindow());
    }

    /**
     * A configuration that flies an unavailable component is refused.
     */
    @Test
    void unavailableComponentBlocksItsMissions() {
        PlayerSnapshot snapshot = player();
        snapshot.getUnavailableComponents().put("capsule-mercury", 1);

        assertEquals(MissionLaunchOutcome.MISSION_COMPONENT_UNAVAILABLE,
            MissionLaunch.launch(snapshot, "crewless-std", List.of(), List.of(), CATALOG));
    }

    /**
     * An employee who is unavailable is treated as not idle for a launch.
     */
    @Test
    void unavailableAstronautCannotBeAssigned() {
        PlayerSnapshot snapshot = player();
        MissionConfiguration crewed = new MissionConfiguration("crewed-std", "mercury-program",
            List.of(new MissionSeason(List.of(new MissionStep("s1",
                List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(),
                List.of(new MissionStep.CrewInvolvement(de.mkoehler.launchwindow.model.AstronautSkill.PILOTING, 100)),
                false)))),
            1, 0, 0, 0);
        ContentCatalog crewCatalog = new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM), List.of(crewed),
            List.of(CAPSULE));
        Astronaut astronaut = new Astronaut("a1", "a1", 30, 70, 60);
        astronaut.setUnavailableSeasons(1);
        snapshot.setAstronauts(List.of(astronaut));

        assertEquals(MissionLaunchOutcome.ASTRONAUT_NOT_IDLE,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("a1"), List.of(), crewCatalog));

        astronaut.setUnavailableSeasons(0);
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, "crewed-std", List.of("a1"), List.of(), crewCatalog));
        assertEquals(AssignmentState.ASSIGNED, astronaut.getAssignmentState());
    }

    /**
     * Unavailable components and employees count down once per season, and clear at zero.
     */
    @Test
    void unavailabilityCountsDownOncePerSeason() {
        PlayerSnapshot snapshot = player();
        snapshot.getUnavailableComponents().put("capsule-mercury", 2);
        Astronaut astronaut = new Astronaut("a1", "a1", 30, 70, 60);
        astronaut.setUnavailableSeasons(1);
        snapshot.setAstronauts(List.of(astronaut));

        RandomEvents.advanceSeason(snapshot, QUIET, new Random(1), 1, 1);
        assertEquals(1, snapshot.getUnavailableComponents().get("capsule-mercury"));
        assertEquals(0, astronaut.getUnavailableSeasons());

        RandomEvents.advanceSeason(snapshot, QUIET, new Random(1), 2, 2);
        assertTrue(snapshot.getUnavailableComponents().isEmpty());
    }
}
