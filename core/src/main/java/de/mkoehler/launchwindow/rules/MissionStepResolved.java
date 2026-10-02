package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one step of an in-progress mission's current season
 * resolved, during a {@link MissionProgress#advanceSeason} pass.</p>
 *
 * @param missionInstanceId     the {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 *                              this step belongs to
 * @param missionConfigurationId the {@link de.mkoehler.launchwindow.content.MissionConfiguration#id()}
 *                              this instance is running
 * @param seasonIndex           the 0-based season index this step belonged to
 * @param stepId                the {@link de.mkoehler.launchwindow.content.MissionStep#id()}
 *                              that resolved
 * @param outcome               the resolved outcome
 * @author Mario Koehler
 */
public record MissionStepResolved(
    String missionInstanceId,
    String missionConfigurationId,
    int seasonIndex,
    String stepId,
    MissionStepOutcome outcome
) implements ResolutionEvent {
}
