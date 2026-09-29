package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PlayerActionJournal}.</p>
 *
 * @author Mario Koehler
 */
class PlayerActionJournalTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verifies that a journal with nothing appended yet reads back as
     * empty rather than throwing.
     */
    @Test
    void readPendingReturnsEmptyListWhenNothingAppendedYet(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);

        assertTrue(journal.readPending().isEmpty());
    }

    /**
     * Verifies that appended actions are read back with the same field
     * values, in submission order.
     */
    @Test
    void readPendingReturnsAppendedActionsInSubmissionOrder(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);

        journal.append(new SetFundsAction(1_000));
        journal.append(new SetFundsAction(2_000));
        journal.append(new SetFundsAction(3_000));

        List<PlayerActionJournal.PendingAction> pending = journal.readPending();

        assertEquals(3, pending.size());
        assertEquals(1_000, ((SetFundsAction) pending.get(0).action()).getFunds());
        assertEquals(2_000, ((SetFundsAction) pending.get(1).action()).getFunds());
        assertEquals(3_000, ((SetFundsAction) pending.get(2).action()).getFunds());
    }

    /**
     * Verifies submission order survives even when many actions are
     * appended in a tight loop, where clock-based ordering (millisecond
     * timestamps) could plausibly tie and fall back to random ordering.
     */
    @Test
    void preservesOrderAcrossManyRapidAppends(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);

        for (int i = 0; i < 200; i++) {
            journal.append(new SetFundsAction(i));
        }

        List<PlayerActionJournal.PendingAction> pending = journal.readPending();
        assertEquals(200, pending.size());
        for (int i = 0; i < 200; i++) {
            assertEquals(i, ((SetFundsAction) pending.get(i).action()).getFunds());
        }
    }

    /**
     * Verifies that archiving moves exactly the given pending actions' files
     * into a per-turn archive subdirectory and removes them from the
     * journal.
     */
    @Test
    void archiveMovesGivenFilesOutOfTheJournal(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);
        journal.append(new SetFundsAction(1_000));
        journal.append(new SetFundsAction(2_000));
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();

        journal.archive(pending, 7);

        assertTrue(journal.readPending().isEmpty());
        Path archiveDir = playerRoot.resolve("journal-archive").resolve("7");
        try (var listing = Files.list(archiveDir)) {
            assertEquals(2, listing.count());
        }
    }

    /**
     * Verifies that an action submitted after a batch was read for
     * archiving is left pending, not silently archived along with the
     * earlier batch - the fix for the submit-during-resolve race.
     */
    @Test
    void doesNotArchiveActionsSubmittedAfterTheBatchWasRead(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);
        journal.append(new SetFundsAction(1_000));
        List<PlayerActionJournal.PendingAction> readForResolution = journal.readPending();

        journal.append(new SetFundsAction(2_000));
        journal.archive(readForResolution, 7);

        List<PlayerActionJournal.PendingAction> stillPending = journal.readPending();
        assertEquals(1, stillPending.size());
        assertEquals(2_000, ((SetFundsAction) stillPending.get(0).action()).getFunds());
    }

    /**
     * Verifies that archiving the same batch twice (simulating a retry
     * after a crash between the move and whatever called archive) does not
     * fail on the files already moved by the first attempt.
     */
    @Test
    void archivingTheSameBatchTwiceDoesNotFail(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);
        journal.append(new SetFundsAction(1_000));
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();

        journal.archive(pending, 7);
        journal.archive(pending, 7);

        Path archiveDir = playerRoot.resolve("journal-archive").resolve("7");
        try (var listing = Files.list(archiveDir)) {
            assertEquals(1, listing.count());
        }
    }

    /**
     * Verifies that a file name's sequence number is never reused after an
     * archive - a new action's sequence prefix must always sort after
     * every previously archived one, since {@code PlayerStateService}'s
     * crash-recovery cutoff relies on comparing file names as plain
     * strings.
     */
    @Test
    void sequenceNumberIsNeverReusedAcrossAnArchive(@TempDir Path playerRoot) throws IOException {
        PlayerActionJournal journal = new PlayerActionJournal(playerRoot, mapper);
        journal.append(new SetFundsAction(1_000));
        String firstFileName = journal.readPending().get(0).file().getFileName().toString();
        journal.archive(journal.readPending(), 1);

        journal.append(new SetFundsAction(2_000));
        String secondFileName = journal.readPending().get(0).file().getFileName().toString();

        assertTrue(secondFileName.compareTo(firstFileName) > 0,
            "a file name submitted after an archive must sort after every previously archived one");
    }
}
