package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.BuildingUpgradeOutcome;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;

/**
 * Starts a building upgrade, paid up front and final ({@link BuildingUpgrades#startUpgrade}).
 *
 * <p>Journaled as {@code "upgradeBuilding"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class UpgradeBuildingAction extends PlayerAction {

    private Building building;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public UpgradeBuildingAction() {
    }

    /**
     * Creates the action.
     *
     * @param building the building
     */
    public UpgradeBuildingAction(Building building) {
        setBuilding(building);
    }

    /**
     * Returns the building.
     *
     * @return the building
     */
    public Building getBuilding() {
        return building;
    }

    /**
     * Sets the building.
     *
     * @param building the building
     */
    public void setBuilding(Building building) {
        this.building = building;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(BuildingUpgrades.startUpgrade(snapshot, building, context.catalog().buildings()),
            BuildingUpgradeOutcome.STARTED);
    }
}
