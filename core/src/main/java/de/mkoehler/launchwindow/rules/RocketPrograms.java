package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.RocketClass;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.Objects;

/**
 * <p>Rocket programs: how a player gets a rocket. A rocket is a mission component
 * no payload program grants; opening its rocket program buys it, and closing the
 * program sets it aside. BASPM's {@code OpenRocketProgramAndAddMissionComponents}
 * and {@code CloseRocketProgram} ({@code baspm.md} &sect;10.9). Headless, an M3
 * rules-engine resolver.</p>
 *
 * <p>Opening charges the rocket's full creation cost every time, as BASPM does,
 * even when the rocket is still owned from a closed program; that reopening keeps
 * the rocket's reliability. The VAB caps how many rocket programs are open at once,
 * and its level sets the largest rocket class. An open rocket program is an owned,
 * active rocket.</p>
 *
 * @author Mario Koehler
 */
public final class RocketPrograms {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private RocketPrograms() {
    }

    /**
     * Returns how many rocket programs a player has open: owned, active rockets.
     *
     * @param snapshot the player
     * @param catalog  the content catalog
     * @return the open rocket program count
     */
    public static int openCount(PlayerSnapshot snapshot, ContentCatalog catalog) {
        int count = 0;
        for (var owned : snapshot.getMissionComponents().entrySet()) {
            if (owned.getValue().isActive()
                    && catalog.missionComponent(owned.getKey()).orElseThrow().hasRocketSpec()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns whether a VAB level can build a rocket class: level 1 builds Light rockets, 2
     * Medium, 3 Heavy ({@code baspm.md} &sect;10.9).
     *
     * @param rocketClass the class
     * @param vabLevel    the VAB's level
     * @return whether it can
     */
    public static boolean classAllowed(RocketClass rocketClass, int vabLevel) {
        return rocketClass.ordinal() < vabLevel;
    }

    /**
     * Returns whether a mission in flight uses a rocket, which keeps its program from closing.
     *
     * @param snapshot the player
     * @param rocketId the rocket component
     * @return whether one does
     */
    public static boolean inFlight(PlayerSnapshot snapshot, String rocketId) {
        return snapshot.getInProgressMissions().values().stream()
            .anyMatch(mission -> mission.getRocketIds().contains(rocketId));
    }

    /**
     * Checks whether a rocket program could open now, without changing anything: the same checks
     * {@link #open} makes, in the same order.
     *
     * @param snapshot the player
     * @param rocketId the rocket component
     * @param catalog  the content catalog
     * @return {@link RocketProgramOutcome#OPENED} if it could, else why not
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RocketProgramOutcome checkOpen(PlayerSnapshot snapshot, String rocketId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(rocketId, "rocketId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        MissionComponent rocket = catalog.missionComponent(rocketId).orElse(null);
        if (rocket == null || !rocket.hasRocketSpec()) {
            return RocketProgramOutcome.UNKNOWN_ROCKET;
        }
        if (!rocket.availableTo(snapshot.getArchetype())) {
            return RocketProgramOutcome.WRONG_AGENCY;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocketId);
        if (owned != null && owned.isActive()) {
            return RocketProgramOutcome.ALREADY_OPEN;
        }
        if (openCount(snapshot, catalog) >= BuildingUpgrades.capacity(snapshot, Building.VEHICLE_ASSEMBLY,
                catalog.buildings())) {
            return RocketProgramOutcome.VAB_FULL;
        }
        if (!classAllowed(rocket.rocket().rocketClass(), snapshot.buildingLevel(Building.VEHICLE_ASSEMBLY))) {
            return RocketProgramOutcome.ROCKET_CLASS_TOO_LARGE;
        }
        if (snapshot.getFunds() < rocket.creationCost()) {
            return RocketProgramOutcome.INSUFFICIENT_FUNDS;
        }
        return RocketProgramOutcome.OPENED;
    }

    /**
     * Opens a rocket program, paying the rocket's creation cost.
     *
     * @param snapshot the player; mutated only when the program opens
     * @param rocketId the rocket component
     * @param catalog  the content catalog
     * @return the outcome
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RocketProgramOutcome open(PlayerSnapshot snapshot, String rocketId, ContentCatalog catalog) {
        RocketProgramOutcome check = checkOpen(snapshot, rocketId, catalog);
        if (check != RocketProgramOutcome.OPENED) {
            return check;
        }
        MissionComponent rocket = catalog.missionComponent(rocketId).orElseThrow();
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocketId);
        snapshot.setFunds(snapshot.getFunds() - rocket.creationCost());
        if (owned != null) {
            owned.setActive(true);
        } else {
            double initial = openingReliability(snapshot, rocket);
            snapshot.getMissionComponents().put(rocketId, new OwnedMissionComponent(initial, initial));
        }
        return RocketProgramOutcome.OPENED;
    }

    /**
     * Returns the reliability a rocket would have if its program opened now: the reliability it kept
     * if it was open before, else its baseline plus the one-time transfer from the related rockets
     * the player owns, at their reliability right now ({@link ProgramOpening#reliabilityTransferFor}).
     * The transfer is taken only at opening, so researching a related rocket first raises it.
     *
     * @param snapshot the player
     * @param rocket   the rocket
     * @return the reliability, in percent
     * @throws NullPointerException if an argument is {@code null}
     */
    public static double openingReliability(PlayerSnapshot snapshot, MissionComponent rocket) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(rocket, "rocket must not be null");
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocket.id());
        if (owned != null) {
            return owned.getCurrentReliability();
        }
        double transfer = ProgramOpening.reliabilityTransferFor(rocket, snapshot.getMissionComponents());
        return Math.min(rocket.maxReliability(), rocket.baselineReliability() + transfer);
    }

    /**
     * Closes a rocket program: the rocket goes inactive, keeping its reliability,
     * pays no upkeep, and its researchers are released. Refused while a mission in
     * flight uses the rocket.
     *
     * @param snapshot the player; mutated only when the program closes
     * @param rocketId the rocket component
     * @param catalog  the content catalog
     * @return the outcome
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RocketProgramOutcome close(PlayerSnapshot snapshot, String rocketId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(rocketId, "rocketId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        MissionComponent rocket = catalog.missionComponent(rocketId).orElse(null);
        if (rocket == null || !rocket.hasRocketSpec()) {
            return RocketProgramOutcome.UNKNOWN_ROCKET;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocketId);
        if (owned == null || !owned.isActive()) {
            return RocketProgramOutcome.NOT_OPEN;
        }
        if (inFlight(snapshot, rocketId)) {
            return RocketProgramOutcome.MISSIONS_IN_FLIGHT;
        }
        owned.setActive(false);
        ResearchProgress.releaseResearchers(snapshot, rocketId);
        return RocketProgramOutcome.CLOSED;
    }
}
