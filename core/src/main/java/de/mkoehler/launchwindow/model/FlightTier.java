package de.mkoehler.launchwindow.model;

/**
 * <p>Which repeat tier a mission configuration's prestige is paid at, read
 * from how many times the player has already completed that configuration
 * successfully (see {@code design.md} &sect;2.6(d) and
 * {@code baspm.md} &sect;11.7). Read live, at each season's own resolution -
 * never stored on the mission, so two copies of one configuration that end in
 * the same pass correctly see each other's completions.</p>
 *
 * <p>Not carried over the wire: this is computed, never stored in
 * {@code PlayerSnapshot}, so it isn't registered in {@code MessageRegistry}.</p>
 *
 * @author Mario Koehler
 */
public enum FlightTier {
    /** The configuration has never been completed successfully by this player. */
    FIRST_TIME,
    /** Completed successfully exactly once before. */
    SECOND_TIME,
    /** Completed successfully two or more times before. */
    LATER_TIME
}
