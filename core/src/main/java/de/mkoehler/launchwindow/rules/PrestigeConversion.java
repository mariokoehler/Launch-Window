package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.Objects;

/**
 * <p>The prestige-to-funds conversion a player may take while in debt
 * ({@code design.md} &sect;2.1(d), decided 2026-10-03): the player chooses an
 * amount of funds, paid for in prestige at {@link BudgetSchedule#fundsPerPrestige()}
 * funds per point ({@link BudgetSchedule#prestigeCost(long)}). A player is only offered this while their funds are below
 * zero, and the amount is capped so that the funds end up no higher than two
 * seasons of running costs.</p>
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
        /** The player's funds aren't below zero, so nothing may be converted. */
        NOT_IN_DEBT,
        /** The requested amount isn't positive. */
        NOT_POSITIVE,
        /** The requested amount would take funds past two seasons of running costs. */
        EXCEEDS_CAP,
        /** The player doesn't hold enough prestige to pay for the amount. */
        INSUFFICIENT_PRESTIGE
    }

    /**
     * Converts prestige into funds, if the player is in debt and the amount
     * fits the cap. Changes nothing unless the outcome is {@link Outcome#CONVERTED}.
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
        if (snapshot.getFunds() >= 0) {
            return Outcome.NOT_IN_DEBT;
        }
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
        snapshot.setFunds(snapshot.getFunds() + amount);
        snapshot.setPrestige(snapshot.getPrestige() - prestigeCost);
        return Outcome.CONVERTED;
    }

    /**
     * The largest amount one conversion may add: enough to bring funds up to
     * {@link #COVERED_SEASONS} seasons of running costs, which from a debt of
     * {@code D} is {@code D + 2R}. Zero if the funds already cover that.
     *
     * @param snapshot the player
     * @param catalog  the content catalog, for the running costs
     * @return the cap, non-negative
     */
    public static long maxConversion(PlayerSnapshot snapshot, ContentCatalog catalog) {
        long target = COVERED_SEASONS * BudgetProgress.runningCosts(snapshot, catalog);
        return Math.max(0, target - snapshot.getFunds());
    }
}
