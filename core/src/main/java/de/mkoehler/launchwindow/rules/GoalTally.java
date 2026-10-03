package de.mkoehler.launchwindow.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>The game-wide count of how many times each goal has settled, across
 * every player of one game - the basis of BASPM's first-time/second-time goal
 * prestige (see {@code design.md} &sect;2.6(d) and {@code baspm.md} &sect;11.7).</p>
 *
 * <p><b>Same-turn rule (decided 2026-10-03):</b> a resolution reads these
 * counts, but only {@link #commit} changes them, and the caller commits only
 * after every player has resolved. So two players settling the same goal in one
 * resolution both see the same count and both pay the first-time value. Not
 * per-snapshot state: it belongs to the game, and in M4 it is persisted
 * alongside {@code game-meta.json}.</p>
 *
 * @author Mario Koehler
 */
public final class GoalTally {

    private final Map<String, Integer> counts = new HashMap<>();

    /**
     * Creates an empty tally - no goal has settled yet.
     */
    public GoalTally() {
    }

    /**
     * Returns how many times this goal has settled in the game, as of the last
     * {@link #commit}.
     *
     * @param goalId the goal id
     * @return the settlement count, zero if the goal has never settled
     */
    public int countOf(String goalId) {
        return counts.getOrDefault(goalId, 0);
    }

    /**
     * Adds every {@link GoalSettled} event in the given list to the counts. Call
     * this once per resolution, after every player's pass has finished.
     *
     * @param events one or more resolutions' events; non-{@link GoalSettled} events are ignored
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public void commit(List<? extends ResolutionEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        for (ResolutionEvent event : events) {
            if (event instanceof GoalSettled settled) {
                counts.merge(settled.goalId(), 1, Integer::sum);
            }
        }
    }
}
