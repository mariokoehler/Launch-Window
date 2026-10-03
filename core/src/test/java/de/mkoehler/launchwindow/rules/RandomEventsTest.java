package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.RandomEventRoster;
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

    /** Sub-slice (a) - the events this resolver implements. */
    private static final Set<RandomEventKind> IMPLEMENTED = EnumSet.of(
        RandomEventKind.BASIC_TRAINING_EXTENSION, RandomEventKind.EMPLOYEE_DISMISSED,
        RandomEventKind.EMPLOYEE_FAILED_BASIC_TRAINING, RandomEventKind.EMPLOYEE_RESIGNING,
        RandomEventKind.PERISHED_ASTRONAUT, RandomEventKind.MORALE_DROP, RandomEventKind.MORALE_BOOST,
        RandomEventKind.PRESTIGE_DROP, RandomEventKind.PRESTIGE_POINTS_BOOST, RandomEventKind.UNEXPECTED_COST,
        RandomEventKind.RELIABILITY_SETBACK, RandomEventKind.EXTRA_FUNDING);

    /** The roster with BASPM's own ranges and every chance set to a given value. */
    private static RandomEventRoster roster(int badPercent, int goodPercent) {
        RandomEventRoster base = RandomEventRoster.baspmClose();
        return new RandomEventRoster(badPercent, goodPercent, base.events());
    }

    private static PlayerSnapshot quietPlayer(long funds, long prestige) {
        return new PlayerSnapshot("Agency", Archetype.NASA, funds, prestige, 3_000, 1);
    }

    /**
     * With no personnel, no prestige, no components in R&amp;D, and no failures, a
     * positive-funds player can only ever suffer an unexpected cost.
     */
    @Test
    void unexpectedCostIsTheOnlyEventInAQuietState() {
        PlayerSnapshot player = quietPlayer(10_000, 0);

        List<ResolutionEvent> events = RandomEvents.advanceSeason(player, roster(100, 0), new Random(1), 5, 5);

        assertEquals(1, events.size());
        RandomEventOccurred occurred = (RandomEventOccurred) events.get(0);
        assertEquals(RandomEventKind.UNEXPECTED_COST, occurred.kind());
        assertTrue(occurred.magnitude() >= 7 && occurred.magnitude() < 15, "7-15% range, got " + occurred.magnitude());
        long expectedCost = (long) Math.floor(10_000 * occurred.magnitude() / 100.0);
        assertEquals(10_000 - expectedCost, player.getFunds());
        assertEquals(1, player.getNewsLog().size());
        assertEquals(new NewsEntry(5, RandomEventKind.UNEXPECTED_COST, null, occurred.magnitude()),
            player.getNewsLog().get(0));
    }

    /**
     * In debt there is no cash to take, so the same quiet state yields nothing.
     */
    @Test
    void debtBlocksEverythingInAQuietState() {
        PlayerSnapshot player = quietPlayer(-500, 0);

        assertTrue(RandomEvents.advanceSeason(player, roster(100, 0), new Random(2), 5, 5).isEmpty());
        assertEquals(-500, player.getFunds());
    }

    /**
     * A morale drop needs a mission failure this very season, and nothing else
     * applies in a quiet state, so the drop either happens or nothing does.
     */
    @Test
    void moraleDropNeedsAFailureThisSeason() {
        PlayerSnapshot withFailure = quietPlayer(0, 0);
        withFailure.setLastFailureTurn(5);
        Astronaut astronaut = new Astronaut("a1", "a1", 30, 80, 60);
        withFailure.setAstronauts(List.of(astronaut));
        // Make the astronaut un-eligible for the personnel events by putting them on a mission.
        astronaut.setAssignmentState(AssignmentState.ASSIGNED);
        astronaut.setAssignedMissionId("m0");

        List<ResolutionEvent> events = RandomEvents.advanceSeason(withFailure, roster(100, 0), new Random(3), 5, 5);
        assertEquals(RandomEventKind.MORALE_DROP, ((RandomEventOccurred) events.get(0)).kind());
        assertTrue(((RandomEventOccurred) events.get(0)).magnitude() >= 10, "a 10-19 point drop, inclusive of 10");

        PlayerSnapshot noFailure = quietPlayer(0, 0);
        noFailure.setAstronauts(List.of(new Astronaut("a1", "a1", 30, 80, 60)));
        noFailure.getAstronauts().get(0).setAssignmentState(AssignmentState.ASSIGNED);
        assertTrue(RandomEvents.advanceSeason(noFailure, roster(100, 0), new Random(3), 5, 5).isEmpty());
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
            for (int season = 1; season <= 60; season++) {
                if (random.nextDouble() < 0.5) {
                    player.setLastFlightSeasonTurn(season);
                }
                if (random.nextDouble() < 0.2) {
                    player.setLastFailureTurn(season);
                }
                List<ResolutionEvent> events = RandomEvents.advanceSeason(player, roster(100, 100), random,
                    season, season);
                for (ResolutionEvent event : events) {
                    if (!(event instanceof RandomEventOccurred occurred)) {
                        continue;
                    }
                    seen.add(occurred.kind());
                    assertTrue(IMPLEMENTED.contains(occurred.kind()), "unexpected kind " + occurred.kind());
                }
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
