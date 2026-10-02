package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one employee's {@link de.mkoehler.launchwindow.model.TrainingKind#EXPERT}
 * training succeeded this season, granting them the Expert trait for
 * {@code skillName} - during a {@link TrainingProgress#advanceSeason} pass.
 * Unlike {@link TrainingCompleted}, there is no per-season event for a
 * failed roll (the employee simply stays {@code IN_TRAINING} into another
 * season, silently) - this event only ever reports a success. See
 * {@code design.md} &sect;2.2(c).</p>
 *
 * @param group       which roster the employee belongs to - always
 *                    {@link PersonnelGroup#ASTRONAUT} or
 *                    {@link PersonnelGroup#MISSION_CONTROL}, never
 *                    {@link PersonnelGroup#SET}
 * @param personnelId the employee's stable id
 * @param name        the employee's display name
 * @param skillName   the {@code name()} of the skill enum constant the
 *                    Expert trait was granted for - a plain string since
 *                    the two eligible groups use different skill enum
 *                    types with no common supertype to hold here
 * @author Mario Koehler
 */
public record ExpertTrainingCompleted(
    PersonnelGroup group,
    String personnelId,
    String name,
    String skillName
) implements ResolutionEvent {
}
