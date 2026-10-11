package de.mkoehler.launchwindow.model;

import java.util.List;
import java.util.Objects;

/**
 * The R&amp;D decision waiting for the player, or decided this turn ({@code ui-spec.md} component 26):
 * offered when a turn resolves, decided whenever the player likes, settled when the next turn
 * resolves (its default applied if it's still open). The outcome is drawn from {@link #seed()}, fixed
 * when it was offered, so deciding can't be rerolled and a replay of the journal draws the same.
 *
 * @param decisionId     the decision, by its id in {@code decisions.json}
 * @param componentId    the component it's about
 * @param offeredTurn    the game turn it was offered on
 * @param seed           the seed its outcome is drawn from; never sent to the client
 * @param decided        whether the player has decided
 * @param optionId       the option chosen, or {@code null} while open
 * @param outcomeId      the outcome drawn, or {@code null} while open
 * @param personId       the person the option took (overseer, seconded, first of a pair), or {@code null}
 * @param secondPersonId the second of a pair, or {@code null}
 * @param effects        what the outcome did, empty while open
 * @param handsOn        a hands-on decision's puzzle once the player started it, else {@code null}
 * @author Mario Koehler
 */
public record PendingDecision(String decisionId, String componentId, int offeredTurn, long seed, boolean decided,
                              String optionId, String outcomeId, String personId, String secondPersonId,
                              List<DecisionEffect> effects, HandsOnState handsOn) {

    /**
     * Checks the ids and copies the effects.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public PendingDecision {
        Objects.requireNonNull(decisionId, "decisionId must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /**
     * Returns a decision just offered.
     *
     * @param decisionId  the decision
     * @param componentId the component
     * @param turn        the turn
     * @param seed        its seed
     * @return the open decision
     */
    public static PendingDecision offered(String decisionId, String componentId, int turn, long seed) {
        return new PendingDecision(decisionId, componentId, turn, seed, false, null, null, null, null, List.of(), null);
    }

    /**
     * Returns this decision, decided.
     *
     * @param option   the option chosen
     * @param outcome  the outcome drawn
     * @param person   the person it took, or {@code null}
     * @param second   the second of a pair, or {@code null}
     * @param applied  what it did
     * @return the decided decision
     */
    public PendingDecision decide(String option, String outcome, String person, String second,
                                  List<DecisionEffect> applied) {
        return new PendingDecision(decisionId, componentId, offeredTurn, seed, true, option, outcome, person, second,
            applied, handsOn);
    }

    /**
     * Returns this decision with its seed hidden, for the client.
     *
     * @return the copy
     */
    public PendingDecision withoutSeed() {
        return new PendingDecision(decisionId, componentId, offeredTurn, 0, decided, optionId, outcomeId, personId,
            secondPersonId, effects, handsOn);
    }

    /**
     * Returns this decision with its puzzle as it now stands.
     *
     * @param state the puzzle
     * @return the copy
     */
    public PendingDecision withHandsOn(HandsOnState state) {
        return new PendingDecision(decisionId, componentId, offeredTurn, seed, decided, optionId, outcomeId, personId,
            secondPersonId, effects, state);
    }

    /**
     * Returns whether the player has started this decision's puzzle and not finished it.
     *
     * @return whether a hands-on campaign is in progress
     */
    public boolean handsOnInProgress() {
        return handsOn != null && !decided;
    }
}
