package de.mkoehler.launchwindow.model;

/**
 * <p>Every random event Launch Window can roll, as data - BASPM's own set of 15
 * negative and 5 positive events, {@code baspm.md} &sect;12.1 - {@code design.md}
 * &sect;2.7. Effects and gates live in the random-events resolver; the magnitudes
 * and the pools live in content ({@code RandomEventRoster}).</p>
 *
 * <p>Kryo-registered (see {@code MessageRegistry}): a {@code NewsEntry} carries one,
 * and news is part of {@code PlayerSnapshot}.</p>
 *
 * @author Mario Koehler
 */
public enum RandomEventKind {

    /** Extends one trainee's basic training by one season. */
    BASIC_TRAINING_EXTENSION(false),
    /** Dismisses one employee. */
    EMPLOYEE_DISMISSED(false),
    /** Dismisses one trainee who failed basic training. */
    EMPLOYEE_FAILED_BASIC_TRAINING(false),
    /** One employee resigns. */
    EMPLOYEE_RESIGNING(false),
    /** One employee is unavailable for a season. */
    EMPLOYEE_UNAVAILABLE(false),
    /** Caps launches at one for the next launch window. */
    MAX_LAUNCHES_ALLOWED(false),
    /** Some R&D components are unavailable for a few seasons. */
    MISSION_COMPONENTS_UNAVAILABLE(false),
    /** One personnel group loses morale after a recent failure. */
    MORALE_DROP(false),
    /** No launches are allowed in the next launch window. */
    NO_LAUNCHES_ALLOWED(false),
    /** An idle astronaut dies. */
    PERISHED_ASTRONAUT(false),
    /** Prestige falls by a share of its current total. */
    PRESTIGE_DROP(false),
    /** R&D results are set back for a few seasons. */
    RD_RESULTS_SETBACK(false),
    /** Some R&D components lose reliability. */
    RELIABILITY_SETBACK(false),
    /** An unexpected cost takes a share of current funds. */
    UNEXPECTED_COST(false),
    /** An unexpected cost, plus no launches in the next window. */
    UNEXPECTED_COST_AND_NO_LAUNCHES(false),

    /** The funding rate rises until the next review. */
    EXTRA_FUNDING(true),
    /** One personnel group gains morale after recent successes. */
    MORALE_BOOST(true),
    /** Prestige rises by a share of its current total. */
    PRESTIGE_POINTS_BOOST(true),
    /** R&D results are boosted for a few seasons. */
    RD_RESULTS_BOOST(true),
    /** Running costs fall for a few seasons. */
    REDUCED_EXPENDITURES(true);

    private final boolean positive;

    RandomEventKind(boolean positive) {
        this.positive = positive;
    }

    /**
     * Returns whether this is a positive (good) event.
     *
     * @return {@code true} for the five good events, {@code false} for the fifteen bad ones
     */
    public boolean isPositive() {
        return positive;
    }
}
