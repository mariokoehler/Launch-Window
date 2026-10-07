package de.mkoehler.launchwindow.rules;

/**
 * A line of the turn report's Intelligence section ({@code design.md} &sect;2.9): one finding of the
 * agency's intelligence service, as it was applied.
 *
 * @param finding the finding
 * @author Mario Koehler
 */
public record IntelligenceFound(IntelFinding finding) implements ResolutionEvent {
}
