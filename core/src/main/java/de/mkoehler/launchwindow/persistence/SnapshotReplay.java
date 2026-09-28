package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * <p>Applies a sequence of {@link PlayerAction}s onto a copy of a base
 * {@link PlayerSnapshot}, without mutating the original - the mechanism
 * both the login flow (replay today's journal so far) and the turn-
 * resolution loop (replay a full day's journal) rely on (see
 * {@code design.md} &sect;3.7).</p>
 *
 * @author Mario Koehler
 */
public class SnapshotReplay {

    private final ObjectMapper mapper;

    /**
     * Creates a new replay helper using the given mapper to copy snapshots.
     *
     * @param mapper the Jackson mapper used to deep-copy a snapshot before
     *               replaying actions onto it
     */
    public SnapshotReplay(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Returns a new snapshot equal to {@code base} with every action in
     * {@code actions} applied to it in order. {@code base} itself is left
     * unmodified.
     *
     * @param base    the snapshot to start from
     * @param actions the actions to apply, in the order they should be
     *                applied
     * @return the resulting snapshot
     */
    public PlayerSnapshot replay(PlayerSnapshot base, List<PlayerAction> actions) {
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(actions, "actions must not be null");

        PlayerSnapshot result = copy(base);
        for (PlayerAction action : actions) {
            action.applyTo(result);
        }
        return result;
    }

    /**
     * Deep-copies a snapshot via a Jackson serialize/deserialize round
     * trip, so that {@link PlayerSnapshot}'s own persistence format is
     * always the single source of truth for what "a copy" means - a field
     * added to the snapshot later needs no matching change here.
     *
     * @param snapshot the snapshot to copy
     * @return an independent copy of {@code snapshot}
     */
    private PlayerSnapshot copy(PlayerSnapshot snapshot) {
        try {
            return mapper.readValue(mapper.writeValueAsString(snapshot), PlayerSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
