package de.mkoehler.launchwindow.rules;

import java.util.random.RandomGenerator;

/**
 * <p>The pure two-roll mission-step resolution formula, re-deriving the
 * code-confirmed formula shape traced in {@code baspm.md} &sect;8.2 (see
 * {@code design.md} &sect;2.5(c)/(d) for the decision and status). Every
 * method here is a plain function of {@code double}s/{@code boolean}s, with
 * no dependency on {@link de.mkoehler.launchwindow.content.MissionComponent}
 * or {@link de.mkoehler.launchwindow.model.Personnel} - summing a step's
 * involved components/controllers/crew into the contribution totals this
 * class takes as input, and walking a season's step batch against
 * per-player mission state, are both a future caller's responsibility; see
 * {@code design.md} &sect;2.5(d) for that caller's own status.</p>
 *
 * <p><b>Contribution sums are already on the 0-100 scale</b> - each term a
 * caller adds into {@code componentContributionSum}/{@code crewContributionSum}/
 * {@code controllerContributionSum} is {@code (involvement percentage / 100) *}
 * the relevant reliability-or-skill value (itself 0-100), <em>not</em> the raw
 * percentage times that value - otherwise the 100-based thresholds and
 * divisions throughout this class would be off by a factor of 100.</p>
 *
 * <p><b>Random rolls drawn separately from the formula they feed</b> - both
 * roll values are plain parameters on the fully deterministic
 * {@link #resolve(double, double, boolean, double, double)} overload, so a
 * test can check an exact outcome without stubbing a random source. The
 * {@link #resolve(double, double, boolean, RandomGenerator)} overload is the
 * thin wrapper that actually draws from a {@link RandomGenerator}, and draws
 * its second roll only when the first one actually failed - mirroring the
 * bot harness's own reproducibility needs ({@code design.md} &sect;3.10).</p>
 *
 * <p><b>All the numeric constants below are Launch Window's own values, not
 * BASPM's</b> - {@code design.md} &sect;1.2 forbids reusing BASPM's own
 * tuning constants (its own {@code Constants.xml} values) verbatim, so every
 * one here was re-derived to a concrete placeholder number while keeping the
 * same formula shape traced in {@code baspm.md} &sect;8.2.
 * <b>(Unconfirmed default, {@code design.md} &sect;2.5(d), 2026-10-01):</b>
 * placeholder-but-concrete starting values, expected to be tuned later via
 * the bot-driven playout harness once a caller exists, not final game
 * balance.</p>
 *
 * @author Mario Koehler
 */
public final class MissionStepResolution {

    /**
     * The component-reliability contribution sum a step's hardware must
     * exceed before crew skill is blended into the base probability at all.
     */
    public static final double CREW_BLEND_HARDWARE_THRESHOLD = 65.0;

    /**
     * The weight crew skill carries in the base-probability blend, once
     * {@link #CREW_BLEND_HARDWARE_THRESHOLD} is cleared. The
     * component-reliability sum keeps the remaining {@code 1 -} this weight,
     * so crew skill can only ever nudge an already-solid base probability,
     * never carry it on its own.
     */
    public static final double CREW_BLEND_WEIGHT = 0.20;

    /**
     * The fraction of a failed saving throw's own range that falls into the
     * {@link MissionStepOutcome#CATASTROPHIC}/{@link MissionStepOutcome#FAILURE}
     * band (the bottom slice, nearest a roll of zero).
     */
    public static final double CATASTROPHIC_BAND_FRACTION = 0.10;

    /**
     * The fraction of a failed saving throw's own range that falls into the
     * plain {@link MissionStepOutcome#FAILURE} band (the middle slice,
     * directly above the catastrophic/failure band).
     */
    public static final double FAILURE_BAND_FRACTION = 0.50;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MissionStepResolution() {
    }

    /**
     * Computes a mission component's effective reliability for a step: its
     * current reliability plus a step-specific modifier (e.g. a difficulty
     * adjustment), floored at zero. A caller weights this by the
     * component's involvement percentage before summing it into
     * {@link #computeBaseProbability}'s {@code componentContributionSum}.
     *
     * @param currentReliability the component's current reliability
     * @param modifier            a step-specific adjustment; may be negative
     * @return {@code max(0, currentReliability + modifier)}
     */
    public static double effectiveReliability(double currentReliability, double modifier) {
        return Math.max(0.0, currentReliability + modifier);
    }

