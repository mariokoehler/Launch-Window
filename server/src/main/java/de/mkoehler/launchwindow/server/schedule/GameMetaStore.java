package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * <p>Loads {@code game-meta.json} from a server's data root, or creates a
 * default one on first boot if none exists yet (see {@code design.md}
 * &sect;3.6). Game creation being a v1 host file edit, that default is meant
 * to be hand-edited afterward, not used as-is for a real game.</p>
 *
 * @author Mario Koehler
 */
public final class GameMetaStore {

    /**
     * The resolution time of day used for a newly created default
     * {@code game-meta.json}. (Unconfirmed default: design.md &sect;3.6 does
     * not specify one; a host is expected to change it.)
     */
    private static final LocalTime DEFAULT_RESOLUTION_TIME = LocalTime.of(9, 0);

    /**
     * The timezone used for a newly created default {@code game-meta.json}
     * (user, 2026-09-29: hardcoded to the host's own locale rather than
     * derived from the server process, since the same person is expected to
     * be the only host for the foreseeable future). A host running
     * elsewhere edits this field directly - see {@code design.md}
     * &sect;3.6's requirement that this be an explicit, host-chosen zone
     * rather than an ambient one such as a container's UTC default.
     */
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Berlin");

    /**
     * Not instantiable; every member here is static.
     */
    private GameMetaStore() {
    }

    /**
     * Loads {@code game-meta.json} if it exists, or creates and saves a
     * default DAILY-schedule one, in {@link #DEFAULT_TIMEZONE}, if it
     * doesn't.
     *
     * @param file   the path to {@code game-meta.json}
     * @param mapper the Jackson mapper to read/write it with
     * @return the loaded or newly created game meta, together with whether
     *     it had to be created
     * @throws IOException            if the file cannot be read or written
     * @throws IllegalStateException if an existing file fails validation
     */
    public static Result loadOrCreateDefault(Path file, ObjectMapper mapper) throws IOException {
        if (Files.exists(file)) {
            GameMeta meta = mapper.readValue(file.toFile(), GameMeta.class);
            meta.validate();
            return new Result(meta, false);
        }
        GameMeta meta = new GameMeta(ScheduleProfile.daily(DEFAULT_RESOLUTION_TIME, DEFAULT_TIMEZONE), List.of());
        save(file, meta, mapper);
        System.err.println("No " + file.toAbsolutePath() + " found - created a default DAILY schedule at "
            + DEFAULT_RESOLUTION_TIME + " " + DEFAULT_TIMEZONE + ". Edit this file to configure the real game.");
        return new Result(meta, true);
    }

    /**
     * Atomically writes the given game meta to disk.
     *
     * @param file   the path to write to
     * @param meta   the game meta to write
     * @param mapper the Jackson mapper to write it with
     * @throws IOException if the write fails
     */
    private static void save(Path file, GameMeta meta, ObjectMapper mapper) throws IOException {
        Files.createDirectories(file.getParent());
        Path tempFile = Files.createTempFile(file.getParent(), "game-meta-", ".json.tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), meta);
            AtomicFiles.moveReplacingWithRetry(tempFile, file);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * The outcome of {@link #loadOrCreateDefault(Path, ObjectMapper)}.
     *
     * @param meta    the loaded or newly created game meta
     * @param created {@code true} if no file existed and a default one was
     *                just created
     */
    public record Result(GameMeta meta, boolean created) {

        /**
         * Validates that {@code meta} is present.
         *
         * @throws NullPointerException if {@code meta} is {@code null}
         */
        public Result {
            Objects.requireNonNull(meta, "meta must not be null");
        }
    }
}
