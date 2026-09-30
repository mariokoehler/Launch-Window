package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.TrainingKind;

/**
 * <p>Reports that one employee's fixed-length training round finished
 * during a {@link TrainingProgress#advanceSeason} pass.</p>
 *
 * @param group        which roster the employee belongs to
 * @param personnelId  the employee's stable id
 * @param name         the employee's display name
 * @param trainingKind which kind of training completed ({@link TrainingKind#BASIC}
 *                     or {@link TrainingKind#ADVANCED} - this event is never
 *                     raised for {@link TrainingKind#PERMANENT}, which
 *                     {@link TrainingProgress} does not resolve)
 * @param skillName    the {@code name()} of the skill enum constant the
 *                     training targeted - a plain string since the three
 *                     personnel groups use different skill enum types with
 *                     no common supertype to hold here
 * @param skillGain    how much the target skill grew
 * @author Mario Koehler
 */
public record TrainingCompleted(
    PersonnelGroup group,
    String personnelId,
    String name,
    TrainingKind trainingKind,
    String skillName,
    double skillGain
) implements ResolutionEvent {
}
