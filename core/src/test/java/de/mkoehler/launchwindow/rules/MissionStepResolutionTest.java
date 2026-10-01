package de.mkoehler.launchwindow.rules;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link MissionStepResolution}'s pure two-roll formula.</p>
 *
 * @author Mario Koehler
 */
class MissionStepResolutionTest {

    /**
     * Verifies the effective-reliability helper adds a positive modifier
     * and does not floor it.
     */
    @Test
    void effectiveReliabilityAddsAPositiveModifier() {
        assertEquals(85.0, MissionStepResolution.effectiveReliability(80.0, 5.0));
    }

    /**
     * Verifies the effective-reliability helper floors a modifier that
     * would otherwise push the result negative.
     */
    @Test
    void effectiveReliabilityFloorsAtZero() {
        assertEquals(0.0, MissionStepResolution.effectiveReliability(10.0, -50.0));
    }

    /**
     * Verifies the base probability is exactly the component contribution
     * sum when no crew are involved at all.
     */
    @Test
    void baseProbabilityWithNoCrewIsTheComponentSumAlone() {
        assertEquals(55.0, MissionStepResolution.computeBaseProbability(55.0, false, 90.0));
    }

    /**
     * Verifies crew skill is not blended in when the component sum has not
     * yet cleared {@link MissionStepResolution#CREW_BLEND_HARDWARE_THRESHOLD},
     * even with crew involved.
     */
    @Test
    void baseProbabilityWithCrewBelowTheHardwareThresholdIsTheComponentSumAlone() {
        assertEquals(60.0, MissionStepResolution.computeBaseProbability(60.0, true, 90.0));
    }

    /**
     * Verifies the exact boundary: a component sum equal to the threshold
     * (not exceeding it) still does not blend in crew skill.
     */
    @Test
    void baseProbabilityAtExactlyTheHardwareThresholdDoesNotBlendCrew() {
        double threshold = MissionStepResolution.CREW_BLEND_HARDWARE_THRESHOLD;
        assertEquals(threshold, MissionStepResolution.computeBaseProbability(threshold, true, 90.0));
    }

    /**
     * Verifies the exact blend once the component sum clears the threshold
     * and crew are involved: {@code 1 - CREW_BLEND_WEIGHT} component sum
     * plus {@code CREW_BLEND_WEIGHT} crew sum.
     */
    @Test
    void baseProbabilityAboveTheHardwareThresholdBlendsInCrewSkill() {
        // 80 * 0.8 + 40 * 0.2 = 64 + 8 = 72.
        assertEquals(72.0, MissionStepResolution.computeBaseProbability(80.0, true, 40.0), 1e-9);
    }

    /**
     * Verifies the saving-throw probability is exactly the base probability
     * scaled by the controller contribution sum, divided by 100.
     */
    @Test
    void savingThrowProbabilityMatchesTheExactFormula() {
        assertEquals(30.0, MissionStepResolution.computeSavingThrowProbability(60.0, 50.0), 1e-9);
    }

    /**
     * Verifies a first roll just above its pass threshold resolves
     * successfully, consulting the saving roll not at all.
     */
    @Test
    void firstRollJustAboveItsThresholdSucceeds() {
        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, true, 40.1, 0.0);

