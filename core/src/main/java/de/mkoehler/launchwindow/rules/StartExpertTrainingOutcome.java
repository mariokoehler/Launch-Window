package de.mkoehler.launchwindow.rules;

/**
 * <p>What happened when {@code TrainingProgress.startAstronautExpertTraining}/
 * {@code startMissionControlExpertTraining} was called. See
 * {@code design.md} &sect;2.2(c).</p>
 *
 * @author Mario Koehler
 */
public enum StartExpertTrainingOutcome {

    /** Training started. */
    STARTED,

    /** The given employee id is not on this player's roster. */
    UNKNOWN_PERSONNEL,

    /** The employee exists, but is not currently {@code IDLE}. */
    NOT_IDLE,

    /**
     * The employee's current value in the targeted skill is below
     * {@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD}.
     */
    SKILL_BELOW_THRESHOLD,

    /** The employee already holds the Expert trait in the targeted skill. */
    ALREADY_EXPERT_IN_SKILL
}
