package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Building upgrades: starting one (a player action) and finishing one (the
 * season pass). An M3 rules-engine resolver, headless. See {@code design.md}
 * &sect;5 (M3 decisions) and {@code baspm.md} &sect;10.9.</p>
 *
 * <p>An upgrade is paid in full when it starts and takes the schedule's number of
 * seasons; the building keeps its current level, and cap, until then. Several
 * buildings may be upgraded at once, but one building only one level at a time.</p>
 *
 * @author Mario Koehler
 */
public final class BuildingUpgrades {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private BuildingUpgrades() {
    }

    /**
     * Starts upgrading a building to its next level, paying the upgrade's cost.
     *
     * @param snapshot the player; mutated only when the upgrade starts
     * @param building the building to upgrade
     * @param schedule the building table
     * @return the outcome
     * @throws NullPointerException if an argument is {@code null}
     */
    public static BuildingUpgradeOutcome startUpgrade(PlayerSnapshot snapshot, Building building,
            BuildingSchedule schedule) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(building, "building must not be null");
        Objects.requireNonNull(schedule, "schedule must not be null");
        if (snapshot.getBuildingUpgradeSeasonsRemaining().containsKey(building)) {
            return BuildingUpgradeOutcome.ALREADY_UPGRADING;
        }
        BuildingSchedule.Levels table = schedule.of(building);
        int level = snapshot.buildingLevel(building);
        if (level >= table.maxLevel()) {
            return BuildingUpgradeOutcome.MAX_LEVEL;
        }
        long cost = table.upgradeCosts().get(level - 1);
        if (snapshot.getFunds() < cost) {
            return BuildingUpgradeOutcome.INSUFFICIENT_FUNDS;
        }
        snapshot.setFunds(snapshot.getFunds() - cost);
        snapshot.getBuildingUpgradeSeasonsRemaining().put(building, table.upgradeSeasons().get(level - 1));
        return BuildingUpgradeOutcome.STARTED;
    }

    /**
     * Advances every upgrade in progress by one season, raising a building's
     * level when its upgrade finishes.
     *
     * @param snapshot the player; mutated in place
     * @param schedule the building table
     * @return one {@link BuildingUpgraded} per upgrade that finished
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, BuildingSchedule schedule) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(schedule, "schedule must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        Iterator<Map.Entry<Building, Integer>> upgrades =
            snapshot.getBuildingUpgradeSeasonsRemaining().entrySet().iterator();
        while (upgrades.hasNext()) {
            Map.Entry<Building, Integer> upgrade = upgrades.next();
            int remaining = upgrade.getValue() - 1;
            if (remaining > 0) {
                upgrade.setValue(remaining);
                continue;
            }
            upgrades.remove();
            Building building = upgrade.getKey();
            int newLevel = snapshot.buildingLevel(building) + 1;
            snapshot.getBuildingLevels().put(building, newLevel);
            events.add(new BuildingUpgraded(building, newLevel, schedule.capacity(building, newLevel)));
        }
        return events;
    }

    /**
     * Returns the cap a player's building currently grants.
     *
     * @param snapshot the player
     * @param building the building
     * @param schedule the building table
     * @return the cap at the building's current level
     */
    public static int capacity(PlayerSnapshot snapshot, Building building, BuildingSchedule schedule) {
        return schedule.capacity(building, snapshot.buildingLevel(building));
    }

    /**
     * Returns what a player's buildings cost to maintain this season, at their current levels.
     *
     * @param snapshot the player
     * @param schedule the building table
     * @return the summed maintenance
     */
    public static long maintenance(PlayerSnapshot snapshot, BuildingSchedule schedule) {
        long total = 0;
        for (Building building : Building.values()) {
            total += schedule.maintenance(building, snapshot.buildingLevel(building));
        }
        return total;
    }
}
