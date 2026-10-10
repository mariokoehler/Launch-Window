package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.RandomEventKind;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * The little stories told about random events (the user's request, 2026-10-10): a headline and a
 * sentence or two per story, several per event kind, so the turn report says what happened and not
 * only what it cost. A player sees no story twice until every story of its event has run, then that
 * event's stories start over. Read from {@code content/event-stories.json} on the classpath.
 * Deliberately outside {@link ContentPackLoader#FILES} (not hashed, not frozen), like
 * {@code strings.json} and {@link PrPress}: turn reports store story ids, so an id is never renamed
 * or reused.
 *
 * <p>Stories fit both agencies and carry no figures: the report adds the event's own line with the
 * numbers. Placeholders: {@value #NAME} for the person an event struck, {@value #COMPONENT} for the
 * component a reliability setback hit, and {@value #GROUP} for the department whose morale moved.</p>
 *
 * @param stories every story
 * @author Mario Koehler
 */
@JsonIgnoreProperties("_comment")
public record EventStories(List<Story> stories) {

    /** The stories' file, under {@code content/}. */
    public static final String FILE = "event-stories.json";

    /** No stories. */
    public static final EventStories EMPTY = new EventStories(List.of());

    /** The struck person's name. */
    public static final String NAME = "{name}";

    /** The component a reliability setback hit. */
    public static final String COMPONENT = "{component}";

    /** The department whose morale moved, plural ("engineers"). */
    public static final String GROUP = "{group}";

    /**
     * How many stories each event kind has: sized from the bot playouts (2026-10-10) so a Moon race
     * rarely repeats one. {@code EventStoriesTest} holds the file to these.
     */
    public static final Map<RandomEventKind, Integer> SIZED = Map.ofEntries(
        Map.entry(RandomEventKind.PRESTIGE_POINTS_BOOST, 18),
        Map.entry(RandomEventKind.RD_RESULTS_BOOST, 18),
        Map.entry(RandomEventKind.REDUCED_EXPENDITURES, 12),
        Map.entry(RandomEventKind.MORALE_BOOST, 6),
        Map.entry(RandomEventKind.EXTRA_FUNDING, 5),
        Map.entry(RandomEventKind.RELIABILITY_SETBACK, 4));

    /** How many stories each event kind not in {@link #SIZED} has. */
    public static final int SIZED_OTHERWISE = 3;

    private static volatile EventStories standard;

    /**
     * One story.
     *
     * @param id       its id, never reused
     * @param kind     the event it tells
     * @param headline the headline
     * @param text     a sentence or two
     */
    public record Story(String id, RandomEventKind kind, String headline, String text) {

        /**
         * Validates the story.
         *
         * @throws NullPointerException if a field is {@code null}
         */
        public Story {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
        }

        /**
         * Returns the headline with its placeholders filled.
         *
         * @param name      the struck person's name, or {@code null}
         * @param component the component's name, or {@code null}
         * @param group     the department, plural, or {@code null}
         * @return the headline
         */
        public String headlineFor(String name, String component, String group) {
            return fill(headline, name, component, group);
        }

        /**
         * Returns the text with its placeholders filled.
         *
         * @param name      the struck person's name, or {@code null}
         * @param component the component's name, or {@code null}
         * @param group     the department, plural, or {@code null}
         * @return the text
         */
        public String textFor(String name, String component, String group) {
            return fill(text, name, component, group);
        }

        private static String fill(String template, String name, String component, String group) {
            return template.replace(NAME, name == null ? "someone" : name)
                .replace(COMPONENT, component == null ? "a component" : component)
                .replace(GROUP, group == null ? "staff" : group);
        }
    }

    /**
     * Reads the stories.
     *
     * @param stories the stories
     */
    @JsonCreator
    public EventStories(@JsonProperty("stories") List<Story> stories) {
        this.stories = List.copyOf(Objects.requireNonNull(stories, "stories must not be null"));
    }

    /**
     * Returns the stories shipped on the classpath, read once.
     *
     * @return the stories, {@link #EMPTY} if the file is missing
     * @throws UncheckedIOException if the file can't be read
     */
    public static EventStories standard() {
        EventStories loaded = standard;
        if (loaded == null) {
            try (InputStream in = EventStories.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, EventStories.class);
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
    public static EventStories load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new ObjectMapper().readValue(in, EventStories.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a story by id.
     *
     * @param id the id, or {@code null}
     * @return the story, empty for {@code null} or an id no longer in the file
     */
    public Optional<Story> byId(String id) {
        return id == null ? Optional.empty() : stories.stream().filter(story -> story.id().equals(id)).findFirst();
    }

    /**
     * Picks a story of an event that the player hasn't been shown yet, and records it as shown.
     * Once every story of that event has run, its stories start over. Ids of stories no longer in
     * the file are dropped from the record.
     *
     * @param kind   the event
     * @param random the random source
     * @param used   the ids already shown; mutated in place
     * @return the story's id, or {@code null} if the event has no story
     */
    public String pick(RandomEventKind kind, RandomGenerator random, List<String> used) {
        Objects.requireNonNull(used, "used must not be null");
        used.removeIf(id -> byId(id).isEmpty());
        List<Story> ofKind = stories.stream().filter(story -> story.kind() == kind).toList();
        if (ofKind.isEmpty()) {
            return null;
        }
        List<Story> fresh = ofKind.stream().filter(story -> !used.contains(story.id())).toList();
        if (fresh.isEmpty()) {
            ofKind.forEach(story -> used.remove(story.id()));
            fresh = ofKind;
        }
        String id = fresh.get(random.nextInt(fresh.size())).id();
        used.add(id);
        return id;
    }
}
