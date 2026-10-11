package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * <p>Reads and writes one player's resolved turns as flat JSON files on
 * disk, one file per resolved game turn (see {@code design.md} &sect;3.7) -
 * {@code snapshots/&lt;turn&gt;.json}, each holding a {@link ResolvedSnapshot}.
 * A turn's file is written once and never overwritten, and is never pruned;
 * "the current state" is always the highest-numbered file present.</p>
 *
 * <p>Writes are atomic: the new content is written to a temporary file in
 * the same directory, then renamed into place, so a crash mid-write can
 * never leave a half-written file behind.</p>
 *
 * @author Mario Koehler
 */
public class PlayerSnapshotStore {

    private static final String SUFFIX = ".json";

    private final Path snapshotsDir;
    private final ObjectMapper mapper;

    /**
     * Creates a new store for one player, rooted at the given directory.
     *
     * @param playerRoot the player's own data directory
     * @param mapper     the Jackson mapper used to read/write snapshots
     */
    public PlayerSnapshotStore(Path playerRoot, ObjectMapper mapper) {
        Objects.requireNonNull(playerRoot, "playerRoot must not be null");
        this.snapshotsDir = playerRoot.resolve("snapshots");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Loads the player's most recently resolved snapshot, if any turn has
     * ever been resolved for them.
     *
     * @return the latest snapshot, or {@link Optional#empty()} if this
     *     player has no saved snapshot yet
     * @throws IOException if a snapshot file exists but cannot be read
     */
    public Optional<PlayerSnapshot> load() throws IOException {
        OptionalLong latest = latestTurnNumber();
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        return load(latest.getAsLong());
    }

    /**
     * Loads the snapshot resolved for a specific turn.
     *
     * @param turn the turn number to load
     * @return the snapshot for that turn, or {@link Optional#empty()} if no
     *     snapshot was ever saved for it
     * @throws IOException if the snapshot file exists but cannot be read
     */
    public Optional<PlayerSnapshot> load(long turn) throws IOException {
        return loadRecord(turn).map(ResolvedSnapshot::getSnapshot);
    }

    /**
     * Returns the journal file name of the last action applied to resolve
     * the given turn, for a crash-recovery retry to know exactly which
     * journal files that turn's resolution already consumed.
     *
     * @param turn the turn number to look up
     * @return the last applied action's file name, or {@link Optional#empty()}
     *     if that turn has no saved snapshot, or if it was resolved with no
     *     actions pending
     * @throws IOException if the snapshot file exists but cannot be read
     */
    /**
     * Loads the most recent snapshot resolved before a turn - the player's state
     * at the start of that turn, even if a crashed attempt already wrote the turn
     * itself ({@code design.md} &sect;5, M4: the game-wide goal tally must be built
     * from start-of-turn state).
     *
     * @param turn the turn being resolved
     * @return the latest snapshot with a lower turn number, if any
     * @throws IOException if the snapshot folder can't be read
     */
    public Optional<PlayerSnapshot> loadLatestBefore(long turn) throws IOException {
        if (!Files.exists(snapshotsDir)) {
            return Optional.empty();
        }
        OptionalLong before;
        try (Stream<Path> listing = Files.list(snapshotsDir)) {
            before = listing
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(SUFFIX))
                .mapToLong(name -> Long.parseLong(name.substring(0, name.length() - SUFFIX.length())))
                .filter(number -> number < turn)
                .max();
        }
        return before.isEmpty() ? Optional.empty() : load(before.getAsLong());
    }

    public Optional<String> lastAppliedActionFile(long turn) throws IOException {
        return loadRecord(turn).map(ResolvedSnapshot::getLastAppliedActionFile);
    }

    /**
     * Saves the given snapshot as the result of resolving the given turn.
     * A no-op if that turn's file already exists (idempotent, so a crashed
     * and retried resolution never fails on this step alone).
     *
     * @param turn                   the turn number this snapshot is the
     *                               result of
     * @param snapshot               the snapshot to save
     * @param lastAppliedActionFile  the file name of the last action
     *                               applied to produce it, or {@code null}
     *                               if none were pending
     * @throws IOException if the snapshot cannot be written
     */
    public void save(long turn, PlayerSnapshot snapshot, String lastAppliedActionFile) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Path target = snapshotsDir.resolve(turn + SUFFIX);
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(snapshotsDir);
        Path tempFile = Files.createTempFile(snapshotsDir, "snapshot-", ".json.tmp");
        try {
            mapper.writeValue(tempFile.toFile(), new ResolvedSnapshot(snapshot, lastAppliedActionFile));
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * Deletes a turn's snapshot, when the turn is rolled back ({@code design.md} &sect;3.7): the
     * one exception to "written once, never replaced".
     *
     * @param turn the turn
     * @throws IOException if the file exists but can't be deleted
     */
    public void delete(long turn) throws IOException {
        Files.deleteIfExists(snapshotsDir.resolve(turn + SUFFIX));
    }

    /**
     * Returns the highest turn number this player has a saved snapshot for.
     *
     * @return the latest resolved turn number, or an empty result if this
     *     player has no saved snapshot yet
     * @throws IOException if the snapshots directory cannot be read
     */
    public OptionalLong latestTurnNumber() throws IOException {
        if (!Files.exists(snapshotsDir)) {
            return OptionalLong.empty();
        }
        try (Stream<Path> listing = Files.list(snapshotsDir)) {
            return listing
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(SUFFIX))
                .mapToLong(name -> Long.parseLong(name.substring(0, name.length() - SUFFIX.length())))
                .max();
        }
    }

    /**
     * Loads the raw on-disk envelope for a specific turn.
     *
     * @param turn the turn number to load
     * @return the envelope for that turn, or {@link Optional#empty()} if no
     *     snapshot was ever saved for it
     * @throws IOException if the snapshot file exists but cannot be read
     */
    private Optional<ResolvedSnapshot> loadRecord(long turn) throws IOException {
        Path file = snapshotsDir.resolve(turn + SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(file.toFile(), ResolvedSnapshot.class));
    }
}
