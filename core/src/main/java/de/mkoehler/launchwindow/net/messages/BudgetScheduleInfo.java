package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.content.BudgetSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * The game's budget schedule as it travels in {@link GameInfoResponse}, so the client prices
 * funding reviews and prestige conversions with the game's own numbers instead of assuming the
 * default ({@code design.md} &sect;2.1(d), decided 2026-10-06). {@link BudgetSchedule} itself keeps
 * its reviews in an immutable list Kryo can't rebuild, so the reviews travel here as an
 * {@link ArrayList}.
 *
 * @param initialFunds       the funds a new player starts with
 * @param initialPrestige    the prestige a new player starts with
 * @param initialFundingRate the funding per season before the first review
 * @param fundsPerPrestige   the funds one prestige point buys when converted in debt
 * @param reviews            the review brackets, in order
 * @author Mario Koehler
 */
public record BudgetScheduleInfo(long initialFunds, long initialPrestige, long initialFundingRate,
                                 long fundsPerPrestige, List<BudgetSchedule.Review> reviews) {

    /**
     * Copies a schedule for the wire.
     *
     * @param schedule the schedule
     * @return its wire form
     */
    public static BudgetScheduleInfo of(BudgetSchedule schedule) {
        return new BudgetScheduleInfo(schedule.initialFunds(), schedule.initialPrestige(),
            schedule.initialFundingRate(), schedule.fundsPerPrestige(), new ArrayList<>(schedule.reviews()));
    }

    /**
     * Rebuilds the schedule, validated as any schedule is.
     *
     * @return the schedule
     */
    public BudgetSchedule toSchedule() {
        return new BudgetSchedule(initialFunds, initialPrestige, initialFundingRate, fundsPerPrestige, reviews);
    }
}
