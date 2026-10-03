package de.mkoehler.launchwindow.content;

/**
 * <p>A rocket's size class. The Vehicle Assembly Building's level sets the largest
 * class a player can open: level 1 Light, level 2 Medium, level 3 Heavy
 * ({@code baspm.md} &sect;10.9). Declared smallest first, so a class is allowed
 * when its ordinal is below the VAB level.</p>
 *
 * @author Mario Koehler
 */
public enum RocketClass {

    /** Opened from VAB level 1. */
    LIGHT,
    /** Opened from VAB level 2. */
    MEDIUM,
    /** Opened from VAB level 3. */
    HEAVY
}
