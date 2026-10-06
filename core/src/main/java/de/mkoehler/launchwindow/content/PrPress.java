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
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Press stories about an astronaut on PR duty ({@code design.md} &sect;2.2(c), user decision
 * 2026-10-06): now and then a tour makes the papers, for better or worse. The stories live in
 * {@code assets/content/pr-press.json}, outside the frozen content pack, so they can be rewritten
 * without touching a running game. A few are nods to real events ({@code design.md} &sect;1.1). A
 * turn report stores a story's id; the client fills in the astronaut's name.
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
     * What a press story does besides its prestige ({@code design.md} &sect;2.2(c), user decision
     * 2026-10-06). Turn reports store applied effects by kind: constants are only ever appended,
     * never renamed or reordered.
     */
    public enum EffectKind {
        /** Replaces the share of the season's yield a good story adds (amount: the share, 1.0 = full yield). */
        PRESTIGE_SHARE,
        /** A bigger hiring pool (target: the group; amount: extra candidates; seasons: refreshes it lasts). */
        HIRING_POOL,
        /** Candidates from the best quality band (target: the group; amount: how many; seasons: refreshes). */
        STANDOUT_CANDIDATE,
        /** Morale for a whole group (target: the group; amount: points, signed). */
        GROUP_MORALE,
        /** Morale for the astronaut in the story (amount: points, signed). */
        ASTRONAUT_MORALE,
        /** A skill of the astronaut in the story (target: the skill; amount: points, signed). */
        ASTRONAUT_SKILL,
        /** Cash at once (amount: a share of the season's funding rate). */
        FUNDS,
        /** A higher funding rate until the next review (amount: a share of the rate). */
        FUNDING_RATE,
        /** R&amp;D results scaled (amount: the factor; seasons: research seasons it covers). */
        RD_RESULTS,
        /** Running costs scaled (amount: the factor; seasons: budgets it covers). */
        RUNNING_COSTS,
        /** One component in R&amp;D loses seasons of its expected progress (amount: seasons lost). */
        RESEARCH_SETBACK
    }

    /**
     * One effect of a story. In {@code pr-press.json} it holds the authored values; in a turn
     * report ({@code PrDutyPerformed}) it holds what was applied: {@link EffectKind#FUNDS} and
     * {@link EffectKind#FUNDING_RATE} in money, {@link EffectKind#RESEARCH_SETBACK} with the
     * component as target and the reliability points lost as amount.
     *
     * @param kind    what it does
     * @param target  a group, skill or component name, or {@code null} when the kind needs none
     * @param amount  the size, as the kind describes
     * @param seasons how long it lasts, 0 for a one-off
     */
    public record Effect(EffectKind kind, String target, double amount, int seasons) {

        /**
         * Checks the kind.
         */
        public Effect {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    /**
     * One press story.
     *
     * @param id       a stable id, never reused, since reports store it
     * @param tone     whether it helps or hurts
     * @param headline the headline, with {@link #NAME} for the astronaut
     * @param text     one or two sentences, with {@link #NAME} for the astronaut
     * @param effects  what it does besides prestige, possibly none
     */
    public record Story(String id, Tone tone, String headline, String text, List<Effect> effects) {

        /**
         * Checks the fields; missing effects read as none.
         */
        public Story {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(tone, "tone must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
            effects = effects == null ? List.of() : List.copyOf(effects);
        }

        /**
         * A story with no effects besides prestige.
         *
         * @param id       the id
         * @param tone     the tone
         * @param headline the headline
         * @param text     the text
         */
        public Story(String id, Tone tone, String headline, String text) {
            this(id, tone, headline, text, List.of());
        }

        /**
         * Returns the effects of one kind.
         *
         * @param kind the kind
         * @return the effects, in file order
         */
        public List<Effect> effects(EffectKind kind) {
            return effects.stream().filter(effect -> effect.kind() == kind).toList();
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
     * Picks a story of one tone that the player hasn't been shown yet, and records it as shown.
     * Once every story of that tone has run, the tone's stories start over (user decision
     * 2026-10-06). Ids of stories no longer in the file are dropped from the record.
     *
     * @param tone   the tone
     * @param random the random source
     * @param used   the ids already shown; mutated in place
     * @return the story's id, or {@code null} if there's none of that tone
     */
    public String pick(Tone tone, RandomGenerator random, List<String> used) {
        return pick(tone, random, used, story -> true);
    }

    /**
     * Picks a usable story of one tone that the player hasn't been shown yet, and records it as
     * shown. Once every story of that tone has run, the tone's stories start over. When the only
     * fresh stories are unusable this season (an effect with nothing to act on), the usable one
     * shown longest ago runs again and moves to the back of the record, so the unusable ones keep
     * their turn and the usable ones still don't repeat back to back.
     *
     * @param tone   the tone
     * @param random the random source
     * @param used   the ids already shown; mutated in place
     * @param usable which stories can run now
     * @return the story's id, or {@code null} if no story of that tone is usable
     */
    public String pick(Tone tone, RandomGenerator random, List<String> used, Predicate<Story> usable) {
        Objects.requireNonNull(used, "used must not be null");
        Objects.requireNonNull(usable, "usable must not be null");
        used.removeIf(id -> byId(id).isEmpty());
        List<Story> ofTone = stories.stream().filter(story -> story.tone() == tone).toList();
        List<Story> usableOfTone = ofTone.stream().filter(usable).toList();
        if (usableOfTone.isEmpty()) {
            return null;
        }
        List<Story> fresh = ofTone.stream().filter(story -> !used.contains(story.id())).toList();
        if (fresh.isEmpty()) {
            ofTone.forEach(story -> used.remove(story.id()));
            fresh = ofTone;
        }
        List<Story> freshUsable = fresh.stream().filter(usable).toList();
        if (freshUsable.isEmpty()) {
            // Every usable story already ran: rerun the one shown longest ago and move it to the
            // back, so the usable ones keep cycling without repeats while the rest wait their turn.
            String oldest = used.stream().filter(id -> usableOfTone.stream().anyMatch(s -> s.id().equals(id)))
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
