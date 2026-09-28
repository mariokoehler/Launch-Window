package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * <p>Ties {@link PlayerSnapshotStore}, {@link PlayerActionJournal} and
 * {@link SnapshotReplay} together into the two operations
 * {@code design.md} &sect;3.7 describes: the login flow (replay today's
 * journal so far onto the last snapshot) and the turn-resolution loop
 * (replay the full day's journal, then persist the result as the next
 * snapshot).</p>
 *
 * <p>{@link #resolveTurn(long)} applies declared actions and advances the
 * season counter; it runs no simulation logic.</p>
 *
 * @author Mario Koehler
 */
public class PlayerStateService {

    private final PlayerSnapshotStore snapshotStore;
    private final PlayerActionJournal journal;
    private final SnapshotReplay replay;

    /**
     * Creates a new state service for one player, rooted at the given
     * directory.
     *
     * @param playerRoot the player's own data directory
     * @param mapper     the Jackson mapper used by every underlying store
     */
    public PlayerStateService(Path playerRoot, ObjectMapper mapper) {
        Objects.requireNonNull(playerRoot, "playerRoot must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        this.snapshotStore = new PlayerSnapshotStore(playerRoot, mapper);
        this.journal = new PlayerActionJournal(playerRoot, mapper);
        this.replay = new SnapshotReplay(mapper);
    }

    /**
     * Returns the player's current state: their last resolved snapshot with
     * today's not-yet-resolved actions replayed on top. Safe to call
     * repeatedly within the same day - an empty journal simply replays
     * nothing.
     *
     * @return the player's current state
     * @throws IOException           if the snapshot or journal cannot be read
     * @throws IllegalStateException if this player has no saved snapshot yet
     */
    public PlayerSnapshot currentState() throws IOException {
        PlayerSnapshot base = loadLatestSnapshot();
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();
        return replay.replay(base, pending.stream().map(PlayerActionJournal.PendingAction::action).toList());
    }

    /**
     * Appends a new action to the player's journal for the current,
     * not-yet-resolved day.
     *
     * @param action the action to submit
     * @throws IOException if the action cannot be written
     */
    public void submitAction(PlayerAction action) throws IOException {
        journal.append(action);
    }

    /**
     * Resolves the given game turn for this player: replays exactly the
     * actions pending at the moment this call reads the journal (not
     * whatever happens to be pending when the archive step runs, so an
     * action submitted mid-resolution is never silently dropped) onto
     * their last snapshot, advances their personal season counter by one,
     * persists the result as turn {@code gameTurn}'s snapshot together with
     * a cutoff marking the last action applied, and archives the actions
     * that were just applied.
     *
     * <p>Idempotent: if turn {@code gameTurn} (or a later one) has already
     * been saved - e.g. a previous call crashed after saving the snapshot
     * but before archiving the journal - this call does not re-replay or
     * re-save. It only finishes archiving, using the saved cutoff to
     * archive exactly the actions that earlier call actually applied -
     * never a later action submitted during the crash window, which stays
     * pending for the next turn instead. Safe to retry after a crash at
     * any point.</p>
     *
     * @param gameTurn the game turn being resolved
     * @throws IOException           if any read/write step fails
     * @throws IllegalStateException if this player has no saved snapshot yet
     */
    public void resolveTurn(long gameTurn) throws IOException {
        OptionalLong latestTurn = snapshotStore.latestTurnNumber();
        boolean alreadyResolved = latestTurn.isPresent() && latestTurn.getAsLong() >= gameTurn;

        if (alreadyResolved) {
            finishArchivingUpToCutoff(gameTurn);
            return;
        }

        PlayerSnapshot base = loadLatestSnapshot();
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();
        PlayerSnapshot resolved =
            replay.replay(base, pending.stream().map(PlayerActionJournal.PendingAction::action).toList());
        resolved.setSeason(resolved.getSeason() + 1);
        String cutoff = pending.isEmpty() ? null : lastFileName(pending);
        snapshotStore.save(gameTurn, resolved, cutoff);
        journal.archive(pending, gameTurn);
    }

    /**
     * Finishes archiving a turn that was already resolved (its snapshot
     * exists) but whose journal may not have been archived yet, using that
     * turn's saved cutoff to archive only the actions it actually applied.
     *
     * @param gameTurn the already-resolved turn to finish archiving
     * @throws IOException if the cutoff or journal cannot be read, or a
     *                      file cannot be moved
     */
    private void finishArchivingUpToCutoff(long gameTurn) throws IOException {
        Optional<String> cutoff = snapshotStore.lastAppliedActionFile(gameTurn);
        if (cutoff.isEmpty()) {
            return;
        }
        List<PlayerActionJournal.PendingAction> toArchive = journal.readPending().stream()
            .filter(pending -> pending.file().getFileName().toString().compareTo(cutoff.get()) <= 0)
            .toList();
        journal.archive(toArchive, gameTurn);
    }

    /**
     * Returns the journal file name of the last entry in a list of pending
     * actions read in submission order.
     *
     * @param pending the pending actions, oldest first
     * @return the last entry's file name
     */
    private String lastFileName(List<PlayerActionJournal.PendingAction> pending) {
        return pending.get(pending.size() - 1).file().getFileName().toString();
    }

    /**
     * Loads this player's most recently resolved snapshot.
     *
     * @return the latest snapshot
     * @throws IOException           if the snapshot cannot be read
     * @throws IllegalStateException if this player has no saved snapshot yet
     */
    private PlayerSnapshot loadLatestSnapshot() throws IOException {
        return snapshotStore.load()
            .orElseThrow(() -> new IllegalStateException("no snapshot exists for this player yet"));
    }
}
