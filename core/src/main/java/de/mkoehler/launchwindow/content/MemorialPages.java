package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The memorial pages' words, "In memory" ({@code ui-spec.md} &sect;4.18.3, G19): what the paper prints the turn
 * after a crew is lost in flight, with the agency's statement in the tone its director chose and the rivals'
 * condolences. They live in {@code assets/content/memorial-pages.json}, outside the frozen content pack like the
 * other papers' words, so a correction reaches running games.
 *
 * <p>The server picks the statement's story and the condolences' lines and stores their ids on the page; the
 * client puts the article together from them and the crew's public facts (the lead, a sentence on each person,
 * the first the flight had reached, the stand-down, the close).</p>
 *
 * @param numbers      small numbers in words, from zero
 * @param ordinals     ordinals in words, from index 1 ("first")
 * @param headlines    the headlines (set in capitals)
 * @param decks        the decks
 * @param places       where a flight was lost, matched on its step's id, in order
 * @param elsewhere    where a flight was lost when no place matches
 * @param firsts       the firsts a lost flight may have reached, most telling first
 * @param leads        the leads
 * @param withheld     the sentence on the first the flight had reached
 * @param standDown    the sentence on the grounding
 * @param people       the sentences on each person
 * @param tones        the agency's statement's stories, per tone
 * @param spokesman    the statement printed when the agency gave none
 * @param condolences  the rivals' lines
 * @param historyNotes the history notes on the page's strip and the two dialogs
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record MemorialPages(List<String> numbers, List<String> ordinals, Headlines headlines, Decks decks,
                            List<Place> places, Place elsewhere, List<FirstPhrase> firsts, List<Line> leads,
                            Line withheld, Line standDown, People people, List<Tone> tones, Story spokesman,
                            Condolences condolences, HistoryNotes historyNotes) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "memorial-pages.json";

    /** No words: what a missing file reads as. */
    public static final MemorialPages EMPTY = new MemorialPages(null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null);

    private static volatile MemorialPages standard;

    /**
     * Copies the collections, reading missing ones as empty.
     */
    public MemorialPages {
        numbers = numbers == null ? List.of() : List.copyOf(numbers);
        ordinals = ordinals == null ? List.of() : List.copyOf(ordinals);
        headlines = headlines == null ? new Headlines("", "", "", "") : headlines;
        decks = decks == null ? new Decks("", "") : decks;
        places = places == null ? List.of() : List.copyOf(places);
        elsewhere = elsewhere == null ? new Place("flight", List.of(), "in flight", "in flight", null) : elsewhere;
        firsts = firsts == null ? List.of() : List.copyOf(firsts);
        leads = leads == null ? List.of() : List.copyOf(leads);
        withheld = withheld == null ? new Line("", "") : withheld;
        standDown = standDown == null ? new Line("", "") : standDown;
        people = people == null ? new People("", "", "", "", Map.of()) : people;
        tones = tones == null ? List.of() : List.copyOf(tones);
        spokesman = spokesman == null ? new Story("", "", "", "") : spokesman;
        condolences = condolences == null ? new Condolences("", List.of(), new Line("", "")) : condolences;
        historyNotes = historyNotes == null ? new HistoryNotes("", "", "") : historyNotes;
    }

    /**
     * The headlines: one person or several, and the short forms for when the long one doesn't fit.
     *
     * @param one       one person lost ({@code {name}}, {@code {where}})
     * @param many      several ({@code {count}}, {@code {where}})
     * @param shortOne  one person, short
     * @param shortMany several, short
     */
    public record Headlines(String one, String many, String shortOne, String shortMany) {
    }

    /**
     * The decks: with or without the first the flight had reached.
     *
     * @param plain {@code {crew}} died {@code {during}}
     * @param first the same, with {@code {after}}
     */
    public record Decks(String plain, String first) {
    }

    /**
     * Where a flight was lost, matched on its step's id.
     *
     * @param id     a stable id
     * @param match  parts of a step id that put the loss here
     * @param where  for the headline ("on the way home")
     * @param during for the deck and lead ("during reentry")
     * @param tail   an optional clause after the lead's place, or {@code null}
     */
    public record Place(String id, List<String> match, String where, String during, String tail) {

        /**
         * Copies the list, reading a missing one as empty.
         */
        public Place {
            match = match == null ? List.of() : List.copyOf(match);
            tail = tail == null ? "" : tail;
        }
    }

    /**
     * A first a lost flight may have reached.
     *
     * @param goal  the goal
     * @param first to follow "the first to" ("walk in space")
     * @param after to follow "after" ("the first walk in space")
     */
    public record FirstPhrase(String goal, String first, String after) {
    }

    /**
     * A sentence with an id.
     *
     * @param id   a stable id
     * @param text the sentence
     */
    public record Line(String id, String text) {
    }

    /**
     * The sentences on each person: the people's facts, never anything hidden.
     *
     * @param first       on a first flight
     * @param again       on a later one ({@code {nth}})
     * @param served      with the years served, on a later flight
     * @param servedFirst with the years served, on a first flight
     * @param traits      by a revealed trait's name
     */
    public record People(String first, String again, String served, String servedFirst, Map<String, String> traits) {

        /**
         * Copies the map, reading a missing one as empty.
         */
        public People {
            traits = traits == null ? Map.of() : Map.copyOf(traits);
        }
    }

    /**
     * The agency's statement's stories in one tone.
     *
     * @param tone    the tone
     * @param stories its stories
     */
    public record Tone(FrontPage.Stance tone, List<Story> stories) {

        /**
         * Copies the list, reading a missing one as empty.
         */
        public Tone {
            stories = stories == null ? List.of() : List.copyOf(stories);
        }
    }

    /**
     * One way of telling the agency's statement.
     *
     * @param id     a stable id, stored on the page
     * @param quoted the statement with the director's words ({@code {quote}} at its end)
     * @param plain  the statement without them
     * @param close  the article's last paragraph
     */
    public record Story(String id, String quoted, String plain, String close) {
    }

    /**
     * The rivals' condolences.
     *
     * @param head  the small head over them
     * @param lines their lines, per stance, quoted or not
     * @param also  the sentence naming the rest, when more sent condolences than the page tells one by one
     */
    public record Condolences(String head, List<CondolenceLine> lines, Line also) {

        /**
         * Copies the list, reading a missing one as empty.
         */
        public Condolences {
            lines = lines == null ? List.of() : List.copyOf(lines);
        }
    }

    /**
     * One rival's line.
     *
     * @param id     a stable id, stored on the page
     * @param stance condolences or a tribute
     * @param quoted whether it tells the rival's words ({@code {quote}} at its end)
     * @param text   the line
     */
    public record CondolenceLine(String id, FrontPage.Stance stance, boolean quoted, String text) {
    }

    /**
     * The history notes, checked in {@code design.md} &sect;1.1.
     *
     * @param page        on the page's strip
     * @param statement   on the agency's statement dialog
     * @param condolences on the rivals' condolence form
     */
    public record HistoryNotes(String page, String statement, String condolences) {
    }

    /**
     * Returns the words bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the words, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static MemorialPages standard() {
        MemorialPages loaded = standard;
        if (loaded == null) {
            try (InputStream in = MemorialPages.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, MemorialPages.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Sets the words {@link #standard()} returns, for tests and tools that have no assets on the classpath.
     *
     * @param words the words
     */
    public static void setStandard(MemorialPages words) {
        standard = Objects.requireNonNull(words);
    }

    /**
     * Reads the words from a file.
     *
     * @param file the file
     * @return the words
     * @throws UncheckedIOException if the file can't be read
     */
    public static MemorialPages load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), MemorialPages.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns where a flight was lost, from its step's id.
     *
     * @param stepId the step
     * @return the first place whose match the id contains, else {@link #elsewhere}
     */
    public Place place(String stepId) {
        String id = stepId == null ? "" : stepId;
        return places.stream().filter(p -> p.match().stream().anyMatch(id::contains)).findFirst().orElse(elsewhere);
    }

    /**
     * Returns a tone's stories.
     *
     * @param tone the tone
     * @return its stories, maybe none
     */
    public List<Story> storiesOf(FrontPage.Stance tone) {
        return tones.stream().filter(t -> t.tone() == tone).findFirst().map(Tone::stories).orElse(List.of());
    }

    /**
     * Returns a story by its id, the spokesman's too.
     *
     * @param id the id
     * @return the story, if the file has it
     */
    public Optional<Story> story(String id) {
        return Stream.concat(tones.stream().flatMap(t -> t.stories().stream()), Stream.of(spokesman))
            .filter(s -> s.id().equals(id)).findFirst();
    }

    /**
     * Returns the most telling first among the goals a lost flight would have been first to.
     *
     * @param firstGoalIds the withheld goals that would have been first in the game
     * @return its words, if one is in the list
     */
    public Optional<FirstPhrase> first(List<String> firstGoalIds) {
        return firsts.stream().filter(f -> firstGoalIds.contains(f.goal())).findFirst();
    }

    /**
     * Returns a rival's line.
     *
     * @param id the line's id
     * @return the line, if the file has it
     */
    public Optional<CondolenceLine> condolenceLine(String id) {
        return condolences.lines().stream().filter(l -> l.id().equals(id)).findFirst();
    }

    /**
     * Returns the line for a stance, quoted or not.
     *
     * @param stance the stance
     * @param quoted whether it tells the rival's words
     * @return the line, if the file has one
     */
    public Optional<CondolenceLine> condolenceLine(FrontPage.Stance stance, boolean quoted) {
        return condolences.lines().stream().filter(l -> l.stance() == stance && l.quoted() == quoted).findFirst();
    }

    /**
     * Returns a number in words, or in digits past the list.
     *
     * @param n the number
     * @return the words
     */
    public String number(int n) {
        return n >= 0 && n < numbers.size() ? numbers.get(n) : String.valueOf(n);
    }

    /**
     * Returns an ordinal in words ("second"), or "{@code n}th" past the list.
     *
     * @param n the number, from 1
     * @return the words
     */
    public String ordinal(int n) {
        return n >= 1 && n < ordinals.size() ? ordinals.get(n) : n + "th";
    }
}
