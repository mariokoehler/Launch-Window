package de.mkoehler.launchwindow.devtools.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PersonnelLifecyclePlayouts}.</p>
 *
 * @author Mario Koehler
 */
class PersonnelLifecyclePlayoutsTest {

    /**
     * Verifies that the same seed reproduces an identical report on a
     * second, independent run - the reproducibility the harness's whole
     * point (comparing tuning changes across runs) depends on.
     */
    @Test
    void sameSeedProducesIdenticalReportOnRepeatedRuns() {
        PersonnelPlayoutReport first = PersonnelLifecyclePlayouts.run(50, 4, 40, 1234L);
        PersonnelPlayoutReport second = PersonnelLifecyclePlayouts.run(50, 4, 40, 1234L);

        assertEquals(first, second);
    }

    /**
     * Verifies a run's own bookkeeping is internally consistent: every
     * employee that started or was hired is accounted for as resigned,
     * retired, lost in a mission accident, or still employed, with no
     * double-counting or loss - a run that throws
     * {@code IllegalStateException} from {@code checkInvariants} would
     * already fail this test outright, but this also catches the report's
     * own arithmetic being wrong even if the invariant checks pass.
     */
    @Test
    void everyStartingEmployeeIsAccountedFor() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(30, 5, 60, 99L);

