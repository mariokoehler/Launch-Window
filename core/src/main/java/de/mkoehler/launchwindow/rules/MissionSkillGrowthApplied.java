package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one person's skills grew from flying a mission's season,
 * during a {@link MissionProgress#advanceSeason} pass - one event per
 * person per season, summing every individual skill's own gain that
 * season into {@link #totalSkillGain()} rather than reporting one event
 * per skill (an astronaut grows all five at once; see
 * {@code design.md} &sect;2.5(g)).</p>
 *
 * @param missionInstanceId the {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 *                          whose season caused this growth
 * @param personnelId       the grown employee's stable id
 * @param name              the grown employee's display name
 * @param group             which roster the employee belongs to - always
 *                          {@link PersonnelGroup#ASTRONAUT} or
 *                          {@link PersonnelGroup#MISSION_CONTROL}, never
 *                          {@link PersonnelGroup#SET}
 * @param totalSkillGain    the sum of every skill's own gain this season
 * @author Mario Koehler
 */
public record MissionSkillGrowthApplied(
    String missionInstanceId,
    String personnelId,
    String name,
    PersonnelGroup group,
    double totalSkillGain
) implements ResolutionEvent {
}
