package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.HiringPoolBonus;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
     * No story repeats until every story of its tone has run; then the tone starts over, and the
     * other tone's record is kept.
     */
    @Test
    void storiesDontRepeatUntilAllHaveRun() {
        PrPress press = new PrPress(List.of(
            new PrPress.Story("g1", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("g2", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("g3", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("b1", PrPress.Tone.BAD, "h", "t")));
        RandomGenerator first = scripted();
        List<String> used = new ArrayList<>(List.of("gone", "b1"));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            assertTrue(seen.add(press.pick(PrPress.Tone.GOOD, first, used)), "no repeat within a round");
        }
        assertEquals(Set.of("g1", "g2", "g3"), seen);
        assertFalse(used.contains("gone"), "ids no longer in the file are dropped");
        assertEquals("g1", press.pick(PrPress.Tone.GOOD, first, used), "a new round starts");
        assertEquals(List.of("b1", "g1"), used, "the bad record is kept");
    }

    /**
     * The bundled press file loads, has both tones, unique ids, and only letters the fonts can draw.
     */
    @Test
    void theBundledPressStoriesAreWellFormed() {
        PrPress press = PrPress.load(Path.of("..", "assets", "content", "standard", PrPress.FILE));
        Set<String> ids = new HashSet<>();
        press.stories().forEach(story -> assertTrue(ids.add(story.id()), story.id()));
        assertFalse(press.stories().stream().noneMatch(story -> story.tone() == PrPress.Tone.GOOD));
        assertFalse(press.stories().stream().noneMatch(story -> story.tone() == PrPress.Tone.BAD));
        for (PrPress.Story story : press.stories()) {
            String text = story.headlineFor("Yuri Gagarin") + story.textFor("Yuri Gagarin");
            text.chars().forEach(c -> assertTrue(c == ' '
                || de.mkoehler.launchwindow.client.ui.Theme.CHARACTERS.indexOf(c) >= 0, story.id()));
            assertTrue(story.effects(PrPress.EffectKind.PRESTIGE_SHARE).size()
                <= (story.tone() == PrPress.Tone.GOOD ? 1 : 0), story.id() + ": one share, good stories only");
            for (PrPress.Effect effect : story.effects()) {
                String where = story.id() + " " + effect.kind();
                switch (effect.kind()) {
                    case HIRING_POOL, STANDOUT_CANDIDATE, GROUP_MORALE ->
                        assertDoesNotThrow(() -> PersonnelGroup.valueOf(effect.target()), where);
                    case ASTRONAUT_SKILL -> assertDoesNotThrow(() -> AstronautSkill.valueOf(effect.target()), where);
                    default -> assertEquals(null, effect.target(), where);
                }
                switch (effect.kind()) {
                    case HIRING_POOL, STANDOUT_CANDIDATE, RD_RESULTS, RUNNING_COSTS ->
                        assertTrue(effect.seasons() >= 1, where + " lasts");
                    default -> assertEquals(0, effect.seasons(), where + " is a one-off");
                }
                assertTrue(effect.amount() != 0, where + " does something");
                for (String sentence : List.of(TurnReportView.pressEffect(effect, "Yuri Gagarin"))) {
                    sentence.chars().forEach(c -> assertTrue(c == ' '
                        || de.mkoehler.launchwindow.client.ui.Theme.CHARACTERS.indexOf(c) >= 0, where));
                }
            }
        }
    }

    /**
     * A story's effects apply when it runs: its own prestige share, morale, a skill, cash and a
     * bigger pool, each reported as applied (cash in money).
     */
    @Test
    void storyEffectsApply() {
        PrPress press = new PrPress(List.of(new PrPress.Story("fx", PrPress.Tone.GOOD, "h", "t", List.of(
            new PrPress.Effect(PrPress.EffectKind.PRESTIGE_SHARE, null, 1.0, 0),
            new PrPress.Effect(PrPress.EffectKind.GROUP_MORALE, "SET", 5, 0),
            new PrPress.Effect(PrPress.EffectKind.ASTRONAUT_MORALE, null, -3, 0),
            new PrPress.Effect(PrPress.EffectKind.ASTRONAUT_SKILL, "FITNESS", -2, 0),
            new PrPress.Effect(PrPress.EffectKind.FUNDS, null, 0.1, 0),
            new PrPress.Effect(PrPress.EffectKind.FUNDING_RATE, null, 0.05, 0),
            new PrPress.Effect(PrPress.EffectKind.HIRING_POOL, "ASTRONAUT", 3, 2)))));
        PlayerSnapshot snapshot = snapshot(astronaut("a", 80, 1, 1));
        snapshot.setFundingRatePerSeason(2_000);
        SetSpecialist engineer = new SetSpecialist("s", "S", 30, 50, 50);
        snapshot.getSetSpecialists().add(engineer);
        PrDuty.start(snapshot, "a");

        PrDutyPerformed event = (PrDutyPerformed) PrDuty.advanceSeason(snapshot, press, scripted(0.05, 0.5)).get(0);

        assertEquals(32, event.prestige(), "16 + the full 16");
        assertEquals(55, engineer.getMorale());
        Astronaut astronaut = snapshot.getAstronauts().get(0);
        assertEquals(57, astronaut.getMorale(), "60 - 3");
        assertEquals(48.0, astronaut.getSkill(AstronautSkill.FITNESS));
        assertEquals(1_000 + 200, snapshot.getFunds());
        assertEquals(2_100, snapshot.getFundingRatePerSeason());
        assertEquals(List.of(new HiringPoolBonus("ASTRONAUT", 3, 0, 2)), snapshot.getHiringPoolBonuses());
        assertEquals(6, event.pressEffects().size(), "everything but the share");
        assertTrue(event.pressEffects().contains(new PrPress.Effect(PrPress.EffectKind.FUNDS, null, 200, 0)));
        assertTrue(event.pressEffects().contains(new PrPress.Effect(PrPress.EffectKind.FUNDING_RATE, null, 100, 0)));
    }

    /**
     * A raised funding rate only runs well clear of a review, which would reset it; a research
     * setback needs the content and a component in R&amp;D.
     */
    @Test
    void storiesNeedSomethingToActOn() {
        PrPress.Story raise = new PrPress.Story("r", PrPress.Tone.GOOD, "h", "t",
            List.of(new PrPress.Effect(PrPress.EffectKind.FUNDING_RATE, null, 0.05, 0)));
        PrPress.Story setback = new PrPress.Story("s", PrPress.Tone.BAD, "h", "t",
            List.of(new PrPress.Effect(PrPress.EffectKind.RESEARCH_SETBACK, null, 1, 0)));
        PlayerSnapshot snapshot = snapshot();
        assertTrue(PressEffects.usable(raise, snapshot, null, 1));
        assertTrue(PressEffects.usable(raise, snapshot, null, 10), "6 seasons to the review");
        assertFalse(PressEffects.usable(raise, snapshot, null, 11), "5 seasons to the review");
        assertFalse(PressEffects.usable(raise, snapshot, null, 16), "the review runs this season");
        assertFalse(PressEffects.usable(setback, snapshot, null, 1));
    }

    /**
     * When the only fresh story can't run, the usable ones keep cycling oldest first, so none
     * repeats within a round, and the fresh one keeps its turn for when it can run.
     */
    @Test
    void anUnusableFreshStoryKeepsItsTurn() {
        PrPress press = new PrPress(List.of(
            new PrPress.Story("a", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("b", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("c", PrPress.Tone.GOOD, "h", "t"),
            new PrPress.Story("fresh", PrPress.Tone.GOOD, "h", "t")));
        List<String> used = new ArrayList<>();
        List<String> drawn = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            drawn.add(press.pick(PrPress.Tone.GOOD, scripted(), used, story -> !story.id().equals("fresh")));
        }
        for (int i = 0; i + 3 <= drawn.size(); i++) {
            assertEquals(3, Set.copyOf(drawn.subList(i, i + 3)).size(), "no repeat within three: " + drawn);
        }
        assertFalse(used.contains("fresh"));
        assertEquals(null, press.pick(PrPress.Tone.GOOD, scripted(), used, story -> false));
        assertEquals("fresh", press.pick(PrPress.Tone.GOOD, scripted(), used));
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
