package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link RandomEvents}: each gate and effect in isolation where the
 * state allows it, and a seeded long run that checks every season's invariants
 * and that every sub-slice-(a) event actually fires. See {@code design.md} &sect;2.7.</p>
 *
 * @author Mario Koehler
 */
class RandomEventsTest {

    /** Every event kind - all twenty are implemented as of sub-slice (b). */
    private static final Set<RandomEventKind> IMPLEMENTED = EnumSet.allOf(RandomEventKind.class);

    /** The roster with BASPM's own ranges and every chance set to a given value. */
    private static RandomEventRoster roster(int badPercent, int goodPercent) {
        RandomEventRoster base = RandomEventRoster.baspmClose();
        return new RandomEventRoster(badPercent, goodPercent, base.events());
    }

    private static PlayerSnapshot quietPlayer(long funds, long prestige) {
        return new PlayerSnapshot("Agency", Archetype.NASA, funds, prestige, 3_000, 1);
    }

    /**
     * Applied kinds, in order, for one season of a roster with the given chances.
     */
    private static List<RandomEventKind> kindsApplied(List<ResolutionEvent> events) {
        return events.stream().filter(event -> event instanceof RandomEventOccurred)
            .map(event -> ((RandomEventOccurred) event).kind()).toList();
    }

    /**
     * Over many seeds in a state with positive funds and nothing else to act on,
     * unexpected cost is one of the events that can apply, and when it does, it takes
     * its rolled share of funds.
     */
    @Test
    void unexpectedCostTakesItsRolledShareOfFunds() {
        boolean sawIt = false;
        for (long seed = 0; seed < 200; seed++) {
            PlayerSnapshot player = quietPlayer(10_000, 0);
            List<ResolutionEvent> events = RandomEvents.advanceSeason(player, roster(100, 0), new Random(seed), 5, 5);
            for (ResolutionEvent event : events) {
                if (event instanceof RandomEventOccurred occurred && occurred.kind() == RandomEventKind.UNEXPECTED_COST) {
                    sawIt = true;
                    assertTrue(occurred.magnitude() >= 7 && occurred.magnitude() < 15,
                        "7-15% range, got " + occurred.magnitude());
                    long expectedCost = (long) Math.floor(10_000 * occurred.magnitude() / 100.0);
                    assertEquals(10_000 - expectedCost, player.getFunds());
                }
            }
        }
        assertTrue(sawIt, "unexpected cost should be reachable from a funded, quiet state");
    }

    /**
     * In debt there is no cash to take, so no funds-based event ever applies, whatever else fires.
     */
    @Test
    void debtBlocksTheCashEventsEverywhere() {
        for (long seed = 0; seed < 200; seed++) {
            PlayerSnapshot player = quietPlayer(-500, 0);
            for (RandomEventKind kind : kindsApplied(RandomEvents.advanceSeason(player, roster(100, 0),
                    new Random(seed), 5, 5))) {
                assertTrue(kind != RandomEventKind.UNEXPECTED_COST
                    && kind != RandomEventKind.UNEXPECTED_COST_AND_NO_LAUNCHES, "cash event in debt: " + kind);
            }
            assertEquals(-500, player.getFunds());
        }
    }

    /**
     * A morale drop needs a mission failure this very season: without one it never
     * applies, and with one it can.
     */
    @Test
    void moraleDropNeedsAFailureThisSeason() {
        for (long seed = 0; seed < 200; seed++) {
            PlayerSnapshot noFailure = quietPlayer(0, 0);
            noFailure.setAstronauts(List.of(new Astronaut("a1", "a1", 30, 80, 60)));
            assertTrue(!kindsApplied(RandomEvents.advanceSeason(noFailure, roster(100, 0), new Random(seed), 5, 5))
                .contains(RandomEventKind.MORALE_DROP), "morale drop applied without a failure");
        }

        boolean sawDrop = false;
        for (long seed = 0; seed < 200 && !sawDrop; seed++) {
            PlayerSnapshot withFailure = quietPlayer(0, 0);
            withFailure.setLastFailureTurn(5);
            withFailure.setAstronauts(List.of(new Astronaut("a1", "a1", 30, 80, 60)));
            sawDrop = kindsApplied(RandomEvents.advanceSeason(withFailure, roster(100, 0), new Random(seed), 5, 5))
                .contains(RandomEventKind.MORALE_DROP);
        }
        assertTrue(sawDrop, "morale drop should be reachable after a failure this season");
    }

