package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Intelligence;

/**
 * Runs a background check on a candidate, answered at once: one of their traits, or "no notable
 * traits" ({@link Intelligence#backgroundCheck}, {@code design.md} &sect;2.9). Uses an action and
 * can't be undone.
 *
 * @author Mario Koehler
 */
public class BackgroundCheckAction extends PlayerAction {

    private String candidateId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public BackgroundCheckAction() {
    }

    /**
     * Creates the action.
     *
     * @param candidateId the candidate
     */
    public BackgroundCheckAction(String candidateId) {
        setCandidateId(candidateId);
    }

    /**
     * Returns the candidate.
     *
     * @return the candidate's id
     */
    public String getCandidateId() {
        return candidateId;
    }

    /**
     * Sets the candidate.
     *
     * @param candidateId the candidate's id
     */
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Intelligence.backgroundCheck(snapshot, candidateId), Intelligence.Outcome.CHECKED);
    }
}
