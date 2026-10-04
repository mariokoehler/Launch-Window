package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The goals that achieving one goal settles for a player: the goal itself, then every goal it
 * cancels, recursively, that the player doesn't hold yet. "Cancels" means "also credits": each
 * cancelled goal pays its own prestige and counts as fulfilled, as in BASPM ({@code baspm.md}
 * &sect;11.7). Shared by the season pass, which settles the cascade, and the client, which shows
 * what a goal is worth.
 *
 * @author Mario Koehler
 */
public final class GoalCascade {

    private GoalCascade() {
    }

    /**
     * Returns the goals achieving a goal would settle for a player, in settlement order (the goal
     * first, then its cancelled goals depth first, in each goal's listed order), skipping goals the
     * player already holds. Empty if the player holds the goal itself.
     *
     * @param goalId  the goal achieved
     * @param state   the player's state
     * @param catalog the content pack
     * @return the goal ids to settle
     * @throws IllegalStateException if a goal in the cascade isn't in the pack
     */
    public static List<String> unheld(String goalId, PlayerSnapshot state, ContentCatalog catalog) {
        return unheld(goalId, state::hasAchievedOrCancelledGoal, catalog);
    }

    /**
     * As {@link #unheld(String, PlayerSnapshot, ContentCatalog)}, against any notion of which goals
     * are held: the mission assessment walks a flight step by step, holding each step's goals once
     * it's past.
     *
     * @param goalId  the goal achieved
     * @param held    whether a goal is already held
     * @param catalog the content pack
     * @return the goal ids to settle
     */
    public static List<String> unheld(String goalId, Predicate<String> held, ContentCatalog catalog) {
        Set<String> order = new LinkedHashSet<>();
        if (!held.test(goalId)) {
            visit(goalId, held, catalog, order);
        }
        return new ArrayList<>(order);
    }

    private static void visit(String goalId, Predicate<String> held, ContentCatalog catalog, Set<String> order) {
        order.add(goalId);
        Goal goal = catalog.goal(goalId).orElseThrow(
            () -> new IllegalStateException("Unknown Goal " + goalId));
        for (String cancelledId : goal.cancelsGoalIds()) {
            if (!held.test(cancelledId) && !order.contains(cancelledId)) {
                visit(cancelledId, held, catalog, order);
            }
        }
    }

    /**
     * Returns the most prestige achieving a goal can pay a player: the first-time prestige of every
     * goal in its cascade. Less if another agency got there first (second-time prestige).
     *
     * @param goalId  the goal achieved
     * @param state   the player's state
     * @param catalog the content pack
     * @return the prestige
     */
    public static long maxPrestige(String goalId, PlayerSnapshot state, ContentCatalog catalog) {
        long total = 0;
        for (String id : unheld(goalId, state, catalog)) {
            total += catalog.goal(id).orElseThrow().firstTimePrestige();
        }
        return total;
    }
}
