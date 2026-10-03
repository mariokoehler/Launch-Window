package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>Launches a mission for a player: an M2 rules-engine resolver (see
 * {@code design.md} &sect;5 M2, &sect;2.5(f)). Headless, per
 * {@code design.md} &sect;3.10 - no persistence/network/libGDX
 * dependency.</p>
 *
 * <p><b>{@link #launch} is a plain headless operation, not tied to any
 * action-submission system</b> - same posture as {@code HiringPool.hire}/
 * {@code ProgramOpening.open}: there is no player-action dispatch path yet,
 * so this is a method a future action handler will call.</p>
 *
 * <p><b>A mission's crew/controller headcount is declared on
 * {@link MissionConfiguration} itself, not inferred from its content</b> -
 * {@link MissionConfiguration#requiredAstronautCount()}/
 * {@link MissionConfiguration#requiredMissionControlOfficerCount()} say
 * exactly how many of each a launch must supply; a launch supplying any
 * other count is rejected outright, not just "too few." This also closes
 * a departure-protection exploit:
 * {@link de.mkoehler.launchwindow.model.AssignmentState#ASSIGNED} shields
 * an Astronaut/Mission Control officer from the resignation/retirement
 * check ({@link PersonnelLifecycle#isProtectedFromDeparture}), so a
 * configuration requiring 0 of a kind can never have anyone of that kind
 * assigned to it at all - there is no way to park an idle employee on a
 * mission that has no actual need for them just to make them
 * {@code ASSIGNED}.</p>
 *
 * <p><b>Composition order for whoever wires this into one season pass
 * (M4):</b> launching a mission is a journaled player action, like
 * {@code HiringPool.hire}/{@code ProgramOpening.open}, so it runs before
 * any season-advance resolver - {@link PersonnelLifecycle}, then
 * {@link MissionProgress}, then {@link TrainingProgress}, then
 * {@link ResearchProgress}, then {@code HiringPool.refreshCandidates}
 * last.</p>
 *
 * @author Mario Koehler
 */
public final class MissionLaunch {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MissionLaunch() {
    }

    /**
     * Launches a mission for this player: validates the configuration, its
     * program, the player's funds against {@link MissionConfiguration#cost()},
     * and the supplied crew/controllers, then deducts that cost, creates a
     * new {@link InProgressMission} at season 0, and marks every supplied
     * person {@link AssignmentState#ASSIGNED} with
     * {@link Personnel#setAssignedMissionId} pointing at it. Fails without
     * mutating anything if any check fails.
     *
     * @param snapshot               the player snapshot to launch the
     *                               mission for; mutated in place on success
     * @param missionConfigurationId the mission configuration to launch
     * @param astronautIds           the astronaut ids to assign as crew;
     *                               must number exactly
     *                               {@link MissionConfiguration#requiredAstronautCount()}
     * @param missionControlOfficerIds the Mission Control officer ids to
     *                               assign; must number exactly
     *                               {@link MissionConfiguration#requiredMissionControlOfficerCount()}
     * @param catalog                the content catalog the configuration
     *                               is looked up in
     * @return the outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public static MissionLaunchOutcome launch(PlayerSnapshot snapshot, String missionConfigurationId,
            List<String> astronautIds, List<String> missionControlOfficerIds, ContentCatalog catalog) {
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(astronautIds, "astronautIds must not be null");
        Objects.requireNonNull(missionControlOfficerIds, "missionControlOfficerIds must not be null");
        return launch(snapshot, new LaunchRequest(missionConfigurationId, astronautIds, missionControlOfficerIds,
            List.of(), List.of()), catalog);
    }

    /**
     * Launches a mission from a full {@link LaunchRequest}: as the shorter overload,
     * plus one rocket per launch and the CAPCOM astronauts ({@code design.md} &sect;5,
     * M3). Each rocket must be a rocket the player owns and has active (its rocket
     * program open), able to lift its launch's summed payload mass to the launch's
     * destination, and human-rated for a crewed launch. The launch costs the
     * configuration's {@link MissionConfiguration#cost()} plus every chosen rocket's
     * {@link MissionComponent#unitCost()}. Fails without mutating anything if any check
     * fails.
     *
     * @param snapshot the player; mutated in place on success
     * @param request  the launch choices
     * @param catalog  the content catalog
     * @return the outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public static MissionLaunchOutcome launch(PlayerSnapshot snapshot, LaunchRequest request,
            ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");

        return catalog.missionConfiguration(request.missionConfigurationId())
            .map(configuration -> launchKnownConfiguration(snapshot, configuration, request, catalog))
            .orElse(MissionLaunchOutcome.UNKNOWN_MISSION_CONFIGURATION);
    }

    /**
     * Returns what launching a configuration with these rockets costs: the
     * configuration's own cost plus each rocket's unit cost.
     *
     * @param configuration the configuration
     * @param rocketIds     the chosen rockets, all known to the catalog
     * @param catalog       the content catalog
     * @return the launch cost
     */
    public static long launchCost(MissionConfiguration configuration, List<String> rocketIds,
            ContentCatalog catalog) {
        long cost = configuration.cost();
        for (String rocketId : rocketIds) {
            cost += catalog.missionComponent(rocketId).orElseThrow().unitCost();
        }
        return cost;
    }

    /**
     * Checks the chosen rockets against the configuration's launches.
     *
     * @return the first problem, or {@code null} when every rocket fits its launch
     */
    private static MissionLaunchOutcome validateRockets(PlayerSnapshot snapshot, MissionConfiguration configuration,
            List<String> rocketIds, ContentCatalog catalog) {
        if (rocketIds.size() != configuration.launches().size()) {
            return MissionLaunchOutcome.WRONG_ROCKET_COUNT;
        }
        for (int i = 0; i < rocketIds.size(); i++) {
            MissionComponent rocket = catalog.missionComponent(rocketIds.get(i)).orElse(null);
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocketIds.get(i));
            if (rocket == null || !rocket.hasRocketSpec() || owned == null || !owned.isActive()) {
                return MissionLaunchOutcome.ROCKET_NOT_AVAILABLE;
            }
            MissionConfiguration.Launch launch = configuration.launches().get(i);
            double payloadMass = 0;
            for (String payloadId : launch.payloadComponentIds()) {
                payloadMass += catalog.missionComponent(payloadId).orElseThrow().mass();
            }
            double capacity = rocket.payloadCapacity(launch.launchType());
            // A rocket with no capacity for this launch type can't fly it at all, even an empty payload.
            if (capacity <= 0 || capacity < payloadMass) {
                return MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD;
            }
        }
        return null;
    }

    /**
     * The rest of {@link #launch} once the mission configuration itself is
     * known to exist in the catalog.
     *
     * @param snapshot                 the player snapshot to launch the
     *                                 mission for; mutated in place on success
     * @param configuration            the mission configuration to launch
     * @param astronautIds             the astronaut ids to assign as crew
     * @param missionControlOfficerIds the Mission Control officer ids to assign
     * @return the outcome
     */
    private static MissionLaunchOutcome launchKnownConfiguration(PlayerSnapshot snapshot,
            MissionConfiguration configuration, LaunchRequest request, ContentCatalog catalog) {
        List<String> astronautIds = request.astronautIds();
        List<String> missionControlOfficerIds = request.missionControlOfficerIds();
        if (!snapshot.getOpenedProgramIds().contains(configuration.programId())) {
            return MissionLaunchOutcome.PROGRAM_NOT_OPEN;
        }
        // Rockets before funds: the price depends on which rockets fly.
        MissionLaunchOutcome rocketProblem = validateRockets(snapshot, configuration, request.rocketIds(), catalog);
        if (rocketProblem != null) {
            return rocketProblem;
        }
        long cost = launchCost(configuration, request.rocketIds(), catalog);
        if (snapshot.getFunds() < cost) {
            return MissionLaunchOutcome.INSUFFICIENT_FUNDS;
        }
        if (snapshot.getLaunchCap() >= 0 && snapshot.getLaunchesThisWindow() >= snapshot.getLaunchCap()) {
            return MissionLaunchOutcome.LAUNCH_CAP_REACHED;
        }
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String componentId = resolveComponentId(involvement.componentId(), request.rocketIds());
                    if (snapshot.getUnavailableComponents().containsKey(componentId)) {
                        return MissionLaunchOutcome.MISSION_COMPONENT_UNAVAILABLE;
                    }
                }
            }
        }

        MissionLaunchOutcome crewProblem =
            validateAstronauts(snapshot, astronautIds, configuration.requiredAstronautCount());
        if (crewProblem != null) {
            return crewProblem;
        }
        MissionLaunchOutcome controllerProblem = validateMissionControlOfficers(snapshot, missionControlOfficerIds,
            configuration.requiredMissionControlOfficerCount());
        if (controllerProblem != null) {
            return controllerProblem;
        }
        MissionLaunchOutcome capcomProblem = validateCapcom(snapshot, request.capcomAstronautIds(), astronautIds,
            configuration.requiredCapcomCount());
        if (capcomProblem != null) {
            return capcomProblem;
        }

        String instanceId = snapshot.allocateMissionInstanceId();
        InProgressMission mission =
            new InProgressMission(instanceId, configuration.id(), astronautIds, missionControlOfficerIds);
        mission.setRocketIds(request.rocketIds());
        mission.setCapcomAstronautIds(request.capcomAstronautIds());
        mission.setLaunchCost(cost);
        snapshot.getInProgressMissions().put(instanceId, mission);

        snapshot.setFunds(snapshot.getFunds() - cost);
        snapshot.setLaunchesThisWindow(snapshot.getLaunchesThisWindow() + 1);
        assign(snapshot.getAstronauts(), astronautIds, instanceId);
        assign(snapshot.getAstronauts(), request.capcomAstronautIds(), instanceId);
        assign(snapshot.getMissionControlOfficers(), missionControlOfficerIds, instanceId);

        return MissionLaunchOutcome.LAUNCHED;
    }

    /**
     * Resolves a step's component involvement id to the component that actually
     * flies: a rocket slot {@code @rocket-n} becomes the rocket chosen for launch
     * {@code n}; any other id is already a component id.
     *
     * @param involvementId the step's involvement id
     * @param rocketIds     the mission's chosen rockets, in launch order
     * @return the component id
     */
    public static String resolveComponentId(String involvementId, List<String> rocketIds) {
        int launchIndex = MissionConfiguration.rocketSlotIndex(involvementId);
        return launchIndex < 0 ? involvementId : rocketIds.get(launchIndex);
    }

    /**
     * Validates the given astronaut ids: no duplicates, each exists and is
     * currently {@code IDLE}, and the list's size exactly matches
     * {@code requiredCount}.
     *
     * @param snapshot      the player snapshot the ids are looked up against
     * @param astronautIds  the astronaut ids to validate
     * @param requiredCount how many are required
     * @return the first problem found, or {@code null} if the ids are valid
     */
    private static MissionLaunchOutcome validateAstronauts(PlayerSnapshot snapshot, List<String> astronautIds,
            int requiredCount) {
        if (hasDuplicate(astronautIds)) {
            return MissionLaunchOutcome.DUPLICATE_ASTRONAUT;
        }
        for (String id : astronautIds) {
            Astronaut astronaut = findById(snapshot.getAstronauts(), id);
            if (astronaut == null) {
                return MissionLaunchOutcome.UNKNOWN_ASTRONAUT;
            }
            if (astronaut.getAssignmentState() != AssignmentState.IDLE || astronaut.getUnavailableSeasons() > 0) {
                return MissionLaunchOutcome.ASTRONAUT_NOT_IDLE;
            }
        }
        if (astronautIds.size() != requiredCount) {
            return MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT;
        }
        return null;
    }

    /**
     * Validates the CAPCOM astronauts: each known, idle and available, none repeated
     * and none also in the crew, and exactly the configuration's CAPCOM count.
     *
     * @param snapshot      the player
     * @param capcomIds     the CAPCOM astronaut ids
     * @param crewIds       the crew astronaut ids
     * @param requiredCount how many CAPCOM astronauts the configuration needs
     * @return the first problem, or {@code null}
     */
    private static MissionLaunchOutcome validateCapcom(PlayerSnapshot snapshot, List<String> capcomIds,
            List<String> crewIds, int requiredCount) {
        if (hasDuplicate(capcomIds)) {
            return MissionLaunchOutcome.CAPCOM_NOT_AVAILABLE;
        }
        for (String id : capcomIds) {
            Astronaut astronaut = findById(snapshot.getAstronauts(), id);
            if (astronaut == null || crewIds.contains(id) || astronaut.getAssignmentState() != AssignmentState.IDLE
                    || astronaut.getUnavailableSeasons() > 0) {
                return MissionLaunchOutcome.CAPCOM_NOT_AVAILABLE;
            }
        }
        if (capcomIds.size() != requiredCount) {
            return MissionLaunchOutcome.WRONG_CAPCOM_COUNT;
        }
        return null;
    }

    /**
     * Validates the given Mission Control officer ids: no duplicates, each
     * exists and is currently {@code IDLE}, and the list's size exactly
     * matches {@code requiredCount}.
     *
     * @param snapshot                 the player snapshot the ids are
     *                                 looked up against
     * @param missionControlOfficerIds the Mission Control officer ids to
     *                                 validate
     * @param requiredCount            how many are required
     * @return the first problem found, or {@code null} if the ids are valid
     */
    private static MissionLaunchOutcome validateMissionControlOfficers(PlayerSnapshot snapshot,
            List<String> missionControlOfficerIds, int requiredCount) {
        if (hasDuplicate(missionControlOfficerIds)) {
            return MissionLaunchOutcome.DUPLICATE_MISSION_CONTROL_OFFICER;
        }
        for (String id : missionControlOfficerIds) {
            MissionControlOfficer officer = findById(snapshot.getMissionControlOfficers(), id);
            if (officer == null) {
                return MissionLaunchOutcome.UNKNOWN_MISSION_CONTROL_OFFICER;
            }
            if (officer.getAssignmentState() != AssignmentState.IDLE || officer.getUnavailableSeasons() > 0) {
                return MissionLaunchOutcome.MISSION_CONTROL_OFFICER_NOT_IDLE;
            }
        }
        if (missionControlOfficerIds.size() != requiredCount) {
            return MissionLaunchOutcome.WRONG_MISSION_CONTROL_OFFICER_COUNT;
        }
        return null;
    }

    /**
     * Marks every given id's {@link Personnel} record
     * {@link AssignmentState#ASSIGNED} with
     * {@link Personnel#setAssignedMissionId} pointing at {@code instanceId}.
     * Every id is already known to exist and be {@code IDLE} by the time
     * this runs - see {@link #validateAstronauts}/{@link #validateMissionControlOfficers}.
     *
     * @param roster     the roster the ids belong to
     * @param ids        the ids to assign
     * @param instanceId the mission instance id to point them at
     */
    private static void assign(List<? extends Personnel> roster, List<String> ids, String instanceId) {
        for (String id : ids) {
            Personnel person = findById(roster, id);
            person.setAssignmentState(AssignmentState.ASSIGNED);
            person.setAssignedMissionId(instanceId);
        }
    }

    /**
     * Finds a {@link Personnel} record by id within a roster.
     *
     * @param roster the roster to search
     * @param id     the id to find
     * @param <T>    the roster's own {@link Personnel} subtype
     * @return the matching record, or {@code null} if none matches
     */
    private static <T extends Personnel> T findById(List<T> roster, String id) {
        for (T person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }

    /**
     * Whether a list of ids contains any duplicate.
     *
     * @param ids the ids to check
     * @return {@code true} if any id repeats
     */
    private static boolean hasDuplicate(List<String> ids) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                return true;
            }
        }
        return false;
    }
}
