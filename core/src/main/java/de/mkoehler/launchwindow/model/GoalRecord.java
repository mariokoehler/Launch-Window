package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * How one of the agency's goals settled, kept for the Museum's achievements
 * ({@code ui-spec.md} &sect;4.11). Written at settlement, beside the achieved goal or the
 * cancelled one; goals settled before this record existed have none.
 *
 * @param goalId                 the goal
 * @param turn                   the game turn it settled on
 * @param missionConfigurationId the configuration whose step settled it
 * @param creditedThroughGoalId  the goal actually reached, when this one was credited through its
 *                               cascade; {@code null} when this goal was reached itself
 * @param firstInGame            whether it paid first-time prestige (no agency held it before)
 * @param prestige               the prestige it paid
 * @author Mario Koehler
 */
public record GoalRecord(String goalId, int turn, String missionConfigurationId, String creditedThroughGoalId,
                         boolean firstInGame, long prestige) {

    /**
     * Checks the required fields.
     */
    public GoalRecord {
        Objects.requireNonNull(goalId, "goalId must not be null");
    }
}
