package de.mkoehler.launchwindow.model;

/**
 * <p>What a {@link Personnel} record is currently doing, independent of
 * which group they belong to. Idle staff lose morale each season, busy
 * staff gain morale, and resignation/retirement is only ever checked while
 * idle - see {@code design.md} &sect;2.2(a)/(b).</p>
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
