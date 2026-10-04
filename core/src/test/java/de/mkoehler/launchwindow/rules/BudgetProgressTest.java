package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link BudgetProgress}: running costs, the season order, the
 * review cadence and interpolation, the catch-up shift, and debt. A two-bracket
 * schedule keeps the arithmetic easy to check by hand.</p>
 *
 * @author Mario Koehler
 */
class BudgetProgressTest {

    private static final ContentCatalog EMPTY = TestBuildings.catalogWithoutUpkeep(List.of(), List.of(), List.of(), List.of());

    /** Bracket 1 pays 100 at 0 prestige, 200 at 1,000; bracket 2 pays 300 at 2,000, 600 at 4,000. */
    private static final BudgetSchedule SCHEDULE = new BudgetSchedule(1_000, 0, 50, 4, List.of(
        new BudgetSchedule.Review(0, 100, 1_000, 200),
        new BudgetSchedule.Review(2_000, 300, 4_000, 600)));

    private static PlayerSnapshot player(long funds, long prestige, long rate, double... salaries) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, funds, prestige, rate, 1);
        List<Astronaut> roster = new java.util.ArrayList<>();
        int id = 0;
        for (double salary : salaries) {
            Astronaut astronaut = new Astronaut("a" + id, "a" + id, 30, 70, 70);
            astronaut.setSalary(salary);
            roster.add(astronaut);
            id++;
        }
        snapshot.setAstronauts(roster);
        return snapshot;
    }

    /**
     * Running costs are the roster's salaries; nothing else is open or owned here.
     */
    @Test
    void runningCostsAreTheRostersSalaries() {
        assertEquals(70, BudgetProgress.runningCosts(player(0, 0, 0, 30, 40), EMPTY));
    }

    /**
     * A quiet season charges the costs, then grants the current rate.
     */
    @Test
    void ordinarySeasonChargesCostsThenGrantsTheRate() {
        PlayerSnapshot snapshot = player(1_000, 0, 200, 30, 40);

        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 1);

        assertEquals(1_000 - 70 + 200, snapshot.getFunds());
        assertEquals(1, events.size(), "no review falls due at season 1");
        SeasonFundsSettled settled = (SeasonFundsSettled) events.get(0);
        assertEquals(List.of(70L, 200L, 1_130L), List.of(settled.expenses(), settled.income(), settled.fundsAfter()));
        assertEquals(70, settled.costs().salaries() + settled.costs().programs() + settled.costs().components()
            + settled.costs().buildings(), "the parts add up to the total");
    }

    /**
     * A review falls due every sixteenth season, and its new rate is the one
     * granted that same season.
     */
    @Test
    void reviewFallsDueEverySixteenthSeasonAndPaysTheNewRate() {
        PlayerSnapshot snapshot = player(0, 500, 200);

        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 16);

        // Bracket 1 at 500 prestige: halfway between 100 and 200.
        assertEquals(new BudgetReviewed(1, 500, 0, 150), events.get(0));
        assertEquals(150, snapshot.getFundingRatePerSeason());
        assertEquals(500, snapshot.getPrestigeAtLastReview());
        SeasonFundsSettled settled = (SeasonFundsSettled) events.get(1);
        assertEquals(List.of(0L, 150L, 150L), List.of(settled.expenses(), settled.income(), settled.fundsAfter()));
    }

    /**
     * Interpolation rounds up, and is flat at both ends of the bracket.
     */
    @Test
    void interpolationIsFlatAtEachEndAndRoundsUpInBetween() {
        assertEquals(100, BudgetProgress.interpolate(0, 100, 1_000, 200, -5));
        assertEquals(100, BudgetProgress.interpolate(0, 100, 1_000, 200, 0));
        assertEquals(200, BudgetProgress.interpolate(0, 100, 1_000, 200, 1_000));
        assertEquals(200, BudgetProgress.interpolate(0, 100, 1_000, 200, 9_999));
        assertEquals(150, BudgetProgress.interpolate(0, 100, 1_000, 200, 500));
        assertEquals(151, BudgetProgress.interpolate(0, 100, 1_000, 200, 501));
    }

    /**
     * An underperforming review pulls the next review's bracket down by the
     * shortfall against the previous review's raw minimum.
     */
    @Test
    void catchUpShiftsTheNextBracketByTheShortfall() {
        PlayerSnapshot snapshot = player(0, 2_000, 200);
        snapshot.setPrestigeAtLastReview(0);

        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 32);

        // Previous raw minimum is 0 and 0 prestige is not below it, so no shift applies.
        assertEquals(new BudgetReviewed(2, 2_000, 0, 300), events.get(0));
    }

    /**
     * A shortfall against the previous review's minimum shifts the bracket down
     * by exactly that shortfall, and the shift does not compound on a later review.
     */
    @Test
    void catchUpShortfallIsMeasuredAgainstThePreviousRawMinimum() {
        BudgetSchedule strict = new BudgetSchedule(1_000, 0, 50, 4, List.of(
            new BudgetSchedule.Review(500, 100, 1_000, 200),
            new BudgetSchedule.Review(2_000, 300, 4_000, 600)));
        PlayerSnapshot snapshot = player(0, 2_000, 200);
        snapshot.setPrestigeAtLastReview(100);

        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, strict, 32);

        // Previous raw minimum 500 was missed by 400 (100 reached), so the bracket shifts down 400:
        // now 1,600..3,600, and at 2,000 the line gives 0.15 * 2,000 + 60 = 360.
        assertEquals(new BudgetReviewed(2, 2_000, 400, 360), events.get(0));
    }

    /**
     * Funds may go negative: debt is allowed, not a defeat ({@code design.md} &sect;2.1(d)).
     */
    @Test
    void debtIsAllowedAndNotClampedToZero() {
        PlayerSnapshot snapshot = player(0, 0, 50, 100);

        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 1);

        assertEquals(-50, snapshot.getFunds());
    }

    /**
     * A conversion brings funds up to two seasons of running costs at most, and
     * is only available while in debt.
     */
    @Test
    void conversionIsOnlyOfferedInDebtAndCappedAtTwoSeasonsOfCosts() {
        PlayerSnapshot solvent = player(10, 10_000, 0, 200);
        assertEquals(PrestigeConversion.Outcome.NOT_IN_DEBT, PrestigeConversion.convert(solvent, EMPTY, SCHEDULE, 5));

        // Funds -500, running costs 200 -> cap is 2*200 + 500 = 900.
        PlayerSnapshot indebted = player(-500, 10_000, 0, 200);
        assertEquals(900, PrestigeConversion.maxConversion(indebted, EMPTY));
        assertEquals(PrestigeConversion.Outcome.EXCEEDS_CAP, PrestigeConversion.convert(indebted, EMPTY, SCHEDULE, 901));
        assertEquals(PrestigeConversion.Outcome.NOT_POSITIVE, PrestigeConversion.convert(indebted, EMPTY, SCHEDULE, 0));

        assertEquals(PrestigeConversion.Outcome.CONVERTED, PrestigeConversion.convert(indebted, EMPTY, SCHEDULE, 900));
        assertEquals(400, indebted.getFunds(), "funds end at exactly two seasons of running costs");
        assertEquals(10_000 - 900 * 4, indebted.getPrestige(), "4 prestige per fund");
    }

    /**
     * A conversion the player can't pay for in prestige changes nothing.
     */
    @Test
    void conversionNeedsEnoughPrestigeAndChangesNothingOtherwise() {
        PlayerSnapshot poor = player(-500, 100, 0, 200);

        assertEquals(PrestigeConversion.Outcome.INSUFFICIENT_PRESTIGE,
            PrestigeConversion.convert(poor, EMPTY, SCHEDULE, 900));
        assertEquals(-500, poor.getFunds());
        assertEquals(100, poor.getPrestige());
    }

    /**
     * A schedule must have at least one bracket, and each bracket's top must
     * sit above its bottom.
     */
    @Test
    void scheduleRejectsAnEmptyTableAndAnInvertedBracket() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetSchedule(0, 0, 0, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new BudgetSchedule.Review(1_000, 0, 1_000, 10));
        assertFalse(SCHEDULE.reviews().isEmpty());
        assertTrue(SCHEDULE.reviewFor(9).equals(SCHEDULE.reviews().get(1)), "past the table repeats the last bracket");
    }
}
