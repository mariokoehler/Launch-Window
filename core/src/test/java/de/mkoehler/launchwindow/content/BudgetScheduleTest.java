package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link BudgetSchedule}'s validation and its prestige conversion helpers.
 *
 * @author Mario Koehler
 */
class BudgetScheduleTest {

    private static final List<BudgetSchedule.Review> ONE_REVIEW = List.of(new BudgetSchedule.Review(0, 0, 10, 10));

    /**
     * The default schedule converts at 2 funds per prestige point (user, 2026-10-06).
     */
    @Test
    void defaultScheduleConvertsAtTwoFundsPerPoint() {
        assertEquals(2, BudgetSchedule.baspmClose().fundsPerPrestige());
    }

    /**
     * A conversion costs one point per {@code fundsPerPrestige} funds, rounded up.
     */
    @Test
    void prestigeCostRoundsUp() {
        BudgetSchedule schedule = new BudgetSchedule(0, 0, 0, 2, ONE_REVIEW);
        assertEquals(0, schedule.prestigeCost(0));
        assertEquals(1, schedule.prestigeCost(1), "a part point is paid in full");
        assertEquals(1, schedule.prestigeCost(2));
        assertEquals(500, schedule.prestigeCost(1_000));
        assertEquals(501, schedule.prestigeCost(1_001));
        assertThrows(IllegalArgumentException.class, () -> schedule.prestigeCost(-1));
    }

    /**
     * The funds prestige can pay for are the points times the rate, never negative.
     */
    @Test
    void affordableFundsIsPointsTimesRate() {
        BudgetSchedule schedule = new BudgetSchedule(0, 0, 0, 2, ONE_REVIEW);
        assertEquals(800, schedule.affordableFunds(400));
        assertEquals(0, schedule.affordableFunds(-5));
    }

    /**
     * A rate below one fund per point is refused.
     */
    @Test
    void rejectsNonPositiveRate() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetSchedule(0, 0, 0, 0, ONE_REVIEW));
    }
}
