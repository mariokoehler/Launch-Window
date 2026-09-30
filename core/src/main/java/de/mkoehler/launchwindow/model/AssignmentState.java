package de.mkoehler.launchwindow.model;

/**
 * <p>What a {@link Personnel} record is currently doing, independent of
 * which group they belong to. Idle staff lose morale each season, busy
 * (non-{@code IDLE}) staff gain morale - that part is uniform across all
 * three groups. <b>Whether resignation/retirement is checked is not
 * uniform</b>: it depends on both this state and the employee's group - see
 * {@code de.mkoehler.launchwindow.rules.PersonnelLifecycle}'s own
 * {@code isProtectedFromDeparture} for the actual per-group rule (SET is
 * never protected regardless of this
 * state; Astronaut/Mission Control are protected only while
 * {@link #ASSIGNED}, not while {@link #IN_TRAINING}) and
 * {@code design.md} &sect;2.2(a)/(c) for why.</p>
 *
 * @author Mario Koehler
 */
public enum AssignmentState {

    /**
     * Not currently assigned to anything or in training.
     */
    IDLE,

    /**
     * Actively assigned to a duty (R&amp;D, a mission, or similar).
     */
    ASSIGNED,

    /**
     * Currently undergoing training.
     */
    IN_TRAINING
}
