package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * Reports that the government answered rivals' Firsts ({@link RivalFirsts}, {@code design.md}
 * &sect;2.1(e)): the next funding review counts extra prestige.
 *
 * @param goalIds     the Firsts answered, settled by other agencies last turn
 * @param credit      the prestige they add to the next review
 * @param creditTotal the prestige the next review adds in all, this included
 * @author Mario Koehler
 */
public record RivalFirstsAnswered(List<String> goalIds, long credit, long creditTotal) implements ResolutionEvent {

    /**
     * Copies the goals.
     *
     * @throws NullPointerException if {@code goalIds} is {@code null}
     */
    public RivalFirstsAnswered {
        goalIds = List.copyOf(Objects.requireNonNull(goalIds, "goalIds must not be null"));
    }
}
