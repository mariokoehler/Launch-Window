package de.mkoehler.launchwindow.rules;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link SkillGrowth}'s pure growth formulas.</p>
 *
 * @author Mario Koehler
 */
class SkillGrowthTest {

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
}
