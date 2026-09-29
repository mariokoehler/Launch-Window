package de.mkoehler.launchwindow.server.schedule;

import java.time.LocalDate;
import java.util.Objects;

/**
 * <p>An inclusive date range, in the game's configured timezone, on which no
 * turn resolves (see {@code design.md} &sect;3.6). Part of {@link GameMeta},
 * so this is also part of {@code game-meta.json}'s shape and can be
 * hand-edited by the host - including to cover the period between a server
 * going live and a game's announced start date, which uses this exact
 * mechanism rather than a dedicated field.</p>
 *
 * @param start the first blacked-out date, inclusive
 * @param end   the last blacked-out date, inclusive
 * @author Mario Koehler
 */
public record Blackout(LocalDate start, LocalDate end) {

    /**
     * Validates that both dates are present and not out of order.
     *
     * @throws IllegalArgumentException if {@code start} is after {@code end}
     */
    public Blackout {
        Objects.requireNonNull(start, "blackout start must not be null");
        Objects.requireNonNull(end, "blackout end must not be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("blackout start " + start + " is after end " + end);
        }
    }

    /**
     * Checks whether the given date falls within this blackout.
     *
     * @param date the date to check
     * @return {@code true} if {@code date} is within {@code [start, end]}
     */
    public boolean includes(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
