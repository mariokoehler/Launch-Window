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
 * per-player mission state, are both {@link MissionProgress}'s own
 * responsibility; see {@code design.md} &sect;2.5(f) for that resolver's
 * exact shape, including the average-not-sum rule it uses to fold several
 * assigned crew/controllers into one skill's contribution.</p>
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
 * tuning constants verbatim, whether code-confirmed (BASPM's own 70
 * threshold/25% weight, traced to {@code ComputeProbabilityOfSuccessForMissionStep}
 * itself) or sourced from its {@code Constants.xml} (the saving-throw band
 * fractions), so every one here was re-derived to a concrete placeholder
 * number while keeping the same formula shape traced in {@code baspm.md}
 * &sect;8.2.
 * <b>(Unconfirmed default, {@code design.md} &sect;2.5(d), 2026-10-01):</b>
 * placeholder-but-concrete starting values, expected to be tuned later via
 * the bot-driven playout harness once a caller exists, not final game
 * balance.</p>
 *
 * <p><b>Expert rescue, added 2026-10-02</b> (see {@code design.md}
 * &sect;2.5(i)) - {@link #applyExpertBoost}/{@link #expertRescueBoostPercentForUse}/
 * {@link #EXPERT_RESCUE_PROBABILITY_CAP} are the pure formulas
 * {@link MissionProgress#resolveStep} uses to let an Expert-flagged
 * employee (see {@code design.md} &sect;2.2(c)) boost a saving-throw
 * probability already in progress; deciding <em>who</em> is eligible for
 * a given step, and tracking per-employee exhaustion across a turn, are
 * both {@link MissionProgress}'s own responsibility, the same split this
 * class already keeps for everything else.</p>
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
     * @param crewContributionSum      the sum, across every crew skill
     *                                 involved in the step, of
     *                                 {@code (involvement percentage / 100) *}
     *                                 that skill's contribution - the mean
     *                                 skill value across every assigned
     *                                 crew member, not a sum of them (see
     *                                 {@link MissionProgress}); ignored
     *                                 when {@code crewInvolved} is
     *                                 {@code false}
     * @return the base success probability, as a percentage; not clamped to
     *     100 - when every component's involvement percentages sum to 100
     *     (see {@code design.md} &sect;2.5(e)), this only exceeds 100 if a
     *     positive {@link #effectiveReliability} modifier pushes some
     *     component's own effective reliability above 100
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
     * @param controllerContributionSum the sum, across every flight-
     *                                   controller skill involved in the
     *                                   step, of
     *                                   {@code (involvement percentage / 100) *}
     *                                   that skill's contribution - the
     *                                   mean skill value across every
     *                                   assigned controller, not a sum of
     *                                   them (see {@link MissionProgress})
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
        if (firstRollSucceeded(baseProbability, firstRoll)) {
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
        if (firstRollSucceeded(baseProbability, firstRoll)) {
            return MissionStepOutcome.SUCCESSFUL;
        }
        double savingRoll = drawRoll(random);
        return resolve(baseProbability, savingThrowProbability, crewRisk, firstRoll, savingRoll);
    }

    /**
     * Whether a step's first roll alone already succeeds, with no saving
     * throw ever needed - shared by every {@link #resolve} overload above,
     * and by {@link MissionProgress#resolveStep} directly: that caller
     * must know this <em>before</em> computing an Expert-boosted saving-
     * throw probability (see {@code design.md} &sect;2.5(i)), since an
     * Expert is only ever "used" (and exhausted) when a saving throw is
     * actually needed - never merely for being eligible.
     *
     * @param baseProbability the step's base success probability, as
     *                         returned by {@link #computeBaseProbability}
     * @param firstRoll        the step's first roll, uniformly in
     *                         {@code [0, 100)}
     * @return {@code true} if the first roll alone succeeds
     */
    static boolean firstRollSucceeded(double baseProbability, double firstRoll) {
        return firstRoll > 100.0 - baseProbability;
    }

    /**
     * Draws one roll, uniformly in {@code [0, 100)} - package-private so
     * {@link MissionProgress#resolveStep} can draw its own first roll
     * explicitly (see {@link #firstRollSucceeded}'s own Javadoc for why),
     * rather than only ever through the auto-drawing {@link #resolve}
     * overload above.
     *
     * @param random the random source to draw from
     * @return the drawn roll
     */
    static double drawRoll(RandomGenerator random) {
        return random.nextDouble(0, 100);
    }

    /**
     * The highest an Expert-boosted saving-throw probability may ever
     * reach, applied once by {@link #capExpertRescue} after every eligible
     * Expert's own {@link #applyExpertBoost} call for a step - see
     * {@code design.md} &sect;2.5(i). Launch Window's own unconfirmed
     * default, not traced from BASPM - {@code baspm.md} &sect;9.5's own
     * Tiger Teams tiers max out at 50% of the remaining gap in a single
     * use, structurally far short of ever needing a cap like this one.
     */
    public static final double EXPERT_RESCUE_PROBABILITY_CAP = 95.0;

    /**
     * The share of the remaining gap to 100% an Expert's rescue attempt
     * closes on its first use within one turn - see
     * {@link #expertRescueBoostPercentForUse}. User-confirmed, 2026-10-02,
     * see {@code design.md} &sect;2.5(i); re-derives {@code baspm.md}
     * &sect;9.5's own single-use gap-closing shape with Launch Window's
     * own number, not BASPM's own tiered 25/40/50% (there, those three
     * numbers were tied to a hireable "how many Tiger Teams" choice that
     * doesn't exist here).
     */
    public static final double EXPERT_RESCUE_FIRST_USE_BOOST_PERCENT = 50.0;

    /** The same employee's second use within one turn - see {@link #EXPERT_RESCUE_FIRST_USE_BOOST_PERCENT}. */
    public static final double EXPERT_RESCUE_SECOND_USE_BOOST_PERCENT = 40.0;

    /** The same employee's third use within one turn - see {@link #EXPERT_RESCUE_FIRST_USE_BOOST_PERCENT}. */
    public static final double EXPERT_RESCUE_THIRD_USE_BOOST_PERCENT = 10.0;

    /**
     * How much a Mission Control officer's own rescue contribution scales
     * by when they are {@code ASSIGNED} to a <em>different</em> active
     * mission than the one glitching - still on Earth, just not fully
     * dedicated (user decision, 2026-10-02, see {@code design.md}
     * &sect;2.5(i)). An Astronaut in the same situation is excluded
     * entirely instead (see {@link MissionProgress#applyExpertRescue});
     * this scaling only ever applies to a Mission Control officer.
     */
    public static final double EXPERT_RESCUE_OFF_MISSION_CONTROLLER_EFFICIENCY = 0.75;

    /**
     * Looks up how much of the remaining gap to 100% an Expert's rescue
     * attempt closes, by how many times that same employee has already
     * been drawn on this turn (see {@code design.md} &sect;2.5(i)'s own
     * "scope: per employee, per turn" rule) - {@code useNumber} 1 is their
     * first use this turn, 2 their second, and so on. The fourth use and
     * every one after it contributes {@code 0} - not hard-capped at three
     * uses, just negligible from then on (user decision, 2026-10-02).
     *
     * @param useNumber which use this is for the employee this turn,
     *                  1-indexed; must be positive
     * @return the boost percent, 0-100
     * @throws IllegalArgumentException if {@code useNumber} is not positive
     */
    public static double expertRescueBoostPercentForUse(int useNumber) {
        if (useNumber <= 0) {
            throw new IllegalArgumentException("useNumber must be positive: " + useNumber);
        }
        return switch (useNumber) {
            case 1 -> EXPERT_RESCUE_FIRST_USE_BOOST_PERCENT;
            case 2 -> EXPERT_RESCUE_SECOND_USE_BOOST_PERCENT;
            case 3 -> EXPERT_RESCUE_THIRD_USE_BOOST_PERCENT;
            default -> 0.0;
        };
    }

    /**
     * Applies one Expert's rescue attempt to a saving-throw probability
     * already in progress: closes {@code boostPercent} of the remaining
     * gap to 100%, re-deriving {@code baspm.md} &sect;9.5's own
     * {@code boost = percentageBoost × (100 − currentProb)} shape. Meant
     * to be called once per eligible Expert, in sequence, each call
     * working off the previous call's own result - stacking several
     * Experts this way inherently keeps the result strictly below 100%
     * (every {@code boostPercent} used is itself strictly below 100%, so
     * the remaining gap shrinks geometrically but never reaches exactly
     * zero), satisfying "never guaranteed" by construction, with no extra
     * guard needed here. {@link #capExpertRescue} is a separate, explicit
     * ceiling a caller applies once, after every Expert's own boost has
     * been applied - not part of this formula itself, since the cap must
     * never lower an unboosted probability that was already above it on
     * its own (see {@code design.md} &sect;2.5(i)).
     *
     * @param currentProbability the saving-throw probability before this
     *                           Expert's own contribution (the unboosted
     *                           value, or another Expert's already-
     *                           applied boost)
     * @param boostPercent       this Expert's own boost percent for this
     *                           use, as returned by
     *                           {@link #expertRescueBoostPercentForUse}
     *                           (already scaled down for an off-mission
     *                           Mission Control officer, if applicable -
     *                           this formula takes the final percent, it
     *                           does not apply that scaling itself)
     * @return the boosted probability; never below {@code currentProbability}
     */
    public static double applyExpertBoost(double currentProbability, double boostPercent) {
        return currentProbability + (100.0 - currentProbability) * boostPercent / 100.0;
    }

    /**
     * Applies {@link #EXPERT_RESCUE_PROBABILITY_CAP} to a saving-throw
     * probability after every eligible Expert's own {@link #applyExpertBoost}
     * has already been applied in sequence: {@code min(boosted, max(unboosted,
     * cap))} - never lets Expert stacking push the result above the cap,
     * but also never lowers {@code unboosted} itself when it was already
     * above the cap on its own merits, with zero Experts involved at all
     * (see {@code design.md} &sect;2.5(i)).
     *
     * @param boosted   the saving-throw probability after every eligible
     *                  Expert's own contribution (equal to
     *                  {@code unboosted} if none applied)
     * @param unboosted the saving-throw probability before any Expert's
     *                  own contribution
     * @return the final, capped saving-throw probability
     */
    public static double capExpertRescue(double boosted, double unboosted) {
        return Math.min(boosted, Math.max(unboosted, EXPERT_RESCUE_PROBABILITY_CAP));
    }
}
