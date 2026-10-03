package de.mkoehler.launchwindow.rules;

/**
 * The result of {@link ProgramOpening#close}. Every outcome but {@link #CLOSED}
 * leaves the player unchanged.
 *
 * @author Mario Koehler
 */
public enum CloseProgramOutcome {

    /** The program was closed, and components no open program needs went inactive. */
    CLOSED,

    /** The program isn't open. */
    NOT_OPEN,

    /** One of the program's missions is still in flight. */
    MISSIONS_IN_FLIGHT
}
