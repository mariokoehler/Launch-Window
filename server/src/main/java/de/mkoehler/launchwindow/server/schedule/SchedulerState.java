package de.mkoehler.launchwindow.server.schedule;

import java.time.LocalDate;

/**
 * <p>{@link TurnScheduler}'s own persisted state: the game turn number its
 * next resolution will use, and the last calendar date (in the schedule's
 * timezone) it already resolved a turn for. Unlike {@link GameMeta}, this
 * is written by the scheduler itself, never by a host.</p>
 *
 * <p>A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.</p>
 *
 * @author Mario Koehler
 */
public class SchedulerState {

    private long nextGameTurn = 1;
    private LocalDate lastResolvedDate;

    /**
     * Returns the game turn number the next resolution will use.
     *
     * @return the next game turn number
     */
    public long getNextGameTurn() {
        return nextGameTurn;
    }

    /**
     * Sets the game turn number the next resolution will use.
     *
     * @param nextGameTurn the new next game turn number
     */
    public void setNextGameTurn(long nextGameTurn) {
        this.nextGameTurn = nextGameTurn;
    }

    /**
     * Returns the last calendar date a turn was resolved for, or
     * {@code null} if no turn has been resolved yet.
     *
     * @return the last resolved date, or {@code null}
     */
    public LocalDate getLastResolvedDate() {
        return lastResolvedDate;
    }

    /**
     * Sets the last calendar date a turn was resolved for.
     *
     * @param lastResolvedDate the new last resolved date
     */
    public void setLastResolvedDate(LocalDate lastResolvedDate) {
        this.lastResolvedDate = lastResolvedDate;
    }
}
