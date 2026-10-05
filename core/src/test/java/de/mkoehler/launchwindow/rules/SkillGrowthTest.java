package de.mkoehler.launchwindow.rules;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link SkillGrowth}'s pure growth formulas.</p>
 *
 * @author Mario Koehler
 */
class SkillGrowthTest {

    /**
     * Verifies a full team's expected raw R&amp;D amount is the middle of each slot's band, summed:
     * bands 19-28, 14-19, 9-14 and 0-9 for these maxima.
     */
    @Test
    void expectedFullTeamRawAmountSumsTheMiddleOfEachBand() {
        assertEquals(23.5 + 16.5 + 11.5 + 4.5, SkillGrowth.expectedFullTeamRawAmount(List.of(9.0, 14.0, 19.0, 28.0)),
            1e-9);
    }

    /**
     * Verifies that applying the diminishing-returns gain never pushes a
     * skill above the cap, even at the highest possible factor and
     * learning potential.
     */
    @Test
    void diminishingReturnsGainNeverPushesSkillPastTheCap() {
        double currentSkill = 95.0;
        double gain = SkillGrowth.diminishingReturnsGain(currentSkill, SkillGrowth.LEARNING_POTENTIAL_MAX,
            SkillGrowth.TRAINING_FACTOR_MAX);

        assertTrue(currentSkill + gain <= SkillGrowth.SKILL_CAP);
    }

    /**
     * Verifies a skill already at the cap gains nothing further, regardless
     * of factor or learning potential.
     */
    @Test
    void diminishingReturnsGainAtTheCapIsZero() {
        double gain = SkillGrowth.diminishingReturnsGain(SkillGrowth.SKILL_CAP, SkillGrowth.LEARNING_POTENTIAL_MAX,
            SkillGrowth.TRAINING_FACTOR_MAX);

        assertEquals(0.0, gain);
    }

    /**
     * Verifies the gain strictly shrinks as the starting skill rises,
     * holding factor and learning potential fixed - the defining property
     * of a diminishing-returns curve.
     */
    @Test
    void diminishingReturnsGainShrinksAsCurrentSkillRises() {
        double factor = 0.4;
        double learningPotential = 80.0;

        double gainAtLow = SkillGrowth.diminishingReturnsGain(10.0, learningPotential, factor);
        double gainAtMid = SkillGrowth.diminishingReturnsGain(50.0, learningPotential, factor);
        double gainAtHigh = SkillGrowth.diminishingReturnsGain(90.0, learningPotential, factor);

        assertTrue(gainAtLow > gainAtMid, "gain at low skill should exceed gain at mid skill");
        assertTrue(gainAtMid > gainAtHigh, "gain at mid skill should exceed gain at high skill");
    }

    /**
     * Verifies zero learning potential gives zero training gain, no matter
     * how favorable the factor or how far from the cap the skill is.
     */
    @Test
    void zeroLearningPotentialGivesZeroGain() {
        double gain = SkillGrowth.diminishingReturnsGain(10.0, 0.0, SkillGrowth.TRAINING_FACTOR_MAX);

        assertEquals(0.0, gain);
    }

    /**
     * Verifies partial credit is exactly the full gain scaled by the
     * completed/total ratio.
     */
    @Test
    void partialCreditIsExactlyFullGainTimesCompletedOverTotal() {
        assertEquals(10.0 / 3.0, SkillGrowth.partialCredit(10.0, 1, 3));
    }

    /**
     * Verifies pulling out after every season completes gives the full gain
     * back unscaled.
     */
    @Test
    void partialCreditAtFullCompletionEqualsFullGain() {
        assertEquals(7.5, SkillGrowth.partialCredit(7.5, 3, 3));
    }

    /**
     * Verifies pulling out before any season completes gives no credit.
     */
    @Test
    void partialCreditAtZeroCompletionIsZero() {
        assertEquals(0.0, SkillGrowth.partialCredit(7.5, 0, 3));
    }

