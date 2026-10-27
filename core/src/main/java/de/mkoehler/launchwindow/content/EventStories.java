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
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * The little stories told about random events (the user's request, 2026-10-10): a headline and a
 * sentence or two per story, several per event kind, so the turn report says what happened and not
 * only what it cost. A player sees no story twice until every story of its event has run, then that
 * event's stories start over. A story can wait for a goal (2026-10-27): the agency's hardware isn't
 * overhead before it has a satellite in orbit. Read from {@code content/event-stories.json} on the classpath.
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
     * @param id           its id, never reused
     * @param kind         the event it tells
     * @param headline     the headline
     * @param text         a sentence or two
     * @param requiresGoal a goal the agency must have reached before the story may run (its hardware
     *                     overhead needs a satellite in orbit; 2026-10-27), or {@code null} for none.
     *                     The stories file is outside the pack, so the goal must exist in every pack
     */
    public record Story(String id, RandomEventKind kind, String headline, String text, String requiresGoal) {

        /**
         * Validates the story.
         *
         * @throws NullPointerException if a field other than {@code requiresGoal} is {@code null}
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
        return pick(kind, random, used, story -> true);
    }

    /**
     * Picks a usable story of an event that the player hasn't been shown yet, and records it as
     * shown; a story is unusable while the goal it {@linkplain Story#requiresGoal requires} isn't
     * reached. Once every story of that event has run, its stories start over. When the only fresh
     * stories are unusable, the usable one shown longest ago runs again and moves to the back of the
     * record, so the unusable ones keep their turn. The same rules as {@link PrPress#pick(PrPress.Tone,
     * RandomGenerator, List, Predicate)}: keep the two alike.
     *
     * @param kind   the event
     * @param random the random source
     * @param used   the ids already shown; mutated in place
     * @param usable which stories can run now
     * @return the story's id, or {@code null} if no story of the event is usable
     */
    public String pick(RandomEventKind kind, RandomGenerator random, List<String> used, Predicate<Story> usable) {
        Objects.requireNonNull(used, "used must not be null");
        Objects.requireNonNull(usable, "usable must not be null");
        used.removeIf(id -> byId(id).isEmpty());
        List<Story> ofKind = stories.stream().filter(story -> story.kind() == kind).toList();
        List<Story> usableOfKind = ofKind.stream().filter(usable).toList();
        if (usableOfKind.isEmpty()) {
            return null;
        }
        List<Story> fresh = ofKind.stream().filter(story -> !used.contains(story.id())).toList();
        if (fresh.isEmpty()) {
            ofKind.forEach(story -> used.remove(story.id()));
            fresh = ofKind;
        }
        List<Story> freshUsable = fresh.stream().filter(usable).toList();
        if (freshUsable.isEmpty()) {
            // Every usable story already ran: rerun the one shown longest ago and move it to the
            // back, so the usable ones keep cycling without repeats while the rest wait their turn.
            String oldest = used.stream().filter(id -> usableOfKind.stream().anyMatch(s -> s.id().equals(id)))
                .findFirst().orElseThrow();
            used.remove(oldest);
            used.add(oldest);
            return oldest;
        }
        String id = freshUsable.get(random.nextInt(freshUsable.size())).id();
        used.add(id);
        return id;
    }
}
