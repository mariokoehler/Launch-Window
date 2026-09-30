package de.mkoehler.launchwindow.rules;

/**
 * <p>The result of a {@link HiringPool#hire} attempt. A hire can fail for
 * ordinary, expected reasons in an async multiplayer game - a candidate ID
 * from a stale view of the pool (someone else already hired them, or a
 * season-turnover refresh replaced the batch since the player last looked),
 * or a full roster - so this is a plain result, not an exception.</p>
 *
 * @author Mario Koehler
 */
public enum HireOutcome {

    /**
     * The candidate was found, the roster had room, and they moved from
     * the candidate pool to the roster.
     */
    HIRED,

    /**
     * No candidate with that id exists in that group's current pool - a
     * stale id, most likely from a pool that has since been refreshed.
     */
    UNKNOWN_CANDIDATE,

    /**
     * The candidate exists, but that group's roster is already at its
     * capacity.
     */
    ROSTER_FULL
}
