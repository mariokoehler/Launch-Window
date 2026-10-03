package de.mkoehler.launchwindow.rules;

/**
 * The result of {@link RocketPrograms#open} or {@link RocketPrograms#close}. Every
 * outcome but {@link #OPENED} and {@link #CLOSED} leaves the player unchanged.
 *
 * @author Mario Koehler
 */
public enum RocketProgramOutcome {

    /** The rocket program opened: its creation cost was paid and the rocket is active. */
    OPENED,

    /** The rocket program closed: the rocket went inactive, keeping its reliability. */
    CLOSED,

    /** The id isn't a rocket in the content catalog. */
    UNKNOWN_ROCKET,

    /** The rocket belongs to another agency. */
    WRONG_AGENCY,

    /** The rocket program is already open. */
    ALREADY_OPEN,

    /** The rocket program isn't open. */
    NOT_OPEN,

    /** As many rocket programs are open as the VAB's level allows. */
    VAB_FULL,

    /** The rocket's class is larger than the VAB's level supports. */
    ROCKET_CLASS_TOO_LARGE,

    /** The player can't afford the rocket's creation cost. */
    INSUFFICIENT_FUNDS,

    /** A mission in flight is using the rocket. */
    MISSIONS_IN_FLIGHT
}
