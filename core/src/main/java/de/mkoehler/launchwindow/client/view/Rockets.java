package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.RocketClass;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.RocketPrograms;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The VAB's view logic ({@code ui-spec.md} &sect;4.5): which rockets the agency can open and which
 * are open, the VAB's cap and rocket classes, and the missions flying on a rocket. The rules
 * themselves are {@link RocketPrograms}'s, so the screens grey out and refuse exactly as the
 * server would.
 *
 * @author Mario Koehler
 */
public final class Rockets {

    private Rockets() {
    }

    /**
     * Returns the agency's rockets whose programs aren't open, never opened or closed, in the
     * content pack's order. Other agencies' rockets never show.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the rockets
     */
    public static List<MissionComponent> closed(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.missionComponents().stream()
            .filter(MissionComponent::hasRocketSpec)
            .filter(rocket -> rocket.availableTo(state.getArchetype()))
            .filter(rocket -> !isOpen(rocket, state))
            .toList();
    }

    /**
     * Returns the rockets whose programs are open, in the content pack's order.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the rockets
     */
    public static List<MissionComponent> open(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.missionComponents().stream()
            .filter(MissionComponent::hasRocketSpec)
            .filter(rocket -> isOpen(rocket, state))
            .toList();
    }

    private static boolean isOpen(MissionComponent rocket, PlayerSnapshot state) {
        OwnedMissionComponent owned = state.getMissionComponents().get(rocket.id());
        return owned != null && owned.isActive();
    }

    /**
     * Returns whether a rocket is still owned from a closed program: reopening keeps its
     * reliability but charges the creation cost again.
     *
     * @param rocket the rocket
     * @param state  the player's state
     * @return whether kept
     */
    public static boolean kept(MissionComponent rocket, PlayerSnapshot state) {
        OwnedMissionComponent owned = state.getMissionComponents().get(rocket.id());
        return owned != null && !owned.isActive();
    }

    /**
     * Returns whether the VAB's level is high enough for a rocket's class.
     *
     * @param rocket the rocket
     * @param state  the player's state
     * @return whether it can be built
     */
    public static boolean classAllowed(MissionComponent rocket, PlayerSnapshot state) {
        return RocketPrograms.classAllowed(rocket.rocket().rocketClass(), state.buildingLevel(Building.VEHICLE_ASSEMBLY));
    }

    /**
     * Returns the rocket classes a VAB level builds, as the building panel words them
     * ("Light, Medium").
     *
     * @param vabLevel the level
     * @return the words
     */
    public static String classes(int vabLevel) {
        return Arrays.stream(RocketClass.values())
            .filter(rocketClass -> RocketPrograms.classAllowed(rocketClass, vabLevel))
            .map(Rockets::className)
            .collect(Collectors.joining(", "));
    }

    /**
     * Returns the class a VAB level newly unlocks, the one the next level adds.
     *
     * @param vabLevel the level being reached
     * @return the class, or {@code null} if the level adds none
     */
    public static RocketClass unlockedAt(int vabLevel) {
        for (RocketClass rocketClass : RocketClass.values()) {
            if (RocketPrograms.classAllowed(rocketClass, vabLevel)
                && !RocketPrograms.classAllowed(rocketClass, vabLevel - 1)) {
                return rocketClass;
            }
        }
        return null;
    }

    /**
     * Words a rocket class ("Medium").
     *
     * @param rocketClass the class
     * @return the word
     */
    public static String className(RocketClass rocketClass) {
        String lower = rocketClass.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Returns how many rocket programs the VAB holds at its level.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the cap
     */
    public static int cap(PlayerSnapshot state, ContentCatalog catalog) {
        return BuildingUpgrades.capacity(state, Building.VEHICLE_ASSEMBLY, catalog.buildings());
    }

    /**
     * Returns the in-flight missions using a rocket, by their configuration ids.
     *
     * @param rocketId the rocket
     * @param state    the player's state
     * @return the configuration ids, in mission order
     */
    public static List<String> missionsUsing(String rocketId, PlayerSnapshot state) {
        return state.getInProgressMissions().values().stream()
            .filter(mission -> mission.getRocketIds().contains(rocketId))
            .map(InProgressMission::getMissionConfigurationId)
            .toList();
    }
}
