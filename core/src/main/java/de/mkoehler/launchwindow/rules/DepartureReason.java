package de.mkoehler.launchwindow.rules;

/**
 * <p>Why a {@link PersonnelDeparture} happened. {@link #RESIGNED} is
 * {@link PersonnelLifecycle}'s own automatic season-advance outcome,
 * checked against {@link PersonnelLifecycle#isProtectedFromDeparture}
 * rather than unconditionally every season (see {@code design.md}
 * &sect;2.2(a)/(c) and that method's own Javadoc for the per-group
 * protection rule - it is not simply "while idle"). {@link #RETIRED} is
 * reported by two different resolvers: {@link PersonnelLifecycle} the
 * same way as {@link #RESIGNED}, or {@link MissionProgress}'s own
 * retire-on-release check, which fires on an employee still
 * {@link de.mkoehler.launchwindow.model.AssignmentState#ASSIGNED} (and
 * therefore still {@code isProtectedFromDeparture}) at the moment a
 * mission releases them. {@link #LOST_IN_ACCIDENT} is reported by
 * {@link MissionProgress} instead, is astronaut-only, and is never subject
 * to {@code isProtectedFromDeparture} - being assigned to the very mission
 * that kills them is exactly what happens. Dismissal (a genuine player
 * action) is not part of this enum since nothing here triggers it.</p>
 *
 * @author Mario Koehler
 */
public enum DepartureReason {

    /**
     * Morale fell to or below {@link PersonnelLifecycle#RESIGNATION_MORALE_FLOOR}
     * while not protected from departure.
     */
    RESIGNED,

    /**
     * Age reached the employee's group's retirement age. Reported either
     * by {@link PersonnelLifecycle} (while not protected from departure,
     * the same check {@link #RESIGNED} uses) or by {@link MissionProgress}'s
     * own retire-on-release check (while still
     * {@link de.mkoehler.launchwindow.model.AssignmentState#ASSIGNED} to
     * the mission that just released them).
     */
    RETIRED,

    /**
     * Permanently lost - a mission step this astronaut was assigned to
     * resolved to {@link MissionStepOutcome#CATASTROPHIC}. See
     * {@link MissionProgress}.
     */
    LOST_IN_ACCIDENT
}
