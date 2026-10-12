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
     * The crew slots are filled wrongly: a crew slot some step uses is empty, a crew
     * slot no step uses holds someone, or more ids were given than the layout has
     * seats ({@code baspm.md} &sect;10.11).
     */
    WRONG_ASTRONAUT_COUNT,

    /**
     * A flight-controller slot holds an id that isn't on this player's Mission Control
     * roster - an astronaut in a controller slot lands here too.
     */
    UNKNOWN_MISSION_CONTROL_OFFICER,

    /** A given Mission Control officer id is on the roster, but not currently {@code IDLE}. */
    MISSION_CONTROL_OFFICER_NOT_IDLE,

    /** The same Mission Control officer id was given more than once. */
    DUPLICATE_MISSION_CONTROL_OFFICER,

    /**
     * The flight-controller slots are filled wrongly: a used one is empty, an unused
     * one holds someone, or more ids were given than the layout has control slots.
     */
    WRONG_MISSION_CONTROL_OFFICER_COUNT,

    /**
     * The launch window already holds as many launches as the random-event cap allows
     * ({@code design.md} &sect;2.7).
     */
    LAUNCH_CAP_REACHED,

    /**
     * One of the configuration's mission components is unavailable for this window ({@code design.md} &sect;2.7).
     */
    MISSION_COMPONENT_UNAVAILABLE,

    /** The number of rockets given doesn't match the configuration's launches. */
    WRONG_ROCKET_COUNT,

    /** A given rocket isn't a rocket the player owns with its rocket program open. */
    ROCKET_NOT_AVAILABLE,

    /**
     * A given rocket can't lift its launch's payload to its destination, or isn't
     * human-rated for a crewed launch ({@code baspm.md} &sect;10.9).
     */
    ROCKET_CANNOT_LIFT_PAYLOAD,

    /**
     * The astronaut-type control slots (CAPCOM) are filled wrongly: a used one is empty,
     * or an unused one holds someone.
     */
    WRONG_CAPCOM_COUNT,

    /**
     * A CAPCOM slot holds someone who isn't an astronaut on the roster, or who is not
     * idle, is unavailable, is repeated, or is also in the crew.
     */
    CAPCOM_NOT_AVAILABLE,

    /**
     * The configuration needs a goal the player hasn't achieved or cancelled yet
     * ({@link de.mkoehler.launchwindow.content.MissionConfiguration#requiredGoalIds()}).
     */
    GOAL_NOT_REACHED,

    /**
     * A crewed flight while the agency's crewed flights are grounded after a crew was lost
     * ({@link CrewedGrounding}). Appended last: Kryo sends enums by ordinal.
     */
    CREWED_FLIGHTS_GROUNDED
}
