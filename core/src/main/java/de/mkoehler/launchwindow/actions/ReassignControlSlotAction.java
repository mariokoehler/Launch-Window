package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.MissionStaffing;

/**
 * Puts a different person into one control slot of a scheduled or flying mission, at
 * no cost ({@link MissionStaffing#reassign}). The crew can't be changed this way.
 *
 * <p>Journaled as {@code "reassignControlSlot"}; see {@code design.md} &sect;5, M5.</p>
 *
 * @author Mario Koehler
 */
public class ReassignControlSlotAction extends PlayerAction {

    private String missionInstanceId;
    private int slot;
    private String personId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public ReassignControlSlotAction() {
    }

    /**
     * Creates the action.
     *
     * @param missionInstanceId the mission instance
     * @param slot              the control slot index
     * @param personId          the new holder
     */
    public ReassignControlSlotAction(String missionInstanceId, int slot, String personId) {
        setMissionInstanceId(missionInstanceId);
        setSlot(slot);
        setPersonId(personId);
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

    /**
     * Returns the control slot index.
     *
     * @return the control slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Sets the control slot index.
     *
     * @param slot the control slot index
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the new holder.
     *
     * @return the new holder
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets the new holder.
     *
     * @param personId the new holder
     */
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        MissionStaffing.ReassignOutcome outcome =
            MissionStaffing.reassign(snapshot, missionInstanceId, slot, personId, context.catalog());
        return new ActionResult(outcome == MissionStaffing.ReassignOutcome.REASSIGNED, outcome.name());
    }
}
