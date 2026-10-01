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
 *                                          {@code retiredCount}
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
    int programsOpenedCount
) {
}
