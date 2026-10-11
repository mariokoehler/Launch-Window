package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Season;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The words of the Firsts' front pages ({@code ui-spec.md} &sect;4.18, G14): one entry per First,
 * in race order, with its caption, its "In brief" lines, its stories and a story for a tie. They live
 * in {@code assets/content/front-pages.json}, outside the frozen content pack like the display text,
 * so a correction reaches running games.
 *
 * <p>The server prints a page while resolving a turn and stores only the story's id; the client
 * looks the text up here and fills in the tokens. An id this file doesn't know shows the page
 * without an article.</p>
 *
 * @param weather lines for the left ear, per season
 * @param firsts  every First, in race order
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record FrontPages(Map<Season, List<String>> weather, List<First> firsts) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "front-pages.json";

    /** No Firsts: what a missing file reads as. */
    public static final FrontPages EMPTY = new FrontPages(Map.of(), List.of());

    private static volatile FrontPages standard;

    /**
     * One First and what its page says.
     *
     * @param goal    the goal whose first achievement prints the page
     * @param caption the photo's caption (tokens allowed)
     * @param brief   the two "In brief" lines
     * @param stories the stories for one agency (at least two)
     * @param tie     the story for two or more agencies at once
     */
    public record First(String goal, String caption, List<String> brief, List<Story> stories, Story tie) {

        /**
         * Returns the story with an id, the tie story included.
         *
         * @param id the story's id
         * @return the story, if this First has it
         */
        public Optional<Story> story(String id) {
            if (tie != null && tie.id().equals(id)) {
                return Optional.of(tie);
            }
            return stories.stream().filter(s -> s.id().equals(id)).findFirst();
        }
    }

    /**
     * One story. Its article reads: {@code lead}, then {@code also} when another First is folded in,
     * then {@code rivals2} or {@code rivals1} when the game has that many rivals, then {@code close}.
     *
     * @param id            a stable id, never reused, since printed pages store it
     * @param headline      the headline (set in capitals on the page)
     * @param headlineShort a shorter headline, for when the long one doesn't fit two lines
     * @param deck          the subheadline
     * @param lead          the opening paragraphs
     * @param also          the paragraph naming the Firsts folded into this page, or empty
     * @param rivals2       a paragraph naming two rivals
     * @param rivals1       a paragraph naming one rival
     * @param close         the closing paragraphs; the page drops them from the end if they don't fit
     */
    public record Story(String id, String headline, String headlineShort, String deck, List<String> lead,
                        String also, String rivals2, String rivals1, List<String> close) {

        /**
         * Returns the article's paragraphs in order, before the tokens are filled in.
         *
         * @param folded whether another First is folded into the page
         * @param rivals how many rivals the page names (0, 1 or 2)
         * @return the paragraphs
         */
        public List<String> paragraphs(boolean folded, int rivals) {
            List<String> paragraphs = new ArrayList<>(lead);
            if (folded && also != null && !also.isBlank()) {
                paragraphs.add(also);
            }
            if (rivals >= 2 && rivals2 != null && !rivals2.isBlank()) {
                paragraphs.add(rivals2);
            } else if (rivals >= 1 && rivals1 != null && !rivals1.isBlank()) {
                paragraphs.add(rivals1);
            }
            paragraphs.addAll(close);
            return paragraphs;
        }
    }

    /**
     * Returns the pages' words bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the words, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static FrontPages standard() {
        FrontPages loaded = standard;
        if (loaded == null) {
            try (InputStream in = FrontPages.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, FrontPages.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Sets the words {@link #standard()} returns, for tests and tools that have no assets on the
     * classpath. Put {@link #EMPTY} back afterwards.
     *
     * @param pages the words
     */
    public static void setStandard(FrontPages pages) {
        standard = Objects.requireNonNull(pages);
    }

    /**
     * Reads the words from a file.
     *
     * @param file the file
     * @return the words
     * @throws UncheckedIOException if the file can't be read
     */
    public static FrontPages load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), FrontPages.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a First by its goal.
     *
     * @param goalId the goal
     * @return the First, if the goal is one
     */
    public Optional<First> first(String goalId) {
        return firsts.stream().filter(f -> f.goal().equals(goalId)).findFirst();
    }

    /**
     * Returns where a goal stands in the race order.
     *
     * @param goalId the goal
     * @return its index, or -1 if it isn't a First
     */
    public int order(String goalId) {
        for (int i = 0; i < firsts.size(); i++) {
            if (firsts.get(i).goal().equals(goalId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the weather lines of a season.
     *
     * @param season the season
     * @return its lines, maybe empty
     */
    public List<String> weatherFor(Season season) {
        return weather == null ? List.of() : weather.getOrDefault(season, List.of());
    }
}
