package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Building;

/**
 * The capped buildings' names, as the screens say them.
 *
 * @author Mario Koehler
 */
public final class Buildings {

    private Buildings() {
    }

    /**
     * Names a building ("SET Centre").
     *
     * @param building the building
     * @return the name
     */
    public static String name(Building building) {
        return switch (building) {
            case ASTRONAUT_CENTER -> "Astronaut Complex";
            case MISSION_CONTROL -> "Mission Control";
            case SET_CENTER -> "SET Centre";
            case HEADQUARTERS -> "Headquarters";
            case VEHICLE_ASSEMBLY -> "VAB";
        };
    }
}
