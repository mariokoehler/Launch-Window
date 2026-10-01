package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionComponent;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * <p>Pure skill-growth formulas - the second M2 rules-engine slice
 * (see {@code design.md} &sect;5 M2, and &sect;2.2(a)/(c)/`baspm.md` &sect;9.3
 * for the mechanic this re-derives). Every method here is a plain function
 * of {@code double}s, with no dependency on {@link de.mkoehler.launchwindow.model.Personnel}
 * or any of its subclasses - deciding *which* skill(s) a training round, an
 * R&amp;D assignment, or flying a mission actually touches (a single skill,
 * all five, or a Flight Director's five-way-spread average per
 * {@code baspm.md} &sect;9.4) is a caller's job, not this class's.
 * {@link TrainingProgress} (a single target skill, fixed-length training)
 * and {@link ResearchProgress} (R&amp;D's own employee-skill and
 * component-reliability growth) are such callers; mission-based growth's
 * own caller is still a later M2 slice.</p>
 *
 * <p><b>Random factor drawn separately from the formula it feeds</b> - every
 * growth formula here takes its random factor as a plain parameter rather
 * than drawing it internally, so a test can check an exact value without
 * stubbing a random source, and the bot harness ({@code design.md}
 * &sect;3.10) can reproduce a run from a seed. {@link #drawTrainingFactor}/
 * {@link #drawRdGainFactor} are the thin wrappers that actually draw one,
 * taking a {@link RandomGenerator} rather than a concrete {@link java.util.Random}
 * so a caller can inject any seeded source - {@code Random} itself already
 * implements this interface, so the harness needs no change to use these.</p>
 *
 * <p><b>All the numeric constants below are Launch Window's own values, not
 * BASPM's</b> - {@code design.md} &sect;1.2 forbids reusing BASPM's own
 * tuning constants verbatim (its skill cap is 98, its training factor range
 * is 0.3-0.6, its flat R&amp;D gain range is 0.2-0.4), so every one here was
 * re-derived to a different concrete number while keeping the same formula
 * shape. <b>(Unconfirmed default, {@code design.md} &sect;2.2(c),
 * 2026-09-30):</b> placeholder-but-concrete starting values, expected to be
 * tuned later via the bot-driven playout harness, not final game balance.</p>
 *
 * @author Mario Koehler
 */
public final class SkillGrowth {

    /**
     * The skill scale's upper bound. {@link #diminishingReturnsGain} only
     * ever asymptotically approaches it through its own distance-to-cap
     * term; {@link #flatRdGain} has no such term by definition (it is
     * flat), so it clamps against this bound explicitly instead.
     */
    public static final double SKILL_CAP = 100.0;

    /**
     * The learning-potential stat's own upper bound, used to turn it into a
     * 0-1 fraction. A separate scale from {@link #SKILL_CAP} by design -
     * BASPM's own formula reuses its skill cap for both roles, but nothing
     * requires the two concepts to share one number.
     */
    public static final double LEARNING_POTENTIAL_MAX = 100.0;

    /** The lowest random factor a full (not pulled-out-early) training round can draw. */
    public static final double TRAINING_FACTOR_MIN = 0.25;

    /** The highest random factor a full training round can draw. */
    public static final double TRAINING_FACTOR_MAX = 0.55;

    /** The lowest random factor a season of flat SET R&D growth can draw. */
    public static final double RD_GAIN_FACTOR_MIN = 0.15;

    /** The highest random factor a season of flat SET R&D growth can draw. */
    public static final double RD_GAIN_FACTOR_MAX = 0.35;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private SkillGrowth() {
    }

    /**
     * The diminishing-returns growth formula shared by training and
     * mission-based astronaut/controller skill growth
     * ({@code baspm.md} &sect;9.3/&sect;9.4 - BASPM reuses one formula for
     * both, and so does this re-derivation): the closer a skill already is
     * to {@link #SKILL_CAP}, the smaller a given round's gain, scaled by
     * what fraction of {@link #LEARNING_POTENTIAL_MAX} the employee's own
     * learning potential is.
     *
     * @param currentSkill      the skill's current value before this gain
     * @param learningPotential the employee's learning-potential stat
     * @param factor            the random factor for this round, normally
     *                          drawn via {@link #drawTrainingFactor}
     * @return the gain to add to {@code currentSkill}; never negative, and
     *     {@code currentSkill + gain} never exceeds {@link #SKILL_CAP} for
     *     any {@code factor} and {@code learningPotential} in
     *     {@code [0, 1]}-normalized range
     */
    public static double diminishingReturnsGain(double currentSkill, double learningPotential, double factor) {
        double distanceToCap = Math.max(0.0, SKILL_CAP - currentSkill);
        double learningFraction = clamp(learningPotential / LEARNING_POTENTIAL_MAX, 0.0, 1.0);
        return distanceToCap * factor * learningFraction;
    }

    /**
     * The linear partial-credit rule for pulling an employee out of training
     * early ({@code baspm.md} &sect;9.3): exactly {@code fullGain} scaled by
     * how much of the training was actually completed.
     *
     * @param fullGain         the gain a completed training round would have
     *                         produced
     * @param seasonsCompleted how many seasons of that round were actually
     *                         completed before the pull-out
     * @param totalSeasons     that training type's normal total length in
     *                         seasons
     * @return {@code fullGain * seasonsCompleted / totalSeasons}
     * @throws IllegalArgumentException if {@code totalSeasons} is not
     *                                   positive, or {@code seasonsCompleted}
     *                                   is negative or greater than
     *                                   {@code totalSeasons}
     */
    public static double partialCredit(double fullGain, int seasonsCompleted, int totalSeasons) {
        if (totalSeasons <= 0) {
            throw new IllegalArgumentException("totalSeasons must be positive: " + totalSeasons);
        }
        if (seasonsCompleted < 0 || seasonsCompleted > totalSeasons) {
            throw new IllegalArgumentException(
                "seasonsCompleted must be between 0 and totalSeasons: " + seasonsCompleted);
        }
        return fullGain * seasonsCompleted / (double) totalSeasons;
    }

    /**
     * SET's flat per-season R&amp;D growth ({@code baspm.md} &sect;9.3): no
     * distance-to-cap *scaling* at all, unlike
     * {@link #diminishingReturnsGain} - the gain is simply the random
     * factor itself, every season, regardless of how close to the cap the
     * skill already is. <b>Still clamped against {@link #SKILL_CAP}</b>,
     * since without any distance-to-cap term of its own, a standing R&amp;D
     * order left running indefinitely would otherwise walk straight past
     * it - unlike {@link #diminishingReturnsGain}, whose own term makes
     * that impossible by construction.
     *
     * @param currentSkill the skill's current value before this gain
     * @param factor       the random factor for this season, normally drawn
     *                     via {@link #drawRdGainFactor}
     * @return the gain to add to {@code currentSkill}; never negative, and
     *     {@code currentSkill + gain} never exceeds {@link #SKILL_CAP}
     */
    public static double flatRdGain(double currentSkill, double factor) {
        return Math.min(factor, Math.max(0.0, SKILL_CAP - currentSkill));
    }

    /**
     * One R&amp;D slot's per-season reliability-gain contribution -
     * <b>code-confirmed, re-deriving {@code baspm.md} &sect;10.8's exact
     * formula</b> (user decision, 2026-10-01, see {@code design.md}
     * &sect;3.4) with Launch Window's own numbers, not BASPM's: a
     * <em>separate</em> function from {@link #diminishingReturnsGain}, not
     * that function reused with an external clamp, because its
     * distance-to-cap term is computed against the given
     * {@code maxRdReliability} directly rather than the fixed
     * {@link #SKILL_CAP}: a component instance's own R&amp;D ceiling is not
     * always 100, so reusing the fixed-100-cap formula would compute too
     * large a gain near that ceiling and then clip sharply there, not taper
     * off. {@link ResearchProgress} sums one call of this per occupied slot
     * to get a component's total gain for the season - this method itself
     * doesn't sum across slots, apply the stagnation guard, or clamp to
     * {@code maxRdReliability}, since those are whole-component concerns,
     * not a single slot's.
     *
     * @param rawAmount           this slot's raw improvement amount for the
     *                            season, normally drawn via
     *                            {@link #drawRdSlotRawAmount}
     * @param currentReliability  the component's current reliability before
     *                            this season's R&amp;D, shared by every slot
     *                            (BASPM computes the headroom factor once
     *                            per component, before any slot's
     *                            contribution, not sequentially per slot)
     * @param maxRdReliability    the component's own R&amp;D reliability
     *                            ceiling (always &le; its absolute
     *                            {@code maxReliability} - see
     *                            {@link MissionComponent#maxRdReliability()})
     * @param assigneeSkill       this slot's assignee's current skill in the
     *                            component's associated group
     * @return this slot's contribution to add to {@code currentReliability};
     *     never negative
     */
    public static double rdReliabilityGain(double rawAmount, double currentReliability, double maxRdReliability,
            double assigneeSkill) {
        double headroomFactor = Math.max(0.0, maxRdReliability - currentReliability) / 100.0;
        double skillFraction = clamp(assigneeSkill / SKILL_CAP, 0.0, 1.0);
        return rawAmount * headroomFactor * skillFraction;
    }

    /**
     * Draws the raw improvement amount one R&amp;D slot contributes this
     * season, uniformly from that slot's own band within
     * {@code rdSlotBandMaxAmounts} - re-deriving {@code baspm.md}
     * &sect;10.8's per-component {@code RDLevelMaxAmount L1}-{@code L4}
     * mechanic: slot 0 (the best) draws from the band between the third and
     * fourth entries, each successive slot one band lower, down to slot
     * {@value MissionComponent#RD_SLOT_COUNT}{@code  - 1} (the worst), which
     * draws from {@code [0, rdSlotBandMaxAmounts.get(0)]}.
     *
     * @param rdSlotBandMaxAmounts the component's four ascending band
     *                             ceilings, see
     *                             {@link MissionComponent#rdSlotBandMaxAmounts()}
     * @param slot                 the slot index, {@code 0} to
     *                              {@value MissionComponent#RD_SLOT_COUNT}{@code  - 1}
     * @param random                the random source to draw from
     * @return the drawn raw amount
     * @throws IllegalArgumentException if {@code slot} is outside
     *                                   {@code [0, }{@value MissionComponent#RD_SLOT_COUNT}{@code )}
     *                                   or {@code rdSlotBandMaxAmounts}
     *                                   doesn't have exactly
     *                                   {@value MissionComponent#RD_SLOT_COUNT}
     *                                   entries
     */
    public static double drawRdSlotRawAmount(List<Double> rdSlotBandMaxAmounts, int slot, RandomGenerator random) {
        double lower = rdSlotBandLower(rdSlotBandMaxAmounts, slot);
        double upper = rdSlotBandUpper(rdSlotBandMaxAmounts, slot);
        return lower >= upper ? lower : random.nextDouble(lower, upper);
    }

    /**
     * Returns the inclusive lower bound of one R&amp;D slot's raw-amount
     * band - see {@link #drawRdSlotRawAmount}.
     *
     * @param rdSlotBandMaxAmounts the component's four ascending band
     *                             ceilings
     * @param slot                 the slot index
     * @return the band's lower bound; {@code 0} for the worst slot
     */
    public static double rdSlotBandLower(List<Double> rdSlotBandMaxAmounts, int slot) {
        validateSlot(rdSlotBandMaxAmounts, slot);
        return slot == MissionComponent.RD_SLOT_COUNT - 1 ? 0.0 : rdSlotBandMaxAmounts.get(2 - slot);
    }

    /**
     * Returns the inclusive upper bound of one R&amp;D slot's raw-amount
     * band - see {@link #drawRdSlotRawAmount}.
     *
     * @param rdSlotBandMaxAmounts the component's four ascending band
     *                             ceilings
     * @param slot                 the slot index
     * @return the band's upper bound
     */
    public static double rdSlotBandUpper(List<Double> rdSlotBandMaxAmounts, int slot) {
        validateSlot(rdSlotBandMaxAmounts, slot);
        return rdSlotBandMaxAmounts.get(MissionComponent.RD_SLOT_COUNT - 1 - slot);
    }

    private static void validateSlot(List<Double> rdSlotBandMaxAmounts, int slot) {
        if (rdSlotBandMaxAmounts.size() != MissionComponent.RD_SLOT_COUNT) {
            throw new IllegalArgumentException(
                "rdSlotBandMaxAmounts must have exactly " + MissionComponent.RD_SLOT_COUNT + " entries: "
                    + rdSlotBandMaxAmounts);
        }
        if (slot < 0 || slot >= MissionComponent.RD_SLOT_COUNT) {
            throw new IllegalArgumentException(
                "slot must be within [0, " + MissionComponent.RD_SLOT_COUNT + "): " + slot);
        }
    }

    /**
     * Draws a random factor for one training round, uniformly from
     * {@code [}{@link #TRAINING_FACTOR_MIN}{@code ,} {@link #TRAINING_FACTOR_MAX}{@code )}.
     *
     * @param random the random source to draw from
     * @return the drawn factor
     */
    public static double drawTrainingFactor(RandomGenerator random) {
        return random.nextDouble(TRAINING_FACTOR_MIN, TRAINING_FACTOR_MAX);
    }

    /**
     * Draws a random factor for one season of flat R&amp;D growth,
     * uniformly from {@code [}{@link #RD_GAIN_FACTOR_MIN}{@code ,}
     * {@link #RD_GAIN_FACTOR_MAX}{@code )}.
     *
     * @param random the random source to draw from
     * @return the drawn factor
     */
    public static double drawRdGainFactor(RandomGenerator random) {
        return random.nextDouble(RD_GAIN_FACTOR_MIN, RD_GAIN_FACTOR_MAX);
    }

    /**
     * Clamps a value to a closed range.
     *
     * @param value the value to clamp
     * @param min   the lowest allowed value
     * @param max   the highest allowed value
     * @return {@code value}, clamped to {@code [min, max]}
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
