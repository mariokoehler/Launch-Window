package de.mkoehler.launchwindow.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reports that a crewed flight earned no goals because its crew didn't come home ({@code design.md} &sect;2.6,
 * the user's decision 2026-10-22: "and return him safely"). The goals its steps reached before the loss, with
 * those the cascade credited, are taken back; this names them, for the turn report and the memorial page.
 *
 * @param missionInstanceId      the mission that lost its crew
 * @param missionConfigurationId that mission's configuration
 * @param goalIds                the goals taken back, reached and credited, in the order they settled
 * @param firstGoalIds           those of them that would have been first in the game
 * @author Mario Koehler
 */
public record GoalsWithheld(String missionInstanceId, String missionConfigurationId, List<String> goalIds,
                            List<String> firstGoalIds) implements ResolutionEvent {

    /**
     * Validates the ids and copies the lists.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public GoalsWithheld {
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        goalIds = goalIds == null ? new ArrayList<>() : new ArrayList<>(goalIds);
        firstGoalIds = firstGoalIds == null ? new ArrayList<>() : new ArrayList<>(firstGoalIds);
    }
}
