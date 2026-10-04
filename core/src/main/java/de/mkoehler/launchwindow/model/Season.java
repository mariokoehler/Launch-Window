package de.mkoehler.launchwindow.model;

import java.util.Locale;

/**
 * The four seasons of the game calendar, in calendar order within a year. A game year starts in
 * winter, as BASPM's does ({@code baspm.md} &sect;2.5). The lower-cased names
 * match the season part of the hub art's file names ({@code ..._fall_noon.png}).
 *
 * @author Mario Koehler
 */
public enum Season {

    /** The first season of a year. */
    WINTER,
    /** The second season of a year. */
    SPRING,
    /** The third season of a year. */
    SUMMER,
    /** The last season of a year. */
    FALL;

    /**
     * Returns the season's name as shown to the player.
     *
     * @return "Winter", "Spring", "Summer" or "Fall"
     */
    public String label() {
        String lower = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
