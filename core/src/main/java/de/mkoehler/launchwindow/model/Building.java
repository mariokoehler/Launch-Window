package de.mkoehler.launchwindow.model;

/**
 * <p>The five space-complex buildings a player owns, each upgradable in levels
 * that raise a cap. Every player starts with all five at level 1; there is no
 * construction step. See {@code design.md} &sect;5 (M3 decisions) and
 * {@code baspm.md} &sect;10.9.</p>
 *
 * @author Mario Koehler
 */
public enum Building {

    /** Caps the astronaut roster. */
    ASTRONAUT_CENTER,

    /** Caps the Mission Control roster. */
    MISSION_CONTROL,

    /** Caps the SET roster. */
    SET_CENTER,

    /** Caps how many payload programs can be open at once. */
    HEADQUARTERS,

    /** Caps how many rocket programs can be open at once, and the largest rocket class. */
    VEHICLE_ASSEMBLY
}
