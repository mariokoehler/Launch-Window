package de.mkoehler.launchwindow.server;

import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link FrozenContentPack}.
 *
 * @author Mario Koehler
 */
class FrozenContentPackTest {

    private static final ClassLoader BUNDLED = FrozenContentPackTest.class.getClassLoader();

    /**
     * The first start copies the bundled pack; the copy loads and hashes like the original.
     */
    @Test
    void theFirstStartCopiesTheBundledPack(@TempDir Path dataRoot) throws Exception {
        FrozenContentPack frozen = FrozenContentPack.freeze(dataRoot, "early-era", BUNDLED);

        assertEquals(dataRoot.resolve("content").resolve("early-era"), frozen.directory());
        for (String file : ContentPackLoader.FILES) {
            assertTrue(Files.isRegularFile(frozen.directory().resolve(file)), file);
        }
        assertEquals(ContentPackHash.ofClasspath("content/early-era", BUNDLED), frozen.hash());
        ContentPackLoader.load(frozen.directory());
        try (Stream<Path> leftovers = Files.list(dataRoot.resolve("content"))) {
            assertEquals(1, leftovers.count(), "no staging folder is left behind");
        }
    }

    /**
     * Once frozen, the game keeps its copy: a changed copy is what later starts load and hash,
     * not the bundled pack.
     */
    @Test
    void aFrozenCopyIsNeverOverwritten(@TempDir Path dataRoot) throws Exception {
        FrozenContentPack first = FrozenContentPack.freeze(dataRoot, "early-era", BUNDLED);
        Path goals = first.directory().resolve(ContentPackLoader.GOALS_FILE);
        Files.writeString(goals, Files.readString(goals) + "\n");
        Files.writeString(goals, Files.readString(goals).replaceFirst("\\[", "[ "));

        FrozenContentPack again = FrozenContentPack.freeze(dataRoot, "early-era", BUNDLED);

        assertNotEquals(first.hash(), again.hash());
        assertEquals(ContentPackHash.of(first.directory()), again.hash());
    }

    /**
     * A pack id that could climb out of the data root, or a pack that isn't bundled, is refused.
     */
    @Test
    void badPackIdsAreRefused(@TempDir Path dataRoot) {
        assertThrows(IllegalArgumentException.class, () -> FrozenContentPack.freeze(dataRoot, "../evil", BUNDLED));
        assertThrows(java.io.IOException.class, () -> FrozenContentPack.freeze(dataRoot, "no-such-pack", BUNDLED));
        assertTrue(Files.notExists(dataRoot.resolve("content").resolve("no-such-pack")));
    }
}
