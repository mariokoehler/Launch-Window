package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The archive ({@code design.md} &sect;2.12, {@code ui-spec.md} &sect;4.19): pages about the real
 * missions that sent back something memorable, each opened in a game by its goal, the first time any
 * agency holds it. The pages are {@code assets/content/archive.json}, outside the content pack like the
 * stories; when the goals were reached comes from the server ({@link ArchiveUnlock}).
 *
 * @author Mario Koehler
 */
public final class Archive {

    /** The pages' file, under {@code assets/content/}. */
    public static final String FILE = "archive.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static volatile Archive standard;

    /**
     * A row of a page's fact box.
     *
     * @param label the label, such as "Launched"
     * @param value the value, one line per line
     */
    public record Fact(String label, String value) {
    }

    /**
     * A section of a page's text.
     *
     * @param heading its heading
     * @param text    its paragraph
     */
    public record Section(String heading, String text) {
    }

    /**
     * A picture on a page.
     *
     * @param file    the file, under {@code assets/}
     * @param caption what it shows
     * @param credit  its credit line, with its id
     */
    public record Picture(String file, String caption, String credit) {
    }

    /**
     * A page's sound.
     *
     * @param file    the file, under {@code assets/}
     * @param caption what it is
     * @param credit  its credit
     * @param seconds its length
     */
    public record Sound(String file, String caption, String credit, int seconds) {
    }

    /**
     * A page.
     *
     * @param id       the page's id, also its thumbnail's name ({@code ui/archive/<id>-thumb.jpg})
     * @param goalId   the goal that opens it
     * @param title    its title, such as "Apollo 8: Earthrise"
     * @param agency   who flew it, such as "NASA"
     * @param date     when, such as "December 1968"
     * @param when     the year and month it's ordered by, such as "1968-12"
     * @param summary  one line for its card
     * @param facts    the fact box's rows
     * @param firsts   the mission's firsts
     * @param sections the text
     * @param sources  where the facts come from
     * @param images   its pictures, the first also its thumbnail's
     * @param sound    its sound, or {@code null}
     */
    public record Page(String id, String goalId, String title, String agency, String date, String when,
                       String summary, List<Fact> facts, List<String> firsts, List<Section> sections,
                       List<String> sources, List<Picture> images, Sound sound) {

        /**
         * Returns the year the page's card sits under on the timeline.
         *
         * @return the year
         */
        public int year() {
            return Integer.parseInt(when.substring(0, 4));
        }

        /**
         * Returns the thumbnail's path under {@code assets/ui/}.
         *
         * @return the path
         */
        public String thumbnail() {
            return "archive/" + id + "-thumb.jpg";
        }
    }

    /**
     * The pages' file.
     *
     * @param pages every page
     */
    public record File(List<Page> pages) {
    }

    /**
     * A page in this game: open or not, since when, and whether this player's agency flew it.
     *
     * @param page     the page
     * @param turn     the turn its goal was first reached in the game, or the race decided; {@code null}
     *                 while it's locked
     * @param byYou    whether this player's agency was among the first to reach it
     * @param atTheEnd whether it opened because the race was decided, with nobody having reached it
     */
    public record Entry(Page page, Integer turn, boolean byYou, boolean atTheEnd) {

        /**
         * An entry opened by its goal, or locked.
         *
         * @param page  the page
         * @param turn  the turn its goal was first reached, or {@code null}
         * @param byYou whether this player's agency was among the first
         */
        public Entry(Page page, Integer turn, boolean byYou) {
            this(page, turn, byYou, false);
        }

        /**
         * Returns whether the page is open.
         *
         * @return whether it is
         */
        public boolean open() {
            return turn != null;
        }
    }

    private final List<Page> pages;

    private Archive(List<Page> pages) {
        List<Page> sorted = new ArrayList<>(pages);
        sorted.sort(Comparator.comparing(Page::when));
        this.pages = List.copyOf(sorted);
    }

    /**
     * Builds an archive of the given pages (tests).
     *
     * @param pages the pages
     * @return the archive
     */
    public static Archive of(List<Page> pages) {
        return new Archive(pages);
    }

    /**
     * The game's archive, read from the classpath once; empty without the file.
     *
     * @return the archive
     */
    public static Archive standard() {
        Archive loaded = standard;
        if (loaded == null) {
            try (InputStream in = Archive.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = new Archive(in == null ? List.of() : MAPPER.readValue(in, File.class).pages());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read content/" + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads the archive from the repository's folders (tests and tools).
     *
     * @param assets the {@code assets} folder
     * @return the archive
     */
    public static Archive load(Path assets) {
        try {
            return new Archive(MAPPER.readValue(Files.readString(assets.resolve("content").resolve(FILE)), File.class)
                .pages());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns every page, in the order the missions flew.
     *
     * @return the pages
     */
    public List<Page> pages() {
        return pages;
    }

    /**
     * Returns the pages a game can open: those whose goal its content pack has.
     *
     * @param catalog the game's content
     * @return the pages, in the order the missions flew
     */
    public List<Page> pages(ContentCatalog catalog) {
        return pages.stream().filter(page -> catalog.goal(page.goalId()).isPresent()).toList();
    }

    /**
     * Returns the goals the server is asked about.
     *
     * @param catalog the game's content
     * @return the goals of the game's pages
     */
    public List<String> goalIds(ContentCatalog catalog) {
        return pages(catalog).stream().map(Page::goalId).distinct().toList();
    }

    /**
     * Returns a page by its id.
     *
     * @param id the page's id
     * @return the page, or {@code null}
     */
    public Page page(String id) {
        return pages.stream().filter(page -> page.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns the game's pages with what the server said about their goals.
     *
     * @param catalog the game's content
     * @param unlocks the server's answer, or {@code null} before it came: every page locked
     * @return one entry per page, in the order the missions flew
     */
    public List<Entry> entries(ContentCatalog catalog, List<ArchiveUnlock> unlocks) {
        Map<String, ArchiveUnlock> byGoal = new HashMap<>();
        for (ArchiveUnlock unlock : unlocks == null ? List.<ArchiveUnlock>of() : unlocks) {
            byGoal.put(unlock.goalId(), unlock);
        }
        List<Entry> entries = new ArrayList<>();
        for (Page page : pages(catalog)) {
            ArchiveUnlock unlock = byGoal.get(page.goalId());
            entries.add(unlock == null ? new Entry(page, null, false)
                : new Entry(page, unlock.turn(), unlock.byYou(), unlock.atTheEnd()));
        }
        return entries;
    }

    /**
     * Returns the pages a turn opened: those whose goal was first reached in the game that turn, for
     * the turn report's "New in the archive" card.
     *
     * @param catalog the game's content
     * @param unlocks the server's answer, or {@code null}
     * @param turn    the report's turn
     * @return the entries, in the order the missions flew; empty without an answer
     */
    public List<Entry> openedIn(ContentCatalog catalog, List<ArchiveUnlock> unlocks, long turn) {
        return entries(catalog, unlocks).stream().filter(entry -> entry.open() && entry.turn() == turn).toList();
    }

    /**
     * Returns the entry for a page.
     *
     * @param entries the game's entries
     * @param pageId  the page
     * @return its entry
     */
    public static Entry entry(List<Entry> entries, String pageId) {
        return entries.stream().filter(entry -> entry.page().id().equals(pageId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No page " + Objects.requireNonNull(pageId)));
    }
}
