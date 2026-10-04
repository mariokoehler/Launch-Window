package de.mkoehler.launchwindow.server.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ResolutionSchedule}: when a turn is due, and when the next one is, for the
 * countdown players see.
 *
 * @author Mario Koehler
 */
class ResolutionScheduleTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    /** A Wednesday. */
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);

    private static GameMeta daily(Blackout... blackouts) {
        return new GameMeta(ScheduleProfile.daily(LocalTime.of(9, 0), BERLIN), List.of(blackouts));
    }

    private static ZonedDateTime at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), BERLIN);
    }

    /**
     * Before today's resolution time, the next resolution is today at that time.
     */
    @Test
    void beforeTheTimeItIsToday() {
        assertEquals(Optional.of(at(WEDNESDAY, 9, 0)),
            ResolutionSchedule.next(daily(), WEDNESDAY.minusDays(1), at(WEDNESDAY, 7, 30)));
        assertFalse(ResolutionSchedule.isDue(daily(), WEDNESDAY.minusDays(1), at(WEDNESDAY, 7, 30)));
    }

    /**
     * Once today has resolved, the next resolution is tomorrow.
     */
    @Test
    void afterResolvingItIsTomorrow() {
        assertEquals(Optional.of(at(WEDNESDAY.plusDays(1), 9, 0)),
            ResolutionSchedule.next(daily(), WEDNESDAY, at(WEDNESDAY, 12, 0)));
    }

    /**
     * Past the time but not yet resolved (the server's next tick will do it): due, and "now".
     */
    @Test
    void anOverdueTurnIsDueNow() {
        ZonedDateTime now = at(WEDNESDAY, 9, 1);
        assertTrue(ResolutionSchedule.isDue(daily(), WEDNESDAY.minusDays(1), now));
        assertEquals(Optional.of(now), ResolutionSchedule.next(daily(), WEDNESDAY.minusDays(1), now));
    }

    /**
     * A weekday game skips the weekend: on Friday evening the next resolution is Monday morning.
     */
    @Test
    void weekdaysSkipTheWeekend() {
        GameMeta weekdays = new GameMeta(ScheduleProfile.weekdays(LocalTime.of(9, 0), BERLIN), List.of());
        LocalDate friday = WEDNESDAY.plusDays(2);
        assertEquals(Optional.of(at(friday.plusDays(3), 9, 0)),
            ResolutionSchedule.next(weekdays, friday, at(friday, 20, 0)));
    }

    /**
     * A blackout (the signup window is one) pushes the next resolution past its last day.
     */
    @Test
    void aBlackoutPushesItPastTheEnd() {
        GameMeta meta = daily(new Blackout(WEDNESDAY, WEDNESDAY.plusDays(4)));
        assertEquals(Optional.of(at(WEDNESDAY.plusDays(5), 9, 0)),
            ResolutionSchedule.next(meta, WEDNESDAY.minusDays(1), at(WEDNESDAY, 10, 0)));
    }

    /**
     * Across the switch to summer time the resolution stays at 09:00 local time.
     */
    @Test
    void daylightSavingKeepsTheLocalTime() {
        LocalDate dayBeforeSwitch = LocalDate.of(2027, 3, 27);
        ZonedDateTime next = ResolutionSchedule.next(daily(), dayBeforeSwitch, at(dayBeforeSwitch, 10, 0)).orElseThrow();
        assertEquals(LocalTime.of(9, 0), next.toLocalTime());
        assertEquals(dayBeforeSwitch.plusDays(1), next.toLocalDate());
    }

    /**
     * A brand-new game started before today's time resolves today; started after it, tomorrow.
     */
    @Test
    void aNewGameBootstrapsAroundTheTime() {
        assertEquals(WEDNESDAY.minusDays(1), ResolutionSchedule.bootstrapLastResolvedDate(daily(), at(WEDNESDAY, 8, 0)));
        assertEquals(WEDNESDAY, ResolutionSchedule.bootstrapLastResolvedDate(daily(), at(WEDNESDAY, 10, 0)));
    }

    /**
     * Times given in another zone are compared in the schedule's own zone.
     */
    @Test
    void otherZonesAreConverted() {
        ZonedDateTime utc = at(WEDNESDAY, 7, 30).withZoneSameInstant(ZoneId.of("UTC"));
        assertEquals(Optional.of(at(WEDNESDAY, 9, 0)), ResolutionSchedule.next(daily(), WEDNESDAY.minusDays(1), utc));
    }

    /**
     * With every day blacked out there is no next resolution.
     */
    @Test
    void noResolutionDayMeansNone() {
        GameMeta meta = daily(new Blackout(WEDNESDAY, WEDNESDAY.plusYears(2)));
        assertEquals(Optional.empty(), ResolutionSchedule.next(meta, WEDNESDAY.minusDays(1), at(WEDNESDAY, 8, 0)));
    }
}
