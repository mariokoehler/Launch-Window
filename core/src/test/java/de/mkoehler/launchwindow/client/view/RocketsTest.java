package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.RocketClass;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the VAB's view logic on a small catalog with what the real pack lacks: a Medium rocket
 * (every early-era rocket is Light) and another agency's rocket.
 *
 * @author Mario Koehler
 */
class RocketsTest {

    private static final List<Double> BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    private static MissionComponent rocket(String id, RocketClass rocketClass, Archetype agency, long cost) {
        return new MissionComponent(id, SetSkill.ROCKETS, 40, 95, 85, BANDS, 0, List.of(), 50, cost, 300, 0, agency,
            new MissionComponent.RocketSpec(rocketClass, 1_000, 500, 0, 0));
    }

    private static ContentCatalog catalog() {
        return new ContentCatalog(List.of(), List.of(), List.of(), List.of(
            rocket("light", RocketClass.LIGHT, Archetype.SOVIET, 2_000),
            rocket("medium", RocketClass.MEDIUM, Archetype.SOVIET, 4_000),
            rocket("theirs", RocketClass.LIGHT, Archetype.NASA, 1_000)), List.of());
    }

    private static PlayerSnapshot soviet() {
        return new PlayerSnapshot("Vega", Archetype.SOVIET, 50_000, 0, 0, 1);
    }

    /**
     * Another agency's rockets never show; open ones move from one list to the other, and a closed
     * one is "kept".
     */
    @Test
    void listsAndKeptRockets() {
        ContentCatalog catalog = catalog();
        PlayerSnapshot state = soviet();
        assertEquals(List.of("light", "medium"), Rockets.closed(state, catalog).stream().map(MissionComponent::id)
            .toList());
        state.getMissionComponents().put("light", new OwnedMissionComponent(50, 40));
        assertEquals(List.of("medium"), Rockets.closed(state, catalog).stream().map(MissionComponent::id).toList());
        assertEquals(List.of("light"), Rockets.open(state, catalog).stream().map(MissionComponent::id).toList());
        state.getMissionComponents().get("light").setActive(false);
        assertTrue(Rockets.kept(catalog.missionComponent("light").orElseThrow(), state));
        assertFalse(Rockets.kept(catalog.missionComponent("medium").orElseThrow(), state));
    }

    /**
     * A Level 1 VAB builds Light rockets only: the Medium row is greyed out, and the rule refuses
     * it with the code the server sends. Level 2 builds it.
     */
    @Test
    void classGateFollowsTheVabLevel() {
        ContentCatalog catalog = catalog();
        PlayerSnapshot state = soviet();
        MissionComponent medium = catalog.missionComponent("medium").orElseThrow();
        assertFalse(Rockets.classAllowed(medium, state));
        assertEquals(RocketProgramOutcome.ROCKET_CLASS_TOO_LARGE, RocketPrograms.checkOpen(state, "medium", catalog));
        assertEquals(50_000, state.getFunds(), "checking changes nothing");
        state.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 2);
        assertTrue(Rockets.classAllowed(medium, state));
        assertEquals(RocketProgramOutcome.OPENED, RocketPrograms.checkOpen(state, "medium", catalog));
    }

    /**
     * The panel's class words, and what each upgrade unlocks.
     */
    @Test
    void classWordsPerLevel() {
        assertEquals("Light", Rockets.classes(1));
        assertEquals("Light, Medium", Rockets.classes(2));
        assertEquals("Light, Medium, Heavy", Rockets.classes(3));
        assertEquals(RocketClass.MEDIUM, Rockets.unlockedAt(2));
        assertEquals(RocketClass.HEAVY, Rockets.unlockedAt(3));
        assertNull(Rockets.unlockedAt(4));
    }
}