        assertEquals(30, report.gamesPlayed());
        assertEquals(60, report.seasonsPerGame());
        assertEquals(report.employeesStarted() + report.hiredCount(),
            report.resignedCount() + report.retiredCount() + report.lostInAccidentCount()
                + report.stillEmployedAtEnd());
        assertTrue(report.employeesStarted() > 0);
        assertTrue(report.hiredCount() > 0, "expected at least one bot hire over 60 seasons");
    }

    /**
     * Verifies a long enough run actually produces both kinds of
     * departure - a sign the resignation floor and retirement ages are
     * reachable at all with the current starting-stat ranges and
     * reassignment churn, not just theoretically defined constants.
     */
    @Test
    void aLongRunProducesBothResignationsAndRetirements() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.resignedCount() > 0, "expected at least one resignation over 100 seasons");
        assertTrue(report.retiredCount() > 0, "expected at least one retirement over 100 seasons");
    }

    /**
     * Verifies a long enough run actually completes training rounds too -
     * a sign the bot's training-starting path and
     * {@code TrainingProgress}'s own countdown are both genuinely
     * exercised, not just theoretically wired together. Every run already
     * self-checks the IN_TRAINING/trainingKind consistency invariant
     * internally (see {@code checkTrainingInvariants}); this only adds
     * that at least one completion actually happens.
     */
    @Test
    void aLongRunProducesTrainingCompletions() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.trainingCompletedCount() > 0, "expected at least one training completion over 100 seasons");
        assertTrue(report.averageTrainingGain() > 0);
    }

    /**
     * Verifies a long enough run actually opens programs too - a sign the
     * bot's open-program path and {@code ProgramOpening} are both
     * genuinely exercised, not just theoretically wired together. Every
     * run already self-checks the funds/opened-id/reliability/required-by
     * invariants internally (see {@code checkProgramInvariants}); this
     * only adds that at least one open actually succeeds.
     */
    @Test
    void aLongRunOpensPrograms() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.programsOpenedCount() > 0, "expected at least one program opened over 100 seasons");
    }

    /**
     * Verifies a long enough run actually completes R&D seasons too - a
     * sign the bot's own-component-targeting reassignment path and
     * {@code ResearchProgress} are both genuinely exercised, not just
     * theoretically wired together. Every run already self-checks the
     * ASSIGNED/rdTargetComponentId pairing invariant internally (see
     * {@code checkResearchInvariants}); without this, a fixture or bot
     * change could quietly make that pairing check vacuous by never
     * actually assigning anyone to research.
     */
    @Test
    void aLongRunProducesResearch() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.researchCompletedCount() > 0, "expected at least one R&D season over 100 seasons");
    }

    /**
     * Verifies a long enough run actually launches missions and resolves
     * mission steps too - a sign the bot's {@code maybeLaunchMission} path
     * and {@code MissionProgress} are both genuinely exercised, not just
     * theoretically wired together. Every run already self-checks the
     * ASSIGNED/assignedMissionId pairing invariant internally (see
     * {@code checkMissionAssignmentConsistency}); without this, a fixture
     * or bot change could quietly make that pairing check vacuous by never
     * actually launching a mission.
     */
    @Test
    void aLongRunLaunchesMissionsAndResolvesSteps() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.missionsLaunchedCount() > 0, "expected at least one mission launched over 100 seasons");
        int stepsResolved = report.missionStepSuccessfulCount() + report.missionStepMinorGlitchSolvedCount()
            + report.missionStepMinorGlitchCount() + report.missionStepFailureCount()
            + report.missionStepCatastrophicCount();
        assertTrue(stepsResolved > 0, "expected at least one mission step resolved over 100 seasons");
    }

    /**
     * Verifies a long enough run actually produces a crew loss too, since
     * {@code mercury-mission-std}'s every step is flagged crew-risk - a
     * sign the {@code CATASTROPHIC} crew-loss path is genuinely reachable
     * under the fixture's own numbers, not just theoretically implemented.
     */
    @Test
    void aLongRunProducesAtLeastOneCrewLoss() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(200, 5, 100, 7L);

        assertTrue(report.lostInAccidentCount() > 0, "expected at least one astronaut lost over 200 games");
    }

    /**
     * Verifies a long enough run actually exercises
     * {@code MissionProgress}'s own retire-on-release check (an employee
     * who had already reached retirement age at the moment a mission
     * released them) - a sign the loophole-closing path is genuinely
     * reachable under the fixture's own starting ages/mission lengths, not
     * just theoretically implemented.
     */
    @Test
    void aLongRunRetiresSomeoneOnMissionRelease() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.retiredOnMissionReleaseCount() > 0,
            "expected at least one retire-on-release over 100 seasons");
    }

    /**
     * Verifies a long enough run actually applies post-mission-season
     * reliability change and mission-based skill growth - a sign the two
     * mechanics ({@code MissionProgress#applyPostSeasonReliabilityChange}/
     * {@code #applyPostSeasonSkillGrowth}) are genuinely exercised, not
     * just theoretically wired in. Every run already self-checks the
     * reliability-bounds and skill-range invariants internally (see
     * {@code checkProgramInvariants}/{@code checkTrainingInvariants}'s
     * shared {@code checkSkillsInRange}); this only adds that both
     * mechanics actually fire at least once.
     */
    @Test
    void aLongRunAppliesPostMissionReliabilityChangeAndSkillGrowth() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.missionReliabilityChangeCount() > 0,
            "expected at least one post-mission reliability change over 100 seasons");
        assertTrue(report.missionSkillGrowthCount() > 0,
            "expected at least one mission-based skill growth event over 100 seasons");
        assertTrue(report.averageMissionSkillGrowth() > 0);
    }

    /**
     * Verifies a long enough run actually exercises the mission-triggered
     * SET-release path too - a component's post-season reliability gain
     * pushing it to its own R&amp;D ceiling, freeing any SET specialist
     * still targeting it (see {@code ResearchProgress#releaseResearchersIfMaxed},
     * shared with {@code ResearchProgress}'s own R&amp;D-pass release). A
     * sign this path is genuinely reachable under the fixture's own
     * numbers across enough games, not just theoretically implemented.
     */
    @Test
    void aLongRunReleasesSetSpecialistsViaMissionTriggeredResearchCeiling() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(200, 5, 100, 7L);

        assertTrue(report.missionTriggeredSetReleaseCount() > 0,
            "expected at least one mission-triggered SET release over 200 games");
    }

    /**
     * Verifies a long enough run actually starts and completes
     * {@code TrainingKind.EXPERT} training - a sign the bot's own
     * eligibility check and the validated start action are both genuinely
     * exercised, not just theoretically wired in. Every run already
     * self-checks the threshold/pairing invariants internally (see
     * {@code checkExpertTrainingEligibility}/{@code checkExpertSkillThreshold});
     * this only adds that both the start and the eventual completion
     * actually happen.
     */
    @Test
    void aLongRunStartsAndCompletesExpertTraining() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.expertTrainingStartedCount() > 0, "expected at least one Expert training start");
        assertTrue(report.expertTrainingCompletedCount() > 0, "expected at least one Expert training completion");
    }

    /**
     * Verifies a long enough run actually exercises the general pull-out
     * path - a sign {@code TrainingProgress#pullOutOfTraining} is
     * genuinely reachable under the bot's own low pull-out chance, not
     * just theoretically implemented.
     */
    @Test
    void aLongRunPullsSomeoneOutOfTraining() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(100, 5, 100, 7L);

        assertTrue(report.pulledOutOfTrainingCount() > 0, "expected at least one pull-out over 100 seasons");
    }

    /**
     * Verifies a long enough run actually applies a nonzero Expert rescue
     * boost to a mission step's saving throw - a sign the roster-wide
     * eligibility pool and the stacking/exhaustion mechanic are genuinely
     * reachable, not just theoretically implemented. Every run already
     * self-checks the exact boost-percent/probability invariants
     * internally (see {@code checkExpertRescueInvariants}); this only adds
     * that a nonzero rescue actually happens.
     */
    @Test
    void aLongRunAppliesANonzeroExpertRescue() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(200, 5, 100, 7L);

        assertTrue(report.expertRescueAppliedCount() > 0, "expected at least one nonzero Expert rescue over 200 games");
    }

    /**
     * Verifies the prestige arithmetic actually runs end to end over a long
     * run: prestige is paid for successful steps, deducted by failing ones,
     * and completed flights advance the repeat tier. Every season already
     * self-checks the exact per-step payouts, the failure replacement, and
     * the zero floor (see {@code checkPrestigeInvariants}); this only adds
     * that each path is reachable at this scale.
     */
    @Test
    void aLongRunPaysAndDeductsPrestigeAndCompletesFlights() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(200, 5, 100, 7L);

        assertTrue(report.prestigePaidTotal() > 0, "expected prestige to be paid over 200 games");
        assertTrue(report.prestigeDeductedTotal() > 0, "expected at least one failing season to deduct prestige");
        assertTrue(report.completedFlightCount() > 0, "expected completed flights over 200 games");
    }

    /**
     * Verifies each non-positive argument is rejected.
     */
    @Test
    void rejectsNonPositiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(0, 5, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 0, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 5, 0, 1L));
    }
}
