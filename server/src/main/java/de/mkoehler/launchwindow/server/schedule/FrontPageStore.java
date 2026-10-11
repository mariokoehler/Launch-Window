package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The game's printed front pages ({@code ui-spec.md} &sect;4.18, G14), kept in the data root's
 * {@code frontpages.json} and rewritten whole, atomically, like the game result. Shared by every
 * player; append-only, except that rolling turns back removes theirs.
 *
 * @author Mario Koehler
 */
public final class FrontPageStore {

    /** The file's name in the data root. */
    public static final String FILE_NAME = "frontpages.json";

    private FrontPageStore() {
    }

    /**
     * What the file holds.
     *
     * @param pages every page, oldest first
     */
    public record Pages(List<FrontPage> pages) {

        /**
         * Copies the list.
         */
        public Pages {
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    /**
     * Reads every page.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @return the pages, oldest first; empty if none were printed
     * @throws IOException if the file can't be read
     */
    public static List<FrontPage> load(Path dataRoot, ObjectMapper mapper) throws IOException {
        Path file = dataRoot.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            return List.of();
        }
        return mapper.readValue(file.toFile(), Pages.class).pages();
    }

    /**
     * Keeps a turn's pages, replacing any kept for that turn before (a retried resolution prints
     * the same pages again).
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @param printed  the turn's pages
     * @throws IOException if the file can't be read or written
     */
    public static void keep(Path dataRoot, ObjectMapper mapper, long gameTurn, List<FrontPage> printed)
            throws IOException {
        List<FrontPage> existing = load(dataRoot, mapper);
        List<FrontPage> pages = new ArrayList<>(existing.stream().filter(p -> p.gameTurn() != gameTurn).toList());
        if (printed.isEmpty() && pages.size() == existing.size()) {
            return;
        }
        pages.addAll(printed);
        pages.sort(Comparator.comparingInt(FrontPage::gameTurn));
        write(dataRoot, mapper, pages);
    }

    /**
     * Removes the pages of a turn and every turn after it, for rolling turns back.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the first turn whose pages go
     * @throws IOException if the file can't be read or written
     */
    public static void dropFrom(Path dataRoot, ObjectMapper mapper, long gameTurn) throws IOException {
        List<FrontPage> pages = load(dataRoot, mapper);
        List<FrontPage> kept = pages.stream().filter(p -> p.gameTurn() < gameTurn).toList();
        if (kept.size() != pages.size()) {
            write(dataRoot, mapper, kept);
        }
    }

    private static void write(Path dataRoot, ObjectMapper mapper, List<FrontPage> pages) throws IOException {
        Files.createDirectories(dataRoot);
        Path temp = Files.createTempFile(dataRoot, "frontpages-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), new Pages(pages));
        AtomicFiles.moveReplacingWithRetry(temp, dataRoot.resolve(FILE_NAME));
    }
}
