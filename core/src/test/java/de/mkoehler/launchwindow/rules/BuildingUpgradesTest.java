package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BuildingUpgrades}, and for the caps and upkeep the building
 * levels feed into: the Headquarters program cap and the running costs. See
 * {@code design.md} &sect;5 (M3 decisions) and {@code baspm.md} &sect;10.9.
 *
 * @author Mario Koehler
 */
class BuildingUpgradesTest {

    private static final BuildingSchedule SCHEDULE = BuildingSchedule.baspmClose();

    private static PlayerSnapshot player(long funds) {
        return new PlayerSnapshot("Agency", Archetype.NASA, funds, 0, 0, 1);
    }

    @Test
    void everyBuildingStartsAtLevelOneWithBaspmCaps() {
        PlayerSnapshot snapshot = player(0);
        for (Building building : Building.values()) {
            assertEquals(1, snapshot.buildingLevel(building));
        }
        assertEquals(3, BuildingUpgrades.capacity(snapshot, Building.HEADQUARTERS, SCHEDULE));
        assertEquals(4, BuildingUpgrades.capacity(snapshot, Building.VEHICLE_ASSEMBLY, SCHEDULE));
        // 175 + 150 + 200 + 150 + 250, BASPM's level-1 upkeep.
        assertEquals(925, BuildingUpgrades.maintenance(snapshot, SCHEDULE));
    }

    @Test
    void upgradeIsPaidUpFrontAndFinishesAfterItsSeasons() {
        PlayerSnapshot snapshot = player(5_000);

        assertEquals(BuildingUpgradeOutcome.STARTED,
            BuildingUpgrades.startUpgrade(snapshot, Building.ASTRONAUT_CENTER, SCHEDULE));
        assertEquals(3_900, snapshot.getFunds());

        assertTrue(BuildingUpgrades.advanceSeason(snapshot, SCHEDULE).isEmpty());
        assertEquals(1, snapshot.buildingLevel(Building.ASTRONAUT_CENTER), "still level 1 mid-upgrade");

        List<ResolutionEvent> events = BuildingUpgrades.advanceSeason(snapshot, SCHEDULE);
        assertEquals(List.of(new BuildingUpgraded(Building.ASTRONAUT_CENTER, 2, 14)), events);
        assertEquals(2, snapshot.buildingLevel(Building.ASTRONAUT_CENTER));
        assertTrue(snapshot.getBuildingUpgradeSeasonsRemaining().isEmpty());
        assertEquals(925 - 175 + 525, BuildingUpgrades.maintenance(snapshot, SCHEDULE));
    }

    @Test
    void secondUpgradeOfTheSameBuildingIsRefusedWhileOneRuns() {
        PlayerSnapshot snapshot = player(5_000);
        BuildingUpgrades.startUpgrade(snapshot, Building.MISSION_CONTROL, SCHEDULE);

        assertEquals(BuildingUpgradeOutcome.ALREADY_UPGRADING,
            BuildingUpgrades.startUpgrade(snapshot, Building.MISSION_CONTROL, SCHEDULE));
        assertEquals(BuildingUpgradeOutcome.STARTED,
            BuildingUpgrades.startUpgrade(snapshot, Building.HEADQUARTERS, SCHEDULE),
            "different buildings may upgrade at the same time");
        assertEquals(5_000 - 900 - 500, snapshot.getFunds());
    }

    @Test
    void topLevelAndShortFundsAreRefusedWithoutChange() {
        PlayerSnapshot snapshot = player(100);
        snapshot.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 3);

        assertEquals(BuildingUpgradeOutcome.MAX_LEVEL,
            BuildingUpgrades.startUpgrade(snapshot, Building.VEHICLE_ASSEMBLY, SCHEDULE));
        assertEquals(BuildingUpgradeOutcome.INSUFFICIENT_FUNDS,
            BuildingUpgrades.startUpgrade(snapshot, Building.SET_CENTER, SCHEDULE));
        assertEquals(100, snapshot.getFunds());
        assertTrue(snapshot.getBuildingUpgradeSeasonsRemaining().isEmpty());
    }

    @Test
    void headquartersCapsOpenPrograms() {
        ProgramCategory category = new ProgramCategory("sats", "Satellites");
        List<MissionComponent> components = new ArrayList<>();
        List<Program> programs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            components.add(new MissionComponent("c" + i, SetSkill.SPACE_PROBES, 3, 90, 80,
                List.of(1.0, 2.0, 3.0, 4.0), 0, List.of(), 0));
            programs.add(new Program("p" + i, "sats", 10, 0, List.of("c" + i), List.of()));
        }
        ContentCatalog catalog = new ContentCatalog(List.of(category), programs, List.of(), components);
        PlayerSnapshot snapshot = player(1_000);

        for (int i = 0; i < 3; i++) {
            assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(snapshot, "p" + i, catalog));
        }
        assertEquals(OpenProgramOutcome.HEADQUARTERS_FULL, ProgramOpening.open(snapshot, "p3", catalog));

        snapshot.getBuildingLevels().put(Building.HEADQUARTERS, 2);
        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(snapshot, "p3", catalog));
    }

    @Test
    void buildingUpkeepIsPartOfRunningCosts() {
        ContentCatalog empty = new ContentCatalog(List.of(), List.of(), List.of(), List.of());
        assertEquals(925, BudgetProgress.runningCosts(player(0), empty));
    }
}
