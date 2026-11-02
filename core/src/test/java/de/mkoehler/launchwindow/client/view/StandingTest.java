package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.StandingSeries;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link Standing}, the chart's numbers ({@code ui-spec.md} component 30).
 */
class StandingTest {

    private static StandingSeries you(int points, long... reviews) {
        return new StandingSeries("Zarya", true, false, "kMe", 0, new long[points], new long[points], reviews,
            new long[0]);
    }

    private static StandingSeries rival(String name, long firstTurn, int points) {
        return new StandingSeries(name, false, true, "k" + name, firstTurn, new long[points], new long[0], new long[0],
            new long[0]);
    }

    /** A chart at the window's size shows it all; one point more drops the oldest. */
    @Test
    void theWindowSlidesPastItsSizeUnlessTheWholeGameIsAsked() {
        Standing.Model full = Standing.of(List.of(you(Standing.WINDOW)), null, false);
        assertFalse(full.windowed());
        assertEquals(0, full.from());

        Standing.Model over = Standing.of(List.of(you(Standing.WINDOW + 1, 0, 1, 40)), null, false);
        assertTrue(over.windowed());
        assertEquals(1, over.from());
        assertEquals(Standing.WINDOW, over.points());
        assertEquals(List.of(new Standing.Review(1, 2), new Standing.Review(40, 3)), over.reviews());

        Standing.Model whole = Standing.of(List.of(you(Standing.WINDOW + 1)), null, true);
        assertFalse(whole.windowed());
        assertEquals(0, whole.from());
    }

    /** One point draws no line; a late joiner's line starts late; the player's line is drawn last. */
    @Test
    void linesKeepTheirOwnStart() {
        assertTrue(Standing.of(List.of(StandingSeries.of("Zarya", true, false, "kMe", List.of())), null, false).empty());
        Standing.Model model = Standing.of(List.of(rival("Cape", 8, 24), you(32)), null, false);
        assertEquals("Cape (bot)", model.lines().get(0).name());
        assertTrue(model.lines().get(1).you());
        assertFalse(model.lines().get(0).has(7));
        assertTrue(model.lines().get(0).has(8));
        assertEquals(31, model.to());
    }

    /** A rival's colour goes by the agencies' order, in the chart and the race alike, and goals are marked. */
    @Test
    void coloursMatchTheRaceAndFirstsAreMarked() {
        List<RivalSummary> race = List.of(
            RivalSummary.of("Orion", "A", Archetype.NASA, false, Map.of("g", 5), "kOrion"),
            RivalSummary.of("Zarya", "B", Archetype.SOVIET, true, Map.of("g", 9), "kMe"),
            RivalSummary.of("Cape", "C", Archetype.NASA, false, Map.of(), "kCape"));
        assertArrayEquals(new int[] {0, -1, 1}, Standing.colours(race));
        Standing.Model model = Standing.of(List.of(rival("Orion", 0, 12), you(12), rival("Cape", 0, 12)), race, false);
        assertEquals(0, model.lines().get(0).colour());
        assertEquals(1, model.lines().get(1).colour());
        assertEquals(Standing.EventKind.FIRST, model.lines().get(0).goals().getFirst().kind());
        assertEquals(Standing.EventKind.GOAL, model.yours().goals().getFirst().kind());
        assertTrue(model.events(5, false).isEmpty());
        assertEquals(1, model.events(5, true).size());
    }

    /** Ticks are round, include zero, and cover the range, debt too. */
    @Test
    void ticksAreRound() {
        assertArrayEquals(new long[] {0, 2_000, 4_000, 6_000}, Standing.ticks(250, 5_030, 3));
        assertArrayEquals(new long[] {0, 10_000, 20_000, 30_000}, Standing.ticks(4_000, 25_000, 3));
        assertArrayEquals(new long[] {-10_000, 0, 10_000, 20_000, 30_000}, Standing.ticks(-4_500, 25_000, 3));
        assertArrayEquals(new long[] {0, 2_500, 5_000, 7_500, 10_000}, Standing.ticks(250, 9_820, 4));
        assertEquals(2, Standing.ticks(0, 0, 3).length);
    }

    /** Labels stay apart and in order, inside their bounds. */
    @Test
    void labelsArePushedApart() {
        float[] placed = Standing.pushApart(new float[] {100f, 110f, 300f}, 34f, 0f, 400f);
        assertEquals(34f, placed[1] - placed[0], 0.01f);
        assertEquals(300f, placed[2], 0.01f);
        float[] top = Standing.pushApart(new float[] {395f, 398f}, 34f, 0f, 400f);
        assertEquals(400f, top[1], 0.01f);
        assertEquals(366f, top[0], 0.01f);
    }

    /** The first point is the start; the axis names years, or every season with few points. */
    @Test
    void theAxisReadsDates() {
        assertEquals("Start", Standing.date(0));
        assertEquals("Winter 1955", Standing.date(1));
        assertEquals(List.of(0L, 1L, 2L), Standing.labelledTurns(0, 2, 1500f, 64f));
        assertEquals("1955", Standing.axisLabel(1, 40));
        assertEquals(Arrays.asList(1L, 5L, 9L), Standing.labelledTurns(0, 11, 1500f, 64f));
    }
}
