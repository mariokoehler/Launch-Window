package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.server.GameServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link SchedulerStateStore}.</p>
 *
 * @author Mario Koehler
 */
class SchedulerStateStoreTest {

    private final ObjectMapper mapper = GameServer.createMapper();

    /**
     * Verifies that loading a store with nothing saved yet returns an empty
     * result rather than throwing.
     */
    @Test
    void loadReturnsEmptyWhenNoStateExistsYet(@TempDir Path dataRoot) throws IOException {
        SchedulerStateStore store = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);

        assertTrue(store.load().isEmpty());
    }

    /**
     * Verifies that a saved state can be loaded back with the same field
     * values.
     */
    @Test
    void saveThenLoadRoundTripsTheState(@TempDir Path dataRoot) throws IOException {
        SchedulerStateStore store = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);
        SchedulerState original = new SchedulerState();
        original.setNextGameTurn(7);
        original.setLastResolvedDate(LocalDate.of(2026, 9, 28));

        store.save(original);
        Optional<SchedulerState> loaded = store.load();

        assertTrue(loaded.isPresent());
        assertEquals(7, loaded.get().getNextGameTurn());
        assertEquals(LocalDate.of(2026, 9, 28), loaded.get().getLastResolvedDate());
    }

    /**
     * Verifies that a later save replaces the earlier one rather than
     * merging with it.
     */
    @Test
    void saveReplacesThePreviouslySavedState(@TempDir Path dataRoot) throws IOException {
        SchedulerStateStore store = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);
        SchedulerState first = new SchedulerState();
        first.setNextGameTurn(1);
        store.save(first);
        SchedulerState second = new SchedulerState();
        second.setNextGameTurn(2);
        second.setLastResolvedDate(LocalDate.of(2026, 9, 29));

        store.save(second);

        assertEquals(2, store.load().get().getNextGameTurn());
    }
}
