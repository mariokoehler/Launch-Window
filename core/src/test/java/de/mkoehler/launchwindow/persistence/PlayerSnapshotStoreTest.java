package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PlayerSnapshotStore}.</p>
 *
 * @author Mario Koehler
 */
class PlayerSnapshotStoreTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verifies that loading a store with no saved snapshot yet returns an
     * empty result rather than throwing.
     */
    @Test
    void loadReturnsEmptyWhenNoSnapshotExistsYet(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);

        assertTrue(store.load().isEmpty());
    }

    /**
     * Verifies that a saved snapshot can be loaded back with the same
     * field values.
     */
    @Test
    void saveThenLoadRoundTripsTheSnapshot(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        PlayerSnapshot original = new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 4_000, 10, 500, 2);

        store.save(1, original, "00000000000000000001-abc.json");
        Optional<PlayerSnapshot> loaded = store.load();

        assertTrue(loaded.isPresent());
        assertEquals(original.getAgencyName(), loaded.get().getAgencyName());
        assertEquals(original.getArchetype(), loaded.get().getArchetype());
        assertEquals(original.getFunds(), loaded.get().getFunds());
        assertEquals(original.getSeason(), loaded.get().getSeason());
    }

    /**
     * Verifies that {@code load()} with no argument returns the
     * highest-numbered turn's snapshot, not just the most recently saved
     * one by wall-clock time.
     */
    @Test
    void loadWithoutArgumentReturnsTheHighestNumberedTurn(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        store.save(5, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 5_000, 0, 0, 5), null);
        store.save(2, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 2_000, 0, 0, 2), null);

        Optional<PlayerSnapshot> loaded = store.load();

        assertEquals(5_000, loaded.orElseThrow().getFunds());
    }

    /**
     * Verifies that saving the same turn number twice keeps the first
     * write rather than overwriting it - turns are immutable once saved,
     * and this is also what makes a crashed-and-retried resolution safe.
     */
    @Test
    void savingTheSameTurnTwiceKeepsTheFirstWrite(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        store.save(1, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1), null);

        store.save(1, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 9_999, 0, 0, 1), null);

        assertEquals(1_000, store.load(1).orElseThrow().getFunds());
    }

    /**
     * Verifies that {@code latestTurnNumber} reports the highest turn a
     * snapshot has been saved for.
     */
    @Test
    void latestTurnNumberReportsTheHighestSavedTurn(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        store.save(3, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 3), null);
        store.save(9, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 9), null);

        OptionalLong latest = store.latestTurnNumber();

        assertEquals(9, latest.orElseThrow());
    }

    /**
     * Verifies that the cutoff saved alongside a turn's snapshot is read
     * back unchanged.
     */
    @Test
    void lastAppliedActionFileReturnsTheSavedCutoff(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        store.save(1, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1),
            "00000000000000000001-abc.json");

        Optional<String> cutoff = store.lastAppliedActionFile(1);

        assertEquals("00000000000000000001-abc.json", cutoff.orElseThrow());
    }

    /**
     * Verifies that a turn saved with no actions pending has no cutoff.
     */
    @Test
    void lastAppliedActionFileIsEmptyWhenTurnHadNoPendingActions(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot, mapper);
        store.save(1, new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1), null);

        Optional<String> cutoff = store.lastAppliedActionFile(1);

        assertTrue(cutoff.isEmpty());
    }
}
