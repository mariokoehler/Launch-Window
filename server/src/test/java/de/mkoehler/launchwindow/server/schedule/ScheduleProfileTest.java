package de.mkoehler.launchwindow.server.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link ScheduleProfile}.</p>
 *
 * @author Mario Koehler
 */
class ScheduleProfileTest {

    /**
     * Verifies that the DAILY preset activates every weekday.
     */
    @Test
    void dailyActivatesEveryWeekday() {
        ScheduleProfile profile = ScheduleProfile.daily(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin"));

        assertEquals(EnumSet.allOf(DayOfWeek.class), profile.getActiveDays());
    }

    /**
     * Verifies that the WEEKDAYS preset activates Monday through Friday and
     * excludes the weekend.
     */
    @Test
    void weekdaysExcludesTheWeekend() {
        ScheduleProfile profile = ScheduleProfile.weekdays(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin"));

        assertEquals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), profile.getActiveDays());
        assertTrue(profile.getActiveDays().contains(DayOfWeek.FRIDAY));
    }

    /**
     * Verifies that {@code setActiveDays} copies the given set defensively,
     * so mutating the caller's set afterward does not affect the profile.
     */
    @Test
    void setActiveDaysCopiesDefensively() {
        ScheduleProfile profile = ScheduleProfile.daily(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin"));
        Set<DayOfWeek> mutable = EnumSet.of(DayOfWeek.MONDAY);
        profile.setActiveDays(mutable);

        mutable.add(DayOfWeek.TUESDAY);

        assertEquals(EnumSet.of(DayOfWeek.MONDAY), profile.getActiveDays());
    }

    /**
     * Verifies that validation rejects an empty active-days set.
     */
    @Test
    void validateRejectsEmptyActiveDays() {
        ScheduleProfile profile = new ScheduleProfile(EnumSet.noneOf(DayOfWeek.class), LocalTime.of(9, 0), ZoneId.of("UTC"));

        assertThrows(IllegalStateException.class, profile::validate);
    }

    /**
     * Verifies that validation rejects a missing resolution time or
     * timezone.
     */
    @Test
    void validateRejectsMissingTimeOrZone() {
        ScheduleProfile missingTime = new ScheduleProfile(EnumSet.allOf(DayOfWeek.class), null, ZoneId.of("UTC"));
        ScheduleProfile missingZone = new ScheduleProfile(EnumSet.allOf(DayOfWeek.class), LocalTime.of(9, 0), null);

        assertThrows(NullPointerException.class, missingTime::validate);
        assertThrows(NullPointerException.class, missingZone::validate);
    }
}
