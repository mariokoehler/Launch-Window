package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
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
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(astronautIds, "astronautIds must not be null");
        Objects.requireNonNull(missionControlOfficerIds, "missionControlOfficerIds must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");

        return catalog.missionConfiguration(missionConfigurationId)
            .map(configuration -> launchKnownConfiguration(snapshot, configuration, astronautIds,
                missionControlOfficerIds))
            .orElse(MissionLaunchOutcome.UNKNOWN_MISSION_CONFIGURATION);
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
            MissionConfiguration configuration, List<String> astronautIds, List<String> missionControlOfficerIds) {
        if (!snapshot.getOpenedProgramIds().contains(configuration.programId())) {
            return MissionLaunchOutcome.PROGRAM_NOT_OPEN;
        }
        if (snapshot.getFunds() < configuration.cost()) {
            return MissionLaunchOutcome.INSUFFICIENT_FUNDS;
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

        String instanceId = snapshot.allocateMissionInstanceId();
        InProgressMission mission =
            new InProgressMission(instanceId, configuration.id(), astronautIds, missionControlOfficerIds);
        snapshot.getInProgressMissions().put(instanceId, mission);

        snapshot.setFunds(snapshot.getFunds() - configuration.cost());
        assign(snapshot.getAstronauts(), astronautIds, instanceId);
        assign(snapshot.getMissionControlOfficers(), missionControlOfficerIds, instanceId);

        return MissionLaunchOutcome.LAUNCHED;
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
            if (astronaut.getAssignmentState() != AssignmentState.IDLE) {
                return MissionLaunchOutcome.ASTRONAUT_NOT_IDLE;
            }
        }
        if (astronautIds.size() != requiredCount) {
            return MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT;
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
            if (officer.getAssignmentState() != AssignmentState.IDLE) {
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