    /**
     * Extra funding raises the rate only with a recent failure-free flight and at
     * least six seasons until the next review: never with four seasons left.
     */
    @Test
    void extraFundingNeedsReviewLeadAndRecentFlight() {
        for (long seed = 0; seed < 200; seed++) {
            PlayerSnapshot player = quietPlayer(0, 0);
            player.setLastFlightSeasonTurn(10);
            // 12 seasons elapsed leaves 4 until the next review, short of the six it needs.
            List<ResolutionEvent> events = RandomEvents.advanceSeason(player, roster(0, 100), new Random(seed), 10, 12);
            for (ResolutionEvent event : events) {
                assertFalse(event instanceof RandomEventOccurred occurred && occurred.kind() == RandomEventKind.EXTRA_FUNDING,
                    "extra funding fired with only four seasons until review");
            }
        }
        PlayerSnapshot eligible = quietPlayer(0, 0);
        eligible.setLastFlightSeasonTurn(10);
        List<ResolutionEvent> events = RandomEvents.advanceSeason(eligible, roster(0, 100), new Random(9), 10, 10);
        assertFalse(events.isEmpty(), "a recent flight with six seasons of review lead enables a good event");
    }

    /**
     * Over a long seeded run with every chance at 100%, every season's invariants
     * hold, and every sub-slice-(a) event fires at least once. The sub-slice-(b)
     * events never fire, since they aren't implemented yet.
     */
    @Test
    void longRunKeepsInvariantsAndReachesEveryImplementedEvent() {
        Set<RandomEventKind> seen = EnumSet.noneOf(RandomEventKind.class);
        for (long seed = 0; seed < 40; seed++) {
            Random random = new Random(seed);
            PlayerSnapshot player = populated(random);
            int perished = 0;
            for (int season = 1; season <= 60; season++) {
                if (random.nextDouble() < 0.5) {
                    player.setLastFlightSeasonTurn(season);
                }
                if (random.nextDouble() < 0.2) {
                    player.setLastFailureTurn(season);
                }
                // The agency's own season, as the server keeps it: the basic-training events skip its first.
                player.setSeason(season);
                List<ResolutionEvent> events = RandomEvents.advanceSeason(player, roster(100, 100), random,
                    season, season);
                for (ResolutionEvent event : events) {
                    if (!(event instanceof RandomEventOccurred occurred)) {
                        continue;
                    }
                    seen.add(occurred.kind());
                    assertTrue(IMPLEMENTED.contains(occurred.kind()), "unexpected kind " + occurred.kind());
                    if (occurred.kind() == RandomEventKind.PERISHED_ASTRONAUT) {
                        perished++;
                        MemorialEntry remembered = player.getMemorial().get(player.getMemorial().size() - 1);
                        assertEquals(MemorialEntry.Cause.ACCIDENT, remembered.cause());
                        assertEquals(season, remembered.turn());
                    }
                }
                assertEquals(perished, player.getMemorial().size(), "every perished astronaut is remembered");
                assertTrue(player.getNewsLog().size() <= PlayerSnapshot.NEWS_LOG_CAPACITY);
                assertTrue(player.getPrestige() >= 0);
                for (var astronaut : player.getAstronauts()) {
                    assertTrue(astronaut.getMorale() >= 0 && astronaut.getMorale() <= 100);
                }
                for (OwnedMissionComponent owned : player.getMissionComponents().values()) {
                    assertTrue(owned.getCurrentReliability() >= owned.getFloorReliability(),
                        "reliability fell below its transfer floor");
                }
                // Keep the run alive: a dismissal or death shrinks the roster, so refill it.
                if (player.getAstronauts().isEmpty()) {
                    player.setAstronauts(List.of(new Astronaut("refill" + season, "refill", 30, 70, 60)));
                }
            }
        }
        assertEquals(IMPLEMENTED, seen, "every implemented event should fire somewhere in the long run");
    }

