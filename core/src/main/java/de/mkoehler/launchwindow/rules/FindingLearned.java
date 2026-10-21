package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * Reports that other agencies published a finding ({@link Findings}, {@code design.md} &sect;2.13):
 * from now on this agency's matching steps are safer, unless it knew the finding already.
 *
 * @param goalId     the finding
 * @param publishers the agencies that published it
 * @param knewIt     whether this agency knew it already
 * @author Mario Koehler
 */
public record FindingLearned(String goalId, List<String> publishers, boolean knewIt) implements ResolutionEvent {

    /**
     * Copies the publishers.
     *
     * @throws NullPointerException if {@code goalId} is {@code null}
     */
    public FindingLearned {
        Objects.requireNonNull(goalId, "goalId must not be null");
        publishers = publishers == null ? List.of() : List.copyOf(publishers);
    }
}
