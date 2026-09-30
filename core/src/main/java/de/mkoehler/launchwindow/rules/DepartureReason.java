package de.mkoehler.launchwindow.rules;

/**
 * <p>Why a {@link PersonnelDeparture} happened. See {@code design.md}
 * &sect;2.2(a)/(c) - both are automatic season-advance outcomes, checked
 * against {@link PersonnelLifecycle#isProtectedFromDeparture} rather than
 * unconditionally every season (see that method's own Javadoc for the
 * per-group protection rule - it is not simply "while idle"); dismissal (a
 * genuine player action) is not part of this enum since
 * {@link PersonnelLifecycle} never triggers it.</p>
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
     * Age reached the employee's group's retirement age while not
     * protected from departure.
     */
    RETIRED
}
