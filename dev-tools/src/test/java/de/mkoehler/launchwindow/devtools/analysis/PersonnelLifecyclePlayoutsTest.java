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
     * retired, or still employed, with no double-counting or loss - a run
     * that throws {@code IllegalStateException} from {@code checkInvariants}
     * would already fail this test outright, but this also catches the
     * report's own arithmetic being wrong even if the invariant checks
     * pass.
     */
    @Test
    void everyStartingEmployeeIsAccountedFor() {
        PersonnelPlayoutReport report = PersonnelLifecyclePlayouts.run(30, 5, 60, 99L);

        assertEquals(30, report.gamesPlayed());
        assertEquals(60, report.seasonsPerGame());
        assertEquals(report.employeesStarted() + report.hiredCount(),
            report.resignedCount() + report.retiredCount() + report.stillEmployedAtEnd());
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
     * Verifies each non-positive argument is rejected.
     */
    @Test
    void rejectsNonPositiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(0, 5, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 0, 10, 1L));
        assertThrows(IllegalArgumentException.class, () -> PersonnelLifecyclePlayouts.run(5, 5, 0, 1L));
    }
}
