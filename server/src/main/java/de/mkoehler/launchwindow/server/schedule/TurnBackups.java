package de.mkoehler.launchwindow.server.schedule;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>Zips the game's data root into {@code backups/} before every turn resolves and before every
 * rollback ({@code design.md} &sect;3.7): the safety net under {@link TurnRollback}, for a bug that
 * damages files the rollback relies on. To restore one, stop the server, delete everything in the
 * data root but {@code accounts.json}, {@code content/} and {@code backups/} (which the zip leaves
 * out), then unzip and start the server again. Never unzip over the folder: newer journal files
 * would stay beside an older sequence counter.</p>
 *
 * <p>Each zip holds the game's whole history, so it grows with the game; only the last
 * {@link #KEEP} turns' zips are kept. Rollback zips are never pruned.</p>
 *
 * @author Mario Koehler
 */
public final class TurnBackups {

    /** The folder under the data root the zips go in. */
    public static final String DIR = "backups";

    /** How many turns' zips are kept. */
    public static final int KEEP = 10;

    private static final Pattern TURN_ZIP = Pattern.compile("before-turn-(\\d+)\\.zip");

    /** Left out of every zip: the zips themselves, the logins' password hashes, the frozen pack. */
    private static final List<String> LEFT_OUT = List.of(DIR, "accounts.json", "content");

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private TurnBackups() {
    }

    /**
     * Zips the data root as it is before a turn resolves, as {@code backups/before-turn-N.zip}, and
     * prunes the zips older than the last {@link #KEEP}. Written once: a resolution retried after a
     * failure keeps the zip of the state before its first attempt.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn about to resolve
     * @return the zip
     * @throws IOException if the zip can't be written
     */
    public static Path beforeTurn(Path dataRoot, long gameTurn) throws IOException {
        Path zip = dataRoot.resolve(DIR).resolve(turnZipName(gameTurn));
        if (!Files.exists(zip)) {
            write(dataRoot, zip);
        }
        prune(dataRoot, gameTurn);
        return zip;
    }

    /**
     * Zips the data root as it is before a rollback, as
     * {@code backups/before-rollback-to-turn-N-<time>.zip}.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn being rolled back to
     * @param now      the local time, for the name
     * @return the zip
     * @throws IOException if the zip can't be written
     */
    public static Path beforeRollback(Path dataRoot, long gameTurn, LocalDateTime now) throws IOException {
        Path zip = dataRoot.resolve(DIR).resolve("before-rollback-to-turn-" + gameTurn + "-"
            + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip");
        write(dataRoot, zip);
        return zip;
    }

    /**
     * Deletes the turn zips from a timeline a rollback abandoned: those before turns after the one
     * rolled back to, which would otherwise stand in for the redone turns' own.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn rolled back to; its own zip stays
     * @throws IOException if the folder can't be read or a zip can't be deleted
     */
    public static void dropAfter(Path dataRoot, long gameTurn) throws IOException {
        for (Path zip : turnZips(dataRoot)) {
            if (turnOf(zip) > gameTurn) {
                Files.delete(zip);
            }
        }
    }

    /**
     * Returns the name of the zip of the state before a turn.
     *
     * @param gameTurn the turn
     * @return the file name
     */
    static String turnZipName(long gameTurn) {
        return "before-turn-" + gameTurn + ".zip";
    }

    private static void prune(Path dataRoot, long gameTurn) throws IOException {
        for (Path zip : turnZips(dataRoot)) {
            if (turnOf(zip) <= gameTurn - KEEP) {
                Files.delete(zip);
            }
        }
    }

    private static List<Path> turnZips(Path dataRoot) throws IOException {
        Path dir = dataRoot.resolve(DIR);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> listing = Files.list(dir)) {
            return listing.filter(path -> TURN_ZIP.matcher(path.getFileName().toString()).matches()).toList();
        }
    }

    private static long turnOf(Path zip) {
        Matcher matcher = TURN_ZIP.matcher(zip.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a turn zip: " + zip);
        }
        return Long.parseLong(matcher.group(1));
    }

    /**
     * Zips the data root into a temporary file beside the target, then renames it, so a crash
     * halfway never leaves a broken zip under the final name.
     */
    private static void write(Path dataRoot, Path zip) throws IOException {
        Files.createDirectories(zip.getParent());
        Path temp = Files.createTempFile(zip.getParent(), "backup-", ".zip.tmp");
        try {
            List<Path> files;
            try (Stream<Path> walk = Files.walk(dataRoot)) {
                files = walk.filter(Files::isRegularFile)
                    .filter(file -> !leftOut(dataRoot.relativize(file)))
                    .sorted()
                    .toList();
            }
            try (OutputStream out = Files.newOutputStream(temp); ZipOutputStream zipOut = new ZipOutputStream(out)) {
                for (Path file : files) {
                    zipOut.putNextEntry(new ZipEntry(entryName(dataRoot.relativize(file))));
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();
                }
            }
            Files.move(temp, zip);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    private static boolean leftOut(Path relative) {
        return LEFT_OUT.contains(relative.getName(0).toString()) || relative.getFileName().toString().endsWith(".tmp");
    }

    private static String entryName(Path relative) {
        StringBuilder name = new StringBuilder();
        for (Path part : relative) {
            if (!name.isEmpty()) {
                name.append('/');
            }
            name.append(part);
        }
        return name.toString();
    }
}
