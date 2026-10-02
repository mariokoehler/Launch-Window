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
     * Verifies each non-positive argument is rejected.
     */
    @Test
    void rejectsNonPositiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(0, 5, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 0, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 5, 0, 1L));
    }
}
