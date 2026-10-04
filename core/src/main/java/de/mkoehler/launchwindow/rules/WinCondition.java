package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>The win-condition check for one game, across all of its players: the first
 * player to be done with every goal on the game's declared list wins, and a
 * same-turn tie is settled by a medal table over the listed goals
 * ({@code design.md} &sect;2.9, decided 2026-09-28). A headless function: the real
 * call site is M4's {@code TurnScheduler}, which owns the declared list in
 * {@code game-meta.json}.</p>
 *
 * <p><b>Done</b> means achieved, or cancelled by an achieved goal (BASPM's own
 * gate semantics, {@code baspm.md} &sect;11.3). A cancelled goal's time is the
 * game turn of the achieved goal that cancelled it, the earliest one if several
 * did. <b>Completion turn</b> is the latest of a player's listed goal times, so
 * a player is only "complete" once the last listed goal is done.</p>
 *
 * <p><b>Tie-break:</b> only players whose completion turn equals the earliest one
 * compete. For each listed goal, they are placed by that goal's time, with
 * standard competition ranking (a shared place is shared, and the next place
 * skips accordingly). Then the one with the most first places wins, then the most
 * second places, and so on. An exact tie after every place leaves all of them
 * tied, with no winner.</p>
 *
 * @author Mario Koehler
 */
public final class WinCondition {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private WinCondition() {
    }

    /**
     * How the check ended.
     */
    public enum Status {
        /** No player has completed the declared list yet. */
        NO_WINNER_YET,
        /** Exactly one player won, outright or on the medal table. */
        WON,
        /** Players are still exactly level on every medal count, so none is declared the winner. */
        TIED
    }

    /**
     * The result of {@link #evaluate}.
     *
     * @param status  how the check ended
     * @param winners the winner's id when {@link Status#WON}; every still-tied player's id when
     *                {@link Status#TIED}; empty when {@link Status#NO_WINNER_YET}
     */
    public record Outcome(Status status, List<String> winners) {

        /**
         * Copies {@code winners} defensively.
         *
         * @throws NullPointerException if {@code status} or {@code winners} is {@code null}
         */
        public Outcome {
            Objects.requireNonNull(status, "status must not be null");
            winners = List.copyOf(winners);
        }
    }

