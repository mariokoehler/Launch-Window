package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports one season's prestige change for one in-progress mission, from
 * a {@link MissionProgress#advanceSeason} pass. See {@code design.md}
 * &sect;2.6(d).</p>
 *
 * <p>{@code requested} is the season's own net before the zero floor
 * applies - the successful steps' payouts, or, on a failing season, the
 * failing step's replacement deduction (negative). {@code applied} is what
 * actually changed the player's prestige after the floor.</p>
 *
 * @param missionInstanceId         the {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 * @param missionConfigurationId    the {@link de.mkoehler.launchwindow.content.MissionConfiguration#id()} flown
 * @param seasonIndex               the mission season this change belongs to
 * @param requested                 the season's net before the zero floor
 * @param applied                   the signed change actually applied to the player's prestige
 * @author Mario Koehler
 */
public record MissionPrestigeChanged(
    String missionInstanceId,
    String missionConfigurationId,
    int seasonIndex,
    long requested,
    long applied
) implements ResolutionEvent {
}
