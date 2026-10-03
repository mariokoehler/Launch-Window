package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Building;

/**
 * <p>Reports a building finishing an upgrade, from a
 * {@link BuildingUpgrades#advanceSeason} pass. See {@code design.md} &sect;5 (M3).</p>
 *
 * @param building the building upgraded
 * @param newLevel the level it reached
 * @param newCap   the cap that level grants
 * @author Mario Koehler
 */
public record BuildingUpgraded(Building building, int newLevel, int newCap) implements ResolutionEvent {
}
