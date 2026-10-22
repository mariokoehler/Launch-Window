package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Prints the follow-up pages, "The world reacts" ({@code ui-spec.md} &sect;4.18.1, G17): for each
 * First printed at the end of the turn before, the rivals' statements given during this one, told as one
 * article.
 *
 * <p>The statements' mix picks the story's mood; statements are told in stance order, quoted before
 * unquoted, each by a stance line drawn without repeats, with a linking sentence wherever the stance
 * changes and sometimes a clause on where the rival stands in the race. At most {@link #MAX_TOLD}
 * are told one by one, quotes first; the rest share an "also" line per stance. Rivals who gave none
 * share a "declined" line. A page whose rivals all stayed silent isn't printed.</p>
 *
 * <p>Every draw comes from the game seed salted and mixed with the follow-up's id, never from the
 * season's random stream, so a retried resolution prints the same page.</p>
 *
 * @author Mario Koehler
 */
public final class ReactionPrinter {

    /** Mixed into the game seed, so the draw never shares another random stream. */
    private static final long SALT = 0x5EAC_7104_1957_1004L;

    /** How many statements an article tells one by one at most. */
    public static final int MAX_TOLD = 4;

    /** How often a detail clause other than a matched First is told. */
    private static final double DETAIL_CHANCE = 0.5;

    private ReactionPrinter() {
    }

    /**
     * One agency in the game as the follow-ups see it.
     *
     * @param agencyName   the agency's name
     * @param directorName its director's name
     * @param firstTurn    the first turn it has a report for; a page printed before it isn't its to answer
     * @param goalsHeld    the goals it holds now, for the detail clauses
     * @param statements   the statements it gave this turn, by First page id
     */
    public record Agency(String agencyName, String directorName, int firstTurn, Set<String> goalsHeld,
                         Map<String, FrontPage.Statement> statements) {
    }

    /**
     * Returns the first turn an agency has a report for, from its state before a turn: its season
     * counts 1 at the start and one more per resolved turn.
     *
     * @param turn   the turn the state is from the start of
     * @param season the state's season
     * @return the first turn it resolved in
     */
    public static int firstTurn(long turn, int season) {
        return Math.toIntExact(turn - Math.max(0, season - 1));
    }

    /**
     * Returns whether an agency may give a statement about a page in an open turn.
     *
     * @param page        the page
     * @param openTurn    the open turn
     * @param agencyName  the agency
     * @param firstTurn   the first turn it has a report for
     * @return whether it may
     */
    public static boolean mayReact(FrontPage page, int openTurn, String agencyName, int firstTurn) {
        return !page.followUp() && page.gameTurn() == openTurn - 1 && !page.madeBy(agencyName)
            && firstTurn <= page.gameTurn();
    }

    /**
     * Prints the follow-ups of one turn.
     *
     * @param gameTurn the turn just resolved, in which the statements were given
     * @param gameSeed the game's seed
     * @param firsts   the First pages printed at the end of the turn before
     * @param agencies every agency in the game
     * @param words    the follow-ups' words
     * @param pages    the Firsts' words, for the weather
     * @return the follow-ups, in the order of their Firsts; empty if nobody spoke
     */
    public static List<FrontPage> print(int gameTurn, long gameSeed, List<FrontPage> firsts, List<Agency> agencies,
                                        FrontPageReactions words, FrontPages pages) {
        List<FrontPage> printed = new ArrayList<>();
        for (FrontPage first : firsts) {
            if (first.followUp() || first.gameTurn() != gameTurn - 1) {
                continue;
            }
            FrontPageReactions.First text = words.first(first.goalId()).orElse(null);
            if (text == null || text.stories().isEmpty()) {
                continue;
            }
            printOne(gameTurn, gameSeed, first, agencies, words, text, pages).ifPresent(printed::add);
        }
        return printed;
    }

    private static Optional<FrontPage> printOne(int gameTurn, long gameSeed, FrontPage first,
            List<Agency> agencies, FrontPageReactions words, FrontPageReactions.First text, FrontPages pages) {
        String id = first.id() + FrontPage.REACTIONS_SUFFIX;
        SplittableRandom random = random(gameSeed, id);
        List<Agency> spoke = new ArrayList<>();
        List<Agency> silent = new ArrayList<>();
        for (Agency agency : agencies) {
            if (!mayReact(first, gameTurn, agency.agencyName(), agency.firstTurn())) {
                continue;
            }
            FrontPage.Statement statement = agency.statements().get(first.id());
            if (statement != null && statement.stance() != null && statement.stance().givable()) {
                spoke.add(agency);
            } else {
                silent.add(agency);
            }
        }
        if (spoke.isEmpty()) {
            return Optional.empty();
        }
        FrontPageReactions.Mood mood = FrontPageReactions.Mood.of(spoke.stream()
            .map(a -> stance(a, first)).toList());
        List<FrontPageReactions.Story> stories = text.stories().stream().filter(s -> s.mood() == mood).toList();
        if (stories.isEmpty()) {
            stories = text.stories();
        }
        String storyId = stories.get(random.nextInt(stories.size())).id();
        List<String> weather = pages.weatherFor(GameDate.ofTurn(gameTurn).season());
        String weatherLine = weather.isEmpty() ? "" : weather.get(random.nextInt(weather.size()));

        // Told one by one: quotes first, then by stance and name; the rest share a line.
        Comparator<Agency> byStance = Comparator.comparing((Agency a) -> stance(a, first))
            .thenComparing(a -> quote(a, first).isEmpty())
            .thenComparing(Agency::agencyName);
        List<Agency> chosen = new ArrayList<>(spoke);
        chosen.sort(Comparator.comparing((Agency a) -> quote(a, first).isEmpty()).thenComparing(byStance));
        List<Agency> told = new ArrayList<>(chosen.subList(0, Math.min(MAX_TOLD, chosen.size())));
        List<Agency> folded = new ArrayList<>(chosen.subList(told.size(), chosen.size()));
        told.sort(byStance);
        folded.sort(byStance);

        Map<String, Integer> used = new HashMap<>();
        List<FrontPage.Reaction> reactions = new ArrayList<>();
        FrontPage.Stance previous = null;
        for (Agency agency : told) {
            FrontPage.Stance stance = stance(agency, first);
            String quote = quote(agency, first);
            FrontPageReactions.StanceLines lines = words.linesOf(stance);
            List<FrontPageReactions.Line> pool = !quote.isEmpty() && !lines.quoted().isEmpty() ? lines.quoted()
                : lines.plain();
            String lineId = draw(pool, used, random);
            String linkId = null;
            if (previous != null && previous != stance) {
                linkId = drawLink(words.linksBetween(previous, stance), used, random);
            }
            String detailId = detail(agency, first, text, used, random);
            reactions.add(new FrontPage.Reaction(agency.agencyName(), agency.directorName(), stance,
                stance.quotable() ? quote : "", lineId, linkId, detailId));
            previous = stance;
        }
        Map<FrontPage.Stance, String> alsoLines = new EnumMap<>(FrontPage.Stance.class);
        for (Agency agency : folded) {
            FrontPage.Stance stance = stance(agency, first);
            String lineId = alsoLines.computeIfAbsent(stance,
                s -> draw(words.linesOf(s).also(), used, random));
            reactions.add(new FrontPage.Reaction(agency.agencyName(), agency.directorName(), stance, "", lineId,
                null, null));
        }
        silent.sort(Comparator.comparing(Agency::agencyName));
        if (!silent.isEmpty()) {
            List<FrontPageReactions.Line> pool = silent.size() == 1 ? words.declined().one() : words.declined().many();
            String lineId = draw(pool, used, random);
            for (Agency agency : silent) {
                reactions.add(new FrontPage.Reaction(agency.agencyName(), agency.directorName(),
                    FrontPage.Stance.DECLINED, "", lineId, null, null));
            }
        }
        return Optional.of(new FrontPage(id, first.goalId(), gameTurn, storyId, weatherLine,
            first.foldedGoalIds(), first.achievers(), List.of(), FrontPage.Kind.REACTIONS, first.id(), reactions));
    }

    private static FrontPage.Stance stance(Agency agency, FrontPage first) {
        return agency.statements().get(first.id()).stance();
    }

    private static String quote(Agency agency, FrontPage first) {
        FrontPage.Statement statement = agency.statements().get(first.id());
        return statement.stance().quotable() ? statement.quote() : "";
    }

    /** The clause on where a rival stands: always when it matched the First, else about half the time. */
    private static String detail(Agency agency, FrontPage first, FrontPageReactions.First text,
                                 Map<String, Integer> used, SplittableRandom random) {
        FrontPageReactions.DetailKind kind;
        boolean held = text.before() == null ? !agency.goalsHeld().isEmpty()
            : agency.goalsHeld().contains(text.before());
        if (agency.goalsHeld().contains(first.goalId())) {
            kind = FrontPageReactions.DetailKind.MATCHED;
        } else {
            kind = held ? FrontPageReactions.DetailKind.HELD_BEFORE : FrontPageReactions.DetailKind.LACKS_BEFORE;
        }
        // Drawn every time, so whether one detail is told never shifts the draws after it.
        boolean tell = random.nextDouble() < DETAIL_CHANCE;
        if (kind != FrontPageReactions.DetailKind.MATCHED && !tell) {
            return null;
        }
        return draw(text.detailsOf(kind), used, random);
    }

    /**
     * Draws a line from a pool, never one already used on the page while others are left; a pool used
     * up starts over. Returns {@code null} for an empty pool.
     */
    private static String draw(List<FrontPageReactions.Line> pool, Map<String, Integer> used,
                               SplittableRandom random) {
        if (pool.isEmpty()) {
            return null;
        }
        List<FrontPageReactions.Line> fresh = pool.stream().filter(l -> !used.containsKey(l.id())).toList();
        List<FrontPageReactions.Line> from = fresh.isEmpty() ? pool : fresh;
        FrontPageReactions.Line line = from.get(random.nextInt(from.size()));
        used.merge(line.id(), 1, Integer::sum);
        return line.id();
    }

    private static String drawLink(List<FrontPageReactions.Link> pool, Map<String, Integer> used,
                                   SplittableRandom random) {
        return draw(pool.stream().map(l -> new FrontPageReactions.Line(l.id(), l.text())).toList(), used, random);
    }

    private static SplittableRandom random(long gameSeed, String pageId) {
        long hash = 1125899906842597L;
        for (int i = 0; i < pageId.length(); i++) {
            hash = 31 * hash + pageId.charAt(i);
        }
        return new SplittableRandom(new SplittableRandom(gameSeed ^ SALT).nextLong() ^ hash);
    }
}
