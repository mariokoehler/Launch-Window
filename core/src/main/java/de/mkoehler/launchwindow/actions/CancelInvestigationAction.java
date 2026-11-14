package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Investigations;

/**
 * Withdraws an investigation started this turn, the inverse of {@link StartInvestigationAction}
 * ({@link Investigations#withdraw}): the fee comes back and the lead returns to the R&amp;D they left.
 *
 * <p>Journaled as {@code "cancelInvestigation"}.</p>
 *
 * @author Mario Koehler
 */
public class CancelInvestigationAction extends PlayerAction {

    private String incidentId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public CancelInvestigationAction() {
    }

    /**
     * Creates the action.
     *
     * @param incidentId the incident whose investigation to withdraw
     */
    public CancelInvestigationAction(String incidentId) {
        this.incidentId = incidentId;
    }

    /**
     * Returns the incident.
     *
     * @return the incident's id
     */
    public String getIncidentId() {
        return incidentId;
    }

    /**
     * Sets the incident.
     *
     * @param incidentId the incident's id
     */
    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Investigations.withdraw(snapshot, context.catalog(), incidentId),
            Investigations.Outcome.WITHDRAWN);
    }
}
