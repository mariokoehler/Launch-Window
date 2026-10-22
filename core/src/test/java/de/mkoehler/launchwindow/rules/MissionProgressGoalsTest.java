package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
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
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Scenario tests for goals inside {@link MissionProgress}: settlement and
 * the cancellation cascade, the same-turn tally rule, forfeiture on a failing
 * season, the minor-glitch halving, and the live goal-dependency modifier. Each
 * test runs the real resolver against a real {@link GoalTally}. See
 * {@code design.md} &sect;2.6(d) and {@code baspm.md} &sect;11.3/&sect;11.7.</p>
 *
 * @author Mario Koehler
 */
class MissionProgressGoalsTest {

    private static final String CONFIG = "goal-std";

    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");

    private static final List<Goal> GOALS = List.of(
        new Goal("orbit", 200, 100, List.of("basic-training")),
        new Goal("basic-training", 50, 25, List.of("pre-flight")),
        new Goal("pre-flight", 30, 15, List.of()));

    /** Post-flight padding after a single-component, no-crew step: one reliability draw. */
    private static final double[] POST_FLIGHT = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1};

    private static MissionStep step(String id, int share, long deduction, List<String> goals) {
        return new MissionStep(id, List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false, share, deduction, goals);
    }

    private static MissionConfiguration configuration(List<MissionStep> steps, List<MissionConfiguration.GoalModifier> deps) {
        return new MissionConfiguration(CONFIG, "mercury-program", List.of(new MissionSeason(steps)),
            0, 0, 0, 1_000, deps, List.of());
    }

    private static ContentCatalog catalog(MissionConfiguration configuration) {
        return new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM), List.of(configuration), List.of(CAPSULE), GOALS);
    }

    private static PlayerSnapshot snapshotWithMission(long prestige, String... missionIds) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, prestige, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setMissionComponents(Map.of("capsule-mercury", new OwnedMissionComponent(10.0, 10.0)));
        for (String missionId : missionIds) {
            snapshot.getInProgressMissions().put(missionId, new InProgressMission(missionId, CONFIG, List.of(), List.of()));
        }
        return snapshot;
    }

    private static double[] rolls(double... stepRolls) {
        double[] all = new double[stepRolls.length + POST_FLIGHT.length];
        System.arraycopy(stepRolls, 0, all, 0, stepRolls.length);
        System.arraycopy(POST_FLIGHT, 0, all, stepRolls.length, POST_FLIGHT.length);
        return all;
    }

    private static List<GoalSettled> settlements(List<ResolutionEvent> events) {
        return events.stream().filter(e -> e instanceof GoalSettled).map(e -> (GoalSettled) e).toList();
    }

    /**
     * A completed flight credits its crew with a flight, and with one first if a step set a first in
     * the game, however many goals the cascade credits (PR duty's fame, design.md section 2.2(c)).
     */
    @Test
    void aCompletedFlightCreditsItsCrewOnceForAFirst() {
        MissionConfiguration crewed = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0, List.of("orbit"))))), 1, 0, 0, 1_000, List.of(),
            List.of());
        ContentCatalog catalog = catalog(crewed);
        GoalTally tally = new GoalTally();
        PlayerSnapshot snapshot = snapshotWithMission(0);
        Astronaut pilot = new Astronaut("a1", "Pilot", 30, 60, 50);
        pilot.setAssignmentState(AssignmentState.ASSIGNED);
        pilot.setAssignedMissionId("m0");
        snapshot.getAstronauts().add(pilot);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", CONFIG, List.of("a1"), List.of()));

        tally.commit(MissionProgress.advanceSeason(snapshot, catalog, scripted(rolls(99.0)), tally, 7));
        assertEquals(1, pilot.getFlightsFlown());
        assertEquals(1, pilot.getFirstsFlown(), "orbit and its cascade set three firsts, but count once");

        pilot.setAssignmentState(AssignmentState.ASSIGNED);
        pilot.setAssignedMissionId("m1");
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", CONFIG, List.of("a1"), List.of()));
        MissionProgress.advanceSeason(snapshot, catalog, scripted(rolls(99.0)), tally, 8);
        assertEquals(2, pilot.getFlightsFlown());
        assertEquals(1, pilot.getFirstsFlown(), "the second flight set no first");
    }

    /**
     * Achieving a goal pays its first-time value on top of the step's own share,
     * records the achievement, and counts once the tally commits.
     */
    @Test
    void achievingAGoalPaysFirstTimeAndCountsOnCommit() {
        ContentCatalog catalog = catalog(configuration(List.of(step("s1", 100, 0, List.of("orbit"))), List.of()));
        PlayerSnapshot snapshot = snapshotWithMission(0, "m0");
        GoalTally tally = new GoalTally();

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
            scripted(rolls(99.0)), tally, 7);

        assertEquals(1_280, snapshot.getPrestige(), "orbit 200 + its cascade 50 + 30 on top of 1000");
        assertEquals(7, snapshot.getAchievedGoals().get("orbit"));
        assertEquals(0, tally.countOf("orbit"), "the tally must not move during the pass");
        tally.commit(events);
        assertEquals(1, tally.countOf("orbit"));
    }

    /**
     * Achieving a goal cancels the goals it lists, recursively, and every
     * cancelled goal pays its own first-time prestige and counts toward the tally.
     */
    @Test
    void cancellationCascadePaysAndCountsEveryCancelledGoal() {
        ContentCatalog catalog = catalog(configuration(List.of(step("s1", 100, 0, List.of("orbit"))), List.of()));
        PlayerSnapshot snapshot = snapshotWithMission(0, "m0");
        GoalTally tally = new GoalTally();

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
            scripted(rolls(99.0)), tally, 1);

        assertEquals(1_000 + 200 + 50 + 30, snapshot.getPrestige());
        assertEquals(Map.of("orbit", 1), snapshot.getAchievedGoals());
        assertEquals(List.of("basic-training", "pre-flight"), List.copyOf(snapshot.getCancelledGoalIds()));
        assertEquals(3, settlements(events).size());
        List<GoalRecord> records = snapshot.getGoalRecords();
        assertEquals(List.of("orbit", "basic-training", "pre-flight"), records.stream().map(GoalRecord::goalId).toList());
        assertEquals(java.util.Arrays.asList(null, "orbit", "orbit"),
            records.stream().map(GoalRecord::creditedThroughGoalId).toList());
        assertEquals(List.of(200L, 50L, 30L), records.stream().map(GoalRecord::prestige).toList());
        assertTrue(records.stream().allMatch(record -> record.firstInGame() && record.turn() == 1
            && record.missionConfigurationId() != null));
        tally.commit(events);
        assertEquals(1, tally.countOf("basic-training"));
        assertEquals(1, tally.countOf("pre-flight"));
    }

    /**
     * Two players settling the same goal in one resolution both pay the
     * first-time value, because the tally is only committed after both pass.
     * A third player resolving afterward pays the second-time value.
     */
    @Test
    void sameTurnAchieversBothPayFirstTimeAndLaterAchieverPaysSecondTime() {
        ContentCatalog catalog = catalog(configuration(List.of(step("s1", 100, 0, List.of("orbit"))), List.of()));
        GoalTally tally = new GoalTally();
        PlayerSnapshot alpha = snapshotWithMission(0, "m0");
        PlayerSnapshot beta = snapshotWithMission(0, "m0");

        List<ResolutionEvent> alphaEvents = MissionProgress.advanceSeason(alpha, catalog, scripted(rolls(99.0)), tally, 1);
        List<ResolutionEvent> betaEvents = MissionProgress.advanceSeason(beta, catalog, scripted(rolls(99.0)), tally, 1);
        assertEquals(1_280, alpha.getPrestige());
        assertEquals(1_280, beta.getPrestige());

        tally.commit(alphaEvents);
        tally.commit(betaEvents);
        assertEquals(2, tally.countOf("orbit"));

        PlayerSnapshot gamma = snapshotWithMission(0, "m0");
        MissionProgress.advanceSeason(gamma, catalog, scripted(rolls(99.0)), tally, 2);
        assertEquals(1_140, gamma.getPrestige(), "second-time cascade: orbit 100 + 25 + 15, on top of the step's 1000");
    }

    /**
     * The race exercised through the real launch path: two players launch the same
     * configuration with {@link MissionLaunch}, both settle the goal in one
     * resolution, and both pay first-time. The game-wide count moves only at commit.
     */
    @Test
    void sameTurnRaceThroughRealLaunchPathPaysFirstTimeForBothPlayers() {
        ContentCatalog catalog = catalog(configuration(List.of(step("s1", 100, 0, List.of("orbit"))), List.of()));
        GoalTally tally = new GoalTally();
        PlayerSnapshot alpha = snapshotWithMission(0);
        PlayerSnapshot beta = snapshotWithMission(0);
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(alpha, CONFIG, List.of(), List.of(), catalog));
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(beta, CONFIG, List.of(), List.of(), catalog));

        List<ResolutionEvent> alphaEvents = MissionProgress.advanceSeason(alpha, catalog,
            scripted(rolls(99.0)), tally, 3);
        List<ResolutionEvent> betaEvents = MissionProgress.advanceSeason(beta, catalog,
            scripted(rolls(99.0)), tally, 3);

        assertEquals(1_280, alpha.getPrestige());
        assertEquals(1_280, beta.getPrestige());
        assertEquals(0, tally.countOf("orbit"));
        tally.commit(alphaEvents);
        tally.commit(betaEvents);
        assertEquals(2, tally.countOf("orbit"));
    }

    /**
     * A failing step forfeits the prestige its season's earlier steps earned,
     * goal prestige included - but the goal stays achieved, and it still counts.
     */
    @Test
    void failureForfeitsGoalPrestigeButTheGoalStillCounts() {
        MissionConfiguration configuration = configuration(List.of(
            step("s1", 50, 0, List.of("orbit")), step("s2", 50, 150, List.of())), List.of());
        ContentCatalog catalog = catalog(configuration);
        PlayerSnapshot snapshot = snapshotWithMission(500, "m0");
        GoalTally tally = new GoalTally();

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
            scripted(rolls(99.0, 0.0, 30.0)), tally, 1);

        assertEquals(350, snapshot.getPrestige(), "500 - 150; step 1's 500 + 200 goal prestige is forfeited");
        assertTrue(snapshot.getAchievedGoals().containsKey("orbit"));
        tally.commit(events);
        assertEquals(1, tally.countOf("orbit"));
    }

    /** A crewed flight: a step reaching "orbit", then a crew-risk step that a scripted roll decides. */
    private static PlayerSnapshot crewedFlight(ContentCatalog[] catalog) {
        MissionStep risky = new MissionStep("s2", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true, 50, 150,
            List.of());
        MissionConfiguration crewed = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 50, 0, List.of("orbit")), risky))), 1, 0, 0, 1_000,
            List.of(), List.of());
        catalog[0] = catalog(crewed);
        PlayerSnapshot snapshot = snapshotWithMission(500);
        Astronaut pilot = new Astronaut("a1", "Pilot", 30, 60, 50);
        pilot.setAssignmentState(AssignmentState.ASSIGNED);
        pilot.setAssignedMissionId("m0");
        snapshot.getAstronauts().add(pilot);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", CONFIG, List.of("a1"), List.of()));
        return snapshot;
    }

    /**
     * A goal counts only if the crew comes home (design.md 2.6, 2026-10-22): a crew lost after a step reached
     * "orbit" takes back it and its cascade, with their records, and reports what was withheld.
     */
    @Test
    void aLostCrewEarnsNoGoals() {
        ContentCatalog[] catalog = new ContentCatalog[1];
        PlayerSnapshot snapshot = crewedFlight(catalog);
        GoalTally tally = new GoalTally();

        // Step 1 passes; step 2 fails into the catastrophic band (no controllers: 10).
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog[0],
            scripted(rolls(99.0, 0.0, 5.0)), tally, 4);

        assertTrue(events.stream().anyMatch(e -> e instanceof MissionStepResolved r
            && r.outcome() == MissionStepOutcome.CATASTROPHIC));
        assertTrue(settlements(events).isEmpty(), "no goal settles");
        assertTrue(snapshot.getAchievedGoals().isEmpty());
        assertTrue(snapshot.getCancelledGoalIds().isEmpty());
        assertTrue(snapshot.getGoalRecords().isEmpty());
        assertEquals(List.of(new GoalsWithheld("m0", CONFIG, List.of("orbit", "basic-training", "pre-flight"),
            List.of("orbit", "basic-training", "pre-flight"))),
            events.stream().filter(GoalsWithheld.class::isInstance).toList());
        assertEquals(350, snapshot.getPrestige(), "the failing step's deduction, as before");
        tally.commit(events);
        assertEquals(0, tally.countOf("orbit"), "nobody holds it, so the next agency is still first");
    }

    /** A flight cut short with the crew alive keeps what it reached: the crew came home. */
    @Test
    void anAbortedCrewedFlightKeepsItsGoals() {
        ContentCatalog[] catalog = new ContentCatalog[1];
        PlayerSnapshot snapshot = crewedFlight(catalog);

        // Step 2 fails outside the catastrophic band: an abort.
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog[0],
            scripted(rolls(99.0, 0.0, 30.0)), new GoalTally(), 4);

        assertTrue(events.stream().anyMatch(e -> e instanceof MissionStepResolved r
            && r.outcome() == MissionStepOutcome.FAILURE));
        assertEquals(Map.of("orbit", 4), snapshot.getAchievedGoals());
        assertEquals(3, settlements(events).size());
        assertTrue(events.stream().noneMatch(GoalsWithheld.class::isInstance));
    }

    /**
     * A minor glitch halves the step's share and its goal prestige together,
     * and still achieves the goal.
     */
    @Test
    void minorGlitchHalvesGoalPrestigeAndStillAchievesTheGoal() {
        ContentCatalog catalog = catalog(configuration(List.of(step("s1", 100, 0, List.of("orbit"))), List.of()));
        PlayerSnapshot snapshot = snapshotWithMission(0, "m0");

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
            scripted(rolls(0.0, 80.0)), new GoalTally(), 1);

        assertEquals(MissionStepOutcome.MINOR_GLITCH, ((MissionStepResolved) events.get(0)).outcome());
        assertEquals((1_000 + 280) / 2, snapshot.getPrestige());
        assertTrue(snapshot.getAchievedGoals().containsKey("orbit"));
    }

    /**
     * The goal-dependency modifier is computed live: an unmet dependency takes
     * reliability away, and it stops applying the moment the goal is fulfilled.
     */
    @Test
    void goalModifierIsLiveAgainstTheCurrentGoalState() {
        MissionConfiguration configuration = configuration(List.of(step("s1", 100, 0, List.of())),
            List.of(new MissionConfiguration.GoalModifier("orbit", "capsule-mercury", 5)));
        PlayerSnapshot snapshot = snapshotWithMission(0);

        assertEquals(-5, MissionProgress.goalModifierFor(snapshot, configuration, "capsule-mercury"));
        snapshot.getAchievedGoals().put("orbit", 1);
        assertEquals(0, MissionProgress.goalModifierFor(snapshot, configuration, "capsule-mercury"));
        assertEquals(0, MissionProgress.goalModifierFor(snapshot, configuration, "other-component"));
    }

    /**
     * A goal-linked boost adds its amount only once the goal is fulfilled.
     */
    @Test
    void goalBoostAddsOnceTheGoalIsFulfilled() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0, List.of())))), 0, 0, 0, 1_000, List.of(),
            List.of(new MissionConfiguration.GoalModifier("basic-training", "capsule-mercury", 4)));
        PlayerSnapshot snapshot = snapshotWithMission(0);

        assertEquals(0, MissionProgress.goalModifierFor(snapshot, configuration, "capsule-mercury"));
        snapshot.getCancelledGoalIds().add("basic-training");
        assertEquals(4, MissionProgress.goalModifierFor(snapshot, configuration, "capsule-mercury"),
            "a cancelled goal counts as fulfilled");
    }

    /**
     * The catalog rejects a goal cancelling a goal it doesn't define.
     */
    @Test
    void catalogRejectsAGoalCancellingAnUnknownGoal() {
        List<Goal> badGoals = List.of(new Goal("orbit", 1, 1, List.of("nowhere")));
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
            List.of(configuration(List.of(step("s1", 100, 0, List.of())), List.of())), List.of(CAPSULE), badGoals));
    }

    /**
     * A goal may not cancel itself.
     */
    @Test
    void goalCannotCancelItself() {
        assertThrows(IllegalArgumentException.class, () -> new Goal("orbit", 1, 1, List.of("orbit")));
    }

    /**
     * A step that achieves a goal the catalog doesn't define is rejected.
     */
    @Test
    void catalogRejectsAStepAchievingAnUnknownGoal() {
        assertThrows(IllegalArgumentException.class, () -> catalog(
            configuration(List.of(step("s1", 100, 0, List.of("ghost"))), List.of())));
    }

    private static RandomGenerator scripted(double[] rolls) {
        return new ScriptedRolls(rolls);
    }

    /**
     * A {@link RandomGenerator} returning a fixed sequence of {@code nextDouble} results.
     */
    private static final class ScriptedRolls implements RandomGenerator {

        private final double[] rolls;
        private int next = 0;

        private ScriptedRolls(double[] rolls) {
            this.rolls = rolls;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("not used by MissionProgress");
        }

        @Override
        public double nextDouble(double origin, double bound) {
            if (next >= rolls.length) {
                throw new IllegalStateException("no more scripted rolls available");
            }
            return rolls[next++];
        }
    }
}
