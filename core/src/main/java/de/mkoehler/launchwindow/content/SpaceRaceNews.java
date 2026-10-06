package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Season;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Real space-race events for the turn report's news ({@code design.md} &sect;1.1, decided
 * 2026-10-06): each player's report gets one story from the season its turn played out in, picked
 * at random so players don't all read the same one, and told through their archetype's press where
 * the story has such a view. The stories live in {@code assets/content/space-race-news.json},
 * outside the frozen content pack like the display text, so a correction reaches running games.
 *
 * <p>The server picks a story while resolving a turn and stores only its id in the turn report; the
 * client looks the text up here. An id this file doesn't know (a story removed later) shows nothing.</p>
 *
 * @param stories every story, in date order
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record SpaceRaceNews(List<Story> stories) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "space-race-news.json";

    /** No stories: what a missing file reads as. */
    public static final SpaceRaceNews EMPTY = new SpaceRaceNews(List.of());

    /** Mixed into the season seed, so the pick never shares the season pass's random stream. */
    private static final long PICK_SALT = 0x5ACE_2ACE_0000_1957L;

    private static volatile SpaceRaceNews standard;

    /**
     * One real event.
     *
     * @param id       a stable id, never reused, since reports store it
     * @param year     the year it happened
     * @param season   the quarter it happened in (Winter is January to March)
     * @param headline a short headline
     * @param text     two to four sentences
     * @param nasa     how the American side saw it, or {@code null}
     * @param soviet   how the Soviet side saw it, or {@code null}
     */
    public record Story(String id, int year, Season season, String headline, String text, String nasa,
                        String soviet) {

        /**
         * Checks the required fields.
         */
        public Story {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(season, "season must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
        }

        /**
         * Returns how a player of an archetype's own side saw this story, if the story has that view.
         *
         * @param archetype the player's archetype
         * @return that side's view, or empty
         */
        public Optional<String> viewFor(Archetype archetype) {
            return Optional.ofNullable(archetype == Archetype.SOVIET ? soviet : nasa);
        }
    }

    /**
     * Copies the list.
     *
     * @param stories the stories
     */
    public SpaceRaceNews(@JsonProperty("stories") List<Story> stories) {
        this.stories = stories == null ? List.of() : List.copyOf(stories);
    }

    /**
     * Returns the stories bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the stories, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static SpaceRaceNews standard() {
        SpaceRaceNews loaded = standard;
        if (loaded == null) {
            try (InputStream in = SpaceRaceNews.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, SpaceRaceNews.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads the stories from a file.
     *
     * @param file the file
     * @return the stories
     * @throws UncheckedIOException if the file can't be read
     */
    public static SpaceRaceNews load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), SpaceRaceNews.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns the stories from one season.
     *
     * @param date the season
     * @return its stories, in file order
     */
    public List<Story> storiesFor(GameDate date) {
        return stories.stream().filter(story -> story.year() == date.year() && story.season() == date.season())
            .toList();
    }

    /**
     * Returns a story by its id.
     *
     * @param id the id, or {@code null}
     * @return the story, or empty if there's none with that id
     */
    public Optional<Story> byId(String id) {
        return stories.stream().filter(story -> story.id().equals(id)).findFirst();
    }

    /**
     * Picks one story from a season, the same one for the same seed.
     *
     * @param date the season the turn played out in
     * @param seed the player's season seed
     * @return the story's id, or {@code null} if the season has none
     */
    public String pick(GameDate date, long seed) {
        List<Story> candidates = storiesFor(date);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(new SplittableRandom(seed ^ PICK_SALT).nextInt(candidates.size())).id();
    }
}
