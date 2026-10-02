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
import java.util.Set;

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
    void correctCompositionOrderJudgesMoraleAgainstBeingBusyAllSeasonAndStays() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setAge(30);
        astronaut.setMorale(37);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
        TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(lifecycleEvents.isEmpty(),
            "correct order: busy morale gain (37+3=40) keeps them above the resignation floor");
        assertEquals(1, snapshot.getAstronauts().size());
        assertEquals(40, snapshot.getAstronauts().get(0).getMorale());
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Verifies the wrong composition order (running {@code TrainingProgress}
     * first) judges that season's morale against the wrong assignment
     * state and wrongly resigns the same employee - demonstrating why the
     * order in the test above still matters, even though (after the
     * 2026-09-30 departure-protection reversal, see {@link PersonnelLifecycle}'s
     * own Javadoc) it no longer changes *eligibility* for the check, only
     * the morale the check is judged against.
     */
    @Test
    void wrongCompositionOrderJudgesMoraleAgainstAlreadyBeingIdleAndResigns() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setAge(30);
        astronaut.setMorale(37);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        TrainingProgress.advanceSeason(snapshot, new Random(1L));
        List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, lifecycleEvents.size(),
            "wrong order: now-idle morale loss (37-2=35) hits the resignation floor exactly");
        assertTrue(snapshot.getAstronauts().isEmpty());
    }

    /**
     * Verifies {@link TrainingKind#PERMANENT} re-enrolls into another round
     * after completing one that didn't yet reach the target level, rather
     * than finishing like {@link TrainingKind#BASIC}/{@link TrainingKind#ADVANCED}
     * do.
     */
    @Test
    void permanentTrainingReenrollsAfterARoundThatDoesNotReachTheTarget() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.SCIENCE, TrainingKind.PERMANENT,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setSkill(AstronautSkill.SCIENCE, 10.0);
        astronaut.setTrainingTargetLevel(99.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertEquals(1, events.size(), "a round completing still reports an event, even mid-standing-order");
        assertEquals(TrainingKind.PERMANENT, astronaut.getTrainingKind(), "re-enrolled, not finished");
        assertEquals(0, astronaut.getTrainingSeasonsCompleted(), "countdown restarted for the next round");
        assertEquals(TrainingProgress.ADVANCED_TRAINING_SEASONS, astronaut.getTrainingSeasonsTotal());
        assertEquals(AstronautSkill.SCIENCE, astronaut.getTrainingTargetSkill(), "target skill preserved across re-enrollment");
        assertEquals(99.0, astronaut.getTrainingTargetLevel(), "target level preserved across re-enrollment");
        assertEquals(AssignmentState.IN_TRAINING, astronaut.getAssignmentState());
    }

    /**
     * Verifies {@link TrainingKind#PERMANENT} keeps re-enrolling across
     * several rounds and finally stops, resetting like a completed
     * {@code ADVANCED} round would, once the target level is actually
     * reached.
     */
    @Test
    void permanentTrainingStopsOnceTheTargetLevelIsReached() {
        Astronaut astronaut = new Astronaut("a1", "Training Astronaut", 30, 80, 100);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        astronaut.setTrainingKind(TrainingKind.PERMANENT);
        astronaut.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setTrainingTargetSkill(AstronautSkill.SCIENCE);
        astronaut.setTrainingTargetLevel(20.0);
        astronaut.setSkill(AstronautSkill.SCIENCE, 10.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);
        Random random = new Random(1L);

        int seasonsRun = 0;
        int safetyCap = 200;
        while (astronaut.getTrainingKind() == TrainingKind.PERMANENT && seasonsRun < safetyCap) {
            TrainingProgress.advanceSeason(snapshot, random);
            seasonsRun++;
        }

        assertTrue(seasonsRun < safetyCap, "should have terminated well before the loop's own safety cap");
        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
        assertTrue(astronaut.getSkill(AstronautSkill.SCIENCE) >= 20.0);
        assertNull(astronaut.getTrainingTargetSkill());
    }

    /**
     * Verifies the termination guard fires after exactly one round for an
     * employee with zero learning potential - {@link SkillGrowth#diminishingReturnsGain}
     * always returns 0 in that case, so without the guard this would
     * re-enroll forever with no progress at all.
     */
    @Test
    void permanentTrainingStopsAfterOneRoundWithZeroLearningPotential() {
        Astronaut astronaut = new Astronaut("a1", "No Learning Potential", 30, 80, 0);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        astronaut.setTrainingKind(TrainingKind.PERMANENT);
        astronaut.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setTrainingSeasonsCompleted(TrainingProgress.ADVANCED_TRAINING_SEASONS - 1);
        astronaut.setTrainingTargetSkill(AstronautSkill.SCIENCE);
        astronaut.setTrainingTargetLevel(99.0);
        astronaut.setSkill(AstronautSkill.SCIENCE, 10.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind(), "stopped despite never reaching the target");
        assertEquals(10.0, astronaut.getSkill(AstronautSkill.SCIENCE), "zero learning potential means zero gain");
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies the termination guard also fires when the target level sits
     * just above the current skill and near the skill cap, where each
     * round's gain becomes negligible - {@link SkillGrowth#diminishingReturnsGain}
     * only ever asymptotically approaches the cap, so without this guard a
     * target this close would also re-enroll forever.
     */
    @Test
    void permanentTrainingStopsViaEpsilonNearTheSkillCap() {
        Astronaut astronaut = new Astronaut("a1", "Nearly Capped", 30, 80, 100);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        astronaut.setTrainingKind(TrainingKind.PERMANENT);
        astronaut.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setTrainingSeasonsCompleted(TrainingProgress.ADVANCED_TRAINING_SEASONS - 1);
        astronaut.setTrainingTargetSkill(AstronautSkill.SCIENCE);
        astronaut.setTrainingTargetLevel(SkillGrowth.SKILL_CAP);
        astronaut.setSkill(AstronautSkill.SCIENCE, SkillGrowth.SKILL_CAP - 0.01);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind(),
            "the maximum possible gain this close to the cap is still under the epsilon");
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies an employee left {@code IN_TRAINING} with a stale
     * {@link TrainingKind#PERMANENT} but no assignment-state agreement is
     * untouched - the same {@code isActivelyTraining} guard already proven
     * for {@code BASIC} applies identically here.
     */
    @Test
    void idleWithStalePermanentTrainingKindIsUntouched() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.SCIENCE, TrainingKind.PERMANENT,
            TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setAssignmentState(AssignmentState.IDLE);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(1L));

        assertTrue(events.isEmpty());
        assertEquals(TrainingProgress.ADVANCED_TRAINING_SEASONS - 1, astronaut.getTrainingSeasonsCompleted());
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

    /**
     * Verifies a failed {@link TrainingKind#EXPERT} season only advances
     * the (purely informational) counter - no event, no skill change, and
     * the employee stays {@code IN_TRAINING} for another season.
     */
    @Test
    void expertTrainingSeasonThatFailsOnlyAdvancesTheCounter() {
        Astronaut astronaut = expertTrainingAstronaut(AstronautSkill.PILOTING, 90.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        // successChance at skill 90 (threshold 80, cap 100) is 25 + 10*(10/20) = 30 -> pass
        // threshold is roll > 70. A roll of 50 fails.
        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(2L) {
            @Override
            public double nextDouble(double origin, double bound) {
                return 50.0;
            }
        });

        assertTrue(events.isEmpty());
        assertEquals(1, astronaut.getTrainingSeasonsCompleted());
        assertEquals(AssignmentState.IN_TRAINING, astronaut.getAssignmentState());
        assertEquals(TrainingKind.EXPERT, astronaut.getTrainingKind());
        assertEquals(90.0, astronaut.getSkill(AstronautSkill.PILOTING), "EXPERT training never grows the skill");
        assertTrue(astronaut.getExpertSkills().isEmpty());
    }

    /**
     * Verifies a successful {@link TrainingKind#EXPERT} season grants the
     * trait, resets every training field, and reports the exact event -
     * at the precise pass/fail boundary, so a wrong comparison direction
     * would fail this test.
     */
    @Test
    void expertTrainingSeasonThatSucceedsGrantsTheTraitAndResetsFields() {
        Astronaut astronaut = expertTrainingAstronaut(AstronautSkill.PILOTING, 90.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        // successChance 30 -> threshold roll 70. A roll of 70.1 passes.
        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(2L) {
            @Override
            public double nextDouble(double origin, double bound) {
                return 70.1;
            }
        });

        assertEquals(1, events.size());
        ExpertTrainingCompleted completed = (ExpertTrainingCompleted) events.get(0);
        assertEquals(PersonnelGroup.ASTRONAUT, completed.group());
        assertEquals("a1", completed.personnelId());
        assertEquals("PILOTING", completed.skillName());
        assertEquals(Set.of(AstronautSkill.PILOTING), astronaut.getExpertSkills());
        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
        assertNull(astronaut.getTrainingTargetSkill());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
        assertEquals(90.0, astronaut.getSkill(AstronautSkill.PILOTING), "EXPERT training never grows the skill");
    }

    /**
     * Verifies the Mission Control equivalent of
     * {@link #expertTrainingSeasonThatSucceedsGrantsTheTraitAndResetsFields}.
     */
    @Test
    void missionControlExpertTrainingSeasonThatSucceedsGrantsTheTrait() {
        MissionControlOfficer officer = new MissionControlOfficer("m1", "Expert Candidate", 30, 80, 50);
        officer.setSkill(MissionControlSkill.PROPULSION, 90.0);
        officer.setAssignmentState(AssignmentState.IN_TRAINING);
        officer.setTrainingKind(TrainingKind.EXPERT);
        officer.setTrainingTargetSkill(MissionControlSkill.PROPULSION);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setMissionControlOfficers(List.of(officer));

        List<ResolutionEvent> events = TrainingProgress.advanceSeason(snapshot, new Random(2L) {
            @Override
            public double nextDouble(double origin, double bound) {
                return 70.1;
            }
        });

        assertEquals(1, events.size());
        ExpertTrainingCompleted completed = (ExpertTrainingCompleted) events.get(0);
        assertEquals(PersonnelGroup.MISSION_CONTROL, completed.group());
        assertEquals(Set.of(MissionControlSkill.PROPULSION), officer.getExpertSkills());
        assertEquals(AssignmentState.IDLE, officer.getAssignmentState());
    }

    /**
     * Verifies {@link TrainingProgress#startAstronautExpertTraining} starts
     * training for an eligible, idle astronaut at exactly the threshold.
     */
    @Test
    void startAstronautExpertTrainingStartsAtExactlyTheThreshold() {
        Astronaut astronaut = new Astronaut("a1", "Candidate", 30, 80, 50);
        astronaut.setSkill(AstronautSkill.PILOTING, SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        StartExpertTrainingOutcome outcome =
            TrainingProgress.startAstronautExpertTraining(snapshot, "a1", AstronautSkill.PILOTING);

        assertEquals(StartExpertTrainingOutcome.STARTED, outcome);
        assertEquals(TrainingKind.EXPERT, astronaut.getTrainingKind());
        assertEquals(AstronautSkill.PILOTING, astronaut.getTrainingTargetSkill());
        assertEquals(AssignmentState.IN_TRAINING, astronaut.getAssignmentState());
        assertEquals(0, astronaut.getTrainingSeasonsCompleted());
        assertEquals(0, astronaut.getTrainingSeasonsTotal());
    }

    /**
     * Verifies a skill just below the threshold is rejected, mutating
     * nothing.
     */
    @Test
    void startAstronautExpertTrainingRejectsSkillJustBelowTheThreshold() {
        Astronaut astronaut = new Astronaut("a1", "Candidate", 30, 80, 50);
        astronaut.setSkill(AstronautSkill.PILOTING, SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD - 0.1);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        StartExpertTrainingOutcome outcome =
            TrainingProgress.startAstronautExpertTraining(snapshot, "a1", AstronautSkill.PILOTING);

        assertEquals(StartExpertTrainingOutcome.SKILL_BELOW_THRESHOLD, outcome);
        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies an astronaut not currently idle is rejected.
     */
    @Test
    void startAstronautExpertTrainingRejectsWhenNotIdle() {
        Astronaut astronaut = new Astronaut("a1", "Candidate", 30, 80, 50);
        astronaut.setSkill(AstronautSkill.PILOTING, 90.0);
        astronaut.setAssignmentState(AssignmentState.ASSIGNED);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        assertEquals(StartExpertTrainingOutcome.NOT_IDLE,
            TrainingProgress.startAstronautExpertTraining(snapshot, "a1", AstronautSkill.PILOTING));
    }

    /**
     * Verifies an astronaut already Expert in the targeted skill is
     * rejected.
     */
    @Test
    void startAstronautExpertTrainingRejectsWhenAlreadyExpertInTheSkill() {
        Astronaut astronaut = new Astronaut("a1", "Candidate", 30, 80, 50);
        astronaut.setSkill(AstronautSkill.PILOTING, 90.0);
        astronaut.getExpertSkills().add(AstronautSkill.PILOTING);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        assertEquals(StartExpertTrainingOutcome.ALREADY_EXPERT_IN_SKILL,
            TrainingProgress.startAstronautExpertTraining(snapshot, "a1", AstronautSkill.PILOTING));
    }

    /**
     * Verifies an unknown astronaut id is rejected.
     */
    @Test
    void startAstronautExpertTrainingRejectsUnknownId() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);

        assertEquals(StartExpertTrainingOutcome.UNKNOWN_PERSONNEL,
            TrainingProgress.startAstronautExpertTraining(snapshot, "unknown", AstronautSkill.PILOTING));
    }

    /**
     * Verifies {@link TrainingProgress#startMissionControlExpertTraining}
     * mirrors the astronaut path, as a spot check rather than repeating
     * every rejection case.
     */
    @Test
    void startMissionControlExpertTrainingStarts() {
        MissionControlOfficer officer = new MissionControlOfficer("m1", "Candidate", 30, 80, 50);
        officer.setSkill(MissionControlSkill.PROPULSION, 90.0);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setMissionControlOfficers(List.of(officer));

        StartExpertTrainingOutcome outcome =
            TrainingProgress.startMissionControlExpertTraining(snapshot, "m1", MissionControlSkill.PROPULSION);

        assertEquals(StartExpertTrainingOutcome.STARTED, outcome);
        assertEquals(TrainingKind.EXPERT, officer.getTrainingKind());
        assertEquals(AssignmentState.IN_TRAINING, officer.getAssignmentState());
    }

    /**
     * Verifies pulling an astronaut out of a {@code BASIC}/{@code ADVANCED}/
     * {@code PERMANENT} round applies exactly {@code SkillGrowth.partialCredit}
     * against that round's own completed/total seasons - a real,
     * scaled-down gain, neither the full gain nor zero.
     */
    @Test
    void pullOutOfTrainingCreditsAPartialGainForAFixedLengthRound() {
        Astronaut astronaut = trainingAstronaut(AstronautSkill.EVA, TrainingKind.ADVANCED,
            1, TrainingProgress.ADVANCED_TRAINING_SEASONS);
        astronaut.setSkill(AstronautSkill.EVA, 40.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        double expectedFactor = SkillGrowth.drawTrainingFactor(new Random(11L));
        double expectedFullGain =
            SkillGrowth.diminishingReturnsGain(40.0, astronaut.getLearningPotential(), expectedFactor);
        double expectedCreditedGain = SkillGrowth.partialCredit(expectedFullGain, 1,
            TrainingProgress.ADVANCED_TRAINING_SEASONS);

        PullOutOfTrainingOutcome outcome =
            TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.ASTRONAUT, "a1", new Random(11L));

        assertEquals(PullOutOfTrainingOutcome.PULLED_OUT, outcome);
        assertTrue(expectedCreditedGain > 0 && expectedCreditedGain < expectedFullGain,
            "sanity check on the fixture: partial credit must be strictly between zero and the full gain");
        assertEquals(40.0 + expectedCreditedGain, astronaut.getSkill(AstronautSkill.EVA));
        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
        assertNull(astronaut.getTrainingTargetSkill());
    }

    /**
     * Verifies pulling an astronaut out of {@link TrainingKind#EXPERT}
     * training credits nothing at all - there is no continuous skill
     * value that training ever grows, unlike the fixed-length kinds.
     */
    @Test
    void pullOutOfTrainingCreditsNothingForExpertTraining() {
        Astronaut astronaut = expertTrainingAstronaut(AstronautSkill.PILOTING, 90.0);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        PullOutOfTrainingOutcome outcome =
            TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.ASTRONAUT, "a1", new Random(1L));

        assertEquals(PullOutOfTrainingOutcome.PULLED_OUT, outcome);
        assertEquals(90.0, astronaut.getSkill(AstronautSkill.PILOTING));
        assertTrue(astronaut.getExpertSkills().isEmpty(), "pulling out early never grants the trait");
        assertEquals(TrainingKind.NONE, astronaut.getTrainingKind());
        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }

    /**
     * Verifies a SET specialist's own fixed-length round is pulled out of
     * the same way as an astronaut's/officer's.
     */
    @Test
    void pullOutOfTrainingCreditsAPartialGainForASetSpecialist() {
        SetSpecialist specialist = new SetSpecialist("s1", "Set One", 30, 80, 50);
        specialist.setAssignmentState(AssignmentState.IN_TRAINING);
        specialist.setTrainingKind(TrainingKind.BASIC);
        specialist.setTrainingSeasonsTotal(TrainingProgress.SET_BASIC_TRAINING_SEASONS + 1);
        specialist.setTrainingSeasonsCompleted(1);
        specialist.setTrainingTargetSkill(SetSkill.ROCKETS);
        specialist.setSkill(SetSkill.ROCKETS, 20.0);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setSetSpecialists(List.of(specialist));

        double expectedFactor = SkillGrowth.drawTrainingFactor(new Random(1L));
        double expectedFullGain =
            SkillGrowth.diminishingReturnsGain(20.0, specialist.getLearningPotential(), expectedFactor);
        double expectedCreditedGain = SkillGrowth.partialCredit(expectedFullGain, 1,
            TrainingProgress.SET_BASIC_TRAINING_SEASONS + 1);

        PullOutOfTrainingOutcome outcome =
            TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.SET, "s1", new Random(1L));

        assertEquals(PullOutOfTrainingOutcome.PULLED_OUT, outcome);
        assertTrue(expectedCreditedGain > 0, "sanity check on the fixture: partial credit must be positive here");
        assertEquals(20.0 + expectedCreditedGain, specialist.getSkill(SetSkill.ROCKETS));
        assertEquals(TrainingKind.NONE, specialist.getTrainingKind());
        assertEquals(AssignmentState.IDLE, specialist.getAssignmentState());
    }

    /**
     * Verifies an employee not currently training is rejected.
     */
    @Test
    void pullOutOfTrainingRejectsWhenNotInTraining() {
        Astronaut astronaut = new Astronaut("a1", "Idle", 30, 80, 50);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        assertEquals(PullOutOfTrainingOutcome.NOT_IN_TRAINING,
            TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.ASTRONAUT, "a1", new Random(1L)));
    }

    /**
     * Verifies an unknown id is rejected.
     */
    @Test
    void pullOutOfTrainingRejectsUnknownId() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);

        assertEquals(PullOutOfTrainingOutcome.UNKNOWN_PERSONNEL,
            TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.ASTRONAUT, "unknown", new Random(1L)));
    }

    /**
     * Builds an astronaut already {@link TrainingKind#EXPERT} training for
     * the given skill, with that skill already at the given value.
     *
     * @param target the skill being trained toward the Expert trait in
     * @param skillValue the astronaut's current value in {@code target}
     * @return the built astronaut
     */
    private static Astronaut expertTrainingAstronaut(AstronautSkill target, double skillValue) {
        Astronaut astronaut = new Astronaut("a1", "Expert Candidate", 30, 80, 50);
        astronaut.setSkill(target, skillValue);
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
        astronaut.setTrainingKind(TrainingKind.EXPERT);
        astronaut.setTrainingTargetSkill(target);
        return astronaut;
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
