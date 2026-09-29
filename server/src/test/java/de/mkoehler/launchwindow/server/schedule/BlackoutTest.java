package de.mkoehler.launchwindow.server.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link Blackout}.</p>
 *
 * @author Mario Koehler
 */
class BlackoutTest {

    /**
     * Verifies that a date range with start after end is rejected.
     */
    @Test
    void constructorRejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
            () -> new Blackout(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1)));
    }

    /**
     * Verifies that both range boundaries are treated as included.
     */
    @Test
    void includesIsInclusiveOfBothBoundaries() {
        Blackout blackout = new Blackout(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        assertTrue(blackout.includes(LocalDate.of(2026, 5, 1)));
        assertTrue(blackout.includes(LocalDate.of(2026, 5, 2)));
        assertTrue(blackout.includes(LocalDate.of(2026, 5, 3)));
    }

    /**
     * Verifies that dates outside the range are excluded.
     */
    @Test
    void includesExcludesDatesOutsideTheRange() {
        Blackout blackout = new Blackout(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        assertFalse(blackout.includes(LocalDate.of(2026, 4, 30)));
        assertFalse(blackout.includes(LocalDate.of(2026, 5, 4)));
    }
}
