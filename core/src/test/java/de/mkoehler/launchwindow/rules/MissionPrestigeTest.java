package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.FlightTier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link MissionPrestige}'s pure arithmetic: repeat tiers,
 * tier totals, and step payouts. See {@code design.md} &sect;2.6(d).</p>
 *
 * @author Mario Koehler
 */
class MissionPrestigeTest {

    private static MissionConfiguration configurationPaying(long firstTimePrestige) {
        MissionStep step = new MissionStep("s1",
            List.of(new MissionStep.ComponentInvolvement("capsule", 100)), List.of(), List.of(), false);
        return new MissionConfiguration("cfg", "program", List.of(new MissionSeason(List.of(step))), 0, 0, 0,
            firstTimePrestige);
    }

    /**
     * Zero prior completions is a first-time flight.
     */
    @Test
    void noPriorCompletionIsFirstTime() {
        assertEquals(FlightTier.FIRST_TIME, MissionPrestige.tierFor(0));
    }

    /**
     * One prior completion is a second-time flight.
     */
    @Test
    void oneCompletionIsSecondTime() {
        assertEquals(FlightTier.SECOND_TIME, MissionPrestige.tierFor(1));
    }

    /**
     * Two or more prior completions are all later-time flights.
     */
    @Test
    void twoOrMoreCompletionsAreLaterTime() {
        assertEquals(FlightTier.LATER_TIME, MissionPrestige.tierFor(2));
        assertEquals(FlightTier.LATER_TIME, MissionPrestige.tierFor(7));
    }

    /**
     * A negative completion count is a bug upstream, not a tier.
     */
    @Test
    void negativeCompletionCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MissionPrestige.tierFor(-1));
    }

    /**
     * First-time pays the full total; repeats pay the fixed ratios, floored.
     */
    @Test
    void tierTotalsUseTheFixedRatiosAndFloor() {
        MissionConfiguration configuration = configurationPaying(1_001);
        assertEquals(1_001, MissionPrestige.tierTotal(configuration, FlightTier.FIRST_TIME));
        assertEquals(600, MissionPrestige.tierTotal(configuration, FlightTier.SECOND_TIME));
        assertEquals(400, MissionPrestige.tierTotal(configuration, FlightTier.LATER_TIME));
    }

    /**
     * Repeat flights are worth less, and later flights less again.
     */
    @Test
    void laterFlightsAreWorthLessThanEarlierOnes() {
        assertTrue(MissionPrestige.LATER_TIME_PERCENT < MissionPrestige.SECOND_TIME_PERCENT);
        assertTrue(MissionPrestige.SECOND_TIME_PERCENT < 100);
    }

    /**
     * A configuration that pays nothing pays nothing at every tier.
     */
    @Test
    void freeConfigurationPaysNothingAtAnyTier() {
        MissionConfiguration configuration = configurationPaying(0);
        for (FlightTier tier : FlightTier.values()) {
            assertEquals(0, MissionPrestige.tierTotal(configuration, tier));
        }
    }

    /**
     * A step's payout is its percentage share of the tier total, floored.
     */
    @Test
    void stepPayoutIsItsFlooredPercentageShare() {
        assertEquals(330, MissionPrestige.stepPayout(1_000, 33));
        assertEquals(0, MissionPrestige.stepPayout(1_000, 0));
        assertEquals(1_000, MissionPrestige.stepPayout(1_000, 100));
        assertEquals(3, MissionPrestige.stepPayout(10, 33));
    }
}
