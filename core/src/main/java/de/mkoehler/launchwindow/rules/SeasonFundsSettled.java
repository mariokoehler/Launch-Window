package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports one season's funds movement, from a {@link BudgetProgress#advanceSeason}
 * pass: the running costs charged first, then the funding granted. Funds may
 * go negative - debt is allowed ({@code design.md} &sect;2.1(d)).</p>
 *
 * @param expenses   the running costs charged this season
 * @param income     the funding granted this season, at the rate in force after any review
 * @param fundsAfter the player's funds after both
 * @author Mario Koehler
 */
public record SeasonFundsSettled(long expenses, long income, long fundsAfter) implements ResolutionEvent {
}
