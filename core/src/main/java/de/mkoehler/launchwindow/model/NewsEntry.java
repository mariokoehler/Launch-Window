package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * <p>One entry in a player's news log: a random event that touched them, stored
 * as structured data only. The client renders its own wording from these fields;
 * no text is stored here ({@code design.md} &sect;2.7).</p>
 *
 * @param turn      the game turn the event resolved on
 * @param kind      which event this was
 * @param subjectId the affected personnel, component, or mission id, or {@code null} when the event
 *                  has no single subject
 * @param magnitude the rolled magnitude: a percentage, a points figure, a season count, or a count of
 *                  affected things, depending on {@code kind}
 * @author Mario Koehler
 */
public record NewsEntry(int turn, RandomEventKind kind, String subjectId, double magnitude) {

    /**
     * Validates the kind.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public NewsEntry {
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
