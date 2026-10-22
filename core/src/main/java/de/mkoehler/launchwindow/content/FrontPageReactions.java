package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The words of the follow-up pages, "The world reacts" ({@code ui-spec.md} &sect;4.18, G17): what the
 * paper prints the turn after a First, from the rivals' statements about it. They live in
 * {@code assets/content/front-page-reactions.json}, outside the frozen content pack like the Firsts'
 * own words, so a correction reaches running games.
 *
 * <p>An article reads as one story, not a list: its headline, deck and lead are written whole for the
 * First and the mood of the statements (all gracious, a challenge, mixed, dismissive or silent); each
 * statement is told by a stance line, with a sentence leading into it when the stance changes, and
 * sometimes a clause on where the rival stands in the race, from public facts only (the goals it
 * holds, never what the Intelligence Office sells). The server picks every piece and stores its id;
 * the client looks the text up here.</p>
 *
 * @param firsts   every First's own words
 * @param stances  the lines per stance (not {@link FrontPage.Stance#DECLINED})
 * @param links    the sentences leading from one stance to another
 * @param declined the lines naming the rivals who gave no statement
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record FrontPageReactions(List<First> firsts, Map<FrontPage.Stance, StanceLines> stances, List<Link> links,
                                 Declined declined) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "front-page-reactions.json";

    /** No words: what a missing file reads as. */
    public static final FrontPageReactions EMPTY = new FrontPageReactions(List.of(), Map.of(), List.of(),
        new Declined(List.of(), List.of()));

    private static volatile FrontPageReactions standard;

    /**
     * Copies the collections, reading missing ones as empty.
     */
    public FrontPageReactions {
        firsts = firsts == null ? List.of() : List.copyOf(firsts);
        stances = stances == null ? Map.of() : Map.copyOf(stances);
        links = links == null ? List.of() : List.copyOf(links);
        declined = declined == null ? new Declined(List.of(), List.of()) : declined;
    }

    /**
     * The mood of a round of statements, which picks the story.
     */
    public enum Mood {
        /** Only congratulations. */
        GRACIOUS,
        /** Someone promises to catch up, and nobody belittles the feat. */
        CHALLENGE,
        /** Congratulations and belittling both. */
        MIXED,
        /** Belittling, and no congratulations. */
        DISMISSIVE,
        /** Nothing but pointed refusals. */
        SILENT;

        /**
         * Returns the mood of the statements given.
         *
         * @param stances the stances given, none {@link FrontPage.Stance#DECLINED}; at least one
         * @return the mood
         */
        public static Mood of(Collection<FrontPage.Stance> stances) {
            boolean gracious = stances.contains(FrontPage.Stance.ENTHUSIASTIC)
                || stances.contains(FrontPage.Stance.POLITE);
            boolean downplay = stances.contains(FrontPage.Stance.DOWNPLAY);
            if (downplay) {
                return gracious ? MIXED : DISMISSIVE;
            }
            if (stances.contains(FrontPage.Stance.NEXT)) {
                return CHALLENGE;
            }
            return gracious ? GRACIOUS : SILENT;
        }
    }

    /**
     * Where a rival stands in the race, for the clause about it. Public facts only: the goals it holds.
     */
    public enum DetailKind {
        /** It reached the First itself in the turn it gave its statement. */
        MATCHED,
        /** It holds the goal before this First ({@link First#before()}), or for the first First any goal. */
        HELD_BEFORE,
        /** It doesn't. */
        LACKS_BEFORE
    }

    /**
     * One First's words.
     *
     * @param goal    the First's goal
     * @param before  the First before it in the race, for the detail clauses; {@code null} for the first
     * @param stories the stories, at least one per mood
     * @param details the clauses about a rival, per kind; each starts with ", " and ends with ","
     * @param close   the closing paragraphs; the page drops them from the end if they don't fit
     */
    public record First(String goal, String before, List<Story> stories, Map<DetailKind, List<Line>> details,
                        List<String> close) {

        /**
         * Copies the collections, reading missing ones as empty.
         */
        public First {
            stories = stories == null ? List.of() : List.copyOf(stories);
            details = details == null ? Map.of() : Map.copyOf(details);
            close = close == null ? List.of() : List.copyOf(close);
        }

        /**
         * Returns a story by its id.
         *
         * @param id the id
         * @return the story, if this First has it
         */
        public Optional<Story> story(String id) {
            return stories.stream().filter(s -> s.id().equals(id)).findFirst();
        }

        /**
         * Returns the clauses of one kind.
         *
         * @param kind the kind
         * @return the clauses, maybe empty
         */
        public List<Line> detailsOf(DetailKind kind) {
            return details.getOrDefault(kind, List.of());
        }
    }

    /**
     * The top of a follow-up's article, for one First and one mood. The headline must read with one
     * agency or two in {@code {agency}} (a tie); the deck and lead have a form for a single statement.
     *
     * @param id            a stable id, never reused, since printed pages store it
     * @param mood          the mood it's for
     * @param headline      the headline (set in capitals on the page)
     * @param headlineShort a shorter headline, for when the long one doesn't fit two lines
     * @param deck          the subheadline, for two statements or more
     * @param deckOne       the subheadline, for a single statement
     * @param lead          the opening paragraphs, for two statements or more
     * @param leadOne       the opening paragraphs, for a single statement
     */
    public record Story(String id, Mood mood, String headline, String headlineShort, String deck, String deckOne,
                        List<String> lead, List<String> leadOne) {

        /**
         * Copies the lists, reading missing ones as empty.
         */
        public Story {
            lead = lead == null ? List.of() : List.copyOf(lead);
            leadOne = leadOne == null ? List.of() : List.copyOf(leadOne);
        }
    }

    /**
     * One line, with the stable id printed pages store.
     *
     * @param id   a stable id, never reused
     * @param text the text, with tokens
     */
    public record Line(String id, String text) {
    }

    /**
     * The lines of one stance.
     *
     * @param quoted lines for a statement with a quote, each ending in {@code {quote}}
     * @param plain  lines for a statement without one
     * @param also   lines naming several rivals of this stance at once ({@code {rivals}}), for the
     *               statements a crowded page can't tell one by one
     */
    public record StanceLines(List<Line> quoted, List<Line> plain, List<Line> also) {

        /**
         * Copies the lists, reading missing ones as empty.
         */
        public StanceLines {
            quoted = quoted == null ? List.of() : List.copyOf(quoted);
            plain = plain == null ? List.of() : List.copyOf(plain);
            also = also == null ? List.of() : List.copyOf(also);
        }
    }

    /**
     * A sentence leading from a statement of one stance to the next of another.
     *
     * @param id   a stable id, never reused
     * @param from the stance before
     * @param to   the stance it leads into
     * @param text the sentence
     */
    public record Link(String id, FrontPage.Stance from, FrontPage.Stance to, String text) {
    }

    /**
     * The lines naming the rivals who gave no statement.
     *
     * @param one  lines for one rival ({@code {rivals}} is its name)
     * @param many lines for several ({@code {rivals}} is the list)
     */
    public record Declined(List<Line> one, List<Line> many) {

        /**
         * Copies the lists, reading missing ones as empty.
         */
        public Declined {
            one = one == null ? List.of() : List.copyOf(one);
            many = many == null ? List.of() : List.copyOf(many);
        }
    }

    /**
     * Returns the words bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the words, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static FrontPageReactions standard() {
        FrontPageReactions loaded = standard;
        if (loaded == null) {
            try (InputStream in = FrontPageReactions.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, FrontPageReactions.class);
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
     * @param words the words
     */
    public static void setStandard(FrontPageReactions words) {
        standard = Objects.requireNonNull(words);
    }

    /**
     * Reads the words from a file.
     *
     * @param file the file
     * @return the words
     * @throws UncheckedIOException if the file can't be read
     */
    public static FrontPageReactions load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), FrontPageReactions.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a First's words.
     *
     * @param goalId the First's goal
     * @return the words, if the file has them
     */
    public Optional<First> first(String goalId) {
        return firsts.stream().filter(f -> f.goal().equals(goalId)).findFirst();
    }

    /**
     * Returns a stance's lines.
     *
     * @param stance the stance
     * @return its lines, maybe none
     */
    public StanceLines linesOf(FrontPage.Stance stance) {
        StanceLines lines = stances.get(stance);
        return lines == null ? new StanceLines(List.of(), List.of(), List.of()) : lines;
    }

    /**
     * Returns the sentences leading from one stance to another.
     *
     * @param from the stance before
     * @param to   the next stance
     * @return the sentences, maybe none
     */
    public List<Link> linksBetween(FrontPage.Stance from, FrontPage.Stance to) {
        return links.stream().filter(l -> l.from() == from && l.to() == to).toList();
    }

    /**
     * Returns whether a line tells several rivals at once: an "also" line or a "declined" line.
     *
     * @param id the line's id
     * @return whether it does
     */
    public boolean shared(String id) {
        return stances.values().stream().anyMatch(s -> s.also().stream().anyMatch(l -> l.id().equals(id)))
            || declined.one().stream().anyMatch(l -> l.id().equals(id))
            || declined.many().stream().anyMatch(l -> l.id().equals(id));
    }

    /**
     * Returns a line's text by its id: a stance line, an "also" line, a "declined" line, a link or a
     * detail clause.
     *
     * @param id the id
     * @return the text, if the file knows it
     */
    public Optional<String> text(String id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Line> all = new ArrayList<>();
        stances.values().forEach(s -> {
            all.addAll(s.quoted());
            all.addAll(s.plain());
            all.addAll(s.also());
        });
        all.addAll(declined.one());
        all.addAll(declined.many());
        firsts.forEach(f -> f.details().values().forEach(all::addAll));
        links.forEach(l -> all.add(new Line(l.id(), l.text())));
        return all.stream().filter(l -> l.id().equals(id)).map(Line::text).findFirst();
    }
}
