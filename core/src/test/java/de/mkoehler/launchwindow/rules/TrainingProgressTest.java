package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link TrainingProgress}'s fixed-length training countdown.</p>
 *
 * @author Mario Koehler
 */
class TrainingProgressTest {

    /**
     * Verifies a season that doesn't complete training only advances the
     * countdown, leaving the employee's assignment state and skill
     * untouched.
     */
    @Test
    void seasonBeforeCompletionOnlyAdvancesTheCountdown() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            0, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(1, astronaut.getTrainingSeasonsCompleted());
        assertEquals(AssignmentState.IN_TRAINING, astronaut.getAssignmentState());
        assertEquals(0.0, astronaut.getSkill(AstronautSkill.EVA));
    }

    /**
     * Verifies training completes exactly when the countdown reaches the
     * round's total length, applying the exact diminishing-returns gain a
     * second, identically-seeded random source would compute directly.
     */
    @Test
    void trainingCompletesAtTheFinalSeasonWithTheExactComputedGain() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setSkill(AstronautSkill.EVA, 40.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        double expectedFactor = SkillGrowth.drawTrainingFactor(new Random(77L));
        double expectedGain = SkillGrowth.diminishingReturnsGain(40.0, astronaut.getLearningPotential(), expectedFactor);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(77L));

        assertEquals(1, events.size());
        TrainingCompleted completed = (TrainingCompleted) events.get(0);
        assertEquals(PersonnelGroup.ASTRONAUT, completed.group());
        assertEquals("a1", completed.personnelId());
        assertEquals(TrainingKind.ADVANCED, completed.trainingKind());
        assertEquals("EVA", completed.skillName());
        assertEquals(expectedGain, completed.skillGain());
        assertEquals(40.0 + expectedGain, astronaut.getSkill(AstronautSkill.EVA));
    }

    /**
     * Verifies a completed training round resets every training field and
     * returns the employee to idle.
     */
    @Test
    void completedTrainingResetsAllTrainingFieldsAndGoesIdle() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.PILOTING, TrainingKind.BASIC,
            TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS - 1, TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        TrainingProgress.advanceSeason(snapshot, new Random(5L));

        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
        assertEquals(0, astronaut.getTrainingSeasonsTotal());
        assertNull(astronaut.getTrainingTargetSkill());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies an employee not currently training ({@link TrainingKind#NONE})
     * is left completely untouched.
     */
    @Test
    void employeeNotTrainingIsUntouched() {
        Astronaut astronaut = new Astronaut("a1", "Idle Astronaut", 30, 80, 50);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies an employee left {@link AssignmentState#IN_TRAINING} with no
     * real {@code trainingKind} set ({@link TrainingKind#NONE}) - the shape
     * {@code PersonnelLifecycleTest}'s own busy-employee fixtures and the
     * bot harness's assignment shuffle both use today - is not advanced,
     * since {@code trainingKind} and {@code AssignmentState} must agree
     * before this class treats a round as genuinely active.
     */
    @Test
    void inTrainingWithNoRealTrainingKindIsUntouched() {
        Astronaut astronaut = new Astronaut("a1", "Not Really Training", 30, 80, 50);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
    }

    /**
     * Verifies an employee whose {@code trainingKind} is still a stale
     * {@link TrainingKind#BASIC} but whose assignment state has already
     * moved to {@link AssignmentState#IDLE} (e.g. a future pull-out path
     * that only updates the assignment state) is not advanced either -
     * without this guard, such an employee would keep silently counting
     * down and eventually "complete" a round they were actually pulled out
     * of.
     */
    @Test
    void idleWithStaleTrainingKindIsUntouched() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.BASIC,
            TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS - 1, TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS);
        astronaut.setAssignmentState(AssignmentState.IDLE);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS - 1, astronaut.getTrainingSeasonsCompleted());
    }

    /**
     * Verifies the documented composition order (run {@code PersonnelLifecycle}
     * before {@code TrainingProgress}) actually protects a completing
     * trainee from departing in the same season their training finishes,
     * even though their underlying stats (age past retirement, morale at
     * rock bottom) already cross both thresholds.
     */
    @Test
    void correctCompositionOrderProtectsACompletingTraineeFromDepartingTheSameSeason() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setAge(PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE);
        astronaut.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 1);
        astronaut.setMorale(1);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
        TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(lifecycleEvents.isEmpty(), "the trainee was busy going into this season, so must not depart");
        assertEquals(1, snapshot.getAstronauts().size());
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Verifies the wrong composition order (running {@code TrainingProgress}
     * first) lets the same employee wrongly depart the same season their
     * training completes - demonstrating why the order in the test above
     * actually matters, not just documenting a preference.
     */
    @Test
    void wrongCompositionOrderLetsACompletingTraineeWronglyDepart() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setAge(PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE);
        astronaut.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 1);
        astronaut.setMorale(1);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        TrainingProgress.advanceSeason(snapshot, new Random(1L));
        List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, lifecycleEvents.size(), "wrong order: the now-idle trainee wrongly departs this season");
        assertTrue(snapshot.getAstronauts().isEmpty());
    }

    /**
     * Verifies {@link TrainingKind#PERMANENT} is deliberately not resolved
     * by this class - its re-enrollment logic is out of scope
     * ({@code design.md} &sect;3.4).
     */
    @Test
    void permanentTrainingIsNotResolved() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.SCIENCE, TrainingKind.PERMANENT, 0, 0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(TrainingKind.PERMANENT, astronaut.getTrainingKind());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
    }

    /**
     * Verifies SET's own basic-training length completes a SET specialist's
     * training correctly, as a spot check that each group's roster is
     * advanced independently with its own per-group constant.
     */
    @Test
    void setSpecialistCompletesUsingTheSetBasicTrainingLength() {
        SetSpecialist specialist = new SetSpecialist("s1", "Set One", 30, 80, 50);
        specialist.setAssignmentState(AssignmentState.IN_TRAINING);
        specialist.setTrainingKind(TrainingKind.BASIC);
        specialist.setTrainingSeasonsCompleted(TrainingProgress.SET_BASIC_TRAINING_SEASONS - 1);
        specialist.setTrainingSeasonsTotal(TrainingProgress.SET_BASIC_TRAINING_SEASONS);
        specialist.setTrainingTargetSkill(SetSkill.ROCKETS);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setSetSpecialists(List.of(specialist));

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(3L));

        assertEquals(1, events.size());
        assertEquals(PersonnelGroup.SET, ((TrainingCompleted) events.get(0)).group());
        assertEquals(AssignmentState.IDLE, specialist.getAssignmentState());
    }

    /**
     * Verifies Mission Control's own basic-training length completes a
     * controller's training correctly.
     */
    @Test
    void missionControlOfficerCompletesUsingTheMissionControlBasicTrainingLength() {
        MissionControlOfficer officer = new MissionControlOfficer("m1", "MC One", 30, 80, 50);
        officer.setAssignmentState(AssignmentState.IN_TRAINING);
        officer.setTrainingKind(TrainingKind.BASIC);
        officer.setTrainingSeasonsCompleted(TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS - 1);
        officer.setTrainingSeasonsTotal(TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS);
        officer.setTrainingTargetSkill(MissionControlSkill.PROPULSION);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setMissionControlOfficers(List.of(officer));

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(9L));

        assertEquals(1, events.size());
        assertEquals(PersonnelGroup.MISSION_CONTROL, ((TrainingCompleted) events.get(0)).group());
        assertEquals(AssignmentState.IDLE, officer.getAssignmentState());
    }

    /**
     * Verifies both {@code null} arguments are rejected.
     */
    @Test
    void rejectsNullArguments() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        assertThrows(NullPointerException.class, () -> TrainingProgress.advanceSeason(null, new Random(1L)));
        assertThrows(NullPointerException.class, () -> TrainingProgress.advanceSeason(snapshot, null));
    }

    private static Astronaut trainingAstronaut(AstronautSkill target, TrainingKind kind,
            int seasonsCompleted, int seasonsTotal) {
        Astronaut astronaut = new Astronaut("a1", "Training Astronaut", 30, 80, 50);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        astronaut.setTrainingKind(kind);
        astronaut.setTrainingSeasonsCompleted(seasonsCompleted);
        astronaut.setTrainingSeasonsTotal(seasonsTotal);
        astronaut.setTrainingTargetSkill(target);
        return astronaut;
    }

    private static PlayerSnapshot snapshotWithAstronaut(Astronaut astronaut) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setAstronauts(List.of(astronaut));
        return snapshot;
    }
}
