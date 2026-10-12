package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p><b>A launch fills staffing slots, as in BASPM</b> ({@code baspm.md}
 * &sect;10.11): one person per slot the configuration's steps use, of the slot's
 * type, and nobody in a slot no step uses. Any other filling is rejected. This
 * also closes a departure-protection exploit:
 * {@link de.mkoehler.launchwindow.model.AssignmentState#ASSIGNED} shields an
 * Astronaut/Mission Control officer from the resignation/retirement check
 * ({@link PersonnelLifecycle#isProtectedFromDeparture}), and since an unused slot
 * can never hold anyone, there is no way to park an idle employee on a mission
 * that has no actual need for them just to make them {@code ASSIGNED}.</p>
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
     * @param astronautIds           one astronaut per crew slot, in slot order
     *                               ({@link LaunchRequest#crewSlotIds()})
     * @param missionControlOfficerIds one person per control slot, in slot order
     *                               ({@link LaunchRequest#controlSlotIds()})
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
            List.of()), catalog);
    }

    /**
     * Launches a mission from a full {@link LaunchRequest}: as the shorter overload,
     * plus one rocket per launch and the CAPCOM astronauts ({@code design.md} &sect;5,
     * M3). Each rocket must be a rocket the player owns and has active (its rocket
     * program open), able to lift its launch's payload ({@link #requiredLift}) to the launch's
     * destination, and human-rated for a crewed launch; an air-dropped launch takes {@code ""}
     * instead. A configuration with {@link MissionConfiguration#requiredGoalIds()} needs them
     * achieved or cancelled. The launch costs the
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
            if (!rocketId.isEmpty()) {
                cost += catalog.missionComponent(rocketId).orElseThrow().unitCost();
            }
        }
        return cost;
    }

    /**
     * Checks the chosen rockets against the configuration's launches.
     *
     * @return the first problem, or {@code null} when every rocket fits its launch
     */
    /**
     * Checks whether a launch could go ahead now, without changing anything: the same checks
     * {@link #launch(PlayerSnapshot, LaunchRequest, ContentCatalog)} makes, in the same order.
     *
     * @param snapshot the player
     * @param request  the launch
     * @param catalog  the content catalog
     * @return {@link MissionLaunchOutcome#LAUNCHED} if it could, else why not
     * @throws NullPointerException if an argument is {@code null}
     */
    public static MissionLaunchOutcome check(PlayerSnapshot snapshot, LaunchRequest request, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        return catalog.missionConfiguration(request.missionConfigurationId())
            .map(configuration -> {
                MissionLaunchOutcome problem = validate(snapshot, configuration, request, catalog);
                return problem == null ? MissionLaunchOutcome.LAUNCHED : problem;
            })
            .orElse(MissionLaunchOutcome.UNKNOWN_MISSION_CONFIGURATION);
    }

    /**
     * Returns the payload mass one launch of a configuration carries.
     *
     * @param launch  the launch
     * @param catalog the content catalog
     * @return the mass, in kg
     */
    public static double payloadMass(MissionConfiguration.Launch launch, ContentCatalog catalog) {
        double payloadMass = 0;
        for (String payloadId : launch.payloadComponentIds()) {
            payloadMass += catalog.missionComponent(payloadId).orElseThrow().mass();
        }
        return payloadMass;
    }

    /**
     * Returns what the rocket must lift for a launch: the payloads' mass less the lift payloads
     * such as the Agena or Apollo's service module add themselves (BASPM's
     * {@code ComputeRequiredPayloadMinusExtraBoostProvidedByPayload}).
     *
     * @param launch  the launch
     * @param catalog the content catalog
     * @return the mass, in kg, never negative
     */
    public static double requiredLift(MissionConfiguration.Launch launch, ContentCatalog catalog) {
        double ownLift = 0;
        for (String payloadId : launch.payloadComponentIds()) {
            ownLift += catalog.missionComponent(payloadId).orElseThrow().ownLiftFor(launch.launchType());
        }
        return Math.max(0, payloadMass(launch, catalog) - ownLift);
    }

    /**
     * Returns whether a rocket can fly a launch: it reaches the launch type (a crewed type needs a
     * human-rated rocket) and lifts the payload ({@link #requiredLift}). A rocket with no capacity
     * for the type can't fly it at all, even an empty payload, and no rocket flies an air drop.
     *
     * @param rocket  the rocket
     * @param launch  the launch
     * @param catalog the content catalog
     * @return whether it can
     */
    public static boolean canLift(MissionComponent rocket, MissionConfiguration.Launch launch, ContentCatalog catalog) {
        double capacity = rocket.payloadCapacity(launch.launchType());
        return launch.launchType().needsRocket() && capacity > 0 && capacity >= requiredLift(launch, catalog);
    }

    private static MissionLaunchOutcome validateRockets(PlayerSnapshot snapshot, MissionConfiguration configuration,
            List<String> rocketIds, ContentCatalog catalog) {
        if (rocketIds.size() != configuration.launches().size()) {
            return MissionLaunchOutcome.WRONG_ROCKET_COUNT;
        }
        for (int i = 0; i < rocketIds.size(); i++) {
            if (!configuration.launches().get(i).launchType().needsRocket()) {
                if (!rocketIds.get(i).isEmpty()) {
                    return MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD;
                }
                continue;
            }
            MissionComponent rocket = catalog.missionComponent(rocketIds.get(i)).orElse(null);
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(rocketIds.get(i));
            if (rocket == null || !rocket.hasRocketSpec() || owned == null || !owned.isActive()) {
                return MissionLaunchOutcome.ROCKET_NOT_AVAILABLE;
            }
            if (!canLift(rocket, configuration.launches().get(i), catalog)) {
                return MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD;
            }
        }
        return null;
    }

    /**
     * The rest of {@link #launch} once the mission configuration itself is
     * known to exist in the catalog.
     *
     * @param snapshot      the player; mutated in place on success
     * @param configuration the mission configuration to launch
     * @param request       the launch choices
     * @param catalog       the content catalog
     * @return the outcome
     */
    private static MissionLaunchOutcome launchKnownConfiguration(PlayerSnapshot snapshot,
            MissionConfiguration configuration, LaunchRequest request, ContentCatalog catalog) {
        MissionLaunchOutcome problem = validate(snapshot, configuration, request, catalog);
        if (problem != null) {
            return problem;
        }
        long cost = launchCost(configuration, request.rocketIds(), catalog);
        List<String> crewSlotIds = padded(request.crewSlotIds(), configuration.crewSlots().size());
        List<String> controlSlotIds = padded(request.controlSlotIds(), configuration.controlSlots().size());

        String instanceId = snapshot.allocateMissionInstanceId();
        InProgressMission mission = new InProgressMission(instanceId, configuration.id(), crewSlotIds, controlSlotIds);
        mission.setRocketIds(request.rocketIds());
        mission.setLaunchCost(cost);
        mission.setLaunchReliability(launchReliability(snapshot, configuration, request.rocketIds()));
        snapshot.getInProgressMissions().put(instanceId, mission);
        Contracts.launched(snapshot, mission);

        snapshot.setFunds(snapshot.getFunds() - cost);
        snapshot.setLaunchesThisWindow(snapshot.getLaunchesThisWindow() + 1);
        for (String id : mission.crewIds()) {
            assign(findPerson(snapshot, id), instanceId);
        }
        for (String id : mission.groundIds()) {
            assign(findPerson(snapshot, id), instanceId);
        }
        return MissionLaunchOutcome.LAUNCHED;
    }

    /**
     * Runs every launch check, changing nothing.
     *
     * @return the first problem, or {@code null} if the launch can go ahead
     */
    private static MissionLaunchOutcome validate(PlayerSnapshot snapshot, MissionConfiguration configuration,
            LaunchRequest request, ContentCatalog catalog) {
        if (!snapshot.getOpenedProgramIds().contains(configuration.programId())) {
            return MissionLaunchOutcome.PROGRAM_NOT_OPEN;
        }
        if (!configuration.requiredGoalIds().stream().allMatch(snapshot::hasAchievedOrCancelledGoal)) {
            return MissionLaunchOutcome.GOAL_NOT_REACHED;
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
        if (CrewedGrounding.grounded(snapshot) && CrewedGrounding.crewed(configuration)) {
            return MissionLaunchOutcome.CREWED_FLIGHTS_GROUNDED;
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

        MissionLaunchOutcome crewProblem = validateCrew(snapshot, configuration, request.crewSlotIds());
        if (crewProblem != null) {
            return crewProblem;
        }
        List<String> crewSlotIds = padded(request.crewSlotIds(), configuration.crewSlots().size());
        List<String> controlSlotIds = padded(request.controlSlotIds(), configuration.controlSlots().size());
        if (controlSlotIds == null) {
            return MissionLaunchOutcome.WRONG_MISSION_CONTROL_OFFICER_COUNT;
        }
        MissionLaunchOutcome controllerProblem = validateControllerSlots(snapshot, configuration, controlSlotIds);
        if (controllerProblem != null) {
            return controllerProblem;
        }
        return validateGroundAstronautSlots(snapshot, configuration, controlSlotIds, crewSlotIds);
    }

    /**
     * Records every component a flight uses as it is at launch: the copy built for this flight,
     * whose reliability later ground R&amp;D and other flights don't change (user decision
     * 2026-10-07).
     *
     * @param snapshot      the player
     * @param configuration the configuration
     * @param rocketIds     the rockets chosen per launch
     * @return the reliabilities by component id, in flight order
     */
    static Map<String, Double> launchReliability(PlayerSnapshot snapshot,
            MissionConfiguration configuration, List<String> rocketIds) {
        Map<String, Double> reliability = new LinkedHashMap<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String componentId = resolveComponentId(involvement.componentId(), rocketIds);
                    OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
                    if (owned != null) {
                        reliability.putIfAbsent(componentId, owned.getCurrentReliability());
                    }
                }
            }
        }
        return reliability;
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
     * Pads a slot list with {@code null}s to the layout's size.
     *
     * @param ids  the given slot ids
     * @param size the layout's slot count
     * @return the padded copy, or {@code null} when more ids than slots were given
     */
    private static List<String> padded(List<String> ids, int size) {
        if (ids.size() > size) {
            return null;
        }
        List<String> copy = new ArrayList<>(ids);
        while (copy.size() < size) {
            copy.add(null);
        }
        return copy;
    }

    /**
     * Validates the crew: nobody twice, each astronaut known, idle, and available,
     * then no more ids than crew slots, every used crew slot filled, and no unused one.
     *
     * @return the first problem, or {@code null}
     */
    private static MissionLaunchOutcome validateCrew(PlayerSnapshot snapshot, MissionConfiguration configuration,
            List<String> requestedIds) {
        List<String> filled = requestedIds.stream().filter(Objects::nonNull).toList();
        if (hasDuplicate(filled)) {
            return MissionLaunchOutcome.DUPLICATE_ASTRONAUT;
        }
        for (String id : filled) {
            Astronaut astronaut = findById(snapshot.getAstronauts(), id);
            if (astronaut == null) {
                return MissionLaunchOutcome.UNKNOWN_ASTRONAUT;
            }
            if (!isAvailable(astronaut)) {
                return MissionLaunchOutcome.ASTRONAUT_NOT_IDLE;
            }
        }
        List<String> crewSlotIds = padded(requestedIds, configuration.crewSlots().size());
        if (crewSlotIds == null) {
            return MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT;
        }
        Set<Integer> used = configuration.usedCrewSlots();
        for (int slot = 0; slot < crewSlotIds.size(); slot++) {
            if (used.contains(slot) != (crewSlotIds.get(slot) != null)) {
                return MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT;
            }
        }
        return null;
    }

    /**
     * Validates the flight-controller slots: every used one holds a flight controller,
     * no unused one holds anyone, nobody twice, and each known, idle, and available.
     *
     * @return the first problem, or {@code null}
     */
    private static MissionLaunchOutcome validateControllerSlots(PlayerSnapshot snapshot,
            MissionConfiguration configuration, List<String> controlSlotIds) {
        List<String> filled = new ArrayList<>();
        for (int slot = 0; slot < controlSlotIds.size(); slot++) {
            if (isControllerSlot(configuration, slot) && controlSlotIds.get(slot) != null) {
                filled.add(controlSlotIds.get(slot));
            }
        }
        if (hasDuplicate(filled)) {
            return MissionLaunchOutcome.DUPLICATE_MISSION_CONTROL_OFFICER;
        }
        for (String id : filled) {
            MissionControlOfficer officer = findById(snapshot.getMissionControlOfficers(), id);
            if (officer == null) {
                return MissionLaunchOutcome.UNKNOWN_MISSION_CONTROL_OFFICER;
            }
            if (!isAvailable(officer)) {
                return MissionLaunchOutcome.MISSION_CONTROL_OFFICER_NOT_IDLE;
            }
        }
        Set<Integer> used = configuration.usedControlSlots();
        for (int slot = 0; slot < controlSlotIds.size(); slot++) {
            if (isControllerSlot(configuration, slot) && used.contains(slot) != (controlSlotIds.get(slot) != null)) {
                return MissionLaunchOutcome.WRONG_MISSION_CONTROL_OFFICER_COUNT;
            }
        }
        return null;
    }

    /**
     * Validates the ground astronaut slots (CAPCOM): every used one holds an astronaut,
     * no unused one holds anyone, and each astronaut known, idle, available, not
     * repeated, and not also in the crew.
     *
     * @return the first problem, or {@code null}
     */
    private static MissionLaunchOutcome validateGroundAstronautSlots(PlayerSnapshot snapshot,
            MissionConfiguration configuration, List<String> controlSlotIds, List<String> crewSlotIds) {
        Set<String> seen = new HashSet<>();
        for (int slot = 0; slot < controlSlotIds.size(); slot++) {
            String id = controlSlotIds.get(slot);
            if (isControllerSlot(configuration, slot) || id == null) {
                continue;
            }
            Astronaut astronaut = findById(snapshot.getAstronauts(), id);
            if (astronaut == null || crewSlotIds.contains(id) || !seen.add(id) || !isAvailable(astronaut)) {
                return MissionLaunchOutcome.CAPCOM_NOT_AVAILABLE;
            }
        }
        Set<Integer> used = configuration.usedControlSlots();
        for (int slot = 0; slot < controlSlotIds.size(); slot++) {
            if (!isControllerSlot(configuration, slot) && used.contains(slot) != (controlSlotIds.get(slot) != null)) {
                return MissionLaunchOutcome.WRONG_CAPCOM_COUNT;
            }
        }
        return null;
    }

    private static boolean isControllerSlot(MissionConfiguration configuration, int slot) {
        return configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.MISSION_CONTROL;
    }

    /**
     * Whether a person can be put into a slot: idle and not unavailable.
     *
     * @param person the person
     * @return {@code true} when free
     */
    public static boolean isAvailable(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() <= 0;
    }

    /**
     * Marks a person {@link AssignmentState#ASSIGNED} to a mission instance.
     *
     * @param person     the person, already validated
     * @param instanceId the mission instance id
     */
    static void assign(Personnel person, String instanceId) {
        person.setAssignmentState(AssignmentState.ASSIGNED);
        person.setAssignedMissionId(instanceId);
    }

    /**
     * Finds an astronaut or flight controller by id. Ids are unique across rosters.
     *
     * @param snapshot the player
     * @param id       the id
     * @return the person, or {@code null}
     */
    static Personnel findPerson(PlayerSnapshot snapshot, String id) {
        Personnel astronaut = findById(snapshot.getAstronauts(), id);
        return astronaut != null ? astronaut : findById(snapshot.getMissionControlOfficers(), id);
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
