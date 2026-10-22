package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The crews lost in flight in each turn ({@code ui-spec.md} &sect;4.18.3, G19), kept in {@code losses/turn-N.json},
 * N being the turn they were lost in: public a turn early, so the next turn's statements and condolences can
 * answer them, and read again when that turn prints their pages. Written whole by the resolution of turn N, so a
 * retry writes the same file. The losing agency is named by its {@code GameSeeds.rivalKey}, never its account id.
 *
 * @author Mario Koehler
 */
public final class LossStore {

    private static final TypeReference<List<FrontPage.Loss>> TYPE = new TypeReference<>() { };

    private LossStore() {
    }

    /**
     * Returns a turn's file.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn
     * @return the file
     */
    static Path file(Path dataRoot, long gameTurn) {
        return dataRoot.resolve("losses").resolve("turn-" + gameTurn + ".json");
    }

    /**
     * Reads the crews lost in a turn.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @return the losses; empty if there were none, or for a turn before the game began
     * @throws IOException if the file can't be read
     */
    public static List<FrontPage.Loss> load(Path dataRoot, ObjectMapper mapper, long gameTurn) throws IOException {
        Path file = file(dataRoot, gameTurn);
        return Files.exists(file) ? List.copyOf(mapper.readValue(file.toFile(), TYPE)) : List.of();
    }

    /**
     * Keeps the crews lost in a turn, replacing what an earlier try of the same resolution kept. A turn that lost
     * none keeps no file.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @param losses   the losses
     * @throws IOException if the file can't be written or deleted
     */
    public static void keep(Path dataRoot, ObjectMapper mapper, long gameTurn, List<FrontPage.Loss> losses)
            throws IOException {
        Path file = file(dataRoot, gameTurn);
        if (losses.isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "turn-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), losses);
        AtomicFiles.moveReplacingWithRetry(temp, file);
    }

    /**
     * Deletes the losses of a turn and every turn after it, for rolling turns back: they are results of the
     * turns being undone.
     *
     * @param dataRoot the data root
     * @param gameTurn the first turn whose losses go
     * @throws IOException if a file can't be deleted
     */
    public static void dropFrom(Path dataRoot, long gameTurn) throws IOException {
        Path dir = dataRoot.resolve("losses");
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.matches("turn-\\d+\\.json") && Long.parseLong(name.substring(5, name.length() - 5)) >= gameTurn) {
                    Files.delete(file);
                }
            }
        }
    }
}
