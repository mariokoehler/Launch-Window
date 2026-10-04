package de.mkoehler.launchwindow.server;

import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.ContentPackLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A game's own copy of its content pack ({@code design.md} &sect;5 M5, "Content packs"): the
 * first time a game's server starts, the pack named in {@code game-meta.json} is copied from the
 * classpath into {@code <dataRoot>/content/<packId>/}, and from then on the game only ever loads
 * that copy. A later server update that changes the bundled pack leaves running games alone;
 * replacing a game's copy is a deliberate host step.
 *
 * @param packId    the pack's id, its folder name
 * @param directory the frozen copy
 * @param hash      the copy's {@link ContentPackHash}
 * @author Mario Koehler
 */
public record FrozenContentPack(String packId, Path directory, String hash) {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /**
     * Returns the game's frozen copy of a pack, making it first if the game has none yet. The copy
     * is written to a temporary folder and renamed into place, so a crash mid-copy never leaves a
     * half-written folder that would later pass for the frozen pack.
     *
     * @param dataRoot    the game's data root
     * @param packId      the pack's id, as named in {@code game-meta.json}
     * @param classLoader the class loader holding the bundled packs under {@code content/}
     * @return the frozen copy
     * @throws IOException              if the copy can't be made
     * @throws IllegalArgumentException if the id isn't a plain folder name
     */
    public static FrozenContentPack freeze(Path dataRoot, String packId, ClassLoader classLoader) throws IOException {
        Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        if (packId == null || !SAFE_ID.matcher(packId).matches()) {
            throw new IllegalArgumentException("contentPack must be a plain folder name: " + packId);
        }
        Path contentRoot = dataRoot.resolve("content");
        Path target = contentRoot.resolve(packId);
        if (!Files.isDirectory(target)) {
            Files.createDirectories(contentRoot);
            Path staging = Files.createTempDirectory(contentRoot, "." + packId + "-");
            try {
                for (String fileName : ContentPackLoader.FILES) {
                    String resource = "content/" + packId + "/" + fileName;
                    try (InputStream in = classLoader.getResourceAsStream(resource)) {
                        if (in == null) {
                            throw new IOException("no bundled content pack file " + resource);
                        }
                        Files.copy(in, staging.resolve(fileName));
                    }
                }
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                deleteIfPresent(staging);
            }
        }
        return new FrozenContentPack(packId, target, ContentPackHash.of(target));
    }

    private static void deleteIfPresent(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
