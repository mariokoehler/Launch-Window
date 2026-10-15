package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Archive} and its pages in {@code assets/content/archive.json} ({@code design.md}
 * &sect;2.12): every page opened by a goal of the standard pack, its pictures and sound there and
 * credited, and nothing in the archive's folders left over.
 *
 * @author Mario Koehler
 */
class ArchiveTest {

    private static final Path ASSETS = Path.of("..", "assets");
    private static final Archive ARCHIVE = Archive.load(ASSETS);
    private static final ContentCatalog STANDARD = ContentPackLoader.load(ASSETS.resolve("content").resolve("standard"));

    /**
     * Every page is complete, its goal is in the standard pack, and the pages run in the order the
     * missions flew.
     */
    @Test
    void everyPageIsCompleteAndOpenedByAStandardGoal() {
        assertFalse(ARCHIVE.pages().isEmpty());
        Set<String> ids = new HashSet<>();
        String previous = "";
        for (Archive.Page page : ARCHIVE.pages()) {
            assertTrue(ids.add(page.id()), "twice: " + page.id());
            assertTrue(STANDARD.goal(page.goalId()).isPresent(), page.id() + ": no goal " + page.goalId());
            assertTrue(page.when().matches("\\d{4}-\\d{2}"), page.id());
            assertTrue(page.when().compareTo(previous) >= 0, page.id() + " out of order");
            previous = page.when();
            assertFalse(page.title().isBlank() || page.agency().isBlank() || page.date().isBlank(), page.id());
            assertTrue(page.summary().length() <= 90, page.id() + ": summary too long");
            assertFalse(page.facts().isEmpty(), page.id());
            assertTrue(page.sections().size() >= 3, page.id());
            assertFalse(page.sources().isEmpty(), page.id());
            assertFalse(page.images().isEmpty(), page.id());
            for (Archive.Picture picture : page.images()) {
                assertFalse(picture.caption().isBlank() || picture.credit().isBlank(), picture.file());
            }
        }
        assertEquals(ARCHIVE.pages().size(), ARCHIVE.pages(STANDARD).size(), "every page opens in a standard game");
    }

    /**
     * Every picture, thumbnail and sound is there and credited, and the archive's folders hold
     * nothing else.
     *
     * @throws IOException if a file can't be read
     */
    @Test
    void everyFileIsThereCreditedAndUsed() throws IOException {
        String credits = Files.readString(ASSETS.resolve("CREDITS.md"), StandardCharsets.UTF_8);
        Set<String> used = new HashSet<>();
        for (Archive.Page page : ARCHIVE.pages()) {
            for (Archive.Picture picture : page.images()) {
                assertTrue(Files.exists(ASSETS.resolve("ui").resolve(picture.file())), picture.file());
                assertTrue(credits.contains("ui/" + picture.file()), "not credited: " + picture.file());
                used.add("ui/" + picture.file());
            }
            assertTrue(Files.exists(ASSETS.resolve("ui").resolve(page.thumbnail())), page.thumbnail());
            used.add("ui/" + page.thumbnail());
            if (page.sound() != null) {
                assertTrue(Files.exists(ASSETS.resolve(page.sound().file())), page.sound().file());
                assertTrue(credits.contains(page.sound().file()), "not credited: " + page.sound().file());
                assertTrue(page.sound().seconds() > 0, page.sound().file());
                used.add(page.sound().file());
            }
        }
        for (String folder : List.of("ui/archive", "audio/archive")) {
            try (Stream<Path> files = Files.walk(ASSETS.resolve(folder))) {
                files.filter(Files::isRegularFile).map(path -> ASSETS.relativize(path).toString().replace('\\', '/'))
                    .forEach(file -> assertTrue(used.contains(file), "unused: " + file));
            }
        }
    }

    /**
     * The server's answer opens pages: since its turn, by the player or a rival; a page whose goal
     * nobody holds stays locked, and the turn report shows the pages its turn opened.
     */
    @Test
    void unlocksOpenPages() {
        Archive.Page first = ARCHIVE.pages().get(0);
        Archive.Page second = ARCHIVE.pages().get(1);
        List<ArchiveUnlock> unlocks = List.of(new ArchiveUnlock(first.goalId(), 7, true),
            new ArchiveUnlock(second.goalId(), 9, false));

        List<Archive.Entry> entries = ARCHIVE.entries(STANDARD, unlocks);

        Archive.Entry one = Archive.entry(entries, first.id());
        assertTrue(one.open() && one.byYou());
        assertEquals(7, one.turn());
        Archive.Entry two = Archive.entry(entries, second.id());
        assertTrue(two.open() && !two.byYou());
        Archive.Entry last = Archive.entry(entries, ARCHIVE.pages().get(ARCHIVE.pages().size() - 1).id());
        assertNull(last.turn());
        assertEquals(List.of(second.id()), ARCHIVE.openedIn(STANDARD, unlocks, 9).stream().map(e -> e.page().id()).toList());
        assertTrue(ARCHIVE.openedIn(STANDARD, null, 9).isEmpty(), "nothing before the server answers");
        assertTrue(ARCHIVE.entries(STANDARD, null).stream().noneMatch(Archive.Entry::open));

        // Opened when the race was decided: open, nobody's flight.
        Archive.Entry end = Archive.entry(ARCHIVE.entries(STANDARD, List.of(new ArchiveUnlock(last.page().goalId(), 60,
            false, true))), last.page().id());
        assertTrue(end.open() && end.atTheEnd() && !end.byYou());
    }
}
