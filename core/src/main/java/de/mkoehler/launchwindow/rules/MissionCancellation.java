package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
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
     * What calling a mission off would do, as {@link #cancel} would do it.
     *
     * @param outcome      the outcome
     * @param refund       the funds returned
     * @param prestigeLost the prestige taken, at most what the player has
     */
    public record Preview(Outcome outcome, long refund, long prestigeLost) {
    }

    /**
     * Says what calling a mission off would do, without changing anything: a full refund for an
     * unflown mission, or the configuration's refund share and prestige loss for an uncrewed one
     * in flight; a crewed one in flight can't be called off.
     *
     * @param snapshot          the player
     * @param missionInstanceId the mission
     * @param catalog           the content catalog
     * @return the preview
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Preview preview(PlayerSnapshot snapshot, String missionInstanceId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        InProgressMission mission = snapshot.getInProgressMissions().get(missionInstanceId);
        if (mission == null) {
            return new Preview(Outcome.UNKNOWN_MISSION, 0, 0);
        }
        // A mission that has flown any season has moved past index 0, or has already ended.
        if (mission.getCurrentSeasonIndex() == 0) {
            return new Preview(Outcome.SCRUBBED, mission.getLaunchCost(), 0);
        }
        if (!mission.crewIds().isEmpty()) {
            return new Preview(Outcome.CREWED_MISSION_IN_FLIGHT, 0, 0);
        }
        MissionConfiguration configuration =
            catalog.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow();
        return new Preview(Outcome.CANCELLED,
            mission.getLaunchCost() * configuration.cancellationRefundPercent() / 100,
            Math.min(snapshot.getPrestige(), configuration.cancellationPrestigeDeduction()));
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
        Preview preview = preview(snapshot, missionInstanceId, catalog);
        if (preview.outcome() != Outcome.SCRUBBED && preview.outcome() != Outcome.CANCELLED) {
            return preview.outcome();
        }
        InProgressMission mission = snapshot.getInProgressMissions().get(missionInstanceId);
        boolean unflown = preview.outcome() == Outcome.SCRUBBED;
        snapshot.setFunds(snapshot.getFunds() + preview.refund());
        snapshot.setPrestige(snapshot.getPrestige() - preview.prestigeLost());
        if (unflown) {
            snapshot.setLaunchesThisWindow(Math.max(0, snapshot.getLaunchesThisWindow() - 1));
        }
        List<String> everyone = new ArrayList<>(mission.crewIds());
        everyone.addAll(mission.groundIds());
        // Released as at a mission's end: anyone already at retirement age retires.
        for (String id : everyone) {
            MissionProgress.releaseOrRetire(snapshot, id);
        }
        Contracts.calledOff(snapshot, mission, unflown);
        snapshot.getInProgressMissions().remove(missionInstanceId);
        return unflown ? Outcome.SCRUBBED : Outcome.CANCELLED;
    }
}
