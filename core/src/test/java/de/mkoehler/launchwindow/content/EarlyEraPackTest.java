package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Building;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the early-era content pack ({@code assets/content/early-era}) beyond what
 * {@link ContentCatalog} validates: every mission can actually be flown by its own
 * agency, and every staffing requirement fits a reachable building level. See
 * {@code design.md} &sect;5 (M3).
 *
 * @author Mario Koehler
 */
class EarlyEraPackTest {

    /** The pack, relative to the core module, where Maven runs the tests. */
    static final Path PACK = Path.of("..", "assets", "content", "early-era");

    private static ContentCatalog catalog;
    private static List<MissionComponent> components;

    @BeforeAll
    static void load() {
        catalog = ContentPackLoader.load(PACK);
        components = new ArrayList<>();
        for (String id : List.of("sputnik-1-satellite", "sputnik-2-satellite", "vostok-capsule",
                "explorer-1-satellite", "mercury-capsule", "r7-sputnik", "r7-vostok", "jupiter-c", "redstone",
                "atlas")) {
            components.add(catalog.missionComponent(id).orElseThrow());
        }
    }

    @Test
    void everyConfigurationHasARocketItsAgencyCanOpenAtVabLevelOne() {
        for (String configurationId : configurationIds()) {
            MissionConfiguration configuration = catalog.missionConfiguration(configurationId).orElseThrow();
            Program program = catalog.program(configuration.programId()).orElseThrow();
            assertTrue(!configuration.launches().isEmpty(), configurationId + " flies on a chosen rocket");
            for (MissionConfiguration.Launch launch : configuration.launches()) {
                double mass = launch.payloadComponentIds().stream()
                    .mapToDouble(id -> catalog.missionComponent(id).orElseThrow().mass()).sum();
                boolean flyable = components.stream().anyMatch(rocket -> rocket.hasRocketSpec()
                    && rocket.availableTo(program.agency())
                    && rocket.rocket().rocketClass().ordinal() < 1
                    && rocket.payloadCapacity(launch.launchType()) >= Math.max(mass, Double.MIN_VALUE));
                assertTrue(flyable, configurationId + " " + launch.launchType() + " (mass " + mass
                    + ") has no rocket its agency can open at VAB level 1");
            }
        }
    }

    @Test
    void staffingFitsAReachableBuildingLevel() {
        BuildingSchedule buildings = catalog.buildings();
        int maxControllers = buildings.capacity(Building.MISSION_CONTROL, 99);
        int maxAstronauts = buildings.capacity(Building.ASTRONAUT_CENTER, 99);
        for (String configurationId : configurationIds()) {
            MissionConfiguration configuration = catalog.missionConfiguration(configurationId).orElseThrow();
            assertTrue(configuration.requiredMissionControlOfficerCount() <= maxControllers, configurationId);
            assertTrue(configuration.requiredAstronautCount() + configuration.requiredCapcomCount() <= maxAstronauts,
                configurationId);
        }
    }

    @Test
    void theRaceGoalsAreReachableByBothAgencies() {
        // A goal is done when achieved or cancelled, so follow each achieved goal's cancel chain, as BASPM does
        // (Mercury's suborbital flight cancels "human in space", baspm.md §11.3).
        for (String goalId : List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit")) {
            long agencies = configurationIds().stream()
                .map(id -> catalog.missionConfiguration(id).orElseThrow())
                .filter(configuration -> configuration.seasons().stream()
                    .flatMap(season -> season.steps().stream())
                    .flatMap(step -> step.goalIdsAchieved().stream())
                    .anyMatch(achieved -> reaches(achieved, goalId, new java.util.HashSet<>())))
                .map(configuration -> catalog.program(configuration.programId()).orElseThrow().agency())
                .distinct().count();
            assertEquals(2, agencies, goalId + " should be reachable by NASA and Soviet alike");
        }
    }

    private static boolean reaches(String from, String target, java.util.Set<String> seen) {
        if (from.equals(target)) {
            return true;
        }
        if (!seen.add(from)) {
            return false;
        }
        return catalog.goal(from).orElseThrow().cancelsGoalIds().stream()
            .anyMatch(next -> reaches(next, target, seen));
    }

    @Test
    void theClasspathLoaderReadsTheSamePack() throws Exception {
        // core does not bundle assets/ itself, so this reads through a class loader rooted at assets/, the way
        // server and lwjgl3 see it.
        try (java.net.URLClassLoader assets = new java.net.URLClassLoader(
                new java.net.URL[] {Path.of("..", "assets").toUri().toURL()}, null)) {
            ContentCatalog viaClasspath = ContentPackLoader.loadFromClasspath("content/early-era", assets);
            assertEquals(catalog.program("mercury"), viaClasspath.program("mercury"));
            assertEquals(catalog.missionConfiguration("vostok-orbital"),
                viaClasspath.missionConfiguration("vostok-orbital"));
        }
    }

    static List<String> configurationIds() {
        return Stream.of("sputnik-1-orbit", "sputnik-1-extended", "sputnik-2-orbit", "korabl-sputnik-2-orbit",
            "vostok-uncrewed-suborbital", "vostok-suborbital", "vostok-uncrewed-orbital", "vostok-orbital",
            "vostok-orbital-duration-1", "vostok-orbital-duration-2", "vostok-joint-orbital", "explorer-1-orbit",
            "explorer-1-extended", "mercury-uncrewed-suborbital", "mercury-suborbital", "mercury-uncrewed-orbital",
            "mercury-orbital", "mercury-orbital-duration-1", "mercury-joint-orbital").toList();
    }
}
