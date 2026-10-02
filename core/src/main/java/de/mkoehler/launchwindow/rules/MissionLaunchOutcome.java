package de.mkoehler.launchwindow.rules;

/**
 * <p>What happened when {@link MissionLaunch#launch} was called. See
 * {@code design.md} &sect;2.5(f).</p>
 *
 * @author Mario Koehler
 */
public enum MissionLaunchOutcome {

    /** The mission was launched. */
    LAUNCHED,

    /** The given mission configuration id is not known to the content catalog. */
    UNKNOWN_MISSION_CONFIGURATION,

    /** The mission configuration's own program has not been opened by this player. */
    PROGRAM_NOT_OPEN,

    /** The player's current funds are below the mission configuration's own {@code cost}. */
    INSUFFICIENT_FUNDS,

    /** A given astronaut id is not on this player's astronaut roster. */
    UNKNOWN_ASTRONAUT,

    /** A given astronaut id is on the roster, but not currently {@code IDLE}. */
    ASTRONAUT_NOT_IDLE,

    /** The same astronaut id was given more than once. */
    DUPLICATE_ASTRONAUT,

    /**
     * The number of astronaut ids given does not exactly match the
     * mission configuration's own {@code requiredAstronautCount}.
     */
    WRONG_ASTRONAUT_COUNT,

    /** A given Mission Control officer id is not on this player's roster. */
    UNKNOWN_MISSION_CONTROL_OFFICER,

    /** A given Mission Control officer id is on the roster, but not currently {@code IDLE}. */
    MISSION_CONTROL_OFFICER_NOT_IDLE,

    /** The same Mission Control officer id was given more than once. */
    DUPLICATE_MISSION_CONTROL_OFFICER,

    /**
     * The number of Mission Control officer ids given does not exactly
     * match the mission configuration's own
     * {@code requiredMissionControlOfficerCount}.
     */
    WRONG_MISSION_CONTROL_OFFICER_COUNT
}
