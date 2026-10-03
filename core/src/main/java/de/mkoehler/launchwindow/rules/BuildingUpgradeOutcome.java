package de.mkoehler.launchwindow.rules;

/**
 * The result of asking {@link BuildingUpgrades#startUpgrade} to start an
 * upgrade. Every outcome but {@link #STARTED} leaves the player unchanged.
 *
 * @author Mario Koehler
 */
public enum BuildingUpgradeOutcome {

    /** The upgrade was paid for and started. */
    STARTED,

    /** The building is already being upgraded. */
    ALREADY_UPGRADING,

    /** The building is already at its top level. */
    MAX_LEVEL,

    /** The player can't afford the upgrade. */
    INSUFFICIENT_FUNDS
}
