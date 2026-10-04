package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link WinCondition}: the finish line, the same-turn medal table,
 * exact ties, and cancelled goals counting at their canceller's turn. See
 * {@code design.md} &sect;2.9.</p>
 *
 * @author Mario Koehler
 */
class WinConditionTest {

    /** A and B are plain goals; A cancels D; C is unused by the lists below. */
    private static final ContentCatalog CATALOG = new ContentCatalog(List.of(), List.of(), List.of(), List.of(),
        List.of(new Goal("A", 1, 1, List.of("D")), new Goal("B", 1, 1, List.of()),
            new Goal("C", 1, 1, List.of()), new Goal("D", 1, 1, List.of())));

    private static PlayerSnapshot player(Map<String, Integer> achieved, List<String> cancelled) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setAchievedGoals(achieved);
        snapshot.setCancelledGoalIds(new java.util.LinkedHashSet<>(cancelled));
        return snapshot;
    }

    private static Map<String, PlayerSnapshot> game(String alphaId, PlayerSnapshot alpha, String betaId,
            PlayerSnapshot beta) {
        Map<String, PlayerSnapshot> players = new LinkedHashMap<>();
        players.put(alphaId, alpha);
        players.put(betaId, beta);
        return players;
    }

    /**
     * Nobody has finished the list yet.
     */
    @Test
    void nobodyFinishedMeansNoWinnerYet() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of("A", 3), List.of()),
            "beta", player(Map.of("B", 4), List.of()));

        WinCondition.Outcome outcome = WinCondition.evaluate(players, List.of("A", "B"), CATALOG);

        assertEquals(WinCondition.Status.NO_WINNER_YET, outcome.status());
        assertEquals(List.of(), outcome.winners());
    }

    /**
     * The first player to finish the list wins outright, on the earlier turn.
     */
    @Test
    void earlierFinishWinsOutright() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of("A", 3, "B", 5), List.of()),
            "beta", player(Map.of("A", 2, "B", 6), List.of()));

        WinCondition.Outcome outcome = WinCondition.evaluate(players, List.of("A", "B"), CATALOG);

        assertEquals(WinCondition.Status.WON, outcome.status());
        assertEquals(List.of("alpha"), outcome.winners());
    }

    /**
     * On a same-turn finish, the medal table decides: alpha takes first place on both
     * goals to beta's one.
     */
    @Test
    void sameTurnFinishIsDecidedByTheMedalTable() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of("A", 5, "B", 7), List.of()),
            "beta", player(Map.of("A", 6, "B", 7), List.of()));

        WinCondition.Outcome outcome = WinCondition.evaluate(players, List.of("A", "B"), CATALOG);

        assertEquals(WinCondition.Status.WON, outcome.status());
        assertEquals(List.of("alpha"), outcome.winners());
    }

    /**
     * When every place is level, neither player is declared the winner.
     */
    @Test
    void exactMedalTableTieLeavesBothPlayersTied() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of("A", 5, "B", 7), List.of()),
            "beta", player(Map.of("A", 7, "B", 5), List.of()));

        WinCondition.Outcome outcome = WinCondition.evaluate(players, List.of("A", "B"), CATALOG);

        assertEquals(WinCondition.Status.TIED, outcome.status());
        assertEquals(List.of("alpha", "beta"), outcome.winners());
    }

    /**
     * A cancelled goal is done at the turn of the achieved goal that cancelled it,
     * so alpha, who achieved A on turn 4 (which cancels D), finishes on turn 4.
     */
    @Test
    void cancelledGoalIsDoneAtItsCancellersTurn() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of("A", 4), List.of("D")),
            "beta", player(Map.of("B", 2, "D", 5), List.of()));

        WinCondition.Outcome outcome = WinCondition.evaluate(players, List.of("A", "D"), CATALOG);

        assertEquals(WinCondition.Status.WON, outcome.status());
        assertEquals(List.of("alpha"), outcome.winners());
    }

    /**
     * An empty list, a repeated goal, or an unknown goal is rejected.
     */
    @Test
    void rejectsAnInvalidDeclaredList() {
        Map<String, PlayerSnapshot> players = game("alpha", player(Map.of(), List.of()),
            "beta", player(Map.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> WinCondition.evaluate(players, List.of(), CATALOG));
        assertThrows(IllegalArgumentException.class,
            () -> WinCondition.evaluate(players, List.of("A", "A"), CATALOG));
        assertThrows(IllegalArgumentException.class,
            () -> WinCondition.evaluate(players, List.of("A", "ghost"), CATALOG));
    }

    /**
     * On the real early-era pack, a Vostok Orbital landing settles all three default declared
     * goals: Human in orbit through the cascade, and Human in space two links further down it.
     */
    @Test
    void aVostokOrbitalLandingFinishesTheDefaultGoals() {
        ContentCatalog pack = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
        List<String> declared = List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit");
        PlayerSnapshot zarya = player(Map.of("earth-orbit-satellite", 12, "vostok-orbital-flown", 24),
            List.of("radio-signals-from-orbit", "vostok-uncrewed-orbital-flown", "vostok-suborbital-flown",
                "human-in-orbit", "vostok-uncrewed-suborbital-flown", "human-in-space"));

        Map<String, Integer> times = WinCondition.goalTimes(zarya, declared, pack);

        assertEquals(Map.of("earth-orbit-satellite", 12, "human-in-space", 24, "human-in-orbit", 24), times);
        assertEquals(java.util.Optional.of(24), WinCondition.completionTurn(times, declared));
    }

    /**
     * The medal table places each tied player per goal, sharing a shared place.
     */
    @Test
    void theMedalTablePlacesTiedPlayers() {
        Map<String, Map<String, Integer>> times = new LinkedHashMap<>();
        times.put("alpha", Map.of("A", 2, "B", 5));
        times.put("beta", Map.of("A", 3, "B", 5));
        times.put("gamma", Map.of("A", 1));

        assertEquals(List.of("alpha", "beta"), WinCondition.sameTurnFinishers(times, List.of("A", "B")));
        WinCondition.MedalTable table = WinCondition.medalTable(List.of("alpha", "beta"), times, List.of("A", "B"));
        assertEquals(List.of(1, 1), table.places().get("alpha"));
        assertEquals(List.of(2, 1), table.places().get("beta"));
        assertEquals(List.of("alpha"), WinCondition.evaluateTimes(times, List.of("A", "B")).winners());
    }
}
