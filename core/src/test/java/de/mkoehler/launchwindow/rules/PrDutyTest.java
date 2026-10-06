package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PrDuty}: starting and recalling, the yield with fame and its cap, diminishing
 * returns, rust and its season count, and press stories ({@code design.md} &sect;2.2(c)).
 *
 * @author Mario Koehler
 */
class PrDutyTest {

    private static final PrPress PRESS = new PrPress(List.of(
        new PrPress.Story("good", PrPress.Tone.GOOD, "Crowds for {name}", "{name} is a hit."),
        new PrPress.Story("bad", PrPress.Tone.BAD, "Oops, {name}", "{name} said the wrong thing.")));

    /** A random source that never makes the papers. */
    private static final RandomGenerator QUIET = scripted(0.99);

    private static Astronaut astronaut(String id, double leadership, int flights, int firsts) {
        Astronaut astronaut = new Astronaut(id, "A " + id, 30, 60, 50);
        for (AstronautSkill skill : AstronautSkill.values()) {
            astronaut.setSkill(skill, 50.0);
        }
        astronaut.setSkill(AstronautSkill.LEADERSHIP, leadership);
        astronaut.setFlightsFlown(flights);
        astronaut.setFirstsFlown(firsts);
        return astronaut;
    }

    private static PlayerSnapshot snapshot(Astronaut... astronauts) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 1_000, 100, 0, 1);
        snapshot.getAstronauts().addAll(List.of(astronauts));
        return snapshot;
    }

    /**
     * Only an idle, available astronaut can go on tour, and only one on tour can be recalled.
     */
    @Test
    void startAndRecall() {
        Astronaut busy = astronaut("b", 50, 0, 0);
        busy.setAssignmentState(AssignmentState.IN_TRAINING);
        PlayerSnapshot snapshot = snapshot(astronaut("a", 50, 0, 0), busy);
        assertEquals(PrDuty.Outcome.UNKNOWN_ASTRONAUT, PrDuty.start(snapshot, "x"));
        assertEquals(PrDuty.Outcome.NOT_IDLE, PrDuty.start(snapshot, "b"));
        assertEquals(PrDuty.Outcome.NOT_ON_DUTY, PrDuty.recall(snapshot, "a"));
        assertEquals(PrDuty.Outcome.STARTED, PrDuty.start(snapshot, "a"));
        assertEquals(AssignmentState.PR_DUTY, snapshot.getAstronauts().get(0).getAssignmentState());
        assertEquals(PrDuty.Outcome.RECALLED, PrDuty.recall(snapshot, "a"));
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Yield is Leadership / 10 plus 3 per flight and 5 per flight with a first, at most 25.
     */
    @Test
    void yieldAddsFameAndIsCapped() {
        assertEquals(5, PrDuty.individualYield(astronaut("r", 50, 0, 0)), "an unflown rookie");
        assertEquals(13, PrDuty.individualYield(astronaut("v", 70, 2, 0)), "a veteran");
        assertEquals(16, PrDuty.individualYield(astronaut("g", 80, 1, 1)), "a Gagarin");
        assertEquals(PrDuty.MAX_YIELD, PrDuty.individualYield(astronaut("s", 90, 4, 3)), "capped");
    }

    /**
     * The best on tour earns all of their yield, the next 60 %, then 30 %, then nothing; ties keep
     * roster order.
     */
    @Test
    void diminishingReturnsRankByYield() {
        Map<String, Long> yields = PrDuty.effectiveYields(List.of(astronaut("low", 50, 0, 0),
            astronaut("high", 80, 1, 1), astronaut("mid", 70, 2, 0), astronaut("tie", 50, 0, 0)));
        assertEquals(List.of("low", "high", "mid", "tie"), List.copyOf(yields.keySet()), "roster order kept");
        assertEquals(16L, yields.get("high"));
        assertEquals(8L, yields.get("mid"), "13 x 0.6, rounded");
        assertEquals(2L, yields.get("low"), "5 x 0.3, rounded, first of the tied pair");
        assertEquals(0L, yields.get("tie"));
    }

    /**
     * A season on tour pays the prestige; rust starts in the third season in a row, floored at 0; a
     * season off duty resets the count.
     */
    @Test
    void seasonsPayAndRustSetsInOnTheThird() {
        Astronaut astronaut = astronaut("a", 80, 1, 1);
        astronaut.setSkill(AstronautSkill.FITNESS, 0.5);
        PlayerSnapshot snapshot = snapshot(astronaut);
        PrDuty.start(snapshot, "a");

        for (int season = 1; season <= 2; season++) {
            PrDuty.advanceSeason(snapshot, PRESS, QUIET);
            assertEquals(50.0, astronaut.getSkill(AstronautSkill.PILOTING), "no rust in season " + season);
        }
        assertEquals(100 + 2 * 16, snapshot.getPrestige());
        List<ResolutionEvent> third = PrDuty.advanceSeason(snapshot, PRESS, QUIET);
        assertEquals(49.0, astronaut.getSkill(AstronautSkill.PILOTING));
        assertEquals(0.0, astronaut.getSkill(AstronautSkill.FITNESS), "floored at 0");
        assertEquals(new PrDutyPerformed("a", "A a", 16, 16, null, 1.0), third.get(0));

        PrDuty.recall(snapshot, "a");
        PrDuty.advanceSeason(snapshot, PRESS, QUIET);
        assertEquals(0, astronaut.getPrDutySeasons(), "a season off duty resets the count");
    }

    /**
     * Recalling and restarting on the same day doesn't reset the count: the pass still finds the
     * astronaut on duty.
     */
    @Test
    void recallAndRestartSameDayKeepsTheRustClock() {
        Astronaut astronaut = astronaut("a", 50, 0, 0);
        PlayerSnapshot snapshot = snapshot(astronaut);
        PrDuty.start(snapshot, "a");
        PrDuty.advanceSeason(snapshot, PRESS, QUIET);
        PrDuty.advanceSeason(snapshot, PRESS, QUIET);
        PrDuty.recall(snapshot, "a");
        PrDuty.start(snapshot, "a");
        PrDuty.advanceSeason(snapshot, PRESS, QUIET);
        assertEquals(3, astronaut.getPrDutySeasons());
        assertEquals(49.0, astronaut.getSkill(AstronautSkill.PILOTING), "rust in the third season regardless");
    }

    /**
     * With nobody on tour the pass draws no randomness, so other resolvers' streams don't shift.
     */
    @Test
    void nobodyOnTourDrawsNothing() {
        RandomGenerator forbidden = () -> {
            throw new AssertionError("drew a random number");
        };
        PlayerSnapshot snapshot = snapshot(astronaut("a", 50, 0, 0));
        assertTrue(PrDuty.advanceSeason(snapshot, PRESS, forbidden).isEmpty());
    }

    /**
     * A good story adds half the yield; a bad one turns the season into a loss of half of it.
     */
    @Test
    void pressStoriesHelpOrHurt() {
        PlayerSnapshot good = snapshot(astronaut("a", 80, 1, 1));
        PrDuty.start(good, "a");
        // Makes the papers (0.05), good (0.5), first good story (index 0).
        PrDutyPerformed event = (PrDutyPerformed) PrDuty.advanceSeason(good, PRESS, scripted(0.05, 0.5)).get(0);
        assertEquals("good", event.pressStoryId());
        assertEquals(24, event.prestige(), "16 + 8");
        assertEquals(100 + 24, good.getPrestige());

        PlayerSnapshot bad = snapshot(astronaut("a", 80, 1, 1));
        PrDuty.start(bad, "a");
        event = (PrDutyPerformed) PrDuty.advanceSeason(bad, PRESS, scripted(0.05, 0.9)).get(0);
        assertEquals("bad", event.pressStoryId());
        assertEquals(-8, event.prestige());
        assertEquals(100 - 8, bad.getPrestige());
    }

    /**
     * The bundled press file loads, has both tones, unique ids, and only letters the fonts can draw.
     */
    @Test
    void theBundledPressStoriesAreWellFormed() {
        PrPress press = PrPress.load(Path.of("..", "assets", "content", PrPress.FILE));
        Set<String> ids = new HashSet<>();
        press.stories().forEach(story -> assertTrue(ids.add(story.id()), story.id()));
        assertFalse(press.stories().stream().noneMatch(story -> story.tone() == PrPress.Tone.GOOD));
        assertFalse(press.stories().stream().noneMatch(story -> story.tone() == PrPress.Tone.BAD));
        for (PrPress.Story story : press.stories()) {
            String text = story.headlineFor("Yuri Gagarin") + story.textFor("Yuri Gagarin");
            text.chars().forEach(c -> assertTrue(c == ' '
                || de.mkoehler.launchwindow.client.ui.Theme.CHARACTERS.indexOf(c) >= 0, story.id()));
        }
    }

    /**
     * A random source that returns the given doubles in turn, then 0.99; {@code nextInt} returns 0.
     */
    private static RandomGenerator scripted(double... values) {
        Deque<Double> queue = new ArrayDeque<>();
        for (double value : values) {
            queue.add(value);
        }
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public double nextDouble() {
                return queue.isEmpty() ? 0.99 : queue.poll();
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }
}
