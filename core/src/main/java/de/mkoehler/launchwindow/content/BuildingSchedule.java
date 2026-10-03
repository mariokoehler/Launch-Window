package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Building;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>The game-wide building table: per building and level, the cap it grants,
 * its maintenance per season, and what the upgrade to the next level costs and
 * how many seasons it takes. See {@code design.md} &sect;5 (M3 decisions).</p>
 *
 * <p>{@link #baspmClose()} carries BASPM's own values ({@code baspm.md} &sect;10.9),
 * per the user's 2026-10-03 instruction. Every player starts at level 1 of every
 * building, so BASPM's construction cost (its first cost entry) has no
 * counterpart here.</p>
 *
 * <p>A content-definition type: shared, read-only game data, never per-player
 * state, and never sent over the network.</p>
 *
 * @param levels each building's level table; must hold all five buildings
 * @author Mario Koehler
 */
public record BuildingSchedule(Map<Building, Levels> levels) {

    /**
     * One building's level table. Level {@code n} reads index {@code n - 1} of
     * {@code caps} and {@code maintenance}; the upgrade from level {@code n} reads
     * index {@code n - 1} of {@code upgradeCosts} and {@code upgradeSeasons}.
     *
     * @param caps           the cap at each level; never empty, ascending
     * @param maintenance    the maintenance per season at each level, one per level
     * @param upgradeCosts   the cost of each upgrade, one fewer than levels
     * @param upgradeSeasons the seasons each upgrade takes, one fewer than levels, each positive
     */
    public record Levels(List<Integer> caps, List<Long> maintenance, List<Long> upgradeCosts,
                         List<Integer> upgradeSeasons) {

        /**
         * Validates the table and defensively copies every list.
         *
         * @throws NullPointerException     if a list is {@code null}
         * @throws IllegalArgumentException if the lists' sizes don't line up, a value
         *                                   is negative, an upgrade takes no time, or
         *                                   a cap drops from one level to the next
         */
        public Levels {
            caps = List.copyOf(caps);
            maintenance = List.copyOf(maintenance);
            upgradeCosts = List.copyOf(upgradeCosts);
            upgradeSeasons = List.copyOf(upgradeSeasons);
            if (caps.isEmpty()) {
                throw new IllegalArgumentException("caps must not be empty");
            }
            if (maintenance.size() != caps.size() || upgradeCosts.size() != caps.size() - 1
                    || upgradeSeasons.size() != caps.size() - 1) {
                throw new IllegalArgumentException("level lists must line up with " + caps.size() + " levels");
            }
            for (int i = 0; i < caps.size(); i++) {
                if (caps.get(i) < 0 || maintenance.get(i) < 0) {
                    throw new IllegalArgumentException("caps and maintenance must not be negative");
                }
                if (i > 0 && caps.get(i) < caps.get(i - 1)) {
                    throw new IllegalArgumentException("caps must not drop between levels: " + caps);
                }
            }
            for (int i = 0; i < upgradeCosts.size(); i++) {
                if (upgradeCosts.get(i) < 0) {
                    throw new IllegalArgumentException("upgradeCosts must not be negative");
                }
                if (upgradeSeasons.get(i) < 1) {
                    throw new IllegalArgumentException("upgradeSeasons must be positive");
                }
            }
        }

        /**
         * Returns the highest level this building reaches.
         *
         * @return the level count
         */
        public int maxLevel() {
            return caps.size();
        }
    }

    /**
     * Validates that every building has a table, and copies the map.
     *
     * @throws NullPointerException     if {@code levels} is {@code null}
     * @throws IllegalArgumentException if a building is missing
     */
    public BuildingSchedule {
        Objects.requireNonNull(levels, "levels must not be null");
        EnumMap<Building, Levels> copy = new EnumMap<>(Building.class);
        copy.putAll(levels);
        for (Building building : Building.values()) {
            if (copy.get(building) == null) {
                throw new IllegalArgumentException("no level table for " + building);
            }
        }
        levels = java.util.Collections.unmodifiableMap(copy);
    }

    /**
     * BASPM's own building table ({@code baspm.md} &sect;10.9).
     *
     * @return the BASPM-close schedule
     */
    public static BuildingSchedule baspmClose() {
        Map<Building, Levels> levels = new EnumMap<>(Building.class);
        levels.put(Building.ASTRONAUT_CENTER, new Levels(List.of(7, 14, 24, 35),
            List.of(175L, 525L, 1_000L, 1_400L), List.of(1_100L, 1_600L, 2_000L), List.of(2, 2, 2)));
        levels.put(Building.MISSION_CONTROL, new Levels(List.of(5, 13, 22, 35),
            List.of(150L, 500L, 900L, 1_300L), List.of(900L, 1_400L, 1_800L), List.of(1, 2, 2)));
        levels.put(Building.SET_CENTER, new Levels(List.of(7, 14, 20, 30),
            List.of(200L, 600L, 1_200L, 1_750L), List.of(1_000L, 1_500L, 1_900L), List.of(2, 2, 3)));
        levels.put(Building.HEADQUARTERS, new Levels(List.of(3, 7, 10, 16),
            List.of(150L, 400L, 750L, 1_200L), List.of(500L, 1_000L, 2_500L), List.of(2, 2, 3)));
        levels.put(Building.VEHICLE_ASSEMBLY, new Levels(List.of(4, 8, 14),
            List.of(250L, 650L, 1_300L), List.of(2_000L, 3_500L), List.of(3, 4)));
        return new BuildingSchedule(levels);
    }

    /**
     * Returns a building's level table.
     *
     * @param building the building
     * @return its table
     */
    public Levels of(Building building) {
        return levels.get(Objects.requireNonNull(building, "building must not be null"));
    }

    /**
     * Returns the cap a building grants at a level.
     *
     * @param building the building
     * @param level    the level, 1 or more; clamped to the building's top level
     * @return the cap
     */
    public int capacity(Building building, int level) {
        Levels table = of(building);
        return table.caps().get(clamp(level, table) - 1);
    }

    /**
     * Returns a building's maintenance per season at a level.
     *
     * @param building the building
     * @param level    the level, 1 or more; clamped to the building's top level
     * @return the maintenance
     */
    public long maintenance(Building building, int level) {
        Levels table = of(building);
        return table.maintenance().get(clamp(level, table) - 1);
    }

    private static int clamp(int level, Levels table) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be 1 or more: " + level);
        }
        return Math.min(level, table.maxLevel());
    }
}
