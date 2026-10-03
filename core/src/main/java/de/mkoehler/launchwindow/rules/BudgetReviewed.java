package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports one funding review, from a {@link BudgetProgress#advanceSeason}
 * pass. See {@code design.md} &sect;2.1(d) and {@code baspm.md} &sect;11.1.</p>
 *
 * @param reviewNumber the one-based review number (every 16 seasons)
 * @param prestige     the player's prestige at the review
 * @param catchUpShift how far the bracket shifted down for an underperforming previous review; zero if none
 * @param fundingRate  the new funding per season the review set
 * @author Mario Koehler
 */
public record BudgetReviewed(int reviewNumber, long prestige, long catchUpShift, long fundingRate)
    implements ResolutionEvent {
}
