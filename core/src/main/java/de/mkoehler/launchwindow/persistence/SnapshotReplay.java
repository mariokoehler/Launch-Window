package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Replays a player's pending actions onto a copy of their last resolved snapshot
 * ({@code design.md} &sect;3.7). Each action runs through its validated rules
 * method, in journal order; one that no longer takes effect changes nothing and is
 * reported, rather than stopping the replay. Replay is deterministic: the same
 * snapshot, actions, and context always give the same state.</p>
 *
 * @author Mario Koehler
 */
public class SnapshotReplay {

    private final ObjectMapper mapper;

    /**
     * Creates a replayer that deep-copies snapshots through the given mapper.
     *
     * @param mapper the mapper snapshots are persisted with
     */
    public SnapshotReplay(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * The state after a replay, and what each action did.
     *
     * @param state   the resulting state
     * @param results one result per action, in order
     */
    public record Replayed(PlayerSnapshot state, List<ActionResult> results) {
    }

    /**
     * Replays actions onto a copy of {@code base}, leaving {@code base} untouched.
     *
     * @param base    the last resolved snapshot
     * @param actions the pending actions, in journal order
     * @param context the game's content and budget schedule
     * @return the resulting state and each action's result
     */
    public Replayed replay(PlayerSnapshot base, List<PlayerAction> actions, ActionContext context) {
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(actions, "actions must not be null");
        Objects.requireNonNull(context, "context must not be null");
        PlayerSnapshot result = copy(base);
        List<ActionResult> results = new ArrayList<>();
        for (PlayerAction action : actions) {
            // Each action runs on its own copy, kept only if it takes effect, so even an action that
            // throws halfway can never leave a partial change behind.
            PlayerSnapshot candidate = copy(result);
            ActionResult actionResult = applySafely(action, candidate, context);
            if (actionResult.applied()) {
                result = candidate;
            }
            results.add(actionResult);
        }
        return new Replayed(result, results);
    }

    /**
     * Applies one action, treating a malformed one (a missing id, an unknown
     * enum) as an action that simply doesn't take effect. The rules methods
     * change nothing before they reject, so a rejected action leaves the state as
     * it was.
     *
     * @param action   the action
     * @param snapshot the state; mutated only if the action takes effect
     * @param context  the game's content and budget schedule
     * @return the action's result
     */
    public static ActionResult applySafely(PlayerAction action, PlayerSnapshot snapshot, ActionContext context) {
        try {
            ActionResult result = action.apply(snapshot, context);
            if (result.applied()) {
                // The player acted: decisions may come again after a lapse (ui-spec.md component 26).
                snapshot.setDecisionPaused(false);
            }
            return result;
        } catch (NullPointerException | IllegalArgumentException e) {
            return new ActionResult(false, "INVALID_ACTION");
        }
    }

    /**
     * Deep-copies a snapshot through JSON.
     *
     * @param snapshot the snapshot
     * @return an independent copy
     */
    public PlayerSnapshot copy(PlayerSnapshot snapshot) {
        try {
            return mapper.readValue(mapper.writeValueAsString(snapshot), PlayerSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
