package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.Trait;

import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Planning help for {@link de.mkoehler.launchwindow.model.TrainingKind#PERMANENT} training, for the
 * training dialog (the user's request, 2026-10-10): how many seasons it takes to reach a target
 * level. It follows {@link TrainingProgress}'s rounds step for step: each round lasts
 * {@link TrainingProgress#ADVANCED_TRAINING_SEASONS} seasons and adds
 * {@link SkillGrowth#diminishingReturnsGain}, and training ends at the target or once a round gains
 * less than {@link TrainingProgress#PERMANENT_TRAINING_GAIN_EPSILON}. The average uses the middle of
 * the training factor's range; traits count only once revealed. Headless and read-only.
 *
 * @author Mario Koehler
 */
public final class TrainingPlanning {

    /** How many random runs a range takes. */
    static final int RANGE_RUNS = 300;

    /** The most rounds a projection follows: far past any target a player can set. */
    static final int MAX_ROUNDS = 200;

    private static final long RANGE_SEED = 0x7EA1L;

    private TrainingPlanning() {
    }

    /**
     * Where permanent training is headed on average.
     *
     * @param seasons    the seasons until it ends
     * @param rounds     the rounds that takes
     * @param reaches    whether it ends at the target; otherwise it stalls below it
     * @param finalSkill the skill it ends at
     */
    public record Estimate(int seasons, int rounds, boolean reaches, double finalSkill) {
    }

    /**
     * The spread of the seasons random runs take to reach the target.
     *
     * @param low  the tenth percentile
     * @param high the ninetieth percentile, {@code Integer.MAX_VALUE} when more than one run in ten
     *             stalls below the target
     */
    public record Range(int low, int high) {
    }

    /**
     * Estimates permanent training from a skill to a target level, on average.
     *
     * @param person  the employee, for learning potential and revealed traits
     * @param current the skill now
     * @param target  the target level
     * @return the estimate
     * @throws NullPointerException if {@code person} is {@code null}
     */
    public static Estimate permanent(Personnel person, double current, double target) {
        Objects.requireNonNull(person, "person must not be null");
        double average = (SkillGrowth.TRAINING_FACTOR_MIN + SkillGrowth.TRAINING_FACTOR_MAX) / 2.0;
        return run(person, current, target, null, average);
    }

    /**
     * Returns the spread of the seasons permanent training takes to reach a target: the tenth and
     * ninetieth percentile of {@link #RANGE_RUNS} runs with each round's factor random, as in the
     * season pass. The seed is fixed, so the dialog shows the same range every time.
     *
     * @param person  the employee
     * @param current the skill now
     * @param target  the target level
     * @return the range
     * @throws NullPointerException if {@code person} is {@code null}
     */
    public static Range range(Personnel person, double current, double target) {
        Objects.requireNonNull(person, "person must not be null");
        int[] seasons = new int[RANGE_RUNS];
        SplittableRandom random = new SplittableRandom(RANGE_SEED);
        for (int i = 0; i < RANGE_RUNS; i++) {
            Estimate estimate = run(person, current, target, random, 0);
            seasons[i] = estimate.reaches() ? estimate.seasons() : Integer.MAX_VALUE;
        }
        Arrays.sort(seasons);
        return new Range(seasons[RANGE_RUNS / 10], seasons[RANGE_RUNS - 1 - RANGE_RUNS / 10]);
    }

    private static Estimate run(Personnel person, double current, double target, RandomGenerator random,
                                double fixedFactor) {
        double traits = knownTrainingGain(person);
        double skill = current;
        int rounds = 0;
        while (skill < target && rounds < MAX_ROUNDS) {
            double factor = random == null ? fixedFactor : SkillGrowth.drawTrainingFactor(random);
            double gain = SkillGrowth.diminishingReturnsGain(skill, person.getLearningPotential(), factor) * traits;
            skill += gain;
            rounds++;
            if (skill < target && gain < TrainingProgress.PERMANENT_TRAINING_GAIN_EPSILON) {
                break;
            }
        }
        return new Estimate(rounds * TrainingProgress.ADVANCED_TRAINING_SEASONS, rounds, skill >= target, skill);
    }

    /** {@link TraitEffects#trainingGain} with only the traits the player has seen. */
    private static double knownTrainingGain(Personnel person) {
        double factor = 1;
        if (Personality.known(person, Trait.AMBITIOUS)) {
            factor *= TraitEffects.AMBITIOUS_TRAINING;
        }
        if (Personality.known(person, Trait.QUICK_STUDY)) {
            factor *= TraitEffects.QUICK_STUDY_TRAINING;
        }
        return factor;
    }
}
