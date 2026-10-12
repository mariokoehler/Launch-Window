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
     * The review forecast matches the real season pass: it counts the same number of resolutions
     * to the next review, names its number, and predicts the rate it then pays.
     */
    @Test
    void theForecastMatchesTheSeasonPass() {
        for (int start : new int[] {1, 5, 15, 16, 17, 31, 32}) {
            PlayerSnapshot snapshot = player(10_000, 2_500, 150);
            snapshot.setSeason(start);
            snapshot.setPrestigeAtLastReview(400);
            BudgetProgress.ReviewForecast forecast = BudgetProgress.forecast(snapshot, SCHEDULE);

            int resolutions = 0;
            BudgetReviewed review = null;
            while (review == null) {
                resolutions++;
                for (ResolutionEvent event : BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE,
                        snapshot.getSeason())) {
                    if (event instanceof BudgetReviewed reviewed) {
                        review = reviewed;
                    }
                }
                snapshot.setSeason(snapshot.getSeason() + 1);
            }
            assertEquals(resolutions, forecast.seasonsUntil(), "from season " + start);
            assertEquals(review.reviewNumber(), forecast.reviewNumber(), "from season " + start);
            assertEquals(review.fundingRate(), forecast.payToday(), "from season " + start);
        }
    }

    /**
     * The runway names the season the real season pass first leaves the funds below zero, the next
     * review included, and is empty while funding covers the costs.
     */
    @Test
    void theRunwayMatchesTheSeasonPass() {
        for (int start : new int[] {1, 9, 16}) {
            for (long extra : new long[] {0, 40}) {
                PlayerSnapshot snapshot = player(2_000, 2_500, 150, 200, 150);
                snapshot.setSeason(start);
                snapshot.setPrestigeAtLastReview(400);
                int predicted = BudgetProgress.runway(snapshot, EMPTY, SCHEDULE, extra).orElseThrow();
                int season = 0;
                while (snapshot.getFunds() >= 0) {
                    season++;
                    BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, snapshot.getSeason());
                    snapshot.setFunds(snapshot.getFunds() - extra);
                    snapshot.setSeason(snapshot.getSeason() + 1);
                }
                assertEquals(season, predicted, "from season " + start + " with " + extra + " more");
            }
        }
        assertTrue(BudgetProgress.runway(player(2_000, 2_500, 500, 200), EMPTY, SCHEDULE, 0).isEmpty());
    }

    /**
     * The prestige that keeps today's funding is the least that pays it at the next review.
     */
    @Test
    void theForecastFindsThePrestigeThatKeepsTodaysFunding() {
        PlayerSnapshot snapshot = player(10_000, 2_500, 450);
        snapshot.setSeason(20);
        snapshot.setPrestigeAtLastReview(1_000);
        BudgetProgress.ReviewForecast forecast = BudgetProgress.forecast(snapshot, SCHEDULE);

        assertEquals(2, forecast.reviewNumber());
        long keep = forecast.keepPrestige();
        assertTrue(forecast.payAt(keep) >= 450);
        assertTrue(forecast.payAt(keep - 1) < 450);
        snapshot.setFundingRatePerSeason(700);
        assertEquals(-1, BudgetProgress.forecast(snapshot, SCHEDULE).keepPrestige(), "more than the bracket pays");
    }

    /**
     * The cost detail adds up: its lines make the base, and the base less the multiplier's saving
     * is exactly what the season pass charges, with and without a cost multiplier.
     */
    @Test
    void theCostDetailAddsUpToTheRunningCosts() {
        PlayerSnapshot snapshot = player(10_000, 0, 100, 30.4, 25.6, 41.5);
        for (double multiplier : new double[] {1.0, 0.85, 1.2}) {
            snapshot.setCostMultiplier(multiplier, multiplier == 1.0 ? 0 : 3);
            BudgetProgress.CostDetail detail = BudgetProgress.costDetail(snapshot, EMPTY);
            long lines = detail.groups().stream().flatMap(group -> group.lines().stream())
                .mapToLong(BudgetProgress.CostLine::amount).sum();
            assertEquals(detail.base(), lines);
            assertEquals(BudgetProgress.runningCosts(snapshot, EMPTY), detail.base() - detail.saving());
            assertEquals(30 + 26 + 42, detail.groups().get(0).lines().get(0).amount(), "per-person rounding");
        }
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
        assertEquals(10_000 - 900 / 4, indebted.getPrestige(), "this schedule pays 4 funds per prestige point");
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

    /** A player reviewed at season 16 with 400 prestige: bracket 1 pays 140. */
    private static PlayerSnapshot reviewed(long prestige) {
        PlayerSnapshot snapshot = player(0, 400, 50);
        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 16);
        assertEquals(140, snapshot.getFundingRatePerSeason());
        snapshot.setPrestige(prestige);
        return snapshot;
    }

    /**
     * Between reviews, prestige gained raises the funding by what the review in force pays for it,
     * the same season, and records the prestige paid for (design.md 2.1(f)).
     */
    @Test
    void prestigeGainedBetweenReviewsRaisesTheFundingAtOnce() {
        PlayerSnapshot snapshot = reviewed(700);

        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 17);

        assertEquals(new FundingRaised(1, 700, 140, 170), events.get(0));
        assertEquals(170, snapshot.getFundingRatePerSeason());
        assertEquals(170, ((SeasonFundsSettled) events.get(1)).income(), "the season pays the raised rate");
        assertEquals(700, snapshot.getFundedPrestige());
        assertEquals(400, snapshot.getPrestigeAtLastReview(), "the catch-up baseline is the review's own");
    }

    /** It never cuts, and prestige lost then regained pays nothing twice. */
    @Test
    void theFundingNeverFallsBetweenReviewsAndIsNeverPaidTwice() {
        PlayerSnapshot snapshot = reviewed(700);
        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 17);
        snapshot.setPrestige(100);

        assertEquals(1, BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 18).size(), "no raise, no cut");
        assertEquals(170, snapshot.getFundingRatePerSeason());

        snapshot.setPrestige(700);
        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 19);
        assertEquals(170, snapshot.getFundingRatePerSeason());
        snapshot.setPrestige(800);
        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 20);
        assertEquals(180, snapshot.getFundingRatePerSeason());
    }

    /** A random event's raise stays on top: the raise adds, it doesn't reset to the bracket. */
    @Test
    void aRaiseAddsToAnExtraFundingEvent() {
        PlayerSnapshot snapshot = reviewed(700);
        snapshot.setFundingRatePerSeason(160);

        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 17);

        assertEquals(190, snapshot.getFundingRatePerSeason());
    }

    /** Before the first review the starting rate holds, and a review season leaves it to the review. */
    @Test
    void noRaiseBeforeTheFirstReviewNorInAReviewSeason() {
        PlayerSnapshot fresh = player(0, 900, 50);
        assertEquals(1, BudgetProgress.advanceSeason(fresh, EMPTY, SCHEDULE, 5).size());
        assertEquals(50, fresh.getFundingRatePerSeason());

        PlayerSnapshot snapshot = reviewed(3_000);
        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 32);
        assertEquals(new BudgetReviewed(2, 3_000, 0, 450), events.get(0));
        assertEquals(3_000, snapshot.getFundedPrestige());
    }

    /** The raise reads the bracket the review in force used, shifted as it was. */
    @Test
    void aRaiseReadsTheReviewsShiftedBracket() {
        PlayerSnapshot snapshot = player(0, 400, 50);
        snapshot.setRivalFirstsCredit(200);
        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 16);
        assertEquals(160, snapshot.getFundingRatePerSeason(), "400 plus 200 of credit");
        assertEquals(200, snapshot.getLastReviewShift());
        snapshot.setPrestige(700);

        BudgetProgress.advanceSeason(snapshot, EMPTY, SCHEDULE, 17);

        assertEquals(190, snapshot.getFundingRatePerSeason(), "700 reads as 900 in the shifted bracket");
        snapshot.setSeason(18);
        assertEquals(10, BudgetProgress.raiseAt(snapshot, SCHEDULE, 1_000), "1,000 reads as 1,200: capped at 200");
    }

    /** Half a share pays half of each raise, rounded up; 0 turns raises off. */
    @Test
    void theRaiseShareScalesEachRaise() {
        try {
            BudgetProgress.setRaiseShare(0.5);
            PlayerSnapshot half = reviewed(710);
            BudgetProgress.advanceSeason(half, EMPTY, SCHEDULE, 17);
            assertEquals(140 + 16, half.getFundingRatePerSeason());

            BudgetProgress.setRaiseShare(0);
            PlayerSnapshot off = reviewed(900);
            BudgetProgress.advanceSeason(off, EMPTY, SCHEDULE, 17);
            assertEquals(140, off.getFundingRatePerSeason());
        } finally {
            BudgetProgress.setRaiseShare(BudgetProgress.RAISE_SHARE);
        }
    }
}
