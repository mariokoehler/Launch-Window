package de.mkoehler.launchwindow.model;

/**
 * The areas an agency assessment looks at, one per action ({@code design.md} &sect;2.9,
 * Intelligence).
 *
 * @author Mario Koehler
 */
public enum IntelArea {

    /** The rival's components, reliability as ranges. */
    RND,

    /** Open programs, missions in flight, R&amp;D focus, hardware not yet flown, and their Intelligence level. */
    PLANS,

    /** Roster sizes and average skill per group. */
    PERSONNEL
}
