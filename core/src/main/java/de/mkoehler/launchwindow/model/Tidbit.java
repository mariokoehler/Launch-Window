package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * A passive find about a rival ({@code design.md} &sect;2.9, levels 2 and 3): one fact from one area.
 *
 * @param turn the game turn it was found
 * @param fact what it says
 * @author Mario Koehler
 */
public record Tidbit(int turn, IntelFact fact) {

    /**
     * Checks the fact.
     *
     * @throws NullPointerException if {@code fact} is {@code null}
     */
    public Tidbit {
        Objects.requireNonNull(fact, "fact must not be null");
    }
}
