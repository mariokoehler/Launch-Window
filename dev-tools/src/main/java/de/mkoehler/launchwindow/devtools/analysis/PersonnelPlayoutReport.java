package de.mkoehler.launchwindow.devtools.analysis;

/**
 * <p>Summary statistics from a {@link PersonnelLifecyclePlayouts} run - the
 * handful of numbers that actually answer "is this balanced," not a raw
 * per-employee dump (see {@code design.md} &sect;3.10's own description of
 * Robot-Rampage's {@code Report} precedent).</p>
 *
 * @param gamesPlayed                       how many independent games were simulated
 * @param seasonsPerGame                    the season cap each game ran for
 * @param employeesStarted                  total employees across every game's
 *                                          initial rosters, summed over all
 *                                          three groups
 * @param resignedCount                     how many employees resigned across
 *                                          every game
 * @param retiredCount                      how many employees retired across
 *                                          every game
 * @param stillEmployedAtEnd                {@code employeesStarted} plus
 *                                          {@code hiredCount} minus
 *                                          {@code resignedCount} minus
 *                                          {@code retiredCount} minus
 *                                          {@code lostInAccidentCount}
 * @param averageTenureSeasonsWhenResigned  average seasons survived by an
 *                                          employee who resigned, {@code 0} if
 *                                          nobody did
 * @param averageTenureSeasonsWhenRetired   average seasons survived by an
 *                                          employee who retired, {@code 0} if
 *                                          nobody did
 * @param trainingCompletedCount            how many fixed-length training
 *                                          rounds completed across every game
 * @param averageTrainingGain               average skill gain per completed
 *                                          training round, {@code 0} if none
 *                                          completed
 * @param hiredCount                        how many bot hires succeeded
 *                                          across every game (see
 *                                          {@code PersonnelLifecyclePlayouts#HIRE_CHANCE_PER_SEASON});
 *                                          already reflected in
 *                                          {@code stillEmployedAtEnd}
 * @param poorBandBatches                   how many of the three groups'
 *                                          per-season candidate-pool
 *                                          refreshes landed in the "Poor"
 *                                          quality band, across every game
 * @param averageBandBatches                same, for the "Average" band
 * @param goodBandBatches                   same, for the "Good" band
 * @param excellentBandBatches              same, for the "Excellent" band
 * @param programsOpenedCount               how many bot program-opens
 *                                          succeeded across every game (see
 *                                          {@code PersonnelLifecyclePlayouts#OPEN_PROGRAM_CHANCE_PER_SEASON})
 * @param researchCompletedCount            how many seasons of R&amp;D
 *                                          produced a {@code ResearchCompleted}
 *                                          event across every game
 * @param averageResearchReliabilityGain    average component-reliability
 *                                          gain per completed R&amp;D
 *                                          season, {@code 0} if none
 *                                          completed
 * @param missionsLaunchedCount             how many bot mission launches
 *                                          succeeded across every game (see
 *                                          {@code PersonnelLifecyclePlayouts#LAUNCH_MISSION_CHANCE_PER_SEASON})
 * @param missionStepSuccessfulCount        how many mission steps resolved
 *                                          {@code SUCCESSFUL} across every game
 * @param missionStepMinorGlitchSolvedCount how many mission steps resolved
 *                                          {@code MINOR_GLITCH_SOLVED}
 *                                          across every game
 * @param missionStepMinorGlitchCount       how many mission steps resolved
 *                                          {@code MINOR_GLITCH} across every game
 * @param missionStepFailureCount           how many mission steps resolved
 *                                          {@code FAILURE} across every game
 * @param missionStepCatastrophicCount      how many mission steps resolved
 *                                          {@code CATASTROPHIC} across every
 *                                          game
 * @param lostInAccidentCount               how many astronauts were
 *                                          permanently lost to a
 *                                          {@code CATASTROPHIC} step across
 *                                          every game - already reflected
 *                                          in {@code stillEmployedAtEnd}
 * @param retiredOnMissionReleaseCount       how many of {@code retiredCount}'s
 *                                          own total came specifically from
 *                                          {@code MissionProgress}'s own
 *                                          retire-on-release check (an
 *                                          employee who had already reached
 *                                          retirement age at the moment a
 *                                          mission released them), rather
 *                                          than {@code PersonnelLifecycle}'s
 *                                          own season-advance check -
 *                                          already included in
 *                                          {@code retiredCount}, not
 *                                          additional to it
 * @param missionReliabilityChangeCount      how many post-mission-season
 *                                          component reliability changes
 *                                          were applied across every game
 *                                          (see {@code MissionProgress#applyPostSeasonReliabilityChange})
 * @param averageMissionReliabilityDelta    average signed reliability change
 *                                          per component per season it
 *                                          changed, {@code 0} if none did -
 *                                          positive means gains outweighed
 *                                          losses overall
 * @param missionSkillGrowthCount            how many post-mission-season
 *                                          skill-growth events were applied
 *                                          across every game, one per
 *                                          surviving assigned crew/
 *                                          controller per season flown (see
 *                                          {@code MissionProgress#applyPostSeasonSkillGrowth})
 * @param averageMissionSkillGrowth          average total skill gain per
 *                                          skill-growth event, {@code 0} if
 *                                          none occurred
 * @param missionTriggeredSetReleaseCount    how many SET specialists were
 *                                          released from R&amp;D specifically
 *                                          because a mission's post-season
 *                                          reliability gain pushed their
 *                                          target component to its R&amp;D
 *                                          ceiling (as opposed to
 *                                          {@code ResearchProgress}'s own
 *                                          R&amp;D-pass release, not counted
 *                                          here)
 * @param expertTrainingStartedCount         how many bot-started
 *                                          {@code TrainingKind.EXPERT}
 *                                          training attempts succeeded
 *                                          across every game (see
 *                                          {@code PersonnelLifecyclePlayouts#EXPERT_TRAINING_START_CHANCE_PER_SEASON}) -
 *                                          via the validated
 *                                          {@code TrainingProgress#startAstronautExpertTraining}/
 *                                          {@code #startMissionControlExpertTraining}
 *                                          actions, never by setting the
 *                                          training fields directly
 * @param expertTrainingCompletedCount       how many {@code TrainingKind.EXPERT}
 *                                          attempts actually succeeded
 *                                          (granted the Expert trait)
 *                                          across every game - always at
 *                                          most {@code expertTrainingStartedCount},
 *                                          since a season can fail and
 *                                          continue instead
 * @param pulledOutOfTrainingCount           how many bot pull-outs
 *                                          succeeded across every game,
 *                                          any {@code TrainingKind}, any
 *                                          group (see
 *                                          {@code PersonnelLifecyclePlayouts#PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON})
 * @param expertRescueAppliedCount           how many {@code ExpertRescueApplied}
 *                                          events across every game
 *                                          actually contributed a nonzero
 *                                          boost - an eligible but fully
 *                                          exhausted (4th+ use this turn)
 *                                          Expert is still consulted and
 *                                          still emits an event, just not
 *                                          counted here
 * @author Mario Koehler
 */
