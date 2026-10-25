package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * <p>The saved snapshot's format version and the steps that bring an older file up to it
 * (review.md 2.1). A game runs for months, and saved state is read strictly: a field renamed,
 * removed or retyped, or an enum constant renamed, would make every player's file unreadable. Once a
 * game is live, such a change comes with a step here, which rewrites the older JSON before it binds,
 * and the saved-format fixture test proves an older game still loads and resolves.</p>
 *
 * <p>Adding a field needs no step: an older file lacks it and the default applies.</p>
 *
 * @author Mario Koehler
 */
public final class SnapshotMigrations {

    /** The format {@link ResolvedSnapshot} is written in. Files from before versioning count as 1. */
    public static final int CURRENT = 1;

    /** Every step, in order: none yet. */
    static final List<Step> STEPS = List.of();

    /**
     * One step: rewrites an envelope in format {@code from} into format {@code from + 1}.
     *
     * @param from   the format the step reads
     * @param change the rewrite, given the whole envelope ({@code snapshot} inside it); it may change it
     *               in place and return it
     */
    record Step(int from, UnaryOperator<ObjectNode> change) {

        Step {
            Objects.requireNonNull(change, "change must not be null");
        }
    }

    private SnapshotMigrations() {
    }

    /**
     * Brings a saved envelope up to {@link #CURRENT}.
     *
     * @param envelope the file's JSON
     * @return the envelope in the current format
     * @throws IllegalStateException if the file is newer than this build, or a step is missing
     */
    public static ObjectNode migrate(ObjectNode envelope) {
        return migrate(envelope, STEPS, CURRENT);
    }

    /**
     * Brings an envelope up to a format through the given steps.
     *
     * @param envelope the JSON
     * @param steps    the steps, in order
     * @param current  the format to reach
     * @return the envelope in that format
     */
    static ObjectNode migrate(ObjectNode envelope, List<Step> steps, int current) {
        int version = envelope.path("formatVersion").asInt(1);
        if (version > current) {
            throw new IllegalStateException("Saved in format " + version + ", newer than this build's " + current
                + ": update the server.");
        }
        while (version < current) {
            int from = version;
            Step step = steps.stream().filter(candidate -> candidate.from() == from).findFirst()
                .orElseThrow(() -> new IllegalStateException("No migration from saved format " + from));
            envelope = step.change().apply(envelope);
            version++;
        }
        envelope.put("formatVersion", current);
        return envelope;
    }
}
