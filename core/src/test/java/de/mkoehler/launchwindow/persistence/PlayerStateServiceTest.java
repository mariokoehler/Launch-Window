package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PlayerStateService}, covering the login flow and the
 * turn-resolution loop described in {@code design.md} &sect;3.7.</p>
 *
 * @author Mario Koehler
 */
class PlayerStateServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verifies that reading current state before any snapshot has ever
     * been saved fails clearly, rather than silently returning a default.
     */
    @Test
    void currentStateThrowsWhenNoSnapshotExistsYet(@TempDir Path playerRoot) {
        PlayerStateService service = new PlayerStateService(playerRoot, mapper);

        assertThrows(IllegalStateException.class, service::currentState);
    }

    /**
     * Verifies the login flow: a submitted action not yet resolved is
     * reflected in current state without being persisted as the new
     * snapshot.
     */
    @Test
    void currentStateReplaysTodaysUnresolvedActionsWithoutPersistingThem(@TempDir Path playerRoot)
        throws IOException {
        PlayerStateService service = new PlayerStateService(playerRoot, mapper);
        seedInitialSnapshot(playerRoot, 1_000);

        service.submitAction(new SetFundsAction(5_000));
        PlayerSnapshot current = service.currentState();

        assertEquals(5_000, current.getFunds());
        assertEquals(1_000, new PlayerSnapshotStore(playerRoot, mapper).load().orElseThrow().getFunds());
    }

    /**
     * Verifies the turn-resolution loop: resolving persists the replayed
     * result under the resolved turn's own snapshot, advances the season
     * counter, and leaves the journal empty.
     */
    @Test
    void resolveTurnPersistsReplayedStateAndAdvancesSeason(@TempDir Path playerRoot) throws IOException {
        PlayerStateService service = new PlayerStateService(playerRoot, mapper);
        seedInitialSnapshot(playerRoot, 1_000);
        service.submitAction(new SetFundsAction(5_000));

        service.resolveTurn(1);

        PlayerSnapshot resolved = new PlayerSnapshotStore(playerRoot, mapper).load().orElseThrow();
        assertEquals(5_000, resolved.getFunds());
        assertEquals(2, resolved.getSeason());
        assertTrue(new PlayerActionJournal(playerRoot, mapper).readPending().isEmpty());
    }

    /**
     * Verifies that calling {@code resolveTurn} again for an already-
     * resolved turn is a safe no-op: the snapshot is not re-replayed or
     * re-saved, and - the fix for the submit-during-retry bug - an action
     * submitted after that turn was first resolved stays pending for the
     * next turn rather than being silently archived under the old one.
     */
    @Test
    void resolvingAnAlreadyResolvedTurnAgainDoesNotChangeTheSnapshotOrArchiveNewerActions(@TempDir Path playerRoot)
        throws IOException {
        PlayerStateService service = new PlayerStateService(playerRoot, mapper);
        seedInitialSnapshot(playerRoot, 1_000);
        service.submitAction(new SetFundsAction(5_000));
        service.resolveTurn(1);

        service.submitAction(new SetFundsAction(9_999));
        service.resolveTurn(1);

        PlayerSnapshot resolved = new PlayerSnapshotStore(playerRoot, mapper).load().orElseThrow();
        assertEquals(5_000, resolved.getFunds());
        assertEquals(2, resolved.getSeason());

        List<PlayerActionJournal.PendingAction> stillPending =
            new PlayerActionJournal(playerRoot, mapper).readPending();
        assertEquals(1, stillPending.size());
        assertEquals(9_999, ((SetFundsAction) stillPending.get(0).action()).getFunds());
    }

    /**
     * Verifies that retrying an already-resolved turn still finishes
     * archiving the actions that turn actually applied - the case where a
     * previous call crashed after saving the snapshot but before archiving
     * the journal.
     */
    @Test
    void resolvingAnAlreadyResolvedTurnAgainStillArchivesTheActionsItApplied(@TempDir Path playerRoot)
        throws IOException {
        PlayerStateService service = new PlayerStateService(playerRoot, mapper);
        seedInitialSnapshot(playerRoot, 1_000);
        service.submitAction(new SetFundsAction(5_000));
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);
        String appliedFile = journal.readPending().get(0).file().getFileName().toString();
        new PlayerSnapshotStore(playerRoot, mapper)
            .save(1, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 5_000, 0, 0, 2), appliedFile);

        service.resolveTurn(1);

        assertTrue(journal.readPending().isEmpty());
    }

    /**
     * Saves a turn-0 snapshot directly, bypassing {@link PlayerStateService},
     * so a test can start from an existing player rather than exercising
     * the not-yet-built slot-claim flow.
     *
     * @param playerRoot the player's data directory
     * @param funds      the starting funds to seed
     */
    private void seedInitialSnapshot(Path playerRoot, long funds) throws IOException {
        new PlayerSnapshotStore(playerRoot, mapper)
            .save(0, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, funds, 0, 0, 1), null);
    }
}
