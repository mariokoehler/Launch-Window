package de.mkoehler.launchwindow.model;

/**
 * <p>What kind of training a {@link Personnel} record is currently
 * undergoing while {@link AssignmentState#IN_TRAINING}. See {@code baspm.md}
 * &sect;9.3 and {@code design.md} &sect;3.4's field proposal.</p>
 *
 * <p>Kryo serializes enums by ordinal (see {@code MessageRegistry}'s own
 * Javadoc) - new constants may only ever be appended at the end, never
 * inserted or reordered.</p>
 *
 * @author Mario Koehler
 */
public enum TrainingKind {

    /**
     * Not currently training.
     */
    NONE,

    /**
     * Basic training - a fixed length, per-group ({@code baspm.md} &sect;9.3).
     */
    BASIC,

    /**
     * A single fixed-length round of advanced training.
     */
    ADVANCED,

    /**
     * Standing-order advanced training that automatically re-enrolls every
     * completed round until a target skill level is reached
     * ({@code baspm.md} &sect;9.3). <b>Not yet resolved by any code</b> -
     * see {@code design.md} &sect;3.4's field proposal for what's still
     * missing (a target level) before this can be driven by
     * {@code TrainingProgress}.
     */
    PERMANENT
}
