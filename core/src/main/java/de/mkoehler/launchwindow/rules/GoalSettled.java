package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports one goal settling for one player during a
 * {@link MissionProgress#advanceSeason} pass: either achieved by a mission
 * step, or cancelled by an achieved goal in that cascade. Both pay the
 * prestige in {@code prestige} and both move the game-wide tally when the
 * pass's results are committed - {@code baspm.md} &sect;11.7.</p>
 *
 * @param missionInstanceId the mission whose step settled this goal
 * @param goalId            the {@link de.mkoehler.launchwindow.content.Goal#id()} that settled
 * @param cancelled         {@code true} if the goal was cancelled by an achieved goal rather than achieved by a step
 * @param prestige          the first-time or second-time prestige this settlement paid
 * @param firstInGame       whether no player held the goal at the start of the turn, so this
 *                          paid first-time prestige; under the same-turn rule two players can both be first
 * @author Mario Koehler
 */
public record GoalSettled(String missionInstanceId, String goalId, boolean cancelled, long prestige,
                          boolean firstInGame) implements ResolutionEvent {
}
