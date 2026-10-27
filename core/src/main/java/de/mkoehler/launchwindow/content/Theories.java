package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

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

/**
 * Theories of the day ({@code ui-spec.md} &sect;4.18.2, {@code design.md} &sect;1.1): the questions the real
 * space age argued over before a flight settled them, such as whether the Moon's plains are deep dust. A
 * player's scientists raise one when the player first reaches one of its {@code raisedBy} goals (a card in
 * that turn's report); the first flight in the game to reach one of its {@code answeredBy} goals settles it for
 * everyone: a science page in the paper for a question the public followed, a line in the report's news for one
 * argued over in the design offices. Flavour only: nothing in the rules reads them.
 *
 * <p>They live in {@code assets/content/theories.json}, outside the frozen content pack like the papers' words.
 * Printed science pages store a theory's id: never rename or reuse one.</p>
 *
 * @param theories every theory
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record Theories(List<Theory> theories) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "theories.json";

    /**
     * The prefix of a program category's key in the turns {@link Theory#raisedOn} reads: the turn the player first
     * opened a program of that category, beside the goals' turns (2026-10-27).
     */
    public static final String CATEGORY = "category:";

    /** No theories: what a missing file reads as. */
    public static final Theories EMPTY = new Theories(List.of());

    private static volatile Theories standard;

    /**
     * Copies the list, reading a missing one as empty.
     */
    public Theories {
        theories = theories == null ? List.of() : List.copyOf(theories);
    }

    /** How far a theory reached, which picks how its answer is told. */
    public enum Reach {
        /** The public followed it: its answer is a science page in the paper. */
        PUBLIC,
        /** It was argued over among engineers: its answer is a line in the report's news. */
        TECHNICAL
    }

    /**
     * One theory.
     *
     * @param id            a stable id, never reused
     * @param reach         how far it reached
     * @param raisedBy      the goals whose first reaching raises it for a player
     * @param answeredBy    the goals whose first reaching in the game settles it
     * @param title         its question, short
     * @param question      the scientists' briefing that raises it
     * @param settledBy     what would settle it, to follow "What would settle it: "
     * @param answer        the answer in one sentence
     * @param headline      a public theory's science page headline (set in capitals)
     * @param headlineShort a shorter headline, for when the long one doesn't fit two lines
     * @param deck          the science page's deck ({@code {agency}})
     * @param article       the science page's paragraphs ({@code {agency}}, {@code {season}})
     * @param notice        a technical theory's news line ({@code {agency}})
     * @param historyNote   the real history, checked in {@code design.md} &sect;1.1
     * @param archive       the archive pages about the flights that settled it ({@code archive.json} ids), maybe none
     * @param raisedByCategories program categories whose first opening also raises it for a player, so a question a
     *                      goal would raise and settle at once comes earlier (2026-10-27); maybe none
     */
    public record Theory(String id, Reach reach, List<String> raisedBy, List<String> answeredBy, String title,
                         String question, String settledBy, String answer, String headline, String headlineShort,
                         String deck, List<String> article, String notice, String historyNote,
                         List<String> archive, List<String> raisedByCategories) {

        /**
         * Copies the lists, reading missing ones as empty.
         */
        public Theory {
            archive = archive == null ? List.of() : List.copyOf(archive);
            raisedBy = raisedBy == null ? List.of() : List.copyOf(raisedBy);
            raisedByCategories = raisedByCategories == null ? List.of() : List.copyOf(raisedByCategories);
            answeredBy = answeredBy == null ? List.of() : List.copyOf(answeredBy);
            article = article == null ? List.of() : List.copyOf(article);
        }

        /**
         * Returns whether a game's content pack can raise and settle this theory.
         *
         * @param inPack whether the pack has a goal
         * @return whether one of each kind of goal is there
         */
        public boolean playable(Predicate<String> inPack) {
            return raisedBy.stream().anyMatch(inPack) && answeredBy.stream().anyMatch(inPack);
        }

        /**
         * Returns the turn a player raised this theory: the earliest turn any of its {@code raisedBy} goals was
         * reached, or a program of one of its {@code raisedByCategories} was first opened.
         *
         * @param turns goals and the turn each was reached, and {@link #CATEGORY} plus a category for the turn its
         *              first program was opened
         * @return the turn, if one was reached
         */
        public Optional<Integer> raisedOn(Map<String, Integer> turns) {
            return java.util.stream.Stream.concat(raisedBy.stream(), raisedByCategories.stream().map(c -> CATEGORY + c))
                .map(turns::get).filter(Objects::nonNull).min(Integer::compare);
        }
    }

    /**
     * Returns the theories bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the theories, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static Theories standard() {
        Theories loaded = standard;
        if (loaded == null) {
            try (InputStream in = Theories.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, Theories.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Sets the theories {@link #standard()} returns, for tests and tools that have no assets on the classpath.
     * Put {@link #EMPTY} back afterwards.
     *
     * @param theories the theories
     */
    public static void setStandard(Theories theories) {
        standard = Objects.requireNonNull(theories);
    }

    /**
     * Reads the theories from a file.
     *
     * @param file the file
     * @return the theories
     * @throws UncheckedIOException if the file can't be read
     */
    public static Theories load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), Theories.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a theory by its id.
     *
     * @param id the id
     * @return the theory, if there is one
     */
    public Optional<Theory> theory(String id) {
        return theories.stream().filter(t -> t.id().equals(id)).findFirst();
    }
}