    /**
     * Verifies invalid {@code totalSeasons}/{@code seasonsCompleted}
     * combinations are rejected rather than silently producing a nonsense
     * ratio.
     */
    @Test
    void partialCreditRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.partialCredit(10.0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.partialCredit(10.0, -1, 3));
        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.partialCredit(10.0, 4, 3));
    }

    /**
     * Verifies the flat R&D gain is exactly the given factor when there is
     * plenty of room below the cap, unscaled by current skill or learning
     * potential the way {@link SkillGrowth#diminishingReturnsGain} is.
     */
    @Test
    void flatRdGainReturnsTheFactorUnchangedWellBelowTheCap() {
        assertEquals(0.27, SkillGrowth.flatRdGain(50.0, 0.27));
    }

    /**
     * Verifies the flat R&D gain still never pushes a skill above the cap,
     * even though it has no distance-to-cap scaling term of its own - a
     * standing R&D order left running indefinitely must not walk a skill
     * straight past 100.
     */
    @Test
    void flatRdGainNeverPushesSkillPastTheCap() {
        double currentSkill = 99.9;
        double gain = SkillGrowth.flatRdGain(currentSkill, SkillGrowth.RD_GAIN_FACTOR_MAX);

        assertTrue(currentSkill + gain <= SkillGrowth.SKILL_CAP);
    }

    /**
     * Verifies a skill already at the cap gains nothing further from flat
     * R&D growth either.
     */
    @Test
    void flatRdGainAtTheCapIsZero() {
        assertEquals(0.0, SkillGrowth.flatRdGain(SkillGrowth.SKILL_CAP, SkillGrowth.RD_GAIN_FACTOR_MAX));
    }

    /**
     * Verifies {@code drawTrainingFactor} always stays within its
     * documented range across many draws.
     */
    @Test
    void drawTrainingFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawTrainingFactor(random);
            assertTrue(factor >= SkillGrowth.TRAINING_FACTOR_MIN && factor < SkillGrowth.TRAINING_FACTOR_MAX);
        }
    }

    /**
     * Verifies {@code drawRdGainFactor} always stays within its documented
     * range across many draws.
     */
    @Test
    void drawRdGainFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawRdGainFactor(random);
            assertTrue(factor >= SkillGrowth.RD_GAIN_FACTOR_MIN && factor < SkillGrowth.RD_GAIN_FACTOR_MAX);
        }
    }

    /**
     * Verifies the exact per-slot reliability-gain formula - re-deriving
     * {@code baspm.md} &sect;10.8's code-confirmed formula: the slot's raw
     * amount, times the component's own headroom to its R&amp;D ceiling as
     * a fraction of 100, times the assignee's skill as a fraction of
     * {@link SkillGrowth#SKILL_CAP}.
     */
    @Test
    void rdReliabilityGainMatchesTheExactFormula() {
        // 10 * ((90 - 50) / 100) * (80 / 100) = 10 * 0.4 * 0.8 = 3.2.
        assertEquals(3.2, SkillGrowth.rdReliabilityGain(10.0, 50.0, 90.0, 80.0), 1e-9);
    }

    /**
     * Verifies a zero-skill assignee contributes no reliability gain at
     * all, regardless of raw amount or headroom.
     */
    @Test
    void rdReliabilityGainWithZeroSkillIsZero() {
        assertEquals(0.0, SkillGrowth.rdReliabilityGain(12.0, 10.0, 90.0, 0.0));
    }

    /**
     * Verifies a component already at its own R&amp;D ceiling contributes
     * no further gain, regardless of raw amount or skill - the exact bug
     * this formula's headroom term exists to avoid, that a formula scaled
     * against the fixed skill cap instead would not catch near a lower,
     * component-specific ceiling.
     */
    @Test
    void rdReliabilityGainAtItsOwnCeilingIsZero() {
        assertEquals(0.0, SkillGrowth.rdReliabilityGain(12.0, 90.0, 90.0, SkillGrowth.SKILL_CAP));
    }

    /**
     * Verifies the four R&amp;D slot bands - best (slot 0) to worst (slot
     * 3) - match the ascending ceiling list exactly, re-deriving
     * {@code baspm.md} &sect;10.8's own slot-to-band mapping with Launch
     * Window's own numbers, not a BASPM component's real values.
     */
    @Test
    void rdSlotBandsMatchTheDocumentedMapping() {
        List<Double> bands = List.of(3.0, 6.0, 9.0, 12.0);

        assertEquals(9.0, SkillGrowth.rdSlotBandLower(bands, 0));
        assertEquals(12.0, SkillGrowth.rdSlotBandUpper(bands, 0));
        assertEquals(6.0, SkillGrowth.rdSlotBandLower(bands, 1));
        assertEquals(9.0, SkillGrowth.rdSlotBandUpper(bands, 1));
        assertEquals(3.0, SkillGrowth.rdSlotBandLower(bands, 2));
        assertEquals(6.0, SkillGrowth.rdSlotBandUpper(bands, 2));
        assertEquals(0.0, SkillGrowth.rdSlotBandLower(bands, 3));
        assertEquals(3.0, SkillGrowth.rdSlotBandUpper(bands, 3));
    }

    /**
     * Verifies an out-of-range slot index is rejected.
     */
    @Test
    void rdSlotBandsRejectOutOfRangeSlot() {
        List<Double> bands = List.of(3.0, 6.0, 9.0, 12.0);

        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.rdSlotBandLower(bands, -1));
        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.rdSlotBandUpper(bands, 4));
    }

    /**
     * Verifies a {@code rdSlotBandMaxAmounts} list with other than exactly
     * four entries is rejected.
     */
    @Test
    void rdSlotBandsRejectWrongListSize() {
        assertThrows(IllegalArgumentException.class, () -> SkillGrowth.rdSlotBandLower(List.of(1.0, 2.0), 0));
    }

    /**
     * Verifies {@code drawRdSlotRawAmount} always stays within the drawn
     * slot's own band across many draws, for every slot.
     */
    @Test
    void drawRdSlotRawAmountStaysWithinItsSlotsBand() {
        List<Double> bands = List.of(3.0, 6.0, 9.0, 12.0);
        Random random = new Random(42L);
        for (int slot = 0; slot < 4; slot++) {
            double lower = SkillGrowth.rdSlotBandLower(bands, slot);
            double upper = SkillGrowth.rdSlotBandUpper(bands, slot);
            for (int i = 0; i < 1_000; i++) {
                double amount = SkillGrowth.drawRdSlotRawAmount(bands, slot, random);
                assertTrue(amount >= lower && amount < upper,
                    "slot " + slot + " drew " + amount + " outside [" + lower + ", " + upper + ")");
            }
        }
    }

    /**
     * Verifies a degenerate band (lower equal to upper, e.g. an all-zero
     * band list) draws the fixed bound rather than throwing -
     * {@code RandomGenerator.nextDouble(min, max)} itself requires a
     * strictly smaller bound, so this formula must guard that case.
     */
    @Test
    void drawRdSlotRawAmountHandlesADegenerateBand() {
        List<Double> allZero = List.of(0.0, 0.0, 0.0, 0.0);

        assertEquals(0.0, SkillGrowth.drawRdSlotRawAmount(allZero, 3, new Random(42L)));
    }

    /**
     * Verifies the exact post-mission-season reliability-gain formula:
     * headroom to {@code maxReliability}, times the factor.
     */
    @Test
    void missionReliabilityGainMatchesTheExactFormula() {
        // (90 - 50) * 0.1 = 4.0.
        assertEquals(4.0, SkillGrowth.missionReliabilityGain(50.0, 90.0, 0.1), 1e-9);
    }

    /**
     * Verifies a component already at or above its own max gains nothing
     * further, rather than the headroom term going negative.
     */
    @Test
    void missionReliabilityGainAtOrAboveMaxIsZero() {
        assertEquals(0.0, SkillGrowth.missionReliabilityGain(90.0, 90.0, SkillGrowth.MISSION_RELIABILITY_SUCCESS_FACTOR_MAX));
        assertEquals(0.0, SkillGrowth.missionReliabilityGain(95.0, 90.0, SkillGrowth.MISSION_RELIABILITY_SUCCESS_FACTOR_MAX));
    }

    /**
     * Verifies the exact post-mission-season reliability-loss formula:
     * headroom to {@code floorReliability}, times the factor, times the
     * involvement percentage expressed as a 0-1 fraction.
     */
    @Test
    void missionReliabilityLossMatchesTheExactFormula() {
        // (50 - 20) * 0.2 * (60 / 100) = 30 * 0.2 * 0.6 = 3.6.
        assertEquals(3.6, SkillGrowth.missionReliabilityLoss(50.0, 20.0, 0.2, 60.0), 1e-9);
    }

    /**
     * Verifies a component already at or below its own floor loses nothing
     * further, rather than the headroom term going negative.
     */
    @Test
    void missionReliabilityLossAtOrBelowFloorIsZero() {
        assertEquals(0.0, SkillGrowth.missionReliabilityLoss(20.0, 20.0, SkillGrowth.MISSION_RELIABILITY_FAILURE_FACTOR_MAX, 100.0));
        assertEquals(0.0, SkillGrowth.missionReliabilityLoss(15.0, 20.0, SkillGrowth.MISSION_RELIABILITY_FAILURE_FACTOR_MAX, 100.0));
    }

    /**
     * Verifies full (100%) involvement applies the loss unscaled - the
     * involvement clamp's identity case.
     */
    @Test
    void missionReliabilityLossAtFullInvolvementIsUnscaled() {
        assertEquals(6.0, SkillGrowth.missionReliabilityLoss(50.0, 20.0, 0.2, 100.0), 1e-9);
    }

    /**
     * Verifies zero involvement applies no loss at all, regardless of
     * headroom or factor.
     */
    @Test
    void missionReliabilityLossAtZeroInvolvementIsZero() {
        assertEquals(0.0, SkillGrowth.missionReliabilityLoss(50.0, 20.0, SkillGrowth.MISSION_RELIABILITY_FAILURE_FACTOR_MAX, 0.0));
    }

    /**
     * Verifies an involvement percentage outside {@code [0, 100]} is
     * clamped rather than producing a loss outside the headroom-scaled
     * range - {@code MissionStep}'s own validation should already keep this
     * from happening in practice, but the formula itself must not rely on
     * that.
     */
    @Test
    void missionReliabilityLossClampsOutOfRangeInvolvement() {
        double atZero = SkillGrowth.missionReliabilityLoss(50.0, 20.0, 0.2, -10.0);
        double atFull = SkillGrowth.missionReliabilityLoss(50.0, 20.0, 0.2, 150.0);

        assertEquals(0.0, atZero);
        assertEquals(6.0, atFull, 1e-9);
    }

    /**
     * Verifies {@code drawMissionSkillGrowthFactor} always stays within its
     * documented range across many draws.
     */
    @Test
    void drawMissionSkillGrowthFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawMissionSkillGrowthFactor(random);
            assertTrue(factor >= SkillGrowth.MISSION_SKILL_GROWTH_FACTOR_MIN
                && factor < SkillGrowth.MISSION_SKILL_GROWTH_FACTOR_MAX);
        }
    }

    /**
     * Verifies {@code drawMissionReliabilitySuccessFactor} always stays
     * within its documented range across many draws.
     */
    @Test
    void drawMissionReliabilitySuccessFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawMissionReliabilitySuccessFactor(random);
            assertTrue(factor >= SkillGrowth.MISSION_RELIABILITY_SUCCESS_FACTOR_MIN
                && factor < SkillGrowth.MISSION_RELIABILITY_SUCCESS_FACTOR_MAX);
        }
    }

    /**
     * Verifies {@code drawMissionReliabilityMinorGlitchFactor} always stays
     * within its documented range across many draws.
     */
    @Test
    void drawMissionReliabilityMinorGlitchFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawMissionReliabilityMinorGlitchFactor(random);
            assertTrue(factor >= SkillGrowth.MISSION_RELIABILITY_MINOR_GLITCH_FACTOR_MIN
                && factor < SkillGrowth.MISSION_RELIABILITY_MINOR_GLITCH_FACTOR_MAX);
        }
    }

    /**
     * Verifies {@code drawMissionReliabilityFailureFactor} always stays
     * within its documented range across many draws.
     */
    @Test
    void drawMissionReliabilityFailureFactorStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double factor = SkillGrowth.drawMissionReliabilityFailureFactor(random);
            assertTrue(factor >= SkillGrowth.MISSION_RELIABILITY_FAILURE_FACTOR_MIN
                && factor < SkillGrowth.MISSION_RELIABILITY_FAILURE_FACTOR_MAX);
        }
    }

    /**
     * Verifies a skill exactly at the threshold gets no bonus at all -
     * just the flat base chance.
     */
    @Test
    void expertTrainingSuccessChanceAtTheThresholdIsJustTheBase() {
        assertEquals(SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE,
            SkillGrowth.expertTrainingSuccessChance(SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD), 1e-9);
    }

    /**
     * Verifies a skill at the cap gets the full bonus on top of the base
     * chance.
     */
    @Test
    void expertTrainingSuccessChanceAtTheCapIsBasePlusFullBonus() {
        assertEquals(
            SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE + SkillGrowth.EXPERT_TRAINING_SKILL_BONUS_MAX,
            SkillGrowth.expertTrainingSuccessChance(SkillGrowth.SKILL_CAP), 1e-9);
    }

    /**
     * Verifies the exact formula at a value partway between the threshold
     * and the cap - distinguishing the correct linear interpolation from a
     * plausible but wrong alternative (e.g. scaling against the full 0-100
     * range instead of the threshold-to-cap range).
     */
    @Test
    void expertTrainingSuccessChanceInterpolatesLinearly() {
        // threshold 80, cap 100: skill 90 is halfway, so half the +10 bonus = +5, total 30.
        assertEquals(30.0, SkillGrowth.expertTrainingSuccessChance(90.0), 1e-9);
    }

    /**
     * Verifies the success chance never exceeds base-plus-full-bonus, even
     * for a skill value somehow above the cap.
     */
    @Test
    void expertTrainingSuccessChanceNeverExceedsBasePlusFullBonus() {
        double chance = SkillGrowth.expertTrainingSuccessChance(150.0);

        assertEquals(SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE + SkillGrowth.EXPERT_TRAINING_SKILL_BONUS_MAX,
            chance, 1e-9);
    }

    /**
     * Verifies the success chance never drops below the flat base, even
     * for a skill value somehow below the threshold.
     */
    @Test
    void expertTrainingSuccessChanceNeverDropsBelowTheBase() {
        double chance = SkillGrowth.expertTrainingSuccessChance(50.0);

        assertEquals(SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE, chance, 1e-9);
    }

    /**
     * Verifies {@code drawExpertTrainingRoll} always stays within its
     * documented range across many draws.
     */
    @Test
    void drawExpertTrainingRollStaysWithinItsRange() {
        Random random = new Random(42L);
        for (int i = 0; i < 1_000; i++) {
            double roll = SkillGrowth.drawExpertTrainingRoll(random);
            assertTrue(roll >= 0.0 && roll < 100.0);
        }
    }

    /**
     * Verifies the exact pass/fail boundary: a roll exactly at the
     * threshold fails, one roll above it succeeds.
     */
    @Test
    void expertTrainingSucceedsAtTheExactBoundary() {
        // successChance 30 -> threshold roll 70.
        assertTrue(SkillGrowth.expertTrainingSucceeds(70.1, 30.0));
        assertFalse(SkillGrowth.expertTrainingSucceeds(70.0, 30.0));
    }
}
