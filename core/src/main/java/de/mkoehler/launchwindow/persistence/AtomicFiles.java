package de.mkoehler.launchwindow.persistence;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * <p>Shared helper for the "write to a temp file, then atomically replace
 * the target" pattern every flat-file store in this project uses (see
 * {@code design.md} &sect;3.7's crash-robustness requirement).</p>
 *
 * @author Mario Koehler
 */
public final class AtomicFiles {

    private static final int MOVE_RETRIES = 5;
    private static final long MOVE_RETRY_DELAY_MILLIS = 5;

    /**
     * Not instantiable; every member here is static.
     */
    private AtomicFiles() {
    }

    /**
     * Atomically moves {@code source} onto {@code target}, replacing it,
     * retrying a few times on {@link AccessDeniedException} - Windows can
     * transiently hold a brief lock on a file that was just replaced
     * (antivirus/indexing), which this retry absorbs.
     *
     * @param source the temporary file to move from
     * @param target the file to atomically replace
     * @throws IOException if the move still fails after every retry
     */
    public static void moveReplacingWithRetry(Path source, Path target) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < MOVE_RETRIES; attempt++) {
            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (AccessDeniedException e) {
                lastFailure = e;
                try {
                    Thread.sleep(MOVE_RETRY_DELAY_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastFailure;
    }
}
