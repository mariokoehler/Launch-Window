package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one Expert-flagged employee was consulted to rescue a
 * mission step's saving throw, during a {@link MissionProgress#advanceSeason}
 * pass - one event per eligible Expert actually consulted for a step that
 * genuinely needed rescuing (the step's first roll already failed), never
 * for merely being eligible. See {@code design.md} &sect;2.5(i) for the
 * full mechanic: eligibility (roster-wide, not mission-scoped), stacking
 * (sequential, each against the previous Expert's own result), and
 * per-employee exhaustion. <b>{@code probabilityAfter} never reflects
 * {@link MissionStepResolution#EXPERT_RESCUE_PROBABILITY_CAP}</b> - the cap
 * is applied once, by {@link MissionProgress#applyExpertRescue}, only
 * after every eligible Expert's own event for this step has already been
 * emitted, so a sequence of these events can together show a probability
 * above the cap even though the roll that actually decided the step's
 * outcome used the capped value. Worth remembering if a later "what
 * actually happened" replay (the post-turn replay idea &sect;2.2(c)
 * floats) is ever built from this event stream directly.</p>
 *
 * @param missionInstanceId  the {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 *                           whose step needed rescuing
 * @param stepId             the {@link de.mkoehler.launchwindow.content.MissionStep#id()}
 *                           that needed rescuing
 * @param group              which roster the Expert belongs to - always
 *                           {@link PersonnelGroup#ASTRONAUT} or
 *                           {@link PersonnelGroup#MISSION_CONTROL}, never
 *                           {@link PersonnelGroup#SET}
 * @param personnelId        the Expert's stable id
 * @param name                the Expert's display name
 * @param skillName           the {@code name()} of the matched Expert
 *                            skill - a plain string, same reason as
 *                            {@link TrainingCompleted#skillName()}
 * @param boostPercent        this Expert's own boost percent for this use,
 *                            already scaled down if this was an
 *                            off-mission Mission Control officer (see
 *                            {@code design.md} &sect;2.5(i)'s 75% rule)
 * @param probabilityBefore   the saving-throw probability immediately
 *                            before this Expert's own contribution
 * @param probabilityAfter    the saving-throw probability immediately
 *                            after it
 * @author Mario Koehler
 */
public record ExpertRescueApplied(
    String missionInstanceId,
    String stepId,
    PersonnelGroup group,
    String personnelId,
    String name,
    String skillName,
    double boostPercent,
    double probabilityBefore,
    double probabilityAfter
) implements ResolutionEvent {
}