    /**
     * On the agency's first season, no trainee's basic training is extended or failed (user, 2026-10-23),
     * however the dice fall; from the second season on, both can happen again.
     */
    @Test
    void basicTrainingEventsSkipTheFirstSeason() {
        Set<RandomEventKind> training = EnumSet.of(RandomEventKind.BASIC_TRAINING_EXTENSION,
            RandomEventKind.EMPLOYEE_FAILED_BASIC_TRAINING);
        Set<RandomEventKind> later = EnumSet.noneOf(RandomEventKind.class);
        for (long seed = 0; seed < 400; seed++) {
            PlayerSnapshot first = traineesOnly();
            for (RandomEventKind kind : kindsApplied(RandomEvents.advanceSeason(first, roster(100, 0),
                    new Random(seed), 1, 1))) {
                assertFalse(training.contains(kind), "on the first season: " + kind);
            }
            PlayerSnapshot second = traineesOnly();
            second.setSeason(2);
            later.addAll(kindsApplied(RandomEvents.advanceSeason(second, roster(100, 0), new Random(seed), 2, 2)));
        }
        assertTrue(later.containsAll(training), "both happen from the second season on: " + later);
    }

    /** A first-season agency whose only people are engineers in basic training. */
    private static PlayerSnapshot traineesOnly() {
        PlayerSnapshot player = quietPlayer(0, 0);
        List<SetSpecialist> specialists = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SetSpecialist specialist = new SetSpecialist("s" + i, "s" + i, 35, 70, 60);
            specialist.setAssignmentState(AssignmentState.IN_TRAINING);
            specialist.setTrainingKind(TrainingKind.BASIC);
            specialist.setTrainingSeasonsTotal(TrainingProgress.FIRST_TURN_SET_BASIC_TRAINING_SEASONS);
            specialists.add(specialist);
        }
        player.setSetSpecialists(specialists);
        player.setLastFailureTurn(-1);
        player.setLastFlightSeasonTurn(-1);
        return player;
    }

    /**
     * A seeded starting roster: astronauts, specialists and officers in every state,
     * with one component in R&amp;D, so every gate has something to act on.
     */
    private static PlayerSnapshot populated(Random random) {
        PlayerSnapshot player = quietPlayer(25_000, 500);
        player.setFundingRatePerSeason(3_000);
        List<Astronaut> astronauts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Astronaut astronaut = new Astronaut("a" + i, "a" + i, 30 + i, 70, 60);
            astronaut.setSalary(30);
            if (i % 3 == 1) {
                astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
                astronaut.setTrainingKind(TrainingKind.BASIC);
                astronaut.setTrainingSeasonsTotal(3);
            }
            astronauts.add(astronaut);
        }
        player.setAstronauts(astronauts);
        List<SetSpecialist> specialists = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SetSpecialist specialist = new SetSpecialist("s" + i, "s" + i, 35, 70, 60);
            specialist.setSalary(40);
            specialists.add(specialist);
        }
        specialists.get(0).setAssignmentState(AssignmentState.ASSIGNED);
        specialists.get(0).setRdTargetComponentId("capsule");
        player.setSetSpecialists(specialists);
        List<MissionControlOfficer> officers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            officers.add(new MissionControlOfficer("m" + i, "m" + i, 45, 70, 60));
        }
        player.setMissionControlOfficers(officers);
        player.setMissionComponents(Map.of("capsule", new OwnedMissionComponent(60.0, 40.0)));
        player.setLastFailureTurn(-1);
        player.setLastFlightSeasonTurn(-1);
        return player;
    }

}
