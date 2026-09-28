package de.mkoehler.launchwindow.model;

/**
 * <p>The five stored competencies tracked for a {@link MissionControlOfficer}.</p>
 *
 * <p>These map onto real NASA flight-control disciplines - see
 * {@code baspm.md} &sect;9.1 and {@code design.md} &sect;1.1. A sixth,
 * "Flight Director" competency exists conceptually but is deliberately not a
 * member of this enum: it has no independently stored value and is instead
 * computed as the average of these five, via
 * {@link MissionControlOfficer#flightDirectorSkill()}.</p>
 *
 * @author Mario Koehler
 */
public enum MissionControlSkill {

    /**
     * Launch vehicle propulsion systems.
     */
    PROPULSION,

    /**
     * Trajectory planning and guidance/navigation/control.
     */
    TRAJECTORY_AND_GNC,

    /**
     * Spacecraft bus and onboard systems.
     */
    SPACECRAFT_SYSTEMS,

    /**
     * Crew life support systems and payload operations.
     */
    CREW_SYSTEMS_AND_PAYLOADS,

    /**
     * Overall mission operations and flight planning.
     */
    MISSION_OPERATIONS
}
