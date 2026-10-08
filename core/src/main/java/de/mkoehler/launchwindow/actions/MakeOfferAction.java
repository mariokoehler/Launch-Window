package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Poaching;

/**
 * Makes a poaching offer to a person in one of the agency's personnel files ({@link Poaching},
 * {@code design.md} &sect;2.2(d)): queued for the resolution, cancelled like an operation
 * ({@link CancelIntelOperationAction}).
 *
 * @author Mario Koehler
 */
public class MakeOfferAction extends PlayerAction {

    private String targetId;
    private String personId;
    private int raisePercent;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public MakeOfferAction() {
    }

    /**
     * Creates the action.
     *
     * @param targetId     the rival's player id
     * @param personId     the person's id in the rival's agency
     * @param raisePercent the raise, in percent
     */
    public MakeOfferAction(String targetId, String personId, int raisePercent) {
        setTargetId(targetId);
        setPersonId(personId);
        setRaisePercent(raisePercent);
    }

    /**
     * Returns the rival's player id.
     *
     * @return the id
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Sets the rival's player id.
     *
     * @param targetId the id
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Returns the person's id in the rival's agency.
     *
     * @return the id
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets the person's id in the rival's agency.
     *
     * @param personId the id
     */
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    /**
     * Returns the raise, in percent.
     *
     * @return the raise
     */
    public int getRaisePercent() {
        return raisePercent;
    }

    /**
     * Sets the raise, in percent.
     *
     * @param raisePercent the raise
     */
    public void setRaisePercent(int raisePercent) {
        this.raisePercent = raisePercent;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Poaching.queueOffer(snapshot, targetId, personId, raisePercent),
            Poaching.Outcome.QUEUED);
    }
}
