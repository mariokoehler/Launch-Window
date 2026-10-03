package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Building;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture: building tables for tests that check other arithmetic and
 * shouldn't see building upkeep in their running costs.
 *
 * @author Mario Koehler
 */
public final class TestBuildings {

    private TestBuildings() {
    }

    /**
     * Returns BASPM's caps and upgrade costs, but with no maintenance at any level.
     *
     * @return the schedule
     */
    public static BuildingSchedule withoutMaintenance() {
        Map<Building, BuildingSchedule.Levels> levels = new EnumMap<>(Building.class);
        BuildingSchedule.baspmClose().levels().forEach((building, table) -> levels.put(building,
            new BuildingSchedule.Levels(table.caps(),
                table.caps().stream().map(cap -> 0L).toList(), table.upgradeCosts(), table.upgradeSeasons())));
        return new BuildingSchedule(levels);
    }

    /**
     * Returns a catalog holding the given content and {@link #withoutMaintenance()} buildings.
     *
     * @param categories     the categories
     * @param programs       the programs
     * @param configurations the mission configurations
     * @param components     the mission components
     * @return the catalog
     */
    public static ContentCatalog catalogWithoutUpkeep(List<ProgramCategory> categories, List<Program> programs,
            List<MissionConfiguration> configurations, List<MissionComponent> components) {
        return new ContentCatalog(categories, programs, configurations, components, List.of(), withoutMaintenance());
    }
}
