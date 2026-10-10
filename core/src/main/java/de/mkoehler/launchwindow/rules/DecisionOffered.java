package de.mkoehler.launchwindow.rules;

/**
 * Reports an R&amp;D decision put to the player when the turn resolved ({@code ui-spec.md}
 * component 26). It waits on the player's state until the next turn resolves.
 *
 * @param decisionId  the decision, by its id in {@code decisions.json}
 * @param componentId the component it's about
 * @param turn        the game turn it was offered on
 * @author Mario Koehler
 */
public record DecisionOffered(String decisionId, String componentId, int turn) implements ResolutionEvent {
}
