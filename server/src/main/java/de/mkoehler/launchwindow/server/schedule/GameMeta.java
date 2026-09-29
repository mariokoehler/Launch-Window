package de.mkoehler.launchwindow.server.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>The single game-meta file's contents (see {@code design.md} &sect;3.6
 * and &sect;3.7): the schedule profile and blackout list every game needs,
 * sitting alongside the per-player data under the server's data root.
 * Loaded and, on first boot, created by {@link GameMetaStore}.</p>
 *
 * <p>Game creation is a v1 host file edit rather than a client screen, so
 * this class's shape - not a UI form - is the thing a host actually edits
 * to configure a game: adding a blackout period is done by adding an entry
 * to {@link #getBlackouts()} in the file directly.</p>
 *
 * <p>A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.</p>
 *
 * @author Mario Koehler
 */
public class GameMeta {

    private ScheduleProfile schedule;
    private List<Blackout> blackouts = new ArrayList<>();

    /**
     * Creates an uninitialized instance, for Jackson.
     */
    public GameMeta() {
    }

    /**
     * Creates a new instance with the given fields.
     *
     * @param schedule  the schedule profile
     * @param blackouts the blackout list
     */
    public GameMeta(ScheduleProfile schedule, List<Blackout> blackouts) {
        this.schedule = schedule;
        setBlackouts(blackouts);
    }

    /**
     * Returns the schedule profile.
     *
     * @return the schedule profile
     */
    public ScheduleProfile getSchedule() {
        return schedule;
    }

    /**
     * Sets the schedule profile.
     *
     * @param schedule the new schedule profile
     */
    public void setSchedule(ScheduleProfile schedule) {
        this.schedule = schedule;
    }

    /**
     * Returns the blackout list.
     *
     * @return the blackout list
     */
    public List<Blackout> getBlackouts() {
        return blackouts;
    }

    /**
     * Replaces the blackout list, copying the given list defensively.
     *
     * @param blackouts the new blackout list
     */
    public void setBlackouts(List<Blackout> blackouts) {
        this.blackouts = blackouts == null ? new ArrayList<>() : new ArrayList<>(blackouts);
    }

    /**
     * Decides whether a turn may resolve on the given date: it must fall on
     * one of the schedule's active weekdays and not be covered by any
     * blackout. Does not check the time of day.
     *
     * @param date the date to check, in the schedule's own timezone
     * @return {@code true} if a turn may resolve on {@code date}
     */
    public boolean resolvesOn(LocalDate date) {
        if (!schedule.getActiveDays().contains(date.getDayOfWeek())) {
            return false;
        }
        return blackouts.stream().noneMatch(blackout -> blackout.includes(date));
    }

    /**
     * Checks that this instance and its schedule are well-formed, throwing
     * if not - a hand-edited config file is exactly where a missing field
     * or an inverted blackout range is likely to come from.
     *
     * @throws IllegalStateException if the schedule is missing or invalid,
     *                                or a blackout entry is null
     */
    void validate() {
        Objects.requireNonNull(schedule, "schedule must not be null").validate();
        for (Blackout blackout : blackouts) {
            Objects.requireNonNull(blackout, "a blackout entry must not be null");
        }
    }
}
