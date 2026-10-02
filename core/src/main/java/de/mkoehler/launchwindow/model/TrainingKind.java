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
     * completed round (each the same length as {@link #ADVANCED}) until
     * {@link Personnel#getTrainingTargetLevel()} is reached, resolved by
     * {@code de.mkoehler.launchwindow.rules.TrainingProgress} - see that
     * class's own {@code shouldReenroll} for the termination guard that
     * also stops re-enrollment once a round's own gain becomes negligible
     * (unreachable target, or zero learning potential), not just once the
     * target is actually hit.
     */
    PERMANENT,

    /**
     * Open-ended training toward the Expert trait (an {@code expertSkills}
     * field declared per-subclass, e.g. {@link Astronaut#getExpertSkills()}/
     * {@link MissionControlOfficer#getExpertSkills()}) for one specific
     * skill - unlike {@link #BASIC}/{@link #ADVANCED}/{@link #PERMANENT},
     * has no total season count at all: each season rolls a fresh
     * pass/fail chance
     * ({@code de.mkoehler.launchwindow.rules.SkillGrowth#expertTrainingSuccessChance}),
     * resolved by {@code de.mkoehler.launchwindow.rules.TrainingProgress} -
     * see {@code design.md} &sect;2.2(c). Astronaut/Mission Control only,
     * never {@link SetSpecialist}.
     */
    EXPERT
}
