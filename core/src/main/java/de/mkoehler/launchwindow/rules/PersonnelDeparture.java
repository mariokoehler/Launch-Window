package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.AssignmentState;

/**
 * <p>Reports that one employee left a roster during a
 * {@link PersonnelLifecycle#advanceSeason} pass, automatically (resignation
 * or retirement, never dismissal - see {@link DepartureReason}).</p>
 *
 * @param group                 which roster the employee left
 * @param personnelId           the departed employee's stable id
 * @param name                  the departed employee's display name,
 *                              captured at departure time so a later report
 *                              doesn't need to look it up on a roster the
 *                              employee has already left
 * @param reason                why they left
 * @param ageAtExit             their age, in whole years, at the moment
 *                              they left
 * @param moraleAtExit          their morale at the moment they left
 * @param assignmentStateAtExit what they were doing at the moment they left
 *                              ({@code design.md} &sect;2.2(c)'s "hint" for a
 *                              future UI: a departure while
 *                              {@link AssignmentState#ASSIGNED} or
 *                              {@link AssignmentState#IN_TRAINING} signals
 *                              the player has a replacement to assign, not
 *                              just a roster slot to refill)
 * @param rdTargetComponentIdAtExit the mission component id a departing
 *                              {@code SET} employee's R&amp;D was targeting
 *                              at the moment they left, or {@code null} for
 *                              every non-{@code SET} departure and for a
 *                              {@code SET} departure with no R&amp;D target
 *                              set - the finer-grained hint
 *                              {@link #assignmentStateAtExit} alone can't
 *                              carry
 * @author Mario Koehler
 */
public record PersonnelDeparture(
    PersonnelGroup group,
    String personnelId,
    String name,
    DepartureReason reason,
    int ageAtExit,
    int moraleAtExit,
    AssignmentState assignmentStateAtExit,
    String rdTargetComponentIdAtExit
) implements ResolutionEvent {
}
