package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TrainingPlanning}: the permanent-training estimate sits where the real season pass
 * lands, and a target out of reach is told as a stall.
 *
 * @author Mario Koehler
 */
class TrainingPlanningTest {

    private static SetSpecialist engineer(double rockets, int learningPotential) {
        SetSpecialist specialist = new SetSpecialist("e", "e", 30, 70, learningPotential);
        specialist.setSkill(SetSkill.ROCKETS, rockets);
        return specialist;
    }

    /**
     * Verifies the average estimate and its range match many real permanent trainings run through
     * {@link TrainingProgress}: the estimate within a round of the real median, and the range inside
     * the real spread.
     */
    @Test
    void estimateMatchesTheSeasonPass() {
        SetSpecialist planned = engineer(45, 60);
        TrainingPlanning.Estimate estimate = TrainingPlanning.permanent(planned, 45, 85);
        TrainingPlanning.Range range = TrainingPlanning.range(planned, 45, 85);
        assertTrue(estimate.reaches());
        assertTrue(range.low() <= estimate.seasons() && estimate.seasons() <= range.high(), range.toString());

        int runs = 1_000;
        int[] seasons = new int[runs];
        SplittableRandom random = new SplittableRandom(3);
        for (int i = 0; i < runs; i++) {
            SetSpecialist trainee = engineer(45, 60);
            trainee.setTrainingTargetSkill(SetSkill.ROCKETS);
            trainee.setTrainingKind(TrainingKind.PERMANENT);
            trainee.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
            trainee.setTrainingTargetLevel(85);
            trainee.setAssignmentState(AssignmentState.IN_TRAINING);
            PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
            snapshot.setSetSpecialists(new ArrayList<>(List.of(trainee)));
            while (trainee.getAssignmentState() == AssignmentState.IN_TRAINING) {
                seasons[i]++;
                TrainingProgress.advanceSeason(snapshot, random);
            }
        }
        Arrays.sort(seasons);
        int median = seasons[runs / 2];
        assertEquals(median, estimate.seasons(), TrainingProgress.ADVANCED_TRAINING_SEASONS,
            "within a round of the real median");
        assertTrue(range.low() >= seasons[runs / 20] && range.high() <= seasons[runs - 1 - runs / 20],
            range + " inside the real 5th to 95th percentile");
    }

    /**
     * Verifies a target at the cap stalls below it: gains shrink with the distance left until a round
     * adds too little, and the training ends there.
     */
    @Test
    void targetAtTheCapStalls() {
        TrainingPlanning.Estimate estimate = TrainingPlanning.permanent(engineer(50, 40), 50, 100);
        assertFalse(estimate.reaches());
        assertTrue(estimate.finalSkill() > 99 && estimate.finalSkill() < 100, String.valueOf(estimate.finalSkill()));
        assertEquals(Integer.MAX_VALUE, TrainingPlanning.range(engineer(50, 40), 50, 100).high());
    }
}
