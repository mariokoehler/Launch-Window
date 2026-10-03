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

        Map<String, Map<String, Integer>> timesByPlayer = new LinkedHashMap<>();
        Map<String, Integer> completionByPlayer = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerSnapshot> entry : players.entrySet()) {
            Map<String, Integer> times = goalTimes(entry.getValue(), declaredGoals, catalog);
            if (times.size() == declaredGoals.size()) {
                timesByPlayer.put(entry.getKey(), times);
                completionByPlayer.put(entry.getKey(), times.values().stream().max(Integer::compare).orElseThrow());
            }
        }
        if (completionByPlayer.isEmpty()) {
            return new Outcome(Status.NO_WINNER_YET, List.of());
        }

        int earliest = completionByPlayer.values().stream().min(Integer::compare).orElseThrow();
        List<String> candidates = new ArrayList<>();
        completionByPlayer.forEach((playerId, completion) -> {
            if (completion == earliest) {
                candidates.add(playerId);
            }
        });
        if (candidates.size() == 1) {
            return new Outcome(Status.WON, candidates);
        }
        return medalTable(candidates, timesByPlayer, declaredGoals);
    }

    /**
     * Breaks a same-turn tie: each listed goal places the candidates by its time, then
     * the candidates are compared place by place, most first places first.
     *
     * @param candidates    the players tied on completion turn
     * @param timesByPlayer each player's time for each listed goal
     * @param declaredGoals the listed goals, in the order they were declared
     * @return the medal-table outcome
     */
    private static Outcome medalTable(List<String> candidates, Map<String, Map<String, Integer>> timesByPlayer,
            List<String> declaredGoals) {
        int places = candidates.size();
        Map<String, int[]> medals = new LinkedHashMap<>();
        for (String playerId : candidates) {
            medals.put(playerId, new int[places + 1]);
        }
        for (String goalId : declaredGoals) {
            for (String playerId : candidates) {
                int time = timesByPlayer.get(playerId).get(goalId);
                int place = 1;
                for (String other : candidates) {
                    if (timesByPlayer.get(other).get(goalId) < time) {
                        place++;
                    }
                }
                medals.get(playerId)[place]++;
            }
        }

        Set<String> remaining = new HashSet<>(candidates);
        for (int place = 1; place <= places && remaining.size() > 1; place++) {
            final int currentPlace = place;
            int best = -1;
            for (String playerId : remaining) {
                best = Math.max(best, medals.get(playerId)[currentPlace]);
            }
            final int bestCount = best;
            remaining.removeIf(playerId -> medals.get(playerId)[currentPlace] != bestCount);
        }
        List<String> left = candidates.stream().filter(remaining::contains).toList();
        return left.size() == 1
            ? new Outcome(Status.WON, left)
            : new Outcome(Status.TIED, left);
    }

    /**
     * Each listed goal's settlement turn for one player, for the goals that player is
     * done with. A goal missing from the result is not done yet.
     *
     * @param snapshot      the player
     * @param declaredGoals the listed goals
     * @param catalog       the content catalog
     * @return the turn per done goal
     */
    private static Map<String, Integer> goalTimes(PlayerSnapshot snapshot, List<String> declaredGoals,
            ContentCatalog catalog) {
        Map<String, Integer> times = new LinkedHashMap<>();
        for (String goalId : declaredGoals) {
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
