package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.TrainingKind;

/**
 * <p>Reports that one training <em>round</em> finished during a
 * {@link TrainingProgress#advanceSeason} pass - for {@link TrainingKind#BASIC}/
 * {@link TrainingKind#ADVANCED} this always means the whole training
 * assignment is over; for {@link TrainingKind#PERMANENT} it means just one
 * round of a possibly-still-ongoing standing order completed (it
 * re-enrolls automatically unless the target level was reached or the
 * round's own gain fell below {@link TrainingProgress#PERMANENT_TRAINING_GAIN_EPSILON}
 * - check the employee's own {@code trainingKind} afterward, still
 * {@code PERMANENT} if they re-enrolled, {@code NONE} if this was the
 * final round).</p>
 *
 * @param group        which roster the employee belongs to
 * @param personnelId  the employee's stable id
 * @param name         the employee's display name
 * @param trainingKind which kind of training this round belonged to
 * @param skillName    the {@code name()} of the skill enum constant the
 *                     training targeted - a plain string since the three
 *                     personnel groups use different skill enum types with
 *                     no common supertype to hold here
 * @param skillGain    how much the target skill grew this round
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
