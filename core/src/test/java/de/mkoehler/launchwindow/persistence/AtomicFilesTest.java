package de.mkoehler.launchwindow.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link AtomicFiles}.</p>
 *
 * @author Mario Koehler
 */
class AtomicFilesTest {

    /**
     * Verifies that moving onto a target that doesn't exist yet creates it
     * with the source's content.
     */
    @Test
    void moveReplacingWithRetryCreatesTheTargetWhenItDoesNotExistYet(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("source.tmp");
        Path target = dir.resolve("target.txt");
        Files.writeString(source, "first");

        AtomicFiles.moveReplacingWithRetry(source, target);

        assertEquals("first", Files.readString(target));
        assertFalse(Files.exists(source), "the source file should no longer exist after being moved");
    }

    /**
     * Verifies that moving onto an existing target replaces its content.
     */
    @Test
    void moveReplacingWithRetryReplacesAnExistingTarget(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("target.txt");
        Files.writeString(target, "old");
        Path source = dir.resolve("source.tmp");
        Files.writeString(source, "new");

        AtomicFiles.moveReplacingWithRetry(source, target);

        assertEquals("new", Files.readString(target));
    }

    /**
     * Verifies that repeatedly replacing the same target many times in a
     * row - the pattern the retry exists for - always leaves the target
     * with the last value written.
     */
    @Test
    void repeatedReplacesLeaveTheLastValueWritten(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("target.txt");

        for (int i = 0; i < 100; i++) {
            Path source = dir.resolve("source-" + i + ".tmp");
            Files.writeString(source, String.valueOf(i));
            AtomicFiles.moveReplacingWithRetry(source, target);
        }

        assertEquals("99", Files.readString(target));
        assertTrue(Files.exists(target));
    }
}
