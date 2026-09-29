package de.mkoehler.launchwindow.server.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link GameMeta}.</p>
 *
 * @author Mario Koehler
 */
class GameMetaTest {

    private final ScheduleProfile weekdays = ScheduleProfile.weekdays(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin"));

    /**
     * Verifies that a date outside the active weekdays does not resolve.
     */
    @Test
    void resolvesOnRejectsAnInactiveWeekday() {
        GameMeta meta = new GameMeta(weekdays, List.of());

        // 2026-10-03 is a Saturday.
        assertFalse(meta.resolvesOn(LocalDate.of(2026, 10, 3)));
    }

    /**
     * Verifies that an active weekday with no blackout resolves.
     */
    @Test
    void resolvesOnAcceptsAnActiveWeekdayWithNoBlackout() {
        GameMeta meta = new GameMeta(weekdays, List.of());

        // 2026-10-05 is a Monday.
        assertTrue(meta.resolvesOn(LocalDate.of(2026, 10, 5)));
    }

    /**
     * Verifies that a date covered by a blackout does not resolve, even
     * though it is an active weekday.
     */
    @Test
    void resolvesOnRejectsADateInsideABlackout() {
        Blackout blackout = new Blackout(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        GameMeta meta = new GameMeta(weekdays, List.of(blackout));

        assertFalse(meta.resolvesOn(LocalDate.of(2026, 10, 6)));
    }

    /**
     * Verifies that validation rejects a missing schedule.
     */
    @Test
    void validateRejectsMissingSchedule() {
        GameMeta meta = new GameMeta(null, List.of());

        assertThrows(NullPointerException.class, meta::validate);
    }

    /**
     * Verifies that validation delegates to the schedule's own validation.
     */
    @Test
    void validateDelegatesToTheSchedule() {
        ScheduleProfile invalid = new ScheduleProfile(EnumSet.noneOf(DayOfWeek.class), LocalTime.of(9, 0), ZoneId.of("UTC"));
        GameMeta meta = new GameMeta(invalid, List.of());

        assertThrows(IllegalStateException.class, meta::validate);
    }

    /**
     * Verifies that {@code setBlackouts} copies the given list defensively,
     * so mutating the caller's list afterward does not affect the meta.
     */
    @Test
    void setBlackoutsCopiesDefensively() {
        GameMeta meta = new GameMeta(weekdays, List.of());
        ArrayList<Blackout> mutable = new ArrayList<>();
        mutable.add(new Blackout(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));
        meta.setBlackouts(mutable);

        mutable.add(new Blackout(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2)));

        assertEquals(1, meta.getBlackouts().size());
    }
}
