package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A printed front page ({@code ui-spec.md} &sect;4.18, G14): either one of the great Firsts (which
 * First, the turn it was reached in, the story drawn for it, and who made it), or the follow-up page
 * that prints the rivals' statements about one ({@link Kind#REACTIONS}, &sect;4.18 "The world reacts",
 * G17), a theory of the day settled ({@link Kind#SCIENCE}, &sect;4.18.2), or a crew lost in flight
 * ({@link Kind#MEMORIAL}, &sect;4.18.3). Printed once by the server at a resolution, kept in the game's
 * {@code frontpages.json}, and sent to every player. Agency names only, never account ids.
 *
 * @param id             the page's id, {@code <turn>-<goal-id>}, or {@code <turn>-<goal-id>-reactions}
 * @param goalId         the First's goal; a memorial page's is {@code ""}
 * @param gameTurn       the turn it was printed in: the one the First was reached in, or for a
 *                       follow-up the turn after it, when the statements were given
 * @param storyId        the story's id: in {@code front-pages.json}, or for a follow-up in
 *                       {@code front-page-reactions.json}
 * @param weather        the ear's weather line, drawn with the story
 * @param foldedGoalIds  other Firsts reached by the same flights, told on this page (race order)
 * @param achievers      the agencies that made it, more than one for a tie
 * @param rivals         up to two other agencies the article names (a First's page only)
 * @param kind           what the page is; {@code null} (a page printed before follow-ups) reads as
 *                       {@link Kind#FIRST}
 * @param reactsTo       a follow-up's First page, or {@code null}
 * @param reactions      a follow-up's statements, in the order the article tells them; a memorial page's
 *                       condolences
 * @param memorial       a memorial page's lost crew and the agency's statement, or {@code null}
 * @author Mario Koehler
 */
public record FrontPage(String id, String goalId, int gameTurn, String storyId, String weather,
                        List<String> foldedGoalIds, List<Achiever> achievers, List<String> rivals,
                        Kind kind, String reactsTo, List<Reaction> reactions, Memorial memorial) {

    /** A follow-up page's id ends in this, after the First page's own id. */
    public static final String REACTIONS_SUFFIX = "-reactions";

    /**
     * Copies the lists into {@link ArrayList}s, which Kryo can rebuild.
     */
    public FrontPage {
        foldedGoalIds = foldedGoalIds == null ? new ArrayList<>() : new ArrayList<>(foldedGoalIds);
        achievers = achievers == null ? new ArrayList<>() : new ArrayList<>(achievers);
        rivals = rivals == null ? new ArrayList<>() : new ArrayList<>(rivals);
        kind = kind == null ? Kind.FIRST : kind;
        reactions = reactions == null ? new ArrayList<>() : new ArrayList<>(reactions);
    }

    /**
     * A First's page.
     *
     * @param id            the page's id
     * @param goalId        the First's goal
     * @param gameTurn      the turn it was reached in
     * @param storyId       the story's id
     * @param weather       the ear's weather line
     * @param foldedGoalIds other Firsts told on this page
     * @param achievers     the agencies that made it
     * @param rivals        up to two other agencies the article names
     */
    public FrontPage(String id, String goalId, int gameTurn, String storyId, String weather,
                     List<String> foldedGoalIds, List<Achiever> achievers, List<String> rivals) {
        this(id, goalId, gameTurn, storyId, weather, foldedGoalIds, achievers, rivals, Kind.FIRST, null, null);
    }

    /**
     * A page without a memorial: a First's, a follow-up or a science page.
     *
     * @param id            the page's id
     * @param goalId        the First's goal
     * @param gameTurn      the turn it was printed in
     * @param storyId       the story's id
     * @param weather       the ear's weather line
     * @param foldedGoalIds other Firsts told on this page
     * @param achievers     the agencies that made it
     * @param rivals        up to two other agencies the article names
     * @param kind          what the page is
     * @param reactsTo      a follow-up's First page, or {@code null}
     * @param reactions     a follow-up's statements
     */
    public FrontPage(String id, String goalId, int gameTurn, String storyId, String weather,
                     List<String> foldedGoalIds, List<Achiever> achievers, List<String> rivals, Kind kind,
                     String reactsTo, List<Reaction> reactions) {
        this(id, goalId, gameTurn, storyId, weather, foldedGoalIds, achievers, rivals, kind, reactsTo, reactions,
            null);
    }

    /**
     * Returns a page's id.
     *
     * @param gameTurn the turn
     * @param goalId   the First's goal
     * @return the id
     */
    public static String idOf(int gameTurn, String goalId) {
        return gameTurn + "-" + goalId;
    }

    /**
     * Returns whether the page is a follow-up with the rivals' statements.
     *
     * @return whether it is
     */
    public boolean followUp() {
        return kind == Kind.REACTIONS;
    }

    /**
     * Returns whether the page tells one of the great Firsts (not a follow-up, not a science page). Not named
     * as a getter, so Jackson never saves it.
     *
     * @return whether it does
     */
    public boolean firstPage() {
        return kind == Kind.FIRST;
    }

    /**
     * Returns whether the page is a science page: a theory of the day settled ({@code ui-spec.md} &sect;4.18.2).
     *
     * @return whether it is
     */
    public boolean science() {
        return kind == Kind.SCIENCE;
    }

    /**
     * Returns whether the page is a memorial page: a crew lost in flight ({@code ui-spec.md} &sect;4.18.3).
     *
     * @return whether it is
     */
    public boolean memorialPage() {
        return kind == Kind.MEMORIAL;
    }

    /**
     * Returns whether the page tells a First, its own or folded in. Only a First's page does: a follow-up is
     * about the First, not the news of it, and a science page about a theory.
     *
     * @param goal the goal
     * @return whether it does
     */
    public boolean tells(String goal) {
        return firstPage() && (goalId.equals(goal) || foldedGoalIds.contains(goal));
    }

    /**
     * Returns whether an agency is among the page's achievers.
     *
     * @param agencyName the agency
     * @return whether it made the First
     */
    public boolean madeBy(String agencyName) {
        return achievers.stream().anyMatch(a -> a.agencyName().equals(agencyName));
    }

    /** What a page is. The names are saved: never rename or reuse one. */
    public enum Kind {
        /** One of the great Firsts. */
        FIRST,
        /** The rivals' statements about a First, printed the turn after it. */
        REACTIONS,
        /** A theory of the day settled by a flight (ui-spec.md section 4.18.2); its story id is the theory's. */
        SCIENCE,
        /** A crew lost in flight, printed the turn after the loss (ui-spec.md section 4.18.3). */
        MEMORIAL
    }

    /**
     * What a rival says about a First. The names are saved: never rename or reuse one.
     */
    public enum Stance {
        /** Warm, generous praise. */
        ENTHUSIASTIC,
        /** Correct, and brief. */
        POLITE,
        /** Congratulations, and a promise to catch up. */
        NEXT,
        /** It proves less than it seems. */
        DOWNPLAY,
        /** A pointed refusal to say anything. */
        NO_COMMENT,
        /** Gave no statement at all: the server's, never a player's choice. */
        DECLINED,
        /** A lost crew's agency, on their courage: they knew the risks, and flew (ui-spec.md 4.18.3). */
        COURAGE,
        /** A lost crew's agency, on who they were: the people behind the names. */
        PEOPLE,
        /** A lost crew's agency, on going on: we fly again, and finish what they started. */
        RESOLVE,
        /** A lost crew's agency, on finding out why: we will learn what went wrong, and fix it. */
        CAUSE,
        /** A rival on a lost crew: sympathy for the families and the agency. */
        CONDOLENCES,
        /** A rival on a lost crew: honour the crew and what they did. */
        TRIBUTE;

        /**
         * Returns whether a player may give this stance on a rival's First.
         *
         * @return whether it's one of the five
         */
        public boolean givable() {
            return ordinal() <= NO_COMMENT.ordinal();
        }

        /**
         * Returns whether this is the tone of a lost crew's agency's statement.
         *
         * @return whether it's one of the four tones
         */
        public boolean tone() {
            return this == COURAGE || this == PEOPLE || this == RESOLVE || this == CAUSE;
        }

        /**
         * Returns whether this is what a rival may send on a lost crew.
         *
         * @return whether it's condolences or a tribute
         */
        public boolean condolence() {
            return this == CONDOLENCES || this == TRIBUTE;
        }

        /**
         * Returns whether a statement with this stance may carry a quote.
         *
         * @return whether it may
         */
        public boolean quotable() {
            return this != NO_COMMENT && this != DECLINED;
        }
    }

    /**
     * One person lost with a crew, as the paper names them: what the game knows publicly, never anything hidden.
     *
     * @param name        their name
     * @param age         their age
     * @param portraitId  their portrait, or {@code null}
     * @param personnelId their id on the agency's memorial
     * @param flights     the flights they flew, the last one included
     * @param firsts      the flights of theirs that set a first in the game
     * @param seasons     the seasons they served the agency, or {@code -1} if not known
     * @param traits      their revealed traits, by name
     */
    public record Lost(String name, int age, String portraitId, String personnelId, int flights, int firsts,
                       int seasons, List<String> traits) {

        /**
         * Copies the list into an {@link ArrayList}, which Kryo can rebuild.
         */
        public Lost {
            traits = traits == null ? new ArrayList<>() : new ArrayList<>(traits);
        }
    }

    /**
     * A crew lost in flight, as the world learns of it the turn after ({@code ui-spec.md} &sect;4.18.3): the
     * agency's statement and the rivals' condolences answer its id, and the next resolution prints its page.
     *
     * @param id                     {@code <turn>-loss-<n>}, unique in the game
     * @param gameTurn               the turn the crew was lost in
     * @param rivalKey               the losing agency's key ({@code GameSeeds.rivalKey}), never its account id
     * @param agencyName             the losing agency
     * @param directorName           its director
     * @param missionConfigurationId the flight
     * @param stepId                 the step it was lost on
     * @param crew                   the crew, in seat order
     * @param withheldGoalIds        the goals the flight had reached, withheld (design.md section 2.6)
     * @param firstGoalIds           those of them that would have been first in the game
     */
    public record Loss(String id, int gameTurn, String rivalKey, String agencyName, String directorName,
                       String missionConfigurationId, String stepId, List<Lost> crew, List<String> withheldGoalIds,
                       List<String> firstGoalIds) {

        /**
         * Copies the lists into {@link ArrayList}s, which Kryo can rebuild.
         */
        public Loss {
            crew = crew == null ? new ArrayList<>() : new ArrayList<>(crew);
            withheldGoalIds = withheldGoalIds == null ? new ArrayList<>() : new ArrayList<>(withheldGoalIds);
            firstGoalIds = firstGoalIds == null ? new ArrayList<>() : new ArrayList<>(firstGoalIds);
        }
    }

    /**
     * What a memorial page tells beyond its achiever (the losing agency) and its condolences.
     *
     * @param loss    the loss it remembers
     * @param tone    the agency's statement's tone, or {@code null} for the spokesman's statement
     * @param quote   the agency's words, word for word, or {@code ""}
     * @param lineIds the ids of the pieces the article was made of, in {@code memorial-pages.json}
     */
    public record Memorial(Loss loss, Stance tone, String quote, List<String> lineIds) {

        /**
         * Copies the list into an {@link ArrayList}, which Kryo can rebuild.
         */
        public Memorial {
            quote = quote == null ? "" : quote;
            lineIds = lineIds == null ? new ArrayList<>() : new ArrayList<>(lineIds);
        }
    }

    /**
     * One agency that made the First.
     *
     * @param agencyName   the agency's name
     * @param directorName its director's name
     * @param crewNames    the crew of the flight that made it, in seat order; empty for an uncrewed First
     */
    public record Achiever(String agencyName, String directorName, List<String> crewNames) {

        /**
         * Copies the list into an {@link ArrayList}, which Kryo can rebuild.
         */
        public Achiever {
            crewNames = crewNames == null ? new ArrayList<>() : new ArrayList<>(crewNames);
        }
    }

    /**
     * One rival's statement as a follow-up page prints it. The line ids are in
     * {@code front-page-reactions.json}; reactions that share an "also" or "declined" line are told
     * together, in one sentence.
     *
     * @param agencyName   the rival's name
     * @param directorName its director's name
     * @param stance       what it said
     * @param quote        its quote, word for word, or {@code ""}
     * @param lineId       the line that tells it: a stance line, an "also" line or a "declined" line
     * @param linkId       the sentence leading into it from the statement before, or {@code null}
     * @param detailId     the clause about where the rival stands in the race, or {@code null}
     */
    public record Reaction(String agencyName, String directorName, Stance stance, String quote, String lineId,
                           String linkId, String detailId) {

        /**
         * Reads a missing quote as none.
         */
        public Reaction {
            quote = quote == null ? "" : quote;
        }
    }

    /**
     * A statement a rival gave, before it's printed: kept by the server per turn and sent back to the
     * player who gave it.
     *
     * @param pageId the First's page
     * @param stance what it says
     * @param quote  the quote, cleaned, or {@code ""}
     */
    public record Statement(String pageId, Stance stance, String quote) {

        /** The longest quote, in characters. */
        public static final int MAX_QUOTE_LENGTH = 140;

        /**
         * The characters a quote may hold: letters, digits and everyday punctuation, all in the page's
         * fonts. No braces or angle brackets, so a quote never reads as a token. The client's
         * {@code Theme.CHARACTERS} holds all of them ({@code FrontPageReactionsTest}); they're here
         * because the server mustn't depend on the client's theme.
         */
        public static final String ALLOWED_CHARACTERS =
            " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                + ".,;:!?'\"-()&/%$#@*+="
                + "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞß"
                + "àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ"
                + "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĢģĪīĮįĶķĻļŅņİıŁłŒœŚśŠšŤťŪūŮůŰűŲųŸŹźŻżŽž"
                + "‘’“”–—…·";

        /**
         * Reads a missing quote as none.
         */
        public Statement {
            quote = quote == null ? "" : quote;
        }

        /**
         * Cleans a quote as the server keeps it: stripped, with every run of spaces made one.
         *
         * @param quote the quote as typed, or {@code null}
         * @return the cleaned quote, maybe {@code ""}
         */
        public static String clean(String quote) {
            return quote == null ? "" : quote.strip().replaceAll(" {2,}", " ");
        }

        /**
         * Returns whether a cleaned quote holds only {@link #ALLOWED_CHARACTERS}.
         *
         * @param quote the cleaned quote
         * @return whether it does
         */
        public static boolean allowed(String quote) {
            return quote.chars().allMatch(c -> ALLOWED_CHARACTERS.indexOf(c) >= 0);
        }
    }
}
