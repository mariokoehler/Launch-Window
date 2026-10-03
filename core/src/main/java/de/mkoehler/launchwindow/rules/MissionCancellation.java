package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * <p>Taking back a launched mission - the inverse of {@link MissionLaunch}
 * (user decisions, 2026-10-03, {@code design.md} &sect;5, M4):</p>
 * <ul>
 *   <li><b>Scrub:</b> a mission that hasn't flown a season yet - one launched today -
 *       is called off with a full refund of what its launch cost, and everyone
 *       assigned goes back to idle. BASPM refunds an assembled, unflown mission the
 *       same way ({@code baspm.md} &sect;10.10).</li>
 *   <li><b>Cancel in flight:</b> a mission already flying can be cancelled only if
 *       it carries no crew. It loses the configuration's cancellation prestige
 *       (floored at zero) and restores its cancellation percentage of the launch
 *       cost. A crewed mission always runs to its end or failure.</li>
 * </ul>
 *
 * @author Mario Koehler
 */
public final class MissionCancellation {

    /** The result of {@link #cancel}. */
    public enum Outcome {
        /** An unflown mission was scrubbed with a full refund. */
        SCRUBBED,
        /** An uncrewed mission in flight was cancelled. */
        CANCELLED,
        /** No mission in flight has that instance id. */
        UNKNOWN_MISSION,
        /** The mission is in flight with crew aboard, so it can't be abandoned. */
        CREWED_MISSION_IN_FLIGHT
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MissionCancellation() {
    }

    /**
     * Scrubs or cancels a mission.
     *
     * @param snapshot          the player; mutated only on success
     * @param missionInstanceId the mission instance
     * @param catalog           the content catalog
     * @return the outcome
     */
    public static Outcome cancel(PlayerSnapshot snapshot, String missionInstanceId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        InProgressMission mission = snapshot.getInProgressMissions().get(missionInstanceId);
        if (mission == null) {
            return Outcome.UNKNOWN_MISSION;
        }
        // A mission that has flown any season has moved past index 0, or has already ended.
        boolean unflown = mission.getCurrentSeasonIndex() == 0;
        if (unflown) {
            snapshot.setFunds(snapshot.getFunds() + mission.getLaunchCost());
            snapshot.setLaunchesThisWindow(Math.max(0, snapshot.getLaunchesThisWindow() - 1));
        } else {
            if (!mission.getAssignedAstronautIds().isEmpty()) {
                return Outcome.CREWED_MISSION_IN_FLIGHT;
            }
            MissionConfiguration configuration =
                catalog.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow();
            snapshot.setPrestige(Math.max(0, snapshot.getPrestige() - configuration.cancellationPrestigeDeduction()));
            snapshot.setFunds(snapshot.getFunds()
                + mission.getLaunchCost() * configuration.cancellationRefundPercent() / 100);
        }
        release(snapshot.getAstronauts(), mission.getAssignedAstronautIds());
        release(snapshot.getAstronauts(), mission.getCapcomAstronautIds());
        release(snapshot.getMissionControlOfficers(), mission.getAssignedMissionControlOfficerIds());
        snapshot.getInProgressMissions().remove(missionInstanceId);
        return unflown ? Outcome.SCRUBBED : Outcome.CANCELLED;
    }

    private static void release(List<? extends Personnel> roster, List<String> ids) {
        for (Personnel person : roster) {
            if (ids.contains(person.getId())) {
                person.setAssignmentState(AssignmentState.IDLE);
                person.setAssignedMissionId(null);
            }
        }
    }
}
