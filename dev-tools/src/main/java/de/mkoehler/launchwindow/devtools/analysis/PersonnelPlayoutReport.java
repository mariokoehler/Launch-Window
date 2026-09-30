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
 * @param stillEmployedAtEnd                {@code employeesStarted} minus
 *                                          {@code resignedCount} minus
 *                                          {@code retiredCount}
 * @param averageTenureSeasonsWhenResigned  average seasons survived by an
 *                                          employee who resigned, {@code 0} if
 *                                          nobody did
 * @param averageTenureSeasonsWhenRetired   average seasons survived by an
 *                                          employee who retired, {@code 0} if
 *                                          nobody did
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
    double averageTenureSeasonsWhenRetired
) {
}
