package de.mkoehler.launchwindow.rules;

/**
 * Reports a raise of the funding between reviews ({@link BudgetProgress}, {@code design.md} &sect;2.1(f)):
 * prestige grew past what the rate paid for, and the review in force pays for it now.
 *
 * @param reviewNumber the review in force, whose bracket priced the raise
 * @param prestige     the player's prestige at the raise
 * @param before       the funding per season before
 * @param after        the funding per season now
 * @author Mario Koehler
 */
public record FundingRaised(int reviewNumber, long prestige, long before, long after) implements ResolutionEvent {
}
