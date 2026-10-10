package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the hub's view logic against the real assets: both agencies' hotspots, the art paths, and
 * the time-of-day clock.
 *
 * @author Mario Koehler
 */
class HubTest {

    private static final Path ASSETS = Path.of("..", "assets");

    private static List<Hub.Hotspot> hotspots(Archetype archetype) throws IOException {
        try (InputStream in = Files.newInputStream(ASSETS.resolve(Hub.hotspotPath(archetype)))) {
            return Hub.readHotspots(in);
        }
    }

    /**
     * Each agency has exactly the eight places, every point sits on the cropped art (so the bottom
     * crop cut no polygon), and each label anchor lies inside its own polygon.
     */
    @Test
    void bothAgenciesHaveTheSevenPlacesOnTheCroppedArt() throws IOException {
        for (Archetype archetype : Archetype.values()) {
            List<Hub.Hotspot> spots = hotspots(archetype);
            Set<Hub.Place> places = spots.stream().map(Hub.Hotspot::place).collect(Collectors.toSet());
            assertEquals(EnumSet.allOf(Hub.Place.class), places, archetype.name());
            assertEquals(8, spots.size(), archetype.name());
            for (Hub.Hotspot spot : spots) {
                for (int i = 1; i < spot.vertices().length; i += 2) {
                    assertTrue(spot.vertices()[i] > 0 && spot.vertices()[i] <= Hub.ART_HEIGHT, spot.place().name());
                }
                assertTrue(spot.contains(spot.labelX(), spot.labelY()), archetype + " " + spot.place());
                assertEquals((spot.vertices().length / 2 - 2) * 3, spot.triangles().length, spot.place().name());
            }
        }
    }

    /**
     * The y axis flips against the art's height (1080): NASA's VAB label sits on the VAB of the
     * 2026-10-10 art, at the left, about 348 px from the top.
     */
    @Test
    void yFlipsAgainstTheCroppedHeight() throws IOException {
        Hub.Hotspot vab = hotspots(Archetype.NASA).stream().filter(s -> s.place() == Hub.Place.VAB).findFirst()
            .orElseThrow();
        assertEquals(312, vab.labelX(), 10);
        assertEquals(Hub.ART_HEIGHT - 348, vab.labelY(), 10);
        assertFalse(vab.contains(10, 10));
    }

    /**
     * Every art file the client can ask for exists.
     */
    @Test
    void everyArtFileExists() {
        for (Archetype archetype : Archetype.values()) {
            for (Season season : Season.values()) {
                for (Hub.TimeOfDay time : Hub.TimeOfDay.values()) {
                    assertTrue(Files.isRegularFile(ASSETS.resolve(Hub.artPath(archetype, season, time))),
                        Hub.artPath(archetype, season, time));
                }
            }
        }
        assertEquals("ui/hub/SOVIET_fall_night.jpg", Hub.artPath(Archetype.SOVIET, Season.FALL, Hub.TimeOfDay.NIGHT));
    }

    /**
     * The clock starts at noon, steps once per entry when four minutes have passed since the last
     * step (one step only, however long), and wraps from night to dawn.
     */
    @Test
    void clockStepsOnEntryAfterFourMinutes() {
        Hub.Clock clock = new Hub.Clock();
        long minute = 60_000;
        assertEquals(Hub.TimeOfDay.NOON, clock.enter(0));
        assertEquals(Hub.TimeOfDay.NOON, clock.enter(3 * minute), "under four minutes");
        assertEquals(Hub.TimeOfDay.DUSK, clock.enter(4 * minute), "four minutes since the last step");
        assertEquals(Hub.TimeOfDay.DUSK, clock.enter(7 * minute), "measured from the step, not the last entry");
        assertEquals(Hub.TimeOfDay.NIGHT, clock.enter(30 * minute), "one step, however long it was");
        assertEquals(Hub.TimeOfDay.DAWN, clock.enter(34 * minute), "night wraps to dawn");
        assertEquals(Hub.TimeOfDay.DAWN, clock.current());
    }

    /**
     * A personnel building counts its idle people, not those working, training or away; the other
     * places have none, and the hover line names the group.
     */
    @Test
    void idlePeoplePerBuilding() {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        state.getSetSpecialists().add(new SetSpecialist("e1", "A", 30, 70, 50));
        SetSpecialist away = new SetSpecialist("e2", "B", 30, 70, 50);
        away.setUnavailableSeasons(2);
        state.getSetSpecialists().add(away);
        SetSpecialist working = new SetSpecialist("e3", "C", 30, 70, 50);
        working.setAssignmentState(AssignmentState.ASSIGNED);
        state.getSetSpecialists().add(working);
        state.getAstronauts().add(new Astronaut("a1", "D", 30, 70, 50));
        state.getAstronauts().add(new Astronaut("a2", "E", 30, 70, 50));

        assertEquals(1, Hub.idle(Hub.Place.SET_CENTRE, state));
        assertEquals("1 engineer idle", Hub.idleLine(Hub.Place.SET_CENTRE, state));
        assertEquals("2 cosmonauts idle", Hub.idleLine(Hub.Place.ASTRONAUT_COMPLEX, state));
        assertNull(Hub.idleLine(Hub.Place.MISSION_CONTROL, state));
        assertEquals(0, Hub.idle(Hub.Place.MUSEUM, state));
    }
}
