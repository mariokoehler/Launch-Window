package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.MissionCancellation;

/**
 * Scrubs an unflown mission with a full refund, or cancels an uncrewed one in flight - the inverse of {@link LaunchMissionAction} ({@link MissionCancellation#cancel}).
 *
 * <p>Journaled as {@code "cancelMission"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class CancelMissionAction extends PlayerAction {

    private String missionInstanceId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public CancelMissionAction() {
    }

    /**
     * Creates the action.
     *
     * @param missionInstanceId the mission instance
     */
    public CancelMissionAction(String missionInstanceId) {
        setMissionInstanceId(missionInstanceId);
    }

    /**
     * Returns the mission instance.
     *
     * @return the mission instance
     */
    public String getMissionInstanceId() {
        return missionInstanceId;
    }

    /**
     * Sets the mission instance.
     *
     * @param missionInstanceId the mission instance
     */
    public void setMissionInstanceId(String missionInstanceId) {
        this.missionInstanceId = missionInstanceId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        MissionCancellation.Outcome outcome = MissionCancellation.cancel(snapshot, missionInstanceId, context.catalog());
        return new ActionResult(outcome == MissionCancellation.Outcome.SCRUBBED
            || outcome == MissionCancellation.Outcome.CANCELLED, outcome.name());
    }
}
