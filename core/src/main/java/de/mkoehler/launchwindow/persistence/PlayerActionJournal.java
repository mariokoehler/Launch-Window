package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <p>The append-only, one-file-per-action journal of a player's not-yet-
 * resolved actions (see {@code design.md} &sect;3.7). Each submitted
 * {@link PlayerAction} is written as its own JSON file, named with a
 * zero-padded sequence number so that listing the directory always yields
 * the actions in submission order, even when two actions are submitted
 * within the same millisecond.</p>
 *
 * <p>The sequence number is durably persisted and never reused, for the
 * life of the player's whole game - not reset after an archive. A crash-
 * recovery retry (see {@link PlayerStateService#resolveTurn(long)}) compares
 * journal file names as plain strings to decide what's already been
 * applied; if two different actions, submitted on either side of an
 * archive, could ever share the same sequence number, that comparison
 * would be ambiguous.</p>
 *
 * @author Mario Koehler
 */
public class PlayerActionJournal {

    private static final String SUFFIX = ".json";
    private static final int SEQUENCE_WIDTH = 20;

    private final Path journalDir;
    private final Path archiveRoot;
    private final Path sequenceFile;
    private final ObjectMapper mapper;

    /**
     * Creates a new journal for one player, rooted at the given directory.
     *
     * @param playerRoot the player's own data directory
     * @param mapper     the Jackson mapper used to read/write action files
     */
    public PlayerActionJournal(Path playerRoot, ObjectMapper mapper) {
        Objects.requireNonNull(playerRoot, "playerRoot must not be null");
        this.journalDir = playerRoot.resolve("journal");
        this.archiveRoot = playerRoot.resolve("journal-archive");
        this.sequenceFile = playerRoot.resolve("journal-sequence.txt");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Appends a new action to the journal, after every action already in
     * it. Synchronized so that two concurrent appends within the same
     * process never compute the same next sequence number.
     *
     * @param action the action to append
     * @throws IOException if the action or the sequence counter cannot be
     *                      written
     */
    public synchronized void append(PlayerAction action) throws IOException {
        Objects.requireNonNull(action, "action must not be null");
        Files.createDirectories(journalDir);
        // The sequence counter is reserved and durably persisted BEFORE the action file is
        // written, not after: if a crash happens between the two, the reserved number is simply
        // never used (a harmless gap), rather than the next append recomputing and reusing the
        // same number a still-pending file already has.
        long sequence = readSequenceCounter() + 1;
        writeSequenceCounter(sequence);
        String fileName = String.format("%0" + SEQUENCE_WIDTH + "d", sequence) + "-" + UUID.randomUUID() + SUFFIX;
        Path target = journalDir.resolve(fileName);
        Path tempFile = Files.createTempFile(journalDir, "action-", ".json.tmp");
        try {
            mapper.writeValue(tempFile.toFile(), action);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * Returns every action currently in the journal, each paired with the
     * file it was read from, in submission order.
     *
     * @return the pending actions, oldest first
     * @throws IOException if the journal directory cannot be read
     */
    public List<PendingAction> readPending() throws IOException {
        if (!Files.exists(journalDir)) {
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> listing = Files.list(journalDir)) {
            files = listing
                .filter(path -> path.toString().endsWith(SUFFIX))
                .sorted(Comparator.comparing(Path::getFileName))
                .toList();
        }
        List<PendingAction> pending = new ArrayList<>(files.size());
        for (Path file : files) {
            pending.add(new PendingAction(file, mapper.readValue(file.toFile(), PlayerAction.class)));
        }
        return pending;
    }

    /**
     * Moves exactly the given pending actions' files into a per-turn
     * archive subdirectory. A file already missing (e.g. archived by an
     * earlier, crashed attempt at resolving the same turn) is skipped
     * rather than failing the whole call, so this is safe to retry.
     * Any action submitted after {@code pending} was read is left in the
     * journal untouched, for the next turn to pick up.
     *
     * @param pending    exactly the pending actions to archive, as returned
     *                   by {@link #readPending()}
     * @param turnNumber the turn number the archived actions were resolved
     *                   into, used to name the archive subdirectory
     * @throws IOException if the archive directory cannot be created or a
     *                      file cannot be moved
     */
    public void archive(List<PendingAction> pending, long turnNumber) throws IOException {
        Objects.requireNonNull(pending, "pending must not be null");
        if (pending.isEmpty()) {
            return;
        }
        Path archiveDir = archiveRoot.resolve(String.valueOf(turnNumber));
        Files.createDirectories(archiveDir);
        for (PendingAction action : pending) {
            try {
                Files.move(action.file(), archiveDir.resolve(action.file().getFileName()),
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (NoSuchFileException e) {
                // already archived by an earlier attempt at resolving this same turn - fine.
            }
        }
    }

    /**
     * Reads the last sequence number handed out, or {@code 0} if no action
     * has ever been appended for this player.
     *
     * @return the last sequence number handed out
     * @throws IOException if the sequence file exists but cannot be read
     */
    private long readSequenceCounter() throws IOException {
        if (!Files.exists(sequenceFile)) {
            return 0;
        }
        String content = Files.readString(sequenceFile).strip();
        return content.isEmpty() ? 0 : Long.parseLong(content);
    }

    /**
     * Atomically persists the last sequence number handed out.
     *
     * @param value the sequence number to persist
     * @throws IOException if the sequence file cannot be written
     */
    private void writeSequenceCounter(long value) throws IOException {
        Files.createDirectories(sequenceFile.getParent());
        Path tempFile = Files.createTempFile(sequenceFile.getParent(), "journal-sequence-", ".tmp");
        try {
            Files.writeString(tempFile, Long.toString(value));
            AtomicFiles.moveReplacingWithRetry(tempFile, sequenceFile);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * One pending action together with the journal file it was read from,
     * so a later archive step can move exactly that file rather than
     * whatever happens to be in the journal directory at archive time.
     *
     * @param file   the journal file this action was read from
     * @param action the deserialized action
     */
    public record PendingAction(Path file, PlayerAction action) {
    }
}
