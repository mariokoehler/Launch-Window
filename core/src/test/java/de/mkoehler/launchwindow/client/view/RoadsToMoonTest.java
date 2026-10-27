package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the roads to the Moon ({@code ui-spec.md} &sect;4.21): the words cover every Moon program of both packs, and
 * the figures are the rules' own, checked against the mockup's (a person in space, Mercury open, nothing else).
 *
 * @author Mario Koehler
 */
class RoadsToMoonTest {

    private static final ContentCatalog STANDARD = pack("standard");
    private static final MoonRoads ROADS = read();

    private static ContentCatalog pack(String name) {
        return ContentPackLoader.load(Path.of("..", "assets", "content", name));
    }

    private static MoonRoads read() {
        try {
            return MoonRoads.parse(Files.readString(Path.of("..", "assets", "content", MoonRoads.FILE)));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static PlayerSnapshot afterMercury(Archetype archetype, String capsule, String program) {
        PlayerSnapshot state = new PlayerSnapshot("Agency", archetype, 31_400, 0, 0, 1);
        state.getOpenedProgramIds().add(program);
        state.getMissionComponents().put(capsule, new OwnedMissionComponent(60, 3));
        state.getAchievedGoals().put("earth-orbit-satellite", 1);
        state.getAchievedGoals().put("human-in-space", 9);
        return state;
    }

    /**
     * Every lunar-expedition program is a road or a road that doesn't land, every text is written, and every id
     * names a program of the pack.
     *
     * @param name the pack
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void coversEveryMoonProgram(String name) {
        ContentCatalog catalog = pack(name);
        for (Program program : catalog.programs()) {
            if (program.categoryId().equals("lunar-expeditions")) {
                assertTrue(ROADS.covers(program.id()), program.id() + " has no entry in " + MoonRoads.FILE);
            }
        }
        for (MoonRoads.Road road : ROADS.roads()) {
            assertFalse(road.howItWorks().isBlank(), road.program());
            assertFalse(road.pros().isEmpty(), road.program());
            assertFalse(road.cons().isEmpty(), road.program());
            assertFalse(road.historyNote().isBlank(), road.program());
        }
        for (MoonRoads.WithoutLanding road : ROADS.withoutLanding()) {
            assertFalse(road.line().isBlank(), road.program());
        }
    }

    @Test
    void everyIdNamesAProgramAndEveryFirstAGoal() {
        for (MoonRoads.Road road : ROADS.roads()) {
            assertTrue(STANDARD.program(road.program()).isPresent(), road.program());
            assertTrue(STANDARD.program(road.earthOrbit()).isPresent(), road.earthOrbit());
            assertNotNull(RoadsToMoon.landing(STANDARD.program(road.program()).orElseThrow(), STANDARD), road.program());
        }
        for (MoonRoads.WithoutLanding road : ROADS.withoutLanding()) {
            assertTrue(STANDARD.program(road.program()).isPresent(), road.program());
            assertTrue(STANDARD.program(road.earthOrbit()).isPresent(), road.earthOrbit());
        }
        ROADS.firsts().forEach(goal -> assertTrue(STANDARD.goal(goal).isPresent(), goal));
    }

    /**
     * NASA, Mercury open: the mockup's state 1. Each road's price splits as the rule prices it, the parts once
     * however many programs need them.
     */
    @Test
    void nasaAfterMercury() {
        PlayerSnapshot state = afterMercury(Archetype.NASA, "mercury-capsule", "mercury");
        List<RoadsToMoon.Road> roads = RoadsToMoon.roads(ROADS, state, STANDARD);
        assertEquals(List.of("apollo", "gemini-eor", "gemini-direct-ascent"), roads.stream().map(road -> road.moon().id())
            .toList());

        RoadsToMoon.Road apollo = roads.get(0);
        assertEquals(22_012, apollo.tests().amount());
        assertEquals(RoadsToMoon.Cost.FREE_AFTER_TESTS, apollo.flights().cost());
        assertEquals(23_400, apollo.partsCost());
        assertEquals(45_412, apollo.total());
        assertEquals(7_043, apollo.upkeep());
        assertEquals(4, apollo.toDevelop());
        assertEquals("saturn-v", apollo.rocket().rocket().id());
        assertEquals(RoadsToMoon.RocketState.VAB_TOO_SMALL, apollo.rocket().state());
        assertEquals(3, apollo.rocket().vabLevel());
        assertEquals(3, apollo.crew());
        assertEquals(21, apollo.flightsOnWay());
        assertEquals(28, apollo.landingSteps());

        RoadsToMoon.Road eor = roads.get(1);
        // Its Moon flights free with its own tests, as every road's (the user's fix of BASPM's data, 2026-10-26).
        assertEquals(22_483, eor.total());
        assertEquals(RoadsToMoon.Cost.FREE_AFTER_TESTS, eor.flights().cost());
        assertEquals(3_255, eor.upkeep());
        assertEquals(2, eor.rocket().launches());

        RoadsToMoon.Road direct = roads.get(2);
        assertEquals(29_256, direct.total());
        assertEquals(5_089, direct.upkeep());
        assertEquals("nova", direct.rocket().rocket().id());
        assertEquals(21, direct.landingSteps());

        assertEquals(List.of("circumlunar-gemini", "lunar-orbital-gemini"), RoadsToMoon.withoutLanding(ROADS, state,
            STANDARD).stream().map(road -> road.moon().id()).toList());
    }

    /** With Gemini open, both Gemini roads' tests are free through it, and its capsule is theirs. */
    @Test
    void geminiOpenMakesTheGeminiTestsFree() {
        PlayerSnapshot state = afterMercury(Archetype.NASA, "mercury-capsule", "mercury");
        state.getOpenedProgramIds().add("gemini");
        state.getMissionComponents().put("gemini-capsule", new OwnedMissionComponent(64, 3));
        RoadsToMoon.Road direct = RoadsToMoon.roads(ROADS, state, STANDARD).get(2);
        assertEquals(RoadsToMoon.Cost.FREE, direct.tests().cost());
        assertEquals("gemini", direct.tests().freeBecause());
        assertTrue(direct.parts().stream().anyMatch(part -> part.component().id().equals("gemini-capsule")
            && part.owned()));
    }

    /** The Soviet roads: the 7K-L3 and the LK-700, whose rocket its first test already needs. */
    @Test
    void sovietAfterVostok() {
        PlayerSnapshot state = afterMercury(Archetype.SOVIET, "vostok-capsule", "vostok");
        List<RoadsToMoon.Road> roads = RoadsToMoon.roads(ROADS, state, STANDARD);
        assertEquals(List.of("soyuz-7k-l3", "lk-700"), roads.stream().map(road -> road.moon().id()).toList());
        assertEquals(40_983, roads.get(0).total());
        assertEquals(23_369, roads.get(1).total());
        assertEquals("ur-700", roads.get(1).rocket().rocket().id());
        assertTrue(roads.get(1).rocket().fromTheFirst());
        assertFalse(roads.get(0).rocket().fromTheFirst());
        assertEquals("cosmonauts", RoadsToMoon.crewWord(Archetype.SOVIET, roads.get(1).crew()));
    }

    /** Before a person in space, the Earth-orbit tests still need it, and the screen says so. */
    @Test
    void lockedBeforeAPersonInSpace() {
        PlayerSnapshot state = afterMercury(Archetype.NASA, "mercury-capsule", "mercury");
        state.getAchievedGoals().remove("human-in-space");
        assertEquals(List.of("human-in-space"), RoadsToMoon.roads(ROADS, state, STANDARD).get(0).missingGoals());
    }
}
