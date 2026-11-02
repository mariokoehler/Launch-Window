package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.Objects;

/**
 * <p>The prestige-to-funds conversion ({@code design.md} &sect;2.1(d), decided 2026-10-03, open
 * outside debt since 2026-11-02): the player chooses an amount of funds, paid for in prestige at
 * {@link BudgetSchedule#fundsPerPrestige()} funds per point ({@link BudgetSchedule#prestigeCost(long)}).
 * Two limits hold: funds end up no higher than two seasons of running costs, and a turn's
 * conversions together raise no more than its first one could have, so converting and spending in
 * turns can't empty a player's prestige in one go.</p>
 *
 * <p>Headless: the journaled player action that will call this lands with M4's
 * action dispatch, not here.</p>
 *
 * @author Mario Koehler
 */
public final class PrestigeConversion {

    /** The number of seasons of running costs a conversion may bring funds up to. */
    public static final int COVERED_SEASONS = 2;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PrestigeConversion() {
    }

    /**
     * The result of a conversion attempt.
     */
    public enum Outcome {
        /** The funds were credited and the prestige spent. */
        CONVERTED,
        /** The requested amount isn't positive. */
        NOT_POSITIVE,
        /** The requested amount would take funds past two seasons of running costs, or past the turn's limit. */
        EXCEEDS_CAP,
        /** The player doesn't hold enough prestige to pay for the amount. */
        INSUFFICIENT_PRESTIGE
    }

    /**
     * Converts prestige into funds, if the amount fits the cap. The turn's first conversion sets
     * the turn's limit. Changes nothing unless the outcome is {@link Outcome#CONVERTED}.
     *
     * @param snapshot the player converting; mutated in place on success
     * @param catalog  the content catalog, for the running costs
     * @param schedule the budget schedule, for the conversion rate
     * @param amount   the funds the player asks for
     * @return the outcome
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Outcome convert(PlayerSnapshot snapshot, ContentCatalog catalog, BudgetSchedule schedule,
            long amount) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(schedule, "schedule must not be null");
        if (amount <= 0) {
            return Outcome.NOT_POSITIVE;
        }
        long cap = maxConversion(snapshot, catalog);
        if (amount > cap) {
            return Outcome.EXCEEDS_CAP;
        }
        long prestigeCost = schedule.prestigeCost(amount);
        if (prestigeCost > snapshot.getPrestige()) {
            return Outcome.INSUFFICIENT_PRESTIGE;
        }
        if (snapshot.getConversionLimitThisTurn() == 0) {
            snapshot.setConversionLimitThisTurn(cap);
        }
        snapshot.setConvertedThisTurn(snapshot.getConvertedThisTurn() + amount);
        snapshot.setFunds(snapshot.getFunds() + amount);
        snapshot.setPrestige(snapshot.getPrestige() - prestigeCost);
        return Outcome.CONVERTED;
    }

    /**
     * The largest amount the next conversion may add: enough to bring funds up to
     * {@link #COVERED_SEASONS} seasons of running costs, which from a debt of
     * {@code D} is {@code D + 2R}, and no more than what's left of the turn's limit.
     * Zero if the funds already cover that or the limit is used up.
     *
     * @param snapshot the player
     * @param catalog  the content catalog, for the running costs
     * @return the cap, non-negative
     */
    public static long maxConversion(PlayerSnapshot snapshot, ContentCatalog catalog) {
        long room = roomToCeiling(snapshot, catalog);
        long limit = snapshot.getConversionLimitThisTurn();
        return limit == 0 ? room : Math.min(room, Math.max(0, limit - snapshot.getConvertedThisTurn()));
    }

    /**
     * The turn's limit: what its first conversion set, or, before one, what it could raise now.
     *
     * @param snapshot the player
     * @param catalog  the content catalog, for the running costs
     * @return the limit on the turn's conversions together, non-negative
     */
    public static long turnLimit(PlayerSnapshot snapshot, ContentCatalog catalog) {
        long limit = snapshot.getConversionLimitThisTurn();
        return limit == 0 ? roomToCeiling(snapshot, catalog) : limit;
    }

    /**
     * Whether the turn's limit, not the funds' ceiling, is what stops the next conversion.
     *
     * @param snapshot the player
     * @param catalog  the content catalog, for the running costs
     * @return {@code true} if funds have room but the turn's limit is used up
     */
    public static boolean turnLimitReached(PlayerSnapshot snapshot, ContentCatalog catalog) {
        return roomToCeiling(snapshot, catalog) > 0 && maxConversion(snapshot, catalog) == 0;
    }

    private static long roomToCeiling(PlayerSnapshot snapshot, ContentCatalog catalog) {
        long target = COVERED_SEASONS * BudgetProgress.runningCosts(snapshot, catalog);
        return Math.max(0, target - snapshot.getFunds());
    }
}