    /**
     * Evaluates the win condition for one game.
     *
     * @param players       every player in the game, keyed by a stable player id; iteration
     *                      order is the order ties are reported in
     * @param declaredGoals the game's declared goal list - every goal that must be done to win
     * @param catalog       the content catalog, for each goal's cancellation list
     * @return the outcome
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the declared list is empty, repeats a goal, or names a
     *                                   goal the catalog doesn't define
     */
    public static Outcome evaluate(Map<String, PlayerSnapshot> players, List<String> declaredGoals,
            ContentCatalog catalog) {
        Objects.requireNonNull(players, "players must not be null");
        Objects.requireNonNull(declaredGoals, "declaredGoals must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        requireValidList(declaredGoals, catalog);
        Map<String, Map<String, Integer>> times = new LinkedHashMap<>();
        players.forEach((playerId, snapshot) -> times.put(playerId, goalTimes(snapshot, declaredGoals, catalog)));
        return evaluateTimes(times, declaredGoals);
    }

    /**
     * Evaluates the win condition from each player's goal times, as {@link #goalTimes} gives
     * them. The client reads the same check over the rival summaries it's sent.
     *
     * @param timesByPlayer each player's turn per done goal (missing = not done), keyed by a stable
     *                      player id; iteration order is the order ties are reported in
     * @param declaredGoals the game's declared goal list
     * @return the outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public static Outcome evaluateTimes(Map<String, Map<String, Integer>> timesByPlayer, List<String> declaredGoals) {
        Objects.requireNonNull(timesByPlayer, "timesByPlayer must not be null");
        Objects.requireNonNull(declaredGoals, "declaredGoals must not be null");
        List<String> candidates = sameTurnFinishers(timesByPlayer, declaredGoals);
        if (candidates.isEmpty()) {
            return new Outcome(Status.NO_WINNER_YET, List.of());
        }
        if (candidates.size() == 1) {
            return new Outcome(Status.WON, candidates);
        }
        return decide(medalTable(candidates, timesByPlayer, declaredGoals));
    }

    /**
     * Returns the turn a player finished the declared list: the latest of its goal times.
     *
     * @param times         the player's turn per done goal
     * @param declaredGoals the declared goals
     * @return the turn, or empty while a declared goal isn't done
     */
    public static Optional<Integer> completionTurn(Map<String, Integer> times, List<String> declaredGoals) {
        int latest = 0;
        for (String goalId : declaredGoals) {
            Integer time = times.get(goalId);
            if (time == null) {
                return Optional.empty();
            }
            latest = Math.max(latest, time);
        }
        return Optional.of(latest);
    }

    /**
     * Returns the players who finished the declared list at the earliest turn anyone did.
     *
     * @param timesByPlayer each player's turn per done goal
     * @param declaredGoals the declared goals
     * @return their ids, in the map's order; empty if no one has finished
     */
    public static List<String> sameTurnFinishers(Map<String, Map<String, Integer>> timesByPlayer,
            List<String> declaredGoals) {
        Map<String, Integer> completion = new LinkedHashMap<>();
        timesByPlayer.forEach((playerId, times) -> completionTurn(times, declaredGoals)
            .ifPresent(turn -> completion.put(playerId, turn)));
        if (completion.isEmpty()) {
            return List.of();
        }
        int earliest = completion.values().stream().min(Integer::compare).orElseThrow();
        return completion.entrySet().stream().filter(entry -> entry.getValue() == earliest)
            .map(Map.Entry::getKey).toList();
    }

    /**
     * A same-turn tie's medal table: each candidate's place on each declared goal.
     *
     * @param players the tied candidates, in order
     * @param places  each candidate's place per declared goal, in declared order (1 = first;
     *                shared places are shared, standard competition ranking)
     */
    public record MedalTable(List<String> players, Map<String, List<Integer>> places) {

        /**
         * Copies both defensively.
         */
        public MedalTable {
            players = List.copyOf(players);
            places = Map.copyOf(places);
        }

        /**
         * Returns how many of each place a candidate holds.
         *
         * @param playerId the candidate
         * @return the counts, index 0 = first places, one entry per candidate
         */
        public int[] counts(String playerId) {
            int[] counts = new int[players.size()];
            for (int place : places.get(playerId)) {
                counts[place - 1]++;
            }
            return counts;
        }
    }

    /**
     * Places the tied candidates on each declared goal by its time.
     *
     * @param candidates    the players tied on completion turn
     * @param timesByPlayer each player's time for each listed goal
     * @param declaredGoals the listed goals, in the order they were declared
     * @return the medal table
     */
    public static MedalTable medalTable(List<String> candidates, Map<String, Map<String, Integer>> timesByPlayer,
            List<String> declaredGoals) {
        Map<String, List<Integer>> places = new LinkedHashMap<>();
        for (String playerId : candidates) {
            List<Integer> own = new ArrayList<>();
            for (String goalId : declaredGoals) {
                int time = timesByPlayer.get(playerId).get(goalId);
                int place = 1;
                for (String other : candidates) {
                    if (timesByPlayer.get(other).get(goalId) < time) {
                        place++;
                    }
                }
                own.add(place);
            }
            places.put(playerId, own);
        }
        return new MedalTable(candidates, places);
    }

    /**
     * Breaks a same-turn tie on the medal table: the most first places, then the most second
     * places, and so on.
     *
     * @param table the medal table
     * @return the outcome
     */
    private static Outcome decide(MedalTable table) {
        List<String> candidates = table.players();
        Set<String> remaining = new HashSet<>(candidates);
        for (int place = 0; place < candidates.size() && remaining.size() > 1; place++) {
            final int currentPlace = place;
            int best = -1;
            for (String playerId : remaining) {
                best = Math.max(best, table.counts(playerId)[currentPlace]);
            }
            final int bestCount = best;
            remaining.removeIf(playerId -> table.counts(playerId)[currentPlace] != bestCount);
        }
        List<String> left = candidates.stream().filter(remaining::contains).toList();
        return left.size() == 1
            ? new Outcome(Status.WON, left)
            : new Outcome(Status.TIED, left);
    }

    /**
     * Each listed goal's settlement turn for one player, for the goals that player is
     * done with. A goal missing from the result is not done yet. A cancelled goal's turn is
     * the turn of the earliest achieved goal that cancelled it.
     *
     * @param snapshot the player
     * @param goals    the goals to look up
     * @param catalog  the content catalog
     * @return the turn per done goal, in the goals' order
     */
    public static Map<String, Integer> goalTimes(PlayerSnapshot snapshot, List<String> goals,
            ContentCatalog catalog) {
        Map<String, Integer> times = new LinkedHashMap<>();
        for (String goalId : goals) {
            if (snapshot.getAchievedGoals().containsKey(goalId)) {
                times.put(goalId, snapshot.getAchievedGoals().get(goalId));
            } else if (snapshot.getCancelledGoalIds().contains(goalId)) {
                cancellerTurn(snapshot, goalId, catalog).ifPresent(turn -> times.put(goalId, turn));
            }
        }
        return times;
    }

    /**
     * The earliest turn of an achieved goal whose cascade cancels {@code goalId}.
     *
     * @param snapshot the player
     * @param goalId   the cancelled goal
     * @param catalog  the content catalog
     * @return the turn, or empty if no achieved goal cancels it
     */
    private static Optional<Integer> cancellerTurn(PlayerSnapshot snapshot, String goalId,
            ContentCatalog catalog) {
        Integer earliest = null;
        for (Map.Entry<String, Integer> achieved : snapshot.getAchievedGoals().entrySet()) {
            if (cascadeOf(achieved.getKey(), catalog).contains(goalId)
                    && (earliest == null || achieved.getValue() < earliest)) {
                earliest = achieved.getValue();
            }
        }
        return Optional.ofNullable(earliest);
    }

    /**
     * Every goal an achieved goal cancels, directly or through the cascade.
     *
     * @param goalId  the achieved goal
     * @param catalog the content catalog
     * @return the cancelled goal ids, transitively
     */
    private static Set<String> cascadeOf(String goalId, ContentCatalog catalog) {
        Set<String> cascade = new HashSet<>();
        List<String> pending = new ArrayList<>(List.of(goalId));
        while (!pending.isEmpty()) {
            String current = pending.remove(pending.size() - 1);
            Goal goal = catalog.goal(current).orElseThrow();
            for (String cancelled : goal.cancelsGoalIds()) {
                if (cascade.add(cancelled)) {
                    pending.add(cancelled);
                }
            }
        }
        return cascade;
    }

    /**
     * Checks the declared list: non-empty, no repeats, and every goal exists.
     *
     * @param declaredGoals the list to check
     * @param catalog       the content catalog
     * @throws IllegalArgumentException if the list is empty, repeats a goal, or names an unknown goal
     */
    private static void requireValidList(List<String> declaredGoals, ContentCatalog catalog) {
        if (declaredGoals.isEmpty()) {
            throw new IllegalArgumentException("the declared goal list must not be empty");
        }
        if (declaredGoals.size() != declaredGoals.stream().distinct().count()) {
            throw new IllegalArgumentException("the declared goal list repeats a goal: " + declaredGoals);
        }
        for (String goalId : declaredGoals) {
            if (catalog.goal(goalId).isEmpty()) {
                throw new IllegalArgumentException("the declared goal list names an unknown Goal: " + goalId);
            }
        }
    }
}
