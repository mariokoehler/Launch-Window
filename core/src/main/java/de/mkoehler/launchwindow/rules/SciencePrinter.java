package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;

/**
 * Prints the science pages for one resolved turn ({@code ui-spec.md} &sect;4.18.2): a theory of the day is
 * settled the first time anyone in the game reaches one of its {@code answeredBy} goals (a turn report's
 * {@link GoalSettled} with {@code firstInGame}). Each theory is settled once: one already printed in an earlier
 * turn isn't printed again, though another of its goals is reached first later. Several agencies settling it
 * in the same turn share the page. Both reaches are printed; the client shows a public theory's page in the
 * paper and a technical one's as a line in the report's news. Flavour only.
 *
 * <p>The weather line is drawn with the game seed salted by the page's id, never from the season's random
 * stream, so a retried resolution prints the same pages.</p>
 *
 * @author Mario Koehler
 */
public final class SciencePrinter {

    /** Mixed into the game seed, so the draw never shares another random stream. */
    private static final long SALT = 0x5C1E_7CE0_1966_0203L;

    private SciencePrinter() {
    }

    /**
     * Returns a science page's id.
     *
     * @param gameTurn the turn it was settled in
     * @param theoryId the theory
     * @return the id
     */
    public static String idOf(int gameTurn, String theoryId) {
        return gameTurn + "-science-" + theoryId;
    }

    /**
     * Prints the turn's science pages.
     *
     * @param gameTurn the turn just resolved
     * @param gameSeed the game's seed
     * @param agencies every agency in the game, with its report for the turn
     * @param theories the theories
     * @param settled  the theories settled in earlier turns (their pages' story ids)
     * @param pages    the Firsts' words, for the weather
     * @param inPack   whether the game's content pack has a goal
     * @return the pages, in the theories' order; empty in a turn that settled none
     */
    public static List<FrontPage> print(int gameTurn, long gameSeed, List<FrontPagePrinter.Agency> agencies,
                                        Theories theories, Set<String> settled, FrontPages pages,
                                        Predicate<String> inPack) {
        List<FrontPagePrinter.Agency> sorted = new ArrayList<>(agencies);
        sorted.sort(Comparator.comparing(FrontPagePrinter.Agency::agencyName));
        List<FrontPage> printed = new ArrayList<>();
        for (Theories.Theory theory : theories.theories()) {
            if (settled.contains(theory.id()) || !theory.playable(inPack)) {
                continue;
            }
            String goal = null;
            List<FrontPage.Achiever> achievers = new ArrayList<>();
            for (FrontPagePrinter.Agency agency : sorted) {
                if (agency.report() == null) {
                    continue;
                }
                String reached = null;
                for (ResolutionEvent event : agency.report().events()) {
                    if (event instanceof GoalSettled done && done.firstInGame() && theory.answeredBy()
                            .contains(done.goalId()) && (reached == null || theory.answeredBy().indexOf(done.goalId())
                            < theory.answeredBy().indexOf(reached))) {
                        reached = done.goalId();
                    }
                }
                if (reached != null) {
                    achievers.add(new FrontPage.Achiever(agency.agencyName(), agency.directorName(), List.of()));
                    if (goal == null || theory.answeredBy().indexOf(reached) < theory.answeredBy().indexOf(goal)) {
                        goal = reached;
                    }
                }
            }
            if (goal == null) {
                continue;
            }
            String id = idOf(gameTurn, theory.id());
            List<String> weather = pages.weatherFor(GameDate.ofTurn(gameTurn).season());
            String weatherLine = weather.isEmpty() ? "" : weather.get(random(gameSeed, id).nextInt(weather.size()));
            printed.add(new FrontPage(id, goal, gameTurn, theory.id(), weatherLine, List.of(), achievers, List.of(),
                FrontPage.Kind.SCIENCE, null, null));
        }
        return printed;
    }

    private static SplittableRandom random(long gameSeed, String pageId) {
        long hash = 1125899906842597L;
        for (int i = 0; i < pageId.length(); i++) {
            hash = 31 * hash + pageId.charAt(i);
        }
        return new SplittableRandom(new SplittableRandom(gameSeed ^ SALT).nextLong() ^ hash);
    }
}
