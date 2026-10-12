package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>The answer to a rival's First, the "Sputnik shock" ({@code design.md} &sect;2.1(e), the user's
 * decision, 2026-10-12): when another agency is first to a goal this player was racing for, the
 * government answers, and part of that First's prestige counts toward this player's next funding
 * review. It acts on the review because that's where a bad start snowballs: a low review pays
 * less for sixteen seasons, which delays the next goals, which lowers the next review.</p>
 *
 * <p>A First counts when another agency settled it first last turn (achieved or credited through a
 * cascade, from the goal records at the start of this turn), it pays first-time prestige, this
 * player doesn't hold it, and this player could earn it with a program open to their agency
 * ({@link ContentCatalog#reachableGoals}). {@link #SHARE} of its first-time prestige goes onto
 * {@link PlayerSnapshot#getRivalFirstsCredit()}, which the next review adds to the player's
 * prestige and then clears ({@link BudgetProgress}).</p>
 *
 * @author Mario Koehler
 */
public final class RivalFirsts {

    /** The share of a rival First's first-time prestige the next review counts: unconfirmed. */
    public static final double SHARE = 0.5;

    private static double share = SHARE;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private RivalFirsts() {
    }

    /**
     * Returns the share in force, {@link #SHARE} unless a playout changed it.
     *
     * @return the share
     */
    public static double share() {
        return share;
    }

    /**
     * Sets the share, for playouts that measure the rule (0 turns it off); put {@link #SHARE} back after.
     *
     * @param value the share, from 0
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public static void setShare(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("share must not be negative: " + value);
        }
        share = value;
    }

    /**
     * Returns the goals settled first in the game on a turn, by anyone: what the start of the next
     * turn's states say, so a turn resolved again after a crash reads the same.
     *
     * @param players every player's state at the start of the turn after {@code turn}
     * @param turn    the game turn the Firsts were settled on
     * @return the goals, in the order found
     * @throws NullPointerException if {@code players} is {@code null}
     */
    public static List<String> settledFirstOn(Collection<PlayerSnapshot> players, int turn) {
        Objects.requireNonNull(players, "players must not be null");
        Set<String> firsts = new LinkedHashSet<>();
        for (PlayerSnapshot player : players) {
            for (GoalRecord record : player.getGoalRecords()) {
                if (record.firstInGame() && record.turn() == turn) {
                    firsts.add(record.goalId());
                }
            }
        }
        return List.copyOf(firsts);
    }

    /**
     * Answers last turn's rival Firsts: adds this player's share of each one that counts to their
     * credit for the next review.
     *
     * @param snapshot the player; their credit grows
     * @param catalog  the content catalog
     * @param firsts   the goals settled first in the game last turn ({@link #settledFirstOn})
     * @return a {@link RivalFirstsAnswered} when any counted, else nothing
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            List<String> firsts) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(firsts, "firsts must not be null");
        if (firsts.isEmpty() || share == 0) {
            return List.of();
        }
        Set<String> reachable = catalog.reachableGoals(snapshot.getArchetype());
        List<String> counted = new ArrayList<>();
        long credit = 0;
        for (String goalId : firsts) {
            Goal goal = catalog.goal(goalId).orElse(null);
            if (goal == null || goal.firstTimePrestige() <= 0 || snapshot.hasAchievedOrCancelledGoal(goalId)
                    || !reachable.contains(goalId)) {
                continue;
            }
            counted.add(goalId);
            credit += Math.round(goal.firstTimePrestige() * share);
        }
        if (counted.isEmpty()) {
            return List.of();
        }
        snapshot.setRivalFirstsCredit(snapshot.getRivalFirstsCredit() + credit);
        return List.of(new RivalFirstsAnswered(counted, credit, snapshot.getRivalFirstsCredit()));
    }
}