public record PersonnelPlayoutReport(
    int gamesPlayed,
    int seasonsPerGame,
    int employeesStarted,
    int resignedCount,
    int retiredCount,
    int stillEmployedAtEnd,
    double averageTenureSeasonsWhenResigned,
    double averageTenureSeasonsWhenRetired,
    int trainingCompletedCount,
    double averageTrainingGain,
    int hiredCount,
    int poorBandBatches,
    int averageBandBatches,
    int goodBandBatches,
    int excellentBandBatches,
    int programsOpenedCount,
    int researchCompletedCount,
    double averageResearchReliabilityGain,
    int missionsLaunchedCount,
    int missionStepSuccessfulCount,
    int missionStepMinorGlitchSolvedCount,
    int missionStepMinorGlitchCount,
    int missionStepFailureCount,
    int missionStepCatastrophicCount,
    int lostInAccidentCount,
    int retiredOnMissionReleaseCount,
    int missionReliabilityChangeCount,
    double averageMissionReliabilityDelta,
    int missionSkillGrowthCount,
    double averageMissionSkillGrowth,
    int missionTriggeredSetReleaseCount,
    int expertTrainingStartedCount,
    int expertTrainingCompletedCount,
    int pulledOutOfTrainingCount,
    int expertRescueAppliedCount
) {
}
