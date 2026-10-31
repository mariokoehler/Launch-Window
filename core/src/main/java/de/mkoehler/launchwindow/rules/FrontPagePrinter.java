package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;

/**
 * Prints the Firsts' front pages for one resolved turn ({@code ui-spec.md} &sect;4.18, G14).
 *
 * <p>A First is printed when an agency reached its goal first in the game this turn: the turn
 * report's {@link GoalSettled} with {@code firstInGame}, reached or credited through the cascade
 * alike. Several agencies reaching it in the same turn share one page (a tie). When every agency
 * that reached a First did so on a flight that also reached a later First, the earlier one is
 * folded into the later one's page instead of getting its own (a first orbit tells the first human
 * in space too).</p>
 *
 * <p>The story and the weather line are drawn with the game seed salted by the goal, never from the
 * season's random stream, so a retried resolution prints the same page.</p>
 *
 * @author Mario Koehler
 */
public final class FrontPagePrinter {

    /** Mixed into the game seed, so the draw never shares another random stream. */
    private static final long SALT = 0xF207_7A6E_1961_0412L;

    /** How many rivals an article names at most. */
    public static final int MAX_RIVALS = 2;

    private FrontPagePrinter() {
    }

    /**
     * One agency's part in the turn.
     *
     * @param agencyName   the agency's name
     * @param directorName its director's name
     * @param report       its report for the turn, or {@code null} if it has none
     */
    public record Agency(String agencyName, String directorName, TurnReport report) {
    }

    /**
     * Prints the turn's pages.
     *
     * @param gameTurn the turn just resolved
     * @param gameSeed the game's seed
     * @param agencies every agency in the game, with its report for the turn
     * @param pages    the pages' words
     * @param inPack   whether the game's content pack has a goal
     * @return the pages, in race order; empty in a turn with no First
     */
    public static List<FrontPage> print(int gameTurn, long gameSeed, List<Agency> agencies, FrontPages pages,
                                        Predicate<String> inPack) {
        List<Agency> sorted = new ArrayList<>(agencies);
        sorted.sort(Comparator.comparing(Agency::agencyName));
        // Per agency, its flights that reached Firsts, and per First, who reached it on which flight.
        Map<String, Map<String, Set<String>>> flights = new HashMap<>();
        Map<String, Map<String, String>> reachedBy = new LinkedHashMap<>();
        Map<String, TurnReport.MissionFlown> crews = new HashMap<>();
        for (Agency agency : sorted) {
            if (agency.report() == null) {
                continue;
            }
            for (TurnReport.MissionFlown flown : agency.report().missions()) {
                crews.put(agency.agencyName() + "/" + flown.missionInstanceId(), flown);
            }
            for (ResolutionEvent event : agency.report().events()) {
                if (!(event instanceof GoalSettled settled) || !settled.firstInGame()
                    || pages.order(settled.goalId()) < 0 || !inPack.test(settled.goalId())) {
                    continue;
                }
                String mission = settled.missionInstanceId() == null ? "" : settled.missionInstanceId();
                flights.computeIfAbsent(agency.agencyName(), k -> new HashMap<>())
                    .computeIfAbsent(mission, k -> new LinkedHashSet<>()).add(settled.goalId());
                reachedBy.computeIfAbsent(settled.goalId(), k -> new LinkedHashMap<>())
                    .putIfAbsent(agency.agencyName(), mission);
            }
        }
        if (reachedBy.isEmpty()) {
            return List.of();
        }
        // Fold each First into the latest later First that every one of its agencies reached on the same flight.
        Map<String, String> foldedInto = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : reachedBy.entrySet()) {
            String goal = entry.getKey();
            String target = null;
            Set<String> common = null;
            for (Map.Entry<String, String> by : entry.getValue().entrySet()) {
                Set<String> later = new LinkedHashSet<>();
                for (String other : flights.get(by.getKey()).get(by.getValue())) {
                    if (pages.order(other) > pages.order(goal)) {
                        later.add(other);
                    }
                }
                if (common == null) {
                    common = later;
                } else {
                    common.retainAll(later);
                }
            }
            for (String candidate : common) {
                if (target == null || pages.order(candidate) > pages.order(target)) {
                    target = candidate;
                }
            }
            if (target != null) {
                foldedInto.put(goal, target);
            }
        }
        List<String> printed = new ArrayList<>(reachedBy.keySet().stream().filter(g -> !foldedInto.containsKey(g)).toList());
        printed.sort(Comparator.comparingInt(pages::order));
        List<FrontPage> result = new ArrayList<>();
        for (String goal : printed) {
            List<String> folded = new ArrayList<>();
            for (String other : reachedBy.keySet()) {
                if (goal.equals(root(other, foldedInto)) && !other.equals(goal)) {
                    folded.add(other);
                }
            }
            folded.sort(Comparator.comparingInt(pages::order));
            List<FrontPage.Achiever> achievers = new ArrayList<>();
            Set<String> names = new LinkedHashSet<>();
            for (Agency agency : sorted) {
                String mission = reachedBy.get(goal).get(agency.agencyName());
                if (mission != null) {
                    names.add(agency.agencyName());
                    TurnReport.MissionFlown flown = crews.get(agency.agencyName() + "/" + mission);
                    achievers.add(new FrontPage.Achiever(agency.agencyName(), agency.directorName(),
                        flown == null ? List.of() : flown.crewNames(), flown == null ? null : flown.commanderPortraitId()));
                }
            }
            SplittableRandom random = random(gameSeed, goal);
            FrontPages.First first = pages.first(goal).orElseThrow();
            String storyId = achievers.size() > 1 && first.tie() != null ? first.tie().id()
                : first.stories().get(random.nextInt(first.stories().size())).id();
            List<String> weather = pages.weatherFor(GameDate.ofTurn(gameTurn).season());
            String weatherLine = weather.isEmpty() ? "" : weather.get(random.nextInt(weather.size()));
            List<String> others = new ArrayList<>(sorted.stream().map(Agency::agencyName).filter(n -> !names.contains(n)).toList());
            for (int i = others.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                String swap = others.get(i);
                others.set(i, others.get(j));
                others.set(j, swap);
            }
            List<String> rivals = others.subList(0, Math.min(MAX_RIVALS, others.size()));
            result.add(new FrontPage(FrontPage.idOf(gameTurn, goal), goal, gameTurn, storyId, weatherLine, folded,
                achievers, rivals));
        }
        return result;
    }

    private static String root(String goal, Map<String, String> foldedInto) {
        String current = goal;
        while (foldedInto.containsKey(current)) {
            current = foldedInto.get(current);
        }
        return current;
    }

    private static SplittableRandom random(long gameSeed, String goal) {
        long hash = 1125899906842597L;
        for (int i = 0; i < goal.length(); i++) {
            hash = 31 * hash + goal.charAt(i);
        }
        return new SplittableRandom(new SplittableRandom(gameSeed ^ SALT).nextLong() ^ hash);
    }
}
