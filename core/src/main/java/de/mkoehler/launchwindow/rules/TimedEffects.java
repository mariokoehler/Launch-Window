package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.Objects;

/**
 * Adds a timed R&amp;D or running-cost multiplier on top of whatever is running ({@code design.md}
 * &sect;2.7, user decision 2026-10-06): the factors multiply and the longer of the two durations
 * stays. Random events and press stories share these two slots; BASPM's setter replaced the older
 * effect instead. {@link RandomEvents} counts both down at the start of its phase.
 *
 * @author Mario Koehler
 */
public final class TimedEffects {

    private TimedEffects() {
    }

    /**
     * Adds an R&amp;D results multiplier.
     *
     * @param snapshot the player; mutated in place
     * @param factor   the factor
     * @param seasons  the slot's count to combine, as {@link RandomEvents} counts it
     */
    public static void addRdResults(PlayerSnapshot snapshot, double factor, int seasons) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        boolean running = snapshot.getRdResultsSeasonsRemaining() > 0;
        snapshot.setRdResults(running ? snapshot.getRdResultsMultiplier() * factor : factor,
            Math.max(snapshot.getRdResultsSeasonsRemaining(), seasons));
    }

    /**
     * Adds a running-cost multiplier.
     *
     * @param snapshot the player; mutated in place
     * @param factor   the factor
     * @param seasons  the slot's count to combine, as {@link RandomEvents} counts it
     */
    public static void addCostMultiplier(PlayerSnapshot snapshot, double factor, int seasons) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        boolean running = snapshot.getCostMultiplierSeasonsRemaining() > 0;
        snapshot.setCostMultiplier(running ? snapshot.getCostMultiplier() * factor : factor,
            Math.max(snapshot.getCostMultiplierSeasonsRemaining(), seasons));
    }
}
