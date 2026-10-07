package de.mkoehler.launchwindow.rules;

/**
 * The intelligence service moved one level at a resolution ({@code design.md} &sect;2.9): toward its
 * target, or down because the agency was in debt.
 *
 * @param from   the level before
 * @param to     the level now
 * @param target the target level
 * @param debt   whether it dropped because the funds didn't cover it
 * @author Mario Koehler
 */
public record IntelligenceLevelChanged(int from, int to, int target, boolean debt) implements ResolutionEvent {
}
