package de.mkoehler.launchwindow.persistence;

import java.util.Objects;

/**
 * What applying a {@link PlayerAction} did: whether it changed the player, and the
 * rules outcome's name (for example {@code OPENED} or {@code INSUFFICIENT_FUNDS}).
 *
 * @param applied {@code true} if the action took effect
 * @param outcome the rules outcome's name
 * @author Mario Koehler
 */
public record ActionResult(boolean applied, String outcome) {

    /**
     * Validates the result.
     *
     * @throws NullPointerException if {@code outcome} is {@code null}
     */
    public ActionResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
    }

    /**
     * Builds a result from a rules outcome, applied exactly when it equals the success value.
     *
     * @param outcome the rules outcome
     * @param success the outcome that means the action took effect
     * @return the result
     */
    public static ActionResult of(Enum<?> outcome, Enum<?> success) {
        return new ActionResult(outcome == success, outcome.name());
    }
}
