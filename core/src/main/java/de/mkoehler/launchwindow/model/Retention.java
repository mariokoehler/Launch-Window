package de.mkoehler.launchwindow.model;

/**
 * How an agency answers a rival's offer to one of its people ({@code design.md} &sect;2.2(d),
 * poaching): a standing order per person, so nobody has to be online to react.
 *
 * @author Mario Koehler
 */
public enum Retention {
    /** Match offers up to the agency's default for the person's group. */
    DEFAULT,
    /** Match any offer. */
    KEEP,
    /** Never match. */
    LET_GO
}
