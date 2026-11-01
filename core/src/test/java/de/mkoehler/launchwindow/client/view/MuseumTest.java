package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Museum's wording and order: race places and First tags, achievements from goal records
 * (and without them), flights and the memorial.
 */
class MuseumTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final List<String> DECLARED = List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit");

    @Test
    void theRacePutsTheWinnerFirstAndTagsFirsts() {
        List<RivalSummary> rivals = List.of(
            RivalSummary.of("Orion", "Ana Reyes", Archetype.NASA, false, Map.of("earth-orbit-satellite", 11)),
            RivalSummary.of("Zarya", "Nina Tarasova", Archetype.SOVIET, true,
                Map.of("earth-orbit-satellite", 12, "human-in-space", 24, "human-in-orbit", 24)),
            RivalSummary.of("Kestrel", "Moses Okafor", Archetype.NASA, false, Map.of()));

        List<Museum.RaceRow> rows = Museum.race(rivals, DECLARED);

        assertEquals(List.of("Zarya (you)", "Orion", "Kestrel"), rows.stream().map(Museum.RaceRow::agency).toList());
        assertEquals(List.of(1, 2, 3), rows.stream().map(Museum.RaceRow::place).toList());
        assertTrue(rows.get(0).winner());
        assertFalse(rows.get(0).cells().get(0).first(), "Orion reached orbit a turn earlier");
        assertTrue(rows.get(1).cells().get(0).first());
        assertTrue(rows.get(0).cells().get(2).first());
        assertEquals(Museum.date(24), rows.get(0).finished());
        assertEquals("—", rows.get(1).finished());
        assertTrue(Museum.medalTable(rivals, DECLARED).isEmpty());
    }

    @Test
    void achievementsSayHowEachGoalSettled() {
        PlayerSnapshot state = new PlayerSnapshot("Zarya", Archetype.SOVIET, 0, 0, 0, 25);
        state.setAchievedGoals(Map.of("earth-orbit-satellite", 12, "sputnik-1-flown", 12));
        state.setCancelledGoalIds(new LinkedHashSet<>(List.of("radio-signals-from-orbit")));
        state.setGoalRecords(List.of(
            new GoalRecord("earth-orbit-satellite", 12, "sputnik-1-orbit", null, true, 600),
            new GoalRecord("radio-signals-from-orbit", 12, "sputnik-1-orbit", "earth-orbit-satellite", true, 250)));

        Map<String, Museum.Achievement> rows = new java.util.HashMap<>();
        Museum.achievements(state, PACK).forEach(row -> rows.put(row.goalId(), row));

        assertTrue(rows.get("earth-orbit-satellite").how().startsWith("Reached by "));
        assertEquals("+ 600", rows.get("earth-orbit-satellite").prestige());
        assertEquals("Credited through Earth Orbit Satellite", rows.get("radio-signals-from-orbit").how());
        assertEquals("Reached", rows.get("sputnik-1-flown").how(), "a goal settled before records existed");
        assertFalse(rows.get("human-in-orbit").done());
        Museum.AchievementSummary summary = Museum.summary(state, PACK);
        assertEquals(3, summary.done());
        assertEquals(1, summary.credited());
        assertEquals(2, summary.firsts());
        assertEquals(850, summary.prestige());
    }

    @Test
    void flightsAndTheMemorialReadTheState() {
        PlayerSnapshot state = new PlayerSnapshot("Zarya", Archetype.SOVIET, 0, 0, 0, 25);
        state.setSuccessfulFlightCounts(Map.of("vostok-orbital", 1));
        state.setMemorial(List.of(new MemorialEntry("Pavel Orlov", 31, 22, MemorialEntry.Cause.MISSION,
            "vostok-orbital", "reentry", null)));

        List<Museum.Flight> flights = Museum.flights(state, PACK);
        assertEquals(1, flights.size());
        assertTrue(flights.get(0).crewed());
        assertEquals(0, flights.get(0).commercial());
        List<Museum.Plaque> plaques = Museum.memorial(state, PACK);
        assertEquals("Lost on a mission, " + Museum.date(22) + ", aged 31.", plaques.get(0).line());
        assertTrue(plaques.get(0).detail().contains("reentry"), plaques.get(0).detail());
    }

    /**
     * Commercial flights count on their configuration's card, and put a configuration flown only for
     * clients on the tab (2026-10-27).
     */
    @Test
    void commercialFlightsCountBesideTheAgencysOwn() {
        PlayerSnapshot state = new PlayerSnapshot("Zarya", Archetype.SOVIET, 0, 0, 0, 25);
        state.setSuccessfulFlightCounts(Map.of("vostok-orbital", 1));
        state.setCommercialFlightCounts(Map.of("vostok-orbital", 2, "vostok-uncrewed-orbital", 1));
        Map<String, Museum.Flight> flights = new java.util.HashMap<>();
        Museum.flights(state, PACK).forEach(flight -> flights.put(flight.configurationId(), flight));
        assertEquals(3, flights.get("vostok-orbital").count());
        assertEquals(2, flights.get("vostok-orbital").commercial());
        assertEquals(1, flights.get("vostok-uncrewed-orbital").count());
        assertEquals(1, flights.get("vostok-uncrewed-orbital").commercial());
    }

    /**
     * The animals that came home are honoured per successful flight: Belka and Strelka on the first
     * Korabl-Sputnik 2, Chernushka after one uncrewed Vostok and Zvezdochka after the second.
     */
    @Test
    void flightsHonourTheAnimalsThatCameHome() {
        PlayerSnapshot state = new PlayerSnapshot("Zarya", Archetype.SOVIET, 0, 0, 0, 25);
        state.setSuccessfulFlightCounts(Map.of("korabl-sputnik-2-orbit", 3, "vostok-uncrewed-orbital", 1,
            "vostok-orbital", 1));
        Map<String, List<String>> honoured = new java.util.HashMap<>();
        Museum.flights(state, PACK).forEach(flight -> honoured.put(flight.configurationId(), flight.cameHome()));
        assertEquals(List.of("Belka", "Strelka"), honoured.get("korabl-sputnik-2-orbit"), "once, however often flown");
        assertEquals(List.of("Chernushka"), honoured.get("vostok-uncrewed-orbital"));
        assertEquals(List.of(), honoured.get("vostok-orbital"));

        state.setSuccessfulFlightCounts(Map.of("vostok-uncrewed-orbital", 2));
        assertEquals(List.of("Chernushka", "Zvezdochka"), Museum.flights(state, PACK).get(0).cameHome());
        assertEquals("Chernushka, Zvezdochka and Ham", Museum.names(List.of("Chernushka", "Zvezdochka", "Ham")));
        assertEquals("Belka and Strelka", Museum.names(List.of("Belka", "Strelka")));
    }

    /**
     * The real pack's Sputnik 2 carries Laika, and her plaque reads as a passenger's: no age, and a
     * flight with no way home.
     */
    @Test
    void sputnik2sPassengerHasHerOwnPlaque() {
        assertEquals("Laika", PACK.missionConfiguration("sputnik-2-orbit").orElseThrow().animalPassengers()
            .animals().get(0).name());
        PlayerSnapshot state = new PlayerSnapshot("Zarya", Archetype.SOVIET, 0, 0, 0, 25);
        state.setMemorial(List.of(new MemorialEntry("Laika", 0, 4, MemorialEntry.Cause.PASSENGER,
            "sputnik-2-orbit", null, "laika")));
        Museum.Plaque plaque = Museum.memorial(state, PACK).get(0);
        assertEquals("Passenger", plaque.kind());
        assertEquals("laika", plaque.portraitId());
        assertTrue(plaque.line().startsWith("Flew on "), plaque.line());
        assertFalse(plaque.line().contains("aged"), plaque.line());
        assertEquals("The flight had no way home.", plaque.detail());
    }
}
