package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * One player's turn reports on disk ({@code design.md} &sect;3.7, gap G2): one JSON file per
 * resolved turn under {@code reports/}, named by the turn like the snapshots beside it. Written
 * atomically, before the turn's snapshot, so a crash in between redoes the turn and writes the same
 * report again.
 *
 * @author Mario Koehler
 */
public class TurnReportStore {

    private static final String SUFFIX = ".json";

    private final Path reportsDir;
    private final ObjectMapper mapper;

    /**
     * Creates a store for one player's folder.
     *
     * @param playerRoot the player's folder
     * @param mapper     the persistence mapper
     */
    public TurnReportStore(Path playerRoot, ObjectMapper mapper) {
        this.reportsDir = Objects.requireNonNull(playerRoot, "playerRoot must not be null").resolve("reports");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Writes a turn's report, replacing any earlier attempt at the same turn.
     *
     * @param report the report
     * @throws IOException if it can't be written
     */
    public void save(TurnReport report) throws IOException {
        Objects.requireNonNull(report, "report must not be null");
        Files.createDirectories(reportsDir);
        Path target = reportsDir.resolve(report.gameTurn() + SUFFIX);
        Path temp = Files.createTempFile(reportsDir, "report-", ".json.tmp");
        try {
            mapper.writeValue(temp.toFile(), report);
            AtomicFiles.moveReplacingWithRetry(temp, target);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * Loads a turn's report.
     *
     * @param turn the turn
     * @return the report, or empty if there's none for that turn
     * @throws IOException if the file exists but can't be read
     */
    public Optional<TurnReport> load(long turn) throws IOException {
        Path file = reportsDir.resolve(turn + SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(file.toFile(), TurnReport.class));
    }

    /**
     * Deletes a turn's report, when the turn is rolled back ({@code design.md} &sect;3.7).
     *
     * @param turn the turn
     * @throws IOException if the file exists but can't be deleted
     */
    public void delete(long turn) throws IOException {
        Files.deleteIfExists(reportsDir.resolve(turn + SUFFIX));
    }

    /**
     * Returns the latest turn with a report.
     *
     * @return the turn, or empty if there's no report yet
     * @throws IOException if the folder can't be read
     */
    public OptionalLong latestTurn() throws IOException {
        if (!Files.exists(reportsDir)) {
            return OptionalLong.empty();
        }
        try (Stream<Path> listing = Files.list(reportsDir)) {
            return listing
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(SUFFIX) && !name.startsWith("report-"))
                .mapToLong(name -> Long.parseLong(name.substring(0, name.length() - SUFFIX.length())))
                .max();
        }
    }
}
