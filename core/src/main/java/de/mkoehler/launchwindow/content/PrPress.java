package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Press stories about an astronaut on PR duty ({@code design.md} &sect;2.2(c), user decision
 * 2026-10-06): now and then a tour makes the papers, for better or worse. The stories live in
 * {@code assets/content/pr-press.json}, outside the frozen content pack, so they can be rewritten
 * without touching a running game. The first set is placeholders, to be replaced with varied
 * stories later. A turn report stores a story's id; the client fills in the astronaut's name.
 *
 * @param stories every story
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record PrPress(List<Story> stories) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "pr-press.json";

    /** No stories: what a missing file reads as. */
    public static final PrPress EMPTY = new PrPress(List.of());

    /** The placeholder in a story's text for the astronaut's name. */
    public static final String NAME = "{name}";

    private static volatile PrPress standard;

    /** Whether a story helps or hurts. */
    public enum Tone {
        /** A good story: the tour earns a bonus. */
        GOOD,
        /** A bad story: the tour costs prestige. */
        BAD
    }

    /**
     * One press story.
     *
     * @param id       a stable id, never reused, since reports store it
     * @param tone     whether it helps or hurts
     * @param headline the headline, with {@link #NAME} for the astronaut
     * @param text     one or two sentences, with {@link #NAME} for the astronaut
     */
    public record Story(String id, Tone tone, String headline, String text) {

        /**
         * Checks the fields.
         */
        public Story {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(tone, "tone must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
        }

        /**
         * Returns the headline about one astronaut.
         *
         * @param name the astronaut's name
         * @return the headline
         */
        public String headlineFor(String name) {
            return headline.replace(NAME, name);
        }

        /**
         * Returns the text about one astronaut.
         *
         * @param name the astronaut's name
         * @return the text
         */
        public String textFor(String name) {
            return text.replace(NAME, name);
        }
    }

    /**
     * Copies the list.
     *
     * @param stories the stories
     */
    public PrPress(@JsonProperty("stories") List<Story> stories) {
        this.stories = stories == null ? List.of() : List.copyOf(stories);
    }

    /**
     * Returns the stories bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the stories, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static PrPress standard() {
        PrPress loaded = standard;
        if (loaded == null) {
            try (InputStream in = PrPress.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, PrPress.class);
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
    public static PrPress load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), PrPress.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a story by its id.
     *
     * @param id the id, or {@code null}
     * @return the story, or empty
     */
    public Optional<Story> byId(String id) {
        return stories.stream().filter(story -> story.id().equals(id)).findFirst();
    }

    /**
     * Picks a story of one tone.
     *
     * @param tone   the tone
     * @param random the random source
     * @return the story's id, or {@code null} if there's none of that tone
     */
    public String pick(Tone tone, RandomGenerator random) {
        List<Story> candidates = stories.stream().filter(story -> story.tone() == tone).toList();
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size())).id();
    }
}
