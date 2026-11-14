package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Investigations;

/**
 * Starts the agency's investigation of a failed flight with an SET specialist as its lead
 * ({@link Investigations#start}, {@code design.md} &sect;2.14). The inverse is {@link CancelInvestigationAction}.
 * The action's server seed settles it at the resolution.
 *
 * <p>Journaled as {@code "startInvestigation"}.</p>
 *
 * @author Mario Koehler
 */
public class StartInvestigationAction extends PlayerAction {

    private String incidentId;
    private String leadId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public StartInvestigationAction() {
    }

    /**
     * Creates the action.
     *
     * @param incidentId the incident to investigate
     * @param leadId     the SET specialist to lead it
     */
    public StartInvestigationAction(String incidentId, String leadId) {
        this.incidentId = incidentId;
        this.leadId = leadId;
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

    /**
     * Returns the lead.
     *
     * @return the lead's id
     */
    public String getLeadId() {
        return leadId;
    }

    /**
     * Sets the lead.
     *
     * @param leadId the lead's id
     */
    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Investigations.start(snapshot, context.catalog(), incidentId, leadId, getSeed()),
            Investigations.Outcome.STARTED);
    }
}
