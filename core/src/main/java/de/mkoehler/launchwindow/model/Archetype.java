package de.mkoehler.launchwindow.model;

/**
 * <p>The agency archetype a player picks when claiming a slot in a game.
 * Archetypes are non-exclusive - several players may share one - and gate
 * which programs/mission components a player can access, while otherwise
 * using an identical economy. See {@code design.md} &sect;2.1(a1)/(c1).</p>
 *
 * @author Mario Koehler
 */
public enum Archetype {

    /**
     * The NASA-style archetype.
     */
    NASA,

    /**
     * The Soviet-style archetype.
     */
    SOVIET
}
