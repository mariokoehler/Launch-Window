package de.mkoehler.launchwindow.rules;

/**
 * <p>Why a {@link PersonnelDeparture} happened. See {@code design.md}
 * &sect;2.2(a)/(c) - both are automatic season-advance outcomes, checked
 * only while the employee is idle; dismissal (a genuine player action) is
 * not part of this enum since {@link PersonnelLifecycle} never triggers
 * it.</p>
 *
 * @author Mario Koehler
 */
public enum DepartureReason {

    /**
     * Morale fell to or below {@link PersonnelLifecycle#RESIGNATION_MORALE_FLOOR}
     * while idle.
     */
    RESIGNED,

    /**
     * Age reached the employee's group's retirement age while idle.
     */
    RETIRED
}
