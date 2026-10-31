package de.mkoehler.launchwindow.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.PersonnelActions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The hub's reminder of a component R&amp;D could still improve with nobody on it.
 *
 * @author Mario Koehler
 */
class PayloadRndUnstaffedTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final String ROCKET = "jupiter-c";

    private final PlayerSnapshot state = new PlayerSnapshot("Agency", Archetype.NASA, 100_000, 0, 0, 1);

    @Test
    void nothingOwnedNothingToSay() {
        assertNull(PayloadRnd.unstaffedLine(state, PACK, true));
    }

    @Test
    void aRocketBelowItsCapWithNobodyOnItShows() {
        state.getMissionComponents().put(ROCKET, new OwnedMissionComponent(10, 10));
        assertNotNull(PayloadRnd.unstaffedLine(state, PACK, true));
        assertEquals(ROCKET, PayloadRnd.unstaffed(state, PACK, true).get(0).component().id());
    }

    @Test
    void aRocketShowsOnlyOnTheRocketBoard() {
        state.getMissionComponents().put(ROCKET, new OwnedMissionComponent(10, 10));
        assertNull(PayloadRnd.unstaffedLine(state, PACK, false));
    }

    @Test
    void oneEngineerOnItClearsIt() {
        state.getMissionComponents().put(ROCKET, new OwnedMissionComponent(10, 10));
        state.setSetSpecialists(new ArrayList<>(List.of(new SetSpecialist("e", "e", 30, 70, 50))));
        PersonnelActions.assignResearch(state, "e", ROCKET, PACK);
        assertNull(PayloadRnd.unstaffedLine(state, PACK, true));
    }

    @Test
    void aRocketAtItsCapDoesNotShow() {
        MissionComponent rocket = PACK.missionComponent(ROCKET).orElseThrow();
        double cap = rocket.maxRdReliability();
        state.getMissionComponents().put(ROCKET, new OwnedMissionComponent(cap, cap));
        assertTrue(PayloadRnd.unstaffed(state, PACK, true).isEmpty());
    }
}
