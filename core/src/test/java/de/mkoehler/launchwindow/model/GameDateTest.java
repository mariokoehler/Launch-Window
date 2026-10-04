package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link GameDate}.
 *
 * @author Mario Koehler
 */
class GameDateTest {

    /**
     * Turn 1 is winter 1955, and each turn moves one season on, rolling the year after fall.
     */
    @Test
    void eachTurnIsOneSeason() {
        assertEquals(new GameDate(1955, Season.WINTER), GameDate.ofTurn(1));
        assertEquals(new GameDate(1955, Season.SPRING), GameDate.ofTurn(2));
        assertEquals(new GameDate(1955, Season.FALL), GameDate.ofTurn(4));
        assertEquals(new GameDate(1956, Season.WINTER), GameDate.ofTurn(5));
        assertEquals(new GameDate(1969, Season.SUMMER), GameDate.ofTurn(14 * 4 + 3));
    }

    /**
     * The label reads as the player sees it.
     */
    @Test
    void theLabelNamesSeasonAndYear() {
        assertEquals("Winter 1955", GameDate.ofTurn(1).label());
        assertEquals("Fall 1957", GameDate.ofTurn(12).label());
    }

    /**
     * There is no turn 0.
     */
    @Test
    void turnsStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> GameDate.ofTurn(0));
    }
}