        assertEquals(MissionStepOutcome.SUCCESSFUL, outcome);
    }

    /**
     * Verifies a first roll exactly at its pass threshold (not exceeding it)
     * fails, falling through to the saving throw.
     */
    @Test
    void firstRollExactlyAtItsThresholdFails() {
        // base = 60 -> pass threshold = 40; saving = 30 -> saving pass threshold = 70;
        // a saving roll of 71 then resolves to MINOR_GLITCH_SOLVED, proving the first roll fell through.
        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, true, 40.0, 71.0);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, outcome);
    }

    /**
     * Verifies a saving roll just above its own pass threshold fully
     * rescues the step.
     */
    @Test
    void savingRollJustAboveItsThresholdIsFullyRescued() {
        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, true, 0.0, 70.1);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, outcome);
    }

    /**
     * Verifies a saving roll in the bottom band on a crew-risk step resolves
     * to the worst tier.
     */
    @Test
    void savingRollInTheBottomBandOnACrewRiskStepIsCatastrophic() {
        // saving pass threshold = 70; failing range = 70; catastrophic boundary = 7.
        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, true, 0.0, 5.0);

        assertEquals(MissionStepOutcome.CATASTROPHIC, outcome);
    }

    /**
     * Verifies the same bottom-band roll downgrades to a plain failure when
     * the step is not flagged crew-risk.
     */
    @Test
    void savingRollInTheBottomBandOnANonCrewRiskStepDowngradesToFailure() {
        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, false, 0.0, 5.0);

        assertEquals(MissionStepOutcome.FAILURE, outcome);
    }

    /**
     * Verifies a saving roll in the middle band is always a plain failure,
     * regardless of the crew-risk flag.
     */
    @Test
    void savingRollInTheMiddleBandIsAlwaysFailure() {
        // catastrophic boundary = 7, failure boundary = 7 + 70*0.5 = 42; 20 falls in [7, 42).
        assertEquals(MissionStepOutcome.FAILURE,
            MissionStepResolution.resolve(60.0, 30.0, true, 0.0, 20.0));
        assertEquals(MissionStepOutcome.FAILURE,
            MissionStepResolution.resolve(60.0, 30.0, false, 0.0, 20.0));
    }

    /**
     * Verifies a saving roll in the top band, below its own pass threshold,
     * is a half-credited minor glitch.
     */
    @Test
    void savingRollInTheTopBandIsMinorGlitch() {
        // failure boundary = 42, saving pass threshold = 70; 50 falls in [42, 70].
        assertEquals(MissionStepOutcome.MINOR_GLITCH,
            MissionStepResolution.resolve(60.0, 30.0, true, 0.0, 50.0));
    }

    /**
     * Verifies the exact boundary between the top failing band and a full
     * rescue: a saving roll equal to its own pass threshold still fails
     * (not exceeding it), landing in the top band rather than being rescued.
     */
    @Test
    void savingRollExactlyAtItsThresholdLandsInTheTopBandNotRescued() {
        assertEquals(MissionStepOutcome.MINOR_GLITCH,
            MissionStepResolution.resolve(60.0, 30.0, true, 0.0, 70.0));
    }

    /**
     * Verifies the random-drawing wrapper draws exactly one roll, and
     * resolves successfully, when that one roll already succeeds.
     */
    @Test
    void randomWrapperDrawsNoSecondRollWhenFirstRollSucceeds() {
        ScriptedRandomGenerator scripted = new ScriptedRandomGenerator(99.0);

        MissionStepOutcome outcome = MissionStepResolution.resolve(60.0, 30.0, false, scripted);

        assertEquals(MissionStepOutcome.SUCCESSFUL, outcome);
        assertEquals(1, scripted.drawCount());
    }

    /**
     * Verifies the random-drawing wrapper draws its two rolls in
     * first-roll-then-saving-roll order: a base probability of zero forces
     * the first roll to always fail, so with the scripted rolls 10.0 then
     * 75.0, only the correct order (first=10.0 against base=0, saving=75.0
     * against a saving pass threshold of 70) resolves to
     * {@code MINOR_GLITCH_SOLVED} - a swapped or reused roll would instead
     * land saving=10.0 in the plain-failure band.
     */
    @Test
    void randomWrapperDrawsRollsInFirstThenSavingOrder() {
        ScriptedRandomGenerator scripted = new ScriptedRandomGenerator(10.0, 75.0);

        MissionStepOutcome outcome = MissionStepResolution.resolve(0.0, 30.0, true, scripted);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, outcome);
        assertEquals(2, scripted.drawCount());
    }

    /**
     * A {@link RandomGenerator} stub that returns a fixed, ordered sequence
     * of {@code nextDouble(origin, bound)} results and records how many
     * times it was actually drawn from - used to pin both the exact roll
     * values {@link MissionStepResolution#resolve(double, double, boolean, RandomGenerator)}
     * draws and the exact number of draws it makes.
     */
    private static final class ScriptedRandomGenerator implements RandomGenerator {

        private final double[] scriptedRolls;
        private int drawCount = 0;

        /**
         * @param scriptedRolls the rolls to return, in order, one per call
         *                       to {@link #nextDouble(double, double)}
         */
        private ScriptedRandomGenerator(double... scriptedRolls) {
            this.scriptedRolls = scriptedRolls;
        }

        /**
         * Not used by {@link MissionStepResolution}, which only ever calls
         * {@link #nextDouble(double, double)}.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("not used by MissionStepResolution");
        }

        /**
         * Returns the next scripted roll in sequence, ignoring
         * {@code origin}/{@code bound}.
         *
         * @param origin unused
         * @param bound  unused
         * @return the next scripted roll
         * @throws IllegalStateException if more rolls are drawn than were scripted
         */
        @Override
        public double nextDouble(double origin, double bound) {
            if (drawCount >= scriptedRolls.length) {
                throw new IllegalStateException("no more scripted rolls available");
            }
            return scriptedRolls[drawCount++];
        }

        /**
         * @return how many times {@link #nextDouble(double, double)} was
         *     actually called
         */
        private int drawCount() {
            return drawCount;
        }
    }
}