    /**
     * Computes a mission step's base success probability: the
     * component-reliability contribution sum alone, unless crew are
     * involved and that sum already clears {@link #CREW_BLEND_HARDWARE_THRESHOLD},
     * in which case the crew-skill contribution sum is blended in at
     * {@link #CREW_BLEND_WEIGHT}. Flight-controller skill plays no part in
     * this computation - see {@link #computeSavingThrowProbability}.
     *
     * @param componentContributionSum the sum, across every mission
     *                                 component involved in the step, of
     *                                 {@code (involvement percentage / 100) *}
     *                                 that component's {@link #effectiveReliability}
     * @param crewInvolved             whether any flight crew are involved
     *                                 in this step at all
     * @param crewContributionSum      the sum, across every crew member
     *                                 involved in the step, of
     *                                 {@code (involvement percentage / 100) *}
     *                                 their relevant skill; ignored when
     *                                 {@code crewInvolved} is {@code false}
     * @return the base success probability, as a percentage; not clamped to
     *     100, since an overwhelming contribution sum legitimately means a
     *     guaranteed-success step
     */
    public static double computeBaseProbability(double componentContributionSum, boolean crewInvolved,
            double crewContributionSum) {
        if (crewInvolved && componentContributionSum > CREW_BLEND_HARDWARE_THRESHOLD) {
            return componentContributionSum * (1 - CREW_BLEND_WEIGHT) + crewContributionSum * CREW_BLEND_WEIGHT;
        }
        return componentContributionSum;
    }

    /**
     * Computes the saving-throw probability used only after a step's first
     * roll (against {@code baseProbability}) has failed: the base
     * probability scaled by the flight-controller contribution sum.
     *
     * @param baseProbability           the step's base success probability,
     *                                   as returned by
     *                                   {@link #computeBaseProbability}
     * @param controllerContributionSum the sum, across every flight
     *                                   controller involved in the step, of
     *                                   {@code (involvement percentage / 100) *}
     *                                   their relevant skill
     * @return the saving-throw probability, as a percentage; not clamped to
     *     100, for the same reason {@link #computeBaseProbability} is not
     */
    public static double computeSavingThrowProbability(double baseProbability, double controllerContributionSum) {
        return baseProbability * controllerContributionSum / 100.0;
    }

    /**
     * Resolves a mission step's outcome from two already-drawn rolls. The
     * first roll is checked against {@code baseProbability}; only if it
     * fails is {@code savingRoll} consulted at all, against
     * {@code savingThrowProbability}'s own failing range split into bands.
     *
     * @param baseProbability        the step's base success probability, as
     *                                returned by {@link #computeBaseProbability}
     * @param savingThrowProbability the step's saving-throw probability, as
     *                                returned by {@link #computeSavingThrowProbability}
     * @param crewRisk                whether this step is flagged as one
     *                                 where a worst-tier failure can cost
     *                                 crew lives; gates whether
     *                                 {@link MissionStepOutcome#CATASTROPHIC}
     *                                 is reachable at all
     * @param firstRoll                the step's first roll, uniformly in
     *                                 {@code [0, 100)}
     * @param savingRoll               the step's saving-throw roll,
     *                                 uniformly in {@code [0, 100)}; only
     *                                 consulted if {@code firstRoll} fails
     * @return the resolved outcome
     */
    public static MissionStepOutcome resolve(double baseProbability, double savingThrowProbability,
            boolean crewRisk, double firstRoll, double savingRoll) {
        if (firstRoll > 100.0 - baseProbability) {
            return MissionStepOutcome.SUCCESSFUL;
        }

        double passThreshold = 100.0 - savingThrowProbability;
        if (savingRoll > passThreshold) {
            return MissionStepOutcome.MINOR_GLITCH_SOLVED;
        }

        double failingRangeSize = passThreshold;
        double catastrophicBoundary = failingRangeSize * CATASTROPHIC_BAND_FRACTION;
        double failureBoundary = catastrophicBoundary + failingRangeSize * FAILURE_BAND_FRACTION;
        if (savingRoll < catastrophicBoundary) {
            return crewRisk ? MissionStepOutcome.CATASTROPHIC : MissionStepOutcome.FAILURE;
        }
        if (savingRoll < failureBoundary) {
            return MissionStepOutcome.FAILURE;
        }
        return MissionStepOutcome.MINOR_GLITCH;
    }

    /**
     * Resolves a mission step's outcome by drawing its own rolls from
     * {@code random}, drawing the saving-throw roll only if the first roll
     * actually fails.
     *
     * @param baseProbability        the step's base success probability, as
     *                                returned by {@link #computeBaseProbability}
     * @param savingThrowProbability the step's saving-throw probability, as
     *                                returned by {@link #computeSavingThrowProbability}
     * @param crewRisk                whether this step is flagged as one
     *                                 where a worst-tier failure can cost
     *                                 crew lives
     * @param random                   the random source to draw from
     * @return the resolved outcome
     */
    public static MissionStepOutcome resolve(double baseProbability, double savingThrowProbability,
            boolean crewRisk, RandomGenerator random) {
        double firstRoll = drawRoll(random);
        if (firstRoll > 100.0 - baseProbability) {
            return MissionStepOutcome.SUCCESSFUL;
        }
        double savingRoll = drawRoll(random);
        return resolve(baseProbability, savingThrowProbability, crewRisk, firstRoll, savingRoll);
    }

    /**
     * Draws one roll, uniformly in {@code [0, 100)}.
     *
     * @param random the random source to draw from
     * @return the drawn roll
     */
    private static double drawRoll(RandomGenerator random) {
        return random.nextDouble(0, 100);
    }
}
