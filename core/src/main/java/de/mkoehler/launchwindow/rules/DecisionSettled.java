package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.DecisionEffect;

import java.util.List;

/**
 * Reports how last turn's R&amp;D decision ended ({@code ui-spec.md} component 26): decided by the
 * player during the turn, or lapsed to its default when the turn resolved.
 *
 * @param decisionId  the decision
 * @param componentId the component it was about
 * @param optionId    the option that applied
 * @param outcomeId   the outcome drawn
 * @param lapsed      whether the player left it, so the default applied
 * @param effects     what it did
 * @author Mario Koehler
 */
public record DecisionSettled(String decisionId, String componentId, String optionId, String outcomeId,
                              boolean lapsed, List<DecisionEffect> effects) implements ResolutionEvent {

    /**
     * Copies the effects.
     */
    public DecisionSettled {
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
