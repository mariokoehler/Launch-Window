package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The statements rivals give about a First ({@code ui-spec.md} &sect;4.18.1, G17), kept per turn in
 * {@code reactions/turn-N.json}, N being the open turn they were given in, and read when that turn
 * resolves. Keyed by {@code GameSeeds.rivalKey}, never by account id. Each file is rewritten whole,
 * atomically; a lock keeps a statement arriving while another is written from losing either.
 *
 * @author Mario Koehler
 */
public final class ReactionStore {

    private static final Object LOCK = new Object();
    private static final TypeReference<Map<String, List<FrontPage.Statement>>> TYPE = new TypeReference<>() { };

    private ReactionStore() {
    }

    /**
     * Returns a turn's file.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn
     * @return the file
     */
    static Path file(Path dataRoot, long gameTurn) {
        return dataRoot.resolve("reactions").resolve("turn-" + gameTurn + ".json");
    }

    /**
     * Reads a turn's statements.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @return the statements, per rival key; empty if none were given
     * @throws IOException if the file can't be read
     */
    public static Map<String, List<FrontPage.Statement>> load(Path dataRoot, ObjectMapper mapper, long gameTurn)
            throws IOException {
        synchronized (LOCK) {
            Path file = file(dataRoot, gameTurn);
            return Files.exists(file) ? new TreeMap<>(mapper.readValue(file.toFile(), TYPE)) : new TreeMap<>();
        }
    }

    /**
     * Returns one rival's statements in a turn.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @param key      the rival's key
     * @return its statements, maybe none
     * @throws IOException if the file can't be read
     */
    public static List<FrontPage.Statement> of(Path dataRoot, ObjectMapper mapper, long gameTurn, String key)
            throws IOException {
        return List.copyOf(load(dataRoot, mapper, gameTurn).getOrDefault(key, List.of()));
    }

    /**
     * Keeps a statement, replacing the rival's earlier one about the same page.
     *
     * @param dataRoot  the data root
     * @param mapper    the JSON mapper
     * @param gameTurn  the turn
     * @param key       the rival's key
     * @param statement the statement
     * @return the rival's statements in the turn, after this one
     * @throws IOException if the file can't be read or written
     */
    public static List<FrontPage.Statement> put(Path dataRoot, ObjectMapper mapper, long gameTurn, String key,
                                                FrontPage.Statement statement) throws IOException {
        synchronized (LOCK) {
            Map<String, List<FrontPage.Statement>> all = load(dataRoot, mapper, gameTurn);
            List<FrontPage.Statement> mine = new ArrayList<>(all.getOrDefault(key, List.of()));
            mine.removeIf(s -> s.pageId().equals(statement.pageId()));
            mine.add(statement);
            all.put(key, mine);
            write(dataRoot, mapper, gameTurn, all);
            return List.copyOf(mine);
        }
    }

    /**
     * Withdraws a rival's statement about a page.
     *
     * @param dataRoot the data root
     * @param mapper   the JSON mapper
     * @param gameTurn the turn
     * @param key      the rival's key
     * @param pageId   the page
     * @return the rival's statements in the turn, after this
     * @throws IOException if the file can't be read or written
     */
    public static List<FrontPage.Statement> remove(Path dataRoot, ObjectMapper mapper, long gameTurn, String key,
                                                   String pageId) throws IOException {
        synchronized (LOCK) {
            Map<String, List<FrontPage.Statement>> all = load(dataRoot, mapper, gameTurn);
            List<FrontPage.Statement> mine = new ArrayList<>(all.getOrDefault(key, List.of()));
            if (!mine.removeIf(s -> s.pageId().equals(pageId))) {
                return List.copyOf(mine);
            }
            if (mine.isEmpty()) {
                all.remove(key);
            } else {
                all.put(key, mine);
            }
            write(dataRoot, mapper, gameTurn, all);
            return List.copyOf(mine);
        }
    }

    /**
     * Deletes the statements of every turn after one, for rolling turns back: those were given against
     * results that are being undone. The turn's own stay, so a redone turn prints the same follow-ups.
     *
     * @param dataRoot the data root
     * @param gameTurn the last turn whose statements stay
     * @throws IOException if a file can't be deleted
     */
    public static void dropAfter(Path dataRoot, long gameTurn) throws IOException {
        synchronized (LOCK) {
            Path dir = dataRoot.resolve("reactions");
            if (!Files.isDirectory(dir)) {
                return;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.toList()) {
                    String name = file.getFileName().toString();
                    if (name.matches("turn-\\d+\\.json")
                        && Long.parseLong(name.substring(5, name.length() - 5)) > gameTurn) {
                        Files.delete(file);
                    }
                }
            }
        }
    }

    private static void write(Path dataRoot, ObjectMapper mapper, long gameTurn,
                              Map<String, List<FrontPage.Statement>> all) throws IOException {
        Path file = file(dataRoot, gameTurn);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "turn-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), all);
        AtomicFiles.moveReplacingWithRetry(temp, file);
    }
}
