package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one employee left a roster during a
 * {@link PersonnelLifecycle#advanceSeason} pass, automatically (resignation
 * or retirement, never dismissal - see {@link DepartureReason}).</p>
 *
 * @param group        which roster the employee left
 * @param personnelId  the departed employee's stable id
 * @param name         the departed employee's display name, captured at
 *                     departure time so a later report doesn't need to look
 *                     it up on a roster the employee has already left
 * @param reason       why they left
 * @param ageAtExit    their age, in whole years, at the moment they left
 * @param moraleAtExit their morale at the moment they left
 * @author Mario Koehler
 */
public record PersonnelDeparture(
    PersonnelGroup group,
    String personnelId,
    String name,
    DepartureReason reason,
    int ageAtExit,
    int moraleAtExit
) implements ResolutionEvent {
}
