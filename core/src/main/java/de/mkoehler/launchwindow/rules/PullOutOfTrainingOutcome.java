package de.mkoehler.launchwindow.rules;

/**
 * <p>What happened when {@code TrainingProgress.pullOutOfTraining} was
 * called. See {@code design.md} &sect;2.2(c).</p>
 *
 * @author Mario Koehler
 */
public enum PullOutOfTrainingOutcome {

    /** The employee was pulled out of training. */
    PULLED_OUT,

    /** The given employee id is not on this player's roster. */
    UNKNOWN_PERSONNEL,

    /** The employee exists, but is not currently, genuinely training. */
    NOT_IN_TRAINING
}
