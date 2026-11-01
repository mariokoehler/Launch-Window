package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.RocketPrograms;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the standard pack ({@code assets/content/standard}), BASPM's whole tree (2026-10-08,
 * {@code design.md} &sect;5, M3), beyond what {@link ContentCatalog} validates: every mission can be
 * flown by its own agency at a reachable VAB level, staffing fits the buildings, and every goal can
 * be earned.
 *
 * @author Mario Koehler
 */
class StandardPackTest {

    /** The pack, relative to the core module, where Maven runs the tests. */
    static final Path PACK = Path.of("..", "assets", "content", "standard");

    private static ContentCatalog catalog;

    @BeforeAll
    static void load() {
        catalog = ContentPackLoader.load(PACK);
    }

    @Test
    void holdsTheWholeTree() {
        assertEquals(13, catalog.categories().size());
        assertEquals(55, catalog.programs().size());
        // 85 from BASPM, and Nova for direct ascent (2026-10-26).
        assertEquals(86, catalog.missionComponents().size());
        // 189 from BASPM, less two: the E-154 flies the X-15's three where it had five (2026-10-17); less Apollo's
        // docking with an Agena, which no Apollo flew (2026-10-27).
        assertEquals(186, catalog.missionConfigurations().size());
        assertEquals(207, catalog.goals().size());
    }

    @Test
    void keepsEveryEarlyEraId() {
        ContentCatalog early = ContentPackLoader.load(EarlyEraPackTest.PACK);
        early.programs().forEach(program -> assertTrue(catalog.program(program.id()).isPresent(), program.id()));
        early.missionConfigurations().forEach(configuration ->
            assertTrue(catalog.missionConfiguration(configuration.id()).isPresent(), configuration.id()));
        early.missionComponents().forEach(component ->
            assertTrue(catalog.missionComponent(component.id()).isPresent(), component.id()));
        early.goals().forEach(goal -> assertTrue(catalog.goal(goal.id()).isPresent(), goal.id()));
    }

    @Test
    void everyLaunchHasARocketItsAgencyCanOpenAtSomeVabLevel() {
        int topVab = catalog.buildings().of(Building.VEHICLE_ASSEMBLY).maxLevel();
        for (MissionConfiguration configuration : catalog.missionConfigurations()) {
            Archetype agency = catalog.program(configuration.programId()).orElseThrow().agency();
            for (MissionConfiguration.Launch launch : configuration.launches()) {
                if (!launch.launchType().needsRocket()) {
                    continue;
                }
                boolean flyable = catalog.missionComponents().stream().anyMatch(rocket -> rocket.hasRocketSpec()
                    && rocket.availableTo(agency)
                    && RocketPrograms.classAllowed(rocket.rocket().rocketClass(), topVab)
                    && MissionLaunch.canLift(rocket, launch, catalog));
                assertTrue(flyable, configuration.id() + " " + launch.launchType() + " has no rocket of its agency");
            }
        }
    }

    @Test
    void onlyTheAircraftDropsFlyWithoutARocket() {
        Set<String> drops = catalog.missionConfigurations().stream()
            .filter(configuration -> configuration.launches().stream()
                .anyMatch(launch -> launch.launchType() == LaunchType.AIR_DROP))
            .map(MissionConfiguration::id).collect(Collectors.toSet());
        // The E-154 is the Soviet X-15 (2026-10-17): all three of its configurations are drops too.
        assertEquals(Set.of("x-15-flight-test", "x-15-research-flight-1", "x-15-research-flight-2",
            "mig-e-154-drop-test", "mig-e-154-research-flight-1", "mig-e-154-research-flight-2"), drops);
    }

    @Test
    void staffingFitsAReachableBuildingLevel() {
        BuildingSchedule buildings = catalog.buildings();
        int maxControllers = buildings.capacity(Building.MISSION_CONTROL, 99);
        int maxAstronauts = buildings.capacity(Building.ASTRONAUT_CENTER, 99);
        for (MissionConfiguration configuration : catalog.missionConfigurations()) {
            assertTrue(configuration.requiredMissionControlOfficerCount() <= maxControllers, configuration.id());
            assertTrue(configuration.requiredAstronautCount() + configuration.requiredCapcomCount() <= maxAstronauts,
                configuration.id());
        }
    }

    @Test
    void everyGoalCanBeEarned() {
        // Achieved by a step, or cancelled (and so credited) by a goal that can be.
        Set<String> earned = new HashSet<>();
        catalog.missionConfigurations().forEach(configuration -> configuration.seasons().forEach(season ->
            season.steps().forEach(step -> earned.addAll(step.goalIdsAchieved()))));
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Goal goal : catalog.goals()) {
                if (earned.contains(goal.id())) {
                    grew |= earned.addAll(goal.cancelsGoalIds());
                }
            }
        }
        List<String> never = catalog.goals().stream().map(Goal::id).filter(id -> !earned.contains(id)).toList();
        assertEquals(List.of(), never);
    }

    @Test
    void theRaceGoalsAreReachableByBothAgencies() {
        for (String goalId : List.of("earth-orbit-satellite", "lunar-probe", "human-in-space", "human-in-orbit",
                "spacewalk", "rendezvous", "docking", "crewed-lunar-flyby", "crewed-lunar-orbit",
                "crewed-lunar-landing", "lunar-samples-returned", "venus-probe", "mars-probe")) {
            long agencies = catalog.missionConfigurations().stream()
                .filter(configuration -> configuration.seasons().stream()
                    .flatMap(season -> season.steps().stream())
                    .flatMap(step -> step.goalIdsAchieved().stream())
                    .anyMatch(achieved -> reaches(achieved, goalId, new HashSet<>())))
                .map(configuration -> catalog.program(configuration.programId()).orElseThrow().agency())
                .distinct().count();
            assertEquals(2, agencies, goalId + " should be reachable by NASA and Soviet alike");
        }
    }

    private static boolean reaches(String from, String target, Set<String> seen) {
        if (from.equals(target)) {
            return true;
        }
        if (!seen.add(from)) {
            return false;
        }
        return catalog.goal(from).orElseThrow().cancelsGoalIds().stream().anyMatch(next -> reaches(next, target, seen));
    }
}
