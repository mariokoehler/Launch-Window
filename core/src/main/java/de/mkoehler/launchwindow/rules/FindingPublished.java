package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * Reports that this agency published a finding ({@link Findings}, {@code design.md} &sect;2.13): the
 * rivals it helped, and the prestige it paid.
 *
 * @param goalId       the finding
 * @param prestige     the prestige it paid this agency
 * @param helped       the rivals that didn't know it yet
 * @param rivals       every rival in the game
 * @param coPublishers the other agencies that published it at the same resolution, splitting the payment
 * @author Mario Koehler
 */
public record FindingPublished(String goalId, long prestige, int helped, int rivals, List<String> coPublishers)
        implements ResolutionEvent {

    /**
     * Copies the co-publishers.
     *
     * @throws NullPointerException if {@code goalId} is {@code null}
     */
    public FindingPublished {
        Objects.requireNonNull(goalId, "goalId must not be null");
        coPublishers = coPublishers == null ? List.of() : List.copyOf(coPublishers);
    }
}
