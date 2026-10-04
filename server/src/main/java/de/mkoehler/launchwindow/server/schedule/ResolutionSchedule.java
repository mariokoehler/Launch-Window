package de.mkoehler.launchwindow.server.schedule;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * When turns resolve, as pure functions of the game's configuration, the scheduler's progress and
 * the current time. {@link TurnScheduler} uses these to decide whether to resolve now, and the
 * game info (gap G1) uses the same ones to tell players when the next resolution is, so the
 * countdown a player sees can never drift from what the scheduler actually does.
 *
 * <p>A resolution is due once per resolution day ({@link GameMeta#resolvesOn}), at or after the
 * schedule's resolution time, in the schedule's time zone. A day the server missed entirely is
 * skipped, not caught up: one turn resolves at the next check ({@code design.md} &sect;3.6).</p>
 *
 * @author Mario Koehler
 */
public final class ResolutionSchedule {

    /** How far ahead {@link #next} looks before concluding no resolution day is scheduled at all. */
    private static final int LOOKAHEAD_DAYS = 400;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private ResolutionSchedule() {
    }

    /**
     * Returns whether a turn should resolve now.
     *
     * @param meta             the game's configuration
     * @param lastResolvedDate the last day a turn resolved, in the schedule's zone
     * @param now              the current time
     * @return whether to resolve
     */
    public static boolean isDue(GameMeta meta, LocalDate lastResolvedDate, ZonedDateTime now) {
        ZonedDateTime local = inScheduleZone(meta, now);
        LocalDate today = local.toLocalDate();
        return !today.equals(lastResolvedDate)
            && meta.resolvesOn(today)
            && !local.toLocalTime().isBefore(meta.getSchedule().getResolutionTime());
    }

    /**
     * Returns the "last resolved" day a brand-new game starts from: yesterday if today's
     * resolution time is still ahead, so today still resolves, otherwise today, so a server
     * started after the resolution time doesn't resolve a turn the moment it comes up.
     *
     * @param meta the game's configuration
     * @param now  the current time
     * @return the day to record as last resolved
     */
    public static LocalDate bootstrapLastResolvedDate(GameMeta meta, ZonedDateTime now) {
        ZonedDateTime local = inScheduleZone(meta, now);
        boolean beforeResolutionTime = local.toLocalTime().isBefore(meta.getSchedule().getResolutionTime());
        return beforeResolutionTime ? local.toLocalDate().minusDays(1) : local.toLocalDate();
    }

    /**
     * Returns when the next turn resolves: now, if one is due; otherwise the resolution time of the
     * next resolution day after the last resolved one.
     *
     * @param meta             the game's configuration
     * @param lastResolvedDate the last day a turn resolved, in the schedule's zone
     * @param now              the current time
     * @return the next resolution, or empty if no resolution day falls within the next year or so
     *     (every day blacked out, say)
     */
    public static Optional<ZonedDateTime> next(GameMeta meta, LocalDate lastResolvedDate, ZonedDateTime now) {
        ZonedDateTime local = inScheduleZone(meta, now);
        if (isDue(meta, lastResolvedDate, local)) {
            return Optional.of(local);
        }
        LocalDate day = local.toLocalDate();
        for (int i = 0; i < LOOKAHEAD_DAYS; i++, day = day.plusDays(1)) {
            if (day.equals(lastResolvedDate) || !meta.resolvesOn(day)) {
                continue;
            }
            // ZonedDateTime.of moves a time that falls into a daylight-saving gap forward, as a clock would.
            ZonedDateTime candidate = ZonedDateTime.of(day, meta.getSchedule().getResolutionTime(),
                meta.getSchedule().getTimezone());
            if (!candidate.isBefore(local)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static ZonedDateTime inScheduleZone(GameMeta meta, ZonedDateTime now) {
        Objects.requireNonNull(meta, "meta must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return now.withZoneSameInstant(meta.getSchedule().getTimezone());
    }
}
