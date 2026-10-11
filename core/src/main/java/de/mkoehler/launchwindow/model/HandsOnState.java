package de.mkoehler.launchwindow.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A hands-on decision's puzzle as it stands ({@code ui-spec.md} component 26, type 11), kept on the
 * open decision; the rules are {@code rules.HandsOn}. Holds no seed: the rolls come from the
 * decision's, which the client never gets.
 *
 * @param held           what the player holds now, by resource, queued inputs already paid
 * @param round          the round being planned or executed, from 1
 * @param queue          the commands queued this round, in slot order
 * @param results        the last executed round's slots, for the screen; empty before the first
 * @param resultsRound   the round {@code results} belong to, 0 before the first
 * @param phase          where the puzzle is
 * @param slot           while {@link Phase#AWAITING_RESIST}, the slot that failed
 * @param rolls          how many rolls the puzzle has drawn, for the next roll's seed
 * @param failChance     the chance a rolling command fails, fixed at the start
 * @param exceptionalChance the chance it goes exceptionally well, fixed at the start
 * @param result         once finished, how it ended; {@code null} before
 * @author Mario Koehler
 */
public record HandsOnState(Map<String, Integer> held, int round, List<String> queue, List<Slot> results,
                           int resultsRound, Phase phase, int slot, int rolls, double failChance,
                           double exceptionalChance, Result result) {

    /** Where the puzzle is. */
    public enum Phase {
        /** The player queues commands for the round. */
        PLANNING,
        /** A command failed and the player may call in an expert. */
        AWAITING_RESIST,
        /** Over; {@code result} says how. */
        FINISHED
    }

    /** How a finished puzzle ended. Saved by name: constants are only ever appended. */
    public enum Result {
        /** The bonus amounts were met. */
        BONUS,
        /** The objective, not the bonus. */
        SUCCESS,
        /** Short of the objective. */
        FAILURE
    }

    /** A command's roll. */
    public enum Roll {
        /** One less of the main output. */
        FAIL,
        /** As written. */
        NORMAL,
        /** One more of the main output. */
        EXCEPTIONAL,
        /** The command doesn't roll. */
        NONE
    }

    /**
     * One executed slot.
     *
     * @param command  the command
     * @param rolled   what it rolled
     * @param resisted whether an expert turned a fail into a normal result
     * @param gained   what it made, by resource; empty while it waits on a resist choice
     */
    public record Slot(String command, Roll rolled, boolean resisted, Map<String, Integer> gained) {

        /**
         * Checks the command and keeps the amounts in order.
         *
         * @throws NullPointerException if the command or roll is {@code null}
         */
        public Slot {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(rolled, "rolled must not be null");
            gained = gained == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(gained));
        }
    }

    /**
     * Copies the collections.
     *
     * @throws NullPointerException if the phase is {@code null}
     */
    public HandsOnState {
        held = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(held == null ? Map.of() : held));
        queue = queue == null ? List.of() : List.copyOf(queue);
        results = results == null ? List.of() : List.copyOf(results);
        Objects.requireNonNull(phase, "phase must not be null");
    }

    /**
     * Returns how much of a resource the player holds.
     *
     * @param resource the resource
     * @return the amount
     */
    public int held(String resource) {
        return held.getOrDefault(resource, 0);
    }
}
