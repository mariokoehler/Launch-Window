package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>Reads and writes {@link SchedulerState} as a single flat JSON file
 * (see {@code scheduler-state.json} under a server's data root). Writes are
 * atomic: the new content is written to a temporary file in the same
 * directory, then renamed into place, matching every other flat-file store
 * in this project (see {@code design.md} &sect;3.7's crash-robustness
 * requirement).</p>
 *
 * @author Mario Koehler
 */
public class SchedulerStateStore {

    private final Path stateFile;
    private final ObjectMapper mapper;

    /**
     * Creates a new store backed by the given file.
     *
     * @param stateFile the path to {@code scheduler-state.json}
     * @param mapper    the Jackson mapper to read/write it with
     */
    public SchedulerStateStore(Path stateFile, ObjectMapper mapper) {
        this.stateFile = Objects.requireNonNull(stateFile, "stateFile must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Loads the stored state.
     *
     * @return the stored state, or {@link Optional#empty()} if no state has
     *     been saved yet
     * @throws IOException if the file exists but cannot be read
     */
    public Optional<SchedulerState> load() throws IOException {
        if (!Files.exists(stateFile)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(stateFile.toFile(), SchedulerState.class));
    }

    /**
     * Atomically saves the given state, replacing whatever was saved
     * before.
     *
     * @param state the state to save
     * @throws IOException if the write fails
     */
    public void save(SchedulerState state) throws IOException {
        Objects.requireNonNull(state, "state must not be null");
        Files.createDirectories(stateFile.getParent());
        Path tempFile = Files.createTempFile(stateFile.getParent(), "scheduler-state-", ".json.tmp");
        try {
            mapper.writeValue(tempFile.toFile(), state);
            AtomicFiles.moveReplacingWithRetry(tempFile, stateFile);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }
}
