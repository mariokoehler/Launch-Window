package de.mkoehler.launchwindow.server.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>A game's turn-resolution schedule (see {@code design.md} &sect;3.6): the
 * weekdays a turn may resolve on, the local time of day it resolves at, and
 * the timezone that time of day and every weekday check are evaluated in.
 * Part of {@link GameMeta}, so this is also part of {@code game-meta.json}'s
 * shape and can be hand-edited by the host.</p>
 *
 * <p>A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.</p>
 *
 * @author Mario Koehler
 */
public class ScheduleProfile {

    private Set<DayOfWeek> activeDays = EnumSet.noneOf(DayOfWeek.class);
    private LocalTime resolutionTime;
    private ZoneId timezone;

    /**
     * Creates an uninitialized profile, for Jackson.
     */
    public ScheduleProfile() {
    }

    /**
     * Creates a profile with the given fields.
     *
     * @param activeDays     the weekdays a turn may resolve on
     * @param resolutionTime the local time of day a turn resolves at
     * @param timezone       the timezone {@code activeDays} and
     *                       {@code resolutionTime} are evaluated in
     */
    public ScheduleProfile(Set<DayOfWeek> activeDays, LocalTime resolutionTime, ZoneId timezone) {
        setActiveDays(activeDays);
        this.resolutionTime = resolutionTime;
        this.timezone = timezone;
    }

    /**
     * Builds the "DAILY" preset: every weekday active.
     *
     * @param resolutionTime the local time of day a turn resolves at
     * @param timezone       the timezone to evaluate against
     * @return the new profile
     */
    public static ScheduleProfile daily(LocalTime resolutionTime, ZoneId timezone) {
        return new ScheduleProfile(EnumSet.allOf(DayOfWeek.class), resolutionTime, timezone);
    }

    /**
     * Builds the "WEEKDAYS" preset: Monday through Friday active, Saturday
     * and Sunday excluded.
     *
     * @param resolutionTime the local time of day a turn resolves at
     * @param timezone       the timezone to evaluate against
     * @return the new profile
     */
    public static ScheduleProfile weekdays(LocalTime resolutionTime, ZoneId timezone) {
        return new ScheduleProfile(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), resolutionTime, timezone);
    }

    /**
     * Returns the weekdays a turn may resolve on.
     *
     * @return the active weekdays
     */
    public Set<DayOfWeek> getActiveDays() {
        return activeDays;
    }

    /**
     * Replaces the active weekdays, copying the given set defensively.
     *
     * @param activeDays the new active weekdays
     */
    public void setActiveDays(Set<DayOfWeek> activeDays) {
        this.activeDays = activeDays == null ? new LinkedHashSet<>() : new LinkedHashSet<>(activeDays);
    }

    /**
     * Returns the local time of day a turn resolves at.
     *
     * @return the resolution time
     */
    public LocalTime getResolutionTime() {
        return resolutionTime;
    }

    /**
     * Sets the local time of day a turn resolves at.
     *
     * @param resolutionTime the new resolution time
     */
    public void setResolutionTime(LocalTime resolutionTime) {
        this.resolutionTime = resolutionTime;
    }

    /**
     * Returns the timezone {@link #getActiveDays()} and
     * {@link #getResolutionTime()} are evaluated in.
     *
     * @return the timezone
     */
    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * Sets the timezone {@link #getActiveDays()} and
     * {@link #getResolutionTime()} are evaluated in.
     *
     * @param timezone the new timezone
     */
    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    /**
     * Checks that every field is present and {@link #activeDays} is
     * non-empty, throwing if not - a hand-edited config file is exactly
     * where a missing or empty field is likely to come from.
     *
     * @throws IllegalStateException if any field is missing or invalid
     */
    void validate() {
        if (activeDays == null || activeDays.isEmpty()) {
            throw new IllegalStateException("schedule.activeDays must not be empty");
        }
        Objects.requireNonNull(resolutionTime, "schedule.resolutionTime must not be null");
        Objects.requireNonNull(timezone, "schedule.timezone must not be null");
    }
}
